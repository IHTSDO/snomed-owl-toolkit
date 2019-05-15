/*
 * Copyright 2019 SNOMED International, http://snomed.org
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

import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
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
import org.snomed.otf.owltoolkit.ontology.OntologyDebugUtil;
import org.snomed.otf.owltoolkit.ontology.OntologyService;
import org.snomed.otf.owltoolkit.ontology.PropertyChain;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomyBuilder;
import org.snomed.otf.owltoolkit.util.InputStreamSet;
import org.snomed.otf.owltoolkit.util.OptionalFileInputStream;
import org.snomed.otf.owltoolkit.util.TimerUtil;

import java.io.*;
import java.util.*;

import static java.lang.Long.parseLong;

public class SnomedReasonerService {

	public static final String ELK_REASONER_FACTORY = "org.semanticweb.elk.owlapi.ElkReasonerFactory";

	private final ClassificationResultsWriter classificationResultsWriter;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public SnomedReasonerService() {
		this.classificationResultsWriter = new ClassificationResultsWriter();
	}

	public void classify(String classificationId,
			File previousReleaseRf2SnapshotArchiveFiles,
			File currentReleaseRf2DeltaArchiveFile,
			File resultsRf2DeltaArchiveFile,
			String reasonerFactoryClassName,
			boolean outputOntologyFileForDebug) throws ReasonerServiceException {

		classify(classificationId,
				Collections.singleton(previousReleaseRf2SnapshotArchiveFiles),
				currentReleaseRf2DeltaArchiveFile,
				resultsRf2DeltaArchiveFile,
				reasonerFactoryClassName,
				outputOntologyFileForDebug
		);
	}

	public void classify(String classificationId,
			Set<File> previousReleaseRf2SnapshotArchiveFile,
			File currentReleaseRf2DeltaArchiveFile,
			File resultsRf2DeltaArchiveFile,
			String reasonerFactoryClassName,
			boolean outputOntologyFileForDebug) throws ReasonerServiceException {

		try (InputStreamSet previousReleaseRf2SnapshotArchives = new InputStreamSet(previousReleaseRf2SnapshotArchiveFile);
			 OptionalFileInputStream currentReleaseRf2DeltaArchive = new OptionalFileInputStream(currentReleaseRf2DeltaArchiveFile);
			 OutputStream resultsRf2DeltaArchive = new FileOutputStream(resultsRf2DeltaArchiveFile)) {

			classify(classificationId,
					previousReleaseRf2SnapshotArchives,
					currentReleaseRf2DeltaArchive.getInputStream().orElse(null),
					resultsRf2DeltaArchive,
					reasonerFactoryClassName,
					outputOntologyFileForDebug);
		} catch (IOException e) {
			throw new ReasonerServiceException("IO error handling input/output files.", e);
		}
	}

	public void classify(String classificationId,
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
		SnomedTaxonomyBuilder snomedTaxonomyBuilder = new SnomedTaxonomyBuilder();
		SnomedTaxonomy snomedTaxonomy;
		try {

			// Also load inactive inferred relationships into another MAP in snomedTaxonomyBuilder
			snomedTaxonomy = snomedTaxonomyBuilder.build(previousReleaseRf2SnapshotArchives, currentReleaseRf2DeltaArchive, false);
		} catch (ReleaseImportException e) {
			throw new ReasonerServiceException("Failed to build existing taxonomy.", e);
		}
		timer.checkpoint("Build existing taxonomy");

		logger.info("Creating OwlOntology");
		Set<Long> ungroupedRoles = snomedTaxonomy.getUngroupedRolesForContentTypeOrDefault(parseLong(Concepts.ALL_PRECOORDINATED_CONTENT));
		OntologyService ontologyService = new OntologyService(ungroupedRoles);
		OWLOntology owlOntology;
		try {
			owlOntology = ontologyService.createOntology(snomedTaxonomy);
		} catch (OWLOntologyCreationException e) {
			throw new ReasonerServiceException("Failed to build OWL Ontology.", e);
		}
		timer.checkpoint("Create OWL Ontology");

		Set<PropertyChain> propertyChains = ontologyService.getPropertyChains(owlOntology);

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
		reasoner.dispose();
		timer.checkpoint("Extract ReasonerTaxonomy");

		logger.info("Generate normal form");
		AxiomRelationshipConversionService axiomRelationshipConversionService = new AxiomRelationshipConversionService(ungroupedRoles);
		Map<Long, Set<AxiomRepresentation>> conceptAxiomStatementMap;
		try {
			conceptAxiomStatementMap = axiomRelationshipConversionService.convertAxiomsToRelationships(snomedTaxonomy.getConceptAxiomMap());
		} catch (ConversionException e) {
			throw new ReasonerServiceException("Failed to convert OWL Axiom Expressions into relationships for normal form generation.", e);
		}
		RelationshipNormalFormGenerator normalFormGenerator = new RelationshipNormalFormGenerator(reasonerTaxonomy, snomedTaxonomy, conceptAxiomStatementMap, propertyChains);

		RelationshipChangeProcessor changeCollector = new RelationshipChangeProcessor();
		normalFormGenerator.collectNormalFormChanges(changeCollector);
		timer.checkpoint("Generate normal form");

		logger.info("Inactivating inferred relationships for new inactive concepts");
		new RelationshipInactivationProcessor(snomedTaxonomy).processInactivationChanges(changeCollector);

		// Iterate through new inferred relationships to find inactive ones (from your Map) which match with the same relationshipGroup, sourceId, typeId and destinationId
		// For each matching inactive relationship found, set the relationship id on the new inferred relationship.
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
		classificationResultsWriter.writeResultsRf2Archive(changeCollector, reasonerTaxonomy.getEquivalentConceptIds(), resultsRf2DeltaArchive, startDate);
		timer.checkpoint("Write results to disk");
		timer.finish();
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
