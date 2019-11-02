package org.snomed.otf.owltoolkit.taxonomy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Description {

	private final String id;
	private String term;
	private String typeId;
	private String languageCode;
	private final Map<Long, Long> acceptabilityMap;

	public Description(String id) {
		this.id = id;
		acceptabilityMap = new HashMap<>();
	}

	public Description(String id, String term, String typeId, String languageCode) {
		this(id);
		this.term = term;
		this.typeId = typeId;
		this.languageCode = languageCode;
	}

	public String getTerm() {
		return term;
	}

	public String getTypeId() {
		return typeId;
	}

	public String getLanguageCode() {
		return languageCode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Description that = (Description) o;
		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	void setAcceptability(long refsetId, long acceptabilityId, boolean active) {
		if (active) {
			acceptabilityMap.put(refsetId, acceptabilityId);
		} else {
			acceptabilityMap.remove(refsetId);
		}
	}

	public Map<Long, Long> getAcceptabilityMap() {
		return acceptabilityMap;
	}
}
