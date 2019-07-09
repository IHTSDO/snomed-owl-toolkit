package org.snomed.otf.owltoolkit;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.snomed.otf.owltoolkit.conversion.ConversionException;
import org.snomed.otf.owltoolkit.service.ReasonerServiceException;
import org.snomed.otf.snomedboot.testutil.ZipUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ApplicationTest {

	public static final String HELP = "Usage:\n" +
			" -help                                  Print this help message.\n" +
			"\n" +
			" -classify                              Run classification process.\n" +
			"                                        Results are written to an RF2 delta archive.\n" +
			"\n" +
			" -rf2-to-owl                            (Default mode) Convert RF2 to OWL Functional Syntax.\n" +
			"                                        Results are written to an .owl file.\n" +
			"\n" +
			" -rf2-stated-to-complete-owl            Convert RF2 snapshot stated relationships to complete OWL Axiom reference set preview.\n" +
			"                                        Stated relationships are converted to OWL Axiom reference set entries.\n" +
			"                                        Existing stated relationships are marked as inactive.\n" +
			"                                        Results are written to a zip file containing:\n" +
			"                                         - OWL Axiom reference set delta of all axioms which were previously stated relationships\n" +
			"                                         - Stated relationship delta with all relationships which were previously active marked as inactive.\n" +
			"\n" +
			" -rf2-stated-to-complete-owl-reconcile  Convert mid authoring cycle RF2 snapshot stated relationships to complete OWL Axiom reference set and reconcile with published version to genereate delta changes.\n" +
			"                                        Two snapshot archives must be required. The first one should be the mid authoring cycle snapshot and the second one is the published complete owl snapshot archive.\n" +
			"                                        Stated relationships are converted to OWL Axiom reference set entries.\n" +
			"                                        These converted OWL Axioms are then reconciled with published version.\n" + 
			"                                        Results are written to a zip file containing:\n" + 
			"                                         - OWL Axiom reference set delta consisting of newly added or modified axioms converted from stated relationships.\n" +
			"                                         - Published OWL Axiom reference set uuid will be used for an updated axiom.\n" +
			"\n" +

			" -rf2-snapshot-archives <path>          Comma separated paths of zip files containing RF2 Snapshot files to be loaded. \n" +
			"                                        At least one Snapshot archive is required.\n" +
			"\n" +
			" -rf2-authoring-delta-archive <path>    (Optional) Path to a zip file containing RF2 Delta files to be applied on top \n" +
			"                                        of the Snapshots. This is helpful during an authoring cycle.\n" +
			"\n" +
			" -debug                                 Additional output for debugging.\n" +
			"\n" +
			"\n" +
			"Optional parameters for OWL conversion:\n" +
			" -uri <uri>                             (Optional) URI for the ontology identifier.\n" +
			"                                        Defaults to the id within the header entry of the OWL Ontology reference set.\n" +
			"                                        If no entry found defaults to http://snomed.info/sct/900000000000207008.\n" +
			"\n" +
			" -version <version>                     (Optional) Date for the ontology version e.g. 20180731.\n" +
			"                                        Defaults to today's date.\n" +
			"\n" +
			" -without-annotations                   (Optional) Flag to omit Fully Specified Name annotations from the ontology \n" +
			"                                        resulting in a smaller file size.\n" +
			"\n" +
			"\n";

	private ByteArrayOutputStream errorStream;
	private ByteArrayOutputStream outStream;

	@Before
	public void setup() {
		errorStream = new ByteArrayOutputStream();
		outStream = new ByteArrayOutputStream();
	}

	@Test
	public void mainNoArgs() throws Exception {
		runApplication(new String[] {});

		assertEquals(HELP, outStream.toString());
		assertEquals("", errorStream.toString());
	}

	@Test
	public void mainHelpArg() throws Exception {
		runApplication(new String[] {"-help"});

		assertEquals(HELP, outStream.toString());
		assertEquals("", errorStream.toString());
	}

	@Test
	public void mainArgFileNoValue() throws Exception {
		runApplication(new String[] {"-rf2-snapshot-archives"});

		assertEquals("\n\n" + HELP, outStream.toString());
		assertEquals("Expecting a value with parameter -rf2-snapshot-archives\n", errorStream.toString());
	}

	@Test
	public void mainArgsFileAndBadValue() throws Exception {
		runApplication(new String[] {"-rf2-snapshot-archives", "dummy"});

		assertEquals("\n\n" + HELP, outStream.toString());
		assertTrue(errorStream.toString().endsWith("dummy should be a file.\n"));
	}

	@Test
	public void mainArgTwoFilesSeparatedByCommaAndSpace() throws Exception {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File extensionRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Extension_snapshot");

		String arguments = "-rf2-snapshot-archives " + baseRF2SnapshotZip.getAbsolutePath() + ", " + extensionRF2SnapshotZip.getAbsolutePath();
		runApplication(arguments.split(" "));

		assertEquals("\n\n" + HELP, outStream.toString());
		assertEquals("When specifying multiple archives please include no whitespace before or after the comma.\n", errorStream.toString());
	}

	@Test
	public void mainArgTwoFilesSeparatedByCommaNoSpace() throws Exception {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File extensionRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Extension_snapshot");

		String arguments = "-rf2-snapshot-archives " + baseRF2SnapshotZip.getAbsolutePath() + "," + extensionRF2SnapshotZip.getAbsolutePath();
		runApplication(arguments.split(" "));

		String out = outStream.toString();
		assertTrue(Pattern.compile(".*Snapshot archives: \\[.*\\.zip, .*\\.zip].*", Pattern.DOTALL).matcher(out).matches());
		assertTrue(out.contains("OWL Ontology file written to - ontology-"));
		assertEquals("", errorStream.toString());
	}

	@Test
	public void mainArgsFileAndVersion() throws Exception {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		runApplication(new String[] {
				"-rf2-snapshot-archives", baseRF2SnapshotZip.getAbsolutePath(),
				"-version", "20180731"
		});

		String out = outStream.toString();
		System.out.println(out);
		assertTrue(out.contains("OWL Ontology file written to - ontology-"));
		assertEquals("", errorStream.toString());
	}

	private void runApplication(String[] args) {
		PrintStream originalErrorStream = System.err;
		System.setErr(new PrintStream(errorStream));
		PrintStream originalOutStream = System.out;
		System.setOut(new PrintStream(outStream));
		try {
			Application application = new Application();
			application.deleteOntologyFileOnExit();// Automatic file cleanup
			application.run(args);
		} catch (ConversionException | IOException | IllegalArgumentException | ReasonerServiceException | OWLOntologyCreationException e) {
			// Swallow all
		} finally {
			System.setErr(originalErrorStream);
			System.setOut(originalOutStream);
		}
	}

}
