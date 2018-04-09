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

## Roadmap
### End of April 2018
- Support for multiple release file inputs to enable classification of a Snomed Extension during it's authoring cycle.

## Documentation
* [Calculating the Necessary Normal Form](documentation/calculating-necessary-normal-form.md)

## Snomed RF2 to OWL File Conversion
Convert your Snomed Edition RF2 archive containing Snapshot files to an OWL ontology file (functional syntax). Download the [latest release](https://github.com/IHTSDO/snomed-owl-toolkit/releases), then on the command line give the release file and its release date as arguments to the tool. 
The release date is used in the ontology identifier. 
```bash
java -jar snomed-owl-toolkit*executable.jar -rf2-snap-zip SnomedCT_InternationalRF2.zip -version=20180731
```
After about a minute the OWL ontology file will be written to `ontology-xxxx.owl` including a timestamp in the name.
