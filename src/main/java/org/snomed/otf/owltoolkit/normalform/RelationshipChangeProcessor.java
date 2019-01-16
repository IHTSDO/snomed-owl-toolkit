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
			.thenComparing(Relationship::isUniversal)
			.thenComparing(Relationship::isDestinationNegated);

	private static final Comparator<Relationship> RELATIONSHIP_COMPARATOR_WITHOUT_GROUP = Comparator
			.comparing(Relationship::getTypeId)
			.thenComparing(Relationship::getDestinationId)
			.thenComparing(Relationship::getUnionGroup)
			.thenComparing(Relationship::isUniversal)
			.thenComparing(Relationship::isDestinationNegated);

	private final Map<Long, Set<Relationship>> addedStatements;
	private final Map<Long, Set<Relationship>> removedStatements;
	private Long addedCount;
	private Long removedCount;
	private boolean skipAdditionalPartOf;

	public RelationshipChangeProcessor(boolean skipAdditionalRelationship) {
		addedCount = 0L;
		removedCount = 0L;
		addedStatements = new Long2ObjectOpenHashMap<>();
		removedStatements = new Long2ObjectOpenHashMap<>();
		skipAdditionalPartOf = skipAdditionalRelationship;
	}

	public void apply(final long conceptId, final Collection<Relationship> existingRelationships, final Collection<Relationship> newRelationships) {

		final TreeSet<Relationship> uniqueOlds = new TreeSet<>(RELATIONSHIP_COMPARATOR_ALL_FIELDS);
		final List<Relationship> sortedOld = newSortedList(existingRelationships, RELATIONSHIP_COMPARATOR_ALL_FIELDS);
		final List<Relationship> sortedNew = newSortedList(newRelationships, RELATIONSHIP_COMPARATOR_ALL_FIELDS);

		final Map<Relationship, Relationship> updatedRelationshipNewOldMap = new HashMap<>();

		// For each existing relationship if it can not be found in the new set mark it as removed
		for (final Relationship oldSubject : sortedOld) {
			final int i = Collections.binarySearch(sortedNew, oldSubject, RELATIONSHIP_COMPARATOR_ALL_FIELDS);
			if (i < 0 || !uniqueOlds.add(oldSubject)) {

				// Handle the case where existing relationships are being moved out of group 0.
				// This will happen as editions move from stated relationships to OWL axioms.
				if (oldSubject.getGroup() == 0 && oldSubject.getTypeId() != Concepts.IS_A_LONG) {
					final int y = Collections.binarySearch(sortedNew, oldSubject, RELATIONSHIP_COMPARATOR_WITHOUT_GROUP);
					if (y >= 0) {
						// Update existing relationship rather than creating new
						updatedRelationshipNewOldMap.put(sortedNew.get(y), oldSubject);
						continue;
					}
				}

				handleRemovedSubject(conceptId, oldSubject);
			}
		}

		// For each relationship in the new set if it does not match one in the old set mark is as added
		for (final Relationship newMini : sortedNew) {
			if (updatedRelationshipNewOldMap.containsKey(newMini)) {
				// Update existing relationship
				Relationship existingRelationship = updatedRelationshipNewOldMap.get(newMini);
				existingRelationship.setGroup(newMini.getGroup());
				handleAddedSubject(conceptId, existingRelationship);
			} else if (Collections.binarySearch(sortedOld, newMini, RELATIONSHIP_COMPARATOR_ALL_FIELDS) < 0) {
				newMini.clearId();// Make sure stated relationship ids don't get through into new inferred relationship results
				handleAddedSubject(conceptId, newMini);
			}
		}
	}

	private List<Relationship> newSortedList(Collection<Relationship> relationships, Comparator<Relationship> comparator) {
		final List<Relationship> sortedOld = new ArrayList<>(relationships);
		sortedOld.sort(comparator);
		return sortedOld;
	}

	private void handleAddedSubject(long conceptId, Relationship addedSubject) {
		addedStatements.computeIfAbsent(conceptId, k -> new HashSet<>()).add(addedSubject);
		addedCount++;
	}

	void handleRemovedSubject(long conceptId, Relationship removedSubject) {
		if (skipAdditionalPartOf) {
			//We will preserve any "Additional" characteristic types eg PartOf relationships
			if (removedSubject.getCharacteristicTypeId() == -1 || removedSubject.getCharacteristicTypeId() != Concepts.ADDITIONAL_RELATIONSHIP_LONG) {
				removedStatements.computeIfAbsent(conceptId, k -> new HashSet<>()).add(removedSubject);
				removedCount++;
			}
		} else {
			removedStatements.computeIfAbsent(conceptId, k -> new HashSet<>()).add(removedSubject);
			removedCount++;
		}
	}

	public Long getAddedCount() {
		return addedCount;
	}

	public Long getRemovedCount() {
		return removedCount;
	}

	public Map<Long, Set<Relationship>> getAddedStatements() {
		return addedStatements;
	}

	public Map<Long, Set<Relationship>> getRemovedStatements() {
		return removedStatements;
	}
}
