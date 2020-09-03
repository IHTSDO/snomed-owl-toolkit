package org.snomed.otf.owltoolkit.conversion;

import com.google.common.collect.Sets;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class OWLtoRF2Service {

	private Map<Long, String> conceptDescriptions;
	private Map<Long, Set<OWLAxiom>> conceptAxioms;

	public void writeToRF2(InputStream owlFileStream, OutputStream rf2ZipOutputStream) throws OWLException {
		OWLOntology owlOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(owlFileStream);

		conceptDescriptions = new HashMap<>();
		conceptAxioms = new HashMap<>();

		// Gather all descriptions
		for (OWLEntity owlEntity : Sets.union(owlOntology.getObjectPropertiesInSignature(), owlOntology.getClassesInSignature())) {
			IRI iri = owlEntity.getIRI();
			Long conceptId = getConceptIdFromUri(iri.toString());
			getDescriptions(conceptId, iri, owlOntology);
		}

		// Grab all axioms and process by type
		for (OWLAxiom axiom : owlOntology.getAxioms()) {
			if (axiom instanceof OWLObjectPropertyAxiom) {
				OWLObjectProperty namedConcept = axiom.getObjectPropertiesInSignature().iterator().next();
				addAxiom(getConceptIdFromUri(namedConcept.getNamedProperty().getIRI().toString()), axiom);
			} else if (axiom instanceof OWLSubClassOfAxiom) {
				OWLSubClassOfAxiom subClassOfAxiom = (OWLSubClassOfAxiom) axiom;
				OWLClassExpression subClass = subClassOfAxiom.getSubClass();
				if (subClass.isAnonymous()) {
					// Found GCI axiom
					OWLClassExpression superClass = subClassOfAxiom.getSuperClass();
					addAxiom(getFirstConceptIdFromClassList(superClass.getClassesInSignature()), axiom);
				} else {

					addAxiom(getFirstConceptIdFromClassList(subClass.getClassesInSignature()), axiom);
				}
			} else if (axiom instanceof OWLEquivalentClassesAxiom) {
				OWLEquivalentClassesAxiom equivalentClassesAxiom = (OWLEquivalentClassesAxiom) axiom;
				Set<OWLClassExpression> classExpressions = equivalentClassesAxiom.getClassExpressions();
				OWLClass next = (OWLClass) classExpressions.iterator().next();
				addAxiom(getConceptIdFromUri(next.getIRI().toString()), axiom);
			}
		}

		System.out.println("all axioms:");
		for (Long conceptId : conceptAxioms.keySet()) {
			System.out.println("Concept: " + conceptId);
			System.out.println("Description: " + conceptDescriptions.get(conceptId));
			System.out.println("Axioms: ");
			for (OWLAxiom owlAxiom : conceptAxioms.get(conceptId)) {
				System.out.println(owlAxiom.toString());
			}
			System.out.println();
		}

	}

	private void getDescriptions(Long conceptId, IRI iri, OWLOntology owlOntology) {
		Set<OWLAnnotationAssertionAxiom> annotationAssertionAxioms = owlOntology.getAnnotationAssertionAxioms(iri);
		for (OWLAnnotationAssertionAxiom annotationAssertionAxiom : annotationAssertionAxioms) {
			if ("rdfs:label".equals(annotationAssertionAxiom.getProperty().toString())) {
				String value = annotationAssertionAxiom.getValue().toString();
				if (value.startsWith("\"")) {
					value = value.substring(1, value.lastIndexOf("\""));
				}
				conceptDescriptions.put(conceptId, value);
			}
		}
	}

	private Long getFirstConceptIdFromClassList(Set<OWLClass> classesInSignature) {
		return getConceptIdFromUri(classesInSignature.iterator().next().getIRI().toString());
	}

	private long getConceptIdFromUri(String uri) {
		return Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
	}

	private boolean addAxiom(Long conceptId, OWLAxiom axiom) {
		return conceptAxioms.computeIfAbsent(conceptId, (id) -> new HashSet<>()).add(axiom);

	}

	private boolean addAxiom(Long conceptId, Collection<? extends OWLAxiom> axioms) {
		return conceptAxioms.computeIfAbsent(conceptId, (id) -> new HashSet<>()).addAll(axioms);
	}

}
