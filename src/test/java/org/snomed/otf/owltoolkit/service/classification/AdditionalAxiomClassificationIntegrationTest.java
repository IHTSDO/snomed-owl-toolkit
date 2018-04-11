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

import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.snomed.otf.owltoolkit.service.ReasonerServiceException;
import org.snomed.otf.owltoolkit.service.SnomedReasonerService;
import org.snomed.otf.snomedboot.testutil.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static org.snomed.otf.owltoolkit.service.SnomedReasonerService.ELK_REASONER_FACTORY;
import static org.snomed.otf.owltoolkit.service.classification.TestFileUtil.readInferredRelationshipLinesTrim;

public class AdditionalAxiomClassificationIntegrationTest {

	private SnomedReasonerService snomedReasonerService = new SnomedReasonerService();

	@Test
	public void testClassifyGCI() throws IOException, OWLOntologyCreationException, ReleaseImportException, ReasonerServiceException {
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

}
