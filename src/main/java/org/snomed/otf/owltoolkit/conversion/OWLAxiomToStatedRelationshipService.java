package org.snomed.otf.owltoolkit.conversion;

import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.constants.RF2Headers;
import org.snomed.otf.owltoolkit.domain.AxiomRepresentation;
import org.snomed.otf.owltoolkit.domain.Relationship;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomyBuilder;
import org.snomed.otf.owltoolkit.util.InputStreamSet;

import java.io.*;
import java.util.*;

/**
 * WARNING: Converting Axioms to Stated Relationships will result in a loss of semantic information.
 * The stated relationships will not classify correctly.
 * This class is to create a stated file for statistical purposes only.
 *
 * SCTIDs use just an incremented number no checksum.
 * Only active axioms can be converted.
 */
public class OWLAxiomToStatedRelationshipService {

	private static final String TAB = "\t";

	public void convertAxiomsToStatedRelationships(InputStreamSet snapshotFiles, InputStream deltaFile, OutputStream statedRelationshipOutputStream) throws ReleaseImportException, ConversionException, IOException {
		SnomedTaxonomy snomedTaxonomy = new SnomedTaxonomyBuilder().build(snapshotFiles, deltaFile, false);

		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(statedRelationshipOutputStream))) {

			// Write out RF2 header
			writer.write(RF2Headers.RELATIONSHIP_HEADER);
			writer.newLine();

			Long dummyRelationshipId = 1_000_000L;

			AxiomRelationshipConversionService conversionService = new AxiomRelationshipConversionService(Collections.emptySet());
			Map<Long, Set<OWLAxiom>> conceptAxiomMap = snomedTaxonomy.getConceptAxiomMap();
			for (Long conceptId : conceptAxiomMap.keySet()) {
				Map<Integer, List<Relationship>> conceptRelationships = new HashMap<>();

				for (OWLAxiom axiom : conceptAxiomMap.get(conceptId)) {
					AxiomRepresentation axiomRepresentation = conversionService.convertAxiomToRelationships(axiom);
					if (axiomRepresentation != null) {// Property behaviours and chains will come back null.
						if (conceptId.equals(axiomRepresentation.getLeftHandSideNamedConcept())) {// Check axiom is not a GCI, these can't be represented as stated relationships.
							Map<Integer, List<Relationship>> rightHandSideRelationships = axiomRepresentation.getRightHandSideRelationships();
							// Bump group ids up to keep groups from different axioms separate.
							int groupId = conceptRelationships.isEmpty() ? 1 : conceptRelationships.keySet().stream().max(Integer::compareTo).get() + 1;
							for (Integer group : rightHandSideRelationships.keySet()) {
								if (group == 0) {
									// Merge group 0
									conceptRelationships.computeIfAbsent(0, k -> new ArrayList<>()).addAll(rightHandSideRelationships.get(group));
								} else {
									conceptRelationships.put(groupId, rightHandSideRelationships.get(group));
									groupId++;
								}
							}
						}
					}
				}
				for (Integer groupNumber : conceptRelationships.keySet()) {
					for (Relationship relationship : conceptRelationships.get(groupNumber)) {
						// id	effectiveTime	active	moduleId	sourceId	destinationId	relationshipGroup	typeId	characteristicTypeId	modifierId

						// id
						writer.write(dummyRelationshipId.toString());
						writer.write("020");
						writer.write(TAB);

						// effectiveTime
						writer.write(TAB);

						// active
						writer.write("1");// Only active axioms can be converted.
						writer.write(TAB);

						// moduleId
						writer.write(snomedTaxonomy.getConceptModuleMap().get(conceptId) + "");
						writer.write(TAB);

						// sourceId
						writer.write(conceptId + "");
						writer.write(TAB);

						// destinationId
						writer.write(relationship.getDestinationId() + "");
						writer.write(TAB);

						// relationshipGroup
						writer.write(groupNumber.toString());
						writer.write(TAB);

						// typeId
						writer.write(relationship.getTypeId() + "");
						writer.write(TAB);

						// characteristicTypeId
						writer.write(Concepts.STATED_RELATIONSHIP);
						writer.write(TAB);

						// modifierId
						writer.write(Concepts.EXISTENTIAL_RESTRICTION_MODIFIER);
						writer.write(TAB);

						writer.newLine();
					}
				}
			}
		}
	}

}
