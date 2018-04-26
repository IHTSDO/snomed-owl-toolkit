package org.snomed.otf.owltoolkit;

import org.junit.Before;
import org.junit.Test;
import org.snomed.otf.owltoolkit.conversion.ConversionException;
import org.snomed.otf.snomedboot.testutil.ZipUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ApplicationTest {

	public static final String HELP = "Usage:\n" +
			" -help                                  Print this help message.\n" +
			"\n" +
			" -rf2-snapshot-archives <path>          Comma separated paths of zip files containing RF2 Snapshot files to be loaded. \n" +
			"                                        At least one Snapshot archive is required.\n" +
			"\n" +
			" -rf2-authoring-delta-archive <path>    (Optional) Path to a zip file containing RF2 Delta files to be applied on top \n" +
			"                                        of the Snapshots. This is helpful during an authoring cycle.\n" +
			"\n" +
			" -uri <uri>                             (Optional) URI for the ontology identifier.\n" +
			"                                        Defaults to http://snomed.info/sct/900000000000207008.\n" +
			"\n" +
			" -version <version>                     (Optional) Date for the ontology version e.g. 20180731.\n" +
			"                                        Defaults to today's date.\n" +
			"\n" +
			" -without-annotations                   (Optional) Flag to omit Fully Specified Name annotations from the ontology \n" +
			"                                        resulting in a smaller file size.\n" +
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
		} catch (ConversionException | IOException | IllegalArgumentException e) {
			// Swallow all
		} finally {
			System.setErr(originalErrorStream);
			System.setOut(originalOutStream);
		}
	}

}
