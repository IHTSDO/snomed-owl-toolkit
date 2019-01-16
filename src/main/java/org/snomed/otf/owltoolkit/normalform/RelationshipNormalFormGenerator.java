/*
 * Copyright 2017 SNOMED International, http://snomed.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 *
 * Original author law223 - initial implementation in Snorocket SNOMED API
 */
package org.snomed.otf.owltoolkit.normalform;

import com.google.common.base.Stopwatch;
import com.google.common.collect.*;
import com.google.common.collect.Maps.EntryTransformer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.classification.ReasonerTaxonomy;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.domain.AxiomRepresentation;
import org.snomed.otf.owltoolkit.domain.Relationship;
import org.snomed.otf.owltoolkit.normalform.internal.*;
import org.snomed.otf.owltoolkit.normalform.transitive.NodeGraph;
import org.snomed.otf.owltoolkit.ontology.PropertyChain;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.snomed.otf.owltoolkit.constants.Concepts.IS_A_LONG;

/**
 * Transforms a subsumption hierarchy and a set of non-ISA relationships into
 * distribution normal form.
 */
public final class RelationshipNormalFormGenerator {

	/**
	 * Special group number indicating that the next free group/union group number
	 * should be used when the fragments in this group/union group are converted into
	 * relationships.
	 */
	public static final int NUMBER_NOT_PRESERVED = -1;

	private static final long INTERNATIONAL_CORE_MODULE_ID = Long.parseLong(Concepts.SNOMED_CT_CORE_MODULE);
	private static final int ZERO_GROUP = 0;
	private static final Comparator<Group> CORE_MODULE_GROUP_COMPARATOR = (o1, o2) -> {
		long moduleId1 = o1.getUnionGroups().iterator().next().getRelationshipFragments().iterator().next().getModuleId();
		long moduleId2 = o2.getUnionGroups().iterator().next().getRelationshipFragments().iterator().next().getModuleId();
		if (moduleId1 == INTERNATIONAL_CORE_MODULE_ID && moduleId2 != INTERNATIONAL_CORE_MODULE_ID) {
			return -1;
		} else if (moduleId1 != INTERNATIONAL_CORE_MODULE_ID && moduleId2 == INTERNATIONAL_CORE_MODULE_ID) {
			return 1;
		}
		return 0;
	};
	private static final Logger LOGGER = LoggerFactory.getLogger(RelationshipNormalFormGenerator.class);

	private final ReasonerTaxonomy reasonerTaxonomy;
	private final SnomedTaxonomy snomedTaxonomy;
	private final Set<PropertyChain> propertyChains;

	private final Map<Long, Collection<Relationship>> generatedNonIsACache = new Long2ObjectOpenHashMap<>();
	private final Set<Long> traversableProperties;
	private final Map<Long, NodeGraph> transitiveNodeGraphs = new HashMap<>();
	private final Map<Long, Set<AxiomRepresentation>> conceptAxiomStatementMap;

	/**
	 * Creates a new distribution normal form generator instance.
	 * @param reasonerTaxonomy the reasoner to extract results from (may not be {@code null})
	 * @param snomedTaxonomy the taxonomy as it existed before this classification run (may not be {@code null})
	 * @param conceptAxiomStatementMap map of concept id to axiom set
	 * @param propertyChains collection of property chains
	 */
	public RelationshipNormalFormGenerator(final ReasonerTaxonomy reasonerTaxonomy, final SnomedTaxonomy snomedTaxonomy,
			final Map<Long, Set<AxiomRepresentation>> conceptAxiomStatementMap, final Set<PropertyChain> propertyChains) {

		this.reasonerTaxonomy = reasonerTaxonomy;
		this.snomedTaxonomy = snomedTaxonomy;
		this.propertyChains = propertyChains;
		this.conceptAxiomStatementMap = conceptAxiomStatementMap;

		traversableProperties = propertyChains.stream().map(PropertyChain::getDestinationType).collect(Collectors.toSet());

		// Initialise node graphs for properties we need to traverse
		LOGGER.info("Initialising node graphs for traversable properties {}", traversableProperties);
		traversableProperties.forEach(id -> transitiveNodeGraphs.put(id, new NodeGraph()));
	}

