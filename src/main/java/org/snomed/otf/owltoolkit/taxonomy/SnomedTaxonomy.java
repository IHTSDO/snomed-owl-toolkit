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
package org.snomed.otf.owltoolkit.taxonomy;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.domain.Relationship;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;

public class SnomedTaxonomy {

	private Map<String, String> ontologyNamespaces = new HashMap<>();
	private Map<String, String> ontologyHeader = new HashMap<>();
	private Set<Long> allConceptIds = new LongOpenHashSet();
	private Map<Long, Long> conceptModuleMap = new Long2ObjectOpenHashMap<>();
	private Set<Long> fullyDefinedConceptIds = new LongOpenHashSet();
	private Map<Long, Relationship> statedRelationshipsById = new HashMap<>();
	private Map<Long, Relationship> inferredRelationshipsById = new HashMap<>();
	private Map<Long, Set<Relationship>> conceptStatedRelationshipMap = new Long2ObjectOpenHashMap<>();
	private Map<Long, Set<Relationship>> conceptInferredRelationshipMap = new Long2ObjectOpenHashMap<>();
	private Map<Long, Set<Relationship>> conceptInactiveInferredRelationshipMap = new Long2ObjectOpenHashMap<>();

	// Axiom maps must be synchronised because international and extension refset members are loaded in parallel
	private Map<Long, List<OWLAxiom>> conceptAxiomMap = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
	private Map<String, OWLAxiom> axiomsById = new ConcurrentHashMap<>();

	private Map<Long, Set<Long>> inferredSubTypesMap = new Long2ObjectOpenHashMap<>();

	// Ungrouped roles map must be synchronised because international and extension refset members are loaded in parallel. The US Edition package contains the full MRCM.
	private Map<Long, Set<Long>> ungroupedRolesByContentType = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
	private Set<Long> inactivatedConcepts = new LongOpenHashSet();
	private Map<Long, Set<Description>> conceptDescriptionMap = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
	private Map<Long, Description> descriptionMap = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

	public static final Set<Long> DEFAULT_NEVER_GROUPED_ROLE_IDS = Collections.unmodifiableSet(Sets.newHashSet(
			parseLong(Concepts.PART_OF),
			parseLong(Concepts.LATERALITY),
			parseLong(Concepts.HAS_ACTIVE_INGREDIENT),
			parseLong(Concepts.HAS_DOSE_FORM)
	));

	private static final Logger LOGGER = LoggerFactory.getLogger(SnomedTaxonomy.class);

	public boolean isPrimitive(Long conceptId) {
		return !fullyDefinedConceptIds.contains(conceptId);
	}

	/**
	 * Returns with all the active source relationships of a concept given by its unique ID.
	 * @param conceptId the ID of the SNOMED&nbsp;CT concept.
	 * @return the active source relationships.
	 */
	public Collection<Relationship> getStatedRelationships(Long conceptId) {
		return conceptStatedRelationshipMap.getOrDefault(conceptId, Collections.emptySet());
	}
	
	public Set<Relationship> getInferredRelationships(Long conceptId) {
		return conceptInferredRelationshipMap.getOrDefault(conceptId, Collections.emptySet());
	}

	public Set<Relationship> getInactiveInferredRelationships(Long conceptId) {
		return conceptInactiveInferredRelationshipMap.getOrDefault(conceptId, Collections.emptySet());
	}

	public synchronized void addOrModifyRelationship(boolean stated, long conceptId, Relationship relationship) {
		// Have we seen this relationship before ie we need to modify it?
		Relationship existingRelationship = stated ? statedRelationshipsById.get(relationship.getRelationshipId())
				: inferredRelationshipsById.get(relationship.getRelationshipId());

		if (existingRelationship != null) {
			// Only effectiveTime and groupId are mutable
			existingRelationship.setEffectiveTime(relationship.getEffectiveTime());
			existingRelationship.setGroup(relationship.getGroup());
		} else {
			// add relationship
			if (stated) {
				conceptStatedRelationshipMap.computeIfAbsent(conceptId, k -> new HashSet<>()).add(relationship);
				statedRelationshipsById.put(relationship.getRelationshipId(), relationship);
			} else {
				conceptInferredRelationshipMap.computeIfAbsent(conceptId, k -> new HashSet<>()).add(relationship);
				inferredRelationshipsById.put(relationship.getRelationshipId(), relationship);
				if (relationship.getTypeId() == Concepts.IS_A_LONG) {
					inferredSubTypesMap.computeIfAbsent(relationship.getDestinationId(), k -> new HashSet<>()).add(conceptId);
				}
			}
		}
	}

