package org.cytoscape.cyndex2.internal.util;

import org.cytoscape.cyndex2.internal.CyActivator;

public class UserAgentUtil {
	public static String getCyNDExUserAgent() {
		return CyActivator.getAppName() + "/" + CyActivator.getAppVersion();
	}

	public static String getCytoscapeUserAgent() {
		return "Cytoscape/" + cytoscapeVersion();
	}

	/**
	 * The version comes from a CyProperty the activator wires up, so it is unavailable outside a started
	 * bundle. A missing user agent is not worth failing a request over -- it is a header, not a credential.
	 */
	private static String cytoscapeVersion() {
		try {
			final String version = CyActivator.getCytoscapeVersion();
			return version == null ? "unknown" : version;
		} catch (final RuntimeException e) {
			return "unknown";
		}
	}
	
	public static String getUserAgent() {
		return getCytoscapeUserAgent() + " " + getCyNDExUserAgent();
	}
}
