package org.cytoscape.cyndex2.internal;

import java.util.EnumMap;
import java.util.Map;

import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.write.CyNetworkViewWriterFactory;

/**
 * Tracks the CX reader and writer services published by the CX Support app, keyed by {@link CxFormat}.
 *
 * The OSGi service listeners registered in {@link CyActivator} feed every reader and writer factory
 * through here; the ones whose {@code id} property matches a known format are retained.
 */
public class CxTaskFactoryManager {

	private static final String ID_TAG = "id";

	private final Map<CxFormat, CyNetworkViewWriterFactory> writerFactories = new EnumMap<>(CxFormat.class);
	private final Map<CxFormat, InputStreamTaskFactory> readerFactories = new EnumMap<>(CxFormat.class);

	public static CxTaskFactoryManager INSTANCE = new CxTaskFactoryManager();

	/**
	 * Public so callers -- and tests -- can build a fresh, empty manager and hand it to whatever needs
	 * it, rather than mutating the shared {@link #INSTANCE}. The class is bundle-private either way.
	 */
	public CxTaskFactoryManager() {
	}

	public InputStreamTaskFactory getReaderFactory(final CxFormat format) {
		return readerFactories.get(format);
	}

	public CyNetworkViewWriterFactory getWriterFactory(final CxFormat format) {
		return writerFactories.get(format);
	}

	/**
	 * @deprecated prefer {@link #getReaderFactory(CxFormat)} so the CX version is explicit at the call site.
	 */
	@Deprecated
	public InputStreamTaskFactory getCxReaderFactory() {
		return getReaderFactory(CxFormat.CX1);
	}

	/**
	 * @deprecated prefer {@link #getWriterFactory(CxFormat)} so the CX version is explicit at the call site.
	 */
	@Deprecated
	public CyNetworkViewWriterFactory getCxWriterFactory() {
		return getWriterFactory(CxFormat.CX1);
	}

	@SuppressWarnings("rawtypes")
	public void addWriterFactory(final CyNetworkViewWriterFactory factory, final Map properties) {
		final CxFormat format = writerFormatFor(properties);
		if (format != null) {
			writerFactories.put(format, factory);
		}
	}

	@SuppressWarnings("rawtypes")
	public void removeWriterFactory(final CyNetworkViewWriterFactory factory, final Map properties) {
		final CxFormat format = writerFormatFor(properties);
		if (format != null) {
			writerFactories.remove(format);
		}
	}

	@SuppressWarnings("rawtypes")
	public void addReaderFactory(final InputStreamTaskFactory factory, final Map properties) {
		final CxFormat format = readerFormatFor(properties);
		if (format != null) {
			readerFactories.put(format, factory);
		}
	}

	@SuppressWarnings("rawtypes")
	public void removeReaderFactory(final InputStreamTaskFactory factory, final Map properties) {
		final CxFormat format = readerFormatFor(properties);
		if (format != null) {
			readerFactories.remove(format);
		}
	}

	@SuppressWarnings("rawtypes")
	private static CxFormat writerFormatFor(final Map properties) {
		final String id = idOf(properties);
		for (CxFormat format : CxFormat.values()) {
			if (format.getWriterId().equals(id)) {
				return format;
			}
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	private static CxFormat readerFormatFor(final Map properties) {
		final String id = idOf(properties);
		for (CxFormat format : CxFormat.values()) {
			if (format.getReaderId().equals(id)) {
				return format;
			}
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	private static String idOf(final Map properties) {
		return properties == null ? null : (String) properties.get(ID_TAG);
	}
}
