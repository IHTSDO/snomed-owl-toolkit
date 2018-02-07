package org.snomed.otf.owltoolkit;

import com.google.common.collect.Lists;
import org.snomed.otf.owltoolkit.conversion.ConversionException;
import org.snomed.otf.owltoolkit.conversion.RF2ToOWLService;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Command line application for RF2 to OWL file conversion.
 */
public class Application {

	private static final String ARG_HELP = "-help";
	private static final String ARG_RF2_ZIP = "-rf2-snap-zip";
	private static final String ARG_VERSION = "-version";
	private static final String ARG_WITHOUT_ANNOTATIONS = "-without-annotations";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

	private boolean deleteOntologyFileOnExit;

	public static void main(String[] argsArray) throws FileNotFoundException, ConversionException {
		try {
			new Application().run(argsArray);
		} catch (Exception e) {
			System.exit(1);
		}
		System.exit(0);
	}

	public void run(String[] argsArray) throws IOException, ConversionException {
		List<String> args = Lists.newArrayList(argsArray);

		if (args.isEmpty() || args.contains(ARG_HELP)) {
			// Help
			printHelp();
		} else {
			// RF2 to OWL
			// Parameter validation
			String rf2ArchivePath = getRequiredParameterValue(ARG_RF2_ZIP, args);
			File rf2ArchiveFile = new File(rf2ArchivePath);
			assertTrue(rf2ArchiveFile.getAbsolutePath() + " should be a file.", rf2ArchiveFile.isFile());

			String versionDate = getRequiredParameterValue(ARG_VERSION, args);
			boolean includeFSNs = !args.contains(ARG_WITHOUT_ANNOTATIONS);

			// Conversion
			String outputFilePath = "ontology-" + DATE_FORMAT.format(new Date()) + ".owl";
			File ontologyOutputFile = new File(outputFilePath);
			if (deleteOntologyFileOnExit) {
				ontologyOutputFile.deleteOnExit();
			}
			try (FileInputStream rf2ArchiveStream = new FileInputStream(rf2ArchiveFile);
				 FileOutputStream outputStream = new FileOutputStream(ontologyOutputFile)) {
				new RF2ToOWLService().convertRF2ArchiveToOWL(versionDate, includeFSNs, rf2ArchiveStream, outputStream);
			} catch (IOException e) {
				System.err.println("Failed to close input or output stream.");
				e.printStackTrace();
				throw e;
			}
			System.out.println("OWL Ontology file written to - " + outputFilePath);
		}
	}

	private void printHelp() {
		System.out.println(
				"Usage:\n" +
						pad(ARG_HELP) +
						"Print this help message.\n" +

						pad(ARG_RF2_ZIP + " <path>") +
						"Path to the SNOMED CT RF2 archive containing Snapshot files.\n" +

						pad(ARG_VERSION + " <version>") +
						"Version date e.g. 20180731.\n" +

						pad(ARG_WITHOUT_ANNOTATIONS) +
						"Don't include Fully Specified Name annotations in OWL file (smaller file size).\n" +
				"");
	}

	private String getRequiredParameterValue(String paramName, List<String> args) {
		assertTrue("Expecting parameter " + paramName, args.contains(paramName));
		int valueIndex = args.indexOf(paramName) + 1;
		assertTrue("Expecting a value with parameter " + paramName, valueIndex < args.size());
		return args.get(valueIndex);
	}

	private void assertTrue(String message, boolean bool) {
		if (!bool) {
			System.out.println();
			System.err.println(message);
			System.out.println();
			printHelp();
			throw new IllegalStateException();
		}
	}

	private String pad(String argHelp) {
		StringBuilder argHelpBuilder = new StringBuilder(" " + argHelp);
		while (argHelpBuilder.length() < 25) {
			argHelpBuilder.append(" ");
		}
		return argHelpBuilder.toString();
	}


	protected void deleteOntologyFileOnExit() {
		this.deleteOntologyFileOnExit = true;
	}
}
