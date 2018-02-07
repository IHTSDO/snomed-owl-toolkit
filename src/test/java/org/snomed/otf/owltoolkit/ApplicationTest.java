package org.snomed.otf.owltoolkit;

import org.junit.Before;
import org.junit.Test;
import org.snomed.otf.owltoolkit.conversion.ConversionException;
import org.snomed.otf.owltoolkit.testutil.ZipUtil;

import java.io.*;

import static org.junit.Assert.*;

public class ApplicationTest {

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

		assertEquals("Usage:\n" +
						" -help                   Print this help message.\n" +
						" -rf2-snap-zip <path>    Path to the SNOMED CT RF2 archive containing Snapshot files.\n" +
						" -version <version>      Version date e.g. 20180731.\n" +
						" -without-annotations    Don't include Fully Specified Name annotations in OWL file (smaller file size).\n" +
						"\n",
				outStream.toString());
		assertEquals("", errorStream.toString());
	}

	@Test
	public void mainHelpArg() throws Exception {
		runApplication(new String[] {"-help"});

		assertEquals("Usage:\n" +
						" -help                   Print this help message.\n" +
						" -rf2-snap-zip <path>    Path to the SNOMED CT RF2 archive containing Snapshot files.\n" +
						" -version <version>      Version date e.g. 20180731.\n" +
						" -without-annotations    Don't include Fully Specified Name annotations in OWL file (smaller file size).\n" +
						"\n",
				outStream.toString());
		assertEquals("", errorStream.toString());
	}

	@Test
	public void mainArgFileNoValue() throws Exception {
		runApplication(new String[] {"-rf2-snap-zip"});

		assertEquals("", outStream.toString());
		assertEquals("Expecting a value with parameter -rf2-snap-zip\n", errorStream.toString());
	}

	@Test
	public void mainArgsFileAndBadValue() throws Exception {
		runApplication(new String[] {"-rf2-snap-zip", "dummy"});

		assertEquals("", outStream.toString());
		assertTrue(errorStream.toString().endsWith("dummy should be a file.\n"));
	}

	@Test
	public void mainArgsFileAndFileValue() throws Exception {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		runApplication(new String[] {"-rf2-snap-zip", baseRF2SnapshotZip.getAbsolutePath()});

		assertEquals("", outStream.toString());
		assertEquals("Expecting parameter -version\n", errorStream.toString());
	}

	@Test
	public void mainArgsFileAndVersion() throws Exception {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		runApplication(new String[] {
				"-rf2-snap-zip", baseRF2SnapshotZip.getAbsolutePath(),
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
		} catch (ConversionException | IOException | IllegalStateException e) {
			// Swallow all
		} finally {
			System.setErr(originalErrorStream);
			System.setOut(originalOutStream);
		}
	}

}
