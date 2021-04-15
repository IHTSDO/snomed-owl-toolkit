/*
 * Copyright 2020 SNOMED International, http://snomed.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.snomed.otf.owltoolkit.service;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.classification.ReasonerTaxonomy;
import org.snomed.otf.owltoolkit.classification.ReasonerTaxonomyWalker;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.conversion.AxiomRelationshipConversionService;
import org.snomed.otf.owltoolkit.conversion.ConversionException;
import org.snomed.otf.owltoolkit.domain.AxiomRepresentation;
import org.snomed.otf.owltoolkit.domain.Relationship;
import org.snomed.otf.owltoolkit.normalform.RelationshipChangeProcessor;
import org.snomed.otf.owltoolkit.normalform.RelationshipInactivationProcessor;
import org.snomed.otf.owltoolkit.normalform.RelationshipNormalFormGenerator;
import org.snomed.otf.owltoolkit.ontology.AxiomChangeSet;
import org.snomed.otf.owltoolkit.ontology.OntologyDebugUtil;
import org.snomed.otf.owltoolkit.ontology.OntologyService;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomyBuilder;
import org.snomed.otf.owltoolkit.util.InputStreamSet;
import org.snomed.otf.owltoolkit.util.OptionalFileInputStream;
import org.snomed.otf.owltoolkit.util.TimerUtil;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;

public class SnomedReasonerService {

	public static final String ELK_REASONER_FACTORY = "org.semanticweb.elk.owlapi.ElkReasonerFactory";

	private final ClassificationResultsWriter classificationResultsWriter;

	private final SnomedTaxonomyBuilder snomedTaxonomyBuilder = new SnomedTaxonomyBuilder();

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final Comparator<Relationship> RELATIONSHIP_COMPARATOR_RECENT_CHANGE_FIRST = Comparator
			.comparing(Relationship::getTypeId)
			.thenComparing(Relationship::getDestinationId)
			.thenComparing(Relationship::getValueAsString, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(Relationship::getGroup)
			.thenComparing(Relationship::getEffectiveTime, Comparator.reverseOrder());

	public SnomedReasonerService() {
		this.classificationResultsWriter = new ClassificationResultsWriter();
	}

	public ClassificationContainer classify(String classificationId,
			File previousReleaseRf2SnapshotArchiveFiles,
			File currentReleaseRf2DeltaArchiveFile,
			File resultsRf2DeltaArchiveFile,
			String reasonerFactoryClassName,
			boolean outputOntologyFileForDebug) throws ReasonerServiceException {

		return classify(classificationId,
				Collections.singleton(previousReleaseRf2SnapshotArchiveFiles),
				currentReleaseRf2DeltaArchiveFile,
				resultsRf2DeltaArchiveFile,
				reasonerFactoryClassName,
				outputOntologyFileForDebug
		);
	}

	public ClassificationContainer classify(String classificationId,
			Set<File> previousReleaseRf2SnapshotArchiveFile,
			File currentReleaseRf2DeltaArchiveFile,
			File resultsRf2DeltaArchiveFile,
			String reasonerFactoryClassName,
			boolean outputOntologyFileForDebug) throws ReasonerServiceException {

		try (InputStreamSet previousReleaseRf2SnapshotArchives = new InputStreamSet(previousReleaseRf2SnapshotArchiveFile);
			 OptionalFileInputStream currentReleaseRf2DeltaArchive = new OptionalFileInputStream(currentReleaseRf2DeltaArchiveFile);
			 OutputStream resultsRf2DeltaArchive = new FileOutputStream(resultsRf2DeltaArchiveFile)) {

			return classify(classificationId,
					previousReleaseRf2SnapshotArchives,
					currentReleaseRf2DeltaArchive.getInputStream().orElse(null),
					resultsRf2DeltaArchive,
					reasonerFactoryClassName,
					outputOntologyFileForDebug);
		} catch (IOException e) {
			throw new ReasonerServiceException("IO error handling input/output files.", e);
		}
	}

	public ClassificationContainer classify(String classificationId,
			InputStreamSet previousReleaseRf2SnapshotArchives,
			InputStream currentReleaseRf2DeltaArchive,
			OutputStream resultsRf2DeltaArchive,
			String reasonerFactoryClassName,
			boolean outputOntologyFileForDebug) throws ReasonerServiceException {

		Date startDate = new Date();
		TimerUtil timer = new TimerUtil("Classification");
		logger.info("Checking requested reasoner is available");
		OWLReasonerFactory reasonerFactory = getOWLReasonerFactory(reasonerFactoryClassName);
		timer.checkpoint("Create reasoner factory");

		logger.info("Building snomedTaxonomy");
		SnomedTaxonomy snomedTaxonomy;
		try {
			snomedTaxonomy = snomedTaxonomyBuilder.build(previousReleaseRf2SnapshotArchives, currentReleaseRf2DeltaArchive, false);
		} catch (ReleaseImportException e) {
			throw new ReasonerServiceException("Failed to build existing taxonomy.", e);
		}
		timer.checkpoint("Build existing taxonomy");

		logger.info("Creating OwlOntology");
		Set<Long> ungroupedRoles = snomedTaxonomy.getUngroupedRolesForContentTypeOrDefault(parseLong(Concepts.ALL_PRECOORDINATED_CONTENT));
		OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
		OntologyService ontologyService = new OntologyService(ungroupedRoles, ontologyManager);
		OWLOntology owlOntology;
		try {
			owlOntology = ontologyService.createOntology(snomedTaxonomy);
		} catch (OWLOntologyCreationException e) {
			throw new ReasonerServiceException("Failed to build OWL Ontology.", e);
		}
		timer.checkpoint("Create OWL Ontology");

		if (outputOntologyFileForDebug) {
			OntologyDebugUtil.serialiseOntologyForDebug(classificationId, owlOntology);
			timer.checkpoint("Serialising OWL Ontology to disk for debug");
		}

		logger.info("Creating OwlReasoner");
		final OWLReasonerConfiguration configuration = new SimpleConfiguration(new ConsoleProgressMonitor());
		OWLReasoner reasoner = reasonerFactory.createReasoner(owlOntology, configuration);
		timer.checkpoint("Create reasoner");

		logger.info("OwlReasoner inferring class hierarchy");
		reasoner.flush();
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		timer.checkpoint("Inference computation");

		logger.info("Extract ReasonerTaxonomy");
		ReasonerTaxonomyWalker walker = new ReasonerTaxonomyWalker(reasoner, new ReasonerTaxonomy());
		ReasonerTaxonomy reasonerTaxonomy = walker.walk();
		timer.checkpoint("Extract ReasonerTaxonomy");

		logger.info("Generate normal form");
		AxiomRelationshipConversionService axiomRelationshipConversionService = new AxiomRelationshipConversionService(ungroupedRoles);
		Map<Long, Set<AxiomRepresentation>> conceptAxiomStatementMap;
		try {
			conceptAxiomStatementMap = axiomRelationshipConversionService.convertAxiomsToRelationships(snomedTaxonomy.getConceptAxiomMap(), true);
		} catch (ConversionException e) {
			throw new ReasonerServiceException("Failed to convert OWL Axiom Expressions into relationships for normal form generation.", e);
		}
		RelationshipNormalFormGenerator normalFormGenerator =
				new RelationshipNormalFormGenerator(reasonerTaxonomy, snomedTaxonomy, conceptAxiomStatementMap, ontologyService.getPropertyChains(owlOntology));

		RelationshipChangeProcessor changeCollector = normalFormGenerator.collectNormalFormChanges(null);
		timer.checkpoint("Generate normal form");

		logger.info("Inactivating inferred relationships for new inactive concepts");
		new RelationshipInactivationProcessor(snomedTaxonomy).processInactivationChanges(changeCollector);

		// Restore inactive relationships where appropriate
		for (Long conceptId : changeCollector.getAddedStatements().keySet()) {
			Set<Relationship> conceptInactiveInferredRelationship = snomedTaxonomy.getInactiveInferredRelationships(conceptId);
			Set<Relationship> newInferredRelationship = changeCollector.getAddedStatements().get(conceptId);

			if(!conceptInactiveInferredRelationship.isEmpty() && !newInferredRelationship.isEmpty()) {
				// sort them by the most recent changes first.
				// Today's date is used if the effective time is null or empty when loaded by SnomedTaxonomyLoader
				List<Relationship> inactiveSortedByEffectiveTime = new ArrayList<>(conceptInactiveInferredRelationship);
				Collections.sort(inactiveSortedByEffectiveTime, RELATIONSHIP_COMPARATOR_RECENT_CHANGE_FIRST);

				for (Relationship newRel : newInferredRelationship) {
					if (newRel.getRelationshipId() == -1) {// If we are updating an existing relationship then no need to find another one
						for (Relationship inactiveRel : inactiveSortedByEffectiveTime) {
							if (newRel.getGroup() == inactiveRel.getGroup()
									&& newRel.getTypeId() == inactiveRel.getTypeId()
									&& newRel.getDestinationId() == inactiveRel.getDestinationId()) {
								newRel.setRelationshipId(inactiveRel.getRelationshipId());
								// only need to find one to prevent inferred relationships churning
								break;
							}
						}
					}
				}
			}
		}

		long redundantCount = changeCollector.getRedundantCount();
		long totalChanges = changeCollector.getAddedCount() + changeCollector.getUpdatedCount() + redundantCount + changeCollector.getRemovedDueToConceptInactivationCount();
		logger.info("{} relationship rows changed: {} added, {} updated, {} redundant, {} removed due to concept inactivation.",
				formatDecimal(totalChanges), formatDecimal(changeCollector.getAddedCount()), formatDecimal(changeCollector.getUpdatedCount()),
				formatDecimal(redundantCount), formatDecimal(changeCollector.getRemovedDueToConceptInactivationCount()));

		logger.info("Writing results archive");
		classificationResultsWriter.writeResultsRf2Archive(changeCollector, reasonerTaxonomy.getEquivalentConceptIds(), resultsRf2DeltaArchive, startDate);
		timer.checkpoint("Write results to disk");
		timer.finish();

		ClassificationContainer container = new ClassificationContainer();
		container.setSnomedTaxonomy(snomedTaxonomy);
		container.setUngroupedRoles(ungroupedRoles);
		container.setOwlOntologyManager(ontologyManager);
		container.setReasoner(reasoner);
		container.setReasonerTaxonomy(reasonerTaxonomy);
		container.setNormalFormGenerator(normalFormGenerator);
		return container;
	}

	public void updateClassification(ClassificationContainer container, InputStream deltaZipInputStream, OutputStream resultsRf2DeltaArchive) throws ReasonerServiceException {
		TimerUtil timer = new TimerUtil("Update Classification");

		// TODO:
		// - Detect changes to properties and chains then recompute from scratch? Can reasoner/OWLAPI help detect these?
		// - Clear list of Equivalent concepts and check them again?
		// - Do something with ReasonerTaxonomy unsatisfiableConceptIds. Throw exception or return to terminology server..

		// Delta zip contains:
		// - any stated changes since the last classification, including axiom additions, changes or !deletions!
		// - any saved inferred relationship changes .. inferred relationship changes from the last classification may or may not have been accepted

		// Scope for EXP demo:
		// - Infer parents
		// - Calculate NNF
		// - Could be transient


		// Create copy of Snomed Taxonomy and update
		SnomedTaxonomy snomedTaxonomy = new SnomedTaxonomy(container.getSnomedTaxonomy());
		try {
			snomedTaxonomyBuilder.updateTaxonomy(snomedTaxonomy, deltaZipInputStream);
		} catch (ReleaseImportException e) {
			throw new ReasonerServiceException("Failed to update snomed taxonomy.", e);
		}
		Set<Long> conceptIdsWithStatedChange = snomedTaxonomy.getStatedChangeConceptIds();
		logger.debug("ConceptIds with stated change: {}", conceptIdsWithStatedChange);
		timer.checkpoint("Update SNOMED taxonomy");


		// Update OWLOntology
		Set<Long> ungroupedRoles = snomedTaxonomy.getUngroupedRolesForContentTypeOrDefault(parseLong(Concepts.ALL_PRECOORDINATED_CONTENT));
		final OntologyService ontologyService = new OntologyService(ungroupedRoles, container.getOwlOntologyManager());
		final OWLOntology owlOntology = container.getReasoner().getRootOntology();
		final AxiomChangeSet axiomChangeSet = ontologyService.updateOntology(conceptIdsWithStatedChange, snomedTaxonomy, owlOntology);
		timer.checkpoint("Update OWL Ontology");

		final ReasonerTaxonomy reasonerTaxonomy;
		Set<Long> conceptsToProcess;
		try {
			// Infer hierarchy
			logger.info("OwlReasoner inferring class hierarchy");
			final OWLReasoner reasoner = container.getReasoner();
			reasoner.flush();
			reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
			timer.checkpoint("Inference computation");

			// Extract hierarchy
			logger.info("Update ReasonerTaxonomy");
			// Create copy of ReasonerTaxonomy and update
			reasonerTaxonomy = new ReasonerTaxonomy(container.getReasonerTaxonomy());
			ReasonerTaxonomyWalker walker = new ReasonerTaxonomyWalker(reasoner, reasonerTaxonomy);
			conceptsToProcess = walker.walkUpdatedPart(conceptIdsWithStatedChange);
			timer.checkpoint("Update ReasonerTaxonomy");
		} finally {
			// Revert axiom changes
			ontologyService.revertOntologyUpdate(axiomChangeSet, owlOntology);
		}

		// Generate NNF
		logger.info("Generate normal form");
		// Convert axioms to relationship-like rows
		AxiomRelationshipConversionService axiomRelationshipConversionService = new AxiomRelationshipConversionService(ungroupedRoles);
//		final RelationshipNormalFormGenerator baseNormalFormGenerator = container.getNormalFormGenerator();
		RelationshipNormalFormGenerator normalFormGenerator = new RelationshipNormalFormGenerator(reasonerTaxonomy, snomedTaxonomy, container.getNormalFormGenerator());
		try {
			// Convert just the axioms of concepts with stated changes
			final Map<Long, List<OWLAxiom>> conceptAxiomMapNewEntries = snomedTaxonomy.getConceptAxiomMap().entrySet().stream()
					.filter((entry) -> conceptIdsWithStatedChange.contains(entry.getKey()))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
							(u,v) -> { throw new IllegalStateException(String.format("Duplicate key %s", u)); }, Long2ObjectOpenHashMap::new));
			Map<Long, Set<AxiomRepresentation>> conceptAxiomStatementMapUpdates = axiomRelationshipConversionService.convertAxiomsToRelationships(conceptAxiomMapNewEntries, true);
			// Update existing map
			Map<Long, Set<AxiomRepresentation>> conceptAxiomStatementMap = normalFormGenerator.getConceptAxiomStatementMap();
			// Remove all concepts with stated change
			for (Long conceptId : conceptIdsWithStatedChange) {
				conceptAxiomStatementMap.remove(conceptId);
			}
			// Put all new version axioms
			conceptAxiomStatementMap.putAll(conceptAxiomStatementMapUpdates);
		} catch (ConversionException e) {
			throw new ReasonerServiceException("Failed to convert OWL Axiom Expressions into relationships for normal form generation.", e);
		}

		RelationshipChangeProcessor changeCollector = normalFormGenerator.collectNormalFormChanges(conceptsToProcess);
		timer.checkpoint("Generate normal form");

		logger.info("Inactivating inferred relationships for new inactive concepts");
		new RelationshipInactivationProcessor(snomedTaxonomy).processInactivationChanges(changeCollector);

		// Restore inactive relationships where appropriate
		for (Long conceptId : changeCollector.getAddedStatements().keySet()) {
			Set<Relationship> conceptInactiveInferredRelationship = snomedTaxonomy.getInactiveInferredRelationships(conceptId);
			Set<Relationship> newInferredRelationship = changeCollector.getAddedStatements().get(conceptId);

			if(!conceptInactiveInferredRelationship.isEmpty() && !newInferredRelationship.isEmpty()) {
				for (Relationship newRel : newInferredRelationship) {
					if (newRel.getRelationshipId() == -1) {// If we are updating an existing relationship then no need to find another one
						for (Relationship inactiveRel : conceptInactiveInferredRelationship) {
							if (newRel.getGroup() == inactiveRel.getGroup()
									&& newRel.getTypeId() == inactiveRel.getTypeId()
									&& newRel.getDestinationId() == inactiveRel.getDestinationId()) {
								newRel.setRelationshipId(inactiveRel.getRelationshipId());
							}
						}
					}
				}
			}
		}

		long redundantCount = changeCollector.getRedundantCount();
		long totalChanges = changeCollector.getAddedCount() + changeCollector.getUpdatedCount() + redundantCount + changeCollector.getRemovedDueToConceptInactivationCount();
		logger.info("{} relationship rows changed: {} added, {} updated, {} redundant, {} removed due to concept inactivation.",
				formatDecimal(totalChanges), formatDecimal(changeCollector.getAddedCount()), formatDecimal(changeCollector.getUpdatedCount()),
				formatDecimal(redundantCount), formatDecimal(changeCollector.getRemovedDueToConceptInactivationCount()));

		logger.info("Writing results archive");
		classificationResultsWriter.writeResultsRf2Archive(changeCollector, reasonerTaxonomy.getEquivalentConceptIds(), resultsRf2DeltaArchive, new Date());
		timer.checkpoint("Write results to disk");
		timer.finish();


	}

	private void printAxioms(OWLOntology ontology, OntologyService ontologyService, Long conceptId, String name) {
		System.out.println();
		final OWLClass clazz = ontologyService.getOwlClass(conceptId);
		final Set<OWLClassAxiom> axioms = ontology.getAxioms(clazz, Imports.EXCLUDED);
		System.out.println(axioms.size() + " axioms found for concept " + clazz + ": " + name);
		for (OWLClassAxiom axiom : axioms) {
			System.out.println(axiom);
		}
		System.out.println();
	}

	private String formatDecimal(long number) {
		return String.format("%,d", number);
	}

	private OWLReasonerFactory getOWLReasonerFactory(String reasonerFactoryClassName) throws ReasonerServiceException {
		Class<?> reasonerFactoryClass = null;
		try {
			reasonerFactoryClass = Class.forName(reasonerFactoryClassName);
			return (OWLReasonerFactory) reasonerFactoryClass.newInstance();
		} catch (ClassNotFoundException e) {
			throw new ReasonerServiceException(String.format("Requested reasoner class '%s' not found.", reasonerFactoryClassName), e);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ReasonerServiceException(String.format("An instance of requested reasoner '%s' could not be created.", reasonerFactoryClass), e);
		}
	}

}
