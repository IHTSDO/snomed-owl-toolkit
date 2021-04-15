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
package org.snomed.otf.owltoolkit.service.classification;

import org.junit.Test;
import org.snomed.otf.owltoolkit.service.ReasonerServiceException;
import org.snomed.otf.owltoolkit.service.SnomedReasonerService;
import org.snomed.otf.snomedboot.testutil.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.snomed.otf.owltoolkit.service.SnomedReasonerService.ELK_REASONER_FACTORY;
import static org.snomed.otf.owltoolkit.service.classification.TestFileUtil.readInferredRelationshipLinesTrim;

public class AdditionalAxiomClassificationIntegrationTest {

	private SnomedReasonerService snomedReasonerService = new SnomedReasonerService();

	@Test
	public void testClassifyAdditionalAxiom() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Triangle_Additional_Axiom_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(5, lines.size());

		// Header line plus:

		// Inferred relationship from additional axiom on same concept
		assertTrue("Inferred relationship. Triangle - Number of corners - 3",
				lines.contains("1\t\t100105001\t100107001\t0\t100104001\t900000000000011006\t900000000000451002"));

		assertTrue("Redundant relationship. Right angle triangle - is a - Polygon",
				lines.contains("200107021\t\t0\t\t100106001\t100102001\t0\t116680003\t900000000000011006\t900000000000451002"));

		assertTrue("Inferred relationship. Right angle triangle - is a - Triangle",
				lines.contains("1\t\t100106001\t100105001\t0\t116680003\t900000000000011006\t900000000000451002"));

		// Inferred relationship from additional axiom on parent concept
		assertTrue("Inferred relationship. Right angle triangle - Number of corners - 3",
				lines.contains("1\t\t100106001\t100107001\t0\t100104001\t900000000000011006\t900000000000451002"));

	}

	@Test
	public void testClassifyShouldNotMergeMultipleAxiomsFromDelta() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_GroupSeparation_Additional_Axiom_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		int countGroup1 = 0;
		int countGroup2 = 0;
		for (String line : lines.subList(1, lines.size())) {
			List<String> split = reverseSplit(line, "\t");
			String groupId = split.get(3);
			if ("1".equals(groupId)) {
				countGroup1++;
			}

			if ("2".equals(groupId)) {
				countGroup2++;
			}
		}

		assertEquals(1, countGroup1);
		assertEquals(1, countGroup2);
	}

	@Test
	public void testClassifyShouldNotMergeMultipleAxiomsFromSnapshot() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_GroupSeparation_Additional_Axiom_snapshot");

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, null, results, ELK_REASONER_FACTORY, false);

		// Assert results
		int countGroup1 = 0;
		int countGroup2 = 0;
		List<String> lines = readInferredRelationshipLinesTrim(results);
		for (String line : lines.subList(1, lines.size())) {
			List<String> split = reverseSplit(line, "\t");
			String groupId = split.get(3);
			String active = split.get(7);

			if ("2".equals(groupId)) {
				countGroup2++;
				assertEquals("1", active);
			} else if ("1".equals(groupId)) {
				countGroup1++;
				assertEquals("0", active);
			} else {
				fail();
			}
		}

		assertEquals(1, countGroup1);
		assertEquals(1, countGroup2);
	}

	private List<String> reverseSplit(String input, String delimiter) {
		List<String> output = Arrays.asList(input.split(delimiter));
		Collections.reverse(output);

		return output;
	}

}