	public synchronized void addInactiveInferredRelationship(long conceptId, Relationship relationship) {
		conceptInactiveInferredRelationshipMap.computeIfAbsent(conceptId, k -> new HashSet<>()).add(relationship);
	}

	public Set<Long> getDescendants(Long ancestor) {
		Set<Long> descendants = new HashSet<>();
		for (Long conceptId : allConceptIds) {
			if (conceptHasAncestor(conceptId, ancestor)) {
				descendants.add(conceptId);
			}
		}
		return descendants;
	}

	private boolean conceptHasAncestor(long conceptId, long ancestor) {
		return conceptHasAncestor(conceptId, ancestor, 0);
	}

	private boolean conceptHasAncestor(long conceptId, long ancestor, long depth) {
		if (conceptId == Concepts.ROOT_LONG) {
			return false;
		}
		
		//TODO Temporary code to find out why we're seeing a recursive hierarchy
		if (depth > 30) {
			throw new RuntimeException("Depth limit exceeded searching for potential ancestor " + ancestor + " of concept " + conceptId ); 
		}

		// Check all ancestors for the attribute concept
		for (Relationship relationship : conceptStatedRelationshipMap.getOrDefault(conceptId, Collections.emptySet())) {
			if (relationship.getTypeId() == Concepts.IS_A_LONG) {
				return relationship.getDestinationId() == ancestor || conceptHasAncestor(relationship.getDestinationId(), ancestor, ++depth);
			}
		}
		return false;
	}

	public Set<Long> getSuperTypeIds(long conceptId) {
		if (conceptId == Concepts.ROOT_LONG) {
			return Collections.emptySet();
		}

		Set<Long> superTypes = new HashSet<>();
		for (Relationship relationship : conceptStatedRelationshipMap.get(conceptId)) {
			if (relationship.getTypeId() == Concepts.IS_A_LONG) {
				superTypes.add(relationship.getDestinationId());
			}
		}
		return superTypes;
	}

	public Collection<Relationship> getNonIsAStatements(Long conceptId) {
		return conceptStatedRelationshipMap.getOrDefault(conceptId, Collections.emptySet()).stream().filter(f -> f.getTypeId() != Concepts.IS_A_LONG).collect(Collectors.toList());
	}

	public Set<Long> getSubTypeIds(long conceptId) {
		Set<Long> longs = inferredSubTypesMap.get(conceptId);
		return longs != null ? longs : Collections.emptySet();
	}

	public boolean isExhaustive(long conceptId) {
		// TODO: is this always false?
		return false;
	}

	public Set<Long> getAllConceptIds() {
		return allConceptIds;
	}

	public Map<Long, Long> getConceptModuleMap() {
		return conceptModuleMap;
	}

	public Set<Long> getFullyDefinedConceptIds() {
		return fullyDefinedConceptIds;
	}

	public Set<Long> getConceptIdSet() {
		return allConceptIds;
	}

	public Collection<Relationship> getInferredRelationships(long conceptId) {
		return conceptInferredRelationshipMap.getOrDefault(conceptId, Collections.emptySet());
	}

	public synchronized void removeRelationship(boolean stated, String sourceId, String relationshipIdStr) {
		long relationshipId = parseLong(relationshipIdStr);
		if (stated) {
			getStatedRelationships(parseLong(sourceId)).removeIf(relationship -> relationshipId == relationship.getRelationshipId());
			statedRelationshipsById.remove(relationshipId);
		} else {
			getInferredRelationships(parseLong(sourceId)).removeIf(relationship -> relationshipId == relationship.getRelationshipId());
			inferredRelationshipsById.remove(relationshipId);
		}
	}

	public void addOntologyNamespace(String id, String namespace) {
		ontologyNamespaces.put(id, namespace);
	}

	public void removeOntologyNamespace(String id) {
		ontologyNamespaces.remove(id);
	}

	public Map<String, String> getOntologyNamespaces() {
		return ontologyNamespaces;
	}

	public void addOntologyHeader(String id, String namespace) {
		ontologyHeader.put(id, namespace);
	}

	public void removeOntologyHeader(String id) {
		ontologyHeader.remove(id);
	}

	public Map<String, String> getOntologyHeader() {
		return ontologyHeader;
	}

