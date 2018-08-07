package org.snomed.otf.owltoolkit.conversion;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.snomed.otf.owltoolkit.util.OptionalFileInputStream;
import org.snomed.otf.snomedboot.testutil.ZipUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class StatedRelationshipToOwlRefsetServiceTest {

	@Test
	public void convertStatedRelationshipsToOwlRefset() throws IOException, OWLOntologyCreationException, ConversionException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		StatedRelationshipToOwlRefsetService service = new StatedRelationshipToOwlRefsetService();

		// Set up sequential identifiers for testing
		AtomicInteger sequentialTestId = new AtomicInteger(1);
		service.setIdentifierSupplier(() -> sequentialTestId.getAndIncrement() + "");

		service.convertStatedRelationshipsToOwlRefset(new FileInputStream(baseRF2SnapshotZip), new OptionalFileInputStream(null), byteArrayOutputStream);

		// Sequential identifiers used in this test rather than random UUIDs
		String actualOutput = byteArrayOutputStream.toString();

		assertFalse("Output should not contain the snomed axiom prefix", actualOutput.contains("<http://snomed.info/id/"));

		assertEquals(
				"id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\towlExpression\n" +
				"1\t\t1\t900000000000012004\t733073007\t116680003\tSubClassOf(:116680003 :900000000000441003)\n" +
				"2\t\t1\t900000000000012004\t733073007\t723594008\tSubClassOf(:723594008 :900000000000441003)\n" +
				"3\t\t1\t900000000000012004\t733073007\t410662002\tSubClassOf(:410662002 :900000000000441003)\n" +
				"4\t\t1\t900000000000012004\t733073007\t363698007\tSubClassOf(:363698007 :410662002)\n" +
				"5\t\t1\t900000000000207008\t733073007\t362969004\tEquivalentClasses(:362969004 ObjectIntersectionOf(:404684003 ObjectSomeValuesFrom(:609096000 ObjectSomeValuesFrom(:363698007 :113331007))))\n" +
				"6\t\t1\t900000000000012004\t733073007\t723596005\tSubClassOf(:723596005 :723594008)\n" +
				"7\t\t1\t900000000000207008\t733073007\t404684003\tSubClassOf(:404684003 :138875005)\n" +
				"8\t\t1\t900000000000012004\t733073007\t900000000000441003\tSubClassOf(:900000000000441003 :138875005)\n" +
				"9\t\t1\t900000000000012004\t733073007\t138875005\tSubClassOf(:138875005 owl:Thing)\n" +
				"10\t\t1\t900000000000207008\t733073007\t113331007\tSubClassOf(:113331007 :138875005)\n",
				actualOutput);
	}
}
