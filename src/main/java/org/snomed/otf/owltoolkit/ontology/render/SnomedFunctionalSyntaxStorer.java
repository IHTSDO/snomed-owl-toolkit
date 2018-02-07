package org.snomed.otf.owltoolkit.ontology.render;

import org.semanticweb.owlapi.functional.renderer.FunctionalSyntaxObjectRenderer;
import org.semanticweb.owlapi.functional.renderer.FunctionalSyntaxStorer;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Writer;

public class SnomedFunctionalSyntaxStorer extends FunctionalSyntaxStorer {

	@Override
	public boolean canStoreOntology(OWLDocumentFormat ontologyFormat) {
		return ontologyFormat instanceof SnomedFunctionalSyntaxDocumentFormat;
	}

	@Override
	protected void storeOntology(@Nonnull OWLOntology ontology, @Nonnull Writer writer, OWLDocumentFormat format) throws OWLOntologyStorageException {
		try {
			FunctionalSyntaxObjectRenderer renderer = new FunctionalSyntaxObjectRenderer(ontology, writer);

			// Force ontology prefix manager into renderer
			if (format instanceof PrefixManager) {
				renderer.setPrefixManager((PrefixManager) format);
			}

			ontology.accept(renderer);
			writer.flush();
		} catch (IOException e) {
			throw new OWLOntologyStorageException(e);
		}
	}
}
