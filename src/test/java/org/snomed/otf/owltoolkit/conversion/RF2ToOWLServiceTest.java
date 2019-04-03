package org.snomed.otf.owltoolkit.conversion;

import org.junit.Test;
import org.snomed.otf.owltoolkit.util.InputStreamSet;
import org.snomed.otf.owltoolkit.util.OptionalFileInputStream;
import org.snomed.otf.snomedboot.testutil.ZipUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import static org.junit.Assert.assertEquals;

public class RF2ToOWLServiceTest {

	@Test
	public void convertRF2ArchiveToOWL() throws Exception {
		final RF2ToOWLService rf2ToOWLService = new RF2ToOWLService();

		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final FileInputStream rf2ArchiveStream = new FileInputStream(baseRF2SnapshotZip);
		rf2ToOWLService.convertRF2ArchiveToOWL(null, "20180731", true, new InputStreamSet(rf2ArchiveStream), new OptionalFileInputStream(null), byteArrayOutputStream);

		assertEquals("" +
						"#############################################################################################################\n" +
						"# © International Health Terminology Standards Development Organisation 2002-2019. All rights reserved.\n" +
						"# SNOMED CT® was originally created by the College of American Pathologists.\n" +
						"# \"SNOMED\" and \"SNOMED CT\" are registered trademarks of International Health Terminology Standards\n" +
						"# Development Organisation, trading as SNOMED International.\n" +
						"#\n" +
						"# SNOMED CT has been created by combining SNOMED RT and a computer based nomenclature and classification\n" +
						"# known as Clinical Terms Version 3, formerly known as Read Codes Version 3, which was created on behalf of\n" +
						"# the UK Department of Health and is Crown copyright.\n" +
						"#\n" +
						"# This OWL Ontology contains portions of SNOMED CT® distributed by SNOMED International, which is subject to\n" +
						"# the SNOMED CT® Affiliate License, details of which may be found at\n" +
						"# https://www.snomed.org/snomed-ct/get-snomed\n" +
						"#############################################################################################################\n" +
						"\n" +
						"Prefix(test:=<http://test.com/test/>)\n" +
						"Prefix(:=<http://snomed.info/id/>)\n" +
						"Prefix(owl:=<http://www.w3.org/2002/07/owl#>)\n" +
						"Prefix(rdf:=<http://www.w3.org/1999/02/22-rdf-syntax-ns#>)\n" +
						"Prefix(xml:=<http://www.w3.org/XML/1998/namespace>)\n" +
						"Prefix(xsd:=<http://www.w3.org/2001/XMLSchema#>)\n" +
						"Prefix(rdfs:=<http://www.w3.org/2000/01/rdf-schema#>)\n" +
						"\n" +
						"\n" +
						"Ontology(<http://snomed.info/sct/900000000000207008>\n" +
						"<http://snomed.info/sct/900000000000207008/version/20180731>\n" +
						"\n" +
						"Declaration(Class(:113331007))\n" +
						"Declaration(Class(:116680003))\n" +
						"Declaration(Class(:138875005))\n" +
						"Declaration(Class(:362969004))\n" +
						"Declaration(Class(:404684003))\n" +
						"Declaration(Class(:410662002))\n" +
						"Declaration(Class(:723594008))\n" +
						"Declaration(Class(:723596005))\n" +
						"Declaration(Class(:900000000000441003))\n" +
						"Declaration(ObjectProperty(:363698007))\n" +
						"Declaration(ObjectProperty(:609096000))\n" +
						"Declaration(ObjectProperty(:762705008))\n" +
						"\n" +
						"############################\n" +
						"#   Object Properties\n" +
						"############################\n" +
						"\n" +
						"# Object Property: <http://snomed.info/id/363698007> (Finding site (attribute))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :363698007 \"Finding site (attribute)\"^^xsd:string)\n" +
						"SubObjectPropertyOf(:363698007 :762705008)\n" +
						"\n" +
						"\n" +
						"\n" +
						"############################\n" +
						"#   Classes\n" +
						"############################\n" +
						"\n" +
						"# Class: <http://snomed.info/id/113331007> (Structure of endocrine system (body structure))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :113331007 \"Structure of endocrine system (body structure)\"^^xsd:string)\n" +
						"SubClassOf(:113331007 :138875005)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/116680003> (Is a (attribute))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :116680003 \"Is a (attribute)\"^^xsd:string)\n" +
						"SubClassOf(:116680003 :900000000000441003)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/138875005> (SNOMED CT Concept (SNOMED RT+CTV3))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :138875005 \"SNOMED CT Concept (SNOMED RT+CTV3)\"^^xsd:string)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/362969004> (Disorder of endocrine system (disorder))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :362969004 \"Disorder of endocrine system (disorder)\"^^xsd:string)\n" +
						"EquivalentClasses(:362969004 ObjectIntersectionOf(:404684003 ObjectSomeValuesFrom(:609096000 ObjectSomeValuesFrom(:363698007 :113331007))))\n" +
						"\n" +
						"# Class: <http://snomed.info/id/404684003> (Clinical finding (finding))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :404684003 \"Clinical finding (finding)\"^^xsd:string)\n" +
						"SubClassOf(:404684003 :138875005)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/410662002> (Concept model attribute (attribute))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :410662002 \"Concept model attribute (attribute)\"^^xsd:string)\n" +
						"SubClassOf(:410662002 :900000000000441003)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/723594008> (All precoordinated SNOMED CT content (foundation metadata concept))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :723594008 \"All precoordinated SNOMED CT content (foundation metadata concept)\"^^xsd:string)\n" +
						"SubClassOf(:723594008 :900000000000441003)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/723596005> (All SNOMED CT content (foundation metadata concept))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :723596005 \"All SNOMED CT content (foundation metadata concept)\"^^xsd:string)\n" +
						"SubClassOf(:723596005 :723594008)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/900000000000441003> (SNOMED CT Model Component (metadata))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :900000000000441003 \"SNOMED CT Model Component (metadata)\"^^xsd:string)\n" +
						"SubClassOf(:900000000000441003 :138875005)\n" +
						"\n" +
						"\n" +
						")",
				byteArrayOutputStream.toString());
	}

