package org.snomed.otf.owltoolkit.conversion;

import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.ontology.OntologyService;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomyBuilder;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Set;

import static org.snomed.otf.owltoolkit.ontology.OntologyService.CORE_COMPONENT_NAMESPACE_PATTERN;

public class RF2ToOWLService {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public void convertRF2ArchiveToOWL(String versionDate, InputStream rf2ArchiveStream, OutputStream owlFileOutputStream) throws ConversionException {
		// Load required parts of RF2 into memory
		logger.info("Loading RF2 files");
		SnomedTaxonomy snomedTaxonomy;
		try {
			snomedTaxonomy = new SnomedTaxonomyBuilder().build(rf2ArchiveStream);
		} catch (ReleaseImportException e) {
			throw new ConversionException("Failed to load RF2 archive.", e);
		}

		// Fetch attributes which are not grouped within the MRCM Attribute Domain International reference set.
		Set<Long> neverGroupedRoles = snomedTaxonomy.getUngroupedRolesForContentType(Long.parseLong(Concepts.ALL_PRECOORDINATED_CONTENT));

		// Create OWL Ontology from stated relationships and OWL Axiom reference set
		// using list of never grouped roles during relationship to axiom conversion
		logger.info("Building Ontology");
		OntologyService ontologyService = new OntologyService(neverGroupedRoles);
		OWLOntology ontology;
		try {
			ontology = ontologyService.createOntology(snomedTaxonomy, versionDate);
		} catch (OWLOntologyCreationException e) {
			throw new ConversionException("Failed to build OWL Ontology from SNOMED taxonomy.", e);
		}

		// Write to OutputStream
		try {
			ontologyService.saveOntology(ontology, owlFileOutputStream);
		} catch (OWLOntologyStorageException e) {
			throw new ConversionException("Failed to serialise and write OWL Ontology to output stream.", e);
		}

		logger.info("RF2 to OWL Ontology conversion complete.");
	}

}
