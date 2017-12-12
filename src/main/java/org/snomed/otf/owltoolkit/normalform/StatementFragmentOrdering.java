/*
 * Copyright 2017 SNOMED International, http://snomed.org
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
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

import com.google.common.collect.Ordering;
import com.google.common.primitives.Booleans;
import com.google.common.primitives.Longs;
import org.snomed.otf.owltoolkit.domain.Relationship;

/**
 * Compares {@link Relationship} instances for change processing.
 *
 */
public final class StatementFragmentOrdering extends Ordering<Relationship> {
	
	public static final Ordering<Relationship> INSTANCE = new StatementFragmentOrdering();
	
	private StatementFragmentOrdering() {
		// Prevents instantiation
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.google.common.collect.Ordering#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(final Relationship o1, final Relationship o2) {

		final int attributeDelta = Longs.compare(o1.getTypeId(), o2.getTypeId());
		if (attributeDelta != 0)
			return attributeDelta;

		final int valueDelta = Longs.compare(o1.getDestinationId(), o2.getDestinationId());
		if (valueDelta != 0)
			return valueDelta;

		final int groupDelta = o1.getGroup() - o2.getGroup();
		if (groupDelta != 0)
			return groupDelta;
		
		final int unionGroupDelta = o1.getUnionGroup() - o2.getUnionGroup();
		if (unionGroupDelta != 0)
			return unionGroupDelta;

		final int isUniversalDelta = Booleans.compare(o1.isUniversal(), o2.isUniversal());
		if (isUniversalDelta != 0)
			return isUniversalDelta;

		final int isDestinationNegatedDelta = Booleans.compare(o1.isDestinationNegated(), o2.isDestinationNegated());
		return isDestinationNegatedDelta;
	}
}