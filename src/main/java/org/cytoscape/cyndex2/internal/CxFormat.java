package org.cytoscape.cyndex2.internal;

/**
 * The CX serialization formats CyNDEx-2 can transfer, paired with the CX Support service ids that
 * provide a reader and writer for each.
 *
 * Pick a format explicitly wherever CyNDEx-2 controls it: CX2 for a single network over the NDEx v3
 * API, CX1 for a collection, which CX2 cannot represent because it carries no sibling sub-networks.
 * Where the format is the caller's choice instead — a CX stream posted to the REST API — do not use
 * this enum; let {@code CyNetworkReaderManager} negotiate a reader from the stream's header.
 */
public enum CxFormat {

	CX1("cytoscapeCxNetworkReaderFactory", "cxNetworkWriterFactory"),

	CX2("cytoscapeCx2NetworkReaderFactory", "cx2NetworkWriterFactory");

	private final String readerId;
	private final String writerId;

	private CxFormat(final String readerId, final String writerId) {
		this.readerId = readerId;
		this.writerId = writerId;
	}

	public String getReaderId() {
		return readerId;
	}

	public String getWriterId() {
		return writerId;
	}

	/**
	 * The CX Support version that first registers this format's reader and writer, for error messages
	 * telling the user what to install. CX2 arrived in 2.8.0; CX1 predates every version we support.
	 */
	public String getRequiredCxSupportVersion() {
		return this == CX2 ? "2.8.0" : "2.6.0";
	}
}
