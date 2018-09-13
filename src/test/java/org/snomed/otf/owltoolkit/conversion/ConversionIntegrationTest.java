package org.snomed.otf.owltoolkit.conversion;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.otf.snomedboot.factory.ImpotentComponentFactory;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.snomed.otf.owltoolkit.constants.RF2Headers;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;
import org.snomed.otf.owltoolkit.util.InputStreamSet;
import org.snomed.otf.owltoolkit.util.OptionalFileInputStream;
import org.snomed.otf.snomedboot.testutil.ZipUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;

public class ConversionIntegrationTest {

	private final RF2ToOWLService rf2ToOWLService;
	private final StatedRelationshipToOwlRefsetService statedRelationshipToOwlRefsetService;

	public ConversionIntegrationTest() {
		rf2ToOWLService = new RF2ToOWLService();
		statedRelationshipToOwlRefsetService = new StatedRelationshipToOwlRefsetService();
	}

	@Test
	/*
		In this test we:
		- Create Protégé OWL file from release with stated relationships
		- Create OWL based release by:
				- Converting stated relationships into complete OWL Axiom refset
				- Make stated relationships inactive
		-  Create OWL file from OWL based release
		- Check that OWL files from both the stated relationship and OWL Axiom based releases come out the same
	 */
	public void testCompareStatedToOwlFileAndCompleteAxiomsToOwlFile() throws IOException, ConversionException, OWLOntologyCreationException {

		// Convert stated relationships to OWL file
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (InputStreamSet snapshotArchives = new InputStreamSet(new FileInputStream(baseRF2SnapshotZip));
			 OptionalFileInputStream deltaStream = new OptionalFileInputStream(null)) {

			rf2ToOWLService.convertRF2ArchiveToOWL(null, "20180931", true, snapshotArchives, deltaStream, byteArrayOutputStream);
		}
		String owlFileFromStatedRelationships = byteArrayOutputStream.toString();

		// Convert stated relationships to OWL Axiom refset
		File owlReleaseDir = Files.createTempDirectory("SnomedCT_RF2_OWL_delta").toFile();
		FileUtils.copyDirectory(new File("src/test/resources/SnomedCT_MiniRF2_Empty_delta"), owlReleaseDir);
		File additionalOwlAxioms = new File(owlReleaseDir, "Delta/Terminology/sct2_sRefset_OWLAxiomDelta_INT_20180731.txt");
		try (FileInputStream snapshotStream = new FileInputStream(baseRF2SnapshotZip);
			 OptionalFileInputStream deltaStream = new OptionalFileInputStream(null);
			 FileOutputStream outputStream = new FileOutputStream(additionalOwlAxioms)) {

			SnomedTaxonomy snomedTaxonomy = statedRelationshipToOwlRefsetService.readSnomedTaxonomy(snapshotStream, deltaStream, new ImpotentComponentFactory(), new ImpotentComponentFactory());
			outputStream.write((RF2Headers.OWL_EXPRESSION_REFERENCE_SET_HEADER + "\n").getBytes(StandardCharsets.UTF_8));
			statedRelationshipToOwlRefsetService.convertStatedRelationshipsToOwlRefset(snomedTaxonomy, outputStream);
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Files.copy(additionalOwlAxioms.toPath(), out);

		System.out.println();
		System.out.println("-- OWL Refset start --");
		System.out.println(out.toString());
		System.out.println("-- OWL Refset end --");
		System.out.println();

		// Make all stated relationships inactive
		try (BufferedReader reader = new BufferedReader(new FileReader("src/test/resources/SnomedCT_MiniRF2_Base_snapshot" +
				"/Snapshot/Terminology/sct2_StatedRelationship_Snapshot_INT_20180731.txt"));
			 BufferedWriter writer = new BufferedWriter(new FileWriter(new File(owlReleaseDir, "Delta/Terminology/sct2_StatedRelationship_Delta_INT_20180731.txt")))) {

			// Copy header
			writer.write(reader.readLine());
			writer.newLine();

			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isEmpty() && !line.startsWith("#")) {
					String[] split = line.split("\\t");
					// Clear effective date
					split[1] = "0";
					// Make inactive
					split[2] = "0";

					writer.write(String.join("\t", split));
					writer.newLine();
				}
			}
		}

		// Convert OWL Axiom based release to OWL file
		File owlBasedDeltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines(owlReleaseDir.getAbsolutePath());
		try (InputStreamSet snapshotArchives = new InputStreamSet(new FileInputStream(baseRF2SnapshotZip));
			 OptionalFileInputStream deltaStream = new OptionalFileInputStream(owlBasedDeltaZip)) {

			byteArrayOutputStream.reset();
			rf2ToOWLService.convertRF2ArchiveToOWL(null, "20180931", true, snapshotArchives, deltaStream, byteArrayOutputStream);
		}
		String owlFileFromOwlBasedRelease = byteArrayOutputStream.toString();

		// OWL file from stated relationships should be equal to OWL file from generated OWL Axiom refset
		assertEquals(owlFileFromStatedRelationships, owlFileFromOwlBasedRelease);
	}

}
