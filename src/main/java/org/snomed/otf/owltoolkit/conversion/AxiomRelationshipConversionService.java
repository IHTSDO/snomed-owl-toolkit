package org.snomed.otf.owltoolkit.conversion;

import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.domain.AxiomRepresentation;
import org.snomed.otf.owltoolkit.domain.Relationship;
import org.snomed.otf.owltoolkit.ontology.OntologyHelper;
import org.snomed.otf.owltoolkit.ontology.OntologyService;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomyLoader;

import java.util.*;
import java.util.stream.Collectors;

import static org.snomed.otf.owltoolkit.ontology.OntologyService.CORE_COMPONENT_NAMESPACE_PATTERN;

import static org.snomed.otf.owltoolkit.ontology.OntologyService.SNOMED_ROLE_GROUP_FULL_URI;

public class AxiomRelationshipConversionService {

	private final SnomedTaxonomyLoader snomedTaxonomyLoader;
	private final OntologyService ontologyService;

	private static final Logger LOGGER = LoggerFactory.getLogger(AxiomRelationshipConversionService.class);
	private Collection<Long> objectAttributes;
	private Collection<Long> dataAttributes;

	public AxiomRelationshipConversionService(Set<Long> ungroupedAttributes) {
		snomedTaxonomyLoader = new SnomedTaxonomyLoader();
		ontologyService = new OntologyService(ungroupedAttributes);
	}

	/**
	 * Use this constructor to enable generating SubObjectPropertyOf and SubDataPropertyOf axioms from relationships.
	 * @param ungroupedAttributes
	 * @param objectAttributes
	 * @param dataAttributes
	 */
	public AxiomRelationshipConversionService(Set<Long> ungroupedAttributes, Collection<Long> objectAttributes, Collection<Long> dataAttributes) {
		snomedTaxonomyLoader = new SnomedTaxonomyLoader();
		ontologyService = new OntologyService(ungroupedAttributes);
		this.objectAttributes = objectAttributes;
		this.dataAttributes = dataAttributes;
	}

	/**
	 * Converts an OWL Axiom expression String to an AxiomRepresentation containing a concept id or set of relationships for each side of the expression.
	 * Currently supported axiom types are SubClassOf and EquivalentClasses.
	 *
	 * @param axiomExpression The Axiom expression to convert.
	 * @return AxiomRepresentation with the details of the expression or null if the axiom type is not supported.
	 * @throws ConversionException if the Axiom expression is malformed or of an unexpected structure.
	 */
	public AxiomRepresentation convertAxiomToRelationships(String axiomExpression) throws ConversionException {
		return convertAxiomToRelationships(null, axiomExpression);
	}

	/**
	 * Converts an OWL Axiom expression String to an AxiomRepresentation containing a concept id or set of relationships for each side of the expression.
	 * Currently supported axiom types are SubClassOf and EquivalentClasses.
	 *
	 * @param referencedComponentId Specifying a referencedComponentId will force the other side of the axiom to be returned as relationships even if only a single named concept is on that side.
	 * @param axiomExpression The Axiom expression to convert.
	 * @return AxiomRepresentation with the details of the expression or null if the axiom type is not supported.
	 * @throws ConversionException if the Axiom expression is malformed or of an unexpected structure.
	 */
	public AxiomRepresentation convertAxiomToRelationships(Long referencedComponentId, String axiomExpression) throws ConversionException {
		OWLAxiom owlAxiom = convertOwlExpressionToOWLAxiom(axiomExpression);
		return convertAxiomToRelationships(referencedComponentId, owlAxiom);
	}

