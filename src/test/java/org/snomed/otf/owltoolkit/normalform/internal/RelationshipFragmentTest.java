package org.snomed.otf.owltoolkit.normalform.internal;

import org.junit.Before;
import org.junit.Test;
import org.snomed.otf.owltoolkit.classification.ReasonerTaxonomy;
import org.snomed.otf.owltoolkit.classification.ReasonerTaxonomyEntry;
import org.snomed.otf.owltoolkit.domain.Relationship;
import org.snomed.otf.owltoolkit.domain.Relationship.ConcreteValue;
import org.snomed.otf.owltoolkit.normalform.RelationshipNormalFormGenerator;
import org.snomed.otf.owltoolkit.ontology.PropertyChain;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RelationshipFragmentTest {

	// Attributes
	private static final long CONCEPT_MODEL_ATTRIBUTE = 410662002L;
	private static final long FINDING_SITE = 363698007L;
	private static final long PROCEDURE_SITE = 363704007L;
	private static final long PROCEDURE_SITE_DIRECT = 405813007L;
	private static final long HAS_ACTIVE_INGREDIENT = 127489000L;
	private static final long HAS_PRECISE_ACTIVE_INGREDIENT = 762949000L;
	private static final long IS_MODIFICATION_OF = 738774007L;
	private static final long HAS_STRENGTH = 1142135004L;

	// Values
	private static final long BODY_STRUCTURE = 123037004L;
	private static final long HEART = 80891009L;
	private static final long LUNG = 39607008L;
	private static final long SUBSTANCE = 105590001L;
	private static final long OPIOID = 1001L;
	private static final long MORPHINE = 1002L;
	private static final long MORPHINE_SULFATE = 1003L;

	private RelationshipNormalFormGenerator generator;

	@Before
	public void setup() {
		ReasonerTaxonomy taxonomy = new ReasonerTaxonomy();
		// Entries must be added top down so that ancestors are complete
		add(taxonomy, CONCEPT_MODEL_ATTRIBUTE);
		add(taxonomy, FINDING_SITE, CONCEPT_MODEL_ATTRIBUTE);
		add(taxonomy, PROCEDURE_SITE, CONCEPT_MODEL_ATTRIBUTE);
		add(taxonomy, PROCEDURE_SITE_DIRECT, PROCEDURE_SITE);
		add(taxonomy, HAS_ACTIVE_INGREDIENT, CONCEPT_MODEL_ATTRIBUTE);
		add(taxonomy, HAS_PRECISE_ACTIVE_INGREDIENT, HAS_ACTIVE_INGREDIENT);
		add(taxonomy, IS_MODIFICATION_OF, CONCEPT_MODEL_ATTRIBUTE);
		add(taxonomy, HAS_STRENGTH, CONCEPT_MODEL_ATTRIBUTE);
		add(taxonomy, BODY_STRUCTURE);
		add(taxonomy, HEART, BODY_STRUCTURE);
		add(taxonomy, LUNG, BODY_STRUCTURE);
		add(taxonomy, SUBSTANCE);
		add(taxonomy, OPIOID, SUBSTANCE);
		add(taxonomy, MORPHINE, OPIOID);
		// Morphine sulfate is not a subtype of morphine, it is a modification of it
		add(taxonomy, MORPHINE_SULFATE, SUBSTANCE);

		// Has active ingredient o Is modification of -> Has active ingredient
		Set<PropertyChain> propertyChains = Set.of(new PropertyChain(HAS_ACTIVE_INGREDIENT, IS_MODIFICATION_OF, HAS_ACTIVE_INGREDIENT));
		generator = new RelationshipNormalFormGenerator(taxonomy, new SnomedTaxonomy(), Collections.emptyMap(), propertyChains);
		generator.getTransitiveNodeGraphs().get(IS_MODIFICATION_OF).addParent(MORPHINE_SULFATE, MORPHINE);
	}

	@Test
	public void testRuleOneSameAttributeAndValue() {
		assertTrue(fragment(FINDING_SITE, HEART).isSameOrStrongerThan(fragment(FINDING_SITE, HEART)));
	}

	@Test
	public void testRuleOneMoreSpecificValue() {
		assertTrue(fragment(FINDING_SITE, HEART).isSameOrStrongerThan(fragment(FINDING_SITE, BODY_STRUCTURE)));
		assertFalse(fragment(FINDING_SITE, BODY_STRUCTURE).isSameOrStrongerThan(fragment(FINDING_SITE, HEART)));
	}

	@Test
	public void testRuleOneMoreSpecificAttribute() {
		assertTrue(fragment(PROCEDURE_SITE_DIRECT, HEART).isSameOrStrongerThan(fragment(PROCEDURE_SITE, HEART)));
		assertFalse(fragment(PROCEDURE_SITE, HEART).isSameOrStrongerThan(fragment(PROCEDURE_SITE_DIRECT, HEART)));
	}

	@Test
	public void testRuleOneMoreSpecificAttributeAndValue() {
		assertTrue(fragment(PROCEDURE_SITE_DIRECT, HEART).isSameOrStrongerThan(fragment(PROCEDURE_SITE, BODY_STRUCTURE)));
		assertFalse(fragment(PROCEDURE_SITE, BODY_STRUCTURE).isSameOrStrongerThan(fragment(PROCEDURE_SITE_DIRECT, HEART)));
	}

	@Test
	public void testRuleOneNeedsBothAttributeAndValueToBeSpecific() {
		// More specific attribute, more general value
		assertFalse(fragment(PROCEDURE_SITE_DIRECT, BODY_STRUCTURE).isSameOrStrongerThan(fragment(PROCEDURE_SITE, HEART)));
		assertFalse(fragment(PROCEDURE_SITE, HEART).isSameOrStrongerThan(fragment(PROCEDURE_SITE_DIRECT, BODY_STRUCTURE)));
	}

	@Test
	public void testUnrelated() {
		assertFalse(fragment(FINDING_SITE, HEART).isSameOrStrongerThan(fragment(FINDING_SITE, LUNG)));
		assertFalse(fragment(FINDING_SITE, HEART).isSameOrStrongerThan(fragment(PROCEDURE_SITE, HEART)));
	}

	@Test
	public void testUniversalNotComparedWithExistential() {
		RelationshipFragment universal = new RelationshipFragment(generator,
				new Relationship(-1, -1, -1, FINDING_SITE, HEART, 0, 0, true, -1));
		assertFalse(universal.isSameOrStrongerThan(fragment(FINDING_SITE, BODY_STRUCTURE)));
		assertFalse(fragment(FINDING_SITE, HEART).isSameOrStrongerThan(new RelationshipFragment(generator,
				new Relationship(-1, -1, -1, FINDING_SITE, BODY_STRUCTURE, 0, 0, true, -1))));
	}

	@Test
	public void testConcreteValues() {
		assertTrue(concrete(HAS_STRENGTH, "#5").isSameOrStrongerThan(concrete(HAS_STRENGTH, "#5")));
		assertFalse(concrete(HAS_STRENGTH, "#5").isSameOrStrongerThan(concrete(HAS_STRENGTH, "#6")));
		assertFalse(concrete(HAS_STRENGTH, "#5").isSameOrStrongerThan(fragment(HAS_STRENGTH, HEART)));
		assertFalse(fragment(HAS_STRENGTH, HEART).isSameOrStrongerThan(concrete(HAS_STRENGTH, "#5")));
	}

	@Test
	public void testConcreteValueWithMoreSpecificAttribute() {
		assertTrue(concrete(PROCEDURE_SITE_DIRECT, "#5").isSameOrStrongerThan(concrete(PROCEDURE_SITE, "#5")));
		assertFalse(concrete(PROCEDURE_SITE, "#5").isSameOrStrongerThan(concrete(PROCEDURE_SITE_DIRECT, "#5")));
		assertFalse(concrete(PROCEDURE_SITE_DIRECT, "#5").isSameOrStrongerThan(concrete(PROCEDURE_SITE, "#6")));
	}

	@Test
	public void testRuleTwoPropertyChain() {
		// Morphine sulfate is a modification of morphine so the morphine ingredient is redundant
		assertTrue(fragment(HAS_ACTIVE_INGREDIENT, MORPHINE_SULFATE).isSameOrStrongerThan(fragment(HAS_ACTIVE_INGREDIENT, MORPHINE)));
		assertFalse(fragment(HAS_ACTIVE_INGREDIENT, MORPHINE).isSameOrStrongerThan(fragment(HAS_ACTIVE_INGREDIENT, MORPHINE_SULFATE)));
	}

	@Test
	public void testRuleTwoIncludesSupertypesOfChainValue() {
		// Morphine sulfate is a modification of morphine, which is an opioid
		assertTrue(fragment(HAS_ACTIVE_INGREDIENT, MORPHINE_SULFATE).isSameOrStrongerThan(fragment(HAS_ACTIVE_INGREDIENT, OPIOID)));
	}

	@Test
	public void testRuleTwoAttributeSubtypeOfChainSource() {
		assertTrue(fragment(HAS_PRECISE_ACTIVE_INGREDIENT, MORPHINE_SULFATE).isSameOrStrongerThan(fragment(HAS_ACTIVE_INGREDIENT, MORPHINE)));
	}

	@Test
	public void testRuleTwoOnlyAppliesToChainInferredType() {
		// Has precise active ingredient is not the inferred type of the chain
		assertFalse(fragment(HAS_PRECISE_ACTIVE_INGREDIENT, MORPHINE_SULFATE).isSameOrStrongerThan(fragment(HAS_PRECISE_ACTIVE_INGREDIENT, MORPHINE)));
	}

	@Test
	public void testRuleTwoNeedsChainSourceAttribute() {
		assertFalse(fragment(FINDING_SITE, MORPHINE_SULFATE).isSameOrStrongerThan(fragment(HAS_ACTIVE_INGREDIENT, MORPHINE)));
	}

	private RelationshipFragment fragment(long typeId, long destinationId) {
		return new RelationshipFragment(generator, new Relationship(typeId, destinationId));
	}

	private RelationshipFragment concrete(long typeId, String value) {
		return new RelationshipFragment(generator, new Relationship(0, typeId, new ConcreteValue(value)));
	}

	private static void add(ReasonerTaxonomy taxonomy, long conceptId, Long... parentIds) {
		taxonomy.addEntry(new ReasonerTaxonomyEntry(conceptId, Set.of(parentIds)));
	}
}
