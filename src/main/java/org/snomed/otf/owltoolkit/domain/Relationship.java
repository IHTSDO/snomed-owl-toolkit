/*
 * Copyright 2020 SNOMED International, http://snomed.org
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

import java.util.Objects;

public class Relationship {
	private long relationshipId;
	private int effectiveTime;
	private final long moduleId;
	private final long typeId;
	private final long destinationId;
	private int group;
	private final int unionGroup;
	private final boolean universal;
	private final long characteristicTypeId;
	private ConcreteValue value;

	public Relationship(final int group, final long typeId, ConcreteValue value) {
		this(-1, -1, -1, typeId, -1, group, 0, false, -1);
		this.value = value;
	}


	public Relationship(final long typeId, final long destinationId) {
		this(-1, -1, -1, typeId, destinationId, 0, 0, false, -1);
	}

	public Relationship(final int group, final long typeId, final long destinationId) {
		this(-1, -1, -1, typeId, destinationId, group, 0, false, -1);
	}

	public Relationship(long relationshipId,
			int effectiveTime,
			long moduleId,
			long typeId,
			long destinationId,
			int group,
			int unionGroup,
			boolean universal,
			long characteristicTypeId) {
		this.relationshipId = relationshipId;
		this.effectiveTime = effectiveTime;
		this.moduleId = moduleId;
		this.typeId = typeId;
		this.destinationId = destinationId;
		this.group = group;
		this.unionGroup = unionGroup;
		this.universal = universal;
		this.characteristicTypeId = characteristicTypeId;
		this.value = null;
	}

	public Relationship(long relationshipId,
			int effectiveTime,
			long moduleId,
			long typeId,
			ConcreteValue value,
			int group,
			int unionGroup,
			boolean universal,
			long characteristicTypeId) {
		this.relationshipId = relationshipId;
		this.effectiveTime = effectiveTime;
		this.moduleId = moduleId;
		this.typeId = typeId;
		this.value = value;
		this.group = group;
		this.unionGroup = unionGroup;
		this.universal = universal;
		this.characteristicTypeId = characteristicTypeId;
		this.destinationId = -1;
	}

	public void clearId() {
		relationshipId = -1;
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

	public void setRelationshipId(long relationshipId) {
		this.relationshipId = relationshipId;
	}

	public ConcreteValue getValue() {
		return value;
	}

	public String getValueAsString() {
		return value != null ? value.getRF2Value() : null;
	}

	public boolean isConcrete() {
		return value != null;
	}

	public static final class ConcreteValue {

		public enum Type {
			INTEGER("int"),
			DECIMAL("dec"),
			STRING("str");

			private final String shorthand;

			Type(String shorthand) {
				this.shorthand = shorthand;
			}

			public String getShorthand() {
				return this.shorthand;
			}
		}

		private final Type type;
		private final String value;

		public ConcreteValue(String value) {
			// Guess type, shouldn't matter if this is wrong for NNF process
			if (value.startsWith("#")) {
				value = value.substring(1);
				this.type = value.contains(".") ? Type.DECIMAL : Type.INTEGER;
			} else {
				if (value.startsWith("\"")) {
					value = value.substring(1);
				}
				if (value.endsWith("\"")) {
					value = value.substring(0, value.length() -1);
				}
				type = Type.STRING;
			}
			this.value = value;
		}

		public ConcreteValue(Type type, String value) {
			this.type = type;
			this.value = value;
		}

		public String getRF2Value() {
			if (value != null) {
				switch (type) {
					case STRING:
						return String.format("\"%s\"", value);
					case INTEGER:
						return "#" + asInt();
					case DECIMAL:
						return "#" + asDecimal();
					default:
						break;
				}
			}
			return null;
		}

		public Type getType() {
			return type;
		}

		public boolean isInteger() { return this.type == Type.INTEGER; }

		public int asInt() { return Integer.parseInt(value); }
		
		public boolean isString() { return Type.STRING == type; }
		
		public String asString() { return value; }

		public boolean isDecimal() { return Type.DECIMAL == type; }
		
		public Double asDecimal() { return Double.parseDouble(value); }

		@Override
		public String toString() {
			return "ConcreteValue{" + "type=" + type + ", value='" + value + '\'' + '}';
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ConcreteValue value1 = (ConcreteValue) o;
			return type == value1.type && Objects.equals(value, value1.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, value);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Relationship that = (Relationship) o;
		return relationshipId == that.relationshipId && effectiveTime == that.effectiveTime
				&& moduleId == that.moduleId && typeId == that.typeId
				&& destinationId == that.destinationId && group == that.group
				&& unionGroup == that.unionGroup && universal == that.universal
				&& characteristicTypeId == that.characteristicTypeId
				&& Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(relationshipId, effectiveTime, moduleId, typeId, destinationId,
				group, unionGroup, universal, characteristicTypeId, value);
	}

	@Override
	public String toString() {
		return "Relationship{" + "relationshipId=" + relationshipId
				+ ", typeId=" + typeId + ", destinationId="
				+ destinationId + ", group=" + group + ", value=" + value + '}';
	}
}
