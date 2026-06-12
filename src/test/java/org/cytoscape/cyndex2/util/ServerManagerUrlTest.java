package org.cytoscape.cyndex2.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.cytoscape.cyndex2.internal.util.UrlUtils;
import org.junit.Test;

public class ServerManagerUrlTest {

	// ── addHttpProtocol ───────────────────────────────────────────────────────

	@Test
	public void addHttpProtocol_localhost_port() {
		assertEquals("http://localhost:8080", UrlUtils.addHttpProtocol("localhost:8080"));
	}

	@Test
	public void addHttpProtocol_ip_port() {
		assertEquals("http://127.0.0.1:9999", UrlUtils.addHttpProtocol("127.0.0.1:9999"));
	}

	@Test
	public void addHttpProtocol_plain_host() {
		assertEquals("http://example.com", UrlUtils.addHttpProtocol("example.com"));
	}

	@Test
	public void addHttpProtocol_already_http() {
		assertEquals("http://example.com", UrlUtils.addHttpProtocol("http://example.com"));
	}

	@Test
	public void addHttpProtocol_already_https() {
		assertEquals("https://example.com", UrlUtils.addHttpProtocol("https://example.com"));
	}

	@Test
	public void addHttpProtocol_null_returns_null() {
		assertNull(UrlUtils.addHttpProtocol(null));
	}

	@Test
	public void addHttpProtocol_empty_returns_empty() {
		assertEquals("", UrlUtils.addHttpProtocol(""));
	}

	@Test
	public void addHttpProtocol_ftp_scheme_unchanged() {
		// Must not produce "http://ftp://host" — leave non-http schemes alone
		assertEquals("ftp://example.com", UrlUtils.addHttpProtocol("ftp://example.com"));
	}

	// ── addHttpsProtocol ──────────────────────────────────────────────────────

	@Test
	public void addHttpsProtocol_localhost_port() {
		assertEquals("https://localhost:8080", UrlUtils.addHttpsProtocol("localhost:8080"));
	}

	@Test
	public void addHttpsProtocol_already_http() {
		assertEquals("http://example.com", UrlUtils.addHttpsProtocol("http://example.com"));
	}

	@Test
	public void addHttpsProtocol_already_https_idempotent() {
		assertEquals("https://example.com", UrlUtils.addHttpsProtocol("https://example.com"));
	}

	@Test
	public void addHttpsProtocol_null_returns_null() {
		assertNull(UrlUtils.addHttpsProtocol(null));
	}

	@Test
	public void addHttpsProtocol_empty_returns_empty() {
		assertEquals("", UrlUtils.addHttpsProtocol(""));
	}

	@Test
	public void addHttpsProtocol_ftp_scheme_unchanged() {
		assertEquals("ftp://example.com", UrlUtils.addHttpsProtocol("ftp://example.com"));
	}

	// ── getBaseRoute ──────────────────────────────────────────────────────────

	@Test
	public void getBaseRoute_localhost_port() {
		assertEquals("http://localhost:8080/v2", UrlUtils.getBaseRoute("localhost:8080"));
	}

	@Test
	public void getBaseRoute_ip_port() {
		assertEquals("http://127.0.0.1:9999/v2", UrlUtils.getBaseRoute("127.0.0.1:9999"));
	}

	@Test
	public void getBaseRoute_plain_host() {
		assertEquals("http://example.com/v2", UrlUtils.getBaseRoute("example.com"));
	}

	@Test
	public void getBaseRoute_already_http() {
		assertEquals("http://example.com/v2", UrlUtils.getBaseRoute("http://example.com"));
	}

	@Test
	public void getBaseRoute_https_plain_host() {
		assertEquals("https://example.com/v2", UrlUtils.getBaseRoute("https://example.com"));
	}

	@Test
	public void getBaseRoute_custom_subpath() {
		// Custom path prefix — /v2 is appended after it
		assertEquals("https://host/mypath/v2", UrlUtils.getBaseRoute("https://host/mypath"));
	}

	@Test
	public void getBaseRoute_already_has_v2_no_double_append() {
		assertEquals("http://localhost:8080/v2", UrlUtils.getBaseRoute("http://localhost:8080/v2"));
	}

	@Test
	public void getBaseRoute_already_has_v2_with_scheme_no_double_append() {
		assertEquals("https://example.com/v2", UrlUtils.getBaseRoute("https://example.com/v2"));
	}

	@Test
	public void getBaseRoute_trailing_slash_v2_idempotent() {
		assertEquals("http://localhost:8080/v2/", UrlUtils.getBaseRoute("http://localhost:8080/v2/"));
	}

	@Test
	public void getBaseRoute_null_returns_null() {
		assertNull(UrlUtils.getBaseRoute(null));
	}

	@Test
	public void getBaseRoute_empty_returns_empty() {
		assertEquals("", UrlUtils.getBaseRoute(""));
	}

	// ── hasHttpScheme ─────────────────────────────────────────────────────────

	@Test
	public void hasHttpScheme_localhost_port_is_false() {
		assertFalse(UrlUtils.hasHttpScheme("localhost:8080"));
	}

	@Test
	public void hasHttpScheme_ip_port_is_false() {
		assertFalse(UrlUtils.hasHttpScheme("127.0.0.1:9999"));
	}

	@Test
	public void hasHttpScheme_bare_fqdn_is_false() {
		assertFalse(UrlUtils.hasHttpScheme("www.ndexbio.org"));
	}

	@Test
	public void hasHttpScheme_ftp_is_false() {
		assertFalse(UrlUtils.hasHttpScheme("ftp://example.com"));
	}

	@Test
	public void hasHttpScheme_empty_is_false() {
		assertFalse(UrlUtils.hasHttpScheme(""));
	}

	@Test
	public void hasHttpScheme_http_is_true() {
		assertTrue(UrlUtils.hasHttpScheme("http://example.com"));
	}

	@Test
	public void hasHttpScheme_https_is_true() {
		assertTrue(UrlUtils.hasHttpScheme("https://example.com"));
	}

	@Test
	public void hasHttpScheme_uppercase_https_is_true() {
		assertTrue(UrlUtils.hasHttpScheme("HTTPS://example.com"));
	}

	@Test
	public void hasHttpScheme_null_is_false() {
		assertFalse(UrlUtils.hasHttpScheme(null));
	}
}
