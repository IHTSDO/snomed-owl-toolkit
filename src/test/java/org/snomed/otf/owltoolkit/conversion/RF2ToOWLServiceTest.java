package org.snomed.otf.owltoolkit.conversion;

import org.junit.Test;
import org.snomed.otf.owltoolkit.testutil.ZipUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import static org.junit.Assert.*;

public class RF2ToOWLServiceTest {

	@Test
	public void convertRF2ArchiveToOWL() throws Exception {
		final RF2ToOWLService rf2ToOWLService = new RF2ToOWLService();

		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		rf2ToOWLService.convertRF2ArchiveToOWL("20180731", new FileInputStream(baseRF2SnapshotZip), byteArrayOutputStream);

		assertEquals("" +
						"Prefix(:=<http://snomed.info/id/>)\n" +
						"Prefix(owl:=<http://www.w3.org/2002/07/owl#>)\n" +
						"Prefix(rdf:=<http://www.w3.org/1999/02/22-rdf-syntax-ns#>)\n" +
						"Prefix(xml:=<http://www.w3.org/XML/1998/namespace>)\n" +
						"Prefix(xsd:=<http://www.w3.org/2001/XMLSchema#>)\n" +
						"Prefix(rdfs:=<http://www.w3.org/2000/01/rdf-schema#>)\n" +
						"\n" +
						"\n" +
						"Ontology(<http://snomed.info/sct/900000000000207008/version/20180731>\n" +
						"\n" +
						"Declaration(Class(:113331007))\n" +
						"Declaration(Class(:116680003))\n" +
						"Declaration(Class(:138875005))\n" +
						"Declaration(Class(:362969004))\n" +
						"Declaration(Class(:363698007))\n" +
						"Declaration(Class(:404684003))\n" +
						"Declaration(Class(:410662002))\n" +
						"Declaration(Class(:723594008))\n" +
						"Declaration(Class(:723596005))\n" +
						"Declaration(Class(:900000000000441003))\n" +
						"Declaration(ObjectProperty(:363698007))\n" +
						"Declaration(ObjectProperty(:609096000))\n" +
						"\n" +
						"############################\n" +
						"#   Classes\n" +
						"############################\n" +
						"\n" +
						"# Class: <http://snomed.info/id/113331007> (<http://snomed.info/id/113331007>)\n" +
						"\n" +
						"SubClassOf(:113331007 :138875005)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/116680003> (<http://snomed.info/id/116680003>)\n" +
						"\n" +
						"SubClassOf(:116680003 :900000000000441003)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/138875005> (<http://snomed.info/id/138875005>)\n" +
						"\n" +
						"SubClassOf(:138875005 owl:Thing)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/362969004> (<http://snomed.info/id/362969004>)\n" +
						"\n" +
						"EquivalentClasses(:362969004 ObjectIntersectionOf(:404684003 ObjectSomeValuesFrom(:609096000 ObjectSomeValuesFrom(:363698007 :113331007))))\n" +
						"\n" +
						"# Class: <http://snomed.info/id/363698007> (<http://snomed.info/id/363698007>)\n" +
						"\n" +
						"SubClassOf(:363698007 :410662002)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/404684003> (<http://snomed.info/id/404684003>)\n" +
						"\n" +
						"SubClassOf(:404684003 :138875005)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/410662002> (<http://snomed.info/id/410662002>)\n" +
						"\n" +
						"SubClassOf(:410662002 :900000000000441003)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/723594008> (<http://snomed.info/id/723594008>)\n" +
						"\n" +
						"SubClassOf(:723594008 :900000000000441003)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/723596005> (<http://snomed.info/id/723596005>)\n" +
						"\n" +
						"SubClassOf(:723596005 :723594008)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/900000000000441003> (<http://snomed.info/id/900000000000441003>)\n" +
						"\n" +
						"SubClassOf(:900000000000441003 :138875005)\n" +
						"\n" +
						"\n" +
						")",
				byteArrayOutputStream.toString());
	}

}