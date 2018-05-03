package org.snomed.otf.owltoolkit.ontology;

/**
 * Representation for an OWL property chain.
 *
 * In example property chain: "has_active_ingredient o is_modification_of -> has_active_ingredient"
 * The naming within this class is: "sourceType o destinationType -> inferredType"
 */
public class PropertyChain {

	private Long sourceType;
	private Long destinationType;
	private Long inferredType;

	public PropertyChain(long sourceType, long destinationType, long inferredType) {
		this.sourceType = sourceType;
		this.destinationType = destinationType;
		this.inferredType = inferredType;
	}

	public Long getSourceType() {
		return sourceType;
	}

	public Long getDestinationType() {
		return destinationType;
	}

	public Long getInferredType() {
		return inferredType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PropertyChain that = (PropertyChain) o;

		if (!sourceType.equals(that.sourceType)) return false;
		if (!destinationType.equals(that.destinationType)) return false;
		return inferredType.equals(that.inferredType);
	}

	@Override
	public int hashCode() {
		int result = sourceType.hashCode();
		result = 31 * result + destinationType.hashCode();
		result = 31 * result + inferredType.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "PropertyChain{" +
				"sourceType=" + sourceType +
				", destinationType=" + destinationType +
				", inferredType=" + inferredType +
				'}';
	}
}
