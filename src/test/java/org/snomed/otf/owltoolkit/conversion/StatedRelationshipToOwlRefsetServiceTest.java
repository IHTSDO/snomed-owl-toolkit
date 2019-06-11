package org.snomed.otf.owltoolkit.conversion;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.snomed.otf.owltoolkit.conversion.StatedRelationshipToOwlRefsetService.AxiomChangesGenerator;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomyLoader;
import org.snomed.otf.owltoolkit.util.InputStreamSet;
import org.snomed.otf.owltoolkit.util.OptionalFileInputStream;
import org.snomed.otf.snomedboot.testutil.ZipUtil;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class StatedRelationshipToOwlRefsetServiceTest {
	
	private StatedRelationshipToOwlRefsetService service ;
	
	@Before
	public void setUp() {
		service = new StatedRelationshipToOwlRefsetService();
		// Set up sequential identifiers for testing
		AtomicInteger sequentialTestId = new AtomicInteger(1);
		service.setIdentifierSupplier(() -> sequentialTestId.getAndIncrement() + "");
	}

	@Test
	public void convertStatedRelationshipsToOwlRefset() throws IOException, OWLOntologyCreationException, ConversionException {
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File rF2DeltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Add_Diabetes_delta");
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
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

	@Test
	public void convertAndReconcileStatedRelationshipsToOwlRefset() throws IOException, OWLOntologyCreationException, ConversionException {
		File midCycleSnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_MidAuthoringCycle_snapshot");
		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_CompleteOwl_snapshot");

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		service.convertStatedRelationshipsToOwlReRefsetAndReconcileWithPublishedArchive(new FileInputStream(midCycleSnapshotZip), new FileInputStream(baseRF2SnapshotZip), byteArrayOutputStream, "20190731");

		String owlRefset;
		try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))) {
			ZipEntry nextEntry = zipInputStream.getNextEntry();
			assertEquals("sct2_sRefset_OWLAxiomDelta_INT_20190731.txt", nextEntry.getName());
			owlRefset = StreamUtils.copyToString(zipInputStream, Charset.forName("UTF-8"));
		}

		assertFalse("Output should not contain the snomed axiom prefix", owlRefset.contains("<http://snomed.info/id/"));
		assertEquals(
				"id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\towlExpression\n" +
				"e35258b4-15d8-4fb2-bea5-dcc6b5e7de62\t20190731\t1\t900000000000207008\t733073007\t8801005\tSubClassOf(ObjectIntersectionOf(:73211009 ObjectSomeValuesFrom(:roleGroup ObjectSomeValuesFrom(:100106001 :100102001))) :8801005)\n" +
				"2de0e672-1231-40fb-ae9b-5d69454c6274\t\t1\t900000000000207008\t733073007\t276856001\tSubClassOf(:276856001 ObjectIntersectionOf(:362965005 ObjectSomeValuesFrom(:609096000 ObjectSomeValuesFrom(:363698007 :113331007))))\n" +
				"1\t\t1\t900000000000012004\t733073007\t762705008\tSubClassOf(:762705008 :410662002)\n" +
				"2\t\t1\t900000000000207008\t733073007\t8801005\tSubClassOf(:8801005 :73211009)\n" +
				"3\t\t1\t900000000000012004\t733073007\t762706009\tSubClassOf(:762706009 :410662002)\n" +
				"4\t\t1\t900000000000207008\t733073007\t73211009\tSubClassOf(:73211009 :362969004)\n" +
				"f64a5e6d-a8d6-4432-857e-999cce35b9ac\t\t0\t900000000000207008\t733073007\t362969004\tEquivalentClasses(:362969004 ObjectIntersectionOf(:404684003 ObjectSomeValuesFrom(:609096000 ObjectSomeValuesFrom(:363698007 :113331007))))\n",
				owlRefset);
	}

	@Test
	public void convertExtensionStatedRelationshipsToOwlAxiomRefset() throws Exception {
		File extensionPreviousSnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Extension_snapshot");
		File extensionCurrentDeltaZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Empty_delta");
		File intRF2CompleteOwlSnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_CompleteOwl_snapshot");
		
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		File result = service.convertExtensionStatedRelationshipsToOwlRefsetAndInactiveRelationshipsArchive(new InputStreamSet(new FileInputStream(intRF2CompleteOwlSnapshotZip), new FileInputStream(extensionPreviousSnapshotZip)), 
				new OptionalFileInputStream(extensionCurrentDeltaZip), byteArrayOutputStream, "20190901");
		
		String owlRefset;
		String statedRelationships;
		String statedRelationshipsNotConverted;
		try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))) {
			ZipEntry nextEntry = zipInputStream.getNextEntry();
			assertEquals("sct2_StatedRelationship_Delta_INT_20190901.txt", nextEntry.getName());
			statedRelationships = StreamUtils.copyToString(zipInputStream, Charset.forName("UTF-8"));
			nextEntry = zipInputStream.getNextEntry();
			assertEquals("sct2_sRefset_OWLAxiomDelta_INT_20190901.txt", nextEntry.getName());
			owlRefset = StreamUtils.copyToString(zipInputStream, Charset.forName("UTF-8"));
			assertEquals("sct2_StatedRelationships_Not_Converted.txt", result.getName());
			statedRelationshipsNotConverted = StreamUtils.copyToString(new FileInputStream(result), Charset.forName("UTF-8"));
		}

		assertFalse("Output should not contain the snomed axiom prefix", owlRefset.contains("<http://snomed.info/id/"));

		assertEquals(
				"id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\towlExpression\n" +
				"1\t\t1\t900101001\t733073007\t900101001\tSubClassOf(:900101001 :900000000000441003)\n" + 
				"2\t\t1\t900101001\t733073007\t18736003\tSubClassOf(:18736003 ObjectIntersectionOf(:12481008 :76145000 ObjectSomeValuesFrom(:609096000 ObjectIntersectionOf(ObjectSomeValuesFrom(:260686004 :129287005) ObjectSomeValuesFrom(:405813007 :84301002))) ObjectSomeValuesFrom(:609096000 ObjectIntersectionOf(ObjectSomeValuesFrom(:260686004 :281615006) ObjectSomeValuesFrom(:405813007 :25342003)))))\n",
				owlRefset);
		
		assertEquals(
				"id\teffectiveTime\tactive\tmoduleId\tsourceId\tdestinationId\trelationshipGroup\ttypeId\tcharacteristicTypeId\tmodifierId\n" +
				"600001001\t\t0\t900101001\t900101001\t900000000000441003\t0\t116680003\t900000000000010007\t900000000000451002\n" +
				"3992921024		0	900101001	18736003	76145000	0	116680003	900000000000010007	900000000000451002\n" +
				"3992922028		0	900101001	18736003	12481008	0	116680003	900000000000010007	900000000000451002\n" +
				"4340130021		0	900101001	18736003	281615006	1	260686004	900000000000010007	900000000000451002\n" + 
				"4340131020		0	900101001	18736003	25342003	1	405813007	900000000000010007	900000000000451002\n" +
				"4341684028		0	900101001	18736003	84301002	2	405813007	900000000000010007	900000000000451002\n" + 
				"4479068021		0	900101001	18736003	129287005	2	260686004	900000000000010007	900000000000451002\n",
				statedRelationships);
		
		assertEquals("id\teffectiveTime\tactive\tmoduleId\tsourceId\tdestinationId\trelationshipGroup\ttypeId\tcharacteristicTypeId\tmodifierId\n",
				statedRelationshipsNotConverted);
	}
	
	@Test
	public void testChangesWithTwoAxioms() throws Exception {
		AxiomChangesGenerator generator = new AxiomChangesGenerator();
		Set<OWLAxiom> previous = new HashSet<>();
		SnomedTaxonomyLoader snomedTaxonomyLoader = new SnomedTaxonomyLoader();
		OWLAxiom gci = snomedTaxonomyLoader.deserialiseAxiom("SubClassOf(:432685000 ObjectIntersectionOf(:763158003 ObjectSomeValuesFrom(:766939001 :773905006)))");
		previous.add(gci);
		OWLAxiom classAxiom = snomedTaxonomyLoader.deserialiseAxiom("EquivalentClasses(:432685000 ObjectIntersectionOf(:763158003 ObjectSomeValuesFrom(:411116001 :385268001) ObjectSomeValuesFrom(:609096000 ObjectSomeValuesFrom(:127489000 :387131008))))");
		previous.add(classAxiom);
		Map<OWLAxiom, String> axiomsIdMap = new HashMap<>();
		axiomsIdMap.put(classAxiom, "5149167f-a22d-4a22-b31b-c49c120f0c98");
		Set<OWLAxiom> current = new HashSet<>();
		current.add(classAxiom);
		OWLAxiom change = generator.findChanges(axiomsIdMap, previous, current);
		assertNull("It should be no changes.", change);
	}

}
