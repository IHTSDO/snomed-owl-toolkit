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
import org.snomed.otf.owltoolkit.testutil.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static org.snomed.otf.owltoolkit.service.SnomedReasonerService.ELK_REASONER_FACTORY;
import static org.snomed.otf.owltoolkit.service.classification.TestFileUtil.readInferredRelationshipLinesTrim;

public class PropertyChainClassificationIntegrationTest {

	private SnomedReasonerService snomedReasonerService = new SnomedReasonerService();

	@Test
	public void testClassifyPropertyChain() throws IOException, OWLOntologyCreationException, ReleaseImportException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Active_Ingredient_Property_Chain_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(3, lines.size());

		// This relationship is inferred by the property chain and leads to 'Morphine sulphate product' being subsumed by 'Morphine product'
		// however it should not be part of the normal form because:
		// although 'Morphine substance' is NOT a subclass of 'Morphine sulphate substance'
		// it is a modification of 'Morphine sulphate substance'
		// 'Is modification of' is a transitive attribute
		// This makes the stated relationships "Morphine sulphate product - Has active ingredient - Morphine sulphate substance" more specific.
		// This makes the relationship "Morphine sulphate product - Has active ingredient - Morphine substance" redundant.
		// This relationship is redundant even though it's in a different group to the more specific active ingredient.
		assertFalse("Inferred relationship. Morphine sulphate product - Has active ingredient - Morphine substance",
				lines.contains("1\t\t100206001\t100202001\t2\t127489000\t900000000000011006\t900000000000451002"));

		assertTrue("Inferred relationship. Morphine sulphate product - Is a - Morphine product",
				lines.contains("1\t\t100206001\t100205001\t0\t116680003\t900000000000011006\t900000000000451002"));

		assertTrue("Redundant relationship. Morphine sulphate product - Is a - Product",
				lines.contains("200210021\t\t0\t\t100206001\t100204001\t0\t116680003\t900000000000011006\t900000000000451002"));

	}

}
