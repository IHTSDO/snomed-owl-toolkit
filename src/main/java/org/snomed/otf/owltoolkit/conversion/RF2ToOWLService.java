package org.snomed.otf.owltoolkit.conversion;

import com.google.common.collect.Sets;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.ontology.OntologyService;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomyBuilder;
import org.snomed.otf.owltoolkit.util.InputStreamSet;
import org.snomed.otf.owltoolkit.util.OptionalFileInputStream;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class RF2ToOWLService {

	private static final Set<String> DEFAULT_NAMESPACES = Sets.newHashSet(
			"Prefix(xml:=<http://www.w3.org/XML/1998/namespace>)",
			"Prefix(xsd:=<http://www.w3.org/2001/XMLSchema#>)",
			"Prefix(owl:=<http://www.w3.org/2002/07/owl#>)",
			"Prefix(rdf:=<http://www.w3.org/1999/02/22-rdf-syntax-ns#>)",
			"Prefix(rdfs:=<http://www.w3.org/2000/01/rdf-schema#>)",
			"Prefix(:=<http://snomed.info/id/>)"
	);

	private final Logger logger = LoggerFactory.getLogger(getClass());
	public static final String HEADER_PREFIX = "Ontology(<";
	public static final String HEADER_SUFFIX = ">)";

	public void convertRF2ArchiveToOWL(String ontologyUriOverride, String versionDate, boolean includeFSNs, InputStreamSet snomedRf2SnapshotArchives,
			OptionalFileInputStream deltaStream, OutputStream owlFileOutputStream) throws ConversionException {

		// Load required parts of RF2 into memory
		logger.info("Loading RF2 files");
		SnomedTaxonomy snomedTaxonomy;
		try {
			snomedTaxonomy = new SnomedTaxonomyBuilder().build(snomedRf2SnapshotArchives, deltaStream.getInputStream().orElse(null), includeFSNs);
		} catch (ReleaseImportException e) {
			throw new ConversionException("Failed to load RF2 archive.", e);
		}

		String ontologyUri;
		if (ontologyUriOverride != null && !ontologyUriOverride.isEmpty()) {
			ontologyUri = ontologyUriOverride;
		} else {
			Collection<String> ontologyHeaders = snomedTaxonomy.getOntologyHeader().values();
			String ontologyHeader;
			if (ontologyHeaders.size() > 1) {
				throw new ConversionException("Multiple active Ontology identifiers found. " +
						"An extension should make other Ontology identifier records inactive when adding its own. " + ontologyHeaders.toString());
			} else if (ontologyHeaders.isEmpty()) {
				logger.warn("No Ontology identifier found. Using default identifier {}", OntologyService.SNOMED_INTERNATIONAL_EDITION_URI);
				ontologyHeader = HEADER_PREFIX + OntologyService.SNOMED_INTERNATIONAL_EDITION_URI + HEADER_SUFFIX;
			} else {
				ontologyHeader = ontologyHeaders.iterator().next();
			}
			if (!ontologyHeader.startsWith(HEADER_PREFIX) || !ontologyHeader.endsWith(HEADER_SUFFIX)) {
				throw new ConversionException(String.format("Ontology header should start with '%s' and end with '%s' but this found '%s'", HEADER_PREFIX, HEADER_SUFFIX, ontologyHeader));
			}
			ontologyUri = ontologyHeader.substring(HEADER_PREFIX.length(), ontologyHeader.length() - 2);
		}

		// Fetch attributes which are not grouped within the MRCM Attribute Domain International reference set.
		Set<Long> neverGroupedRoles = snomedTaxonomy.getUngroupedRolesForContentTypeOrDefault(Long.parseLong(Concepts.ALL_PRECOORDINATED_CONTENT));

		// Create OWL Ontology from stated relationships and OWL Axiom reference set
		// using list of never grouped roles during relationship to axiom conversion
		logger.info("Building Ontology");
		OntologyService ontologyService = new OntologyService(neverGroupedRoles);
		OWLOntology ontology;
		try {
			ontology = ontologyService.createOntology(snomedTaxonomy, ontologyUri, versionDate);
		} catch (OWLOntologyCreationException e) {
			throw new ConversionException("Failed to build OWL Ontology from SNOMED taxonomy.", e);
		}

		// Write to any non-default namespaces to OutputStream
		Set<String> extraOntologyNamespaces = snomedTaxonomy.getOntologyNamespaces().values().stream()
				.filter(namespace -> !DEFAULT_NAMESPACES.contains(namespace)).collect(Collectors.toSet());
		if (!extraOntologyNamespaces.isEmpty()) {
			try {
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(owlFileOutputStream));
				writer.write(getCopyrightNotice());
				writer.newLine();
				for (String extraOntologyNamespace : extraOntologyNamespaces) {
					writer.write(extraOntologyNamespace);
					writer.newLine();
				}
				writer.flush();
			} catch (IOException e) {
				throw new ConversionException("Failed to write ontology namespaces to output stream.", e);
			}
		}

		// Write ontology to OutputStream
		try {
			ontologyService.saveOntology(ontology, owlFileOutputStream);
		} catch (OWLOntologyStorageException e) {
			throw new ConversionException("Failed to serialise and write OWL Ontology to output stream.", e);
		}

		logger.info("RF2 to OWL Ontology conversion complete.");
	}

	protected String getCopyrightNotice() throws IOException {
		return FileCopyUtils.copyToString(new InputStreamReader(getClass().getResourceAsStream("/owl-file-copyright-notice.txt")));
	}

}
