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
package org.snomed.otf.owltoolkit.taxonomy;

import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.otf.snomedboot.factory.ComponentFactory;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.conversion.AxiomRelationshipConversionService;
import org.snomed.otf.owltoolkit.conversion.ConversionException;
import org.snomed.otf.owltoolkit.domain.AxiomRepresentation;
import org.snomed.otf.owltoolkit.util.InputStreamSet;
import org.springframework.util.StopWatch;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import static org.snomed.otf.owltoolkit.constants.Concepts.*;

public class SnomedTaxonomyBuilder {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final LoadingProfile SNAPSHOT_LOADING_PROFILE = new LoadingProfile()
			.withoutDescriptions()
			.withInactiveConcepts()
			.withStatedRelationships()
			// Inactive relationships needed when loading the snapshot for ID reuse.
			.withInactiveRelationships()
			.withRefset(OWL_ONTOLOGY_REFERENCE_SET)
			.withRefset(OWL_AXIOM_REFERENCE_SET)
			.withRefset(MRCM_ATTRIBUTE_DOMAIN_INTERNATIONAL_REFERENCE_SET)
			// Giving reference set filename patterns avoids reading them all
			.withIncludedReferenceSetFilenamePattern(".*_sRefset_OWL.*")
			.withIncludedReferenceSetFilenamePattern(".*_cissccRefset_MRCMAttributeDomain.*");

	private static final LoadingProfile SNAPSHOT_LOADING_PROFILE_PLUS_LANGUAGE = SNAPSHOT_LOADING_PROFILE
			.withDescriptions()
			.withAllRefsets()// We don't know what the ID of the language reference set is going to be.
			.withIncludedReferenceSetFilenamePattern("der2_cRefset_Language.*");
	
	
	private static final LoadingProfile OWL_SNAPSHOT_LOADING_PROFILE = new LoadingProfile()
			.withInactiveConcepts()
			.withRefset(OWL_ONTOLOGY_REFERENCE_SET)
			.withRefset(OWL_AXIOM_REFERENCE_SET)
			.withRefset(MRCM_ATTRIBUTE_DOMAIN_INTERNATIONAL_REFERENCE_SET)
			.withoutDescriptions();
	
	private static final LoadingProfile DELTA_LOADING_PROFILE = SNAPSHOT_LOADING_PROFILE
			.withInactiveRelationships()
			.withInactiveRefsetMembers();

	private static final LoadingProfile DELTA_LOADING_PROFILE_PLUS_LANGUAGE = SNAPSHOT_LOADING_PROFILE_PLUS_LANGUAGE
			.withInactiveRelationships()
			.withInactiveRefsetMembers();

	private final ReleaseImporter releaseImporter = new ReleaseImporter();

	public SnomedTaxonomy build(InputStreamSet snomedRf2SnapshotArchives, boolean includeFSNs) throws ReleaseImportException {
		return build(snomedRf2SnapshotArchives, null, includeFSNs);
	}

	public SnomedTaxonomy build(
			InputStreamSet snomedRf2SnapshotArchives,
			InputStream currentReleaseRf2DeltaArchive,
			boolean includeDescriptions) throws ReleaseImportException {

		return build(snomedRf2SnapshotArchives, currentReleaseRf2DeltaArchive, null, null, includeDescriptions);
	}

	public SnomedTaxonomy buildWithAxiomRefset(InputStreamSet snomedRf2OwlSnapshotArchive) throws ReleaseImportException {
		
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		SnomedTaxonomyLoader snomedTaxonomyLoader = new SnomedTaxonomyLoader();
		
		releaseImporter.loadEffectiveSnapshotReleaseFileStreams(snomedRf2OwlSnapshotArchive.getFileInputStreams(), OWL_SNAPSHOT_LOADING_PROFILE, snomedTaxonomyLoader, false);
		snomedTaxonomyLoader.reportErrors();
		logger.info("Loaded release snapshot");
		logger.info("Time taken deserialising axioms {}s", (snomedTaxonomyLoader.getTimeTakenDeserialisingAxioms() / 1000.00));
		
		stopWatch.stop();
		logger.info("SnomedTaxonomy loaded in {} seconds", stopWatch.getTotalTimeSeconds());

		SnomedTaxonomy snomedTaxonomy = snomedTaxonomyLoader.getSnomedTaxonomy();
		logger.info("{} concepts loaded", snomedTaxonomy.getAllConceptIds().size());
		logger.info("{} active axioms loaded", snomedTaxonomy.getAxiomCount());
		return snomedTaxonomy;

	}
	
