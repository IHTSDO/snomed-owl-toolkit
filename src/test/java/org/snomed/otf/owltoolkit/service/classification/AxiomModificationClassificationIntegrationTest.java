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
import org.snomed.otf.owltoolkit.service.ClassificationContainer;
import org.snomed.otf.owltoolkit.service.ReasonerServiceException;
import org.snomed.otf.owltoolkit.service.SnomedReasonerService;
import org.snomed.otf.snomedboot.testutil.ZipUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static org.snomed.otf.owltoolkit.service.SnomedReasonerService.ELK_REASONER_FACTORY;
import static org.snomed.otf.owltoolkit.service.classification.TestFileUtil.readInferredRelationshipLinesTrim;

public class AxiomModificationClassificationIntegrationTest {

	private final SnomedReasonerService snomedReasonerService = new SnomedReasonerService();

	@Test
	public void testClassifyStatedRelationshipAndAxiomSnapshotWithoutChanges() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_with_Axioms_snapshot");
		File deltaZip = null;
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals("Just header line", 1, lines.size());
	}

	@Test
	public void testClassifyAxiomSnapshotWithoutChanges() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_CompleteOwl_snapshot");
		File deltaZip = null;
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals("Just header line", 1, lines.size());
	}

	@Test
	public void testClassifyAxiomSnapshotWithoutDeltaChangeLeadingToRedundantAttribute() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_CompleteOwl_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Change_Axiom_Parents_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(2, lines.size());

		// Existing attribute is made inactive - Disorder of endocrine system - Finding site - Structure of endocrine system
		assertTrue(lines.contains("200111001\t\t0\t\t362969004\t113331007\t1\t363698007\t900000000000011006\t900000000000451002"));
	}

}
