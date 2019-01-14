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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.constants.DescriptionType;
import org.snomed.otf.owltoolkit.util.InputStreamSet;
import org.springframework.util.StopWatch;

import java.io.InputStream;

import static org.snomed.otf.owltoolkit.constants.Concepts.*;

public class SnomedTaxonomyBuilder {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final LoadingProfile SNAPSHOT_LOADING_PROFILE = new LoadingProfile()
			.withStatedRelationships()
			.withInactiveConcepts()
			.withFullRelationshipObjects()
			.withRefset(OWL_ONTOLOGY_REFERENCE_SET)
			.withRefset(OWL_AXIOM_REFERENCE_SET)
			.withRefset(MRCM_ATTRIBUTE_DOMAIN_INTERNATIONAL_REFERENCE_SET)
			.withFullRefsetMemberObjects()
			.withoutDescriptions();

	static {
		// Giving reference set filename patterns avoids reading them all
		SNAPSHOT_LOADING_PROFILE.getIncludedReferenceSetFilenamePatterns().add(".*_sRefset_OWL.*");
		SNAPSHOT_LOADING_PROFILE.getIncludedReferenceSetFilenamePatterns().add(".*_cissccRefset_MRCMAttributeDomain.*");
	}

	public SnomedTaxonomy build(InputStreamSet snomedRf2SnapshotArchives) throws ReleaseImportException {
		return build(snomedRf2SnapshotArchives, null);
	}

	public SnomedTaxonomy build(
			InputStreamSet snomedRf2SnapshotArchives,
			InputStream currentReleaseRf2DeltaArchive) throws ReleaseImportException {

		return build(snomedRf2SnapshotArchives, currentReleaseRf2DeltaArchive, null, null, DescriptionType.NONE, null);
	}

	public SnomedTaxonomy build(
			InputStreamSet snomedRf2SnapshotArchives,
			InputStream currentReleaseRf2DeltaArchive,
			DescriptionType descriptionType,
			String langRefset) throws ReleaseImportException {

		return build(snomedRf2SnapshotArchives, currentReleaseRf2DeltaArchive, null, null, descriptionType, langRefset);
	}

	public SnomedTaxonomy build(
			InputStreamSet snomedRf2SnapshotArchives,
			InputStream currentReleaseRf2DeltaArchive,
			ComponentFactory snapshotComponentFactoryTap,
			ComponentFactory deltaComponentFactoryTap) throws ReleaseImportException {
		return build(snomedRf2SnapshotArchives, currentReleaseRf2DeltaArchive, snapshotComponentFactoryTap, deltaComponentFactoryTap, DescriptionType.NONE, null);
	}

	public SnomedTaxonomy build(
			InputStreamSet snomedRf2SnapshotArchives,
			InputStream currentReleaseRf2DeltaArchive,
			ComponentFactory snapshotComponentFactoryTap,
			ComponentFactory deltaComponentFactoryTap,
			DescriptionType descriptionType,
			String langRefset) throws ReleaseImportException {

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		if (descriptionType == null) {
			descriptionType = DescriptionType.NONE;
		}

		SnomedTaxonomyLoader snomedTaxonomyLoader = new SnomedTaxonomyLoader(snapshotComponentFactoryTap, deltaComponentFactoryTap);
		snomedTaxonomyLoader.setDescriptionType(descriptionType);
		snomedTaxonomyLoader.setLangRefset(langRefset);

		ReleaseImporter releaseImporter = new ReleaseImporter();

		LoadingProfile loadingProfile = SNAPSHOT_LOADING_PROFILE;
		if (descriptionType != DescriptionType.NONE) {
			loadingProfile = SNAPSHOT_LOADING_PROFILE
					.withFullDescriptionObjects()
					.withRefset(langRefset);
			loadingProfile.getIncludedReferenceSetFilenamePatterns().add(".*_cRefset_Language.*");
		}

		releaseImporter.loadEffectiveSnapshotReleaseFileStreams(
				snomedRf2SnapshotArchives.getFileInputStreams(),
				loadingProfile,
				snomedTaxonomyLoader);
		snomedTaxonomyLoader.reportErrors();
		logger.info("Loaded release snapshot");
		logger.info("Time taken deserialising axioms {}s", (snomedTaxonomyLoader.getTimeTakenDeserialisingAxioms() / 1000));

		if (currentReleaseRf2DeltaArchive != null) {
			snomedTaxonomyLoader.startLoadingDelta();

			releaseImporter.loadDeltaReleaseFiles(
					currentReleaseRf2DeltaArchive,
					loadingProfile
							.withInactiveRelationships()
							.withInactiveRefsetMembers(),
					snomedTaxonomyLoader);
			snomedTaxonomyLoader.reportErrors();
			logger.info("Loaded delta");
			logger.info("Time taken deserialising axioms {}s", (snomedTaxonomyLoader.getTimeTakenDeserialisingAxioms() / 1000));
		}

		stopWatch.stop();
		logger.info("SnomedTaxonomy loaded in {} seconds", stopWatch.getTotalTimeSeconds());

		SnomedTaxonomy snomedTaxonomy = snomedTaxonomyLoader.getSnomedTaxonomy();
		logger.info("{} concepts loaded", snomedTaxonomy.getAllConceptIds().size());
		logger.info("{} active stated relationships loaded", snomedTaxonomy.getStatedRelationships().size());
		logger.info("{} active axioms loaded", snomedTaxonomy.getAxiomCount());
		return snomedTaxonomy;
	}
}
