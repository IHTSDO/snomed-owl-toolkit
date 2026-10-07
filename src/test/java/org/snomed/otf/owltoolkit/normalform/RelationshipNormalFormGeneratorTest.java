package org.snomed.otf.owltoolkit.normalform;

import org.junit.Before;
import org.junit.Test;
import org.snomed.otf.owltoolkit.classification.ReasonerTaxonomy;
import org.snomed.otf.owltoolkit.classification.ReasonerTaxonomyEntry;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.domain.Relationship;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class RelationshipNormalFormGeneratorTest {

	private static final long IS_A = Concepts.IS_A_LONG;

	// Attributes
	private static final long CONCEPT_MODEL_ATTRIBUTE = 410662002L;
	private static final long FINDING_SITE = 363698007L;
	private static final long ASSOCIATED_MORPHOLOGY = 116676008L;
	private static final long CAUSATIVE_AGENT = 246075003L;

	// Values
	private static final long BODY_STRUCTURE = 123037004L;
	private static final long HEART = 80891009L;
	private static final long HEART_VALVE = 17401000L;
	private static final long MORPHOLOGY = 49755003L;
	private static final long INFLAMMATION = 23583003L;
	private static final long ORGANISM = 410607006L;
	private static final long BACTERIA = 409822003L;
	private static final long STREPTOCOCCUS = 58800005L;

	// Concepts being normalised
	private static final long DISORDER = 64572001L;
	private static final long PARENT_1 = 1001L;
	private static final long PARENT_2 = 1002L;
	private static final long PARENT_3 = 1003L;
	private static final long CHILD = 1004L;
	private static final long GRANDCHILD = 1005L;

	private ReasonerTaxonomy reasonerTaxonomy;
	private SnomedTaxonomy snomedTaxonomy;
	private long nextRelationshipId = 100;

	@Before
	public void setup() {
		reasonerTaxonomy = new ReasonerTaxonomy();
		// Entries must be added top down so that ancestors are complete
		add(Concepts.ROOT_LONG);
		add(CONCEPT_MODEL_ATTRIBUTE, Concepts.ROOT_LONG);
		add(FINDING_SITE, CONCEPT_MODEL_ATTRIBUTE);
		add(ASSOCIATED_MORPHOLOGY, CONCEPT_MODEL_ATTRIBUTE);
		add(CAUSATIVE_AGENT, CONCEPT_MODEL_ATTRIBUTE);
		add(BODY_STRUCTURE, Concepts.ROOT_LONG);
		add(HEART, BODY_STRUCTURE);
		add(HEART_VALVE, HEART);
		add(MORPHOLOGY, BODY_STRUCTURE);
		add(INFLAMMATION, MORPHOLOGY);
		add(ORGANISM, Concepts.ROOT_LONG);
		add(BACTERIA, ORGANISM);
		add(STREPTOCOCCUS, BACTERIA);
		add(DISORDER, Concepts.ROOT_LONG);
		add(PARENT_1, DISORDER);
		add(PARENT_2, DISORDER);
		add(PARENT_3, DISORDER);
		add(CHILD, PARENT_1, PARENT_2, PARENT_3);
		add(GRANDCHILD, CHILD);
		reasonerTaxonomy.getAttributeIds().addAll(List.of(CONCEPT_MODEL_ATTRIBUTE, FINDING_SITE, ASSOCIATED_MORPHOLOGY, CAUSATIVE_AGENT));

		snomedTaxonomy = new SnomedTaxonomy();
		stated(PARENT_1, 0, CAUSATIVE_AGENT, BACTERIA);
		stated(PARENT_1, 1, FINDING_SITE, HEART);

		// Both stronger than parent 1
		stated(PARENT_2, 0, CAUSATIVE_AGENT, STREPTOCOCCUS);
		stated(PARENT_2, 1, FINDING_SITE, HEART_VALVE);

		// Group is stronger than the group of parent 1 but not the group of parent 2
		stated(PARENT_3, 1, FINDING_SITE, HEART);
		stated(PARENT_3, 1, ASSOCIATED_MORPHOLOGY, INFLAMMATION);

		// Redundant, the same attribute is inherited within a larger group
		stated(GRANDCHILD, 1, ASSOCIATED_MORPHOLOGY, INFLAMMATION);
	}

	@Test
	public void testSingleParentKeepsOwnGroups() {
		Map<Long, Set<Relationship>> results = normalise();

		assertEquals(Set.of(
				"0 116680003 64572001",
				"0 246075003 409822003",
				"1 363698007 80891009"), describe(results.get(PARENT_1)));
	}

	@Test
	public void testRedundantGroupsFromMultipleParentsRemoved() {
		Map<Long, Set<Relationship>> results = normalise();

		Set<Relationship> child = results.get(CHILD);
		assertChildForm(child, PARENT_1, PARENT_2, PARENT_3);
	}

	@Test
	public void testGroupsInheritedThroughMultipleParentChild() {
		Map<Long, Set<Relationship>> results = normalise();

		assertChildForm(results.get(GRANDCHILD), CHILD);
	}

	private void assertChildForm(Set<Relationship> relationships, Long... parentIds) {
		Set<Long> isAParents = relationships.stream().filter(r -> r.getTypeId() == IS_A).map(Relationship::getDestinationId).collect(Collectors.toSet());
		assertEquals(Set.of(parentIds), isAParents);

		List<Relationship> attributes = relationships.stream().filter(r -> r.getTypeId() != IS_A).collect(Collectors.toList());
		assertEquals("Unexpected attributes " + describe(relationships), 4, attributes.size());

		Relationship causativeAgent = find(attributes, CAUSATIVE_AGENT, STREPTOCOCCUS);
		assertEquals(0, causativeAgent.getGroup());

		Relationship heartValveSite = find(attributes, FINDING_SITE, HEART_VALVE);
		Relationship heartSite = find(attributes, FINDING_SITE, HEART);
		Relationship inflammation = find(attributes, ASSOCIATED_MORPHOLOGY, INFLAMMATION);
		assertNotEquals(0, heartValveSite.getGroup());
		assertNotEquals(0, heartSite.getGroup());
		assertEquals("Relationships from the same parent group stay together", heartSite.getGroup(), inflammation.getGroup());
		assertNotEquals("Groups from different parents stay apart", heartSite.getGroup(), heartValveSite.getGroup());
	}

	private Map<Long, Set<Relationship>> normalise() {
		RelationshipNormalFormGenerator generator = new RelationshipNormalFormGenerator(reasonerTaxonomy, snomedTaxonomy,
				Collections.emptyMap(), Collections.emptySet());
		RelationshipChangeProcessor processor = new RelationshipChangeProcessor();
		generator.collectNormalFormChanges(processor);
		return processor.getAddedStatements();
	}

	private Relationship find(Collection<Relationship> relationships, long typeId, long destinationId) {
		return relationships.stream()
				.filter(r -> r.getTypeId() == typeId && r.getDestinationId() == destinationId)
				.findFirst()
				.orElseThrow(() -> new AssertionError("Missing " + typeId + " = " + destinationId + " in " + describe(relationships)));
	}

	private static Set<String> describe(Collection<Relationship> relationships) {
		return relationships.stream().map(r -> r.getGroup() + " " + r.getTypeId() + " " + r.getDestinationId()).collect(Collectors.toSet());
	}

	private void add(long conceptId, Long... parentIds) {
		reasonerTaxonomy.addEntry(new ReasonerTaxonomyEntry(conceptId, Set.of(parentIds)));
	}

	private void stated(long conceptId, int group, long typeId, long destinationId) {
		snomedTaxonomy.addOrModifyRelationship(true, conceptId,
				new Relationship(nextRelationshipId++, 20260101, Long.parseLong(Concepts.SNOMED_CT_CORE_MODULE), typeId, destinationId, group, 0, false,
						Long.parseLong(Concepts.STATED_RELATIONSHIP)));
	}
}
