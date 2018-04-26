# Snomed OWL Toolkit

An Open Source Toolkit enabling simple SNOMED CT to OWL conversion and classification.

This toolkit is used within the SNOMED International Authoring Environment and will keep pace with all the latest changes.

A classification REST API using this toolkit is available, see the [Classification Service](https://github.com/IHTSDO/classification-service).

## Capabilities
- Convert Snomed RF2 to an OWL Ontology
  - Works on the command line
  - Uses MRCM Attribute Domain Reference Set for grouping information
  - Uses OWL Axiom Reference Set
  - Can include FSN annotations
  - Backward compatible with older Snomed RF2 releases
  - Uses a Snomed Edition RF2 Snapshot archive
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

## Snomed RF2 to OWL File Conversion
Convert Snomed RF2 files to an OWL ontology file with functional syntax.

Download the [latest release](https://github.com/IHTSDO/snomed-owl-toolkit/releases), then on the command line give the RF2 file as an argument to the tool. 
```bash
java -jar snomed-owl-toolkit*executable.jar -rf2-snapshot-archives SnomedCT_InternationalRF2.zip
```
After about a minute the OWL ontology file will be written to `ontology-xxxx.owl` including a timestamp in the name.

Full argument options here:
```bash
Usage:
 -help                                  Print this help message.
 
 -rf2-snapshot-archives <path>          Comma separated paths of zip files containing RF2 Snapshot files to be loaded. 
                                        At least one Snapshot archive is required.
 
 -rf2-authoring-delta-archive <path>    (Optional) Path to a zip file containing RF2 Delta files to be applied on top 
                                        of the Snapshots. This is helpful during an authoring cycle.
 
 -uri <uri>                             (Optional) URI for the ontology identifier.
                                        Defaults to http://snomed.info/sct/900000000000207008.
 
 -version <version>                     (Optional) Date for the ontology version e.g. 20180731.
                                        Defaults to today's date.
 
 -without-annotations                   (Optional) Flag to omit Fully Specified Name annotations from the ontology 
                                        resulting in a smaller file size.
```
