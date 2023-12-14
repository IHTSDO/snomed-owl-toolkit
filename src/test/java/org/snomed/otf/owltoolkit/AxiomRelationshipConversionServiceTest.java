package org.snomed.otf.owltoolkit;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.conversion.AxiomRelationshipConversionService;
import org.snomed.otf.owltoolkit.conversion.ConversionException;
import org.snomed.otf.owltoolkit.domain.AxiomRepresentation;
import org.snomed.otf.owltoolkit.domain.Relationship;
import org.snomed.otf.owltoolkit.ontology.OntologyService;

import java.util.*;

import static org.junit.Assert.*;
import static org.snomed.otf.owltoolkit.domain.Relationship.*;

public class AxiomRelationshipConversionServiceTest {

	private AxiomRelationshipConversionService axiomRelationshipConversionService;

	@Before
	public void setup() {
		HashSet<Long> ungroupedAttributes = Sets.newHashSet(
				Concepts.LATERALITY_LONG,
				Long.valueOf(Concepts.HAS_DOSE_FORM),
				763032000L,
				3264479001L);

		HashSet<Long> annotationAttributes = Sets.newHashSet(
				1295447006L);
		axiomRelationshipConversionService = new AxiomRelationshipConversionService(ungroupedAttributes, null, null, annotationAttributes);
	}

	@Test
	public void testGCITwoGroupsOneRelationshipInEach() throws ConversionException {
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

		assertTrue(representation.isPrimitive());

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

		assertTrue(representation.isPrimitive());

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

		assertFalse(representation.isPrimitive());

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

		AxiomRepresentation representation = axiomRelationshipConversionService.convertAxiomToRelationships(axiom);

		assertEquals(118956008, representation.getLeftHandSideNamedConcept().longValue());

		assertEquals(
				"0 116680003=123037004",
				toString(representation.getRightHandSideRelationships()));

		assertEquals(118956008, representation.getLeftHandSideNamedConcept().longValue());

		// Test converting relationships back to an axiom
		String recreatedAxiom = axiomRelationshipConversionService.convertRelationshipsToAxiom(representation);
		assertEquals(axiom, recreatedAxiom);
		assertTrue(representation.isPrimitive());
	}

	@Test
	public void testAxiomPrimitiveWithSingleRelationshipWithoutGivingReferencedComponentId() throws ConversionException {
		String axiom = "SubClassOf(:118956008 :123037004)";

		AxiomRepresentation representation = axiomRelationshipConversionService.convertAxiomToRelationships(axiom);

		assertEquals(
				"0 116680003=123037004",
				toString(representation.getRightHandSideRelationships()));

		assertEquals(118956008, representation.getLeftHandSideNamedConcept().longValue());

		// Test converting relationships back to an axiom
		String recreatedAxiom = axiomRelationshipConversionService.convertRelationshipsToAxiom(representation);
		assertEquals(axiom, recreatedAxiom);
	}

	@Test
	public void testAttributeIsARelationship() throws ConversionException {
		String axiom = "SubObjectPropertyOf(:363698007 :762705008)";

		AxiomRepresentation representation = axiomRelationshipConversionService.convertAxiomToRelationships(axiom);

		assertNotNull(representation);

		assertEquals(
				"0 116680003=762705008",
				toString(representation.getRightHandSideRelationships()));

		assertEquals(363698007, representation.getLeftHandSideNamedConcept().longValue());
		assertTrue(representation.isPrimitive());
	}

	@Test
	public void testAttributePropertyChain() throws ConversionException {
		String axiom = "SubObjectPropertyOf(ObjectPropertyChain(:246093002 :738774007) :246093002)";

		AxiomRepresentation representation = axiomRelationshipConversionService.convertAxiomToRelationships(axiom);

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

		AxiomRepresentation representation = axiomRelationshipConversionService.convertAxiomToRelationships(axiom);

		assertEquals(9846003L, representation.getLeftHandSideNamedConcept().longValue());

		assertFalse(representation.isPrimitive());

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

		assertEquals(Sets.newHashSet(784852002L, 763158003L, 411116001L, 385023001L, 609096000L,
				732943007L, 386859000L,
				733722007L, 258773002L,
				733725009L, 258684004L,
				762949000L, 386859000L,
				1142137007L,
				1142138002L,
				1142139005L),
				axiomRelationshipConversionService.getIdsOfConceptsNamedInAxiom(
						"EquivalentClasses(:784852002 " +
						"	ObjectIntersectionOf(:763158003 " +
						"		ObjectSomeValuesFrom(:411116001 :385023001) " +
						"		ObjectSomeValuesFrom(:609096000 " +
						"			ObjectIntersectionOf(" +
						"				ObjectSomeValuesFrom(:732943007 :386859000) " +
						"				ObjectSomeValuesFrom(:733722007 :258773002) " +
						"				ObjectSomeValuesFrom(:733725009 :258684004) " +
						"				ObjectSomeValuesFrom(:762949000 :386859000) " +
						"				DataHasValue(:1142137007 \"1\"^^xsd:integer) " +
						"				DataHasValue(:1142138002 \"2.5\"^^xsd:decimal)" +
						"			)" +
						"		) " +
						"		DataHasValue(:1142139005 \"1\"^^xsd:integer)" +
						"	)" +
						")"));
	}

