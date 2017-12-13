package org.snomed.otf.owltoolkit.conversion;

import org.semanticweb.owlapi.model.*;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.domain.ExpressionRepresentation;
import org.snomed.otf.owltoolkit.domain.Relationship;
import org.snomed.otf.owltoolkit.ontology.OntologyService;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomyLoader;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

import java.util.*;

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
			throw new IllegalArgumentException("Only SubClassOf and EquivalentClasses can be converted to relationships. " +
					"Axiom given is of type " + axiomType.getName());
		}

		ExpressionRepresentation representation = new ExpressionRepresentation();
		representation.setPrimitive(axiomType == AxiomType.EQUIVALENT_CLASSES);

		OWLSubClassOfAxiomImpl subClassOfAxiom = (OWLSubClassOfAxiomImpl) owlAxiom;
		OWLClassExpression rightHandExpression = subClassOfAxiom.getSuperClass();
		Long rightNamedClass = getNamedClass(axiomExpression, rightHandExpression, "right");
		representation.setRightHandSideNamedConcept(rightNamedClass);
		if (rightNamedClass == null) {
			representation.setRightHandSideRelationships(getRelationships(axiomExpression, rightHandExpression, "right"));
		}

		OWLClassExpression leftHandExpression = subClassOfAxiom.getSubClass();
		Long leftNamedClass = getNamedClass(axiomExpression, leftHandExpression, "left");
		representation.setLeftHandSideNamedConcept(leftNamedClass);
		if (leftNamedClass == null) {
			representation.setLeftHandSideRelationships(getRelationships(axiomExpression, leftHandExpression, "left"));
		}

		return representation;
	}

	private Long getNamedClass(String axiomExpression, OWLClassExpression owlClassExpression, String side) throws ConversionException {
		if (owlClassExpression.getClassExpressionType() != ClassExpressionType.OWL_CLASS) {
			return null;
		}
		Set<OWLClass> classesInSignature = owlClassExpression.getClassesInSignature();
		if (classesInSignature.size() > 1) {
			throw new ConversionException("Expected a maximum of 1 class in " + side + " hand side of axiom, got " + classesInSignature.size() + " - axiom '" + axiomExpression + "'.");
		}

		if (classesInSignature.size() == 1) {
			OWLClass namedClass = classesInSignature.iterator().next();
			return getConceptId(namedClass);
		}
		return null;
	}

	private Map<Integer, List<Relationship>> getRelationships(String axiomExpression, OWLClassExpression owlClassExpression, String side) throws ConversionException {
		if (owlClassExpression.getClassExpressionType() != ClassExpressionType.OBJECT_INTERSECTION_OF) {
			throw new ConversionException("Expected ObjectIntersectionOf at first level of " + side + " hand side of axiom, got " + owlClassExpression.getClassExpressionType() + " - axiom '" + axiomExpression + "'.");
		}

		Map<Integer, List<Relationship>> relationshipGroups = new HashMap<>();
		OWLObjectIntersectionOf intersectionOf = (OWLObjectIntersectionOf) owlClassExpression;
		List<OWLClassExpression> operands = intersectionOf.getOperandsAsList();
		int rollingGroupNumber = 0;
		for (OWLClassExpression operand : operands) {
			ClassExpressionType operandClassExpressionType = operand.getClassExpressionType();
			if (operandClassExpressionType == ClassExpressionType.OWL_CLASS) {
				relationshipGroups.computeIfAbsent(0, key -> new ArrayList<>()).add(new Relationship(0, Concepts.IS_A_LONG, getConceptId(operand.asOWLClass())));

			} else if (operandClassExpressionType == ClassExpressionType.OBJECT_SOME_VALUES_FROM) {
				OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) operand;
				OWLObjectPropertyExpression property = someValuesFrom.getProperty();
				int groupNumber = 0;
				Relationship relationship;
				if (isRoleGroup(property)) {
					groupNumber = ++rollingGroupNumber;
					// Extract Group
					OWLClassExpression filler = someValuesFrom.getFiller();
					if (filler.getClassExpressionType() != ClassExpressionType.OBJECT_SOME_VALUES_FROM) {
						throw new ConversionException("Expected ObjectSomeValuesFrom with role group to have a value of ObjectSomeValuesFrom, got " + filler.getClassExpressionType() + " - axiom '" + axiomExpression + "'.");
					}
					relationship = extractGroup((OWLObjectSomeValuesFrom) filler, groupNumber);
				} else {
					// Extract Group
					relationship = extractGroup(someValuesFrom, groupNumber);
				}
				relationshipGroups.computeIfAbsent(groupNumber, key -> new ArrayList<>()).add(relationship);

			} else {
				throw new ConversionException("Expected Class or ObjectSomeValuesFrom at second level of " + side + " hand side of axiom, got " + operandClassExpressionType + " - axiom '" + axiomExpression + "'.");
			}
		}

		return relationshipGroups;
	}

	private Relationship extractGroup(OWLObjectSomeValuesFrom someValuesFrom, int groupNumber) throws ConversionException {
		OWLObjectPropertyExpression property = someValuesFrom.getProperty();
		OWLObjectProperty namedProperty = property.getNamedProperty();
		long type = getConceptId(namedProperty);

		OWLClassExpression filler = someValuesFrom.getFiller();
		ClassExpressionType classExpressionType = filler.getClassExpressionType();
		if (classExpressionType != ClassExpressionType.OWL_CLASS) {
			throw new ConversionException("Expected right hand side of ObjectSomeValuesFrom to be type Class, got " + classExpressionType + ".");
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
