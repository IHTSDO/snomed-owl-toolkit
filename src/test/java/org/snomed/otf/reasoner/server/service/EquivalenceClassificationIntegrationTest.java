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
package org.snomed.otf.reasoner.server.service;

import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.snomed.otf.owltoolkit.service.ReasonerServiceException;
import org.snomed.otf.owltoolkit.service.SnomedReasonerService;
import org.snomed.otf.owltoolkit.testutil.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.snomed.otf.owltoolkit.service.SnomedReasonerService.ELK_REASONER_FACTORY;
import static org.snomed.otf.reasoner.server.service.TestFileUtil.readEquivalentConceptLinesTrim;
import static org.snomed.otf.reasoner.server.service.TestFileUtil.readInferredRelationshipLinesTrim;

public class EquivalenceClassificationIntegrationTest {

	private SnomedReasonerService snomedReasonerService = new SnomedReasonerService();

	private static final String FINDING_SITE = "363698007";

	/**
	 * The delta contains a clone of 362969004 | Disorder of endocrine system |
	 * We expect the original concept and the clone to be returned in the equivalent concept reference set
	 */
	@Test
	public void testClassify() throws IOException, OWLOntologyCreationException, ReleaseImportException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Equivalence_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> equivalentConceptLines = readEquivalentConceptLinesTrim(results);
		assertEquals("EquivalentConcept delta should contain a pair of concept.", 3, equivalentConceptLines.size());

		String disorderOfEndocrineSystem = "362969004";
		String disorderOfEndocrineSystemClone = "1362969004";

		assertEquals("There should be an equivalent concept member containing the original concept.",
				1, equivalentConceptLines.stream().filter(line -> line.contains("\t1\t\t\t" + disorderOfEndocrineSystem + "\t")).count());
		assertEquals("There should be an equivalent concept member containing the cloned concept.",
				1, equivalentConceptLines.stream().filter(line -> line.contains("\t1\t\t\t" + disorderOfEndocrineSystemClone + "\t")).count());

		List<String> relationshipLines = readInferredRelationshipLinesTrim(results);
		assertEquals("Relationship delta should contain new inferences.", 5, relationshipLines.size());
	}

}
