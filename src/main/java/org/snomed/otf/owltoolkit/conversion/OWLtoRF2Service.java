package org.snomed.otf.owltoolkit.conversion;

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
		for (OWLEntity owlEntity : owlOntology.getSignature()) {
			IRI iri = owlEntity.getIRI();
			Long conceptId = getConceptIdFromUri(iri.toString());
			getDescriptions(conceptId, iri, owlOntology);
		}

		for (OWLAxiom axiom : owlOntology.getAxioms()) {
			System.out.println(axiom.getClass());
			System.out.println(axiom.toString());
			if (axiom instanceof OWLSubPropertyChainOfAxiom) {
				OWLSubPropertyChainOfAxiom propertyChainAxiom = (OWLSubPropertyChainOfAxiom) axiom;
				List<OWLObjectPropertyExpression> propertyChain = propertyChainAxiom.getPropertyChain();
				OWLObjectPropertyExpression namedAttribute = propertyChain.get(0);
				addAxiom(getConceptIdFromUri(namedAttribute.getNamedProperty().getIRI().toString()), axiom);
			}
		}


		System.out.println("all axioms:");
		for (Long conceptId : conceptAxioms.keySet()) {
			System.out.println("Concept " + conceptId);
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
				System.out.println(value);
			}
		}
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
