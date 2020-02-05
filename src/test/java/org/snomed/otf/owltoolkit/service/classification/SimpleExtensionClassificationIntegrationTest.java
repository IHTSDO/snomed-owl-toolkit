/*
 * Copyright 2018 SNOMED International, http://snomed.org
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

import com.google.common.collect.Sets;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.service.ReasonerServiceException;
import org.snomed.otf.owltoolkit.service.SnomedReasonerService;
import org.snomed.otf.snomedboot.testutil.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.snomed.otf.owltoolkit.service.SnomedReasonerService.ELK_REASONER_FACTORY;
import static org.snomed.otf.owltoolkit.service.classification.TestFileUtil.readEquivalentConceptLinesTrim;
import static org.snomed.otf.owltoolkit.service.classification.TestFileUtil.readInferredRelationshipLinesTrim;

public class SimpleExtensionClassificationIntegrationTest {

	private SnomedReasonerService snomedReasonerService = new SnomedReasonerService();

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	public void testClassifyBaseWithAnthrax() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_with_Anthrax_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Empty_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", Sets.newHashSet(baseRF2SnapshotZip), deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(1, lines.size());
	}

	@Test
	public void testClassifyExtensionWithRedundantRelationships() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_with_Anthrax_snapshot");
		File extensionRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Extension_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Empty_delta");
		assertNotNull(snomedReasonerService);

		// Run test many times to guard against false positives due to possible random behaviour.
		for (int i = 0; i < 10; i++) {
			doRunExtensionTest(baseRF2SnapshotZip, extensionRF2SnapshotZip, deltaZip);
		}
	}
	
	private void doRunExtensionTest(File baseRF2SnapshotZip, File extensionRF2SnapshotZip, File deltaZip) throws IOException, ReasonerServiceException {
		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", Sets.newHashSet(baseRF2SnapshotZip, extensionRF2SnapshotZip), deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(11, lines.size());

		assertTrue("Logically equal inferred relationship in extension is made redundant.",
				lines.contains("600101001\t\t0\t\t409498004\t404684003\t0\t116680003\t900000000000011006\t900000000000451002"));
		assertTrue("Logically equal inferred relationship in extension is made redundant.",
				lines.contains("600202001\t\t0\t\t409498004\t21927003\t2\t246075003\t900000000000011006\t900000000000451002"));
		assertTrue("Logically equal inferred relationship in extension is made redundant.",
				lines.contains("600203001\t\t0\t\t409498004\t441862004\t2\t370135005\t900000000000011006\t900000000000451002"));
	}


	@Test
	public void testClassifyExtensionWithIntOWL() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_CompleteOwl_snapshot");
		File extensionRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Extension_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Empty_delta");
		assertNotNull(snomedReasonerService);
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", Sets.newHashSet(baseRF2SnapshotZip, extensionRF2SnapshotZip), deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(8, lines.size());
		assertTrue("Self grouped non IS-A in extension overriding International version should be inacitvated", lines.contains("600204001\t\t0\t\t362969004\t113331007\t0\t363698007\t900000000000011006\t900000000000451002"));
	}

	@Test
	public void testDuplicateAxiomInactivation() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_CompleteOwl_snapshot");
		File extensionRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Extension_snapshot_with_duplicate_axiom_expression");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Extension_delta_remove_duplicate_axiom");
		assertNotNull(snomedReasonerService);
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", Sets.newHashSet(baseRF2SnapshotZip, extensionRF2SnapshotZip), deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals("No classification change - only header line", 1, lines.size());
	}
}
