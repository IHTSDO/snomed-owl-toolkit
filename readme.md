# Snomed OWL Toolkit

An Open Source Toolkit enabling simple SNOMED CT to OWL conversion and classification.

This toolkit is used within the SNOMED International Authoring Environment and will keep pace with all the latest changes.

## Capabilities
- Convert Snomed RF2 to an OWL Ontology (excludes descriptions)
  - Uses MRCM Attribute Domain Reference Set for grouping information
  - Uses OWL Axiom Reference Set
  - Backward compatible with older Snomed RF2 releases
  - Uses Snomed RF2 Snapshot archive or stream
  - Optionally load an RF2 Delta archive on top of the Snapshot
  - *See RF2ToOWLService.java*
- Classify Snomed
  - Uses RF2 to OWL convertion
  - Any OWL API Reasoner supported (default is ELK)
  - RF2 delta output of relationship changes (no identifier generation)
  - *See SnomedReasonerService.java*
- Authoring Environment UI Support
  - Convert relationships to axioms
  - Convert axioms to relationships
  - Support for General Concept Axioms
  - *See AxiomRelationshipConversionService.java*

## Snomed RF2 to OWL File Conversion
Convert your Snomed RF2 archive containing Snapshot files to an OWL ontology file (functional syntax). Download the latest release then in the terminal:
```bash
java -jar snomed-owl-toolkit*.jar -rf2-snap-zip SnomedCT_InternationalRF2.zip
```
After about a minute the OWL ontology file will be written to `ontology-xxxx.owl` including a timestamp in the name.
