package org.snomed.otf.owltoolkit.conversion;

import com.google.common.collect.Sets;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.constants.RF2Headers;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

		System.out.println("all axioms:");
		for (Long conceptId : conceptAxioms.keySet()) {
			System.out.println("Concept: " + conceptId);
			System.out.println("Description: " + conceptDescriptions.get(conceptId));
			System.out.println("Axioms: ");
			for (OWLAxiom owlAxiom : conceptAxioms.get(conceptId)) {
				System.out.println(owlAxiom.toString());
			}
			System.out.println();
		}

		String date = SIMPLE_DATE_FORMAT.format(fileDate);
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(rf2ZipOutputStream)) {
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zipOutputStream))) {

				// Write concept file
				zipOutputStream.putNextEntry(new ZipEntry(String.format("SnomedCT/Snapshot/sct2_Concept_Snapshot_INT_%s.txt", date)));
				writer.write(RF2Headers.CONCEPT_HEADER);
				newline(writer);
				for (Long conceptId : conceptAxioms.keySet()) {
					// id      effectiveTime   active  moduleId        definitionStatusId
					String definitionStatus = definedConcepts.contains(conceptId) ? Concepts.FULLY_DEFINED : Concepts.PRIMITIVE;
					writer.write(String.join("\t", conceptId.toString(), "", "1", Concepts.SNOMED_CT_CORE_MODULE, definitionStatus));
					newline(writer);
				}
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