	public SnomedTaxonomy build(
			InputStreamSet snomedRf2SnapshotArchives,
			InputStream currentReleaseRf2DeltaArchive,
			ComponentFactory snapshotComponentFactoryTap,
			ComponentFactory deltaComponentFactoryTap,
			boolean includeDescriptions) throws ReleaseImportException {

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		SnomedTaxonomyLoader snomedTaxonomyLoader = new SnomedTaxonomyLoader(snapshotComponentFactoryTap, deltaComponentFactoryTap);
		
		releaseImporter.loadEffectiveSnapshotReleaseFileStreams(
				snomedRf2SnapshotArchives.getFileInputStreams(),
				includeDescriptions ? SNAPSHOT_LOADING_PROFILE_PLUS_LANGUAGE : SNAPSHOT_LOADING_PROFILE,
				snomedTaxonomyLoader,
				false);
		snomedTaxonomyLoader.reportErrors();
		logger.info("Loaded release snapshot");
		logger.info("Time taken deserialising axioms {}s", (snomedTaxonomyLoader.getTimeTakenDeserialisingAxioms() / 1000.00));

		if (currentReleaseRf2DeltaArchive != null) {
			logger.info("Loading delta");
			snomedTaxonomyLoader.startLoadingDelta();

			releaseImporter.loadDeltaReleaseFiles(
					currentReleaseRf2DeltaArchive,
					includeDescriptions ? DELTA_LOADING_PROFILE_PLUS_LANGUAGE : DELTA_LOADING_PROFILE,
					snomedTaxonomyLoader,
					false);
			snomedTaxonomyLoader.reportErrors();
			logger.info("Loaded delta");
			logger.info("Time taken deserialising axioms {}s", (snomedTaxonomyLoader.getTimeTakenDeserialisingAxioms() / 1000));
		} else {
			logger.info("Loading complete.");
		}

		stopWatch.stop();
		logger.info("SnomedTaxonomy loaded in {} seconds", stopWatch.getTotalTimeSeconds());

		SnomedTaxonomy snomedTaxonomy = snomedTaxonomyLoader.getSnomedTaxonomy();
		logger.info("{} concepts loaded", snomedTaxonomy.getAllConceptIds().size());
		logger.info("{} active stated relationships loaded", snomedTaxonomy.getStatedRelationships().size());
		logger.info("{} active axioms loaded", snomedTaxonomy.getAxiomCount());
		return snomedTaxonomy;
	}

	public void updateTaxonomy(SnomedTaxonomy snomedTaxonomy, InputStream deltaInputStream) throws ReleaseImportException {
		SnomedTaxonomyLoader snomedTaxonomyLoader = new SnomedTaxonomyLoader(snomedTaxonomy);
		releaseImporter.loadDeltaReleaseFiles(deltaInputStream, DELTA_LOADING_PROFILE, snomedTaxonomyLoader, false);
	}

	/**
	 * Add the set of axioms to the taxonomy in a loose way. Generate UUIDs and add left hand side concepts as needed.
	 * @param snomedTaxonomy The taxonomy to update.
	 * @param axioms The axioms to add.
	 * @param axiomRelationshipConversionService Service to convert the AxiomRepresentations to owl expression strings.
	 */
	public void updateTaxonomyWithTransientAxioms(SnomedTaxonomy snomedTaxonomy, Set<AxiomRepresentation> axioms,
			AxiomRelationshipConversionService axiomRelationshipConversionService) throws ConversionException, OWLOntologyCreationException {

		SnomedTaxonomyLoader snomedTaxonomyLoader = new SnomedTaxonomyLoader(snomedTaxonomy);
		for (AxiomRepresentation axiom : axioms) {
			final Long conceptId = axiom.getLeftHandSideNamedConcept() != null ? axiom.getLeftHandSideNamedConcept() : axiom.getRightHandSideNamedConcept();
			final String owlExpression = axiomRelationshipConversionService.convertRelationshipsToAxiom(axiom);
			snomedTaxonomyLoader.addActiveAxiom(UUID.randomUUID().toString(), conceptId.toString(), owlExpression);
		}
	}
}
