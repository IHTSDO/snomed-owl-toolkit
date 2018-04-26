package org.snomed.otf.owltoolkit;

import com.google.common.collect.Lists;
import org.snomed.otf.owltoolkit.conversion.ConversionException;
import org.snomed.otf.owltoolkit.conversion.RF2ToOWLService;
import org.snomed.otf.owltoolkit.ontology.OntologyService;
import org.snomed.otf.owltoolkit.util.InputStreamSet;
import org.snomed.otf.owltoolkit.util.OptionalInputStream;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Command line application for RF2 to OWL file conversion.
 */
public class Application {

	private static final String ARG_HELP = "-help";
	private static final String ARG_RF2_SNAPSHOT_ARCHIVES = "-rf2-snapshot-archives";
	private static final String ARG_RF2_AUTHORING_DELTA_ARCHIVE = "-rf2-authoring-delta-archive";
	private static final String ARG_URI = "-uri";
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
			Set<File> snapshotFiles = gatherFiles(getRequiredParameterValue(ARG_RF2_SNAPSHOT_ARCHIVES, args));

			File deltaFile = null;
			{
				Set<File> deltaFiles = gatherFiles(getParameterValue(ARG_RF2_AUTHORING_DELTA_ARCHIVE, args));
				if (!deltaFiles.isEmpty()) {
					assertTrue("Expecting a single delta archive file, got " + deltaFiles.size(), deltaFiles.size() == 1);
					deltaFile = deltaFiles.iterator().next();
				}
			}

			String ontologyUri = getParameterValue(ARG_URI, args);
			if (ontologyUri == null) {
				ontologyUri = OntologyService.SNOMED_INTERNATIONAL_EDITION_URI;
			}

			String versionDate = getParameterValue(ARG_VERSION, args);
			if (versionDate == null) {
				versionDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
			}

			boolean includeFSNs = !args.contains(ARG_WITHOUT_ANNOTATIONS);

			System.out.println();
			System.out.println("Creating Ontology using the following options:");
			System.out.println("  Snapshot archives: " + snapshotFiles);
			System.out.println("  Delta archive: " + (deltaFile == null ? "-none-" : deltaFile));
			System.out.println("  Ontology URI: " + ontologyUri);
			System.out.println("  Ontology Version: " + versionDate);
			System.out.println("  Include FSN Annotations: " + includeFSNs);
			System.out.println();

			// Conversion
			String outputFilePath = "ontology-" + DATE_FORMAT.format(new Date()) + ".owl";
			File ontologyOutputFile = new File(outputFilePath);
			if (deleteOntologyFileOnExit) {
				ontologyOutputFile.deleteOnExit();
			}
			try (InputStreamSet snapshotStreams = new InputStreamSet(snapshotFiles);
				 OptionalInputStream deltaStream = new OptionalInputStream(deltaFile);
				 FileOutputStream outputStream = new FileOutputStream(ontologyOutputFile)) {

				new RF2ToOWLService().convertRF2ArchiveToOWL(ontologyUri, versionDate, includeFSNs, snapshotStreams, deltaStream, outputStream);
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
						"\n" +

						pad(ARG_RF2_SNAPSHOT_ARCHIVES + " <path>") +
						"Comma separated paths of zip files containing RF2 Snapshot files to be loaded. \n" +
						pad("") + "At least one Snapshot archive is required.\n" +
						"\n" +

						pad(ARG_RF2_AUTHORING_DELTA_ARCHIVE + " <path>") +
						"(Optional) Path to a zip file containing RF2 Delta files to be applied on top \n" +
						pad("") + "of the Snapshots. This is helpful during an authoring cycle.\n" +
						"\n" +

						pad(ARG_URI + " <uri>") +
						"(Optional) URI for the ontology identifier.\n" +
						pad("") + "Defaults to " + OntologyService.SNOMED_INTERNATIONAL_EDITION_URI + ".\n" +
						"\n" +

						pad(ARG_VERSION + " <version>") +
						"(Optional) Date for the ontology version e.g. 20180731.\n" +
						pad("") + "Defaults to today's date.\n" +
						"\n" +

						pad(ARG_WITHOUT_ANNOTATIONS) +
						"(Optional) Flag to omit Fully Specified Name annotations from the ontology \n" +
						pad("") + "resulting in a smaller file size.\n" +
				"");
	}

	private String getParameterValue(String paramName, List<String> args) {
		if (args.indexOf(paramName) > -1) {
			return args.get(args.indexOf(paramName) + 1);
		}
		return null;
	}

	private String getRequiredParameterValue(String paramName, List<String> args) {
		assertTrue("Expecting parameter " + paramName, args.contains(paramName));
		int valueIndex = args.indexOf(paramName) + 1;
		assertTrue("Expecting a value with parameter " + paramName, valueIndex < args.size());
		return args.get(valueIndex);
	}

	private Set<File> gatherFiles(String filePaths) {
		Set<File> rf2ArchiveFiles = new HashSet<>();
		if (filePaths != null) {
			String[] rf2ArchivePathStrings = filePaths.split("\\,");
			for (String rf2ArchivePath : rf2ArchivePathStrings) {
				File rf2ArchiveFile = new File(rf2ArchivePath);
				assertTrue(rf2ArchiveFile.getAbsolutePath() + " should be a file.", rf2ArchiveFile.isFile());
				rf2ArchiveFiles.add(rf2ArchiveFile);
			}
		}
		return rf2ArchiveFiles;
	}

	private void assertTrue(String message, boolean bool) {
		if (!bool) {
			System.out.println();
			System.err.println(message);
			System.out.println();
			printHelp();
			throw new IllegalArgumentException();
		}
	}

	private String pad(String argHelp) {
		StringBuilder argHelpBuilder = new StringBuilder(" " + argHelp);
		while (argHelpBuilder.length() < 40) {
			argHelpBuilder.append(" ");
		}
		return argHelpBuilder.toString();
	}


	protected void deleteOntologyFileOnExit() {
		this.deleteOntologyFileOnExit = true;
	}
}
