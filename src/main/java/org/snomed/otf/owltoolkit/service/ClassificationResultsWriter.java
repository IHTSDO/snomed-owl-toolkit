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
package org.snomed.otf.owltoolkit.service;

import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.domain.Relationship;
import org.snomed.otf.owltoolkit.normalform.RelationshipChangeProcessor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class ClassificationResultsWriter {

	private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	private static final String RELATIONSHIPS_HEADER = "id\teffectiveTime\tactive\tmoduleId\tsourceId\tdestinationId\trelationshipGroup\ttypeId\tcharacteristicTypeId\tmodifierId";
	private static final String EQUIVALENT_REFSET_HEADER = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tmapTarget";
	private static final String TAB = "\t";

	void writeResultsRf2Archive(
			RelationshipChangeProcessor changeCollector,
			List<Set<Long>> equivalentConceptIdSets,
			OutputStream resultsOutputStream,
			Date startDate) throws ReasonerServiceException {

		try {
			try (ZipOutputStream zipOutputStream = new ZipOutputStream(resultsOutputStream, UTF_8_CHARSET);
				 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zipOutputStream))) {

				String formattedDate = DATE_FORMAT.format(startDate);
				zipOutputStream.putNextEntry(new ZipEntry(String.format("RF2/sct2_Relationship_Delta_Classification_%s.txt", formattedDate)));
				writeRelationshipChanges(writer, changeCollector.getAddedStatements(), changeCollector.getRemovedStatements());

				zipOutputStream.putNextEntry(new ZipEntry(String.format("RF2/der2_sRefset_EquivalentConceptSimpleMapDelta_Classification_%s.txt", formattedDate)));
				writeEquivalentConcepts(writer, equivalentConceptIdSets);
			}
		} catch (IOException e) {
			throw new ReasonerServiceException("Failed to write out results archive.", e);
		}
	}

	private void writeRelationshipChanges(BufferedWriter writer, Map<Long, Set<Relationship>> addedStatements, Map<Long, Set<Relationship>> removedStatements) throws IOException {
		// Write header
		writer.write(RELATIONSHIPS_HEADER);
		writer.newLine();

		// Write newly inferred relationships
		for (Long sourceId : addedStatements.keySet()) {
			String active = "1";
			for (Relationship relationship : addedStatements.get(sourceId)) {
				writeRelationship(writer,
						relationship.getRelationshipId() == -1 ? "" : relationship.getRelationshipId() + "",
						active,
						sourceId,
						relationship.getDestinationId(),
						relationship.getGroup(),
						relationship.getTypeId(),
						Concepts.EXISTENTIAL_RESTRICTION_MODIFIER);
			}
		}

		// Write redundant relationships
		for (Long sourceId : removedStatements.keySet()) {
			String active = "0";
			for (Relationship relationship : removedStatements.get(sourceId)) {
				writeRelationship(writer,
						relationship.getRelationshipId() + "",
						active,
						sourceId,
						relationship.getDestinationId(),
						relationship.getGroup(),
						relationship.getTypeId(),
						Concepts.EXISTENTIAL_RESTRICTION_MODIFIER);
			}
		}

		writer.flush();
	}

	private void writeEquivalentConcepts(BufferedWriter writer, List<Set<Long>> equivalentConceptIdSets) throws IOException {
		// Write header
		writer.write(EQUIVALENT_REFSET_HEADER);
		writer.newLine();

		// Write sets of equivalentConcepts
		for (Set<Long> equivalentConceptIdSet : equivalentConceptIdSets) {
			String setId = UUID.randomUUID().toString();

			for (Long conceptId : equivalentConceptIdSet) {
				// random member id
				writer.write(UUID.randomUUID().toString());
				writer.write(TAB);

				// no effectiveTime
				writer.write(TAB);

				// active
				writer.write("1");
				writer.write(TAB);

				// no moduleId
				writer.write(TAB);

				// no refsetId
				writer.write(TAB);

				// referencedComponentId is one of the concepts in the set
				writer.write(conceptId.toString());
				writer.write(TAB);

				// mapTarget is the unique id for the set
				writer.write(setId);
				writer.newLine();
			}
		}

		writer.flush();
	}

	private void writeRelationship(BufferedWriter writer, String relationshipId, String active, Long sourceId, Long destinationId, Integer group, Long typeId, String existentialRestrictionModifier) throws IOException {
		writer.write(relationshipId);
		writer.write(TAB);

		// No effectiveTime
		writer.write(TAB);

		// active
		writer.write(active);
		writer.write(TAB);

		// No module
		writer.write(TAB);

		// sourceId
		writer.write(sourceId.toString());
		writer.write(TAB);

		// destinationId
		writer.write(destinationId.toString());
		writer.write(TAB);

		// relationshipGroup
		writer.write(group.toString());
		writer.write(TAB);

		// typeId
		writer.write(typeId.toString());
		writer.write(TAB);

		// characteristicTypeId
		writer.write(Concepts.INFERRED_RELATIONSHIP);
		writer.write(TAB);

		// modifierId always existential at this time
		writer.write(existentialRestrictionModifier);
		writer.newLine();
	}

}
