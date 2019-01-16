package org.snomed.otf.owltoolkit.normalform;

import java.util.Set;

import org.snomed.otf.owltoolkit.domain.Relationship;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;

public class RelationshipInactivationProcessor {
	
	private SnomedTaxonomy snomedTaxonomy;
	
	public RelationshipInactivationProcessor(SnomedTaxonomy snomedTaxonomy) {
		this.snomedTaxonomy = snomedTaxonomy;
	}
	
	public void processInactivationChanges(RelationshipChangeProcessor changeCollector) {
		Set<Long> inactiveConcepts = snomedTaxonomy.getInactivatedConcepts();
		for (Long inactive : inactiveConcepts) {
			for (Relationship rel : snomedTaxonomy.getInferredRelationships(inactive)) {
				changeCollector.handleRemovedSubject(inactive, rel);
			}
		}
	}
}
