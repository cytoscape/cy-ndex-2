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

	/**
	 * Strips a trailing NDEx API-version segment ({@code /v2} or {@code /v3}, with or without a trailing
	 * slash) so a stored profile URL can be used as a server root.
	 *
	 * Profile URLs are normalized through {@link #getBaseRoute(String)}, which appends {@code /v2}, and
	 * {@code Server.DEFAULT_SERVER} is literally {@code http://public.ndexbio.org/v2}. Anything that
	 * builds its own path from the server root — the {@code /v3/admin/status} probe, for instance —
	 * must strip that first, or it produces {@code .../v2/v3/admin/status}. Mirrors what
	 * {@code NdexRestClient} does internally with the same URLs.
	 */
	public static String stripApiVersion(String url) {
		if (url == null || url.isEmpty()) return url;
		String stripped = url;
		while (stripped.endsWith("/")) {
			stripped = stripped.substring(0, stripped.length() - 1);
		}
		final String lower = stripped.toLowerCase();
		if (lower.endsWith("/v2") || lower.endsWith("/v3")) {
			stripped = stripped.substring(0, stripped.length() - 3);
		}
		while (stripped.endsWith("/")) {
			stripped = stripped.substring(0, stripped.length() - 1);
		}
		return stripped;
	}

	public static String getBaseRoute(String url) {
		if (url == null || url.isEmpty()) return url;
		String withProto = addHttpProtocol(url);
		return (withProto.endsWith("/v2") || withProto.endsWith("/v2/")) ? withProto : withProto + "/v2";
	}
}
