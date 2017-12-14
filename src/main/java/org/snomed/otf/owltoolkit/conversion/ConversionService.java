package org.snomed.otf.owltoolkit.conversion;

import org.semanticweb.owlapi.model.*;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.domain.ExpressionRepresentation;
import org.snomed.otf.owltoolkit.domain.Relationship;
import org.snomed.otf.owltoolkit.ontology.OntologyService;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomyLoader;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConversionService {

	public static final String SNOMED_ROLE_GROUP = "http://snomed.info/id/roleGroup";

	private SnomedTaxonomyLoader snomedTaxonomyLoader;

	public ConversionService() {
		snomedTaxonomyLoader = new SnomedTaxonomyLoader();
	}

	public ExpressionRepresentation convertAxiomToRelationships(String axiomExpression) throws ConversionException {
		OWLAxiom owlAxiom;
		try {
			owlAxiom = snomedTaxonomyLoader.deserialiseAxiom(axiomExpression);
		} catch (OWLOntologyCreationException e) {
			throw new ConversionException("Failed to deserialise axiom expression '" + axiomExpression + "'.");
		}

		AxiomType<?> axiomType = owlAxiom.getAxiomType();

		if (axiomType != AxiomType.SUBCLASS_OF && axiomType != AxiomType.EQUIVALENT_CLASSES) {
			throw new ConversionException("Only SubClassOf and EquivalentClasses can be converted to relationships. " +
					"Axiom given is of type " + axiomType.getName());
		}

		ExpressionRepresentation representation = new ExpressionRepresentation();
		OWLClassExpression leftHandExpression;
		OWLClassExpression rightHandExpression;
		if (axiomType == AxiomType.EQUIVALENT_CLASSES) {
			OWLEquivalentClassesAxiom equivalentClassesAxiom = (OWLEquivalentClassesAxiom) owlAxiom;
			Set<OWLClassExpression> classExpressions = equivalentClassesAxiom.getClassExpressions();
			if (classExpressions.size() != 2) {
				throw new ConversionException("Expecting EquivalentClasses expression to contain 2 class expressions, got " + classExpressions.size() + " - axiom '" + axiomExpression + "'.");
			}
			Iterator<OWLClassExpression> iterator = classExpressions.iterator();
			leftHandExpression = iterator.next();
			rightHandExpression = iterator.next();
		} else {
			representation.setPrimitive(true);

			OWLSubClassOfAxiom subClassOfAxiom = (OWLSubClassOfAxiom) owlAxiom;
			leftHandExpression = subClassOfAxiom.getSubClass();
			rightHandExpression = subClassOfAxiom.getSuperClass();
		}

		Long leftNamedClass = getNamedClass(axiomExpression, leftHandExpression, "left");
		representation.setLeftHandSideNamedConcept(leftNamedClass);
		if (leftNamedClass == null) {
			// If not a named class it must be an expression which can be converted to a set of relationships
			representation.setLeftHandSideRelationships(getRelationships(leftHandExpression, "left", axiomExpression));
		}

		Long rightNamedClass = getNamedClass(axiomExpression, rightHandExpression, "right");
		representation.setRightHandSideNamedConcept(rightNamedClass);
		if (rightNamedClass == null) {
			// If not a named class it must be an expression which can be converted to a set of relationships
			representation.setRightHandSideRelationships(getRelationships(rightHandExpression, "right", axiomExpression));
		}

		return representation;
	}

	private Long getNamedClass(String axiomExpression, OWLClassExpression owlClassExpression, String side) throws ConversionException {
		if (owlClassExpression.getClassExpressionType() != ClassExpressionType.OWL_CLASS) {
			return null;
		}
		Set<OWLClass> classesInSignature = owlClassExpression.getClassesInSignature();
		if (classesInSignature.size() > 1) {
			throw new ConversionException("Expecting a maximum of 1 class in " + side + " hand side of axiom, got " + classesInSignature.size() + " - axiom '" + axiomExpression + "'.");
		}

		if (classesInSignature.size() == 1) {
			OWLClass namedClass = classesInSignature.iterator().next();
			return getConceptId(namedClass);
		}
		return null;
	}

	private Map<Integer, List<Relationship>> getRelationships(OWLClassExpression owlClassExpression, String side, String wholeAxiomExpression) throws ConversionException {

		Map<Integer, List<Relationship>> relationshipGroups = new HashMap<>();

		List<OWLClassExpression> expressions;
		if (owlClassExpression.getClassExpressionType() == ClassExpressionType.OBJECT_INTERSECTION_OF) {
			OWLObjectIntersectionOf intersectionOf = (OWLObjectIntersectionOf) owlClassExpression;
			expressions = intersectionOf.getOperandsAsList();
		} else {
			throw new ConversionException("Expecting ObjectIntersectionOf at first level of " + side + " hand side of axiom, got " + owlClassExpression.getClassExpressionType() + " - axiom '" + wholeAxiomExpression + "'.");
		}

		int rollingGroupNumber = 0;
		for (OWLClassExpression operand : expressions) {
			ClassExpressionType operandClassExpressionType = operand.getClassExpressionType();
			if (operandClassExpressionType == ClassExpressionType.OWL_CLASS) {
				// Is-a relationship
				relationshipGroups.computeIfAbsent(0, key -> new ArrayList<>()).add(new Relationship(0, Concepts.IS_A_LONG, getConceptId(operand.asOWLClass())));

			} else if (operandClassExpressionType == ClassExpressionType.OBJECT_SOME_VALUES_FROM) {
				// Either start of attribute or role group
				OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) operand;
				OWLObjectPropertyExpression property = someValuesFrom.getProperty();
				if (isRoleGroup(property)) {
					rollingGroupNumber++;
					// Extract Group
					OWLClassExpression filler = someValuesFrom.getFiller();
					if (filler.getClassExpressionType() == ClassExpressionType.OBJECT_SOME_VALUES_FROM) {
						Relationship relationship = extractRelationship((OWLObjectSomeValuesFrom) filler, rollingGroupNumber);
						relationshipGroups.computeIfAbsent(rollingGroupNumber, key -> new ArrayList<>()).add(relationship);
					} else if (filler.getClassExpressionType() == ClassExpressionType.OBJECT_INTERSECTION_OF) {
						OWLObjectIntersectionOf listOfAttributes = (OWLObjectIntersectionOf) filler;
						for (OWLClassExpression classExpression : listOfAttributes.getOperandsAsList()) {
							if (classExpression.getClassExpressionType() == ClassExpressionType.OBJECT_SOME_VALUES_FROM) {
								Relationship relationship = extractRelationship((OWLObjectSomeValuesFrom) classExpression, rollingGroupNumber);
								relationshipGroups.computeIfAbsent(rollingGroupNumber, key -> new ArrayList<>()).add(relationship);
							} else {
								throw new ConversionException("Expecting ObjectSomeValuesFrom within ObjectIntersectionOf as part of role group, got " + classExpression.getClassExpressionType() + " - axiom '" + wholeAxiomExpression + "'.");
							}
						}
					} else {
						throw new ConversionException("Expecting ObjectSomeValuesFrom with role group to have a value of ObjectSomeValuesFrom, got " + filler.getClassExpressionType() + " - axiom '" + wholeAxiomExpression + "'.");
					}
				} else {
					Relationship relationship = extractRelationship(someValuesFrom, 0);
					relationshipGroups.computeIfAbsent(0, key -> new ArrayList<>()).add(relationship);
				}

			} else {
				throw new ConversionException("Expecting Class or ObjectSomeValuesFrom at second level of " + side + " hand side of axiom, got " + operandClassExpressionType + " - axiom '" + wholeAxiomExpression + "'.");
			}
		}

		return relationshipGroups;
	}

	private Relationship extractRelationship(OWLObjectSomeValuesFrom someValuesFrom, int groupNumber) throws ConversionException {
		OWLObjectPropertyExpression property = someValuesFrom.getProperty();
		OWLObjectProperty namedProperty = property.getNamedProperty();
		long type = getConceptId(namedProperty);

		OWLClassExpression filler = someValuesFrom.getFiller();
		ClassExpressionType classExpressionType = filler.getClassExpressionType();
		if (classExpressionType != ClassExpressionType.OWL_CLASS) {
			throw new ConversionException("Expecting right hand side of ObjectSomeValuesFrom to be type Class, got " + classExpressionType + ".");
		}
		long value = getConceptId(filler.asOWLClass());

		return new Relationship(groupNumber, type, value);
	}

	public boolean isConceptClass(final OWLClass owlClass) {
		return owlClass.getIRI().toString().startsWith(OntologyService.SNOMED_IRI);
	}

	private boolean isRoleGroup(OWLObjectPropertyExpression expression) {
		OWLObjectProperty namedProperty = expression.getNamedProperty();
		return SNOMED_ROLE_GROUP.equals(namedProperty.getIRI().toString());
	}

	public long getConceptId(final OWLNamedObject owlNamedObject) {
		return Long.parseLong(owlNamedObject.getIRI().toString().substring(OntologyService.SNOMED_IRI.length()));
	}

}
