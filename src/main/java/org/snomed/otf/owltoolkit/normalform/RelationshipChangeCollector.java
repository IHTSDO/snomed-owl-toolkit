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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RelationshipChangeCollector extends OntologyChangeProcessor<Relationship> {

	private final Map<Long, Set<Relationship>> addedStatements;
	private final Map<Long, Set<Relationship>> removedStatements;
	private Long addedCount;
	private Long removedCount;

	public RelationshipChangeCollector() {
		addedCount = 0L;
		removedCount = 0L;
		addedStatements = new Long2ObjectOpenHashMap<>();
		removedStatements = new Long2ObjectOpenHashMap<>();
	}

	@Override
	protected void handleAddedSubject(long conceptId, Relationship addedSubject) {
		addedStatements.computeIfAbsent(conceptId, k -> new HashSet<>()).add(addedSubject);
		addedCount++;
	}

	@Override
	protected void handleRemovedSubject(long conceptId, Relationship removedSubject) {
		//We will preserve any "Additional" characteristic types eg PartOf relationships
		if (removedSubject.getCharacteristicTypeId() == -1 || removedSubject.getCharacteristicTypeId() != Concepts.ADDITIONAL_RELATIONSHIP_LONG) {
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
