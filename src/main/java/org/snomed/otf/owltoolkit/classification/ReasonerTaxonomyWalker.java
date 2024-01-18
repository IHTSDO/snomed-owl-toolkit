/*
 * Copyright 2018 SNOMED International, http://snomed.org
 * Copyright 2011-2017 B2i Healthcare Pte Ltd, http://b2i.sg
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
package org.snomed.otf.owltoolkit.classification;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNodeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.ontology.OntologyHelper;

import java.util.*;
import java.util.stream.Collectors;

public class ReasonerTaxonomyWalker {

	private static final NodeSet<OWLClass> EMPTY_NODE_SET = new OWLClassNodeSet();

	private final OWLReasoner reasoner;

	private final ReasonerTaxonomy taxonomy;
	private final OWLOntology owlOntology;

	private Set<Long> processedConceptIds;

	private boolean nothingProcessed;

	private static final Logger LOGGER = LoggerFactory.getLogger(ReasonerTaxonomyWalker.class);

	public ReasonerTaxonomyWalker(final OWLReasoner reasoner, final ReasonerTaxonomy changeSet) {
		this.reasoner = reasoner;
		this.owlOntology = reasoner.getRootOntology();
		this.taxonomy = changeSet;
		this.processedConceptIds = new LongOpenHashSet(600000);
	}

	public ReasonerTaxonomy walk() {
		LOGGER.info(">>> SnomedTaxonomy extraction");

		extractProperties();

		// Now process the concepts
		final Deque<Node<OWLClass>> nodesToProcess = new LinkedList<>();
		nodesToProcess.add(reasoner.getTopClassNode());

		// Breadth-first walk through the class hierarchy
		while (!nodesToProcess.isEmpty()) {

			final Node<OWLClass> currentNode = nodesToProcess.removeFirst();
			final NodeSet<OWLClass> nextNodeSet = walkClasses(currentNode);

			if (!nextNodeSet.isEmpty()) {
				nodesToProcess.addAll(nextNodeSet.getNodes());
			}

		}

		processedConceptIds.clear();
		processedConceptIds = null;

		// Move attribute ids to after 'Concept model attribute' concept so they are processed in the correct order.
		List<Long> attributeIds = taxonomy.getAttributeIds();
		attributeIds.remove(Concepts.CONCEPT_MODEL_ATTRIBUTE_LONG);
		List<Long> conceptIds = taxonomy.getConceptIds();
		conceptIds.removeAll(attributeIds);
		conceptIds.addAll(conceptIds.indexOf(Concepts.CONCEPT_MODEL_ATTRIBUTE_LONG) + 1, attributeIds);

		LOGGER.info("<<< taxonomy extraction");
		return taxonomy;
	}

	private void extractProperties() {
		// Some reasoners (ELK v0.4.3) do not support extracting the property hierarchy so we extract them from the stated OWL Ontology

		// Extract of object properties
		OWLObjectProperty topLevelObjectProperty = null;
		// Find top object property
		for (OWLObjectProperty objectProperty : owlOntology.getObjectPropertiesInSignature()) {
			long propertyId = OntologyHelper.getConceptId(objectProperty);
			if (Concepts.CONCEPT_MODEL_OBJECT_ATTRIBUTE_LONG.equals(propertyId)) {
				topLevelObjectProperty = objectProperty;
				break;
			}

			// The 'Concept model *object* attribute' was not present before Jan 2018 so fall back to the 'Concept model attribute' for object properties.
			if (Concepts.CONCEPT_MODEL_ATTRIBUTE_LONG.equals(propertyId)) {
				topLevelObjectProperty = objectProperty;
			}
		}
		walkObjectProperties(topLevelObjectProperty);

		// Extract of data properties
		// Find top data property
		OWLDataProperty topDataProperty = null;
		for (OWLDataProperty dataProperty : owlOntology.getDataPropertiesInSignature()) {
			long propertyId = OntologyHelper.getConceptId(dataProperty);
			if (Concepts.CONCEPT_MODEL_DATA_ATTRIBUTE_LONG.equals(propertyId)) {
				topDataProperty = dataProperty;
				break;
			}
		}
		if (topDataProperty != null) {
			walkDataProperties(topDataProperty);
		}

		// Extract of Annotation properties
		// Find top Annotation property
		OWLAnnotationProperty topAnnotationProperty = null;
		for (OWLAnnotationProperty annotationProperty : owlOntology.getAnnotationPropertiesInSignature()) {
			long propertyId = OntologyHelper.getConceptId(annotationProperty);
			if (Concepts.CONCEPT_ANNOTATION_ATTRIBUTE_LONG.equals(propertyId)) {
				topAnnotationProperty = annotationProperty;
				break;
			}
		}
		if (topAnnotationProperty != null) {
			walkAnnotationProperties(topAnnotationProperty, owlOntology.getAxioms(AxiomType.SUB_ANNOTATION_PROPERTY_OF));
		}

		// The properties extracted are not concepts so we clear them from the list
		taxonomy.getAttributeIds().addAll(taxonomy.getConceptIds());
		taxonomy.getConceptIds().clear();
	}

	private void walkAnnotationProperties(OWLAnnotationProperty annotationProperty, Set<OWLSubAnnotationPropertyOfAxiom> axioms) {
		long propertyId = OntologyHelper.getConceptId(annotationProperty);
		for (OWLSubAnnotationPropertyOfAxiom axiom : axioms) {
			long superPropertyId = OntologyHelper.getConceptId(axiom.getSuperProperty());
			if (superPropertyId == propertyId) {
				Set<Long> parentIds = new HashSet<>();
				parentIds.add(superPropertyId);
				taxonomy.addEntry(new ReasonerTaxonomyEntry(OntologyHelper.getConceptId(axiom.getSubProperty()), parentIds));
			}
		}
	}

	private void walkObjectProperties(OWLObjectProperty objectProperty) {
		long propertyId = OntologyHelper.getConceptId(objectProperty);
		Set<OWLSubObjectPropertyOfAxiom> superProperties = owlOntology.getObjectSubPropertyAxiomsForSubProperty(objectProperty);
		Set<Long> parentIds = superProperties.stream()
				.map(axiom -> axiom.getSuperProperty().getNamedProperty()).map(OntologyHelper::getConceptId).collect(Collectors.toSet());
		taxonomy.addEntry(new ReasonerTaxonomyEntry(propertyId, parentIds));

		Set<OWLSubObjectPropertyOfAxiom> subProperties = owlOntology.getObjectSubPropertyAxiomsForSuperProperty(objectProperty);
		for (OWLSubObjectPropertyOfAxiom subProperty : subProperties) {
			walkObjectProperties(subProperty.getSubProperty().getNamedProperty());
		}
	}

	private void walkDataProperties(OWLDataProperty topDataProperty) {
		long propertyId = OntologyHelper.getConceptId(topDataProperty);
		Set<OWLSubDataPropertyOfAxiom> superProperties = owlOntology.getDataSubPropertyAxiomsForSubProperty(topDataProperty);
		Set<Long> parentIds = superProperties.stream()
				.map(axiom -> axiom.getSuperProperty().asOWLDataProperty()).map(OntologyHelper::getConceptId).collect(Collectors.toSet());
		taxonomy.addEntry(new ReasonerTaxonomyEntry(propertyId, parentIds));

		Set<OWLSubDataPropertyOfAxiom> subProperties = owlOntology.getDataSubPropertyAxiomsForSuperProperty(topDataProperty);
		for (OWLSubDataPropertyOfAxiom subProperty : subProperties) {
			walkDataProperties(subProperty.getSubProperty().asOWLDataProperty());
		}
	}

	private NodeSet<OWLClass> walkClasses(final Node<OWLClass> node) {

		if (isNodeProcessed(node)) {
			return reasoner.getSubClasses(node.getRepresentativeElement(), true);
		}

		// Check first if we are at the bottom node, as all OWL classes are superclasses of Nothing
		final boolean unsatisfiable = node.isBottomNode();
		final Set<Long> conceptIds = new LongOpenHashSet();
		final long representativeConceptId = getConceptIds(node, conceptIds);

		if (unsatisfiable) {
			registerEquivalentConceptIds(conceptIds, true);
			processedConceptIds.addAll(conceptIds);
			return EMPTY_NODE_SET;
		}

		// Check if all parents have already been visited earlier
		final NodeSet<OWLClass> parentNodeSet = reasoner.getSuperClasses(node.getRepresentativeElement(), true);

		for (final Node<OWLClass> parentNode : parentNodeSet) {

			if (!isNodeProcessed(parentNode)) {
				return EMPTY_NODE_SET;
			}
		}

		if (conceptIds.size() > 1) {
			registerEquivalentConceptIds(conceptIds, false);
		}

		final Set<Long> parentConceptIds = new LongOpenHashSet();

		for (final Node<OWLClass> parentNode : parentNodeSet) {

			// No parents if we found the Top node
			if (parentNode.isTopNode()) {
				break;
			}

			final long parentConceptId = getConceptIds(parentNode, new LongOpenHashSet());
			parentConceptIds.add(parentConceptId);
		}

		registerParentConceptIds(representativeConceptId, parentConceptIds);

		processedConceptIds.addAll(conceptIds);

		conceptIds.remove(representativeConceptId);
		parentConceptIds.clear();
		parentConceptIds.add(representativeConceptId);

		for (Long conceptId : conceptIds) {
			registerParentConceptIds(conceptId, parentConceptIds);
		}

		return computeNextNodeSet(node);
	}

	private NodeSet<OWLClass> computeNextNodeSet(final Node<OWLClass> node) {
		final NodeSet<OWLClass> subClasses = reasoner.getSubClasses(node.getRepresentativeElement(), true);

		if (!subClasses.isBottomSingleton()) {
			return subClasses;
		}

		if (nothingProcessed) {
			return EMPTY_NODE_SET;
		} else {
			nothingProcessed = true;
			return subClasses;
		}
	}

	private void registerParentConceptIds(final long child, final Set<Long> parents) {
		taxonomy.addEntry(new ReasonerTaxonomyEntry(child, parents));
	}

	private boolean isNodeProcessed(final Node<OWLClass> node) {
		for (final OWLClass owlClass : node) {
			if (!OntologyHelper.isConceptClass(owlClass)) {
				continue;
			}

			final long storageKey = OntologyHelper.getConceptId(owlClass);
			if (!processedConceptIds.contains(storageKey)) {
				return false;
			}
		}

		return true;
	}

	private long getConceptIds(final Node<OWLClass> node, final Set<Long> conceptIds) {
		for (final OWLClass owlClass : node) {
			if (!OntologyHelper.isConceptClass(owlClass)) {
				continue;
			}

			final long conceptId = OntologyHelper.getConceptId(owlClass);
			conceptIds.add(conceptId);
		}

		return conceptIds.iterator().next();
	}

	private void registerEquivalentConceptIds(final Set<Long> conceptIds, final boolean unsatisfiable) {
		if (unsatisfiable) {
			taxonomy.getUnsatisfiableConceptIds().addAll(conceptIds);
		} else {
			taxonomy.addEquivalentConceptIds(conceptIds);
		}
	}

}
