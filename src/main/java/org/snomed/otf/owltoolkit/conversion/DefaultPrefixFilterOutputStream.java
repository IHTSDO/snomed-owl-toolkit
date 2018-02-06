package org.snomed.otf.owltoolkit.conversion;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.snomed.otf.owltoolkit.ontology.OntologyService.CORE_COMPONENT_NAMESPACE_PATTERN;

final class DefaultPrefixFilterOutputStream extends FilterOutputStream {

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	public static final String PREFIX = "Prefix";
	private final StringBuffer buffer;

	public DefaultPrefixFilterOutputStream(OutputStream out) {
		super(out);
		this.out = out;
		buffer = new StringBuffer();
	}

	@Override
	public void write(int b) throws IOException {
		byte[] bytes = {(byte)b};
		write(bytes);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		String part = new String(Arrays.copyOfRange(b, off, len), UTF8_CHARSET);
		buffer.append(part);
		if (part.contains("\n")) {
			flush();
		}
	}

	@Override
	public void flush() throws IOException {
		out.write(buffer.toString().replaceAll(CORE_COMPONENT_NAMESPACE_PATTERN, ":$1").getBytes());
		buffer.setLength(0);
		out.flush();
	}

}