	@Test
	/*
		This test introduces '99001001 | Fictitious data attribute (attribute)' which is a child of '762706009 | Concept model data attribute (attribute)'
		Here we can see the output of Declaration(DataProperty()) items which is only possible once there is a relationship between data properties.
	 */
	public void convertRF2ArchiveToOWLIncludingFictitiousDataAttribute() throws Exception {
		final RF2ToOWLService rf2ToOWLService = new RF2ToOWLService();

		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File fictitiousDataAttributeDelta = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_OWLDataProperty_delta");

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final FileInputStream rf2ArchiveStream = new FileInputStream(baseRF2SnapshotZip);
		rf2ToOWLService.convertRF2ArchiveToOWL(null, "20180801", true, new InputStreamSet(rf2ArchiveStream), new OptionalFileInputStream(fictitiousDataAttributeDelta), byteArrayOutputStream);

		assertEquals("" +
						rf2ToOWLService.getCopyrightNotice() +
						"\n" +
						"Prefix(test:=<http://test.com/test/>)\n" +
						"Prefix(:=<http://snomed.info/id/>)\n" +
						"Prefix(owl:=<http://www.w3.org/2002/07/owl#>)\n" +
						"Prefix(rdf:=<http://www.w3.org/1999/02/22-rdf-syntax-ns#>)\n" +
						"Prefix(xml:=<http://www.w3.org/XML/1998/namespace>)\n" +
						"Prefix(xsd:=<http://www.w3.org/2001/XMLSchema#>)\n" +
						"Prefix(rdfs:=<http://www.w3.org/2000/01/rdf-schema#>)\n" +
						"\n" +
						"\n" +
						"Ontology(<http://snomed.info/sct/900000000000207008>\n" +
						"<http://snomed.info/sct/900000000000207008/version/20180801>\n" +
						"\n" +
						"Declaration(Class(:113331007))\n" +
						"Declaration(Class(:116680003))\n" +
						"Declaration(Class(:138875005))\n" +
						"Declaration(Class(:362969004))\n" +
						"Declaration(Class(:404684003))\n" +
						"Declaration(Class(:410662002))\n" +
						"Declaration(Class(:723594008))\n" +
						"Declaration(Class(:723596005))\n" +
						"Declaration(Class(:900000000000441003))\n" +
						"Declaration(ObjectProperty(:363698007))\n" +
						"Declaration(ObjectProperty(:609096000))\n" +
						"Declaration(ObjectProperty(:762705008))\n" +
						"Declaration(DataProperty(:762706009))\n" +
						"Declaration(DataProperty(:99001001))\n" +
						"\n" +
						"############################\n" +
						"#   Object Properties\n" +
						"############################\n" +
						"\n" +
						"# Object Property: <http://snomed.info/id/363698007> (Finding site (attribute))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :363698007 \"Finding site (attribute)\"^^xsd:string)\n" +
						"SubObjectPropertyOf(:363698007 :762705008)\n" +
						"\n" +
						"\n" +
						"############################\n" +
						"#   Data Properties\n" +
						"############################\n" +
						"\n" +
						"# Data Property: <http://snomed.info/id/99001001> (<http://snomed.info/id/99001001>)\n" +
						"\n" +
						"SubDataPropertyOf(:99001001 :762706009)\n" +
						"\n" +
						"\n" +
						"\n" +
						"############################\n" +
						"#   Classes\n" +
						"############################\n" +
						"\n" +
						"# Class: <http://snomed.info/id/113331007> (Structure of endocrine system (body structure))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :113331007 \"Structure of endocrine system (body structure)\"^^xsd:string)\n" +
						"SubClassOf(:113331007 :138875005)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/116680003> (Is a (attribute))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :116680003 \"Is a (attribute)\"^^xsd:string)\n" +
						"SubClassOf(:116680003 :900000000000441003)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/138875005> (SNOMED CT Concept (SNOMED RT+CTV3))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :138875005 \"SNOMED CT Concept (SNOMED RT+CTV3)\"^^xsd:string)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/362969004> (Disorder of endocrine system (disorder))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :362969004 \"Disorder of endocrine system (disorder)\"^^xsd:string)\n" +
						"EquivalentClasses(:362969004 ObjectIntersectionOf(:404684003 ObjectSomeValuesFrom(:609096000 ObjectSomeValuesFrom(:363698007 :113331007))))\n" +
						"\n" +
						"# Class: <http://snomed.info/id/404684003> (Clinical finding (finding))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :404684003 \"Clinical finding (finding)\"^^xsd:string)\n" +
						"SubClassOf(:404684003 :138875005)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/410662002> (Concept model attribute (attribute))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :410662002 \"Concept model attribute (attribute)\"^^xsd:string)\n" +
						"SubClassOf(:410662002 :900000000000441003)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/723594008> (All precoordinated SNOMED CT content (foundation metadata concept))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :723594008 \"All precoordinated SNOMED CT content (foundation metadata concept)\"^^xsd:string)\n" +
						"SubClassOf(:723594008 :900000000000441003)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/723596005> (All SNOMED CT content (foundation metadata concept))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :723596005 \"All SNOMED CT content (foundation metadata concept)\"^^xsd:string)\n" +
						"SubClassOf(:723596005 :723594008)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/900000000000441003> (SNOMED CT Model Component (metadata))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :900000000000441003 \"SNOMED CT Model Component (metadata)\"^^xsd:string)\n" +
						"SubClassOf(:900000000000441003 :138875005)\n" +
						"\n" +
						"\n" +
						")",
				byteArrayOutputStream.toString());
	}

