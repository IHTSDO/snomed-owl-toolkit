package org.snomed.otf.owltoolkit.service;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.util.InputStreamSet;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Set;

public class RF2ExtractionService {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public void extractConcepts(InputStreamSet rf2SnapshotArchives, Set<Long> conceptIds, File outputDirectory)
			throws ReleaseImportException, IOException {

		logger.info("Extracting {} concepts from RF2.", conceptIds.size());
		ReleaseImporter releaseImporter = new ReleaseImporter();
		String dateString = ClassificationResultsWriter.DATE_FORMAT.format(new Date());
		try (RF2ExtractionWriter extractionWriter = new RF2ExtractionWriter(conceptIds, dateString, outputDirectory)) {
			releaseImporter.loadEffectiveSnapshotReleaseFileStreams(rf2SnapshotArchives.getFileInputStreams(), LoadingProfile.complete, extractionWriter, false);
		}
		logger.info("Extraction complete.");
	}

	public static void main(String[] args) throws IOException, ReleaseImportException {
		File outputDirectory = new File("rf2-extract");
		FileUtils.deleteDirectory(outputDirectory);
		new RF2ExtractionService().extractConcepts(
				new InputStreamSet(new File("../../release/SnomedCT_InternationalRF2_PRODUCTION_20200731T120000Z.zip")),
				Sets.newHashSet(404684003L, 195967001L), outputDirectory);
	}
}
