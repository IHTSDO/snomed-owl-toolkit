package org.snomed.otf.owltoolkit;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.conversion.ConversionException;
import org.snomed.otf.owltoolkit.conversion.ConversionService;
import org.snomed.otf.owltoolkit.domain.AxiomRepresentation;
import org.snomed.otf.owltoolkit.domain.Relationship;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ConversionServiceTest {

	private ConversionService conversionService;

	@Before
	public void setup() {
		conversionService = new ConversionService(Sets.newHashSet(Concepts.LATERALITY_LONG));
	}

	@Test
	public void testGCIPrimitiveTwoGroupsOneRelationshipInEach() throws ConversionException {
		String axiom =
				"SubClassOf(" +
					"ObjectIntersectionOf(" +
						":73211009 " +
						"ObjectSomeValuesFrom(" +
							":609096000 " +
							"ObjectSomeValuesFrom(" +
								":100105001 " +
								":100101001" +
							")" +
						")" +
					") " +
					":8801005" +
				")";

		AxiomRepresentation representation = conversionService.convertAxiomToRelationships(axiom);

		assertEquals(
				"0 116680003=73211009\n" +
				"1 100105001=100101001",
				toString(representation.getLeftHandSideRelationships()));

		assertEquals(8801005, representation.getRightHandSideNamedConcept().longValue());

		// Test converting relationships back to an axiom
		String recreatedAxiom = conversionService.convertRelationshipsToAxiom(representation);
		recreatedAxiom = simplifySnomedPrefix(recreatedAxiom);
		assertEquals(axiom, recreatedAxiom);
	}

	@Test
	public void testAdditionalAxiomPrimitiveTwoGroupsOneRelationshipInEach() throws ConversionException {
		String axiom =
				"SubClassOf(" +
					":8801005 " +
					"ObjectIntersectionOf(" +
						":73211009 " +
						"ObjectSomeValuesFrom(" +
							":609096000 " +
							"ObjectSomeValuesFrom(" +
								":100105001 " +
								":100101001" +
							")" +
						")" +
					")" +
				")";

		AxiomRepresentation representation = conversionService.convertAxiomToRelationships(axiom);

		assertEquals(8801005, representation.getLeftHandSideNamedConcept().longValue());

		assertEquals(
				"0 116680003=73211009\n" +
				"1 100105001=100101001",
				toString(representation.getRightHandSideRelationships()));

		// Test converting relationships back to an axiom
		String recreatedAxiom = conversionService.convertRelationshipsToAxiom(representation);
		recreatedAxiom = simplifySnomedPrefix(recreatedAxiom);
		assertEquals(axiom, recreatedAxiom);
	}

	@Test
	public void testAdditionalAxiomSufficientlyDefinedTwoRelationshipsInGroup() throws ConversionException {
		String axiom =
				"EquivalentClasses(" +
					"<http://snomed.info/id/10002003> " +
					"ObjectIntersectionOf(" +
						"<http://snomed.info/id/116175006> " +
						"ObjectSomeValuesFrom(" +
							"<http://snomed.info/id/609096000> " +
							"ObjectIntersectionOf(" +
								"ObjectSomeValuesFrom(" +
									"<http://snomed.info/id/260686004> " +
									"<http://snomed.info/id/129304002>" +
								") " +
								"ObjectSomeValuesFrom(" +
									"<http://snomed.info/id/405813007> " +
									"<http://snomed.info/id/414003>" +
								")" +
							")" +
						")" +
					")" +
				" )";

		AxiomRepresentation representation = conversionService.convertAxiomToRelationships(axiom);

		assertEquals(10002003, representation.getLeftHandSideNamedConcept().longValue());

		assertEquals(
				"0 116680003=116175006\n" +
				"1 260686004=129304002\n" +
				"1 405813007=414003",
				toString(representation.getRightHandSideRelationships()));

		// Test converting relationships back to an axiom
		String recreatedAxiom = conversionService.convertRelationshipsToAxiom(representation);
		assertEquals(axiom, recreatedAxiom);
	}

	@Test
	public void testAdditionalAxiomPrimitiveWithSingleRelationship() throws ConversionException {
		String axiom = "SubClassOf(<http://snomed.info/id/118956008> <http://snomed.info/id/123037004>)";

		AxiomRepresentation representation = conversionService.convertAxiomToRelationships(118956008L, axiom);

		assertEquals(118956008, representation.getLeftHandSideNamedConcept().longValue());

		assertEquals(
				"0 116680003=123037004",
				toString(representation.getRightHandSideRelationships()));

		// Test converting relationships back to an axiom
		String recreatedAxiom = conversionService.convertRelationshipsToAxiom(representation);
		assertEquals(axiom, recreatedAxiom);
	}

	@Test
	public void testGCIPrimitiveWithSingleRelationship() throws ConversionException {
		String axiom = "SubClassOf(<http://snomed.info/id/118956008> <http://snomed.info/id/123037004>)";

		AxiomRepresentation representation = conversionService.convertAxiomToRelationships(123037004L, axiom);

		assertEquals(
				"0 116680003=118956008",
				toString(representation.getLeftHandSideRelationships()));

		assertEquals(123037004, representation.getRightHandSideNamedConcept().longValue());

		// Test converting relationships back to an axiom
		String recreatedAxiom = conversionService.convertRelationshipsToAxiom(representation);
		assertEquals(axiom, recreatedAxiom);
	}

	@Test
	public void testAdditionalAxiomNeverGrouped() throws ConversionException {
		String axiom =
				"EquivalentClasses(" +
					":9846003 " +
					"ObjectIntersectionOf(" +
						":39132006 " +
						":64033007 " +
						"ObjectSomeValuesFrom(" +
							":272741003 " +
							":24028007" +
						")" +
					")" +
				" )";

		AxiomRepresentation representation = conversionService.convertAxiomToRelationships(9846003L, axiom);

		assertEquals(9846003L, representation.getLeftHandSideNamedConcept().longValue());

		assertEquals(
				"0 116680003=39132006\n" +
				"0 116680003=64033007\n" +
				"0 272741003=24028007",
				toString(representation.getRightHandSideRelationships()));

		// Test converting relationships back to an axiom
		String recreatedAxiom = conversionService.convertRelationshipsToAxiom(representation);
		recreatedAxiom = simplifySnomedPrefix(recreatedAxiom);
		assertEquals(axiom, recreatedAxiom);
	}

	@Test
	public void testConvertTransitiveObjectPropertyReturnNull() throws ConversionException {
		AxiomRepresentation axiomRepresentation = conversionService.convertAxiomToRelationships("TransitiveObjectProperty(:738774007)");
		assertNull(axiomRepresentation);
	}

	private String toString(Map<Integer, List<Relationship>> relationshipGroups) {
		StringBuilder groupsString = new StringBuilder();
		for (Integer group : relationshipGroups.keySet()) {
			List<Relationship> relationships = relationshipGroups.get(group);
			for (Relationship relationship : relationships) {
				groupsString.append(relationship.getGroup())
						.append(" ")
						.append(relationship.getTypeId())
						.append("=")
						.append(relationship.getDestinationId());
				groupsString.append("\n");
			}
		}
		if (groupsString.length() > 0) {
			groupsString.deleteCharAt(groupsString.length() - 1);
		}
		return groupsString.toString();
	}


	private String simplifySnomedPrefix(String recreatedAxiom) {
		return recreatedAxiom.replace("<http://snomed.info/id/", ":").replace(">", "");
	}
}