	/**
	 * Computes and returns all changes as a result of normal form computation.
	 *
	 * @param processor the change processor to route changes to
	 * @return the total number of generated components
	 */
	public final void collectNormalFormChanges(final RelationshipChangeProcessor processor) {
		LOGGER.info(">>> Relationship normal form generation");
		final Stopwatch stopwatch = Stopwatch.createStarted();
		final List<Long> entries = reasonerTaxonomy.getConceptIds();

		for (Long conceptId : entries) {
			firstNormalisationPass(conceptId);
		}

		for (Long conceptId : entries) {
			final Collection<Relationship> existingComponents = snomedTaxonomy.getInferredRelationships((long) conceptId);
			final Collection<Relationship> generatedComponents = secondNormalisationPass(conceptId);
			processor.apply(conceptId, existingComponents, generatedComponents);
		}

		LOGGER.info(MessageFormat.format("<<< Relationship normal form generation [{0}]", stopwatch.toString()));
	}

	/**
	 * Computes and caches a set of components in normal form for the specified concept.
	 * The first pass uses the is-a hierarchy for normalisation.
	 * This hierarchy is available during the first pass because of the breath first order of processing concepts.
	 *
	 * @param conceptId the concept for which components should be generated
	 */
	private void firstNormalisationPass(long conceptId) {
		final Set<Relationship> inferredNonIsAFragments = getInferredNonIsAFragmentsInNormalForm(conceptId);

		// Place results in the cache, so children can re-use it
		generatedNonIsACache.put(conceptId, ImmutableList.copyOf(inferredNonIsAFragments));

		// Add to transitive graphs
		inferredNonIsAFragments.stream().filter(r -> traversableProperties.contains(r.getTypeId())).forEach(r ->
				transitiveNodeGraphs.get(r.getTypeId()).addParent(conceptId, r.getDestinationId()));
	}

	/**
	 * Performs additional normalisation as required before returning components in normal form for the specified concept.
	 * The second pass uses property chains and transitive properties in order to further normalise components.
	 * Other transitive hierarchies can not be guaranteed to be complete during the first pass because the super-type of a
	 * concept in a transitive property hierarchy may be at a lower level in the is-a hierarchy meaning that it's processed later during the first pass.
	 *
	 * @param conceptId the concept for which components should be generated
	 * @return the generated components of the specified concept in normal form
	 */
	private Collection<Relationship> secondNormalisationPass(final long conceptId) {
		final Set<Long> directSuperTypes = reasonerTaxonomy.getParents(conceptId);

		// Step 1: collect IS-A relationships
		final Iterable<Relationship> inferredIsAFragments = getInferredIsAFragments(conceptId, directSuperTypes);

		Iterable<Relationship> inferredNonIsAFragments = generatedNonIsACache.get(conceptId);
		if (!propertyChains.isEmpty()) {
			for (Relationship inferredNonIsAFragment : inferredNonIsAFragments) {
				// Is there a property chain for this relationship?
				if (propertyChains.stream()
						.filter(propertyChain -> propertyChain.getSourceType().equals(inferredNonIsAFragment.getTypeId()))
						.count() > 0) {

					inferredNonIsAFragments = getInferredNonIsAFragmentsInNormalForm(conceptId);
					break;
				}
			}
		}

		return ImmutableList.copyOf(Iterables.concat(inferredIsAFragments, inferredNonIsAFragments));
	}

