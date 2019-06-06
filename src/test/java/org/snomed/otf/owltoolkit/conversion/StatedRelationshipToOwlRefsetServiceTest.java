package org.snomed.otf.owltoolkit.conversion;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.snomed.otf.owltoolkit.util.OptionalFileInputStream;
import org.snomed.otf.snomedboot.testutil.ZipUtil;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class StatedRelationshipToOwlRefsetServiceTest {

	@Test
	public void convertStatedRelationshipsToOwlRefset() throws IOException, OWLOntologyCreationException, ConversionException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File rF2DeltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Add_Diabetes_delta");
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		StatedRelationshipToOwlRefsetService service = new StatedRelationshipToOwlRefsetService();

		// Set up sequential identifiers for testing
		AtomicInteger sequentialTestId = new AtomicInteger(1);
		service.setIdentifierSupplier(() -> sequentialTestId.getAndIncrement() + "");

		service.convertStatedRelationshipsToOwlRefsetAndInactiveRelationshipsArchive(new FileInputStream(baseRF2SnapshotZip), new OptionalFileInputStream(rF2DeltaZip),
				byteArrayOutputStream, "20180931");


		// Read files from zip
		String owlRefset;
		String statedRelationships;
		try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))) {
			ZipEntry nextEntry = zipInputStream.getNextEntry();
			assertEquals("sct2_StatedRelationship_Delta_INT_20180931.txt", nextEntry.getName());
			statedRelationships = StreamUtils.copyToString(zipInputStream, Charset.forName("UTF-8"));

			nextEntry = zipInputStream.getNextEntry();
			assertEquals("sct2_sRefset_OWLAxiomDelta_INT_20180931.txt", nextEntry.getName());
			owlRefset = StreamUtils.copyToString(zipInputStream, Charset.forName("UTF-8"));
		}


		assertFalse("Output should not contain the snomed axiom prefix", owlRefset.contains("<http://snomed.info/id/"));

		// Sequential identifiers used in this test rather than random UUIDs
		assertEquals(
				"id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\towlExpression\n" +
				"1\t\t1\t900000000000012004\t733073007\t410662002\tSubClassOf(:410662002 :900000000000441003)\n" +
				"2\t\t1\t900000000000012004\t733073007\t116680003\tSubClassOf(:116680003 :900000000000441003)\n" +
				"3\t\t1\t900000000000012004\t733073007\t723594008\tSubClassOf(:723594008 :900000000000441003)\n" +
				"4\t\t1\t900000000000012004\t733073007\t762705008\tSubClassOf(:762705008 :410662002)\n" +
				"5\t\t1\t900000000000207008\t733073007\t73211009\tSubClassOf(:73211009 :362969004)\n" +
				"6\t\t1\t900000000000012004\t733073007\t900000000000441003\tSubClassOf(:900000000000441003 :138875005)\n" +
				"7\t\t1\t900000000000207008\t733073007\t362969004\tEquivalentClasses(:362969004 ObjectIntersectionOf(:404684003 ObjectSomeValuesFrom(:609096000 ObjectSomeValuesFrom(:363698007 :113331007))))\n" +
				"8\t\t1\t900000000000012004\t733073007\t723596005\tSubClassOf(:723596005 :723594008)\n" +
				"9\t\t1\t900000000000012004\t733073007\t762706009\tSubClassOf(:762706009 :410662002)\n" +
				"10\t\t1\t900000000000207008\t733073007\t404684003\tSubClassOf(:404684003 :138875005)\n" +
				"11\t\t1\t900000000000012004\t733073007\t363698007\tSubObjectPropertyOf(:363698007 :762705008)\n" +
				"12\t\t1\t900000000000207008\t733073007\t113331007\tSubClassOf(:113331007 :138875005)\n",
				owlRefset);

		assertEquals(
				"id\teffectiveTime\tactive\tmoduleId\tsourceId\tdestinationId\trelationshipGroup\ttypeId\tcharacteristicTypeId\tmodifierId\n" +
				"100001001\t\t0\t900000000000012004\t900000000000441003\t138875005\t0\t116680003\t900000000000010007\t900000000000451002\n" +
				"100005001\t\t0\t900000000000012004\t410662002\t900000000000441003\t0\t116680003\t900000000000010007\t900000000000451002\n" +
				"100001101\t\t0\t900000000000012004\t762705008\t410662002\t0\t116680003\t900000000000010007\t900000000000451002\n" +
				"100001201\t\t0\t900000000000012004\t762706009\t410662002\t0\t116680003\t900000000000010007\t900000000000451002\n" +
				"100002001\t\t0\t900000000000012004\t116680003\t900000000000441003\t0\t116680003\t900000000000010007\t900000000000451002\n" +
				"100003001\t\t0\t900000000000012004\t723594008\t900000000000441003\t0\t116680003\t900000000000010007\t900000000000451002\n" +
				"100004001\t\t0\t900000000000012004\t723596005\t723594008\t0\t116680003\t900000000000010007\t900000000000451002\n" +
				"100006001\t\t0\t900000000000012004\t363698007\t762705008\t0\t116680003\t900000000000010007\t900000000000451002\n" +
				"100007001\t\t0\t900000000000207008\t113331007\t138875005\t0\t116680003\t900000000000010007\t900000000000451002\n" +
				"100008001\t\t0\t900000000000207008\t404684003\t138875005\t0\t116680003\t900000000000010007\t900000000000451002\n" +
				"100009001\t\t0\t900000000000207008\t362969004\t404684003\t0\t116680003\t900000000000010007\t900000000000451002\n" +
				"100010001\t\t0\t900000000000207008\t362969004\t113331007\t0\t363698007\t900000000000010007\t900000000000451002\n",
				statedRelationships);
	}

}
