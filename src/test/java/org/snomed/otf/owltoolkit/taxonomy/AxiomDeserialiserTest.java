package org.snomed.otf.owltoolkit.taxonomy;

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLRuntimeException;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class AxiomDeserialiserTest {

	private static final String SECRET = "top-secret-file-content";

	private AxiomDeserialiser axiomDeserialiser;
	private HttpServer httpServer;
	private final AtomicInteger requestCount = new AtomicInteger();
	private File secretFile;

	@Before
	public void setUp() throws IOException {
		axiomDeserialiser = new AxiomDeserialiser();
		httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		httpServer.createContext("/", exchange -> {
			requestCount.incrementAndGet();
			byte[] body = SECRET.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		httpServer.start();
		secretFile = File.createTempFile("axiom-deserialiser-test", ".txt");
		Files.writeString(secretFile.toPath(), SECRET);
	}

	@After
	public void tearDown() {
		httpServer.stop(0);
		secretFile.delete();
	}

	@Test
	public void testValidAxiomsAccepted() throws OWLOntologyCreationException {
		OWLAxiom axiom = axiomDeserialiser.deserialiseAxiom("SubClassOf(:404684003 :138875005)", "1");
		assertNotNull(axiom);
		assertNotNull(axiomDeserialiser.deserialiseAxiom("  EquivalentClasses(:404684003 :138875005)", "2"));
		assertNotNull(axiomDeserialiser.deserialiseAxiom("SubAnnotationPropertyOf(:1295447006 :1295448001)", "3"));
		assertNotNull(axiomDeserialiser.deserialiseAxiom("TransitiveObjectProperty(:738774007)", "4"));
		assertNotNull(axiomDeserialiser.deserialiseAxiom("SubObjectPropertyOf(:363698007 :762705008)", "5"));
		assertNotNull(axiomDeserialiser.deserialiseAxiom("SubObjectPropertyOf(ObjectPropertyChain(:127489000 :738774007) :127489000)", "6"));
		assertNotNull(axiomDeserialiser.deserialiseAxiom("SubDataPropertyOf(:100000001001 :762706009)", "7"));
		assertNotNull(axiomDeserialiser.deserialiseAxiom("ReflexiveObjectProperty(:733928003)", "8"));
	}

	@Test
	public void testUnsupportedAxiomMessageNamesKeyword() {
		try {
			axiomDeserialiser.deserialiseAxiom("DisjointClasses(:404684003 :138875005)", "9");
			fail("Expected OWLOntologyCreationException");
		} catch (OWLOntologyCreationException e) {
			assertEquals("Unsupported axiom expression 'DisjointClasses' for axiom id 9", e.getMessage());
		}
	}

	@Test
	public void testParseErrorKeepsParserDetail() {
		try {
			axiomDeserialiser.deserialiseAxiom("SubClassOf(:404684003", null);
			fail("Expected OWLOntologyCreationException");
		} catch (OWLOntologyCreationException e) {
			assertTrue(e.getMessage(), e.getMessage().startsWith("Failed to parse axiom: "));
			assertTrue(e.getCause() instanceof OWLParserException);
		}
	}

	@Test
	public void testHttpImportRejectedWithoutFetching() {
		String url = "http://127.0.0.1:" + httpServer.getAddress().getPort() + "/ssrf";
		assertRejected("Import(<" + url + ">) SubClassOf(:404684003 :138875005)");
		assertRejected("SubClassOf(:404684003 :138875005)) Ontology(Import(<" + url + ">) SubClassOf(:404684003 :138875005)");
		assertRejected("SubClassOf(:404684003 :138875005) Import(<" + url + ">)");
		assertEquals("No request may reach the import URL", 0, requestCount.get());
	}

	@Test
	public void testFileImportRejectedWithoutDisclosure() {
		String fileUrl = secretFile.toURI().toString();
		assertRejected("Import(<" + fileUrl + ">)");
		assertRejected("SubClassOf(:404684003 :138875005)) Ontology(Import(<" + fileUrl + ">) SubClassOf(:404684003 :138875005)");
	}

	@Test
	public void testOntologyManagerDoesNotResolveImports() {
		String url = "http://127.0.0.1:" + httpServer.getAddress().getPort() + "/import";
		assertImportNotLoaded(url);
		assertImportNotLoaded(secretFile.toURI().toString());
		assertEquals("No request may reach the import URL", 0, requestCount.get());
	}

	private void assertImportNotLoaded(String importIri) {
		OWLOntologyManager manager = AxiomDeserialiser.createOntologyManager();
		try {
			manager.loadOntologyFromOntologyDocument(new StringDocumentSource(
					"Prefix(:=<http://snomed.info/id/>) Ontology(Import(<" + importIri + ">) SubClassOf(:404684003 :138875005))"));
			fail("Expected import of " + importIri + " to fail");
		} catch (OWLOntologyCreationException | OWLRuntimeException e) {
			assertEquals(0, manager.getOntologies().size());
		}
	}

	private void assertRejected(String owlExpression) {
		try {
			axiomDeserialiser.deserialiseAxiom(owlExpression, "test");
			fail("Expected OWLOntologyCreationException for: " + owlExpression);
		} catch (OWLOntologyCreationException e) {
			for (Throwable t = e; t != null; t = t.getCause()) {
				assertFalse("Exception must not carry fetched content", String.valueOf(t.getMessage()).contains(SECRET));
			}
		}
	}
}