	@Test
	public void testConvertRelationshipsToAxiomAllowGroupedAttribute() throws ConversionException {
		AxiomRepresentation representation = new AxiomRepresentation();
		representation.setLeftHandSideNamedConcept(9846003L);
		representation.setRightHandSideRelationships(toMap(
				new Relationship(Concepts.IS_A_LONG, 39132006L),
				// Put a grouped attribute in group 1
				new Relationship(1, Concepts.HAS_ACTIVE_INGREDIENT_LONG, 7771000L)));

		String actual = axiomRelationshipConversionService.convertRelationshipsToAxiom(representation);
		assertTrue(actual.contains(OntologyService.ROLE_GROUP_SCTID));
		assertEquals("EquivalentClasses(:9846003 ObjectIntersectionOf(:39132006 ObjectSomeValuesFrom(:609096000 ObjectSomeValuesFrom(:127489000 :7771000))))", actual);
	}

	@Test
	public void testConvertRelationshipsToAxiomMoveUngroupedAttribute() throws ConversionException {
		AxiomRepresentation representation = new AxiomRepresentation();
		representation.setLeftHandSideNamedConcept(9846003L);
		representation.setRightHandSideRelationships(toMap(
				new Relationship(Concepts.IS_A_LONG, 39132006L),
				// Attempt to group an ungrouped attribute by placing it in group 1
				new Relationship(1, Concepts.LATERALITY_LONG, 7771000L)));

		String actual = axiomRelationshipConversionService.convertRelationshipsToAxiom(representation);
		assertFalse(actual.contains(OntologyService.ROLE_GROUP_SCTID));
		assertEquals("EquivalentClasses(:9846003 ObjectIntersectionOf(:39132006 ObjectSomeValuesFrom(:272741003 :7771000)))", actual);
	}

	@Test
	public void testAxiomWithConcreteValueToRelationships() throws ConversionException {
		String axiom = "EquivalentClasses(:322236009 " +
				"ObjectIntersectionOf(:763158003 ObjectSomeValuesFrom(:411116001 :421026006) " +
					"ObjectSomeValuesFrom(:609096000 ObjectIntersectionOf(ObjectSomeValuesFrom(:732943007 :387517004) " +
					"ObjectSomeValuesFrom(:732945000 :258684004) ObjectSomeValuesFrom(:732946004 :38112003) ObjectSomeValuesFrom(:732947008 :732936001) " +
					"ObjectSomeValuesFrom(:762949000 :387517004) DataHasValue(:3264475007 \"500\"^^xsd:decimal))) " +
				"ObjectSomeValuesFrom(:763032000 :732936001) " +
				"DataHasValue(:3264479001 \"1\"^^xsd:integer)))";

		AxiomRepresentation representation = axiomRelationshipConversionService.convertAxiomToRelationships(axiom);

		assertEquals(322236009, representation.getLeftHandSideNamedConcept().longValue());
		assertEquals(
				"0 116680003=763158003\n" +
						"0 411116001=421026006\n" +
						"0 763032000=732936001\n" +
						"0 3264479001=1\n" +
						"1 732943007=387517004\n" +
						"1 732945000=258684004\n" +
						"1 732946004=38112003\n" +
						"1 732947008=732936001\n" +
						"1 762949000=387517004\n" +
						"1 3264475007=500",
				toString(representation.getRightHandSideRelationships()));

		assertNull(representation.getRightHandSideNamedConcept());

		// Test converting relationships back to an axiom
		String recreatedAxiom = axiomRelationshipConversionService.convertRelationshipsToAxiom(representation);
		assertEquals(axiom, recreatedAxiom);
		assertFalse(representation.isPrimitive());
	}