	private Set<Relationship> getInferredNonIsAFragmentsInNormalForm(Long conceptId) {
		final Set<Long> directSuperTypes = reasonerTaxonomy.getParents(conceptId);

		// Step 2: get all non IS-A relationships from ancestors and remove redundancy, then cache the results for later use
		final Map<Long, Collection<Relationship>> otherNonIsAFragments = new Long2ObjectOpenHashMap<>();

		/*
		 * We can rely on the fact that the tree is processed in breadth-first order, so the parents' non-IS A relationships
		 * will already be present in the cache
		 */
		for (Long directSuperTypeId : directSuperTypes) {
			otherNonIsAFragments.put(directSuperTypeId, getCachedNonIsAFragments(directSuperTypeId));
		}

		Set<AxiomRepresentation> axiomRepresentations = conceptAxiomStatementMap.get(conceptId);
		final Collection<Relationship> ownStatedNonIsaRelationships;
		if (axiomRepresentations == null) {
			ownStatedNonIsaRelationships = snomedTaxonomy.getNonIsAStatements(conceptId);
		} else {
			ownStatedNonIsaRelationships = new ArrayList<>(snomedTaxonomy.getNonIsAStatements(conceptId));
			ownStatedNonIsaRelationships.addAll(axiomRepresentations.stream()
					.filter(axiomRepresentation -> conceptId.equals(axiomRepresentation.getLeftHandSideNamedConcept()))
					.map(AxiomRepresentation::getRightHandSideRelationships)
					.map(Map::values)
					.flatMap(Collection::stream)
					.flatMap(Collection::stream)
					.filter(relationship -> relationship.getTypeId() != Concepts.IS_A_LONG)
					.collect(Collectors.toList()));
		}

		final Collection<Relationship> ownInferredFragments = snomedTaxonomy.getInferredRelationships(conceptId);
		final Collection<Relationship> ownInferredNonIsaFragments = Collections2.filter(ownInferredFragments, input -> input.getTypeId() != IS_A_LONG);

		return getInferredNonIsAFragments(conceptId,
				ownInferredNonIsaFragments,
				ownStatedNonIsaRelationships,
				otherNonIsAFragments);
	}

	/**
	 * Outbound relationships are calculated in the following fashion:
	 *
	 * <ol>
	 * <li>
	 * The given concept's <i>direct supertypes</i> are collected from the
	 * inferred taxonomy, and IS_A relationships are created for all of
	 * them;
	 * </li>
	 * <li>
	 * The <i>given concept and all of its ancestors</i> are gathered from
	 * the taxonomy; the outbound non-IS_A relationship set reachable from
	 * these concepts is extracted; this set is further reduced to contain
	 * only non-redundant relationships; the resulting relationship groups
	 * are numbered continuously from 1;
	 * </li>
	 * <li>
	 * Existing inferred non-IS_A relationships are collected for the
	 * concept, forming relationship groups;
	 * </li>
	 * <li>
	 * Where applicable, new inferred relationship group and union group
	 * numbers are shuffled to preserve existing values.
	 * </li>
	 * </ol>
	 *
	 */
	private Set<Relationship> getInferredNonIsAFragments(long conceptId,
			final Collection<Relationship> ownInferredNonIsAFragments,
			final Collection<Relationship> ownStatedNonIsAFragments,
			final Map<Long, Collection<Relationship>> parentStatedNonIsAFragments) {

		// Index existing inferred non-IS A relationship groups into a GroupSet (without redundancy check)
		final GroupSet inferredGroups = new GroupSet();
		final Iterable<Group> ownInferredGroups = toGroups(true, ownInferredNonIsAFragments);
		for (final Group ownInferredGroup : ownInferredGroups) {
			inferredGroups.addUnique(ownInferredGroup);
		}

		// Eliminate redundancy between existing stated non-IS A relationship groups
		final GroupSet groups = new GroupSet();
		final Iterable<Group> ownGroups = toGroups(false, ownStatedNonIsAFragments);
		Iterables.addAll(groups, ownGroups);

		// Continue by adding stated non-IS A relationship groups from parents indicated by the reasoner
		for (Long parentId : parentStatedNonIsAFragments.keySet()) {
			final Iterable<Group> otherGroups = toGroups(false, parentStatedNonIsAFragments.get(parentId));
			Iterables.addAll(groups, otherGroups);
		}

		// The remaining non-redundant groups should be numbered from 1
		groups.fillNumbers();

		// Sort groups to favour the core module
		inferredGroups.sort(CORE_MODULE_GROUP_COMPARATOR);

		// Shuffle around the numbers to match existing inferred group numbers as much as possible
		groups.adjustOrder(inferredGroups);

		// Convert groups back to individual statement fragments
		return fromGroupSet(groups);
	}

