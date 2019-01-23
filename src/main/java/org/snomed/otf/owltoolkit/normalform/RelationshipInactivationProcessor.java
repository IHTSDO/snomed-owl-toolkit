package org.snomed.otf.owltoolkit.normalform;

import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;

import java.util.Set;

public class RelationshipInactivationProcessor {
	
	private SnomedTaxonomy snomedTaxonomy;
	
	public RelationshipInactivationProcessor(SnomedTaxonomy snomedTaxonomy) {
		this.snomedTaxonomy = snomedTaxonomy;
	}
	
	public void processInactivationChanges(RelationshipChangeProcessor changeCollector) {
		Set<Long> inactiveConcepts = snomedTaxonomy.getInactivatedConcepts();
		for (Long inactiveConceptId : inactiveConcepts) {
			changeCollector.processRemovalsDueToInactivation(inactiveConceptId, snomedTaxonomy.getInferredRelationships(inactiveConceptId));
		}
	}
}
