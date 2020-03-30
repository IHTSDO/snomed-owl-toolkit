package org.snomed.otf.owltoolkit.domain;

public class ObjectPropertyAxiomRepresentation {
	boolean isTransitive;
	boolean isReflexive;
	boolean isPropertyChain;
	String owlExpression;
	
	public ObjectPropertyAxiomRepresentation(String axiomExpression) {
		owlExpression = axiomExpression;
	}
	public boolean isTransitive() {
		return isTransitive;
	}
	public void setTransitive(boolean isTransitive) {
		this.isTransitive = isTransitive;
	}
	public boolean isReflexive() {
		return isReflexive;
	}
	public void setReflexive(boolean isReflexive) {
		this.isReflexive = isReflexive;
	}
	public boolean isPropertyChain() {
		return isPropertyChain;
	}
	public void setPropertyChain(boolean isPropertyChain) {
		this.isPropertyChain = isPropertyChain;
	}
	public String getOwl() {
		return owlExpression;
	}
	public void setOwl(String owl) {
		this.owlExpression = owl;
	}
	public void mergeProperties(ObjectPropertyAxiomRepresentation rep) {
		isTransitive = isTransitive || rep.isTransitive;
		isReflexive = isReflexive || rep.isReflexive;
		isPropertyChain = isPropertyChain || rep.isPropertyChain;
		owlExpression = owlExpression + "\n" + rep.owlExpression;
	}
	
}
