package org.snomed.otf.owltoolkit;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.conversion.AxiomRelationshipConversionService;
import org.snomed.otf.owltoolkit.conversion.ConversionException;
import org.snomed.otf.owltoolkit.domain.AxiomRepresentation;
import org.snomed.otf.owltoolkit.domain.Relationship;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AxiomRelationshipConversionServiceTest {

	private AxiomRelationshipConversionService axiomRelationshipConversionService;

	@Before
	public void setup() {
		axiomRelationshipConversionService = new AxiomRelationshipConversionService(Sets.newHashSet(Concepts.LATERALITY_LONG));
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

		AxiomRepresentation representation = axiomRelationshipConversionService.convertAxiomToRelationships(axiom);

		assertEquals(
				"0 116680003=73211009\n" +
				"1 100105001=100101001",
				toString(representation.getLeftHandSideRelationships()));

		assertEquals(8801005, representation.getRightHandSideNamedConcept().longValue());

		// Test converting relationships back to an axiom
		String recreatedAxiom = axiomRelationshipConversionService.convertRelationshipsToAxiom(representation);
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

		AxiomRepresentation representation = axiomRelationshipConversionService.convertAxiomToRelationships(axiom);

		assertEquals(8801005, representation.getLeftHandSideNamedConcept().longValue());

		assertEquals(
				"0 116680003=73211009\n" +
				"1 100105001=100101001",
				toString(representation.getRightHandSideRelationships()));

		// Test converting relationships back to an axiom
		String recreatedAxiom = axiomRelationshipConversionService.convertRelationshipsToAxiom(representation);
		assertEquals(axiom, recreatedAxiom);
	}

	@Test
	public void testAdditionalAxiomSufficientlyDefinedTwoRelationshipsInGroup() throws ConversionException {
		String axiom =
				"EquivalentClasses(" +
					":10002003 " +
					"ObjectIntersectionOf(" +
						":116175006 " +
						"ObjectSomeValuesFrom(" +
							":609096000 " +
							"ObjectIntersectionOf(" +
								"ObjectSomeValuesFrom(" +
									":260686004 " +
									":129304002" +
								") " +
								"ObjectSomeValuesFrom(" +
									":405813007 " +
									":414003" +
								")" +
							")" +
						")" +
					")" +
				")";

		AxiomRepresentation representation = axiomRelationshipConversionService.convertAxiomToRelationships(axiom);

		assertEquals(10002003, representation.getLeftHandSideNamedConcept().longValue());

		assertEquals(
				"0 116680003=116175006\n" +
				"1 260686004=129304002\n" +
				"1 405813007=414003",
				toString(representation.getRightHandSideRelationships()));

		// Test converting relationships back to an axiom
		String recreatedAxiom = axiomRelationshipConversionService.convertRelationshipsToAxiom(representation);
		assertEquals(axiom, recreatedAxiom);
	}

	@Test
	public void testAdditionalAxiomPrimitiveWithSingleRelationship() throws ConversionException {
		String axiom = "SubClassOf(:118956008 :123037004)";

		AxiomRepresentation representation = axiomRelationshipConversionService.convertAxiomToRelationships(118956008L, axiom);

		assertEquals(118956008, representation.getLeftHandSideNamedConcept().longValue());

		assertEquals(
				"0 116680003=123037004",
				toString(representation.getRightHandSideRelationships()));

		// Test converting relationships back to an axiom
		String recreatedAxiom = axiomRelationshipConversionService.convertRelationshipsToAxiom(representation);
		assertEquals(axiom, recreatedAxiom);
	}

	@Test
	public void testGCIPrimitiveWithSingleRelationship() throws ConversionException {
		String axiom = "SubClassOf(:118956008 :123037004)";

		AxiomRepresentation representation = axiomRelationshipConversionService.convertAxiomToRelationships(123037004L, axiom);

		assertEquals(
				"0 116680003=118956008",
				toString(representation.getLeftHandSideRelationships()));

		assertEquals(123037004, representation.getRightHandSideNamedConcept().longValue());

		// Test converting relationships back to an axiom
		String recreatedAxiom = axiomRelationshipConversionService.convertRelationshipsToAxiom(representation);
		assertEquals(axiom, recreatedAxiom);
	}

	@Test
	public void testAttributeIsARelationship() throws ConversionException {
		String axiom = "SubObjectPropertyOf(:363698007 :762705008)";

		AxiomRepresentation representation = axiomRelationshipConversionService.convertAxiomToRelationships(363698007L, axiom);

		assertNotNull(representation);

		assertEquals(
				"0 116680003=762705008",
				toString(representation.getRightHandSideRelationships()));

		assertEquals(363698007, representation.getLeftHandSideNamedConcept().longValue());
	}

	@Test
	public void testAttributePropertyChain() throws ConversionException {
		String axiom = "SubObjectPropertyOf(ObjectPropertyChain(:246093002 :738774007) :246093002)";

		AxiomRepresentation representation = axiomRelationshipConversionService.convertAxiomToRelationships(246093002L, axiom);

		assertNull(representation);
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
				")";

		AxiomRepresentation representation = axiomRelationshipConversionService.convertAxiomToRelationships(9846003L, axiom);

		assertEquals(9846003L, representation.getLeftHandSideNamedConcept().longValue());

		assertEquals(
				"0 116680003=39132006\n" +
				"0 116680003=64033007\n" +
				"0 272741003=24028007",
				toString(representation.getRightHandSideRelationships()));

		// Test converting relationships back to an axiom
		String recreatedAxiom = axiomRelationshipConversionService.convertRelationshipsToAxiom(representation);
		assertEquals(axiom, recreatedAxiom);
	}

	@Test
	public void testConvertTransitiveObjectPropertyReturnNull() throws ConversionException {
		AxiomRepresentation axiomRepresentation = axiomRelationshipConversionService.convertAxiomToRelationships("TransitiveObjectProperty(:738774007)");
		assertNull(axiomRepresentation);
	}

	@Test
	public void testGetIdsOfConceptsNamedInAxiom() throws ConversionException {
		assertEquals(Sets.newHashSet(733930001L), axiomRelationshipConversionService.getIdsOfConceptsNamedInAxiom("TransitiveObjectProperty(:733930001)"));

		assertEquals(Sets.newHashSet(738774007L), axiomRelationshipConversionService.getIdsOfConceptsNamedInAxiom("ReflexiveObjectProperty(:738774007)"));

		assertEquals(Sets.newHashSet(246093002L, 738774007L),
				axiomRelationshipConversionService.getIdsOfConceptsNamedInAxiom("SubObjectPropertyOf(ObjectPropertyChain(:246093002 :738774007) :246093002)"));

		String equivalentClassAxiom =
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
		assertEquals(Sets.newHashSet(9846003L, 39132006L, 64033007L, 272741003L, 24028007L), axiomRelationshipConversionService.getIdsOfConceptsNamedInAxiom(equivalentClassAxiom));
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


}
