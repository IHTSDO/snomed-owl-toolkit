package org.snomed.otf.owltoolkit.ontology.render;

import org.semanticweb.owlapi.functional.renderer.FunctionalSyntaxStorerFactory;
import org.semanticweb.owlapi.model.OWLStorer;

public class SnomedFunctionalSyntaxStorerFactory extends FunctionalSyntaxStorerFactory {

	@Override
	public OWLStorer createStorer() {
		return new SnomedFunctionalSyntaxStorer();
	}
}
