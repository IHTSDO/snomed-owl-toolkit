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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static org.snomed.otf.owltoolkit.service.SnomedReasonerService.ELK_REASONER_FACTORY;
import static org.snomed.otf.owltoolkit.service.classification.TestFileUtil.readInferredRelationshipLinesTrim;

public class TransitiveReflexiveClassificationIntegrationTest {

	private SnomedReasonerService snomedReasonerService = new SnomedReasonerService();

	@Test
	public void testClassify() throws IOException, OWLOntologyCreationException, ReleaseImportException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Anatomy_Transitive_Reflexive_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify(
				"",
				new FileInputStream(baseRF2SnapshotZip),
				new FileInputStream(deltaZip),
				new FileOutputStream(results),
				ELK_REASONER_FACTORY,
				false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);

		// This inference is a direct result of reflection
		// because we stated that Structure_of_upper_limb - All_or_Part_of - Entire_upper_limb
		// and that "All_or_Part_of" is reflexive. The attribute has been reflected onto Entire_upper_limb
		assertTrue("Inferred relationship. Entire_upper_limb - All_or_Part_of - Entire_upper_limb",
				lines.contains("1		500003001	500003001	0	733928003	900000000000011006	900000000000451002"));

		// The attribute added to Entire_upper_limb via reflection causes the concept to be subsumed by Structure_of_upper_limb
		// which is defined as a subclass of Body_structure and All_or_Part_of - Entire_upper_limb
		assertTrue("Inferred relationship. Entire_upper_limb - is a - Structure_of_upper_limb",
				lines.contains("1		500003001	500006001	0	116680003	900000000000011006	900000000000451002"));

		assertTrue("Inferred relationship. Structure_of_hand - is a - Structure_of_upper_limb",
				lines.contains("1		500007001	500006001	0	116680003	900000000000011006	900000000000451002"));
		assertTrue("Redundant relationship. Structure_of_hand - is a - Body_structure",
				lines.contains("200112001		0		500007001	500001001	0	116680003	900000000000011006	900000000000451002"));

		assertTrue("Inferred relationship. Entire_hand - All_or_Part_of - Entire_hand",
				lines.contains("1		500004001	500004001	0	733928003	900000000000011006	900000000000451002"));
		assertTrue("Inferred relationship. Entire_hand - is a - Structure_of_hand",
				lines.contains("1		500004001	500007001	0	116680003	900000000000011006	900000000000451002"));

		assertTrue("Inferred relationship. Structure_of_finger - is a - Structure_of_hand",
				lines.contains("1		500008001	500007001	0	116680003	900000000000011006	900000000000451002"));
		assertTrue("Redundant relationship. Structure_of_finger - is a - Structure_of_hand",
				lines.contains("200114001		0		500008001	500001001	0	116680003	900000000000011006	900000000000451002"));

		assertTrue("Inferred relationship. Entire_finger - All_or_Part_of - Entire_finger",
				lines.contains("1		500005001	500005001	0	733928003	900000000000011006	900000000000451002"));
		assertTrue("Inferred relationship. Entire_finger - is a - Structure_of_finger",
				lines.contains("1		500005001	500008001	0	116680003	900000000000011006	900000000000451002"));

		// Although these relationships would normally be inherited from supertypes they should not be part of the normal form
		// because more specific attributes are present, as identified via the Part_of transitive closure
		// A new transitive closure graph is built for each transitive attribute type and it's subtypes
		assertFalse("Should not be part of the normal form: Structure_of_finger - All_or_Part_of - Entire_upper_limb",
				lines.contains("1		500008001	500003001	0	733928003	900000000000011006	900000000000451002"));
		assertFalse("Should not be part of the normal form: Structure_of_finger - All_or_Part_of - Entire_upper_limb",
				lines.contains("1		500008001	500004001	0	733928003	900000000000011006	900000000000451002"));
		assertFalse("Should not be part of the normal form: Entire_finger - All_or_Part_of - Entire_upper_limb",
				lines.contains("1		500005001	500003001	0	733928003	900000000000011006	900000000000451002"));

		assertEquals(11, lines.size());
	}

}