	@Test
	public void testMinimalAxiomWithConcreteValueToRelationships() throws ConversionException {
		String axiom =
				"SubClassOf(" +
				"	:12345678910" + // Named class
				"	ObjectIntersectionOf(" +
				"		:138875005 " + // Parent (is-a relationship)
				"		ObjectSomeValuesFrom(" +
				"			:609096000 " + // Role group
				"			DataHasValue(:1234567891011 \"1\"^^xsd:integer)" + // Concrete attribute-value pair
				"		)" +
				"	)" +
				")";

		AxiomRepresentation representation = axiomRelationshipConversionService.convertAxiomToRelationships(axiom);

		assertEquals(12345678910L, representation.getLeftHandSideNamedConcept().longValue());
		assertEquals(
				"0 116680003=138875005\n" +
						"1 1234567891011=1",
				toString(representation.getRightHandSideRelationships()));

		assertNull(representation.getRightHandSideNamedConcept());

		// Test converting relationships back to an axiom
		String recreatedAxiom = axiomRelationshipConversionService.convertRelationshipsToAxiom(representation);
		assertEquals(axiom.replaceAll("\\W", ""), recreatedAxiom.replaceAll("\\W", ""));
		assertTrue(representation.isPrimitive());
	}

	@Test
	public void testConvertRelationshipConcreteValueToAxiom() throws ConversionException {
		AxiomRepresentation representation = new AxiomRepresentation();
		representation.setLeftHandSideNamedConcept(322236009L);
		representation.setRightHandSideRelationships(toMap(
				new Relationship(Concepts.IS_A_LONG, 763158003L),
				new Relationship(1, 3264479001L, new ConcreteValue(ConcreteValue.Type.INTEGER, "1")),
				new Relationship(0, 763032000L, 732936001L),
				new Relationship(0, 411116001L, 421026006L)
		));

		String actual = axiomRelationshipConversionService.convertRelationshipsToAxiom(representation);
		assertFalse(actual.contains(OntologyService.ROLE_GROUP_SCTID));
		assertEquals("EquivalentClasses(:322236009 ObjectIntersectionOf(:763158003 ObjectSomeValuesFrom(:411116001 :421026006) " +
				"ObjectSomeValuesFrom(:763032000 :732936001) DataHasValue(:3264479001 \"1\"^^xsd:integer)))", actual);

	}

	@Test(expected = ConversionException.class)
	public void testConvertConcreteValueAndNoParentMustFail() throws ConversionException {
		AxiomRepresentation representation = new AxiomRepresentation();
		representation.setLeftHandSideNamedConcept(322236009L);
		representation.setRightHandSideRelationships(toMap(
				new Relationship(0, 3264479001L, new ConcreteValue(ConcreteValue.Type.INTEGER, "1"))
		));

		axiomRelationshipConversionService.convertRelationshipsToAxiom(representation);
	}

	@Test
	public void testConvertAnnotationAttributeToAxiom() throws ConversionException {
		AxiomRepresentation representation = new AxiomRepresentation();
		representation.setLeftHandSideNamedConcept(322236009L);
		representation.setRightHandSideRelationships(toMap(
				new Relationship(Concepts.IS_A_LONG, 1295447006L)
		));
		String actual = axiomRelationshipConversionService.convertRelationshipsToAxiom(representation);
		assertEquals("SubAnnotationPropertyOf(:322236009 :1295447006)", actual);

	}

	private Map<Integer, List<Relationship>> toMap(Relationship... relationships) {
		HashMap<Integer, List<Relationship>> relationshipMap = new HashMap<>();
		for (Relationship relationship : relationships) {
			relationshipMap.computeIfAbsent(relationship.getGroup(), g -> new ArrayList<>()).add(relationship);
		}
		return relationshipMap;
	}

	private String toString(Map<Integer, List<Relationship>> relationshipGroups) {
		StringBuilder groupsString = new StringBuilder();
		for (Integer group : relationshipGroups.keySet()) {
			List<Relationship> relationships = relationshipGroups.get(group);
			for (Relationship relationship : relationships) {
				groupsString.append(relationship.getGroup())
						.append(" ")
						.append(relationship.getTypeId())
						.append("=");
						if (relationship.getDestinationId() != -1) {
							groupsString.append(relationship.getDestinationId());
						}
						if (relationship.getValue() != null) {
							groupsString.append(relationship.getValue().asString());
						}
				groupsString.append("\n");
			}
		}
		if (groupsString.length() > 0) {
			groupsString.deleteCharAt(groupsString.length() - 1);
		}
		return groupsString.toString();
	}


}
