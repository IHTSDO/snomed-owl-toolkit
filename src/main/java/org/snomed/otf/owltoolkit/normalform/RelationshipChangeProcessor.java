/*
 * Copyright 2017 SNOMED International, http://snomed.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.snomed.otf.owltoolkit.normalform;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.domain.Relationship;

import java.util.*;

/**
 * Compares two collections of change subjects and calls template methods whenever a removed, added or unmodified
 * element is encountered.
 */
public class RelationshipChangeProcessor {

	private static final Comparator<Relationship> RELATIONSHIP_COMPARATOR_ALL_FIELDS = Comparator
			.comparing(Relationship::getTypeId)
			.thenComparing(Relationship::getDestinationId)
			.thenComparing(Relationship::getGroup)
			.thenComparing(Relationship::getUnionGroup)
			.thenComparing(Relationship::isUniversal);

	private static final Comparator<Relationship> RELATIONSHIP_COMPARATOR_WITH_MODULE_ID = Comparator
			.comparing(Relationship::getTypeId)
			.thenComparing(Relationship::getDestinationId)
			.thenComparing(Relationship::getGroup)
			.thenComparing(Relationship::getUnionGroup)
			.thenComparing(Relationship::isUniversal)
			.thenComparing(Relationship::getModuleId, Comparator.reverseOrder());

	private static final Comparator<Relationship> RELATIONSHIP_COMPARATOR_WITHOUT_GROUP = Comparator
			.comparing(Relationship::getTypeId)
			.thenComparing(Relationship::getDestinationId)
			.thenComparing(Relationship::getUnionGroup)
			.thenComparing(Relationship::isUniversal);

	private final Map<Long, Set<Relationship>> addedStatements;
	private final Map<Long, Set<Relationship>> removedStatements;
	private Long addedCount;
	private Long updatedCount;
	private Long removedDueToConceptInactivationCount;

	public RelationshipChangeProcessor() {
		addedCount = 0L;
		updatedCount = 0L;
		removedDueToConceptInactivationCount = 0L;
		addedStatements = new Long2ObjectOpenHashMap<>();
		removedStatements = new Long2ObjectOpenHashMap<>();
	}

	public void apply(final long conceptId, final Collection<Relationship> existingRelationships, final Collection<Relationship> newRelationships) {

		final List<Relationship> sortedOld = newSortedList(existingRelationships, RELATIONSHIP_COMPARATOR_ALL_FIELDS);
		final List<Relationship> sortedNew = newSortedList(newRelationships, RELATIONSHIP_COMPARATOR_ALL_FIELDS);

		final Map<Relationship, Relationship> updatedRelationshipNewOldMap = new HashMap<>();
		final Set<Integer> uniqueIndex = new HashSet<>();

		// For each existing relationship if it can not be found in the new set mark it as removed
		for (final Relationship oldSubject : newSortedList(existingRelationships, RELATIONSHIP_COMPARATOR_WITH_MODULE_ID)) {
			final int i = Collections.binarySearch(sortedNew, oldSubject, RELATIONSHIP_COMPARATOR_ALL_FIELDS);
			if (i < 0) {
				// Handle the case where existing self grouped relationships are being moved out of group 0.
				// This will happen as editions move from stated relationships to OWL axioms.
				if (oldSubject.getGroup() == 0 && oldSubject.getTypeId() != Concepts.IS_A_LONG) {
					final int y = Collections.binarySearch(sortedNew, oldSubject, RELATIONSHIP_COMPARATOR_WITHOUT_GROUP);
					if (y >= 0) {
						// If this is the only relationship in the group we will update the group number.

						int newGroup = sortedNew.get(y).getGroup();
						int relationshipsInGroupCount = 0;
						for (Relationship relationship : sortedNew) {
							if (relationship.getGroup() == newGroup) {
								relationshipsInGroupCount++;
							}
						}
						if (relationshipsInGroupCount == 1) {
							//check that new relationship with group change exists already or not
							int z = Collections.binarySearch(sortedOld, sortedNew.get(y), RELATIONSHIP_COMPARATOR_ALL_FIELDS);
							if (z < 0) {
								// Update existing relationship rather than creating new
								updatedRelationshipNewOldMap.put(sortedNew.get(y), oldSubject);
								continue;
							}
						}
						// We keep the relationship ID when just the relationship group number changes
						// but if the triple joins another group of relationships we need to make this one inactive and recreate.
					}
				}
				handleRedundantRelationship(conceptId, oldSubject);
			} else if (!uniqueIndex.add(i)) {
				// Existing relationship is a duplicate
				handleRedundantRelationship(conceptId, oldSubject);
			}
		}

		// For each relationship in the new set if it does not match one in the old set mark is as added
		for (final Relationship newMini : sortedNew) {
			if (updatedRelationshipNewOldMap.containsKey(newMini)) {
				// Update existing relationship
				Relationship existingRelationship = updatedRelationshipNewOldMap.get(newMini);
				existingRelationship.setGroup(newMini.getGroup());
				handleAddedOrChangedRelationship(conceptId, existingRelationship);
				updatedCount++;
			} else if (Collections.binarySearch(sortedOld, newMini, RELATIONSHIP_COMPARATOR_ALL_FIELDS) < 0) {
				newMini.clearId();// Make sure stated relationship ids don't get through into new inferred relationship results
				handleAddedOrChangedRelationship(conceptId, newMini);
				addedCount++;
			}
		}
	}

	private List<Relationship> newSortedList(Collection<Relationship> relationships, Comparator<Relationship> comparator) {
		final List<Relationship> sortedOld = new ArrayList<>(relationships);
		sortedOld.sort(comparator);
		return sortedOld;
	}

	private void handleAddedOrChangedRelationship(long conceptId, Relationship addedSubject) {
		addedStatements.computeIfAbsent(conceptId, k -> new HashSet<>()).add(addedSubject);
	}

	void handleRedundantRelationship(long conceptId, Relationship removedSubject) {
		//We will preserve any "Additional" characteristic types eg PartOf relationships
		if (removedSubject.getCharacteristicTypeId() == -1 || removedSubject.getCharacteristicTypeId() != Concepts.ADDITIONAL_RELATIONSHIP_LONG) {
			removedStatements.computeIfAbsent(conceptId, k -> new HashSet<>()).add(removedSubject);
		}
	}

	void processRemovalsDueToInactivation(Long inactiveConceptId, Set<Relationship> inferredRelationships) {
		if (inferredRelationships.isEmpty()) {
			return;
		}
		removedDueToConceptInactivationCount += inferredRelationships.size();
		removedStatements.put(inactiveConceptId, inferredRelationships);
	}

	public Long getAddedCount() {
		return addedCount;
	}

	public Long getUpdatedCount() {
		return updatedCount;
	}

	public Long getRedundantCount() {
		long redundantCount = 0L;
		for (Set<Relationship> value : removedStatements.values()) {
			redundantCount += value.size();
		}
		redundantCount -= removedDueToConceptInactivationCount;
		return redundantCount;
	}

	public Long getRemovedDueToConceptInactivationCount() {
		return removedDueToConceptInactivationCount;
	}

	public Map<Long, Set<Relationship>> getAddedStatements() {
		return addedStatements;
	}

	public Map<Long, Set<Relationship>> getRemovedStatements() {
		return removedStatements;
	}
}
