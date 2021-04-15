package org.snomed.otf.owltoolkit.ontology;

import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.HashSet;
import java.util.Set;

public class AxiomChangeSet {

	private Set<OWLAxiom> removedAxioms = new HashSet<>();
	private Set<OWLAxiom> addedAxioms = new HashSet<>();

	public void removedAxiom(OWLAxiom axiom) {
		removedAxioms.add(axiom);
	}

	public void addedAxioms(Set<OWLAxiom> owlAxioms) {
		addedAxioms.addAll(owlAxioms);
	}

	public Set<OWLAxiom> getRemovedAxioms() {
		return removedAxioms;
	}

	public Set<OWLAxiom> getAddedAxioms() {
		return addedAxioms;
	}
}
