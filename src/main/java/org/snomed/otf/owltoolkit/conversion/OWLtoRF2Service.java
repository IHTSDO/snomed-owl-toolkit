package org.snomed.otf.owltoolkit.conversion;

import com.google.common.collect.Sets;
import org.semanticweb.elk.util.collections.Pair;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.constants.RF2Headers;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.lang.String.format;

public class OWLtoRF2Service {

	public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

	private Map<Long, String> conceptDescriptionsFSN;
	private Map<Long, String> conceptDescriptionsPreferredSynonym;
	private Map<Long, Set<OWLAxiom>> conceptAxioms;
	private Set<Long> definedConcepts;

	public void writeToRF2(InputStream owlFileStream, OutputStream rf2ZipOutputStream, Date fileDate) throws OWLException, IOException {
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology owlOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(owlFileStream);

		conceptDescriptionsFSN = new HashMap<>();
		conceptDescriptionsPreferredSynonym = new HashMap<>();
		conceptAxioms = new HashMap<>();
		definedConcepts = new HashSet<>();

		// Gather all descriptions
		for (OWLEntity owlEntity : Sets.union(owlOntology.getObjectPropertiesInSignature(), owlOntology.getClassesInSignature())) {
			IRI iri = owlEntity.getIRI();
			Long conceptId = getConceptIdFromUri(iri.toString());
			getDescriptions(conceptId, iri, owlOntology);
		}

		//for each object property, add an OWL axiom stating that the property is a subproperty of the top property - NEEDED FOR TOOLKIT NNF
		OWLDataFactory df = OWLManager.createOWLOntologyManager().getOWLDataFactory();
		OWLObjectProperty r = new ArrayList<OWLObjectProperty>(owlOntology.getObjectPropertiesInSignature()).get(0);
		String iriString = r.getIRI().toString();
		OWLObjectProperty topProp = df.getOWLObjectProperty(IRI.create(iriString.substring(0, iriString.lastIndexOf("/")+1) + "762705008")); //TODO: check
		for(OWLObjectProperty prop : owlOntology.getObjectPropertiesInSignature()) {
			if(!prop.toString().contains("762705008")) {
				OWLAxiom ax = df.getOWLSubObjectPropertyOfAxiom(prop, topProp);
				man.addAxiom(owlOntology, ax);
			}
		}

		// Grab all axioms and process by type
		for (OWLAxiom axiom : owlOntology.getAxioms()) {
			if (axiom instanceof OWLObjectPropertyAxiom) {
				OWLObjectProperty namedConcept = axiom.getObjectPropertiesInSignature().iterator().next();
				addAxiom(getConceptIdFromUri(namedConcept.getNamedProperty().getIRI().toString()), axiom);
			} else if (axiom instanceof OWLSubClassOfAxiom) {
				OWLSubClassOfAxiom subClassOfAxiom = (OWLSubClassOfAxiom) axiom;
				OWLClassExpression subClass = subClassOfAxiom.getSubClass();
				if (subClass.isAnonymous()) {
					// Found GCI axiom
					OWLClassExpression superClass = subClassOfAxiom.getSuperClass();
					addAxiom(getFirstConceptIdFromClassList(superClass.getClassesInSignature()), axiom);
				} else {
					addAxiom(getFirstConceptIdFromClassList(subClass.getClassesInSignature()), axiom);
				}
			} else if (axiom instanceof OWLEquivalentClassesAxiom) {
				OWLEquivalentClassesAxiom equivalentClassesAxiom = (OWLEquivalentClassesAxiom) axiom;
				Set<OWLClassExpression> classExpressions = equivalentClassesAxiom.getClassExpressions();
				OWLClass next = (OWLClass) classExpressions.iterator().next();
				long conceptId = getConceptIdFromUri(next.getIRI().toString());
				addAxiom(conceptId, axiom);
				definedConcepts.add(conceptId);
			}
		}

		String date = SIMPLE_DATE_FORMAT.format(fileDate);
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(rf2ZipOutputStream)) {
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zipOutputStream))) {
				// Write concept file
				zipOutputStream.putNextEntry(new ZipEntry(format("SnomedCT/Snapshot/Terminology/sct2_Concept_Snapshot_INT_%s.txt", date)));
				writer.write(RF2Headers.CONCEPT_HEADER);
				newline(writer);
				for (Long conceptId : conceptAxioms.keySet()) {
					// id      effectiveTime   active  moduleId        definitionStatusId
					String definitionStatus = definedConcepts.contains(conceptId) ? Concepts.FULLY_DEFINED : Concepts.PRIMITIVE;
					writer.write(String.join("\t", conceptId.toString(), "0", "1", Concepts.SNOMED_CT_CORE_MODULE, definitionStatus));
					newline(writer);
				}
				//metadata needed - TODO: clean
				writer.write(String.join("\t", "138875005", "0", "1", Concepts.SNOMED_CT_CORE_MODULE, definedConcepts.contains(138875005L) ? Concepts.FULLY_DEFINED : Concepts.PRIMITIVE));
				newline(writer);
				writer.write(String.join("\t", "900000000000441003", "0", "1", Concepts.SNOMED_CT_CORE_MODULE, definedConcepts.contains(900000000000441003L) ? Concepts.FULLY_DEFINED : Concepts.PRIMITIVE));
				newline(writer);
				writer.write(String.join("\t", "410662002", "0", "1", Concepts.SNOMED_CT_CORE_MODULE, definedConcepts.contains(410662002L) ? Concepts.FULLY_DEFINED : Concepts.PRIMITIVE));
				newline(writer);
				writer.write(String.join("\t", "762705008", "0", "1", Concepts.SNOMED_CT_CORE_MODULE, definedConcepts.contains(762705008L) ? Concepts.FULLY_DEFINED : Concepts.PRIMITIVE));
				writer.flush();

				// Write description file with FSNs
				zipOutputStream.putNextEntry(new ZipEntry(format("SnomedCT/Snapshot/Terminology/sct2_Description_Snapshot-en_INT_%s.txt", date)));
				writer.write(RF2Headers.DESCRIPTION_HEADER);
				newline(writer);
				int dummySequence = 100000000;
				Map<Pair<Long, String>, String> conceptTermId = new HashMap<>();
				for (Long conceptId : conceptDescriptionsFSN.keySet()) {
					// id	effectiveTime	active	moduleId	conceptId	languageCode	typeId	term	caseSignificanceId
					String descriptionId = format("%s011", dummySequence++);
					String term = conceptDescriptionsFSN.get(conceptId);
					conceptTermId.put(new Pair<>(conceptId, term), descriptionId);
					writer.write(String.join("\t", descriptionId, "0", "1", Concepts.SNOMED_CT_CORE_MODULE, conceptId.toString(), "en", "900000000000003001", term, "900000000000448009"));
					//writer.write(String.join("\t", descriptionId, "0", "1", Concepts.SNOMED_CT_CORE_MODULE, conceptId.toString(), "en", Concepts.FULLY_DEFINED, term, "900000000000448009"));
					newline(writer);
				}
				// Write description file with preferred synonyms
				Map<Pair<Long, String>, String> conceptPreferredSynonymTermId = new HashMap<>();
				for (Long conceptId : conceptDescriptionsPreferredSynonym.keySet()) {
					// id	effectiveTime	active	moduleId	conceptId	languageCode	typeId	term	caseSignificanceId
					String descriptionId = format("%s011", dummySequence++);
					String term = conceptDescriptionsPreferredSynonym.get(conceptId);
					conceptPreferredSynonymTermId.put(new Pair<>(conceptId, term), descriptionId);
					writer.write(String.join("\t", descriptionId, "0", "1", Concepts.SNOMED_CT_CORE_MODULE, conceptId.toString(), "en", "900000000000013009", term, "900000000000448009"));
					//writer.write(String.join("\t", descriptionId, "0", "1", Concepts.SNOMED_CT_CORE_MODULE, conceptId.toString(), "en", Concepts.FULLY_DEFINED, term, "900000000000448009"));
					newline(writer);
				}

				//metadata needed - TODO: clean
				writer.write(String.join("\t", "517382016", "0", "1", Concepts.SNOMED_CT_CORE_MODULE, "138875005", "en", Concepts.FULLY_DEFINED, "SNOMED CT Concept (SNOMED RT+CTV3)", "900000000000448009"));
				newline(writer);
				writer.write(String.join("\t", "900000000000952015", "0", "1", Concepts.SNOMED_CT_CORE_MODULE, "900000000000441003", "en", Concepts.FULLY_DEFINED, "SNOMED CT Model Component (metadata)", "900000000000017005"));
				newline(writer);
				writer.write(String.join("\t", "2466114012", "0", "1", Concepts.SNOMED_CT_CORE_MODULE, "410662002", "en", Concepts.FULLY_DEFINED, "Concept model attribute (attribute)", "900000000000448009"));
				newline(writer);
				writer.write(String.join("\t", "3635487013", "0", "1", Concepts.SNOMED_CT_CORE_MODULE, "762705008", "en", Concepts.FULLY_DEFINED, "Concept model object attribute (attribute)", "900000000000448009"));
				newline(writer);
				writer.write(String.join("\t", "3635487013", "0", "1", Concepts.SNOMED_CT_CORE_MODULE, "762705008", "en", Concepts.FULLY_DEFINED, "Concept model object attribute", "900000000000448009"));
				newline(writer);
				writer.write(String.join("\t", "680946014", "0", "1", Concepts.SNOMED_CT_CORE_MODULE, "116680003", "en", Concepts.FULLY_DEFINED, "Is a (attribute)", "900000000000448009"));
				writer.flush();

				// Write "text definition file" -- TODO: check, seems to be needed for printing FSNs?
				zipOutputStream.putNextEntry(new ZipEntry(format("SnomedCT/Snapshot/Terminology/sct2_TextDefinition_Snapshot-en_INT_%s.txt", date)));
				writer.write(RF2Headers.DESCRIPTION_HEADER);
				newline(writer);
				int dummySequence2 = 100000000;
				//Map<Pair<Long, String>, String> conceptTermId = new HashMap<>();
				for (Long conceptId : conceptDescriptionsFSN.keySet()) {
					// id	effectiveTime	active	moduleId	conceptId	languageCode	typeId	term	caseSignificanceId
					String descriptionId = format("%s011", dummySequence2++);
					String term = conceptDescriptionsFSN.get(conceptId);
					conceptTermId.put(new Pair<>(conceptId, term), descriptionId);
					writer.write(String.join("\t", descriptionId, "0", "1", Concepts.SNOMED_CT_CORE_MODULE, conceptId.toString(), "en", "900000000000003001", term, "900000000000448009"));
					newline(writer);
				}
				writer.flush();

				// Write lang refset file for FSNs
				zipOutputStream.putNextEntry(new ZipEntry(format("SnomedCT/Snapshot/Refset/Language/der2_cRefset_LanguageSnapshot-en_INT_%s.txt", date)));
				writer.write(RF2Headers.LANGUAGE_REFERENCE_SET_HEADER);
				newline(writer);
				for (Long conceptId : conceptDescriptionsFSN.keySet()) {
					String term = conceptDescriptionsFSN.get(conceptId);
					String descriptionId = conceptTermId.get(new Pair(conceptId, term));

					// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	acceptabilityId
					writer.write(String.join("\t", UUID.randomUUID().toString(), "0", "1", Concepts.SNOMED_CT_CORE_MODULE,
							Concepts.US_LANGUAGE_REFSET, descriptionId, Concepts.PREFERRED));
					newline(writer);
				}
				writer.flush();

				// Write OWL expression refset file
				zipOutputStream.putNextEntry(new ZipEntry(format("SnomedCT/Snapshot/Terminology/sct2_sRefset_OWLExpressionSnapshot_INT_%s.txt", date)));
				writer.write(RF2Headers.OWL_EXPRESSION_REFERENCE_SET_HEADER);
				newline(writer);
				for (Long conceptId : conceptAxioms.keySet()) {
					for (OWLAxiom owlAxiom : conceptAxioms.get(conceptId)) {
						// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	owlExpression
						String axiomString = owlAxiom.toString();
						axiomString = axiomString.replace("<http://snomed.info/id/", ":");
						axiomString = axiomString.replace(">", "");
						writer.write(String.join("\t", UUID.randomUUID().toString(), "0", "1", Concepts.SNOMED_CT_CORE_MODULE,
								Concepts.OWL_AXIOM_REFERENCE_SET, conceptId.toString(), axiomString));
						newline(writer);
					}
				}
				//needed metadata - TODO: clean
				writer.write(String.join("\t", UUID.randomUUID().toString(), "0", "1", Concepts.SNOMED_CT_CORE_MODULE, Concepts.OWL_AXIOM_REFERENCE_SET, "762705008", "SubClassOf(:762705008 :410662002)"));
				newline(writer);
				writer.write(String.join("\t", UUID.randomUUID().toString(), "0", "1", Concepts.SNOMED_CT_CORE_MODULE, Concepts.OWL_AXIOM_REFERENCE_SET, "410662002", "SubClassOf(:410662002 :900000000000441003)"));
				newline(writer);
				writer.write(String.join("\t", UUID.randomUUID().toString(), "0", "1", Concepts.SNOMED_CT_CORE_MODULE, Concepts.OWL_AXIOM_REFERENCE_SET, "900000000000441003", "SubClassOf(:900000000000441003 :138875005)"));
				writer.flush();

				// Write relationship file (expected, can be empty)
				zipOutputStream.putNextEntry(new ZipEntry(format("SnomedCT/Snapshot/Terminology/sct2_Relationship_Snapshot_INT_%s.txt", date)));
				writer.write(RF2Headers.OWL_EXPRESSION_REFERENCE_SET_HEADER);
				newline(writer);
				writer.flush();
			}

		}
	}

	private void newline(BufferedWriter writer) throws IOException {
		writer.write("\r\n");// Add windows line ending before newline
		//writer.newLine();
	}

	private void getDescriptions(Long conceptId, IRI iri, OWLOntology owlOntology) {
		Set<OWLAnnotationAssertionAxiom> annotationAssertionAxioms = owlOntology.getAnnotationAssertionAxioms(iri);
		for (OWLAnnotationAssertionAxiom annotationAssertionAxiom : annotationAssertionAxioms) {
			if ("rdfs:label".equals(annotationAssertionAxiom.getProperty().toString())) {
				String value = annotationAssertionAxiom.getValue().toString();
				if (value.startsWith("\"")) {
					value = value.substring(1, value.lastIndexOf("\""));
				}
				conceptDescriptionsFSN.put(conceptId, value);
			}
			else if ("skos:prefLabel".equals(annotationAssertionAxiom.getProperty().toString())) {
				String value = annotationAssertionAxiom.getValue().toString();
				if(value.startsWith("\"")) {
					value = value.substring(1, value.lastIndexOf("\""));
				}
				conceptDescriptionsPreferredSynonym.put(conceptId, value);
			}
		}
	}


	private Long getFirstConceptIdFromClassList(Set<OWLClass> classesInSignature) {
		return getConceptIdFromUri(classesInSignature.iterator().next().getIRI().toString());
	}

	private long getConceptIdFromUri(String uri) {
		return Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
	}

	private boolean addAxiom(Long conceptId, OWLAxiom axiom) {
		return conceptAxioms.computeIfAbsent(conceptId, (id) -> new HashSet<>()).add(axiom);

	}

	private boolean addAxiom(Long conceptId, Collection<? extends OWLAxiom> axioms) {
		return conceptAxioms.computeIfAbsent(conceptId, (id) -> new HashSet<>()).addAll(axioms);
	}

}
