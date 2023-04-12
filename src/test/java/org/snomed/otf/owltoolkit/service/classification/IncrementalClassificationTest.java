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

public class IncrementalClassificationTest {

	private final SnomedReasonerService snomedReasonerService = new SnomedReasonerService();

	@Test
	public void testIncrementallyClassifyAxiomSnapshotWithoutDeltaChangeLeadingToRedundantAttribute() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_CompleteOwl_snapshot");
		assertNotNull(snomedReasonerService);


		// Run initial classification
		File results = TestFileUtil.newTemporaryFile();
		final ClassificationContainer classificationContainer = snomedReasonerService.classify("", baseRF2SnapshotZip, null, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(1, lines.size());


		// Run incremental classification using existing container and reasoner ontology
		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Change_Axiom_Parents_delta");
		results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classifyTransientAxioms(classificationContainer, new FileInputStream(deltaZip), new FileOutputStream(results));

		// Assert results
		lines = readInferredRelationshipLinesTrim(results);
		assertEquals(2, lines.size());

		// Existing attribute is made inactive - Disorder of endocrine system - Finding site - Structure of endocrine system
		assertTrue(lines.contains("200111001\t\t0\t\t362969004\t113331007\t1\t363698007\t900000000000011006\t900000000000451002"));


		// Run incremental classification again
		// Because we have not saved the inferred relationships from the last classification we get the same results again
		deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Change_Axiom_Parents_delta");
		results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classifyTransientAxioms(classificationContainer, new FileInputStream(deltaZip), new FileOutputStream(results));

		// Assert results
		lines = readInferredRelationshipLinesTrim(results);
		assertEquals(2, lines.size());

		// Existing attribute is made inactive - Disorder of endocrine system - Finding site - Structure of endocrine system
		assertTrue(lines.contains("200111001\t\t0\t\t362969004\t113331007\t1\t363698007\t900000000000011006\t900000000000451002"));
	}

	@Test
	// Two finding sites given in axiom, one found in NNF results
	public void testNNF() throws IOException, ReasonerServiceException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_CompleteOwl_snapshot");
		File results = TestFileUtil.newTemporaryFile();
		final ClassificationContainer classificationContainer = snomedReasonerService.classify("", baseRF2SnapshotZip, null, results, ELK_REASONER_FACTORY, false);
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(1, lines.size());


		File deltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_endocrine_system_subtype_delta");
		results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classifyTransientAxioms(classificationContainer, new FileInputStream(deltaZip), new FileOutputStream(results));

		// Assert results
		lines = readInferredRelationshipLinesTrim(results);
		assertEquals(3, lines.size());

		// 5552004 |Disorder of ovary (disorder)| is a 362969004 |Disorder of endocrine system (disorder)|
		assertTrue(lines.contains("1\t\t5552004\t362969004\t0\t116680003\t900000000000011006\t900000000000451002"));

		// 5552004 |Disorder of ovary (disorder)| - 363698007 | Finding site (attribute) | - 15497006 | Ovarian structure (body structure) |
		assertTrue(lines.contains("1\t\t5552004\t15497006\t1\t363698007\t900000000000011006\t900000000000451002"));
	}
}