	private Collection<Relationship> getCachedNonIsAFragments(final long directSuperTypeId) {
		return generatedNonIsACache.get(directSuperTypeId);
	}

	private Iterable<Relationship> getInferredIsAFragments(final long conceptId, final Set<Long> parentIds) {
		return parentIds.stream().map(parentId -> new Relationship(IS_A_LONG, parentId)).collect(Collectors.toSet());
	}

	private Iterable<Group> toGroups(final boolean preserveNumbers, final Collection<Relationship> nonIsARelationshipFragments) {

		final Map<Integer, Collection<Relationship>> relationshipsByGroupId = Multimaps.index(nonIsARelationshipFragments, Relationship::getGroup).asMap();

		final Collection<Collection<Group>> groups = Maps.transformEntries(relationshipsByGroupId,
				(EntryTransformer<Integer, Collection<Relationship>, Collection<Group>>) (key, values) -> {
					final Iterable<UnionGroup> unionGroups = toUnionGroups(preserveNumbers, values);
					final Set<UnionGroup> disjointUnionGroups = getDisjointComparables(unionGroups);

					if (key == 0) {
						// Relationships in group 0 form separate groups
						return ImmutableList.copyOf(toZeroGroups(disjointUnionGroups));
					} else {
						// Other group numbers produce a single group from all fragments
						return ImmutableList.of(toNonZeroGroup(preserveNumbers, key, disjointUnionGroups));
					}
				}).values();

		return Iterables.concat(groups);
	}

	private Iterable<Group> toZeroGroups(final Set<UnionGroup> disjointUnionGroups) {
		return disjointUnionGroups.stream().map(unionGroup -> {
			final Group group = new Group(ImmutableList.of(unionGroup));
			group.setGroupNumber(ZERO_GROUP);
			return group;
		}).collect(Collectors.toList());
	}

	private Group toNonZeroGroup(final boolean preserveNumbers, final int groupNumber, final Iterable<UnionGroup> disjointUnionGroups) {
		final Group group = new Group(disjointUnionGroups);
		if (preserveNumbers) {
			group.setGroupNumber(groupNumber);
		}
		return group;
	}

	private Iterable<UnionGroup> toUnionGroups(final boolean preserveNumbers, final Collection<Relationship> values) {
		final Map<Integer, Collection<Relationship>> relationshipsByUnionGroupId = Multimaps.index(values, Relationship::getUnionGroup).asMap();

		final Collection<Collection<UnionGroup>> unionGroups = Maps.transformEntries(relationshipsByUnionGroupId,
				(EntryTransformer<Integer, Collection<Relationship>, Collection<UnionGroup>>) (key, values1) -> {
					if (key == 0) {
						// Relationships in union group 0 form separate union groups
						return ImmutableList.copyOf(toZeroUnionGroups(values1));
					} else {
						// Other group numbers produce a single union group from all fragments
						return ImmutableList.of(toNonZeroUnionGroup(preserveNumbers, key, values1));
					}
				}).values();

		return Iterables.concat(unionGroups);
	}

