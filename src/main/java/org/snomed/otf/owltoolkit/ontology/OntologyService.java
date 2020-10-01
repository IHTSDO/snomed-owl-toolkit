/*
 * Copyright 2019 SNOMED International, http://snomed.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.snomed.otf.owltoolkit.ontology;

import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.domain.AxiomRepresentation;
import org.snomed.otf.owltoolkit.domain.Relationship;
import org.snomed.otf.owltoolkit.ontology.render.SnomedFunctionalSyntaxDocumentFormat;
import org.snomed.otf.owltoolkit.ontology.render.SnomedFunctionalSyntaxStorerFactory;
import org.snomed.otf.owltoolkit.ontology.render.SnomedPrefixManager;
import org.snomed.otf.owltoolkit.service.ReasonerServiceRuntimeException;
import org.snomed.otf.owltoolkit.taxonomy.Description;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;
import static org.snomed.otf.owltoolkit.domain.Relationship.*;

@SuppressWarnings("Guava")
public class OntologyService {

	public static final String SNOMED_CORE_COMPONENTS_URI = "http://snomed.info/id/";
	public static final String SNOMED_INTERNATIONAL_EDITION_URI = "http://snomed.info/sct/900000000000207008";
	public static final String ONTOLOGY_URI_VERSION_POSTFIX = "/version/";
	public static final String COLON = ":";
	public static final String ROLE_GROUP_SCTID = "609096000";
	public static final String ROLE_GROUP_OUTDATED_CONSTANT = "roleGroup";
	public static final String SNOMED_ROLE_GROUP_SHORT_URI = COLON + ROLE_GROUP_SCTID;
	public static final String SNOMED_ROLE_GROUP_FULL_URI = SNOMED_CORE_COMPONENTS_URI + ROLE_GROUP_SCTID;
	public static final String CORE_COMPONENT_NAMESPACE_PATTERN = "<http://snomed.info/id/([0-9]+)>";

	// SKOS URIs
	private static final String SKOS_PREFIX = "skos";
	private static final String SKOS_URI = "http://www.w3.org/2004/02/skos/core#";
	private static final String SKOS_PREF_LABEL_URI = SKOS_URI + "prefLabel";
	private static final String SKOS_ALT_LABEL_URI = SKOS_URI + "altLabel";
	private static final String SKOS_DEFINITION_LABEL_URI = SKOS_URI + "definition";
	public static final String LANGUAGE_REFSET_DIALECT_MAP_PROPERTIES = "language-refset-dialect-map.properties";

	private final OWLOntologyManager manager;
	private OWLDataFactory factory;
	private DefaultPrefixManager prefixManager;
	private final Set<Long> ungroupedAttributes;
	private AtomicLong missingDialectWarnings;

	public OntologyService(Set<Long> ungroupedAttributes) {
		this.ungroupedAttributes = ungroupedAttributes;
		manager = OWLManager.createOWLOntologyManager();
		factory = new OWLDataFactoryImpl();
		prefixManager = new DefaultPrefixManager();
		prefixManager.setDefaultPrefix(SNOMED_CORE_COMPONENTS_URI);
		missingDialectWarnings = new AtomicLong();
	}

	public OWLOntology createOntology(SnomedTaxonomy snomedTaxonomy) throws OWLOntologyCreationException {
		return createOntology(snomedTaxonomy, null, null, false);
	}

	public OWLOntology createOntology(SnomedTaxonomy snomedTaxonomy, String ontologyUri, String versionDate, boolean includeDescriptions) throws OWLOntologyCreationException {

		Map<Long, Set<OWLAxiom>> axiomsFromStatedRelationships = createAxiomsFromStatedRelationships(snomedTaxonomy);

		Map<Long, String> langRefsetToDialectMap = null;
		if (includeDescriptions) {
			langRefsetToDialectMap = loadLanguageRefsetToDialectMap();
		}

		Set<OWLAxiom> axioms = new HashSet<>();
		for (Long conceptId : snomedTaxonomy.getAllConceptIds()) {

			// Add raw axioms from the axiom reference set file
			axioms.addAll(snomedTaxonomy.getConceptAxiomMap().getOrDefault(conceptId, Collections.emptySet()));

			// Add axioms generated from stated relationships
			axioms.addAll(axiomsFromStatedRelationships.getOrDefault(conceptId, Collections.emptySet()));

			if (includeDescriptions) {
				addDescriptionAnnotations(conceptId, snomedTaxonomy, axioms, langRefsetToDialectMap);
			}
		}

		OWLOntology ontology;
		if (Strings.isNullOrEmpty(ontologyUri)) {
			ontologyUri = SNOMED_INTERNATIONAL_EDITION_URI;
		}
		if (Strings.isNullOrEmpty(versionDate)) {
			ontology = manager.createOntology(IRI.create(ontologyUri));
		} else {
			ontology = manager.createOntology(new OWLOntologyID(
					com.google.common.base.Optional.of(IRI.create(ontologyUri)),
					com.google.common.base.Optional.of(IRI.create(ontologyUri + ONTOLOGY_URI_VERSION_POSTFIX + versionDate))));
		}

		manager.addAxioms(ontology, axioms);
		manager.setOntologyFormat(ontology, getFunctionalSyntaxDocumentFormat());
		return ontology;
	}

	public Map<Long, Set<OWLAxiom>> createAxiomsFromStatedRelationships(SnomedTaxonomy snomedTaxonomy, Set<Long> conceptIds) {
		Map<Long, Set<OWLAxiom>> axiomsMap = new Long2ObjectOpenHashMap<>();

		// Create axioms of concept model attributes
		// The Concept Model Object Attribute concept did not always exist - use the parent if it doesn't exist
		boolean conceptModelObjectAttributePresent = snomedTaxonomy.getAllConceptIds().contains(Concepts.CONCEPT_MODEL_OBJECT_ATTRIBUTE_LONG);
		Long conceptModelObjectAttribute = conceptModelObjectAttributePresent ?
				Concepts.CONCEPT_MODEL_OBJECT_ATTRIBUTE_LONG : Concepts.CONCEPT_MODEL_ATTRIBUTE_LONG;

		Set<Long> descendants = snomedTaxonomy.getDescendants(conceptModelObjectAttribute);
		for (Long objectAttributeId : descendants) {
			if (conceptIds != null && !conceptIds.contains(objectAttributeId)) {
				continue;
			}
			for (Relationship relationship : snomedTaxonomy.getStatedRelationships(objectAttributeId)) {
				if (relationship.getTypeId() == Concepts.IS_A_LONG) {
					axiomsMap.computeIfAbsent(objectAttributeId, (id) -> new HashSet<>())
							.add(createOwlSubObjectPropertyOfAxiom(objectAttributeId, relationship.getDestinationId()));
				}
			}
		}

		if (snomedTaxonomy.getAllConceptIds().contains(Concepts.CONCEPT_MODEL_DATA_ATTRIBUTE_LONG)) {
			for (Long dataAttributeId : snomedTaxonomy.getDescendants(Concepts.CONCEPT_MODEL_DATA_ATTRIBUTE_LONG)) {
				if (conceptIds != null && !conceptIds.contains(dataAttributeId)) {
					continue;
				}
				for (Relationship relationship : snomedTaxonomy.getStatedRelationships(dataAttributeId)) {
					if (relationship.getTypeId() == Concepts.IS_A_LONG) {
						axiomsMap.computeIfAbsent(dataAttributeId, (id) -> new HashSet<>())
								.add(createOwlSubDataPropertyOfAxiom(dataAttributeId, relationship.getDestinationId()));
					}
				}
			}
		}

		// Create axioms of all other Snomed concepts
		Set<Long> attributeIds = snomedTaxonomy.getDescendants(Concepts.CONCEPT_MODEL_ATTRIBUTE_LONG);

		// Link the top object and data property into the class hierarchy.
		// The top object and data properties will be represented as an OWL Property and as a Class to ensure that this link comes out in the NNF.
		// "The OWL 2 specification allows different entities with the same IRI to co-exist in an ontology" (thanks @apeteri)
		// Removing them from the attributeIds here set will ensure Class axioms are created.
		attributeIds.remove(Concepts.CONCEPT_MODEL_OBJECT_ATTRIBUTE_LONG);
		attributeIds.remove(Concepts.CONCEPT_MODEL_DATA_ATTRIBUTE_LONG);

		for (Long conceptId : snomedTaxonomy.getAllConceptIds()) {
			if (conceptIds != null && !conceptIds.contains(conceptId)) {
				continue;
			}
			// Convert any stated relationships to axioms
			boolean primitive = snomedTaxonomy.isPrimitive(conceptId);
			Collection<Relationship> statedRelationships = snomedTaxonomy.getStatedRelationships(conceptId);

			if (!statedRelationships.isEmpty() && !attributeIds.contains(conceptId)) {
				AxiomRepresentation representation = new AxiomRepresentation();
				representation.setPrimitive(primitive);
				representation.setLeftHandSideNamedConcept(conceptId);
				Map<Integer, List<Relationship>> relationshipMap = new HashMap<>();
				for (Relationship statedRelationship : statedRelationships) {
					relationshipMap.computeIfAbsent(statedRelationship.getGroup(), g -> new ArrayList<>()).add(statedRelationship);
				}
				representation.setRightHandSideRelationships(relationshipMap);
				OWLClassAxiom conceptAxiom = createOwlClassAxiom(representation);
				axiomsMap.computeIfAbsent(conceptId, (id) -> new HashSet<>())
						.add(conceptAxiom);
			}
		}
		return axiomsMap;
	}

	public Map<Long, Set<OWLAxiom>> createAxiomsFromStatedRelationships(SnomedTaxonomy snomedTaxonomy) {
		return createAxiomsFromStatedRelationships(snomedTaxonomy, null);
	}
	
	public void saveOntology(OWLOntology ontology, OutputStream outputStream) throws OWLOntologyStorageException {
		manager.getOntologyStorers().add(new SnomedFunctionalSyntaxStorerFactory());

		FunctionalSyntaxDocumentFormat owlDocumentFormat = getFunctionalSyntaxDocumentFormat();
		ontology.getOWLOntologyManager().setOntologyFormat(ontology, owlDocumentFormat);
		ontology.saveOntology(owlDocumentFormat, outputStream);
	}

	public FunctionalSyntaxDocumentFormat getFunctionalSyntaxDocumentFormat() {
		FunctionalSyntaxDocumentFormat owlDocumentFormat = new SnomedFunctionalSyntaxDocumentFormat();
		SnomedPrefixManager prefixManager = getSnomedPrefixManager();
		owlDocumentFormat.setPrefixManager(prefixManager);
		owlDocumentFormat.setDefaultPrefix(SNOMED_CORE_COMPONENTS_URI);
		owlDocumentFormat.setPrefix(SKOS_PREFIX, SKOS_URI);
		return owlDocumentFormat;
	}

	public SnomedPrefixManager getSnomedPrefixManager() {
		SnomedPrefixManager prefixManager = new SnomedPrefixManager();
		prefixManager.setDefaultPrefix(SNOMED_CORE_COMPONENTS_URI);
		prefixManager.setPrefix(SKOS_PREFIX, SKOS_URI);
		return prefixManager;
	}

	public OWLClassAxiom createOwlClassAxiom(AxiomRepresentation axiomRepresentation) {
		// Left side is usually a single named concept
		OWLClassExpression leftSide = createOwlClassExpression(axiomRepresentation.getLeftHandSideNamedConcept(), axiomRepresentation.getLeftHandSideRelationships());

		// Right side is usually an expression created from a set of stated relationships
		OWLClassExpression rightSide = createOwlClassExpression(axiomRepresentation.getRightHandSideNamedConcept(), axiomRepresentation.getRightHandSideRelationships());

		if (axiomRepresentation.isPrimitive()) {
			return factory.getOWLSubClassOfAxiom(leftSide, rightSide);
		} else {
			return factory.getOWLEquivalentClassesAxiom(leftSide, rightSide);
		}
	}

	public OWLSubObjectPropertyOfAxiom createOwlSubObjectPropertyOfAxiom(Long objectAttributeId, long destinationId) {
		return factory.getOWLSubObjectPropertyOfAxiom(getOwlObjectProperty(objectAttributeId), getOwlObjectProperty(destinationId));
	}

	public OWLSubDataPropertyOfAxiom createOwlSubDataPropertyOfAxiom(Long dataAttributeId, long destinationId) {
		return factory.getOWLSubDataPropertyOfAxiom(getOwlDataProperty(dataAttributeId), getOwlDataProperty(destinationId));
	}

	private OWLClassExpression createOwlClassExpression(Long namedConcept, Map<Integer, List<Relationship>> relationships) {
		if (namedConcept != null) {
			return getOwlClass(namedConcept);
		}

		// Process all concept's relationships
		final Set<OWLClassExpression> terms = new HashSet<>();
		Map<Integer, Set<OWLClassExpression>> nonZeroRoleGroups = new TreeMap<>();
		for (List<Relationship> relationshipList : relationships.values()) {
			for (Relationship relationship : relationshipList) {
				int group = relationship.getGroup();
				long typeId = relationship.getTypeId();
				long destinationId = relationship.getDestinationId();
				ConcreteValue value = relationship.getValue();
				if (destinationId != -1 && value == null) {
					processRelationshipsWithDestinationId(group, typeId, destinationId, terms, nonZeroRoleGroups);
				} else if (value != null && destinationId == -1) {
					processRelationshipsWithConcreteValue(group, typeId, value, terms, nonZeroRoleGroups);
				} else {
					throw new IllegalArgumentException("Relationship must not have destination id and concrete value at the same time but got both " + relationship.toString());
				}
			}
		}

		// For each role group if there is more than one statement in the group we wrap them in an ObjectIntersectionOf statement
		for (Integer group : nonZeroRoleGroups.keySet()) {
			Set<OWLClassExpression> expressionGroup = nonZeroRoleGroups.get(group);
			// Write out a group of expressions
			terms.add(getOwlObjectSomeValuesFromGroup(getOnlyValueOrIntersection(expressionGroup)));
		}

		if (terms.isEmpty()) {
			// SNOMED CT root concept
			terms.add(factory.getOWLThing());
		}

		return getOnlyValueOrIntersection(terms);
	}

	private void processRelationshipsWithConcreteValue(int group, long typeId, ConcreteValue value, Set<OWLClassExpression> terms, Map<Integer, Set<OWLClassExpression>> nonZeroRoleGroups) {
		if (group == 0) {
			if (ungroupedAttributes.contains(typeId)) {
				// Special cases
				terms.add(getOwlDataHasValue(typeId, value));
			} else {
				// Self grouped relationships in group 0
				terms.add(getOwlObjectSomeValuesFromGroup(getOwlDataHasValue(typeId, value)));
			}
		} else if (ungroupedAttributes.contains(typeId)) {
			// Prevent MRCM ungrouped attribute from being grouped, even though a group other than 0 was given
			terms.add(getOwlDataHasValue(typeId, value));
		} else {
			// Collect statements in the same role group into sets
			nonZeroRoleGroups.computeIfAbsent(group, g -> new HashSet<>())
					.add(getOwlDataHasValue(typeId, value));
		}
	}

	private void processRelationshipsWithDestinationId(int group, long typeId, long destinationId, Set<OWLClassExpression> terms, Map<Integer, Set<OWLClassExpression>> nonZeroRoleGroups) {
		if (typeId == Concepts.IS_A_LONG) {
			terms.add(getOwlClass(destinationId));
		} else if (group == 0) {
			if (ungroupedAttributes.contains(typeId)) {
				// Special cases
				terms.add(getOwlObjectSomeValuesFrom(typeId, destinationId));
			} else {
				// Self grouped relationships in group 0
				terms.add(getOwlObjectSomeValuesFromGroup(getOwlObjectSomeValuesFrom(typeId, destinationId)));
			}
		} else if (ungroupedAttributes.contains(typeId)) {
			// Prevent MRCM ungrouped attribute from being grouped, even though a group other than 0 was given
			terms.add(getOwlObjectSomeValuesFrom(typeId, destinationId));
		} else {
			// Collect statements in the same role group into sets
			nonZeroRoleGroups.computeIfAbsent(group, g -> new HashSet<>())
					.add(getOwlObjectSomeValuesFrom(typeId, destinationId));
		}
	}

	public Set<PropertyChain> getPropertyChains(OWLOntology owlOntology) {
		Set<PropertyChain> propertyChains = new HashSet<>();

		// Collect property chain axioms
		for (OWLSubPropertyChainOfAxiom propertyChainAxiom : owlOntology.getAxioms(AxiomType.SUB_PROPERTY_CHAIN_OF)) {
			List<OWLObjectPropertyExpression> propertyChain = propertyChainAxiom.getPropertyChain();
			assertTrue("Property chain must be 2 properties long.", propertyChain.size() == 2);
			Long sourceType = getShortForm(propertyChain.get(0));
			Long destinationType = getShortForm(propertyChain.get(1));
			OWLObjectPropertyExpression superProperty = propertyChainAxiom.getSuperProperty();
			Long inferredType = getShortForm(superProperty);
			propertyChains.add(new PropertyChain(sourceType, destinationType, inferredType));
		}

		// Build property chains from transitive properties
		for (OWLTransitiveObjectPropertyAxiom transitiveObjectPropertyAxiom : owlOntology.getAxioms(AxiomType.TRANSITIVE_OBJECT_PROPERTY)) {
			Long propertyId = getShortForm(transitiveObjectPropertyAxiom.getProperty());
			propertyChains.add(new PropertyChain(propertyId, propertyId, propertyId));
		}

		return propertyChains;
	}

	private Long getShortForm(OWLObjectPropertyExpression property) {
		String shortForm = property.getNamedProperty().getIRI().getShortForm();
		return parseLong(shortForm);
	}

	private OWLClassExpression getOnlyValueOrIntersection(Set<OWLClassExpression> terms) {
		return terms.size() == 1 ? terms.iterator().next() : factory.getOWLObjectIntersectionOf(terms);
	}

	private OWLObjectSomeValuesFrom getOwlObjectSomeValuesFromGroup(OWLClassExpression owlObjectSomeValuesFrom) {
		return getOwlObjectSomeValuesWithPrefix(SNOMED_ROLE_GROUP_SHORT_URI, owlObjectSomeValuesFrom);
	}

	private OWLObjectSomeValuesFrom getOwlObjectSomeValuesWithPrefix(String prefix, OWLClassExpression owlObjectSomeValuesFrom) {
		return factory.getOWLObjectSomeValuesFrom(factory.getOWLObjectProperty(prefix, prefixManager), owlObjectSomeValuesFrom);
	}

	private OWLObjectSomeValuesFrom getOwlObjectSomeValuesFrom(long typeId, long destinationId) {
		return factory.getOWLObjectSomeValuesFrom(getOwlObjectProperty(typeId), getOwlClass(destinationId));
	}

	private OWLObjectProperty getOwlObjectProperty(long typeId) {
		return factory.getOWLObjectProperty(COLON + typeId, prefixManager);
	}

	private OWLDataProperty getOwlDataProperty(long typeId) {
		return factory.getOWLDataProperty(COLON + typeId, prefixManager);
	}

	private OWLClass getOwlClass(Long conceptId) {
		return factory.getOWLClass(COLON + conceptId, prefixManager);
	}

	private OWLDataHasValue getOwlDataHasValue(long typeId, ConcreteValue value) {
		OWLLiteral owlLiteral;
		if (value.isInteger()) {
			owlLiteral = factory.getOWLLiteral(value.asInt());
		} else if (value.isFloat()) {
			owlLiteral = factory.getOWLLiteral(value.asFloat());
		} else if (value.isDouble()) {
			owlLiteral = factory.getOWLLiteral(value.asDouble());
		} else if (value.isBoolean()) {
			owlLiteral = factory.getOWLLiteral(value.asBoolean());
		} else if (value.isString()) {
			owlLiteral = factory.getOWLLiteral(value.asString());
		} else if (value.isDecimal()) {
			owlLiteral = factory.getOWLLiteral(value.asString(), OWL2Datatype.XSD_DECIMAL);
		} else {
			throw new UnsupportedOperationException("Type is not supported yet." + value.getType());
		}
		return factory.getOWLDataHasValue(getOwlDataProperty(typeId), owlLiteral);
	}

	private void addDescriptionAnnotations(Long conceptId, SnomedTaxonomy snomedTaxonomy, Set<OWLAxiom> axioms, Map<Long, String> langRefsetToDialectMap) {
		for (Description description : snomedTaxonomy.getConceptDescriptions(conceptId)) {
			String typeId = description.getTypeId();
			String term = description.getTerm();
			Map<Long, Long> acceptabilityMap = description.getAcceptabilityMap();
			String languageAndDialect = getLanguageDialect(langRefsetToDialectMap, description.getLanguageCode(), acceptabilityMap, typeId);

			if (Concepts.FSN.equals(typeId)) {
				// Add FSN as "rdfs:label"
				axioms.add(factory.getOWLAnnotationAssertionAxiom(
						factory.getRDFSLabel(),
						IRI.create(SNOMED_CORE_COMPONENTS_URI + conceptId),
						factory.getOWLLiteral(term, languageAndDialect)));
			} else {
				// Use SKOS for other descriptions:
				String labelUri = null;
				if (Concepts.SYNONYM.equals(typeId)) {
					if (acceptabilityMap.values().contains(Concepts.PREFERRED_LONG)) {
						// Add preferred synonym as "skos:prefLabel"
						labelUri = SKOS_PREF_LABEL_URI;
					} else {
						// Add other synonym as "skos:altLabel"
						labelUri = SKOS_ALT_LABEL_URI;
					}
				} else if (Concepts.DEFINITION.equals(typeId)) {
					// Add Text Definition as "skos:definition"
					labelUri = SKOS_DEFINITION_LABEL_URI;
				}

				if (labelUri != null) {
					axioms.add(factory.getOWLAnnotationAssertionAxiom(
							factory.getOWLAnnotationProperty(IRI.create(labelUri)),
							IRI.create(SNOMED_CORE_COMPONENTS_URI + conceptId),
							factory.getOWLLiteral(term, languageAndDialect)));
				}
			}
		}
	}

	private String getLanguageDialect(Map<Long, String> langRefsetToDialectMap, String language, Map<Long, Long> acceptabilityMap, String typeId) {
		// If term is preferred/acceptable in multiple dialects we will not add the dialect to the language-dialect string.
		List<Map.Entry<Long, Long>> preferredLangRefsets = acceptabilityMap.entrySet().stream()
				.filter((entry) -> Concepts.PREFERRED_LONG.equals(entry.getValue())).collect(Collectors.toList());
		if (preferredLangRefsets.isEmpty() && typeId.equals(Concepts.SYNONYM)) {
			// Not a preferred synonym, let's collect acceptable entries
			preferredLangRefsets = acceptabilityMap.entrySet().stream()
					.filter((entry) -> Concepts.ACCEPTABLE_LONG.equals(entry.getValue())).collect(Collectors.toList());
		}
		if (preferredLangRefsets.size() == 1) {
			Long preferredInLanRefset = preferredLangRefsets.get(0).getKey();
			String dialect = langRefsetToDialectMap.get(preferredInLanRefset);
			if (dialect != null) {
				if (!dialect.isEmpty()) {// Some language reference sets do not require a dialect
					language += "-" + dialect;
				}
			} else {
				if (missingDialectWarnings.incrementAndGet() < 50) {
					Logger logger = LoggerFactory.getLogger(getClass());
					logger.warn("Please add language reference set {} to {} and recompile. " +
							"The dialect ISO code could not be appended to the annotations because it was not found.", preferredInLanRefset, LANGUAGE_REFSET_DIALECT_MAP_PROPERTIES);
				}
			}
		}
		return language;
	}

	private void assertTrue(String message, boolean bool) {
		if (!bool) {
			throw new ReasonerServiceRuntimeException(message);
		}
	}

	private Map<Long, String> loadLanguageRefsetToDialectMap() throws OWLOntologyCreationException {
		Map<Long, String> map = new Long2ObjectOpenHashMap<>();
		try {
			Properties properties = new Properties();
			properties.load(OntologyService.class.getResourceAsStream("/" + LANGUAGE_REFSET_DIALECT_MAP_PROPERTIES));
			for (String key : properties.stringPropertyNames()) {
				map.put(parseLong(key), properties.getProperty(key));
			}
		} catch (IOException e) {
			throw new OWLOntologyCreationException("Failed to load map of language reference set to dialect code.", e);
		}
		return map;
	}

}
