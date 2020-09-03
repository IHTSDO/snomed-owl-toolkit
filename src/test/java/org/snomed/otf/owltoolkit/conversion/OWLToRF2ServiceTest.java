package org.snomed.otf.owltoolkit.conversion;

import org.junit.Test;
import org.snomed.otf.owltoolkit.util.InputStreamSet;
import org.snomed.otf.owltoolkit.util.OptionalFileInputStream;
import org.snomed.otf.snomedboot.testutil.ZipUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class OWLToRF2ServiceTest {

	@Test
	public void convertRF2ArchiveToOWL() throws Exception {
		final OWLtoRF2Service service = new OWLtoRF2Service();

		InputStream owlFileStream = getClass().getResourceAsStream("/owl-to-rf2/ontology.owl");
		File rf2Zip = Files.createTempFile("owl-to-rf2-directory_" + new Date().getTime(), ".zip").toFile();
		service.writeToRF2(owlFileStream, new FileOutputStream(rf2Zip));
	}

}
