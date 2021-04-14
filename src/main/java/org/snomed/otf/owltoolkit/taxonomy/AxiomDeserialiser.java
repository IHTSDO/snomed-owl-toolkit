package org.snomed.otf.owltoolkit.taxonomy;

import org.semanticweb.owlapi.OWLAPIConfigProvider;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.functional.parser.OWLFunctionalSyntaxOWLParser;
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

	private final OWLOntology owlOntology;
	private final OWLOntologyLoaderConfiguration owlOntologyLoaderConfiguration;
	private final OWLFunctionalSyntaxOWLParser owlFunctionalSyntaxOWLParser;
	private final List<OWLAxiom> owlAxiomsLoaded = new ArrayList<>();
	private int axiomsLoaded = 0;
	private long timeTakenDeserialisingAxioms;
	private final OWLOntologyManager owlOntologyManager;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	AxiomDeserialiser() {
		owlOntologyManager = OWLManager.createOWLOntologyManager();
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

	public OWLAxiom deserialiseAxiom(String owlExpression, @Nullable String axiomIdentifier) throws OWLOntologyCreationException {
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
