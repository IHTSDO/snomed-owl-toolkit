package org.snomed.otf.owltoolkit;

import org.junit.Before;
import org.junit.Test;
import org.snomed.otf.owltoolkit.conversion.ConversionException;
import org.snomed.otf.owltoolkit.conversion.ConversionService;
import org.snomed.otf.owltoolkit.domain.ExpressionRepresentation;
import org.snomed.otf.owltoolkit.domain.Relationship;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ConversionServiceTest {

	private ConversionService conversionService;

	@Before
	public void setup() {
		conversionService = new ConversionService();
	}

	@Test
	public void testGCIPrimitiveTwoGroupsOneRelationshipInEach() throws ConversionException {
		ExpressionRepresentation representation = conversionService.convertAxiomToRelationships(
				"SubClassOf(" +
					"ObjectIntersectionOf(" +
						":73211009 " +
						"ObjectSomeValuesFrom(" +
							":roleGroup " +
							"ObjectSomeValuesFrom(" +
								":100105001 " +
								":100101001" +
							")" +
						")" +
					") " +
					":8801005" +
				")");

		assertEquals(
				"0 116680003=73211009\n" +
				"1 100105001=100101001",
				toString(representation.getLeftHandSideRelationships()));

		assertEquals(8801005, representation.getRightHandSideNamedConcept().longValue());
	}

	@Test
	public void testAdditionalAxiomPrimitiveTwoGroupsOneRelationshipInEach() throws ConversionException {
		ExpressionRepresentation representation = conversionService.convertAxiomToRelationships(
				"SubClassOf(" +
					":8801005 " +
					"ObjectIntersectionOf(" +
						":73211009 " +
						"ObjectSomeValuesFrom(" +
							":roleGroup " +
							"ObjectSomeValuesFrom(" +
								":100105001 " +
								":100101001" +
							")" +
						")" +
					")" +
				")");

		assertEquals(8801005, representation.getLeftHandSideNamedConcept().longValue());

		assertEquals(
				"0 116680003=73211009\n" +
				"1 100105001=100101001",
				toString(representation.getRightHandSideRelationships()));
	}

	@Test
	public void testAdditionalAxiomSufficientlyDefinedTwoRelationshipsInGroup() throws ConversionException {
		ExpressionRepresentation representation = conversionService.convertAxiomToRelationships(
				"EquivalentClasses(" +
					"<http://snomed.info/id/10002003> " +
					"ObjectIntersectionOf(" +
						"<http://snomed.info/id/116175006> " +
						"ObjectSomeValuesFrom(" +
							":roleGroup " +
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
				")");

		assertEquals(10002003, representation.getLeftHandSideNamedConcept().longValue());

		assertEquals(
				"0 116680003=116175006\n" +
				"1 260686004=129304002\n" +
				"1 405813007=414003",
				toString(representation.getRightHandSideRelationships()));
	}

	@Test
	public void testAdditionalAxiomPrimitiveWithSingleRelationship() throws ConversionException {
		ExpressionRepresentation representation = conversionService.convertAxiomToRelationships(118956008L, "SubClassOf(<http://snomed.info/id/118956008> <http://snomed.info/id/123037004>)");

		assertEquals(118956008, representation.getLeftHandSideNamedConcept().longValue());

		assertEquals(
				"0 116680003=123037004",
				toString(representation.getRightHandSideRelationships()));
	}

	@Test
	public void testGCIPrimitiveWithSingleRelationship() throws ConversionException {
		ExpressionRepresentation representation = conversionService.convertAxiomToRelationships(123037004L, "SubClassOf(<http://snomed.info/id/118956008> <http://snomed.info/id/123037004>)");

		assertEquals(
				"0 116680003=118956008",
				toString(representation.getLeftHandSideRelationships()));

		assertEquals(123037004, representation.getRightHandSideNamedConcept().longValue());

	}

	private String toString(Map<Integer, List<Relationship>> relationshipGroups) {
		StringBuilder groupsString = new StringBuilder();
		for (Integer group : relationshipGroups.keySet()) {
			List<Relationship> relationships = relationshipGroups.get(group);
			for (Relationship relationship : relationships) {
				groupsString.append(group)
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

}
