package org.snomed.otf.owltoolkit.service;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.ihtsdo.otf.snomedboot.factory.ImpotentComponentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.lang.Long.parseLong;

public class RF2ExtractionWriter extends ImpotentComponentFactory implements AutoCloseable {

	public static final String TAB = "\t";

	private final File outputDirectory;
	private final Set<Long> conceptIds;
	private final Set<Long> descriptionIds;

	private final List<Writer> writers;
	private final BufferedWriter conceptWriter;
	private final BufferedWriter descriptionWriter;
	private final BufferedWriter languageReferenceSetWriter;
	private final BufferedWriter owlAxiomWriter;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public RF2ExtractionWriter(Set<Long> conceptIds, String dateString, File outputDirectory) throws IOException {
		this.conceptIds = new LongOpenHashSet(conceptIds);
		this.descriptionIds = new LongOpenHashSet();
		this.outputDirectory = outputDirectory;

		File snapshotDir = new File(outputDirectory, "Snapshot");
		createDirectoryOrThrow(snapshotDir);

		File terminologyDir = new File(snapshotDir, "Terminology");
		createDirectoryOrThrow(terminologyDir);

		File refsetDir = new File(snapshotDir, "Refset");
		createDirectoryOrThrow(refsetDir);

		File langRefsetDir = new File(refsetDir, "Language");
		createDirectoryOrThrow(langRefsetDir);

		writers = new ArrayList<>();

		conceptWriter = newRF2Writer(
				terminologyDir,
				String.format("sct2_Concept_Snapshot_INT_%s.txt", dateString),
				String.join(TAB, "conceptId", "effectiveTime", "active", "moduleId", "definitionStatusId"));

		descriptionWriter = newRF2Writer(
				terminologyDir,
				String.format("sct2_Description_Snapshot_INT_%s.txt", dateString),
				String.join(TAB, "id", "effectiveTime", "active", "moduleId", "conceptId", "languageCode", "typeId", "term", "caseSignificanceId"));

		languageReferenceSetWriter = newRF2Writer(
				langRefsetDir,
				String.format("der2_cRefset_LanguageSnapshot-en_INT_%s.txt", dateString),
				String.join(TAB, "id", "effectiveTime", "active", "moduleId", "refsetId", "referencedComponentId", "acceptabilityId"));

		owlAxiomWriter = newRF2Writer(
				terminologyDir,
				String.format("sct2_sRefset_OWLExpressionSnapshot_INT_%s.txt", dateString),
				String.join(TAB, "id", "effectiveTime", "active", "moduleId", "refsetId", "referencedComponentId", "owlExpression"));
	}

	private void createDirectoryOrThrow(File snapshotDir) throws IOException {
		if (!snapshotDir.exists() && !snapshotDir.mkdirs()) {
			throw new IOException("Failed to create directory " + snapshotDir.getPath());
		}
	}

	private BufferedWriter newRF2Writer(File terminologyDir, String filename, String header) throws IOException {
		final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(new File(terminologyDir, filename)));
		writers.add(fileWriter);
		// Write header
		fileWriter.write(header);
		fileWriter.newLine();
		return fileWriter;
	}

	@Override
	public void newConceptState(String conceptId, String effectiveTime, String active, String moduleId, String definitionStatusId) {
		if (conceptIds.contains(parseLong(conceptId))) {
			try {
				conceptWriter.write(String.join(TAB, conceptId, effectiveTime, active, moduleId, definitionStatusId));
				conceptWriter.newLine();
			} catch (IOException e) {
				// Ugly but the interface does not allow throwing a checked exception
				throw new RuntimeException("Failed to write to concept file.", e);
			}
		}
	}

	@Override
	public void newDescriptionState(String id, String effectiveTime, String active, String moduleId, String conceptId, String languageCode, String typeId, String term, String caseSignificanceId) {
		if (conceptIds.contains(parseLong(conceptId))) {
			descriptionIds.add(parseLong(id));
			try {
				descriptionWriter.write(String.join(TAB, id, effectiveTime, active, moduleId, conceptId, languageCode, typeId, term, caseSignificanceId));
				descriptionWriter.newLine();
			} catch (IOException e) {
				throw new RuntimeException("Failed to write to description file.", e);
			}
		}
	}

	@Override
	public void newReferenceSetMemberState(String[] fieldNames, String id, String effectiveTime, String active, String moduleId, String refsetId, String referencedComponentId, String... otherValues) {
		if (fieldNames.length == 7 && fieldNames[6].equals("acceptabilityId")) {
			if (descriptionIds.contains(parseLong(referencedComponentId))) {
				try {
					languageReferenceSetWriter.write(String.join(TAB, id, effectiveTime, active, moduleId, refsetId, referencedComponentId, otherValues[0]));
					languageReferenceSetWriter.newLine();
				} catch (IOException e) {
					throw new RuntimeException("Failed to write to language refset file.", e);
				}
			}
		}
		if (fieldNames.length == 7 && fieldNames[6].equals("owlExpression")) {
			if (conceptIds.contains(parseLong(referencedComponentId))) {
				try {
					owlAxiomWriter.write(String.join(TAB, id, effectiveTime, active, moduleId, refsetId, referencedComponentId, otherValues[0]));
					owlAxiomWriter.newLine();
				} catch (IOException e) {
					throw new RuntimeException("Failed to write to OWL axiom refset file.", e);
				}
			}
		}
	}

	@Override
	public void close() throws IOException {
		List<IOException> exceptions = new ArrayList<>();
		for (Writer writer : writers) {
			try {
				writer.close();// Also flushes.
			} catch (IOException e) {
				logger.error("Failed to close extract file writer. {}", e.getMessage());
				exceptions.add(e);
			}
		}
		if (!exceptions.isEmpty()) {
			throw exceptions.iterator().next();
		}
	}
}
