package org.snomed.otf.owltoolkit.ontology;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedObject;

public class OntologyHelper {

	public static boolean isConceptClass(final OWLClass owlClass) {
		return owlClass.getIRI().toString().startsWith(OntologyService.SNOMED_CORE_COMPONENTS_URI);
	}

	public static boolean isNamedConcept(final OWLNamedObject owlNamedObject) {
		return owlNamedObject.getIRI().toString().contains(OntologyService.SNOMED_CORE_COMPONENTS_URI);
	}

	public static long getConceptId(final OWLNamedObject owlNamedObject) {
		return Long.parseLong(owlNamedObject.getIRI().toString().substring(OntologyService.SNOMED_CORE_COMPONENTS_URI.length()));
	}
}
