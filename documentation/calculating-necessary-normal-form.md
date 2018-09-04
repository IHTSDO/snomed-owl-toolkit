# Snomed OWL Toolkit Documentation
## Calculating the Necessary Normal Form

This fairly complex process uses the stated form and the output of the reasoner to calculate the necessary normal form which is represented in the relationship RF2 file.

The most straight forward way to produce the necessary normal form would be to use this toolkit or the Classification Service REST API which is language agnostic. 

### High Level Process
#### Classification

1. Read the Stated Form from RF2 files.
    1. The following files are required: Concept, Stated Relationship, OWL Axiom Reference Set and MRCM Attribute Domain Reference Set.
2. Use the OWL API to infer the class hierarchy
    1. Build the Ontology object using:
        1. Axioms from the OWL Axiom Reference Set, making a note of any Transitive property axioms.
        3. Axioms created by converting Stated Relationships to OWL Axioms using the MRCM Attribute Domain Reference Set for list of attributes which should not be grouped.
    2. Use a reasoner to pre-compute the class hierarchy.

#### Necessary Normal Form Calculation

Calculating the necessary normal form happens in two passes of the hierarchy.

1. Walk the class hierarchy in a top-down, breadth first, order.
    1. For each class visited gather the stated attributes of this class and each inferred parent.
    2. Compare the attributes and remove those which are found to be redundant because they are less specific in terms of depth in the hierarchy.
    3. During this first pass build a hierarchy for each type of transitive property.
2. Walk the class hierarchy again in the same order reducing the attributes of each class further.
    1. Compare the attributes and remove those which are found to be redundant because they are less specific in terms of depth in one of the alternate hierarchies.

For fine level detail the best source of information is the Java class [_org.snomed.otf.owltoolkit.normalform.RelationshipNormalFormGenerator_](https://github.com/IHTSDO/snomed-owl-toolkit/blob/master/src/main/java/org/snomed/otf/owltoolkit/normalform/RelationshipNormalFormGenerator.java) which performs the Necessary Normal Form calculation. 
