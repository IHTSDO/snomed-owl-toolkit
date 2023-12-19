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
import static org.snomed.otf.owltoolkit.service.classification.TestFileUtil.readEquivalentConceptLinesTrim;
import static org.snomed.otf.owltoolkit.service.classification.TestFileUtil.readInferredRelationshipLinesTrim;

public class SimpleClassificationIntegrationTest {

	private SnomedReasonerService snomedReasonerService = new SnomedReasonerService();

	private static final String FINDING_SITE = "363698007";

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Before
	public void setup() {
		logger.info("Logging test");
	}

	@Test
	public void testClassifyBase() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
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
	public void testClassifyNewConcept() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Add_Diabetes_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(3, lines.size());
		assertTrue(lines.contains("1\t\t73211009\t362969004\t0\t" + Concepts.IS_A + "\t900000000000011006\t900000000000451002"));
		assertTrue(lines.contains("1\t\t73211009\t113331007\t0\t" + FINDING_SITE + "\t900000000000011006\t900000000000451002"));
	}

	@Test
	public void testClassifyAnnotationAttributes() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Add_Annotation_Property_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(3, lines.size());
		assertTrue(lines.contains("1\t\t3113249002\t1295447006\t0\t" + Concepts.IS_A+ "\t900000000000011006\t900000000000451002"));
		assertTrue(lines.contains("1\t\t1295449009\t1295447006\t0\t" + Concepts.IS_A+ "\t900000000000011006\t900000000000451002"));
	}


	@Test
	public void testClassifyConceptInactivation() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Concept_Inactivation_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Concept_Inactivation_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(4, lines.size());
		assertTrue(lines.contains("200009001\t\t0\t\t362969004\t404684003\t0\t116680003\t900000000000011006\t900000000000451002"));
		assertTrue(lines.contains("200010001\t\t0\t\t362969004\t113331007\t0\t363698007\t900000000000011006\t900000000000451002"));
		assertTrue(lines.contains("200008001\t\t0\t\t404684003\t138875005\t0\t116680003\t900000000000011006\t900000000000451002"));
		List<String> equivalence = readEquivalentConceptLinesTrim(results);
		assertEquals(1, equivalence.size());
	}

	@Test
	public void testClassifyConceptDeletion() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Concept_Deletion_Orphan_Relationship_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(2, lines.size());
		assertTrue(lines.contains("14658205020\t\t0\t\t1142139005\t404684003\t0\t116680003\t900000000000011006\t900000000000451002"));
		List<String> equivalence = readEquivalentConceptLinesTrim(results);
		assertEquals(1, equivalence.size());
	}

	@Test
	public void testClassifyConceptReactivation() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Concept_Inactivation_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Concept_Reactivation_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(3, lines.size());// We only have 2 inactive concepts now
		assertTrue(lines.contains("200009001\t\t0\t\t362969004\t404684003\t0\t116680003\t900000000000011006\t900000000000451002"));
		assertTrue(lines.contains("200010001\t\t0\t\t362969004\t113331007\t0\t363698007\t900000000000011006\t900000000000451002"));
		List<String> equivalence = readEquivalentConceptLinesTrim(results);
		assertEquals(1, equivalence.size());
	}

	@Test
	/*
		Assert that is-a relationships are inferred for new attributes
	 */
	public void testClassifyNewAttribute() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Add_Attribute_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results - stated relationship also inferred
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(2, lines.size());
		assertTrue(lines.contains("1\t\t246090004\t762705008\t0\t116680003\t900000000000011006\t900000000000451002"));

		// No equivalent concepts
		List<String> equivalence = readEquivalentConceptLinesTrim(results);
		assertEquals(1, equivalence.size());
	}

	@Test
	/*
		New attribute with two parents
	 */
	public void testClassifyAttributeWithTwoParents() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Add_Attribute_with_two_parents_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results - stated relationship also inferred
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(5, lines.size());
		assertTrue(lines.contains("1\t\t42752001\t762705008\t0\t116680003\t900000000000011006\t900000000000451002"));
		assertTrue(lines.contains("1\t\t255234002\t762705008\t0\t116680003\t900000000000011006\t900000000000451002"));
		assertTrue(lines.contains("1\t\t20000090004\t42752001\t0\t116680003\t900000000000011006\t900000000000451002"));
		assertTrue(lines.contains("1\t\t20000090004\t255234002\t0\t116680003\t900000000000011006\t900000000000451002"));

		// No equivalent concepts
		List<String> equivalence = readEquivalentConceptLinesTrim(results);
		assertEquals(1, equivalence.size());
	}

	@Test
	/*
		Assert that inferred is-a relationships are made redundant for newly inactive attributes
	 */
	public void testClassifyInactiveAttribute() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_with_extra_attribute_snapshot");
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Inactivate_Attribute_delta");
		assertNotNull(snomedReasonerService);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, deltaZip, results, ELK_REASONER_FACTORY, false);

		// Assert results - stated relationship also inferred
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(2, lines.size());
		assertTrue(lines.contains("200016001\t\t0\t\t246112005\t762705008\t0\t116680003\t900000000000011006\t900000000000451002"));

		// No equivalent concepts
		List<String> equivalence = readEquivalentConceptLinesTrim(results);
		assertEquals(1, equivalence.size());
	}

	@Test
	public void testClassifyReactivateInferredRelationship() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_some_Inactive_snapshot");

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, null, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		System.out.println(lines);
		assertEquals("Relationship delta should only contain the header line.", 3, lines.size());
		assertTrue(lines.contains("200009001\t\t1\t\t362969004\t404684003\t0\t116680003\t900000000000011006\t900000000000451002"));
		assertTrue(lines.contains("200010001\t\t1\t\t362969004\t113331007\t0\t363698007\t900000000000011006\t900000000000451002"));
	}
}
