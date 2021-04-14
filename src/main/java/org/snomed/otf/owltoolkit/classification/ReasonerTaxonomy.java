/*
 * Copyright 2018 SNOMED International, http://snomed.org
 * Copyright 2011-2016 B2i Healthcare Pte Ltd, http://b2i.sg
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
package org.snomed.otf.owltoolkit.classification;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.io.Serializable;
import java.util.*;

public class ReasonerTaxonomy implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final List<Set<Long>> equivalentConceptIds = new ArrayList<>();
	private final Set<Long> unsatisfiableConceptIds = new LongOpenHashSet();
	private final Map<Long, Set<Long>> parentIds = new Long2ObjectOpenHashMap<>();
	private final Map<Long, Set<Long>> ancestorIds = new Long2ObjectOpenHashMap<>();
	private final List<Long> insertionOrderedIds = new LongArrayList();
	private final List<Long> insertionOrderedAttributeIds = new LongArrayList();

	public ReasonerTaxonomy() {
	}
	
	public void addEquivalentConceptIds(final Set<Long> conceptIds) {
		equivalentConceptIds.add(new LongOpenHashSet(conceptIds));
	}
	
	public List<Set<Long>> getEquivalentConceptIds() {
		return equivalentConceptIds;
	}

	public void addUnsatisfiableConceptIds(final Set<Long> conceptIds) {
		unsatisfiableConceptIds.addAll(conceptIds);
	}
	
	public Set<Long> getUnsatisfiableConceptIds() {
		return unsatisfiableConceptIds;
	}

	public void addEntry(final ReasonerTaxonomyEntry entry) {
		insertionOrderedIds.add(entry.getSourceId());
		getOrCreateSet(parentIds, entry.getSourceId()).addAll(entry.getParentIds());
		getOrCreateSet(ancestorIds, entry.getSourceId()).addAll(entry.getParentIds());
		for (Long parentId : entry.getParentIds()) {
			getOrCreateSet(ancestorIds, entry.getSourceId()).addAll(getOrCreateSet(ancestorIds, parentId));
		}
	}

	private Set<Long> getOrCreateSet(final Map<Long, Set<Long>> map, final long key) {
		if (map.containsKey(key)) {
			return map.get(key);
		} else {
			final Set<Long> newSet = new LongOpenHashSet();
			map.put(key, newSet);
			return newSet;
		}
	}

	private Set<Long> getOrReturnEmptySet(final Map<Long, Set<Long>> map, final long key) {
		if (map.containsKey(key)) {
			return map.get(key);
		} else {
			return Collections.emptySet();
		}
	}
	
	public Set<Long> getParents(final long sourceId) {
		return getOrReturnEmptySet(parentIds, sourceId);
	}
	
	public Set<Long> getAncestors(final long sourceId) {
		return getOrReturnEmptySet(ancestorIds, sourceId);
	}
	
	public List<Long> getConceptIds() {
		return insertionOrderedIds;
	}

	public List<Long> getAttributeIds() {
		return insertionOrderedAttributeIds;
	}

}
