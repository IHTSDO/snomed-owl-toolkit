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

	private Map<Long, String> conceptDescriptions;
	private Map<Long, Set<OWLAxiom>> conceptAxioms;
	private Set<Long> definedConcepts;

	public void writeToRF2(InputStream owlFileStream, OutputStream rf2ZipOutputStream, Date fileDate) throws OWLException, IOException {
		OWLOntology owlOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(owlFileStream);

		conceptDescriptions = new HashMap<>();
		conceptAxioms = new HashMap<>();
		definedConcepts = new HashSet<>();

		// Gather all descriptions
		for (OWLEntity owlEntity : Sets.union(owlOntology.getObjectPropertiesInSignature(), owlOntology.getClassesInSignature())) {
			IRI iri = owlEntity.getIRI();
			Long conceptId = getConceptIdFromUri(iri.toString());
			getDescriptions(conceptId, iri, owlOntology);
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
					writer.write(String.join("\t", conceptId.toString(), "", "1", Concepts.SNOMED_CT_CORE_MODULE, definitionStatus));
					newline(writer);
				}
				writer.flush();

				// Write description file with FSNs
				zipOutputStream.putNextEntry(new ZipEntry(format("SnomedCT/Snapshot/Terminology/sct2_Description_Snapshot-en_INT_%s.txt", date)));
				writer.write(RF2Headers.DESCRIPTION_HEADER);
				newline(writer);
				int dummySequence = 100000000;
				Map<Pair<Long, String>, String> conceptTermId = new HashMap<>();
				for (Long conceptId : conceptDescriptions.keySet()) {
					// id	effectiveTime	active	moduleId	conceptId	languageCode	typeId	term	caseSignificanceId
					String descriptionId = format("%s011", dummySequence++);
					String term = conceptDescriptions.get(conceptId);
					conceptTermId.put(new Pair<>(conceptId, term), descriptionId);
					writer.write(String.join("\t", descriptionId, "", "1", Concepts.SNOMED_CT_CORE_MODULE, conceptId.toString(), "en", Concepts.FULLY_DEFINED, term, "900000000000448009"));
					newline(writer);
				}
				writer.flush();

				// Write lang refset file for FSNs
				zipOutputStream.putNextEntry(new ZipEntry(format("SnomedCT/Snapshot/Refset/Language/der2_cRefset_LanguageSnapshot-en_INT_%s.txt", date)));
				writer.write(RF2Headers.LANGUAGE_REFERENCE_SET_HEADER);
				newline(writer);
				for (Long conceptId : conceptDescriptions.keySet()) {
					String term = conceptDescriptions.get(conceptId);
					String descriptionId = conceptTermId.get(new Pair(conceptId, term));

					// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	acceptabilityId
					writer.write(String.join("\t", UUID.randomUUID().toString(), "", "1", Concepts.SNOMED_CT_CORE_MODULE,
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
						writer.write(String.join("\t", UUID.randomUUID().toString(), "", "1", Concepts.SNOMED_CT_CORE_MODULE,
								Concepts.OWL_AXIOM_REFERENCE_SET, conceptId.toString(), axiomString));
						newline(writer);
					}
				}
				writer.flush();
			}

		}
	}

	private void newline(BufferedWriter writer) throws IOException {
		writer.write("\r");// Add windows line ending before newline
		writer.newLine();
	}

	private void getDescriptions(Long conceptId, IRI iri, OWLOntology owlOntology) {
		Set<OWLAnnotationAssertionAxiom> annotationAssertionAxioms = owlOntology.getAnnotationAssertionAxioms(iri);
		for (OWLAnnotationAssertionAxiom annotationAssertionAxiom : annotationAssertionAxioms) {
			if ("rdfs:label".equals(annotationAssertionAxiom.getProperty().toString())) {
				String value = annotationAssertionAxiom.getValue().toString();
				if (value.startsWith("\"")) {
					value = value.substring(1, value.lastIndexOf("\""));
				}
				conceptDescriptions.put(conceptId, value);
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
