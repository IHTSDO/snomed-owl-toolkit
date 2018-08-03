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

import static org.snomed.otf.owltoolkit.constants.Concepts.MRCM_ATTRIBUTE_DOMAIN_INTERNATIONAL_REFERENCE_SET;
import static org.snomed.otf.owltoolkit.constants.Concepts.OWL_AXIOM_REFERENCE_SET;

import java.io.InputStream;

import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.util.InputStreamSet;
import org.springframework.util.StopWatch;

public class SnomedTaxonomyBuilder {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final LoadingProfile SNAPSHOT_LOADING_PROFILE = new LoadingProfile()
			.withStatedRelationships()
			.withInactiveConcepts() 
			.withFullRelationshipObjects()
			.withRefset(OWL_AXIOM_REFERENCE_SET)
			.withRefset(MRCM_ATTRIBUTE_DOMAIN_INTERNATIONAL_REFERENCE_SET)
			.withFullRefsetMemberObjects()
			.withoutDescriptions();

	static {
		// Giving reference set filename patterns avoids reading them all
		SNAPSHOT_LOADING_PROFILE.getIncludedReferenceSetFilenamePatterns().add(".*Axiom.*");
		SNAPSHOT_LOADING_PROFILE.getIncludedReferenceSetFilenamePatterns().add(".*MRCM.*");
	}

	private static final LoadingProfile DELTA_LOADING_PROFILE = SNAPSHOT_LOADING_PROFILE
			.withInactiveRelationships()
			.withInactiveRefsetMembers();

	public SnomedTaxonomy build(InputStreamSet snomedRf2SnapshotArchives, boolean includeFSNs) throws ReleaseImportException {
		return build(snomedRf2SnapshotArchives, null, includeFSNs);
	}

	public SnomedTaxonomy build(
			InputStreamSet snomedRf2SnapshotArchives,
			InputStream currentReleaseRf2DeltaArchive,
			boolean includeFSNs) throws ReleaseImportException {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		SnomedTaxonomyLoader snomedTaxonomyLoader = new SnomedTaxonomyLoader();

		ReleaseImporter releaseImporter = new ReleaseImporter();
		releaseImporter.loadEffectiveSnapshotReleaseFileStreams(
				snomedRf2SnapshotArchives.getFileInputStreams(),
				includeFSNs ? SNAPSHOT_LOADING_PROFILE.withFullDescriptionObjects() : SNAPSHOT_LOADING_PROFILE,
				snomedTaxonomyLoader);
		snomedTaxonomyLoader.reportErrors();
		logger.info("Loaded release snapshot");

		if (currentReleaseRf2DeltaArchive != null) {
			snomedTaxonomyLoader.startLoadingDelta();

			releaseImporter.loadDeltaReleaseFiles(
					currentReleaseRf2DeltaArchive,
					includeFSNs ? DELTA_LOADING_PROFILE.withFullDescriptionObjects() : DELTA_LOADING_PROFILE,
					snomedTaxonomyLoader);
			snomedTaxonomyLoader.reportErrors();
			logger.info("Loaded delta");
		}

		stopWatch.stop();
		logger.info("SnomedTaxonomy loaded in {} seconds", stopWatch.getTotalTimeSeconds());

		SnomedTaxonomy snomedTaxonomy = snomedTaxonomyLoader.getSnomedTaxonomy();
		logger.info("{} concepts loaded", snomedTaxonomy.getAllConceptIds().size());
		return snomedTaxonomy;
	}
}
