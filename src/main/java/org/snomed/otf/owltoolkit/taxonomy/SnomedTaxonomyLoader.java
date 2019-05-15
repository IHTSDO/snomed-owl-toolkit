/*
 * Copyright 2018 SNOMED International, http://snomed.org
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

import com.google.common.base.Strings;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.factory.ComponentFactory;
import org.ihtsdo.otf.snomedboot.factory.ImpotentComponentFactory;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.domain.Relationship;
import org.snomed.otf.owltoolkit.ontology.OntologyService;

import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.Long.parseLong;
import static org.snomed.otf.owltoolkit.constants.Concepts.*;

public class SnomedTaxonomyLoader extends ImpotentComponentFactory {

	private SnomedTaxonomy snomedTaxonomy = new SnomedTaxonomy();
	private static final String ACTIVE = "1";

	private boolean loadingDelta;
	private int effectiveTimeNow = Integer.parseInt(new SimpleDateFormat("yyyyMMdd").format(new Date()));

	private Exception owlParsingExceptionThrown;
	private String owlParsingExceptionMemberId;
	private final AxiomDeserialiser axiomDeserialiser;
	private ComponentFactory deltaComponentFactoryTap;
	private ComponentFactory snapshotComponentFactoryTap;
	private static final Logger LOGGER = LoggerFactory.getLogger(SnomedTaxonomyLoader.class);

	public SnomedTaxonomyLoader() {
		axiomDeserialiser = new AxiomDeserialiser();
	}

	/**
	 * New component states are copied to componentFactoryTap.
	 * @param snapshotComponentFactoryTap the component factory to copy snapshot states to.
	 * @param deltaComponentFactoryTap the component factory to copy delta states to.
	 */
	SnomedTaxonomyLoader(ComponentFactory snapshotComponentFactoryTap, ComponentFactory deltaComponentFactoryTap) {
		this();
		this.snapshotComponentFactoryTap = snapshotComponentFactoryTap;
		this.deltaComponentFactoryTap = deltaComponentFactoryTap;
	}

	@Override
	public void newConceptState(String conceptId, String effectiveTime, String active, String moduleId, String definitionStatusId) {
		if (ACTIVE.equals(active)) {
			long id = parseLong(conceptId);
			snomedTaxonomy.getAllConceptIds().add(id);
			if (Concepts.FULLY_DEFINED.equals(definitionStatusId)) {
				snomedTaxonomy.getFullyDefinedConceptIds().add(id);
			} else {
				snomedTaxonomy.getFullyDefinedConceptIds().remove(id);
			}
			if (loadingDelta) {
				// This will make sure that concepts active in the delta will be removed from the list if previously inactive in the snapshot
				snomedTaxonomy.getInactivatedConcepts().remove(id);
			}
		} else {
			long id = parseLong(conceptId);
			// This will take inactive concepts from both snapshot and delta
			snomedTaxonomy.getInactivatedConcepts().add(id);
			if (loadingDelta) {
				// Inactive concepts in the delta should be removed from the snapshot view
				snomedTaxonomy.getAllConceptIds().remove(id);
				snomedTaxonomy.getFullyDefinedConceptIds().remove(id);
			}
		}
		ComponentFactory componentFactoryTap = getComponentFactoryTap();
		if (componentFactoryTap != null) {
			componentFactoryTap.newConceptState(conceptId, effectiveTime, active, moduleId, definitionStatusId);
		}
	}

	@Override
	public void newRelationshipState(String id, String effectiveTime, String active, String moduleId, String sourceId, String destinationId, String relationshipGroup, String typeId, String characteristicTypeId, String modifierId) {
		boolean stated = STATED_RELATIONSHIP.equals(characteristicTypeId);

		if (ACTIVE.equals(active) && !ADDITIONAL_RELATIONSHIP.equals(characteristicTypeId)) {// Ignore additional relationships

			boolean universal = UNIVERSAL_RESTRICTION_MODIFIER.equals(modifierId);
			int unionGroup = 0;

			// TODO: is this correct? Is there a better way?
			// From Snow Owl import logic:
			// Universal "has active ingredient" relationships should be put into a union group
			if (Concepts.HAS_ACTIVE_INGREDIENT.equals(typeId) && universal) {
				unionGroup = 1;
			}

			int effectiveTimeInt = !Strings.isNullOrEmpty(effectiveTime) ? Integer.parseInt(effectiveTime) : effectiveTimeNow;
			snomedTaxonomy.addOrModifyRelationship(
					stated,
					parseLong(sourceId),
					new Relationship(
							parseLong(id),
							effectiveTimeInt,
							parseLong(moduleId),
							parseLong(typeId),
							parseLong(destinationId),
							false,// Destination negated is always false
							Integer.parseInt(relationshipGroup),
							unionGroup,
							universal,
							parseLong(characteristicTypeId)
					)
			);
		} else {
			// Inactive
			if (loadingDelta) {
				// Inactive relationships in the delta should be removed from the snapshot view
				snomedTaxonomy.removeRelationship(stated, sourceId, id);
			}
			if (!stated) {
				// Inactive inferred relationships kept for possible reactivation
				int effectiveTimeInt = !Strings.isNullOrEmpty(effectiveTime) ? Integer.parseInt(effectiveTime) : effectiveTimeNow;
				boolean universal = UNIVERSAL_RESTRICTION_MODIFIER.equals(modifierId);
				snomedTaxonomy.addInactiveInferredRelationship(parseLong(sourceId), new Relationship(
						parseLong(id),
						effectiveTimeInt,
						parseLong(moduleId),
						parseLong(typeId),
						parseLong(destinationId),
						false,
						Integer.parseInt(relationshipGroup),
						0,
						universal,
						parseLong(characteristicTypeId)));
			}
		}
		ComponentFactory componentFactoryTap = getComponentFactoryTap();
		if (componentFactoryTap != null) {
			componentFactoryTap.newRelationshipState(id, effectiveTime, active, moduleId, sourceId, destinationId, relationshipGroup, typeId, characteristicTypeId, modifierId);
		}
	}

	@Override
	public void newReferenceSetMemberState(String[] fieldNames, String id, String effectiveTime, String active, String moduleId, String refsetId, String referencedComponentId, String... otherValues) {
		if (refsetId.equals(Concepts.OWL_AXIOM_REFERENCE_SET) && owlParsingExceptionThrown == null) {
			if (ACTIVE.equals(active)) {
				try {
					addActiveAxiom(id, referencedComponentId, otherValues[0]);
				} catch (OWLException | OWLRuntimeException | IllegalArgumentException e) {
					owlParsingExceptionThrown = e;
					owlParsingExceptionMemberId = id;
				}
			} else {
				// Remove the axiom from our active set
				// Match by id rather than a deserialised representation because the equals method may fail.
				snomedTaxonomy.removeAxiom(referencedComponentId, id);
			}
		} else if (refsetId.equals(Concepts.OWL_ONTOLOGY_REFERENCE_SET)) {
			if (Concepts.OWL_ONTOLOGY_NAMESPACE.equals(referencedComponentId)) {
				if (ACTIVE.equals(active)) {
					snomedTaxonomy.addOntologyNamespace(id, otherValues[0]);
				} else {
					snomedTaxonomy.removeOntologyNamespace(id);
				}
			} else if (Concepts.OWL_ONTOLOGY_HEADER.equals(referencedComponentId)) {
				if (ACTIVE.equals(active)) {
					snomedTaxonomy.addOntologyHeader(id, otherValues[0]);
				} else {
					snomedTaxonomy.removeOntologyHeader(id);
				}
			} else {
				LOGGER.warn("Unrecognised referencedComponentId '{}' in OWL Ontology reference set file. Only {} or {} are expected. Ignoring entry.",
						referencedComponentId, Concepts.OWL_ONTOLOGY_NAMESPACE, Concepts.OWL_ONTOLOGY_HEADER);
			}
		} else if (refsetId.equals(Concepts.MRCM_ATTRIBUTE_DOMAIN_INTERNATIONAL_REFERENCE_SET)) {
			long attributeId = parseLong(referencedComponentId);
			boolean ungrouped = otherValues[1].equals("0");
			Long contentTypeId = parseLong(otherValues[5]);
			if (ACTIVE.equals(active) && ungrouped) {
				snomedTaxonomy.addUngroupedRole(contentTypeId, attributeId);
			} else {
				snomedTaxonomy.removeUngroupedRole(contentTypeId, attributeId);
			}
		}
		ComponentFactory componentFactoryTap = getComponentFactoryTap();
		if (componentFactoryTap != null) {
			componentFactoryTap.newReferenceSetMemberState(fieldNames, id, effectiveTime, active, moduleId, refsetId, referencedComponentId, otherValues);
		}
	}

	public void addActiveAxiom(String id, String referencedComponentId, String owlExpression) throws OWLOntologyCreationException {
		String owlExpressionString = owlExpression
				// Replace any remaining outdated role group constants
				.replace(OntologyService.ROLE_GROUP_OUTDATED_CONSTANT, OntologyService.ROLE_GROUP_SCTID);

		OWLAxiom owlAxiom = axiomDeserialiser.deserialiseAxiom(owlExpressionString, id);
		snomedTaxonomy.addAxiom(referencedComponentId, id, owlAxiom);
	}

	@Override
	public void newDescriptionState(String id, String effectiveTime, String active, String moduleId, String conceptId, String languageCode, String typeId, String term, String caseSignificanceId) {
		if (ACTIVE.equals(active) && typeId.equals(Concepts.FSN)) {
			snomedTaxonomy.addFsn(conceptId, term);
		}
		ComponentFactory componentFactoryTap = getComponentFactoryTap();
		if (componentFactoryTap != null) {
			componentFactoryTap.newDescriptionState(id, effectiveTime, active, moduleId, conceptId, languageCode, typeId, term, caseSignificanceId);
		}
	}

	private ComponentFactory getComponentFactoryTap() {
		return loadingDelta ? deltaComponentFactoryTap : snapshotComponentFactoryTap;
	}

	void reportErrors() throws ReleaseImportException {
		if (owlParsingExceptionThrown != null) {
			throw new ReleaseImportException("Failed to parse OWL Axiom in reference set member '" + owlParsingExceptionMemberId + "'",
					owlParsingExceptionThrown);
		}
	}

	public OWLAxiom deserialiseAxiom(String axiomString) throws OWLOntologyCreationException {
		return axiomDeserialiser.deserialiseAxiom(axiomString, null);
	}

	public SnomedTaxonomy getSnomedTaxonomy() {
		return snomedTaxonomy;
	}

	void startLoadingDelta() {
		loadingDelta = true;
		axiomDeserialiser.clearCounters();
	}

	long getTimeTakenDeserialisingAxioms() {
		return axiomDeserialiser.getTimeTakenDeserialisingAxioms();
	}
}
