# Snomed OWL Toolkit [![Build Status](https://travis-ci.org/IHTSDO/snomed-owl-toolkit.svg?branch=master)](https://travis-ci.org/IHTSDO/snomed-owl-toolkit) [![codecov](https://codecov.io/gh/IHTSDO/snomed-owl-toolkit/branch/master/graph/badge.svg)](https://codecov.io/gh/IHTSDO/snomed-owl-toolkit)

An Open Source Toolkit to make SNOMED CT to OWL conversion and classification simple.

This toolkit is used within the SNOMED International Authoring Environment and will keep pace with all the latest changes.

A classification REST API using this toolkit is available, see the [Classification Service](https://github.com/IHTSDO/classification-service).

## Capabilities
- Convert Snomed RF2 to an OWL Ontology
  - Works on the command line
  - Uses MRCM Attribute Domain Reference Set for grouping information
  - Uses OWL Axiom Reference Set
  - Can include FSN annotations
  - Backward compatible with all Snomed RF2 releases
  - Support for Snomed Editions and Extensions
  - *For Java integration see RF2ToOWLService.java*
- Classify Snomed
  - Uses RF2 to OWL conversion
    - Optionally load an RF2 Delta archive on top of the Snapshot
  - Any OWL API Reasoner supported (default is ELK)
  - RF2 delta output of relationship changes (no identifier generation)
  - *See SnomedReasonerService.java*
- Authoring Environment UI Support
  - Convert relationships to axioms
  - Convert axioms to relationships
  - Support for General Concept Axioms
  - *See AxiomRelationshipConversionService.java*

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
 
 -rf2-snapshot-archives <path>          Comma separated paths of zip files containing RF2 Snapshot files to be loaded. 
                                        At least one Snapshot archive is required.
 
 -rf2-authoring-delta-archive <path>    (Optional) Path to a zip file containing RF2 Delta files to be applied on top 
                                        of the Snapshots. This is helpful during an authoring cycle.
 
 -debug                                 Additional output for debugging.
 
 
Optional parameters for OWL conversion:
 -uri <uri>                             (Optional) URI for the ontology identifier.
                                        Defaults to http://snomed.info/sct/900000000000207008.
 
 -version <version>                     (Optional) Date for the ontology version e.g. 20180731.
                                        Defaults to today's date.
 
 -without-annotations                   (Optional) Flag to omit Fully Specified Name annotations from the ontology 
                                        resulting in a smaller file size.
```

### Snomed RF2 to OWL File Conversion
Convert Snomed RF2 files to an OWL ontology file with functional syntax.

Using the executable jar give an RF2 snapshot file as an argument. 
```bash
java -jar snomed-owl-toolkit*executable.jar -rf2-to-owl -rf2-snapshot-archives SnomedCT_InternationalRF2.zip
```
After about a minute the OWL ontology file will be written to `ontology-xxxx.owl` including a timestamp in the name.

### Snomed RF2 Classification
Run the classification process using a Snomed RF2 Snapshot file.

Using the executable jar give an RF2 snapshot file as an argument. 
```bash
java -jar snomed-owl-toolkit*executable.jar -classify -rf2-snapshot-archives SnomedCT_InternationalRF2.zip
```
After about one and a half minutes an RF2 delta archive will be written to `classification-results-xxxx.zip` including a timestamp in the name.

This archive contains a relationship file with active rows for new inferences and inactive rows for redundant relationships. 

The archive also has a reference set containing any sets of concepts which the reasoner found to be logically equivalent. This refset should be empty.
