package org.snomed.otf.owltoolkit.taxonomy;

import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.junit.Test;
import org.snomed.otf.owltoolkit.util.InputStreamSet;
import org.snomed.otf.snomedboot.testutil.ZipUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.*;

public class SnomedTaxonomyBuilderTest {

	@Test
	public void testBuildTaxonomyWithInactiveDuplicateAxiom() throws IOException, ReleaseImportException {
		final SnomedTaxonomyBuilder builder = new SnomedTaxonomyBuilder();
		File intSnapshot = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/duplicate-axiom-test/int-snapshot");
		File usSnapshot = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/duplicate-axiom-test/us-snapshot");
		File usDelta = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/duplicate-axiom-test/us-delta");
		final SnomedTaxonomy snomedTaxonomy = builder.build(new InputStreamSet(new FileInputStream(intSnapshot), new FileInputStream(usSnapshot)),
				new FileInputStream(usDelta), false);

		assertEquals("The donated axiom must remain present after the US duplicate is made inactive", 1, snomedTaxonomy.getConceptAxiomMap().get(362969004L).size());
	}

}