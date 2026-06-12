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
		if (url == null || url.isEmpty()) return url;
		if (hasHttpScheme(url)) return url;
		// Don't mangle URLs that already carry a non-http scheme (e.g. ftp://)
		if (url.contains("://")) return url;
		return "http://" + url;
	}

	public static String addHttpsProtocol(String url) {
		if (url == null || url.isEmpty()) return url;
		if (hasHttpScheme(url)) return url;
		if (url.contains("://")) return url;
		return "https://" + url;
	}

	public static String getBaseRoute(String url) {
		if (url == null || url.isEmpty()) return url;
		String withProto = addHttpProtocol(url);
		return (withProto.endsWith("/v2") || withProto.endsWith("/v2/")) ? withProto : withProto + "/v2";
	}
}
