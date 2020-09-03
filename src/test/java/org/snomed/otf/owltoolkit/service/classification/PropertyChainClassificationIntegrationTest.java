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
import java.util.List;

import static org.junit.Assert.*;
import static org.snomed.otf.owltoolkit.service.SnomedReasonerService.ELK_REASONER_FACTORY;
import static org.snomed.otf.owltoolkit.service.classification.TestFileUtil.readInferredRelationshipLinesTrim;

public class PropertyChainClassificationIntegrationTest {

	private final SnomedReasonerService snomedReasonerService = new SnomedReasonerService();

	@Test
	public void testClassifyPropertyChain() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Active_Ingredient_Property_Chain_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(3, lines.size());

		/*
			We have a property chain: Has active ingredient o Is modification of -> Has active ingredient
					let's call these: 'Source type'         o 'Destination type' -> 'Inferred type'

			Also 'Morphine sulphate' is a modification of 'Morphine'

			We state relationship B: Morphine sulphate product - Has active ingredient - Morphine sulphate substance
			The reasoner infers relationship A: Morphine sulphate product - Has active ingredient - Morphine substance
			After classification this relationship is inherited from the parent.

			Relationship A is redundant because:
			 - B's type is the same as or a subtype of the property chain source type
			 - A's type is the same as the property chain inferred type
			 - B's value has a path to A's value via the property chain destination type

			This relationship is redundant even though it's in a different group to the more specific active ingredient.
		*/
		assertFalse("Relationship not inferred. Morphine sulphate product - Has active ingredient - Morphine substance",
				lines.contains("1\t\t100206001\t100202001\t2\t127489000\t900000000000011006\t900000000000451002"));

		assertTrue("Inferred relationship. Morphine sulphate product - Is a - Morphine product",
				lines.contains("1\t\t100206001\t100205001\t0\t116680003\t900000000000011006\t900000000000451002"));

		assertTrue("Redundant relationship. Morphine sulphate product - Is a - Product",
				lines.contains("200210021\t\t0\t\t100206001\t100204001\t0\t116680003\t900000000000011006\t900000000000451002"));


		/*
			We have a property chain: Has active ingredient o Is modification of -> Has active ingredient
					let's call these: 'Source type'         o 'Destination type' -> 'Inferred type'

			Also 'Fluocortolone hexanoate' is a modification of 'Fluocortolone'

			We state relationship B: Morphine sulphate product - Has precise active ingredient - Fluocortolone hexanoate (substance)
			We state relationship A: Morphine sulphate product - Has precise active ingredient - Fluocortolone (substance)

			Relationship A is not redundant because:
			 - B's type is the same as or a subtype of the property chain source type
			 - A's type is NOT the same as the property chain inferred type
			 	.. so we can not use the property chain destination type path

			Neither of these relationships are redundant even though Fluocortolone hexanoate (substance) is a modification of Fluocortolone (substance)
		*/
		assertFalse("Relationship not redundant. Morphine sulphate product - Has precise active ingredient - Fluocortolone (substance)",
				lines.contains("200212021\t\t0\t900000000000207008\t100206001\t100301001\t3\t762949000\t900000000000011006\t900000000000451002"));
		assertFalse("Relationship not redundant. Morphine sulphate product - Has precise active ingredient - Fluocortolone hexanoate (substance)",
				lines.contains("200213021\t\t0\t900000000000207008\t100206001\t100302001\t3\t762949000\t900000000000011006\t900000000000451002"));
	}

}
