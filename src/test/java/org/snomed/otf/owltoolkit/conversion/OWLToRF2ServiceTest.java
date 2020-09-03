package org.snomed.otf.owltoolkit.conversion;

import org.junit.Test;
import org.snomed.otf.owltoolkit.service.classification.TestFileUtil;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OWLToRF2ServiceTest {

	@Test
	public void convertOWLFileToRF2Archive() throws Exception {
		final OWLtoRF2Service service = new OWLtoRF2Service();

		InputStream owlFileStream = getClass().getResourceAsStream("/owl-to-rf2/ontology.owl");
		File rf2Zip = Files.createTempFile("owl-to-rf2-directory_" + new Date().getTime(), ".zip").toFile();
		service.writeToRF2(owlFileStream, new FileOutputStream(rf2Zip), new GregorianCalendar(2020, Calendar.SEPTEMBER, 3).getTime());

		try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(rf2Zip))) {
			ZipEntry nextEntry = zipInputStream.getNextEntry();
			assertNotNull(nextEntry);
			assertEquals("SnomedCT/Snapshot/Terminology/sct2_Concept_Snapshot_INT_20200903.txt", nextEntry.getName());
			assertAllLines(TestFileUtil.readFileToString("/owl-to-rf2/expected-output/concept.txt"), zipInputStream);

			nextEntry = zipInputStream.getNextEntry();
			assertNotNull(nextEntry);
			assertEquals("SnomedCT/Snapshot/Terminology/sct2_Description_Snapshot-en_INT_20200903.txt", nextEntry.getName());
			assertAllLines(TestFileUtil.readFileToString("/owl-to-rf2/expected-output/description.txt"), zipInputStream);

			nextEntry = zipInputStream.getNextEntry();
			assertNotNull(nextEntry);
			assertEquals("SnomedCT/Snapshot/Refset/Language/der2_cRefset_LanguageSnapshot-en_INT_20200903.txt", nextEntry.getName());
			assertAllLines(TestFileUtil.readFileToString("/owl-to-rf2/expected-output/language-refset.txt"), zipInputStream, true);

			nextEntry = zipInputStream.getNextEntry();
			assertNotNull(nextEntry);
			assertEquals("SnomedCT/Snapshot/Terminology/sct2_sRefset_OWLExpressionSnapshot_INT_20200903.txt", nextEntry.getName());
			assertAllLines(TestFileUtil.readFileToString("/owl-to-rf2/expected-output/owl-axiom-refset.txt"), zipInputStream, true);
		}
	}

	private void assertAllLines(List<String> expectedLines, ZipInputStream zipInputStream) throws IOException {
		assertAllLines(expectedLines, zipInputStream, false);
	}

	private void assertAllLines(List<String> expectedLines, ZipInputStream zipInputStream, boolean ignoreFirstColumn) throws IOException {
		// Leave reader open, do not close zip input stream.
		BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream));
		String line;
		int lineNum = 0;
		while ((line = reader.readLine()) != null) {
			String expected = expectedLines.get(lineNum);
			if (ignoreFirstColumn) {
				line = removeFirstColumn(line);
				expected = removeFirstColumn(expected);
			}
			assertEquals(format("Line %s must match expected line.", lineNum + 1), expected, line);
			lineNum++;
		}
		assertEquals("Expected number of lines must match actual number of lines.", expectedLines.size(), lineNum);
	}

	private String removeFirstColumn(String line) {
		return line.substring(line.indexOf("\t"));
	}

}
