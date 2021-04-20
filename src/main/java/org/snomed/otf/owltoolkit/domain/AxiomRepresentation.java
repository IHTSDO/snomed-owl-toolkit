package org.snomed.otf.owltoolkit.domain;

import java.util.*;

public class AxiomRepresentation {

	private boolean primitive;
	private Long leftHandSideNamedConcept;
	private Map<Integer, List<Relationship>> leftHandSideRelationships;
	private Long rightHandSideNamedConcept;
	private Map<Integer, List<Relationship>> rightHandSideRelationships;

	public AxiomRepresentation() {
	}

	public boolean isPrimitive() {
		return primitive;
	}

	public void setPrimitive(boolean primitive) {
		this.primitive = primitive;
	}

	public Long getLeftHandSideNamedConcept() {
		return leftHandSideNamedConcept;
	}

	public void setLeftHandSideNamedConcept(Long leftHandSideNamedConcept) {
		this.leftHandSideNamedConcept = leftHandSideNamedConcept;
	}

	public Map<Integer, List<Relationship>> getLeftHandSideRelationships() {
		return leftHandSideRelationships;
	}

	public void setLeftHandSideRelationships(Map<Integer, List<Relationship>> leftHandSideRelationships) {
		this.leftHandSideRelationships = leftHandSideRelationships;
	}

	public Long getRightHandSideNamedConcept() {
		return rightHandSideNamedConcept;
	}

	public void setRightHandSideNamedConcept(Long rightHandSideNamedConcept) {
		this.rightHandSideNamedConcept = rightHandSideNamedConcept;
	}

	public Map<Integer, List<Relationship>> getRightHandSideRelationships() {
		return rightHandSideRelationships;
	}

	public void setRightHandSideRelationships(Map<Integer, List<Relationship>> rightHandSideRelationships) {
		this.rightHandSideRelationships = rightHandSideRelationships;
	}

	public void addRightHandSideRelationship(int group, Relationship relationship) {
		if (rightHandSideRelationships == null) {
			rightHandSideRelationships = new HashMap<>();
		}
		rightHandSideRelationships.computeIfAbsent(group, (i) -> new ArrayList<>()).add(relationship);
	}
}
