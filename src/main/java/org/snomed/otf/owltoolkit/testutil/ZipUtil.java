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
package org.snomed.otf.owltoolkit.testutil;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

	public static File zipDirectoryRemovingCommentsAndBlankLines(String directoryPath) throws IOException {
		File directory = new File(directoryPath);
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("This is not a directory " + directory.getAbsolutePath());
		}
		Path zip = Files.createTempFile("zipped-directory_" + new Date().getTime(), "zip");
		File zipFile = zip.toFile();
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
			BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(zipOutputStream));
			Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
					File file = path.toFile();
					zipOutputStream.putNextEntry(new ZipEntry(file.getName()));
					try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
						String line;
						while ((line = bufferedReader.readLine()) != null) {
							if (!line.isEmpty() && !line.startsWith("#")) {
								bufferedWriter.write(line);
								bufferedWriter.newLine();
							}
						}
					}
					bufferedWriter.flush();
					return FileVisitResult.CONTINUE;
				}
			});
		}
		return zipFile;
	}

	public static InputStream getZipEntryStreamOrThrow(File file, String filenamePart) throws IOException {
		ZipFile zipFile = new ZipFile(file);
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			if (entry.getName().contains(filenamePart)) {
				return zipFile.getInputStream(entry);
			}
		}
		throw new FileNotFoundException("No zip file entry found matching filename part '" + filenamePart + "'");
	}
}