	private Iterable<UnionGroup> toZeroUnionGroups(final Collection<Relationship> values) {
		return values.stream().map(relationship -> {
			final UnionGroup unionGroup = new UnionGroup(ImmutableList.of(new RelationshipFragment(RelationshipNormalFormGenerator.this, relationship)));
			unionGroup.setUnionGroupNumber(ZERO_GROUP);
			return unionGroup;
		}).collect(Collectors.toSet());
	}

	private UnionGroup toNonZeroUnionGroup(final boolean preserveNumbers, final int unionGroupNumber, final Collection<Relationship> values) {
		Set<RelationshipFragment> fragments = values.stream()
				.map(relationship -> new RelationshipFragment(RelationshipNormalFormGenerator.this, relationship))
				.collect(Collectors.toSet());

		final UnionGroup unionGroup = new UnionGroup(fragments);
		if (preserveNumbers) {
			unionGroup.setUnionGroupNumber(unionGroupNumber);
		}
		return unionGroup;
	}

	/**
	 * Filters {@link SemanticComparable}s so that the returned Iterable only
	 * includes elements that are not redundant with respect to each other. The
	 * following steps are taken to ensure that no redundant SemanticComparables
	 * remain in the output Iterable:
	 * <p>
	 * <ol>
	 * <li>
	 * a candidate set is maintained for possible results;
	 * </li>
	 * <li>
	 * each incoming item is checked against all existing candidates to see
	 * if they are redundant (in which case the incoming item is skipped), or if
	 * it makes any of the candidates redundant (in which case the redundant
	 * candidates are removed from the set, and the incoming item gets added);
	 * </li>
	 * <li>
	 * all surviving items are returned.
	 * </li>
	 * </ol>
	 * <p>
	 * The returned Iterable is backed by a locally created Set, and supports
	 * <code>remove()</code>.
	 *
	 * @param comparables
	 *            the comparables to filter
	 *
	 * @return an {@link Set} that only includes the reduced comparables
	 */
	private <T extends SemanticComparable<T>> Set<T> getDisjointComparables(final Iterable<T> comparables) {
		final Set<T> candidates = Sets.newHashSet();
		final Set<T> redundant = Sets.newHashSet();

		for (final T comparable : comparables) {

			redundant.clear();
			boolean found = false;

			for (final T candidate : candidates) {

				if (candidate.isSameOrStrongerThan(comparable)) {
					found = true;
					break;
				} else if (comparable.isSameOrStrongerThan(candidate)) {
					redundant.add(candidate);
				}
			}

			if (!found) {
				candidates.removeAll(redundant);
				candidates.add(comparable);
			}
		}

		return candidates;
	}

	private Set<Relationship> fromGroupSet(final GroupSet groups) {
		return groups.stream().map(this::fromGroup).flatMap(Collection::stream).collect(Collectors.toSet());
	}

	private Set<Relationship> fromGroup(final Group group) {
		return group.getUnionGroups().stream().map(unionGroup ->  fromUnionGroup(unionGroup, group.getGroupNumber(), unionGroup.getUnionGroupNumber()))
				.flatMap(Collection::stream).collect(Collectors.toSet());
	}

	private Set<Relationship> fromUnionGroup(final UnionGroup unionGroup, final int groupNumber, final int unionGroupNumber) {
		return unionGroup.getRelationshipFragments().stream()
				.map(input -> new Relationship(
						input.getStatementId(),
						-1,
						input.getModuleId(),
						input.getTypeId(),
						input.getDestinationId(),
						input.isDestinationNegated(),
						groupNumber,
						unionGroupNumber,
						input.isUniversal(),
						-1))
				.collect(Collectors.toSet());
	}

	public ReasonerTaxonomy getReasonerTaxonomy() {
		return reasonerTaxonomy;
	}

	public SnomedTaxonomy getSnomedTaxonomy() {
		return snomedTaxonomy;
	}

	public Set<PropertyChain> getPropertyChains() {
		return propertyChains;
	}

	public Map<Long, NodeGraph> getTransitiveNodeGraphs() {
		return transitiveNodeGraphs;
	}

}