	@Test
	public void convertExtensionRF2ArchivesToOWL() throws Exception {
		final RF2ToOWLService rf2ToOWLService = new RF2ToOWLService();

		File baseRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Base_snapshot");
		File extensionRF2SnapshotZip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/SnomedCT_MiniRF2_Extension_snapshot");

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		rf2ToOWLService.convertRF2ArchiveToOWL(null, "20180731", true, new InputStreamSet(baseRF2SnapshotZip, extensionRF2SnapshotZip), new OptionalFileInputStream(null), byteArrayOutputStream);

		assertEquals("" +
						rf2ToOWLService.getCopyrightNotice() +
						"\n" +
						"Prefix(test:=<http://test.com/test/>)\n" +
						"Prefix(:=<http://snomed.info/id/>)\n" +
						"Prefix(owl:=<http://www.w3.org/2002/07/owl#>)\n" +
						"Prefix(rdf:=<http://www.w3.org/1999/02/22-rdf-syntax-ns#>)\n" +
						"Prefix(xml:=<http://www.w3.org/XML/1998/namespace>)\n" +
						"Prefix(xsd:=<http://www.w3.org/2001/XMLSchema#>)\n" +
						"Prefix(rdfs:=<http://www.w3.org/2000/01/rdf-schema#>)\n" +
						"\n" +
						"\n" +

						// Extension Ontology header
						"Ontology(<http://snomed.info/sct/900101001>\n" +
						"<http://snomed.info/sct/900101001/version/20180731>\n" +

						"\n" +
						"Declaration(Class(:113331007))\n" +
						"Declaration(Class(:116680003))\n" +
						"Declaration(Class(:12481008))\n" +
						"Declaration(Class(:129287005))\n" +
						"Declaration(Class(:138875005))\n" +
						"Declaration(Class(:18736003))\n" +
						"Declaration(Class(:25342003))\n" +
						"Declaration(Class(:281615006))\n" +
						"Declaration(Class(:362969004))\n" +
						"Declaration(Class(:404684003))\n" +
						"Declaration(Class(:410662002))\n" +
						"Declaration(Class(:723594008))\n" +
						"Declaration(Class(:723596005))\n" +
						"Declaration(Class(:76145000))\n" +
						"Declaration(Class(:84301002))\n" +
						"Declaration(Class(:900000000000441003))\n" +

				// Additional Extension class declaration
						"Declaration(Class(:900101001))\n" +

						"Declaration(ObjectProperty(:260686004))\n" +
						"Declaration(ObjectProperty(:363698007))\n" +
						"Declaration(ObjectProperty(:405813007))\n" +
						"Declaration(ObjectProperty(:609096000))\n" +
						"Declaration(ObjectProperty(:762705008))\n" +
						"\n" +
						"############################\n" +
						"#   Object Properties\n" +
						"############################\n" +
						"\n" +
						"# Object Property: <http://snomed.info/id/363698007> (Finding site (attribute))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :363698007 \"Finding site (attribute)\"^^xsd:string)\n" +
						"SubObjectPropertyOf(:363698007 :762705008)\n" +
						"\n" +
						"\n" +
						"\n" +
						"############################\n" +
						"#   Classes\n" +
						"############################\n" +
						"\n" +
						"# Class: <http://snomed.info/id/113331007> (Structure of endocrine system (body structure))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :113331007 \"Structure of endocrine system (body structure)\"^^xsd:string)\n" +
						"SubClassOf(:113331007 :138875005)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/116680003> (Is a (attribute))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :116680003 \"Is a (attribute)\"^^xsd:string)\n" +
						"SubClassOf(:116680003 :900000000000441003)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/138875005> (SNOMED CT Concept (SNOMED RT+CTV3))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :138875005 \"SNOMED CT Concept (SNOMED RT+CTV3)\"^^xsd:string)\n" +
						"\n" +

						"# Class: <http://snomed.info/id/18736003> (Middle ear exploration through ear canal incision (procedure))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :18736003 \"Middle ear exploration through ear canal incision (procedure)\"^^xsd:string)\n" +
						// Class with multiple parents and multiple role groups to test sorting
						"SubClassOf(" +
							":18736003 " +
							"ObjectIntersectionOf(" +
								// Bytes: 49,50,52,56,49,48,48,56
								":12481008 " +
								// Bytes: 55,54,49,52,53,48,48,48
								":76145000 " +
								"ObjectSomeValuesFrom(" +
									// Bytes: 54,48,57,48,57,54,48,48,48
									":609096000 " +
									"ObjectIntersectionOf(" +
										"ObjectSomeValuesFrom(" +
											// Bytes: 50,54,48,54,56,54,48,48,52
											":260686004 " +
											// Bytes: 49,50,57,50,56,55,48,48,53
											":129287005) " +
										"ObjectSomeValuesFrom(" +
											// Bytes: 52,48,53,56,49,51,48,48,55
											":405813007 " +
											// Bytes: 56,52,51,48,49,48,48,50
											":84301002))) " +
								"ObjectSomeValuesFrom(" +
									// Bytes: 54,48,57,48,57,54,48,48,48
									":609096000 " +
									"ObjectIntersectionOf(" +
										"ObjectSomeValuesFrom(" +
											// Bytes: 50,54,48,54,56,54,48,48,52
											":260686004 " +
											// Bytes: 50,56,49,54,49,53,48,48,54
											":281615006) " +
										"ObjectSomeValuesFrom(" +
											// Bytes: 52,48,53,56,49,51,48,48,55
											":405813007 " +
											// Bytes: 50,53,51,52,50,48,48,51
											":25342003)))))\n" +
						"\n" +

						"# Class: <http://snomed.info/id/362969004> (Disorder of endocrine system (disorder))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :362969004 \"Disorder of endocrine system (disorder)\"^^xsd:string)\n" +
						"EquivalentClasses(:362969004 ObjectIntersectionOf(:404684003 ObjectSomeValuesFrom(:609096000 ObjectSomeValuesFrom(:363698007 :113331007))))\n" +
						"\n" +
						"# Class: <http://snomed.info/id/404684003> (Clinical finding (finding))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :404684003 \"Clinical finding (finding)\"^^xsd:string)\n" +
						"SubClassOf(:404684003 :138875005)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/410662002> (Concept model attribute (attribute))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :410662002 \"Concept model attribute (attribute)\"^^xsd:string)\n" +
						"SubClassOf(:410662002 :900000000000441003)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/723594008> (All precoordinated SNOMED CT content (foundation metadata concept))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :723594008 \"All precoordinated SNOMED CT content (foundation metadata concept)\"^^xsd:string)\n" +
						"SubClassOf(:723594008 :900000000000441003)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/723596005> (All SNOMED CT content (foundation metadata concept))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :723596005 \"All SNOMED CT content (foundation metadata concept)\"^^xsd:string)\n" +
						"SubClassOf(:723596005 :723594008)\n" +
						"\n" +
						"# Class: <http://snomed.info/id/900000000000441003> (SNOMED CT Model Component (metadata))\n" +
						"\n" +
						"AnnotationAssertion(rdfs:label :900000000000441003 \"SNOMED CT Model Component (metadata)\"^^xsd:string)\n" +
						"SubClassOf(:900000000000441003 :138875005)\n" +
						"\n" +

				// Additional Extension class

						"# Class: <http://snomed.info/id/900101001> (<http://snomed.info/id/900101001>)\n" +
						"\n" +
						"SubClassOf(:900101001 :900000000000441003)\n" +
						"\n" +
						"\n" +
						")",
				byteArrayOutputStream.toString());
	}

}
