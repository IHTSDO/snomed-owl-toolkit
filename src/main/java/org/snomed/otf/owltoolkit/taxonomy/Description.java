package org.snomed.otf.owltoolkit.taxonomy;

import java.util.Objects;

public class Description {

	private final long id;
	private final String term;
	private final long conceptId;
	private boolean preferred;

	public Description(long id, long conceptId, String term) {
		this.id = id;
		this.conceptId = conceptId;
		this.term = term;
	}

	public void markPreferred() {
		preferred = true;
	}

	public long getId() {
		return id;
	}

	public String getTerm() {
		return term;
	}

	public long getConceptId() {
		return conceptId;
	}

	public boolean isPreferred() {
		return preferred;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Description that = (Description) o;
		return id == that.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