	/**
	 * Converts an OWL Axiom expression String to an AxiomRepresentation containing a concept id or set of relationships for each side of the expression.
	 * Currently supported axiom types are SubClassOf and EquivalentClasses.
	 *
	 * @param referencedComponentId Specifying a referencedComponentId will force the other side of the axiom to be returned as relationships even if only a single named concept is on that side.
	 * @param owlAxiom The Axiom expression to convert.
	 * @return AxiomRepresentation with the details of the expression or null if the axiom type is not supported.
	 * @throws ConversionException if the Axiom expression is malformed or of an unexpected structure.
	 */
	public AxiomRepresentation convertAxiomToRelationships(Long referencedComponentId, OWLAxiom owlAxiom) throws ConversionException {
		AxiomType<?> axiomType = owlAxiom.getAxiomType();

		if (Concepts.ROOT_LONG.equals(referencedComponentId) && axiomType == AxiomType.SUBCLASS_OF) {
			LOGGER.debug("Skipping axiom of root concept which we assume points to 'Thing'.");
			return null;
		}

		if (axiomType != AxiomType.SUBCLASS_OF && axiomType != AxiomType.EQUIVALENT_CLASSES && axiomType != AxiomType.SUB_OBJECT_PROPERTY) {
			LOGGER.debug("Only SubClassOf, EquivalentClasses and SubObjectPropertyOf can be converted to relationships. " +
					"Axiom given is of type " + axiomType.getName() + ". Returning null.");
			return null;
		}

		AxiomRepresentation representation = new AxiomRepresentation();
		OWLClassExpression leftHandExpression;
		OWLClassExpression rightHandExpression;

		if (axiomType == AxiomType.SUB_OBJECT_PROPERTY) {
			OWLSubObjectPropertyOfAxiom subObjectPropertyOfAxiom = (OWLSubObjectPropertyOfAxiom) owlAxiom;

			OWLObjectPropertyExpression subProperty = subObjectPropertyOfAxiom.getSubProperty();
			OWLObjectProperty namedProperty = subProperty.getNamedProperty();
			long subAttributeConceptId = OntologyHelper.getConceptId(namedProperty);

			OWLObjectPropertyExpression superProperty = subObjectPropertyOfAxiom.getSuperProperty();
			OWLObjectProperty superPropertyNamedProperty = superProperty.getNamedProperty();
			long superAttributeConceptId = OntologyHelper.getConceptId(superPropertyNamedProperty);

			representation.setLeftHandSideNamedConcept(subAttributeConceptId);
			representation.setRightHandSideRelationships(newSingleIsARelationship(superAttributeConceptId));

			return representation;

		} else if (axiomType == AxiomType.EQUIVALENT_CLASSES) {
			OWLEquivalentClassesAxiom equivalentClassesAxiom = (OWLEquivalentClassesAxiom) owlAxiom;
			Set<OWLClassExpression> classExpressions = equivalentClassesAxiom.getClassExpressions();
			if (classExpressions.size() != 2) {
				throw new ConversionException("Expecting EquivalentClasses expression to contain 2 class expressions, got " + classExpressions.size() + " - axiom '" + owlAxiom.toString() + "'.");
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

		Long leftNamedClass = getNamedClass(owlAxiom, leftHandExpression, "left");
		if (leftNamedClass != null) {
			if (referencedComponentId != null && !referencedComponentId.equals(leftNamedClass)) {
				// Force the named concept which is not the referencedComponentId to be returned as a set of relationships.
				representation.setLeftHandSideRelationships(newSingleIsARelationship(leftNamedClass));
			} else {
				representation.setLeftHandSideNamedConcept(leftNamedClass);
			}
		} else {
			// If not a named class it must be an expression which can be converted to a set of relationships
			representation.setLeftHandSideRelationships(getRelationships(leftHandExpression));
		}

		Long rightNamedClass = getNamedClass(owlAxiom, rightHandExpression, "right");
		if (rightNamedClass != null) {
			if (referencedComponentId != null && !referencedComponentId.equals(rightNamedClass)) {
				// Force the named concept which is not the referencedComponentId to be returned as a set of relationships.
				representation.setRightHandSideRelationships(newSingleIsARelationship(rightNamedClass));
			} else {
				representation.setRightHandSideNamedConcept(rightNamedClass);
			}
		} else {
			// If not a named class it must be an expression which can be converted to a set of relationships
			representation.setRightHandSideRelationships(getRelationships(rightHandExpression));
		}

		return representation;
	}

	private Map<Integer, List<Relationship>> newSingleIsARelationship(Long leftNamedClass) {
		Map<Integer, List<Relationship>> relationships = new HashMap<>();
		relationships.put(0, Collections.singletonList(new Relationship(0, Concepts.IS_A_LONG, leftNamedClass)));
		return relationships;
	}

	/**
	 * 	Currently supported axiom types are SubClassOf and EquivalentClasses - axioms of other types will be ignored.
	 *
	 * @param conceptAxiomMap
	 * @return
	 * @throws ConversionException
	 */
	public Map<Long, Set<AxiomRepresentation>> convertAxiomsToRelationships(Map<Long, Set<OWLAxiom>> conceptAxiomMap) throws ConversionException {
		Map<Long, Set<AxiomRepresentation>> conceptAxiomStatements = new HashMap<>();
		for (Long conceptId : conceptAxiomMap.keySet()) {
			Set<OWLAxiom> axioms = conceptAxiomMap.get(conceptId);
			for (OWLAxiom axiom : axioms) {
				AxiomRepresentation axiomRepresentation = convertAxiomToRelationships(conceptId, axiom);
				if (axiomRepresentation != null) {
					conceptAxiomStatements.computeIfAbsent(conceptId, id -> new HashSet<>()).add(axiomRepresentation);
				}
			}
		}
		return conceptAxiomStatements;
	}

	public String convertRelationshipsToAxiom(AxiomRepresentation representation) {

		// Identify and convert object and data property axioms
		if (representation.getLeftHandSideNamedConcept() != null && representation.getRightHandSideRelationships() != null) {
			List<Relationship> relationships = representation.getRightHandSideRelationships().get(0);
			for (Relationship relationship : relationships) {
				if (relationship.getTypeId() == Concepts.IS_A_LONG) {
					if (objectAttributes != null && objectAttributes.contains(relationship.getDestinationId())) {
						// Attributes will only have one parent
						return axiomToString(ontologyService.createOwlSubObjectPropertyOfAxiom(representation.getLeftHandSideNamedConcept(), relationship.getDestinationId()));
					} else if (dataAttributes != null && dataAttributes.contains(relationship.getDestinationId())) {
						return axiomToString(ontologyService.createOwlSubDataPropertyOfAxiom(representation.getLeftHandSideNamedConcept(), relationship.getDestinationId()));
					} else {
						// If the first parent is not an attribute then the concept is not an attribute.
						break;
					}
				}
			}
		}

		// Normal axioms and GCI axioms go through here
		return axiomToString(ontologyService.createOwlClassAxiom(representation));
	}

	public String axiomToString(OWLLogicalAxiom owlAxiom) {
		return owlAxiom.toString().replaceAll(CORE_COMPONENT_NAMESPACE_PATTERN, ":$1").replace(") )", "))");
	}

	/**
	 * Extracts all concept ids from any axiom type.
	 * This is intended for validation purposes.
	 *
	 * @param axiomExpression The Axiom expression to extract from.
	 * @return AxiomRepresentation with the details of the expression or null if the axiom type is not supported.
	 * @throws ConversionException if the Axiom expression is malformed or of an unexpected structure.
	 */
	public Set<Long> getIdsOfConceptsNamedInAxiom(String axiomExpression) throws ConversionException {
		OWLAxiom owlAxiom = convertOwlExpressionToOWLAxiom(axiomExpression);
		return owlAxiom.getSignature().stream().map(OntologyHelper::getConceptId).collect(Collectors.toSet());
	}

	private OWLAxiom convertOwlExpressionToOWLAxiom(String axiomExpression) throws ConversionException {
		OWLAxiom owlAxiom;
		try {
			owlAxiom = snomedTaxonomyLoader.deserialiseAxiom(axiomExpression);
		} catch (OWLOntologyCreationException e) {
			throw new ConversionException("Failed to deserialise axiom expression '" + axiomExpression + "'.");
		}
		return owlAxiom;
	}

	private Long getNamedClass(OWLAxiom owlAxiom, OWLClassExpression owlClassExpression, String side) throws ConversionException {
		if (owlClassExpression.getClassExpressionType() != ClassExpressionType.OWL_CLASS) {
			return null;
		}
		Set<OWLClass> classesInSignature = owlClassExpression.getClassesInSignature();
		if (classesInSignature.size() > 1) {
			throw new ConversionException("Expecting a maximum of 1 class in " + side + " hand side of axiom, got " + classesInSignature.size() + " - axiom '" + owlAxiom.toString() + "'.");
		}

		if (classesInSignature.size() == 1) {
			OWLClass namedClass = classesInSignature.iterator().next();
			return OntologyHelper.getConceptId(namedClass);
		}
		return null;
	}

	private Map<Integer, List<Relationship>> getRelationships(OWLClassExpression owlClassExpression) throws ConversionException {
		if (owlClassExpression.getClassExpressionType() != ClassExpressionType.OBJECT_INTERSECTION_OF) {
			throw new ConversionException("Expecting ObjectIntersectionOf at first level of expression, got " + owlClassExpression.getClassExpressionType() + " in expression " + owlClassExpression.toString() + ".");
		}

		OWLObjectIntersectionOf intersectionOf = (OWLObjectIntersectionOf) owlClassExpression;
		List<OWLClassExpression> expressions = intersectionOf.getOperandsAsList();

		Map<Integer, List<Relationship>> relationshipGroups = new HashMap<>();
		int rollingGroupNumber = 0;
		for (OWLClassExpression operand : expressions) {
			ClassExpressionType operandClassExpressionType = operand.getClassExpressionType();
			if (operandClassExpressionType == ClassExpressionType.OWL_CLASS) {
				// Is-a relationship
				relationshipGroups.computeIfAbsent(0, key -> new ArrayList<>()).add(new Relationship(0, Concepts.IS_A_LONG, OntologyHelper.getConceptId(operand.asOWLClass())));

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
								throw new ConversionException("Expecting ObjectSomeValuesFrom within ObjectIntersectionOf as part of role group, got " + classExpression.getClassExpressionType() + " in expression " + owlClassExpression.toString() + ".");
							}
						}
					} else {
						throw new ConversionException("Expecting ObjectSomeValuesFrom with role group to have a value of ObjectSomeValuesFrom, got " + filler.getClassExpressionType() + " in expression " + owlClassExpression.toString() + ".");
					}
				} else {
					Relationship relationship = extractRelationship(someValuesFrom, 0);
					relationshipGroups.computeIfAbsent(0, key -> new ArrayList<>()).add(relationship);
				}

			} else {
				throw new ConversionException("Expecting Class or ObjectSomeValuesFrom at second level of expression, got " + operandClassExpressionType + " in expression " + owlClassExpression.toString() + ".");
			}
		}

		return relationshipGroups;
	}

	private Relationship extractRelationship(OWLObjectSomeValuesFrom someValuesFrom, int groupNumber) throws ConversionException {
		OWLObjectPropertyExpression property = someValuesFrom.getProperty();
		OWLObjectProperty namedProperty = property.getNamedProperty();
		long type = OntologyHelper.getConceptId(namedProperty);

		OWLClassExpression filler = someValuesFrom.getFiller();
		ClassExpressionType classExpressionType = filler.getClassExpressionType();
		if (classExpressionType != ClassExpressionType.OWL_CLASS) {
			throw new ConversionException("Expecting right hand side of ObjectSomeValuesFrom to be type Class, got " + classExpressionType + ".");
		}
		long value = OntologyHelper.getConceptId(filler.asOWLClass());

		return new Relationship(groupNumber, type, value);
	}

	private boolean isRoleGroup(OWLObjectPropertyExpression expression) {
		OWLObjectProperty namedProperty = expression.getNamedProperty();
		return SNOMED_ROLE_GROUP_FULL_URI.equals(namedProperty.getIRI().toString());
	}
}
