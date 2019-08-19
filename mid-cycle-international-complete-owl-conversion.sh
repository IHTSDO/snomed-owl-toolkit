#!/usr/bin/env bash

#
# This script is for use by the SNOMED International team to convert the International release during mid authoring cycle with stated relationships to Complete OWL and reconcile with the published 20190731 Complete OWL release.
# The script provides an automated repeatable process to support testing and production conversion of mid cycle authoring delta into OWL axioms reference set.
# The inputs are the snapshot export archive from TermServer with stated relationships(used for OWL conversion), the SNOMED International complete OWL 20190731 release file and the mid cycle authoring delta export archive.
# The mid cycle authoring delta export will contain stated relationships and some OWL axioms.
# The output is a delta containing a set of OWL axioms converted from mid cycle authoring with stated relationships.
# These OWL axiom reference set components are reconciled with the published complete OWL axioms reference sets.
# Note:
#1.The same uuid is used for an update.
# 2.OWL axioms are inactive for inacivated concepts.
# 3.New axioms are generated for new concepts with brand new UUIDs.
#

# Make script stop if any line fails
set -e;

echo "--"
echo "-- Checking inputs"
echo "--"
if [ "$#" -ne 3 ]; then
  echo "You must enter exactly 3 command line arguments."
  echo "The path to the TermServer snapshot for 20190731 release and the published 20190731 International Complete OWL release snapshot and the mid cycle authoring delta export"
  exit 1;
fi

owlToolkitJar="`ls target/snomed-owl-toolkit*-executable.jar | tail -n1`"
termServer_export_snapshot=$1
published_owl_snapshot=$2
delta=$3


if [ ! -f $owlToolkitJar ]; then
  echo "SNOMED OWL Toolkit jar not found!"
  exit 1;
fi
if [ ! -f $termServer_export_snapshot ]; then
  echo "termServer export snapshot file not found!"
  exit 1;
fi
if [ ! -f $published_owl_snapshot ]; then
  echo "published OWL snapshot file not found!"
  exit 1;
fi

if [ ! -f $delta ]; then
  echo "mid cycle delta file not found!"
  exit 1;
fi

snapshot=$termServer_export_snapshot,$published_owl_snapshot
echo "Jar: $owlToolkitJar"
echo "Snapshot files: $snapshot"
echo "Delta file: $delta"
echo

echo "--"
echo "-- Classifying (before conversion)"
echo "--"
#java -jar $owlToolkitJar -classify -rf2-snapshot-archives $termServer_export_snapshot -rf2-authoring-delta-archive $delta;
echo


echo "--"
echo "-- Generating Ontology file (before conversion)"
echo "--"
java -jar $owlToolkitJar -rf2-to-owl -without-annotations -rf2-snapshot-archives $termServer_export_snapshot -rf2-authoring-delta-archive $delta;
ontologyBefore="`ls -rt ontology-* | tail -n1`"
if [ ! -f $ontologyBefore ]; then
  echo "Can not find generated ontology file"
  exit 1;
fi
mv "$ontologyBefore" "ontology-before-conversion.owl"
ontologyBefore="ontology-before-conversion.owl"
echo "Generated $ontologyBefore"
echo


echo "--"
echo "-- Converting to Complete OWL and reconcile"
echo "--"
java -jar $owlToolkitJar -rf2-stated-to-complete-owl-reconcile -rf2-snapshot-archives $snapshot -rf2-authoring-delta-archive $delta;
conversionDelta="`ls -rt complete-owl-axiom-delta* | tail -n1`"
echo "- Created $conversionDelta"
echo

echo "--"
echo "-- Merging Complete OWL with Delta archive"
echo "--"
rm -rf work
mkdir work
unzip $delta '**/Delta/*' -d work
cd work
unzip ../$conversionDelta
echo "- Replacing Stated Relationship delta with empty data"
head -n1 */Delta/Terminology/sct2_StatedRelationship_Delta* > stated_tmp.txt
cat stated_tmp.txt > */Delta/Terminology/sct2_StatedRelationship_Delta*
rm stated_tmp.txt
echo "- Replacing OWL delta"
# First make one OWL refset file - termserver is exporting OWLAxiom and OWLOntology separately
cd */Delta/Terminology/
# OWLOntology delta will be blank - remove
rm sct2_sRefset_OWLOntology*
# Rename OWLAxiom to OWLExpression
mv sct2_sRefset_OWLAxiomDelta_INT_20190701.txt sct2_sRefset_OWLExpressionDelta_INT_20190701.txt
cd -
# Append conversion output to export delta
cat sct2_sRefset_OWL* > */Delta/Terminology/sct2_sRefset_OWLExpression*
echo "- Creating zip"
completeOWL=SnomedCT_CompleteOWLDelta.zip
zip -r $completeOWL S*
mv $completeOWL ..
cd ..
echo "- Created $completeOWL"
echo


echo "--"
echo "-- Generating Ontology file (after conversion)"
echo "--"
java -jar $owlToolkitJar -rf2-to-owl -without-annotations -rf2-snapshot-archives $published_owl_snapshot -rf2-authoring-delta-archive $completeOWL -debug;
ontologyAfter="`ls -rt ontology-* | tail -n1`"
if [ ! -f $ontologyAfter ]; then
  echo "Can not find generated ontology file"
  exit 1;
fi
mv "$ontologyAfter" "ontology-after-conversion.owl"
ontologyAfter="ontology-after-conversion.owl"
echo "- Generated $ontologyAfter"
echo

echo "--"
echo "-- Diffing Ontology files"
echo "--"
diff $ontologyBefore $ontologyAfter > ontology-diff.txt
if [ "`wc -l ontology-diff.txt | sed 's/^ *\([0-9]*\).*/\1/g'`" -eq 0 ]; then
  echo "- Ontology files before and after OWL conversion are identical"
else
  echo "Difference found between Ontology files before and after OWL conversion! Check the contents of ontology-diff.txt"
  exit 1
fi
echo


echo "--"
echo "-- Classifying Complete OWL"
echo "--"
java -jar $owlToolkitJar -classify -rf2-snapshot-archives $published_owl_snapshot -rf2-authoring-delta-archive $completeOWL;
echo
