package org.snomed.otf.owltoolkit.taxonomy;

import org.semanticweb.owlapi.OWLAPIConfigProvider;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.functional.parser.OWLFunctionalSyntaxOWLParser;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class AxiomDeserialiser {

	private static final String ontologyDocStart = "Prefix(:=<http://snomed.info/id/>) Ontology(";
	private static final String ontologyDocEnd = ")";
	// Anything else (notably Import) could make the OWL API fetch remote URLs or read local files - PIP-1335.
	private static final List<String> ALLOWED_AXIOM_PREFIXES = List.of(
			"SubClassOf(", "EquivalentClasses(", "SubObjectPropertyOf(", "SubDataPropertyOf(", "SubAnnotationPropertyOf(",
			"TransitiveObjectProperty(", "ReflexiveObjectProperty(");
	private static final IRI BLOCKED_IMPORT_IRI = IRI.create("urn:snomed-owl-toolkit:blocked-import");

	private OWLOntology owlOntology;
	private OWLOntologyLoaderConfiguration owlOntologyLoaderConfiguration;
	private OWLFunctionalSyntaxOWLParser owlFunctionalSyntaxOWLParser;
	private List<OWLAxiom> owlAxiomsLoaded = new ArrayList<>();
	private int axiomsLoaded = 0;
	private long timeTakenDeserialisingAxioms;
	private final OWLOntologyManager owlOntologyManager;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	AxiomDeserialiser() {
		owlOntologyManager = createOntologyManager();
		try {
			owlOntology = owlOntologyManager.loadOntologyFromOntologyDocument(
					new StringDocumentSource(ontologyDocStart + ontologyDocEnd));
		} catch (OWLOntologyCreationException e) {
			throw new RuntimeException(e);
		}
		OWLAPIConfigProvider owlapiConfigProvider = new OWLAPIConfigProvider();
		owlOntologyLoaderConfiguration = owlapiConfigProvider.get();
		owlFunctionalSyntaxOWLParser = new OWLFunctionalSyntaxOWLParser();
		owlOntologyManager.addOntologyChangeListener(list -> {
			for (OWLOntologyChange owlOntologyChange : list) {
				if (owlOntologyChange instanceof AddAxiom) {
					AddAxiom addAxiom = (AddAxiom) owlOntologyChange;
					owlAxiomsLoaded.add(addAxiom.getAxiom());
				}
			}
		});
		timeTakenDeserialisingAxioms = 0;
	}

	static OWLOntologyManager createOntologyManager() {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		// A mapper returning null falls back to loading the import IRI itself, so map every IRI to an unloadable one.
		manager.getIRIMappers().clear();
		manager.getIRIMappers().add((OWLOntologyIRIMapper) ontologyIRI -> BLOCKED_IMPORT_IRI);
		return manager;
	}

	public OWLAxiom deserialiseAxiom(String owlExpression, @Nullable String axiomIdentifier) throws OWLOntologyCreationException {
		if (!isAllowedAxiomExpression(owlExpression)) {
			throw new OWLOntologyCreationException("Unsupported axiom expression '" + leadingKeyword(owlExpression) + "'" + forAxiomId(axiomIdentifier));
		}
		synchronized (this) {
			try {
				long start = new Date().getTime();
				owlFunctionalSyntaxOWLParser.parse(new StringDocumentSource(ontologyDocStart + owlExpression + ontologyDocEnd), owlOntology, owlOntologyLoaderConfiguration);

				if (owlAxiomsLoaded.size() != 1) {
					throw new IllegalArgumentException("OWL Axiom string should contain a single Axiom" +
							" found " + owlAxiomsLoaded.size() + " for axiom id " + axiomIdentifier + " - '" + owlExpression + "'");
				}
				timeTakenDeserialisingAxioms += new Date().getTime() - start;

				return owlAxiomsLoaded.iterator().next();
			} catch (IOException e) {
				throw new OWLOntologyCreationException("Failed to parse axiom " + axiomIdentifier + ", '" + owlExpression + "'", e);
			} catch (OWLParserException e) {
				throw new OWLOntologyCreationException("Failed to parse axiom" + forAxiomId(axiomIdentifier) + ": " + e.getMessage(), e);
			} catch (OWLRuntimeException e) {
				// OWL API messages can carry fetched content, so keep them out of the thrown exception.
				logger.debug("Failed to parse axiom {}", axiomIdentifier, e);
				throw new OWLOntologyCreationException("Failed to parse axiom" + forAxiomId(axiomIdentifier));
			} finally {
				axiomsLoaded++;
				if (axiomsLoaded > 0 && axiomsLoaded % 10_000 == 0) {
					logger.info("Deserialised {} axioms...", String.format("%,8d", axiomsLoaded));
				}
				owlOntologyManager.removeAxioms(owlOntology, new HashSet<>(owlAxiomsLoaded));
				owlAxiomsLoaded.clear();
			}
		}
	}

	private static boolean isAllowedAxiomExpression(String owlExpression) {
		if (owlExpression == null) {
			return false;
		}
		String trimmed = owlExpression.stripLeading();
		return ALLOWED_AXIOM_PREFIXES.stream().anyMatch(trimmed::startsWith);
	}

	private static String leadingKeyword(String owlExpression) {
		if (owlExpression == null) {
			return "null";
		}
		String trimmed = owlExpression.strip();
		int end = 0;
		while (end < trimmed.length() && end < 50 && Character.isLetter(trimmed.charAt(end))) {
			end++;
		}
		return trimmed.substring(0, end);
	}

	private static String forAxiomId(@Nullable String axiomIdentifier) {
		return axiomIdentifier != null ? " for axiom id " + axiomIdentifier : "";
	}

	public long getTimeTakenDeserialisingAxioms() {
		return timeTakenDeserialisingAxioms;
	}

	public int getAxiomsLoaded() {
		return axiomsLoaded;
	}

	public void clearCounters() {
		this.timeTakenDeserialisingAxioms = 0;
		this.axiomsLoaded = 0;
	}
}
