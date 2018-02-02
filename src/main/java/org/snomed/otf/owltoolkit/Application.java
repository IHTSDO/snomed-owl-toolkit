package org.snomed.otf.owltoolkit;

import com.google.common.collect.Lists;
import org.snomed.otf.owltoolkit.conversion.ConversionException;
import org.snomed.otf.owltoolkit.conversion.RF2ToOWLService;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Command line application for RF2 to OWL file conversion.
 */
public class Application {

	private static final String ARG_HELP = "-help";
	private static final String ARG_RF2_ZIP = "-rf2-snap-zip";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

	public static void main(String[] argsArray) throws FileNotFoundException, ConversionException {
		List<String> args = Lists.newArrayList(argsArray);

		if (args.isEmpty() || args.contains(ARG_HELP)) {
			// Help
			System.out.println(
					"Usage:\n" +
							" " + ARG_HELP + "              Print this help message.\n" +
							" " + ARG_RF2_ZIP + " <path>    Path to the SNOMED CT RF2 archive containing Snapshot files.\n" +
					"");
		} else {
			// RF2 to OWL
			// Parameter validation
			assertTrue("Expecting 1 key and value argument pair separated by a space.",
					args.size() == 2);
			assertTrue("Expecting argument " + ARG_RF2_ZIP,
					args.contains(ARG_RF2_ZIP));
			String rf2ArchivePath = args.get(1);
			File rf2ArchiveFile = new File(rf2ArchivePath);
			assertTrue(rf2ArchiveFile.getAbsolutePath() + " should be a file.", rf2ArchiveFile.isFile());

			// Conversion
			String outputFilePath = "ontology-" + DATE_FORMAT.format(new Date()) + ".owl";
			try (FileInputStream rf2ArchiveStream = new FileInputStream(rf2ArchiveFile);
				 FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
				new RF2ToOWLService().convertRF2ArchiveToOWL(rf2ArchiveStream, outputStream);
			} catch (IOException e) {
				System.err.println("Failed to close input or output stream.");
				e.printStackTrace();
				System.exit(1);
			}
			System.out.println("OWL Ontology file written to - " + outputFilePath);
			System.exit(0);
		}
	}

	private static void assertTrue(String message, boolean bool) {
		if (!bool) {
			System.err.println(message);
			System.exit(1);
		}
	}

}