	public void addAxiom(String referencedComponentId, String axiomId, OWLAxiom owlAxiom) {
		// Manually remove any existing axiom by axiomId.
		// We can't use the natural behaviour of a Java Set because the OWLAxiom does not use the axiomId in the equals method.
		OWLAxiom existingAxiomVersion = axiomsById.get(axiomId);
		List<OWLAxiom> conceptAxioms = conceptAxiomMap.computeIfAbsent(parseLong(referencedComponentId), id -> new ArrayList<>());
		if (existingAxiomVersion != null) {
			conceptAxioms.remove(existingAxiomVersion);
		}
		conceptAxioms.add(owlAxiom);
		axiomsById.put(axiomId, owlAxiom);
	}

	public void removeAxiom(String referencedComponentId, String id) {
		// Find the previously loaded axiom by id so that it can be removed from the set of axioms on the concept
		OWLAxiom owlAxiomToRemove = axiomsById.remove(id);
		if (owlAxiomToRemove != null) {
			conceptAxiomMap.get(parseLong(referencedComponentId)).remove(owlAxiomToRemove);
		}
	}

	void addDescription(String conceptId, String id, String term, String typeId, String languageCode) {
		Description description = new Description(id, term, typeId, languageCode);
		conceptDescriptionMap.computeIfAbsent(parseLong(conceptId), key -> new HashSet<>()).add(description);
		descriptionMap.put(parseLong(id), description);
	}

	void removeDescription(String conceptId, String id) {
		conceptDescriptionMap.computeIfAbsent(parseLong(conceptId), key -> new HashSet<>())
				.remove(new Description(id));
	}

	void setDescriptionAcceptability(String descriptionId, String refsetId, String acceptabilityId, boolean active) {
		Description description = descriptionMap.get(parseLong(descriptionId));
		if (description == null) {
			LOGGER.warn("Reference set {} references description {} which is not active or does not exist.", refsetId, descriptionId);
			return;
		}
		description.setAcceptability(parseLong(refsetId), parseLong(acceptabilityId), active);
	}

	public Set<Description> getConceptDescriptions(Long conceptId) {
		return conceptDescriptionMap.getOrDefault(conceptId, Collections.emptySet());
	}

	public void addUngroupedRole(Long contentType, Long attributeId) {
		ungroupedRolesByContentType.computeIfAbsent(contentType, type -> new HashSet<>()).add(attributeId);
	}

	public void removeUngroupedRole(Long contentType, Long attributeId) {
		Set<Long> ungrouped = ungroupedRolesByContentType.get(contentType);
		if (ungrouped != null) {
			ungrouped.remove(attributeId);
		}
	}

	public Map<Long, Set<Long>> getUngroupedRolesByContentType() {
		return ungroupedRolesByContentType;
	}

	public Set<Long> getUngroupedRolesForContentTypeOrDefault(Long contentTypeId) {
		Set<Long> subTypeIds = getSubTypeIds(contentTypeId);
		Set<Long> ungrouped = new HashSet<>();
		for (Long subTypeId : subTypeIds) {
			ungrouped.addAll(ungroupedRolesByContentType.getOrDefault(subTypeId, new HashSet<>()));
		}
		ungrouped.addAll(ungroupedRolesByContentType.getOrDefault(contentTypeId, new HashSet<>()));

		if (!ungrouped.isEmpty()) {
			LOGGER.info("Using never grouped role list from MRCM reference set {}", ungrouped);
		} else {
			ungrouped = SnomedTaxonomy.DEFAULT_NEVER_GROUPED_ROLE_IDS;
			LOGGER.info("No MRCM information found, falling back to legacy never grouped role list {}", ungrouped);
		}
		return ungrouped;
	}

	public Map<Long, List<OWLAxiom>> getConceptAxiomMap() {
		return conceptAxiomMap;
	}

	public Set<Long> getInactivatedConcepts() {
		return inactivatedConcepts;
	}

	public Map<Long, Relationship> getStatedRelationships() {
		return statedRelationshipsById;
	}

	public Long getAxiomCount() {
		AtomicLong axiomCount = new AtomicLong();
		conceptAxiomMap.values().forEach(axioms -> axiomCount.addAndGet(axioms.size()));
		return axiomCount.get();
	}

	public Map<String, OWLAxiom> getAxiomsById() {
		return axiomsById;
	}

	//WARREN CHANGE
	public Map<Long, Set<Description>> getConceptDescriptionMap() {
		return conceptDescriptionMap;
	}
}
