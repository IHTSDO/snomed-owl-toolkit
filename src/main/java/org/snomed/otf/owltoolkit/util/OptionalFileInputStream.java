package org.snomed.otf.owltoolkit.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class OptionalFileInputStream implements AutoCloseable {

	private final Optional<InputStream> inputStream;

	public OptionalFileInputStream(java.io.File file) throws FileNotFoundException {
		if (file != null) {
			// Open stream as auto-closable field
			this.inputStream = Optional.of(new FileInputStream(file));// lgtm [java/input-resource-leak]
		} else {
			this.inputStream = Optional.empty();
		}
	}

	public Optional<InputStream> getInputStream() {
		return inputStream;
	}

	@Override
	public void close() throws IOException {
		if (inputStream.isPresent()) {
			inputStream.get().close();
		}
	}
}
