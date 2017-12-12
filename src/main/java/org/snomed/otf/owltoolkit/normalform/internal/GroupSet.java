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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.snomed.otf.owltoolkit.normalform.RelationshipNormalFormGenerator;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Represents a set of groups that do not allow redundant elements.
 */
public final class GroupSet extends AbstractSet<Group> {

	private final List<Group> groups = Lists.newArrayList();

	/**
	 * Adds the specified group to this set if it is not already present.
	 * More formally, adds the specified group e to this set if the set
	 * contains no group e2 such that e2.isSameOrStrongerThan(e). If this
	 * set already contains such group, the call leaves the set unchanged and
	 * returns <code>false</code>. If no group contains the specified group,
	 * the call removes all groups ei from the set where
	 * e.isSameOrStrongerThan(ei) applies, adds the new element, and returns
	 * <code>true</code>.
	 */
	@Override
	public boolean add(final Group e) {
		final List<Group> redundant = Lists.newArrayList();

		for (final Group existingGroup : groups) {
			if (existingGroup.isSameOrStrongerThan(e)) {
				return false;
			} else if (e.isSameOrStrongerThan(existingGroup)) {
				redundant.add(existingGroup);
			}
		}

		groups.removeAll(redundant);
		groups.add(e);

		return true;
	}

	/**
	 * Adds a group to the set, bypassing redundancy checks.
	 *
	 * @see #add(Group)
	 */
	public boolean addUnique(final Group e) {
		return groups.add(e);
	}

	@Override
	public Iterator<Group> iterator() {
		return groups.iterator();
	}

	@Override
	public int size() {
		return groups.size();
	}

	public void adjustOrder(final GroupSet other) {
		if (isEmpty()) {
			return;
		}

		final Map<Integer, Group> oldNumberMap = new Int2ObjectOpenHashMap<>(groups.size());
		for (final Group group : groups) {
			oldNumberMap.put(group.getGroupNumber(), group);
		}

		final Map<Group, Integer> newNumberMap = new Object2IntOpenHashMap<>(groups.size());
		for (final Group group : groups) {
			final Optional<Group> otherGroup = Iterables.tryFind(other.groups, Predicates.equalTo(group));
			if (otherGroup.isPresent()) {
				final int oldNumber = group.getGroupNumber();
				final int newNumber = otherGroup.get().getGroupNumber();

				// If the current group number is 0, it has a single relationship only, and should be kept that way
				if (oldNumber != 0 && oldNumber != newNumber) {
					newNumberMap.put(group, newNumber);
					group.adjustOrder(otherGroup.get());
				}
			}
		}

		for (Group groupToAdjust : newNumberMap.keySet()) {
			final int oldNumber = groupToAdjust.getGroupNumber();
			final int newNumber = newNumberMap.get(groupToAdjust);

			final Group swap = oldNumberMap.get(newNumber);
			if (swap != null) {
				swap.setGroupNumber(oldNumber);
				oldNumberMap.put(oldNumber, swap);
			} else {
				oldNumberMap.remove(oldNumber);
			}

			groupToAdjust.setGroupNumber(newNumber);
			oldNumberMap.put(newNumber, groupToAdjust);
		}
	}

	public void fillNumbers() {
		int groupNumber = 1;

		for (final Group group : groups) {
			group.fillNumbers();

			/*
			 * Group numbers will already be set on existing inferred relationship groups and 0 groups.
			 */
			if (group.getGroupNumber() == RelationshipNormalFormGenerator.NUMBER_NOT_PRESERVED) {
				group.setGroupNumber(groupNumber++);
			}
		}
	}
}
