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
package org.snomed.otf.owltoolkit.classification;

import java.util.Set;

public class ReasonerTaxonomyEntry {

	private final long sourceId;
	private final Set<Long> parentIds;

	public ReasonerTaxonomyEntry(long sourceId, Set<Long> parents) {
		this.sourceId = sourceId;
		this.parentIds = parents;
	}

	public long getSourceId() {
		return sourceId;
	}

	public Set<Long> getParentIds() {
		return parentIds;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ReasonerTaxonomyEntry that = (ReasonerTaxonomyEntry) o;

		if (sourceId != that.sourceId) return false;
		return parentIds != null ? parentIds.equals(that.parentIds) : that.parentIds == null;
	}

	@Override
	public int hashCode() {
		int result = (int) (sourceId ^ (sourceId >>> 32));
		result = 31 * result + (parentIds != null ? parentIds.hashCode() : 0);
		return result;
	}
}
