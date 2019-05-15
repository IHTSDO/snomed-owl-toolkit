package org.snomed.otf.owltoolkit.conversion;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.snomed.otf.owltoolkit.service.ReasonerServiceException;
import org.snomed.otf.owltoolkit.service.SnomedReasonerService;
import org.snomed.otf.owltoolkit.service.classification.TestFileUtil;
import org.snomed.otf.owltoolkit.util.OptionalFileInputStream;
import org.snomed.otf.snomedboot.testutil.ZipUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.snomed.otf.owltoolkit.service.SnomedReasonerService.ELK_REASONER_FACTORY;
import static org.snomed.otf.owltoolkit.service.classification.TestFileUtil.readInferredRelationshipLinesTrim;

public class GroupZeroConversionIntegrationTest {

	private final StatedRelationshipToOwlRefsetService statedRelationshipToOwlRefsetService;
	private final SnomedReasonerService snomedReasonerService;

	public GroupZeroConversionIntegrationTest() {
		statedRelationshipToOwlRefsetService = new StatedRelationshipToOwlRefsetService();
		snomedReasonerService = new SnomedReasonerService();
	}

	@Test
	/*
		In this test we:
		- Convert stated relationships to OWL axiom refset members
			- This will include converting a group 0 relationship to be self grouped in the axiom
		- Classify
		- Check that self grouped relationships come out in a non-zero group and that the original record was kept and updated
	 */
	public void testConvertToOwlAxiomRefsetAndClassify() throws Exception {

		// Convert stated relationships to OWL Axiom refset
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
//		File baseRF2DeltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Add_Laterality_delta");
		File owlOutputZip = Files.createTempFile("OWL_Delta", ".zip").toFile();
		try (FileInputStream snapshotStream = new FileInputStream(baseRF2SnapshotZip);
			 OptionalFileInputStream deltaStream = new OptionalFileInputStream(null);
			 FileOutputStream outputStream = new FileOutputStream(owlOutputZip)) {

			statedRelationshipToOwlRefsetService.convertStatedRelationshipsToOwlRefsetAndInactiveRelationshipsArchive(snapshotStream, deltaStream, outputStream, "20180731");
		}

		// Overlay zip files to create release file with all necessary files
		TestFileUtil.addFilesToZipFlatteningPaths(new File("src/test/resources/SnomedCT_MiniRF2_Empty_delta"), owlOutputZip, false);

		// Run classification
		File results = TestFileUtil.newTemporaryFile();
		snomedReasonerService.classify("", baseRF2SnapshotZip, owlOutputZip, results, ELK_REASONER_FACTORY, false);

		// Assert results
		List<String> lines = readInferredRelationshipLinesTrim(results);
		assertEquals(2, lines.size());

		String inferredGroupNumber = "1";
		// Assert that the existing active group 0 relationship is moved to another group number
		assertEquals("200010001\t\t1\t\t362969004\t113331007\t" + inferredGroupNumber + "\t363698007\t900000000000011006\t900000000000451002", lines.get(1));
	}

}
