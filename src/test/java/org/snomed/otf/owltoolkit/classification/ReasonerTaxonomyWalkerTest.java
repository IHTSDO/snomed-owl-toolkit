package org.snomed.otf.owltoolkit.classification;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.ontology.OntologyService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ReasonerTaxonomyWalkerTest {

	// Each level of the ladder has two concepts which are both children of both concepts on the level above,
	// so the number of paths from the root doubles with every level.
	private static final int LADDER_LEVELS = 12;

	private static final long ROOT = Concepts.ROOT_LONG;
	private static final long X1 = 1001L;
	private static final long X2 = 1002L;
	private static final long X3 = 1003L;
	private static final long Y1 = 1011L;
	private static final long C = 1021L;
	private static final long D = 1022L;

	private OWLDataFactory factory;
	private OWLOntology ontology;
	private OWLReasoner elkReasoner;
	private final AtomicInteger getSubClassesCalls = new AtomicInteger();

	@Before
	public void setup() throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		factory = manager.getOWLDataFactory();
		ontology = manager.createOntology();

		// The walker starts its attribute hierarchy from the concept model object attribute
		manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(
				factory.getOWLObjectProperty(IRI.create(OntologyService.SNOMED_CORE_COMPONENTS_URI + Concepts.CONCEPT_MODEL_OBJECT_ATTRIBUTE))));

		manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(owlClass(ROOT)));
		List<Long> levelAbove = List.of(ROOT);
		for (int level = 1; level <= LADDER_LEVELS; level++) {
			List<Long> levelConcepts = List.of(ladderA(level), ladderB(level));
			for (Long conceptId : levelConcepts) {
				for (Long parentId : levelAbove) {
					manager.addAxiom(ontology, subClassOf(conceptId, parentId));
				}
			}
			levelAbove = levelConcepts;
		}

		// C has direct parents at different depths, it must not be processed until the deeper parent has been
		manager.addAxiom(ontology, subClassOf(X1, ROOT));
		manager.addAxiom(ontology, subClassOf(X2, X1));
		manager.addAxiom(ontology, subClassOf(X3, X2));
		manager.addAxiom(ontology, subClassOf(Y1, ROOT));
		manager.addAxiom(ontology, subClassOf(C, Y1));
		manager.addAxiom(ontology, subClassOf(C, X3));
		manager.addAxiom(ontology, subClassOf(D, C));

		elkReasoner = new ElkReasonerFactory().createReasoner(ontology);
		elkReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
	}

	@After
	public void tearDown() {
		elkReasoner.dispose();
	}

	@Test
	public void testTaxonomyExtracted() {
		ReasonerTaxonomy taxonomy = new ReasonerTaxonomyWalker(countingReasoner(), new ReasonerTaxonomy()).walk();

		assertEquals(Set.of(ROOT), taxonomy.getParents(ladderA(1)));
		assertEquals(Set.of(ladderA(LADDER_LEVELS - 1), ladderB(LADDER_LEVELS - 1)), taxonomy.getParents(ladderB(LADDER_LEVELS)));
		Set<Long> deepestAncestors = taxonomy.getAncestors(ladderA(LADDER_LEVELS));
		assertEquals(2 * (LADDER_LEVELS - 1) + 1, deepestAncestors.size());
		assertTrue(deepestAncestors.contains(ROOT));
		assertTrue(deepestAncestors.contains(ladderB(1)));

		assertEquals(Set.of(Y1, X3), taxonomy.getParents(C));
		assertEquals(Set.of(C), taxonomy.getParents(D));
		assertEquals(Set.of(ROOT, X1, X2, X3, Y1, C), taxonomy.getAncestors(D));
	}

	@Test
	public void testEachConceptListedOnceAfterItsParents() {
		ReasonerTaxonomy taxonomy = new ReasonerTaxonomyWalker(countingReasoner(), new ReasonerTaxonomy()).walk();

		List<Long> conceptIds = taxonomy.getConceptIds();
		Set<Long> listed = new HashSet<>();
		for (Long conceptId : conceptIds) {
			if (taxonomy.getAttributeIds().contains(conceptId)) {
				continue;
			}
			assertTrue("Concept " + conceptId + " listed more than once", listed.add(conceptId));
			for (Long parentId : taxonomy.getParents(conceptId)) {
				assertTrue("Parent " + parentId + " of " + conceptId + " must be listed first", listed.contains(parentId));
			}
		}
		// Root, the ladder, X1-X3, Y1, C and D
		assertEquals(1 + 2 * LADDER_LEVELS + 6, listed.size());
	}

	@Test
	public void testSubtreesNotWalkedAgainForEachExtraParent() {
		new ReasonerTaxonomyWalker(countingReasoner(), new ReasonerTaxonomy()).walk();

		// Children are requested once for each concept plus once for the top node.
		// Walking the subtree again for every extra parent would make this grow with the number of paths (2^levels).
		int classCount = ontology.getClassesInSignature().size();
		assertTrue("getSubClasses called " + getSubClassesCalls.get() + " times for " + classCount + " classes",
				getSubClassesCalls.get() <= classCount + 1);
	}

	private OWLReasoner countingReasoner() {
		return (OWLReasoner) Proxy.newProxyInstance(OWLReasoner.class.getClassLoader(), new Class<?>[]{OWLReasoner.class},
				(proxy, method, args) -> {
					if (method.getName().equals("getSubClasses")) {
						getSubClassesCalls.incrementAndGet();
					}
					try {
						return method.invoke(elkReasoner, args);
					} catch (InvocationTargetException e) {
						throw e.getCause();
					}
				});
	}

	private static long ladderA(int level) {
		return 2000L + level * 10;
	}

	private static long ladderB(int level) {
		return 2000L + level * 10 + 1;
	}

	private OWLClass owlClass(long conceptId) {
		return factory.getOWLClass(IRI.create(OntologyService.SNOMED_CORE_COMPONENTS_URI + conceptId));
	}

	private OWLSubClassOfAxiom subClassOf(long conceptId, long parentId) {
		return factory.getOWLSubClassOfAxiom(owlClass(conceptId), owlClass(parentId));
	}
}
