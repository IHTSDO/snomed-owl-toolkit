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
package org.snomed.otf.owltoolkit.normalform.internal;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.snomed.otf.owltoolkit.normalform.RelationshipNormalFormGenerator;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents a relationship group, consisting of a(n optionally preserved)
 * group number and a list of union groups. The object (source concept) is
 * not stored with the group; it is assumed to be known in context.
 */
public final class Group implements SemanticComparable<Group> {

	private final List<UnionGroup> unionGroups;

	private int groupNumber = RelationshipNormalFormGenerator.NUMBER_NOT_PRESERVED;

	/**
	 * Creates a new group instance.
	 *
	 * @param unionGroups
	 *            the relationship union groups to associate with this group
	 *            (may not be <code>null</code>)
	 */
	public Group(final Iterable<UnionGroup> unionGroups) {
		checkArgument(unionGroups != null, "unionGroups is null.");
		this.unionGroups = ImmutableList.copyOf(unionGroups);
	}

	public List<UnionGroup> getUnionGroups() {
		return unionGroups;
	}

	public int getGroupNumber() {
		return groupNumber;
	}

	public void setGroupNumber(final int groupNumber) {
		checkArgument(groupNumber > RelationshipNormalFormGenerator.NUMBER_NOT_PRESERVED, "Illegal group number '%s'.", groupNumber);
		this.groupNumber = groupNumber;
	}

	@Override
	public boolean isSameOrStrongerThan(final Group other) {

		/*
		 * Things same or stronger than A AND B AND C:
		 *
		 * - A' AND B AND C, where A' is a subclass of A
		 * - A AND B AND C AND D
		 *
		 * So for each end every union group in "other", we'll have to find
		 * a more expressive union group in this group. Points are awarded
		 * if we have extra union groups not used in the comparison.
		 */
		for (final UnionGroup otherUnionGroup : other.unionGroups) {

			boolean found = false;

			for (final UnionGroup ourUnionGroup : unionGroups) {

				if (ourUnionGroup.isSameOrStrongerThan(otherUnionGroup)) {
					found = true;
					break;
				}
			}

			if (!found) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		return 31 + unionGroups.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Group)) {
			return false;
		}

		final Group other = (Group) obj;

		if (unionGroups.size() != other.unionGroups.size()) {
			return false;
		}

		// containsAll should be symmetric in this case
		return unionGroups.containsAll(other.unionGroups);
	}

	void adjustOrder(final Group other) {
		if (unionGroups.isEmpty()) {
			return;
		}

		final Map<Integer, UnionGroup> oldNumberMap = new Int2ObjectOpenHashMap<>(unionGroups.size());
		for (final UnionGroup unionGroup : unionGroups) {
			oldNumberMap.put(unionGroup.getUnionGroupNumber(), unionGroup);
		}

		final Map<UnionGroup, Integer> newNumberMap = new Object2IntOpenHashMap<>(unionGroups.size());
		for (final UnionGroup unionGroup : unionGroups) {
			final Optional<UnionGroup> otherUnionGroup = Iterables.tryFind(other.unionGroups, Predicates.equalTo(unionGroup));
			if (otherUnionGroup.isPresent()) {
				final int oldNumber = unionGroup.getUnionGroupNumber();
				final int newNumber = otherUnionGroup.get().getUnionGroupNumber();

				// If the current union group number is 0, it has a single relationship only, and should be kept that way
				if (oldNumber != 0 && oldNumber != newNumber) {
					newNumberMap.put(unionGroup, newNumber);
				}
			}
		}

		for (UnionGroup unionGroupToAdjust : newNumberMap.keySet()) {
			final int oldNumber = unionGroupToAdjust.getUnionGroupNumber();
			final int newNumber = newNumberMap.get(unionGroupToAdjust);

			final UnionGroup swap = oldNumberMap.get(newNumber);
			if (swap != null) {
				swap.setUnionGroupNumber(oldNumber);
				oldNumberMap.put(oldNumber, swap);
			} else {
				oldNumberMap.remove(oldNumber);
			}

			unionGroupToAdjust.setUnionGroupNumber(newNumber);
			oldNumberMap.put(newNumber, unionGroupToAdjust);
		}
	}

	void fillNumbers() {
		int unionGroupNumber = 1;

		for (final UnionGroup unionGroup : unionGroups) {
			if (unionGroup.getUnionGroupNumber() == RelationshipNormalFormGenerator.NUMBER_NOT_PRESERVED) {
				unionGroup.setUnionGroupNumber(unionGroupNumber++);
			}
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Group [unionGroups=");
		builder.append(unionGroups);
		builder.append("]");
		return builder.toString();
	}
}
