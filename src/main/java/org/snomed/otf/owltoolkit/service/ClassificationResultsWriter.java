/*
 * Copyright 2020 SNOMED International, http://snomed.org
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
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class ClassificationResultsWriter {

	private static final Charset UTF_8_CHARSET = StandardCharsets.UTF_8;
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	private static final String RELATIONSHIPS_HEADER = "id\teffectiveTime\tactive\tmoduleId\tsourceId\tdestinationId\trelationshipGroup\ttypeId\tcharacteristicTypeId\tmodifierId";
	private static final String CONCRETE_RELATIONSHIPS_HEADER = "id\teffectiveTime\tactive\tmoduleId\tsourceId\tvalue\trelationshipGroup\ttypeId\tcharacteristicTypeId\tmodifierId";
	private static final String EQUIVALENT_REFSET_HEADER = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tmapTarget";
	private static final String TAB = "\t";

	void writeResultsRf2Archive(
			RelationshipChangeProcessor changeCollector,
			OutputStream resultsOutputStream,
			Date startDate) throws ReasonerServiceException {

		try {
			try (ZipOutputStream zipOutputStream = new ZipOutputStream(resultsOutputStream, UTF_8_CHARSET);
				 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zipOutputStream))) {

				String formattedDate = DATE_FORMAT.format(startDate);
				zipOutputStream.putNextEntry(new ZipEntry(String.format("RF2/sct2_Relationship_Delta_Classification_%s.txt", formattedDate)));
				writeRelationshipChanges(false, writer, changeCollector.getAddedStatements(), changeCollector.getRemovedStatements());

				zipOutputStream.putNextEntry(new ZipEntry(String.format("RF2/sct2_RelationshipConcreteValues_Delta_Classification_%s.txt", formattedDate)));
				writeRelationshipChanges(true, writer, changeCollector.getAddedStatements(), changeCollector.getRemovedStatements());

				zipOutputStream.putNextEntry(new ZipEntry(String.format("RF2/der2_sRefset_EquivalentConceptSimpleMapDelta_Classification_%s.txt", formattedDate)));
				writeEquivalentConcepts(writer, changeCollector.getEquivalentConceptIds());
			}
		} catch (IOException e) {
			throw new ReasonerServiceException("Failed to write out results archive.", e);
		}
	}

	private void writeRelationshipChanges(boolean concrete, BufferedWriter writer, Map<Long, Set<Relationship>> addedStatements, Map<Long, Set<Relationship>> removedStatements) throws IOException {
		// Write header
		writer.write(concrete ? CONCRETE_RELATIONSHIPS_HEADER : RELATIONSHIPS_HEADER);
		writer.newLine();

		// Write newly inferred relationships
		for (Long sourceId : addedStatements.keySet()) {
			String active = "1";
			for (Relationship relationship : addedStatements.get(sourceId)) {
				if (concrete == relationship.isConcrete()) {
					writeRelationship(writer,
							relationship.getRelationshipId() == -1 ? "" : relationship.getRelationshipId() + "",
							active,
							sourceId,
							concrete ? relationship.getValue().getRF2Value() : "" + relationship.getDestinationId(),
							relationship.getGroup(),
							relationship.getTypeId());
				}
			}
		}

		// Write redundant relationships
		for (Long sourceId : removedStatements.keySet()) {
			String active = "0";
			for (Relationship relationship : removedStatements.get(sourceId)) {
				if (concrete == relationship.isConcrete()) {
					writeRelationship(writer,
							relationship.getRelationshipId() + "",
							active,
							sourceId,
							concrete ? relationship.getValue().getRF2Value() : "" + relationship.getDestinationId(),
							relationship.getGroup(),
							relationship.getTypeId());
				}
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

	private void writeRelationship(BufferedWriter writer, String relationshipId, String active, Long sourceId, String destinationOrValue, Integer group, Long typeId) throws IOException {
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
		writer.write(destinationOrValue);
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
		writer.write(Concepts.EXISTENTIAL_RESTRICTION_MODIFIER);
		writer.newLine();
	}

}
