package org.cytoscape.cyndex2.internal.util;

import org.cytoscape.cyndex2.internal.rest.NdexAdminStatusService;
import org.cytoscape.cyndex2.internal.rest.NdexV3AdminStatus;

/**
 * Checks that an NDEx server speaks the v3 API, which folders, visibility-on-create and file search
 * all require.
 *
 * The probe is injected rather than called statically so tests can supply a lambda instead of an
 * HTTP mock; {@link org.cytoscape.cyndex2.internal.CyActivator} already builds one.
 */
public class NdexServerCapabilities {

	public static final String MINIMUM_VERSION_MESSAGE =
			"requires a minimum of NDEx v3.0.0";

	private final NdexAdminStatusService adminStatus;

	public NdexServerCapabilities(final NdexAdminStatusService adminStatus) {
		this.adminStatus = adminStatus;
	}

	/**
	 * @param serverUrl an NDEx server URL, with or without a trailing {@code /v2} or {@code /v3}
	 * @throws IllegalStateException if the server does not answer the v3 status endpoint
	 */
	public void requireV3(final String serverUrl) {
		if (!supportsV3(serverUrl)) {
			throw new IllegalStateException(
					"NDEx server '" + serverUrl + "' " + MINIMUM_VERSION_MESSAGE + ".");
		}
	}

	public boolean supportsV3(final String serverUrl) {
		if (serverUrl == null || serverUrl.isEmpty()) {
			return false;
		}
		final NdexV3AdminStatus status = adminStatus.fetch(UrlUtils.stripApiVersion(serverUrl));
		return status != null;
	}
}
