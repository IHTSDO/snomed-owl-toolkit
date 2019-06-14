#!/usr/bin/env bash

#
# This script is for use by the SNOMED International team to convert extension releases to Complete OWL for managed service members.
# The script provides an automated repeatable process to support testing and production upgrade of extensions to SNOMED International 20190731 edition.
# The inputs are the previous release zip, the SNOMED International complete OWL 20190731 release zip file, and the current release delta zip containing stated relationships and some OWL axioms.
# The output is a delta zip for the current release containing all stated relationships from the previous release as inactive
# and a set of OWL axioms generated from all stated relationships plus those in the current release.
# Note: OWL axioms generated are for the extension module concepts only. Any concepts donated to the International Edition will be excluded as these exist in the International release already.
#


# Make script stop if any line fails
set -e;

echo "--"
echo "-- Checking inputs"
echo "--"
if [ "$#" -ne 3 ]; then
  echo "You must enter exactly 3 command line arguments."
  echo "The path to the previous release snapshot, the dependent International release snapshot and the path to the current authoring delta."
  exit 1;
fi

owlToolkitJar="`ls target/snomed-owl-toolkit*-executable.jar | tail -n1`"
previous_snapshot=$1
int_snapshot=$2
delta=$3
if [ ! -f $owlToolkitJar ]; then
  echo "SNOMED OWL Toolkit jar not found!"
  exit 1;
fi
if [ ! -f $previous_snapshot ]; then
  echo "previous_snapshot file not found!"
  exit 1;
fi
if [ ! -f $int_snapshot ]; then
  echo "int_snapshot file not found!"
  exit 1;
fi
if [ ! -f $delta ]; then
  echo "Delta file not found!"
  exit 1;
fi

snapshot=$previous_snapshot,$int_snapshot
echo "Jar: $owlToolkitJar"
echo "Snapshot files: $snapshot" 
echo "Delta: $delta"
echo


echo "--"
echo "-- Classifying (before conversion)"
echo "--"
java -jar $owlToolkitJar -classify -rf2-snapshot-archives $snapshot -rf2-authoring-delta-archive $delta;
echo


echo "--"
echo "-- Generating Ontology file (before conversion)"
echo "--"
java -jar $owlToolkitJar -rf2-to-owl -without-annotations -rf2-snapshot-archives $snapshot -rf2-authoring-delta-archive $delta;
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
echo "-- Converting to Complete OWL"
echo "--"
java -jar $owlToolkitJar -rf2-stated-to-complete-owl -rf2-snapshot-archives $snapshot -rf2-authoring-delta-archive $delta;
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
echo "- Replacing Stated Relationship delta"
cat sct2_StatedRelationship_Delta* > */Delta/Terminology/sct2_StatedRelationship_Delta*
echo "- Replacing OWL delta"
cat sct2_sRefset_OWL* > */Delta/Terminology/sct2_sRefset_OWL*
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
java -jar $owlToolkitJar -rf2-to-owl -without-annotations -rf2-snapshot-archives $snapshot -rf2-authoring-delta-archive $completeOWL;
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
echo "-- Checking extension stated relationships are not converted into axioms"
echo "--"
if [ "`wc -l sct2_StatedRelationships_Not_Converted.txt | sed 's/^ *\([0-9]*\).*/\1/g'`" -eq 1 ]; then
  echo "- All extension stated relationships are converted into axioms."
else
  echo "Found extension stated relationships are not converted into axioms due to overriding the International stated view."
  echo "Please check the contents of sct2_StatedRelationships_Not_Converted.txt"
fi
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
java -jar $owlToolkitJar -classify -rf2-snapshot-archives $snapshot -rf2-authoring-delta-archive $completeOWL;
echo
