/*
 * Copyright 2017 SNOMED International, http://snomed.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.snomed.otf.owltoolkit.service.classification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.snomedboot.testutil.ZipUtil;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestFileUtil {

	private static final String EQUIVALENT_DELTA = "der2_sRefset_EquivalentConceptSimpleMapDelta_Classification_";
	private static final String RELATIONSHIP_DELTA = "sct2_Relationship_Delta_Classification_";
	private static final String RELATIONSHIP_CONCRETE_VALUES_DELTA = "sct2_RelationshipConcreteValues_Delta_Classification_";
	private static final Logger LOGGER = LoggerFactory.getLogger(TestFileUtil.class);

	public static List<String> readInferredRelationshipLinesTrim(File zipFile) throws IOException {
		return readLinesTrim(zipFile, RELATIONSHIP_DELTA);
	}

	public static List<String> readInferredRelationshipConcreteValuesLinesTrim(File zipFile) throws IOException {
		return readLinesTrim(zipFile, RELATIONSHIP_CONCRETE_VALUES_DELTA);
	}

	static List<String> readEquivalentConceptLinesTrim(File zipFile) throws IOException {
		return readLinesTrim(zipFile, EQUIVALENT_DELTA);
	}

	private static List<String> readLinesTrim(File zipFile, String zipEntryNamePrefix) throws IOException {
		List<String> lines = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(ZipUtil.getZipEntryStreamOrThrow(zipFile, zipEntryNamePrefix)))) {
			int totalColumn = 0;
			if (zipEntryNamePrefix.equals(RELATIONSHIP_DELTA) || zipEntryNamePrefix.equals(RELATIONSHIP_CONCRETE_VALUES_DELTA)) {
				totalColumn = 10;
			} else if (EQUIVALENT_DELTA.equals(zipEntryNamePrefix)) {
				totalColumn = 7;
			}
			String msg = String.format("There should be %d columns separated by tab in line \n", totalColumn);
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				String[] splits = line.split("\t", -1);
				assertEquals(msg + line, splits.length, totalColumn);
				lines.add(line.trim());
			}
		}
		return lines;
	}

	public static File newTemporaryFile() throws IOException {
		return Files.createTempFile(new Date().getTime() + "", ".txt").toFile();
	}

	public static void addFilesToZipFlatteningPaths(File directoryOfFiles, File existingZipFile, boolean allowEntryOverwriting) throws IOException {

		List<String> newFilenames = new ArrayList<>();
		List<String> newPaths = new ArrayList<>();
		Files.walk(directoryOfFiles.toPath())
				.forEach(path -> {
					if (path.toFile().isFile()) {
						newFilenames.add(path.toFile().getName());
						String absolutePath = path.toFile().getAbsolutePath();
						newPaths.add(absolutePath.replace(directoryOfFiles.getAbsolutePath(), ""));
					}
				});

		File tempFile = Files.createTempFile("temp-file", ".zip").toFile();
		try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(existingZipFile));
			 ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(tempFile))) {

			// Copy existing zip entries to new zip file
			List<String> zipEntriesFilenames = new ArrayList<>();
			ZipEntry nextEntry;
			while ((nextEntry = zipInputStream.getNextEntry()) != null) {
				String existingEntry = nextEntry.getName();
				existingEntry = existingEntry.replaceFirst(".*/", "");
				if (newFilenames.contains(existingEntry) && !allowEntryOverwriting) {
					LOGGER.info("Adding existing zip entry {}", existingEntry);
					zipEntriesFilenames.add(existingEntry);
					zipOutputStream.putNextEntry(nextEntry);
					StreamUtils.copy(zipInputStream, zipOutputStream);
					zipOutputStream.closeEntry();
				}
			}

			// Copy files to new zip file
			for (String entryPath : newPaths) {
				String entryFilename = entryPath.replaceFirst(".*/", "");
				if (!zipEntriesFilenames.contains(entryFilename)) {
					LOGGER.info("Adding new zip entry {}", entryFilename);
					zipOutputStream.putNextEntry(new ZipEntry(entryFilename));
					StreamUtils.copy(new FileInputStream(new File(directoryOfFiles, entryPath)), zipOutputStream);
					zipOutputStream.closeEntry();
				}
			}
		}

		Files.move(tempFile.toPath(), existingZipFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	public static List<String> readFileToString(String filePath) throws IOException {
		List<String> lines = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader("src/test/resources/" + filePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		}
		return lines;
	}
}
