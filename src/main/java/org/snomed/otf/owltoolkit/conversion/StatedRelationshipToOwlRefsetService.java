package org.snomed.otf.owltoolkit.conversion;

import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.factory.ComponentFactory;
import org.ihtsdo.otf.snomedboot.factory.ImpotentComponentFactory;
import org.semanticweb.owlapi.functional.renderer.FunctionalSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.constants.RF2Headers;
import org.snomed.otf.owltoolkit.ontology.OntologyService;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomyBuilder;
import org.snomed.otf.owltoolkit.util.InputStreamSet;
import org.snomed.otf.owltoolkit.util.OptionalFileInputStream;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.lang.Long.parseLong;

/**
 * Used to convert stated relationships within the international edition to a complete OWL Axiom reference set.
 */
public class StatedRelationshipToOwlRefsetService {

	private Supplier<String> identifierSupplier = () -> UUID.randomUUID().toString();
	private static final String TAB = "\t";
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public void convertStatedRelationshipsToOwlRefsetAndInactiveRelationshipsArchive(InputStream snomedRf2SnapshotArchive, OptionalFileInputStream deltaStream,
			OutputStream rf2DeltaZipResults, String effectiveDate) throws ConversionException, OWLOntologyCreationException, IOException {

		// Create zip stream
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(rf2DeltaZipResults)) {

			// Load required parts of RF2 into memory, copying existing owl axioms to output file
			logger.info("Loading RF2 files");

			// Write inactive stated relationships to output zip during snapshot loading
			zipOutputStream.putNextEntry(new ZipEntry("sct2_StatedRelationship_Delta_INT_" + effectiveDate + ".txt"));
			PublishedStatedRelationshipInactivator publishedStatedRelationshipInactivator = new PublishedStatedRelationshipInactivator(zipOutputStream);

			// Write existing axioms to output zip during delta loading
			AxiomCopier axiomCopier = new AxiomCopier(() -> {
				try {
					publishedStatedRelationshipInactivator.complete();
					zipOutputStream.closeEntry();
					zipOutputStream.putNextEntry(new ZipEntry("sct2_sRefset_OWLAxiomDelta_INT_" + effectiveDate + ".txt"));
					return new BufferedWriter(new OutputStreamWriter(zipOutputStream));
				} catch (IOException e) {
					logger.error("Failed to start OWL Axiom zip entry", e);
				}
				return null;
			});
			SnomedTaxonomy snomedTaxonomy = readSnomedTaxonomy(snomedRf2SnapshotArchive, deltaStream, publishedStatedRelationshipInactivator, axiomCopier);
			axiomCopier.complete();

			convertStatedRelationshipsToOwlRefset(snomedTaxonomy, zipOutputStream);
			zipOutputStream.closeEntry();
		}
	}

	SnomedTaxonomy readSnomedTaxonomy(InputStream snomedRf2SnapshotArchive, OptionalFileInputStream deltaStream,
			ComponentFactory publishedStatedRelationshipInactivator, ComponentFactory axiomDeltaCopier) throws ConversionException {

		try {
			return new SnomedTaxonomyBuilder().build(new InputStreamSet(snomedRf2SnapshotArchive), deltaStream.getInputStream().orElse(null),
					publishedStatedRelationshipInactivator, axiomDeltaCopier);
		} catch (ReleaseImportException e) {
			throw new ConversionException("Failed to load RF2 archive.", e);
		}
	}

	void convertStatedRelationshipsToOwlRefset(SnomedTaxonomy snomedTaxonomy, OutputStream outputStream) throws OWLOntologyCreationException, ConversionException {

		// Fetch attributes which are not grouped within the MRCM Attribute Domain International reference set.
		Set<Long> neverGroupedRoles = snomedTaxonomy.getUngroupedRolesForContentTypeOrDefault(parseLong(Concepts.ALL_PRECOORDINATED_CONTENT));

		OntologyService ontologyService = new OntologyService(neverGroupedRoles);
		OWLOntology ontology = ontologyService.createOntology(snomedTaxonomy);

		Map<Long, Set<OWLAxiom>> axiomsFromStatedRelationships = ontologyService.createAxiomsFromStatedRelationships(snomedTaxonomy);

		try {
			// Leave stream open so other entries can be written when used as a zip stream
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

			ByteArrayOutputStream functionalSyntaxOutputStream = new ByteArrayOutputStream();
			OutputStreamWriter functionalSyntaxWriter = new OutputStreamWriter(functionalSyntaxOutputStream, Charset.forName("UTF-8"));
			FunctionalSyntaxObjectRenderer functionalSyntaxObjectRenderer = new FunctionalSyntaxObjectRenderer(ontology, functionalSyntaxWriter);
			functionalSyntaxObjectRenderer.setPrefixManager(ontologyService.getSnomedPrefixManager());

			Set<Long> modelComponentIds = snomedTaxonomy.getDescendants(parseLong(Concepts.SNOMED_CT_MODEL_COMPONENT));
			modelComponentIds.add(parseLong(Concepts.SNOMED_CT_MODEL_COMPONENT));
			modelComponentIds.add(Concepts.ROOT_LONG);

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

					// Module
					writer.write(modelComponentIds.contains(conceptId) ? Concepts.SNOMED_CT_MODEL_COMPONENT_MODULE : Concepts.SNOMED_CT_CORE_MODULE);
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
			writer.flush();
		} catch (IOException e) {
			throw new ConversionException("Failed to write to OWL Reference Set output file.", e);
		}
	}

	private static class PublishedStatedRelationshipInactivator extends ImpotentComponentFactory {

		private final BufferedWriter writer;
		private final List<IOException> exceptionsThrown;

		PublishedStatedRelationshipInactivator(ZipOutputStream zipOutputStream) throws IOException {
			writer = new BufferedWriter(new OutputStreamWriter(zipOutputStream));
			writer.write(RF2Headers.RELATIONSHIP_HEADER);
			writer.newLine();
			exceptionsThrown = new ArrayList<>();
		}

		@Override
		public void newRelationshipState(String id, String effectiveTime, String active, String moduleId, String sourceId, String destinationId, String relationshipGroup, String typeId, String characteristicTypeId, String modifierId) {
			if (active.equals("1") && characteristicTypeId.equals(Concepts.STATED_RELATIONSHIP)) {
				// Make active stated relationship inactive
				try {
					// id
					writer.write(id);
					writer.write(TAB);

					// effectiveTime
					writer.write(TAB);

					// active
					writer.write("0");
					writer.write(TAB);

					// moduleId
					writer.write(moduleId);
					writer.write(TAB);

					// sourceId
					writer.write(sourceId);
					writer.write(TAB);

					// destinationId
					writer.write(destinationId);
					writer.write(TAB);

					// relationshipGroup
					writer.write(relationshipGroup);
					writer.write(TAB);

					// typeId
					writer.write(typeId);
					writer.write(TAB);

					// characteristicTypeId
					writer.write(Concepts.STATED_RELATIONSHIP);
					writer.write(TAB);

					// modifierId
					writer.write("900000000000451002");
					writer.newLine();
				} catch (IOException e) {
					exceptionsThrown.add(e);
				}
			}
		}

		public void complete() throws IOException {
			writer.flush();
			if (!exceptionsThrown.isEmpty()) {
				throw exceptionsThrown.get(0);
			}
		}
	}

	private static class AxiomCopier extends ImpotentComponentFactory {

		private final Supplier<BufferedWriter> startFunction;
		private boolean entryStarted;
		private BufferedWriter writer;
		private final List<IOException> exceptionsThrown;

		AxiomCopier(Supplier<BufferedWriter> startFunction) {
			exceptionsThrown = new ArrayList<>();
			this.startFunction = startFunction;
		}

		@Override
		public void newReferenceSetMemberState(String[] fieldNames, String id, String effectiveTime, String active, String moduleId, String refsetId, String referencedComponentId, String... otherValues) {
			// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	owlExpression
			if (refsetId.equals(Concepts.OWL_AXIOM_REFERENCE_SET)) {
				try {
					startEntry();
					writer.write(id);
					writer.write(TAB);
					writer.write(effectiveTime);
					writer.write(TAB);
					writer.write(active);
					writer.write(TAB);
					writer.write(moduleId);
					writer.write(TAB);
					writer.write(refsetId);
					writer.write(TAB);
					writer.write(referencedComponentId);
					writer.write(TAB);
					writer.write(otherValues[0]);
					writer.newLine();
				} catch (IOException e) {
					exceptionsThrown.add(e);
				}
			}
		}

		private void startEntry() throws IOException {
			if (!entryStarted) {
				writer = startFunction.get();
				writer.write(RF2Headers.OWL_EXPRESSION_REFERENCE_SET_HEADER);
				writer.newLine();
				entryStarted = true;
			}
		}

		private void complete() throws IOException {
			startEntry();
			writer.flush();
			// Let exceptions from component factory bubble up
			if (!exceptionsThrown.isEmpty()) {
				throw exceptionsThrown.get(0);
			}
		}
	}

	private String generateIdentifier() {
		return identifierSupplier.get();
	}

	public void setIdentifierSupplier(Supplier<String> identifierSupplier) {
		this.identifierSupplier = identifierSupplier;
	}
}
