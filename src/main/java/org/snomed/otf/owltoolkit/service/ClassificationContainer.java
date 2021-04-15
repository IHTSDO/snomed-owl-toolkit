package org.snomed.otf.owltoolkit.service;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.snomed.otf.owltoolkit.classification.ReasonerTaxonomy;
import org.snomed.otf.owltoolkit.normalform.RelationshipNormalFormGenerator;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;

import java.util.Set;

public class ClassificationContainer {

	private SnomedTaxonomy snomedTaxonomy;
	private Set<Long> ungroupedRoles;
	private OWLReasoner reasoner;
	private ReasonerTaxonomy reasonerTaxonomy;
	private OWLOntologyManager owlOntologyManager;
	private RelationshipNormalFormGenerator normalFormGenerator;

	// TODO: Call ME
	public void dispose() {
		if (reasoner != null) {
			reasoner.dispose();
		}
	}

	public void setSnomedTaxonomy(SnomedTaxonomy snomedTaxonomy) {
		this.snomedTaxonomy = snomedTaxonomy;
	}

	public SnomedTaxonomy getSnomedTaxonomy() {
		return snomedTaxonomy;
	}

	public void setUngroupedRoles(Set<Long> ungroupedRoles) {
		this.ungroupedRoles = ungroupedRoles;
	}

	public Set<Long> getUngroupedRoles() {
		return ungroupedRoles;
	}

	public void setReasoner(OWLReasoner reasoner) {
		this.reasoner = reasoner;
	}

	public OWLReasoner getReasoner() {
		return reasoner;
	}

	public void setReasonerTaxonomy(ReasonerTaxonomy reasonerTaxonomy) {
		this.reasonerTaxonomy = reasonerTaxonomy;
	}

	public ReasonerTaxonomy getReasonerTaxonomy() {
		return reasonerTaxonomy;
	}

	public void setOwlOntologyManager(OWLOntologyManager owlOntologyManager) {
		this.owlOntologyManager = owlOntologyManager;
	}

	public OWLOntologyManager getOwlOntologyManager() {
		return owlOntologyManager;
	}

	public void setNormalFormGenerator(RelationshipNormalFormGenerator normalFormGenerator) {
		this.normalFormGenerator = normalFormGenerator;
	}

	public RelationshipNormalFormGenerator getNormalFormGenerator() {
		return normalFormGenerator;
	}
}
