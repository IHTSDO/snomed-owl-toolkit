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

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.service.ReasonerServiceException;
import org.snomed.otf.owltoolkit.service.SnomedReasonerService;
import org.snomed.otf.snomedboot.testutil.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static org.snomed.otf.owltoolkit.service.SnomedReasonerService.ELK_REASONER_FACTORY;
import static org.snomed.otf.owltoolkit.service.classification.TestFileUtil.*;

public class ConcreteDomainsClassificationIntegrationTest {

	private final SnomedReasonerService snomedReasonerService = new SnomedReasonerService();

	private static final String FINDING_SITE = "363698007";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Before
	public void setup() {
		logger.info("Logging test");
	}

	@Test
	public void testClassifyConceptsAsNumbers() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_with_Concepts_as_numbers_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Empty_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals("Relationship delta should only contain the header line.",1, lines.size());
	}

	@Test
	public void testClassifyConversionToConcreteDomains() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_with_Concepts_as_numbers_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Concrete_Domain_conversion_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> conceptInferredChanges = readInferredRelationshipLinesTrim(results);
		assertEquals(2, conceptInferredChanges.size());

		// Concept-as-number active ingredient count made inactive
		assertTrue(conceptInferredChanges.contains("250040021\t\t0\t\t871788009\t38112003\t0\t766952006\t900000000000011006\t900000000000451002"));


		// Assert concrete values results
		List<String> concreteValueInferredChanges = readInferredRelationshipConcreteValuesLinesTrim(results);
		assertEquals(2, concreteValueInferredChanges.size());

		assertEquals("id\teffectiveTime\tactive\tmoduleId\tsourceId\tvalue\trelationshipGroup\ttypeId\tcharacteristicTypeId\tmodifierId", concreteValueInferredChanges.get(0));

		// Concrete active ingredient count
		assertEquals("Expecting concrete value with #1 in the 'value' column.",
				"1\t\t871788009\t#1\t0\t100000001001\t900000000000011006\t900000000000451002", concreteValueInferredChanges.get(1));
	}

	@Test
	public void testReClassifyConcreteDomains() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_with_Concepts_as_numbers_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Concrete_Domain_conversion_classified_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> conceptInferredChanges = readInferredRelationshipLinesTrim(results);
		assertEquals("No changes, just header line.", 1, conceptInferredChanges.size());

		// Assert concrete values results
		List<String> concreteValueInferredChanges = readInferredRelationshipConcreteValuesLinesTrim(results);
		assertEquals("No changes, just header line.", 1, concreteValueInferredChanges.size());
	}

	@Test
	public void testReClassifyConcreteDomainsWithNumberChange() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_with_Concepts_as_numbers_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Concrete_Domain_conversion_classified_delta_with_change");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> conceptInferredChanges = readInferredRelationshipLinesTrim(results);
		assertEquals("No changes, just header line.", 1, conceptInferredChanges.size());

		// Assert concrete values results
		List<String> concreteValueInferredChanges = readInferredRelationshipConcreteValuesLinesTrim(results);
		assertEquals("1 concrete inactivation plus 1 new value plus header line.", 3, concreteValueInferredChanges.size());
		assertEquals("1\t\t871788009\t#2\t0\t100000001001\t900000000000011006\t900000000000451002", concreteValueInferredChanges.get(1));
		assertEquals("450040021\t\t0\t\t871788009\t#1\t0\t100000001001\t900000000000011006\t900000000000451002", concreteValueInferredChanges.get(2));
	}

}
