package org.cytoscape.cyndex2.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.cytoscape.cyndex2.internal.util.UrlUtils;
import org.junit.Test;

public class ServerManagerUrlTest {

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
	public void addHttpsProtocol_localhost_port() {
		assertEquals("https://localhost:8080", UrlUtils.addHttpsProtocol("localhost:8080"));
	}

	@Test
	public void addHttpsProtocol_already_http() {
		assertEquals("http://example.com", UrlUtils.addHttpsProtocol("http://example.com"));
	}

	@Test
	public void getBaseRoute_localhost_port() {
		assertEquals("http://localhost:8080/v2", UrlUtils.getBaseRoute("localhost:8080"));
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
	public void hasHttpScheme_localhost_port_is_false() {
		assertFalse(UrlUtils.hasHttpScheme("localhost:8080"));
	}

	@Test
	public void hasHttpScheme_ip_port_is_false() {
		assertFalse(UrlUtils.hasHttpScheme("127.0.0.1:9999"));
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
	public void hasHttpScheme_null_is_false() {
		assertFalse(UrlUtils.hasHttpScheme(null));
	}
}
