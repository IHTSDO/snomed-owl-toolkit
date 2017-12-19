/*
 * Copyright 2017 SNOMED International, http://snomed.org
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
import org.snomed.otf.owltoolkit.normalform.RelationshipChangeCollector;
import org.snomed.otf.owltoolkit.normalform.RelationshipInactivationProcessor;
import org.snomed.otf.owltoolkit.normalform.RelationshipNormalFormGenerator;
import org.snomed.otf.owltoolkit.ontology.OntologyDebugUtil;
import org.snomed.otf.owltoolkit.ontology.OntologyService;
import org.snomed.otf.owltoolkit.service.ClassificationResultsWriter;
import org.snomed.otf.owltoolkit.service.ReasonerServiceException;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomyBuilder;

import java.io.*;
import java.util.Date;
import java.util.Set;

import static java.lang.Long.parseLong;

public class SnomedReasonerService {

	public static final String ELK_REASONER_FACTORY = "org.semanticweb.elk.owlapi.ElkReasonerFactory";

	private final ClassificationResultsWriter classificationResultsWriter;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public SnomedReasonerService() {
		this.classificationResultsWriter = new ClassificationResultsWriter();
	}

	public void classify(String classificationId,
						 File previousReleaseRf2SnapshotArchiveFile,
						 File currentReleaseRf2DeltaArchiveFile,
						 File resultsRf2DeltaArchiveFile,
						 String reasonerFactoryClassName,
						 boolean outputOntologyFileForDebug) throws ReasonerServiceException {

		try (InputStream previousReleaseRf2SnapshotArchive = new FileInputStream(previousReleaseRf2SnapshotArchiveFile);
			 InputStream currentReleaseRf2DeltaArchive = new FileInputStream(currentReleaseRf2DeltaArchiveFile);
			 OutputStream resultsRf2DeltaArchive = new FileOutputStream(resultsRf2DeltaArchiveFile)) {

			classify(classificationId,
					previousReleaseRf2SnapshotArchive,
					currentReleaseRf2DeltaArchive,
					resultsRf2DeltaArchive,
					reasonerFactoryClassName,
					outputOntologyFileForDebug);
		} catch (IOException e) {
			throw new ReasonerServiceException("IO error handling input/output files.", e);
		}
	}

	public void classify(String classificationId,
						 InputStream previousReleaseRf2SnapshotArchive,
						 InputStream currentReleaseRf2DeltaArchive,
						 OutputStream resultsRf2DeltaArchive,
						 String reasonerFactoryClassName,
						 boolean outputOntologyFileForDebug) throws ReasonerServiceException {

		Date startDate = new Date();
		logger.info("Checking requested reasoner is available");
		OWLReasonerFactory reasonerFactory = getOWLReasonerFactory(reasonerFactoryClassName);

		logger.info("Building snomedTaxonomy");
		SnomedTaxonomyBuilder snomedTaxonomyBuilder = new SnomedTaxonomyBuilder();
		SnomedTaxonomy snomedTaxonomy;
		try {
			snomedTaxonomy = snomedTaxonomyBuilder.build(previousReleaseRf2SnapshotArchive, currentReleaseRf2DeltaArchive);
		} catch (ReleaseImportException e) {
			throw new ReasonerServiceException("Failed to build existing taxonomy.", e);
		}

		Set<Long> ungroupedRoles = snomedTaxonomy.getUngroupedRolesForContentType(parseLong(Concepts.ALL_PRECOORDINATED_CONTENT));
		if (ungroupedRoles.isEmpty()) {
			ungroupedRoles = SnomedTaxonomy.DEFAULT_NEVER_GROUPED_ROLE_IDS;
			logger.info("Using ungrouped roles defaults {}", ungroupedRoles);
		} else {
			logger.info("Using ungrouped roles from MRCM reference set {}", ungroupedRoles);
		}

		logger.info("Creating OwlOntology");
		OntologyService ontologyService = new OntologyService(ungroupedRoles);
		OWLOntology owlOntology;
		try {
			owlOntology = ontologyService.createOntology(snomedTaxonomy);
		} catch (OWLOntologyCreationException e) {
			throw new ReasonerServiceException("Failed to build OWL Ontology.", e);
		}
		Set<Long> propertiesDeclaredAsTransitive = ontologyService.getPropertiesDeclaredAsTransitive(owlOntology);

		if (outputOntologyFileForDebug) {
			OntologyDebugUtil.serialiseOntologyForDebug(classificationId, owlOntology);
		}

		logger.info("Creating OwlReasoner");
		final OWLReasonerConfiguration configuration = new SimpleConfiguration(new ConsoleProgressMonitor());
		OWLReasoner reasoner = reasonerFactory.createReasoner(owlOntology, configuration);

		logger.info("OwlReasoner inferring class hierarchy");
		reasoner.flush();
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		logger.info("Inference complete");
		logger.info("{} seconds so far", (new Date().getTime() - startDate.getTime())/1000f);

		logger.info("Extract ReasonerTaxonomy");
		ReasonerTaxonomyWalker walker = new ReasonerTaxonomyWalker(reasoner, new ReasonerTaxonomy(), ontologyService.getPrefixManager());
		ReasonerTaxonomy reasonerTaxonomy = walker.walk();

		logger.info("Generate normal form");
		RelationshipNormalFormGenerator normalFormGenerator = new RelationshipNormalFormGenerator(reasonerTaxonomy, snomedTaxonomy, propertiesDeclaredAsTransitive);
		RelationshipChangeCollector changeCollector = new RelationshipChangeCollector(true);
		normalFormGenerator.collectNormalFormChanges(changeCollector);
		logger.info("{} relationships added, {} removed", changeCollector.getAddedCount(), changeCollector.getRemovedCount());

		logger.info("Inactivating inferred relationships for new inactive concepts");
		RelationshipInactivationProcessor processor = new RelationshipInactivationProcessor(snomedTaxonomy);
		RelationshipChangeCollector inactivationCollector = new RelationshipChangeCollector(false);
		processor.processInactivationChanges(inactivationCollector);
		changeCollector.getRemovedStatements().putAll(inactivationCollector.getRemovedStatements());
		logger.info("{} relationships inactivated ", inactivationCollector.getRemovedCount());
		
		logger.info("Writing results archive");
		classificationResultsWriter.writeResultsRf2Archive(changeCollector, reasonerTaxonomy.getEquivalentConceptIds(), resultsRf2DeltaArchive, startDate);
		logger.info("Results archive written.");
		logger.info("{} seconds total", (new Date().getTime() - startDate.getTime())/1000f);
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
