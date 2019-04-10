package org.snomed.otf.owltoolkit.util;

import com.google.common.collect.Sets;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InputStreamSet extends HashSet<InputStream> implements AutoCloseable {

	private final Set<InputStream> fileInputStreams;

	public InputStreamSet(java.io.File... files) throws FileNotFoundException {
		this(new HashSet<>(Arrays.asList(files)));
	}

	public InputStreamSet(Set<java.io.File> files) throws FileNotFoundException {
		fileInputStreams = new HashSet<>();
		for (File file : files) {
			// Open stream and add to the auto-closable collection
			fileInputStreams.add(new FileInputStream(file));// lgtm [java/input-resource-leak]
		}
	}

	public InputStreamSet(InputStream... inputStream) {
		fileInputStreams = Sets.newHashSet(inputStream);
	}

	@Override
	public void close() throws IOException {
		for (InputStream fileInputStream : fileInputStreams) {
			fileInputStream.close();
		}
	}

	public Set<InputStream> getFileInputStreams() {
		return fileInputStreams;
	}
}
