# Snomed OWL Toolkit [![Build Status](https://travis-ci.org/IHTSDO/snomed-owl-toolkit.svg?branch=master)](https://travis-ci.org/IHTSDO/snomed-owl-toolkit) [![codecov](https://codecov.io/gh/IHTSDO/snomed-owl-toolkit/branch/master/graph/badge.svg)](https://codecov.io/gh/IHTSDO/snomed-owl-toolkit)

An Open Source Toolkit to make SNOMED CT to OWL conversion and classification simple.

This toolkit is used within the SNOMED International Authoring Environment. It is backward compatible with all past RF2 releases and will be forward compatible with all future releases. New versions of this tool will be produced as new description logic features are added to SNOMED CT.

A classification REST API using this toolkit is available, see the [Classification Service](https://github.com/IHTSDO/classification-service).

## Java Library Capabilities
- Convert Snomed RF2 to an OWL Ontology
  - Works on the command line
  - Uses MRCM Attribute Domain Reference Set for grouping information
  - Uses OWL Axiom Reference Set
  - Includes all descriptions:
    - Fully Specified Names as rdfs:label
    - Preferred Synonyms as skos:prefLabel
    - Other Synonyms as skos:altLabel
    - Text Definitions as skos:definition
    - Language and dialect markup
  - Support for Snomed Editions and Extensions
  - *For Java integration see RF2ToOWLService.java*
- Classify Snomed
  - Uses RF2 to OWL conversion
    - Optionally load an RF2 Delta archive on top of the Snapshot
  - RF2 delta output of relationship changes (no identifier generation)
  - Any OWL API Reasoner supported (default is ELK)
  - *See SnomedReasonerService.java*
- Authoring Environment UI Support
  - Convert relationships to axioms
  - Convert axioms to relationships
  - Support for General Concept Axioms
  - *See AxiomRelationshipConversionService.java*

### Stated Relationships and Axioms
The toolkit is capable of working with stated relationships and/or axioms from the OWL Axiom reference set.

_These should only be used when modelling concepts.
The inferred relationships should nearly always be used in SNOMED CT implementations. The inferred relationships contain the attributes of any stated relationships or axioms present._

Concept model information will be loaded from all active relationships and all active axioms. Either format can be used or both in combination.
All the stated relationships of a single concept will form a single new axiom. If there are any axioms in the reference set for the same concept these will be treated as additional axioms.
Stated relationships are never merged into an axiom from the reference set.

If there are only axioms in the International Edition and only stated relationships in an extension then classification or conversion to the OWL file will still work.
The extension can add modelling for its own concepts using stated relationships or axioms. If the extension wants to override the modelling of an International Axiom then a new
state of that entire axiom must be in the extension using the same OWL Axiom reference set member id.

## Documentation
* [Calculating the Necessary Normal Form](documentation/calculating-necessary-normal-form.md)

## Command Line Use

This toolkit has been developed for use in other Java applications as a library but some functionality
can be used via the command line.

The 'executable' jar file is available on the [latest release](https://github.com/IHTSDO/snomed-owl-toolkit/releases) page for use on the command line.

Command line options:
```
Usage:
 -help                                  Print this help message.

 -classify                              Run classification process.
                                        Results are written to an RF2 delta archive.

 -rf2-to-owl                            (Default mode) Convert RF2 to OWL Functional Syntax.
                                        Results are written to an .owl file.

 -rf2-stated-to-complete-owl            Convert RF2 snapshot stated relationships to complete OWL Axiom reference set preview.
                                        Stated relationships are converted to OWL Axiom reference set entries.
                                        Existing stated relationships are marked as inactive.
                                        Results are written to a zip file containing:
                                         - OWL Axiom reference set delta of all axioms which were previously stated relationships
                                         - Stated relationship delta with all relationships which were previously active marked as inactive.

 -rf2-snapshot-archives <path>          Comma separated paths of zip files containing RF2 Snapshot files to be loaded.
                                        At least one Snapshot archive is required.

 -rf2-authoring-delta-archive <path>    (Optional) Path to a zip file containing RF2 Delta files to be applied on top
                                        of the Snapshots. This is helpful during an authoring cycle.

 -debug                                 Additional output for debugging.


Optional parameters for OWL conversion:
 -uri <uri>                             (Optional) URI for the ontology identifier.
                                        Defaults to the id within the header entry of the OWL Ontology reference set.
                                        If no entry found defaults to http://snomed.info/sct/900000000000207008.

 -version <version>                     (Optional) Date for the ontology version e.g. 20180731.
                                        Defaults to today's date.

 -without-annotations                   (Optional) Flag to omit description annotations from the ontology
                                        resulting in a smaller file size.
```

### Snomed RF2 to OWL File Conversion
Convert Snomed RF2 files to an OWL ontology file with functional syntax.

Using the executable jar supply a zip file which contains RF2 snapshot files as an argument.
```bash
java -Xms4g -jar snomed-owl-toolkit*executable.jar -rf2-to-owl -rf2-snapshot-archives SnomedCT_InternationalRF2.zip
```
After about a minute the OWL ontology file will be written to `ontology-xxxx.owl` including a timestamp in the name.

Description dialects will be set for language reference sets included in the [language-refset-dialect-map.properties](src/main/resources/language-refset-dialect-map.properties) file. 
The jar file will need rebuilding in order for changes to that file to be included. Feel free to raise a pull request to add your language reference sets to this project.

### Snomed RF2 Classification
Run the classification process.

Using the executable jar supply a zip file which contains RF2 snapshot files as an argument.
```bash
java -Xms4g -jar snomed-owl-toolkit*executable.jar -classify -rf2-snapshot-archives SnomedCT_InternationalRF2.zip
```
After about one and a half minutes an RF2 delta archive will be written to `classification-results-xxxx.zip` including a timestamp in the name.

This archive contains a relationship file with active rows for new inferences and inactive rows for redundant relationships.

The archive also has a reference set containing any sets of concepts which the reasoner found to be logically equivalent. This refset should be empty.
