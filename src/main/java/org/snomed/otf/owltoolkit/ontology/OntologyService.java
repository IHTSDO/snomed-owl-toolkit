/*
 * Copyright 2017 SNOMED International, http://snomed.org
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
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.domain.AxiomRepresentation;
import org.snomed.otf.owltoolkit.domain.Relationship;
import org.snomed.otf.owltoolkit.ontology.render.SnomedFunctionalSyntaxDocumentFormat;
import org.snomed.otf.owltoolkit.ontology.render.SnomedFunctionalSyntaxStorerFactory;
import org.snomed.otf.owltoolkit.ontology.render.SnomedPrefixManager;
import org.snomed.otf.owltoolkit.service.ReasonerServiceRuntimeException;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.OutputStream;
import java.util.*;

import static java.lang.Long.parseLong;

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

	private final OWLOntologyManager manager;
	private OWLDataFactory factory;
	private DefaultPrefixManager prefixManager;
	private final Set<Long> ungroupedAttributes;

	public OntologyService(Set<Long> ungroupedAttributes) {
		this.ungroupedAttributes = ungroupedAttributes;
		manager = OWLManager.createOWLOntologyManager();
		factory = new OWLDataFactoryImpl();
		prefixManager = new DefaultPrefixManager();
		prefixManager.setDefaultPrefix(SNOMED_CORE_COMPONENTS_URI);
	}

	public OWLOntology createOntology(SnomedTaxonomy snomedTaxonomy) throws OWLOntologyCreationException {
		return createOntology(snomedTaxonomy, null, null);
	}

	public OWLOntology createOntology(SnomedTaxonomy snomedTaxonomy, String ontologyUri, String versionDate) throws OWLOntologyCreationException {

		Map<Long, Set<OWLAxiom>> axiomsFromStatedRelationships = createAxiomsFromStatedRelationships(snomedTaxonomy);

		Set<OWLAxiom> axioms = new HashSet<>();
		for (Long conceptId : snomedTaxonomy.getAllConceptIds()) {

			// Add raw axioms from the axiom reference set file
			axioms.addAll(snomedTaxonomy.getConceptAxiomMap().getOrDefault(conceptId, Collections.emptySet()));

			// Add axioms generated from stated relationships
			axioms.addAll(axiomsFromStatedRelationships.getOrDefault(conceptId, Collections.emptySet()));

			// Add FSN annotation
			addFSNAnnotation(conceptId, snomedTaxonomy, axioms);
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

	public Map<Long, Set<OWLAxiom>> createAxiomsFromStatedRelationships(SnomedTaxonomy snomedTaxonomy) {
		Map<Long, Set<OWLAxiom>> axiomsMap = new Long2ObjectOpenHashMap<>();

		// Create axioms of concept model attributes
		// The Concept Model Object Attribute concept did not always exist - use the parent if it doesn't exist
		boolean conceptModelObjectAttributePresent = snomedTaxonomy.getAllConceptIds().contains(Concepts.CONCEPT_MODEL_OBJECT_ATTRIBUTE_LONG);
		Long conceptModelObjectAttribute = conceptModelObjectAttributePresent ?
				Concepts.CONCEPT_MODEL_OBJECT_ATTRIBUTE_LONG : Concepts.CONCEPT_MODEL_ATTRIBUTE_LONG;

		for (Long objectAttributeId : snomedTaxonomy.getDescendants(conceptModelObjectAttribute)) {
			OWLObjectProperty owlObjectProperty = getOwlObjectProperty(objectAttributeId);
			for (Relationship relationship : snomedTaxonomy.getStatedRelationships(objectAttributeId)) {
				if (relationship.getTypeId() == Concepts.IS_A_LONG && relationship.getDestinationId() != Concepts.CONCEPT_MODEL_ATTRIBUTE_LONG) {
					axiomsMap.computeIfAbsent(objectAttributeId, (id) -> new HashSet<>())
							.add(factory.getOWLSubObjectPropertyOfAxiom(owlObjectProperty, getOwlObjectProperty(relationship.getDestinationId())));
				}
			}
		}

		if (snomedTaxonomy.getAllConceptIds().contains(Concepts.CONCEPT_MODEL_DATA_ATTRIBUTE_LONG)) {
			for (Long dataAttributeId : snomedTaxonomy.getDescendants(Concepts.CONCEPT_MODEL_DATA_ATTRIBUTE_LONG)) {
				OWLDataProperty owlDataProperty = getOwlDataProperty(dataAttributeId);
				for (Relationship relationship : snomedTaxonomy.getStatedRelationships(dataAttributeId)) {
					if (relationship.getTypeId() == Concepts.IS_A_LONG) {
						axiomsMap.computeIfAbsent(dataAttributeId, (id) -> new HashSet<>())
								.add(factory.getOWLSubDataPropertyOfAxiom(owlDataProperty, getOwlDataProperty(relationship.getDestinationId())));
					}
				}
			}
		}

		// Create axioms of all other Snomed concepts
		for (Long conceptId : snomedTaxonomy.getAllConceptIds()) {

			// Convert any stated relationships to axioms
			boolean primitive = snomedTaxonomy.isPrimitive(conceptId);
			Collection<Relationship> statedRelationships = snomedTaxonomy.getStatedRelationships(conceptId);

			if (conceptId.equals(Concepts.ROOT_LONG) || !statedRelationships.isEmpty()) {
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
		return owlDocumentFormat;
	}

	public SnomedPrefixManager getSnomedPrefixManager() {
		SnomedPrefixManager prefixManager = new SnomedPrefixManager();
		prefixManager.setDefaultPrefix(SNOMED_CORE_COMPONENTS_URI);
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
				} else {
					// Collect statements in the same role group into sets
					nonZeroRoleGroups.computeIfAbsent(group, g -> new HashSet<>())
							.add(getOwlObjectSomeValuesFrom(typeId, destinationId));
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

	private void addFSNAnnotation(Long conceptId, SnomedTaxonomy snomedTaxonomy, Set<OWLAxiom> axioms) {
		String conceptFsnTerm = snomedTaxonomy.getConceptFsnTerm(conceptId);
		if (conceptFsnTerm != null) {
			axioms.add(factory.getOWLAnnotationAssertionAxiom(factory.getRDFSLabel(), IRI.create(SNOMED_CORE_COMPONENTS_URI + conceptId), factory.getOWLLiteral(conceptFsnTerm)));
		}
	}

	public DefaultPrefixManager getPrefixManager() {
		return prefixManager;
	}

	private void assertTrue(String message, boolean bool) {
		if (!bool) {
			throw new ReasonerServiceRuntimeException(message);
		}
	}
}
