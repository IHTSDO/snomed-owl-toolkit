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
