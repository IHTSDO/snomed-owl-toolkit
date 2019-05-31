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

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.lang.Long.parseLong;

/**
 * Used to convert stated relationships within the international edition to a complete OWL Axiom reference set.
 */
public class StatedRelationshipToOwlRefsetService {

	private static final String OWL_AXIOM_REFSET_DELTA = "sct2_sRefset_OWLAxiomDelta_INT_";
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
					zipOutputStream.putNextEntry(new ZipEntry(OWL_AXIOM_REFSET_DELTA + effectiveDate + ".txt"));
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
	
	public void convertStatedRelationshipsToOwlReRefsetAndReconcileWithPublishedArchive(InputStream snomedRf2SnapshotArchive,
			InputStream snomedRf2CompleteOwlSnapshotArchive,
			OutputStream rf2DeltaZipResults,
			String effectiveDate) throws ConversionException, OWLOntologyCreationException, IOException {
		
		// Load required parts of RF2 into memory, copying existing owl axioms to output file
		logger.info("Loading RF2 files.");
		// Create delta results zip stream
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(rf2DeltaZipResults)) {
			AxiomCopier axiomCopier = new AxiomCopier(() -> {
				try {
					zipOutputStream.putNextEntry(new ZipEntry(OWL_AXIOM_REFSET_DELTA + effectiveDate + ".txt"));
					return new BufferedWriter(new OutputStreamWriter(zipOutputStream));
				} catch (IOException e) {
					logger.error("Failed to start OWL Axiom zip entry", e);
				}
				return null;
			});
			axiomCopier.setExtractEffectiveTime(effectiveDate);
			SnomedTaxonomy snomedTaxonomy = readSnomedTaxonomy(snomedRf2SnapshotArchive, new OptionalFileInputStream(null), axiomCopier, null);
			axiomCopier.complete();
			// Fetch attributes which are not grouped within the MRCM Attribute Domain International reference set.
			Set<Long> neverGroupedRoles = snomedTaxonomy.getUngroupedRolesForContentTypeOrDefault(parseLong(Concepts.ALL_PRECOORDINATED_CONTENT));
			OntologyService ontologyService = new OntologyService(neverGroupedRoles);
			
			AxiomChangesGenerator generator = new AxiomChangesGenerator();
			generator.generate(snomedTaxonomy, ontologyService, snomedRf2CompleteOwlSnapshotArchive);
			
			OWLOntology ontology = ontologyService.createOntology(snomedTaxonomy);
			convertAxiomsToReferenceSet(generator, ontologyService, ontology, zipOutputStream, snomedTaxonomy);
			zipOutputStream.closeEntry();
		}
	}

	private void convertAxiomsToReferenceSet(AxiomChangesGenerator generator,
			OntologyService ontologyService, OWLOntology ontology,
			ZipOutputStream zipOutputStream, SnomedTaxonomy snomedTaxonomy) throws OWLOntologyCreationException, ConversionException {
		
		convertAxiomsToReferenceSet(generator.getAxiomsIdMap(),
				generator.getChanges(),
				generator.getConceptsInactivated(),
				ontologyService,
				ontology,
				zipOutputStream,
				snomedTaxonomy);
		
	}

	private void convertAxiomsToReferenceSet(Map<OWLAxiom, String> axiomsIdMap,
			Map<Long, Set<OWLAxiom>> changes,
			Set<Long> inactiveConcepts,
			OntologyService ontologyService,
			OWLOntology ontology,
			OutputStream outputStream,
			SnomedTaxonomy snomedTaxonomy) throws OWLOntologyCreationException, ConversionException {
		
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

			for (Long conceptId : changes.keySet()) {
				for (OWLAxiom owlAxiom : changes.get(conceptId)) {
					// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	owlExpression

					if (axiomsIdMap == null || !axiomsIdMap.containsKey(owlAxiom)) {
						writer.write(generateIdentifier());
					} else {
						writer.write(axiomsIdMap.get(owlAxiom));
					}
					writer.write(TAB);

					// Blank effectiveTime
					writer.write(TAB);

					// inactivation
					if (inactiveConcepts != null && inactiveConcepts.contains(conceptId)) {
						writer.write("0");
					} else {
						writer.write("1");
					}
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

	SnomedTaxonomy readSnomedTaxonomy(InputStream snomedRf2SnapshotArchive, OptionalFileInputStream deltaStream,
			ComponentFactory publishedStatedRelationshipInactivator, ComponentFactory axiomDeltaCopier) throws ConversionException {

		try {
			return new SnomedTaxonomyBuilder().build(new InputStreamSet(snomedRf2SnapshotArchive), deltaStream.getInputStream().orElse(null),
					publishedStatedRelationshipInactivator, axiomDeltaCopier, false);
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
		convertAxiomsToReferenceSet(null, axiomsFromStatedRelationships, null, ontologyService, ontology, outputStream, snomedTaxonomy);
	}

	
	static class AxiomChangesGenerator {
		
		private Map<Long, Set<OWLAxiom>> changes = new HashMap<>();
		private Map<OWLAxiom, String> publishedAxiomsIdMap = new HashMap<>();
		private Set<Long> inactivatedConcepts = new HashSet<>();
		private final Logger logger = LoggerFactory.getLogger(getClass());

		public void generate(SnomedTaxonomy snomedTaxonomy, OntologyService ontologyService, InputStream snomedRf2CompleteOwlSnapshotArchive) throws ConversionException {
			//load published complete owl
			logger.info("Loading complete owl snapshot files ..");
			SnomedTaxonomy completeOwlTaxonomy = readSnomedTaxonomyWithoutRelationships(snomedRf2CompleteOwlSnapshotArchive);
			logger.info("Total concepts loaded with axioms:" + completeOwlTaxonomy.getConceptAxiomMap().keySet().size());
			
			for (String id : completeOwlTaxonomy.getAxiomsById().keySet()) {
				if (!snomedTaxonomy.getAxiomsById().containsKey(id)) {
					publishedAxiomsIdMap.put(completeOwlTaxonomy.getAxiomsById().get(id), id);
				}
			}
						
			//convert stated relationships
			Map<Long, Set<OWLAxiom>> axiomsFromStatedRelationships = ontologyService.createAxiomsFromStatedRelationships(snomedTaxonomy);
			int newlyAdded = 0;
			int modifiedTotal = 0;
			int inactivation = 0;
			for (Long conceptId : axiomsFromStatedRelationships.keySet()) {
				
				if (completeOwlTaxonomy.getConceptAxiomMap().keySet().contains(conceptId)) {
					//check any changes
					OWLAxiom modified = findChanges(publishedAxiomsIdMap, completeOwlTaxonomy.getConceptAxiomMap().get(conceptId),
														axiomsFromStatedRelationships.get(conceptId));
					if (modified != null) {
						logger.debug("Axiom modified for concept " + conceptId);
						changes.putIfAbsent(conceptId, new HashSet<>());
						changes.get(conceptId).add(modified);
						modifiedTotal++;
					}
				} else {
					// axioms for new concept
					logger.debug("Axioms added for new concept " + conceptId);
					changes.putIfAbsent(conceptId, new HashSet<>());
					changes.get(conceptId).addAll(axiomsFromStatedRelationships.get(conceptId));
					newlyAdded += axiomsFromStatedRelationships.get(conceptId).size();
				}
			}
			
			//inactivation
			inactivatedConcepts = new LongOpenHashSet(snomedTaxonomy.getInactivatedConcepts());
			inactivatedConcepts.removeAll(completeOwlTaxonomy.getInactivatedConcepts());
			for (Long conceptId : inactivatedConcepts) {
				changes.put(conceptId, completeOwlTaxonomy.getConceptAxiomMap().get(conceptId));
				inactivation += completeOwlTaxonomy.getConceptAxiomMap().get(conceptId).size();
			}
			logger.info("Concepts inactivated:" + inactivatedConcepts.size());

			//re-activation
			Set<Long> reActivations = new LongOpenHashSet(completeOwlTaxonomy.getInactivatedConcepts());
			reActivations.removeAll(snomedTaxonomy.getInactivatedConcepts());
			
			logger.info("Concepts re-activated:" + reActivations.size());
			
			for (Long conceptId : reActivations) {
				if (completeOwlTaxonomy.getConceptAxiomMap().containsKey(conceptId)) {
					changes.put(conceptId, completeOwlTaxonomy.getConceptAxiomMap().get(conceptId));
				}
			}
			
			logger.info("Total changes:" + changes.values().size());
			logger.info("Modified changes:" + modifiedTotal + " added for new concept changes:" + newlyAdded);
			logger.info("Inactivation changes:" + inactivation);
		}
		
		private SnomedTaxonomy readSnomedTaxonomyWithoutRelationships(InputStream snomedRf2CompleteOwlSnapshotArchive) throws ConversionException{
			try {
				return new SnomedTaxonomyBuilder().buildWithAxiomRefset(new InputStreamSet(snomedRf2CompleteOwlSnapshotArchive));
			} catch (ReleaseImportException e) {
				throw new ConversionException("Failed to load RF2 archive.", e);
			}
		}
		
		public Set<Long> getConceptsInactivated() {
			return this.inactivatedConcepts;
		}

		public Map<Long, Set<OWLAxiom>> getChanges() {
			return this.changes;
		}

		public Map<OWLAxiom, String> getAxiomsIdMap() {
			return this.publishedAxiomsIdMap;
		}

		OWLAxiom findChanges(Map<OWLAxiom, String> owlAxiomIdMap, Set<OWLAxiom> previous, Set<OWLAxiom> currentFromStated) {
			Set<OWLAxiom> previousAxiomFromStated = previous
					.stream()
					.filter(a -> owlAxiomIdMap.containsKey(a))
					.collect(Collectors.toSet());
			if (previousAxiomFromStated.isEmpty()) {
				//newly added
				return currentFromStated.iterator().next();
			} else {
				if (!previousAxiomFromStated.iterator().next().equals(currentFromStated.iterator().next())) {
					//modified
					owlAxiomIdMap.put(currentFromStated.iterator().next(), owlAxiomIdMap.get(previousAxiomFromStated.iterator().next()));
					return currentFromStated.iterator().next();
				}
			}
			return null;
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
		private String effectiveTime;

		AxiomCopier(Supplier<BufferedWriter> startFunction) {
			exceptionsThrown = new ArrayList<>();
			this.startFunction = startFunction;
		}
		
		

		public void setExtractEffectiveTime(String effectiveTime) {
			this.effectiveTime = effectiveTime;
		}

		@Override
		public void newReferenceSetMemberState(String[] fieldNames, String id, String effectiveTime, String active, String moduleId, String refsetId, String referencedComponentId, String... otherValues) {
			// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	owlExpression
			if (refsetId.equals(Concepts.OWL_AXIOM_REFERENCE_SET) && (this.effectiveTime == null || this.effectiveTime.equals(effectiveTime))) {
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
