package org.snomed.otf.owltoolkit.ontology.render;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.util.Map;

public class SnomedPrefixManager extends DefaultPrefixManager {

	@Override
	public String getPrefixIRI(IRI iri) {
		Map<String, String> prefixName2PrefixMap = getPrefixName2PrefixMap();
		String iriString = iri.toString();
		for (Map.Entry<String, String> namePrefixEntry : prefixName2PrefixMap.entrySet()) {
			if (iriString.startsWith(namePrefixEntry.getValue())) {
				return namePrefixEntry.getKey() + iriString.substring(namePrefixEntry.getValue().length());
			}
		}
		return null;
	}
}
