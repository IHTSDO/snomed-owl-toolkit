package org.snomed.otf.owltoolkit.conversion;

import org.junit.Test;
import org.snomed.otf.owltoolkit.service.classification.TestFileUtil;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OWLToRF2ServiceTest {

	@Test
	public void convertRF2ArchiveToOWL() throws Exception {
		final OWLtoRF2Service service = new OWLtoRF2Service();

		InputStream owlFileStream = getClass().getResourceAsStream("/owl-to-rf2/ontology.owl");
		File rf2Zip = Files.createTempFile("owl-to-rf2-directory_" + new Date().getTime(), ".zip").toFile();
		service.writeToRF2(owlFileStream, new FileOutputStream(rf2Zip), new GregorianCalendar(2020, Calendar.SEPTEMBER, 3).getTime());

		try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(rf2Zip))) {
			ZipEntry nextEntry = zipInputStream.getNextEntry();
			assertNotNull(nextEntry);

			List<String> expectedLines = TestFileUtil.readFileToString("/owl-to-rf2/expected-output/concept.txt");
			assertEquals("SnomedCT/Snapshot/sct2_Concept_Snapshot_INT_20200903.txt", nextEntry.getName());
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream))) {
				String line;
				int lineNum = 0;
				while ((line = reader.readLine()) != null) {
					assertEquals(expectedLines.get(lineNum), line);
					lineNum++;
				}
			}

		}
	}

}
