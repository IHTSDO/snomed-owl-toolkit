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

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.snomed.otf.owltoolkit.normalform.RelationshipNormalFormGenerator;

import java.util.*;

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

	public void adjustOrder(final GroupSet previousInferredSet) {
		if (isEmpty()) {
			return;
		}

		final Map<Integer, Group> oldNumberMap = new Int2ObjectOpenHashMap<>(groups.size());
		for (final Group statedGroup : groups) {
			oldNumberMap.put(statedGroup.getGroupNumber(), statedGroup);
		}

		final Map<Group, Integer> newNumberMap = new Object2IntOpenHashMap<>(groups.size());
		for (final Group statedGroup : groups) {
			final Optional<Group> previousInferredGroup = previousInferredSet.groups.stream().filter(group -> group.equals(statedGroup)).findAny();
			if (previousInferredGroup.isPresent()) {
				final int statedNumber = statedGroup.getGroupNumber();
				final int previouslyInferredNumber = previousInferredGroup.get().getGroupNumber();

				if (statedNumber != previouslyInferredNumber) {
					// Allow groups to move out of group 0 if they are no longer stated that way
					if (previouslyInferredNumber != 0) {
						newNumberMap.put(statedGroup, previouslyInferredNumber);
						statedGroup.adjustOrder(previousInferredGroup.get());
					}
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

	public void sort(Comparator<Group> groupComparator) {
		groups.sort(groupComparator);
	}
}
