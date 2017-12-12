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

import com.google.common.collect.ImmutableList;
import org.snomed.otf.owltoolkit.normalform.RelationshipNormalFormGenerator;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public final class UnionGroup implements SemanticComparable<UnionGroup> {

	private final List<RelationshipFragment> fragments;

	private int unionGroupNumber = RelationshipNormalFormGenerator.NUMBER_NOT_PRESERVED;

	/**
	 * Creates a new union group instance with the specified parameters,
	 * preserving the union group number for later reference.
	 *
	 * @param fragments
	 *            the relationship fragments to associate with this union
	 *            group (may not be <code>null</code>)
	 */
	public UnionGroup(final Iterable<RelationshipFragment> fragments) {
		checkArgument(fragments != null, "fragments is null.");
		this.fragments = ImmutableList.copyOf(fragments);
	}

	public List<RelationshipFragment> getRelationshipFragments() {
		return fragments;
	}

	public int getUnionGroupNumber() {
		return unionGroupNumber;
	}

	public void setUnionGroupNumber(final int unionGroupNumber) {
		checkArgument(unionGroupNumber > RelationshipNormalFormGenerator.NUMBER_NOT_PRESERVED, "Illegal union group number '%s'.", unionGroupNumber);
		this.unionGroupNumber = unionGroupNumber;
	}

	@Override
	public boolean isSameOrStrongerThan(final UnionGroup other) {

		/*
		 * Things same or stronger than A OR B OR C:
		 *
		 * - A' OR B OR C, where A' is a subclass of A
		 * - B
		 *
		 * So we'll have to check for all of our fragments to see if a less
		 * expressive fragment exists in the "other" union group. Points are
		 * awarded if we manage to get away with less fragments than the
		 * "other" union group.
		 */
		for (final RelationshipFragment ourFragment : fragments) {

			boolean found = false;

			for (final RelationshipFragment otherFragment : other.fragments) {

				if (ourFragment.isSameOrStrongerThan(otherFragment)) {
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
		return 31 + fragments.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof UnionGroup)) {
			return false;
		}

		final UnionGroup other = (UnionGroup) obj;

		if (fragments.size() != other.fragments.size()) {
			return false;
		}

		// containsAll should be symmetric in this case
		return fragments.containsAll(other.fragments);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("UnionGroup [fragments=");
		builder.append(fragments);
		builder.append("]");
		return builder.toString();
	}
}
