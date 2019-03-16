/*
 * Copyright 2017 SNOMED International, http://snomed.org
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
package org.snomed.otf.owltoolkit.normalform;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.TreeSet;

/**
 * Compares two collections of change subjects and calls template methods whenever a removed, added or unmodified
 * element is encountered.
 * 
 * @param <T> the change subject's type
 * 
 */
public abstract class OntologyChangeProcessor<T> {

	public void apply(final long conceptId, final Collection<T> oldCollection, final Collection<T> newCollection, final Ordering<T> ordering) {
		
		final TreeSet<T> uniqueOlds = Sets.newTreeSet(ordering);
		final ImmutableList<T> sortedOld = ordering.immutableSortedCopy(oldCollection);
		final ImmutableList<T> sortedNew = ordering.immutableSortedCopy(newCollection);

		for (final T oldSubject : sortedOld.reverse()) {
			final int idx = ordering.binarySearch(sortedNew, oldSubject);
			if (idx < 0 || !uniqueOlds.add(sortedNew.get(idx))) {
				handleRemovedSubject(conceptId, oldSubject);
			}
		}
		
		for (final T newMini : sortedNew) {
			if (ordering.binarySearch(sortedOld, newMini) < 0) {
				handleAddedSubject(conceptId, newMini);
			}
		}
	}
	
	public void apply(final Collection<OntologyChange<T>> changes) {
		
		if (changes == null || changes.isEmpty()) {
			return;
		}
		
		for (final OntologyChange<T> change : changes) {
			final long conceptId = change.getConceptId();
			switch (change.getNature()) {
				case ADD:
					handleAddedSubject(conceptId, change.getSubject());
					break;
				case REMOVE:
					handleRemovedSubject(conceptId, change.getSubject());
					break;
				default:
					throw new IllegalStateException(MessageFormat.format("Unexpected change nature {0}.", change.getNature()));
			}
		}
	}

	protected void handleRemovedSubject(final long conceptId, final T removedSubject) {
		// Subclasses should override		
	}

	protected void handleAddedSubject(final long conceptId, final T addedSubject) {
		// Subclasses should override
	}
}
