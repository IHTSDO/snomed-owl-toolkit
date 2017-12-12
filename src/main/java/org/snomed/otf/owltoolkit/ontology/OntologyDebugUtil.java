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
package org.snomed.otf.owltoolkit.ontology;

import org.semanticweb.owlapi.functional.renderer.OWLFunctionalSyntaxRenderer;
import org.semanticweb.owlapi.io.OWLRendererException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OntologyDebugUtil {

	private static final Logger logger = LoggerFactory.getLogger(OntologyDebugUtil.class);

	public static void serialiseOntologyForDebug(String classificationId, OWLOntology ontology) {
		OWLFunctionalSyntaxRenderer ontologyRenderer = new OWLFunctionalSyntaxRenderer();
		try {
			File classificationsDirectory = new File("debug");
			classificationsDirectory.mkdirs();
			File owlFile = new File(classificationsDirectory, new Date().getTime() + "_" + classificationId + ".owl");
			logger.info("Serialising OWL Ontology before classification to file {}", owlFile.getAbsolutePath());
			try (FileWriter fileWriter = new FileWriter(owlFile)) {
				ontologyRenderer.render(ontology, fileWriter);
			}

			// If names file exists insert names
			File namesFile = new File("debug/names.txt");
			if (namesFile.isFile()) {
				Map<String, String> names = new HashMap<>();
				try (BufferedReader reader = new BufferedReader(new FileReader(namesFile))) {
					String line;
					while ((line = reader.readLine()) != null) {
						String[] split = line.split("\\t");
						names.putIfAbsent(split[0], split[1].replace(" ", "_"));
					}
				}
				File owlFileWithNames = new File(owlFile.getAbsolutePath().replace(".owl", "-with-names.owl"));
				try (BufferedReader reader = new BufferedReader(new FileReader(owlFile));
					 BufferedWriter writer = new BufferedWriter(new FileWriter(owlFileWithNames))) {
					String line;
					while ((line = reader.readLine()) != null) {
						for (String conceptId : names.keySet()) {
							line = line.replace(conceptId, conceptId + "_" + names.get(conceptId));
						}
						writer.write(line);
						writer.newLine();
					}
				}
			}
		} catch (OWLRendererException | IOException e) {
			logger.error("Failed to serialise OWL Ontology.", e);
		}
	}
}
