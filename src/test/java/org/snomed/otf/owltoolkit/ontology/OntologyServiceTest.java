package org.snomed.otf.owltoolkit.ontology;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomy;
import org.snomed.otf.owltoolkit.taxonomy.SnomedTaxonomyLoader;

import java.util.*;

import static org.junit.Assert.*;

public class OntologyServiceTest {

	private OntologyService ontologyService;

	@Before
	public void setup() {
		ontologyService = new OntologyService(SnomedTaxonomy.DEFAULT_NEVER_GROUPED_ROLE_IDS, OWLManager.createOWLOntologyManager());
	}

	@Test
	public void getPropertyChains() throws Exception {
		SnomedTaxonomyLoader snomedTaxonomyLoader = new SnomedTaxonomyLoader();

		addAttribute("100", snomedTaxonomyLoader);
		addAttribute("200", snomedTaxonomyLoader);
		addAttribute("300", snomedTaxonomyLoader);
		addAttribute("400", snomedTaxonomyLoader);

		// Add regular property chain
		snomedTaxonomyLoader.addActiveAxiom(UUID.randomUUID().toString(), "100", "SubObjectPropertyOf(ObjectPropertyChain(:100 :200) :300)");

		// Add transitive property (shortcut for property chain involving just that type)
		snomedTaxonomyLoader.addActiveAxiom(UUID.randomUUID().toString(), "400", "TransitiveObjectProperty(:400)");

		// Extract property chains from the ontology created from the Snomed taxonomy
		OWLOntology ontology = ontologyService.createOntology(snomedTaxonomyLoader.getSnomedTaxonomy());
		Set<PropertyChain> propertyChains = ontologyService.getPropertyChains(ontology);

		assertEquals(2, propertyChains.size());

		List<PropertyChain> chains = sorted(propertyChains);
		assertEquals("PropertyChain{sourceType=100, destinationType=200, inferredType=300}", chains.get(0).toString());
		assertEquals("PropertyChain{sourceType=400, destinationType=400, inferredType=400}", chains.get(1).toString());
	}

	private void addAttribute(String attribute, SnomedTaxonomyLoader snomedTaxonomyLoader) {
		snomedTaxonomyLoader.newConceptState(attribute, "", "1", Concepts.SNOMED_CT_CORE_MODULE, "");
		snomedTaxonomyLoader.newRelationshipState("101", "", "1", Concepts.SNOMED_CT_CORE_MODULE, attribute, Concepts.CONCEPT_MODEL_ATTRIBUTE, "0", Concepts.IS_A, Concepts.INFERRED_RELATIONSHIP, "");
	}

	private List<PropertyChain> sorted(Set<PropertyChain> propertyChains) {
		List<PropertyChain> chains = Lists.newArrayList(propertyChains);
		chains.sort(Comparator.comparing(PropertyChain::getSourceType));
		return chains;
	}

}