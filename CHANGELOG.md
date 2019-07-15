# Changelog
All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
The change log format is inspired by [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).


## 2.7.1 Release - 2019-07-15

Feature release with scripts to support complete OWL conversion.

### Features
- Script for conversion of extensions using stated relationships to OWL axiom reference set.
- Script for mid-cycle conversion of International Edition (internal use only).

### Improvements
- Prevent MRCM ungrouped attributes being grouped during axiom authoring.

### Fixes
- Deserialised axiom representation for object properties set to primitive.
- Issue #26 Not grouped attribute list incomplete when using complete OWL International release
- Remove hardcoded active ingredient union grouping.


## 2.6.0 Release - 2019-06-06

Minor changes after feedback from the Modelling Advisory Group on the International Alpha release.

### Improvements
- Use class axioms to make 'Concept model object attribute' and 'Concept model data attribute' a child of 'Concept model attribute' (CLASS-112). Representing these concepts as both properties and classes in OWL is known as 'punning' https://www.w3.org/2007/OWL/wiki/Punning.
  - Removed redundant workaround to create these relationships during NNF calculation.
- Validation improvement:
  - Do not require referenceComponentId during conversion of owlExpression to a relationship representation.
