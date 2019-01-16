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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.domain.Relationship;

import java.util.*;

/**
 * Compares two collections of change subjects and calls template methods whenever a removed, added or unmodified
 * element is encountered.
 */
public class RelationshipChangeProcessor {

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

	public void apply(final long conceptId, final Collection<Relationship> oldCollection, final Collection<Relationship> newCollection, final Ordering<Relationship> ordering) {

		final TreeSet<Relationship> uniqueOlds = Sets.newTreeSet(ordering);
		final ImmutableList<Relationship> sortedOld = ordering.immutableSortedCopy(oldCollection);
		final ImmutableList<Relationship> sortedNew = ordering.immutableSortedCopy(newCollection);


		// Collect removed subjects and added subjects
		// Use secondary compare to find matching groups with a new number.. we should mark this during groups.adjustOrder(inferredGroups) to be sure they are from the same group!


		for (final Relationship oldSubject : sortedOld) {
			final int idx = ordering.binarySearch(sortedNew, oldSubject);

			if (idx < 0 || !uniqueOlds.add(oldSubject)) {
				handleRemovedSubject(conceptId, oldSubject);
			}
		}

		for (final Relationship newMini : sortedNew) {
			if (ordering.binarySearch(sortedOld, newMini) < 0) {
				handleAddedSubject(conceptId, newMini);
			}
		}
	}

	protected void handleAddedSubject(long conceptId, Relationship addedSubject) {
		addedStatements.computeIfAbsent(conceptId, k -> new HashSet<>()).add(addedSubject);
		addedCount++;
	}

	protected void handleRemovedSubject(long conceptId, Relationship removedSubject) {
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
