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
package org.snomed.otf.owltoolkit.domain;

public class Relationship {

	private final long relationshipId;
	private int effectiveTime;
	private final long moduleId;
	private final long typeId;
	private final long destinationId;
	private int group;
	private final int unionGroup;
	private final boolean universal;
	private final boolean destinationNegated;
	private final long characteristicTypeId;

	public Relationship(final long typeId, final long destinationId) {
		this(-1, -1, -1, typeId, destinationId, false, 0, 0, false, -1);
	}

	public Relationship(final int group, final long typeId, final long destinationId) {
		this(-1, -1, -1, typeId, destinationId, false, group, 0, false, -1);
	}

	public Relationship(long relationshipId,
						int effectiveTime,
						long moduleId,
						long typeId,
						long destinationId,
						boolean destinationNegated,
						int group,
						int unionGroup,
						boolean universal,
						long characteristicTypeId) {
		this.relationshipId = relationshipId;
		this.effectiveTime = effectiveTime;
		this.moduleId = moduleId;
		this.typeId = typeId;
		this.destinationId = destinationId;
		this.destinationNegated = destinationNegated;
		this.group = group;
		this.unionGroup = unionGroup;
		this.universal = universal;
		this.characteristicTypeId = characteristicTypeId;
	}

	public long getRelationshipId() {
		return relationshipId;
	}

	public long getTypeId() {
		return typeId;
	}

	public long getDestinationId() {
		return destinationId;
	}

	public int getGroup() {
		return group;
	}

	public int getUnionGroup() {
		return unionGroup;
	}

	public boolean isUniversal() {
		return universal;
	}

	public boolean isDestinationNegated() {
		return destinationNegated;
	}

	public long getCharacteristicTypeId() {
		return characteristicTypeId;
	}

	public long getModuleId() {
		return moduleId;
	}

	public int getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(int effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	public void setGroup(int group) {
		this.group = group;
	}

	@Override
	public String toString() {
		return "Relationship{" +
				"typeId=" + typeId +
				", destinationId=" + destinationId +
				'}';
	}
}
