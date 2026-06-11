package org.cytoscape.cyndex2.internal.util;

import java.net.URI;
import java.net.URISyntaxException;

public final class UrlUtils {

	private UrlUtils() {}

	public static boolean hasHttpScheme(String url) {
		if (url == null) return false;
		try {
			String scheme = new URI(url).getScheme();
			return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
		} catch (URISyntaxException e) {
			return false;
		}
	}

	public static String addHttpProtocol(String url) {
		return hasHttpScheme(url) ? url : "http://" + url;
	}

	public static String addHttpsProtocol(String url) {
		return hasHttpScheme(url) ? url : "https://" + url;
	}

	public static String getBaseRoute(String url) {
		return addHttpProtocol(url) + "/v2";
	}
}
