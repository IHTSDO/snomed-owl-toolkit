package org.snomed.otf.owltoolkit.conversion;

import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.semanticweb.owlapi.functional.renderer.FunctionalSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.constants.RF2Headers;
import org.snomed.otf.owltoolkit.domain.Relationship;
import org.snomed.otf.owltoolkit.ontology.OntologyService;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomyBuilder;
import org.snomed.otf.owltoolkit.util.InputStreamSet;
import org.snomed.otf.owltoolkit.util.OptionalFileInputStream;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Used to convert stated relationships within the international edition to a complete OWL Axiom reference set.
 */
public class StatedRelationshipToOwlRefsetService {

	private Supplier<String> identifierSupplier = () -> UUID.randomUUID().toString();
	private static final String TAB = "\t";
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public void convertStatedRelationshipsToOwlRefset(InputStream snomedRf2SnapshotArchive, OptionalFileInputStream deltaStream,
			OutputStream additionalOwlReferenceSetOutput) throws ConversionException, OWLOntologyCreationException {

		// Load required parts of RF2 into memory
		logger.info("Loading RF2 files");
		SnomedTaxonomy snomedTaxonomy;
		try {
			snomedTaxonomy = new SnomedTaxonomyBuilder().build(new InputStreamSet(snomedRf2SnapshotArchive), deltaStream.getInputStream().orElse(null), false);
		} catch (ReleaseImportException e) {
			throw new ConversionException("Failed to load RF2 archive.", e);
		}

		// Fetch attributes which are not grouped within the MRCM Attribute Domain International reference set.
		Set<Long> neverGroupedRoles = snomedTaxonomy.getUngroupedRolesForContentTypeOrDefault(Long.parseLong(Concepts.ALL_PRECOORDINATED_CONTENT));

		OntologyService ontologyService = new OntologyService(neverGroupedRoles);
		OWLOntology ontology = ontologyService.createOntology(snomedTaxonomy);

		Map<Long, Set<OWLAxiom>> axiomsFromStatedRelationships = ontologyService.createAxiomsFromStatedRelationships(snomedTaxonomy);

		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(additionalOwlReferenceSetOutput))) {

			writer.write(RF2Headers.OWL_EXPRESSION_REFERENCE_SET_HEADER);
			writer.newLine();

			ByteArrayOutputStream functionalSyntaxOutputStream = new ByteArrayOutputStream();
			OutputStreamWriter functionalSyntaxWriter = new OutputStreamWriter(functionalSyntaxOutputStream, Charset.forName("UTF-8"));
			FunctionalSyntaxObjectRenderer functionalSyntaxObjectRenderer = new FunctionalSyntaxObjectRenderer(ontology, functionalSyntaxWriter);
			functionalSyntaxObjectRenderer.setPrefixManager(ontologyService.getSnomedPrefixManager());

			for (Long conceptId : axiomsFromStatedRelationships.keySet()) {

				for (OWLAxiom owlAxiom : axiomsFromStatedRelationships.get(conceptId)) {
					// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	owlExpression

					// Random ID
					writer.write(generateIdentifier());
					writer.write(TAB);

					// Blank effectiveTime
					writer.write(TAB);

					// Active
					writer.write("1");
					writer.write(TAB);

					// Module - take from a stated relationship
					Iterator<Relationship> statedRelationshipIterator = snomedTaxonomy.getStatedRelationships(conceptId).iterator();
					writer.write(statedRelationshipIterator.hasNext() ? statedRelationshipIterator.next().getModuleId() + "" : Concepts.SNOMED_CT_MODEL_COMPONENT_MODULE);
					writer.write(TAB);

					// RefsetId
					writer.write(Concepts.OWL_AXIOM_REFERENCE_SET);
					writer.write(TAB);

					// referencedComponentId
					writer.write(conceptId + "");
					writer.write(TAB);

					// OWL Expression
					// Render Axiom
					owlAxiom.accept(functionalSyntaxObjectRenderer);
					functionalSyntaxWriter.flush();
					// Write axiom string to file
					writer.write(functionalSyntaxOutputStream.toString());
					functionalSyntaxOutputStream.reset();
					writer.newLine();
				}
			}
		} catch (IOException e) {
			throw new ConversionException("Failed to write to OWL Reference Set output file.", e);
		}
	}

	private String generateIdentifier() {
		return identifierSupplier.get();
	}

	public void setIdentifierSupplier(Supplier<String> identifierSupplier) {
		this.identifierSupplier = identifierSupplier;
	}
}
