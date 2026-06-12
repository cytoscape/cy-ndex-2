package org.cytoscape.cyndex2.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.cytoscape.cyndex2.internal.rest.NdexV3AdminStatus;
import org.junit.Test;

public class NdexV3AdminStatusTest {

	private static NdexV3AdminStatus make(String registerUrl, String resetUrl) {
		NdexV3AdminStatus s = new NdexV3AdminStatus();
		s.setOauthRegisterUrl(registerUrl);
		s.setOauthResetUrl(resetUrl);
		return s;
	}

	@Test
	public void testBothUrlsValid() {
		NdexV3AdminStatus s = make("https://auth.example.org/register", "https://auth.example.org/reset");
		assertTrue(s.hasValidOAuthUrls());
	}

	@Test
	public void testNullStatus() {
		// Calling code must guard against null — verify the POJO itself handles null fields
		NdexV3AdminStatus s = new NdexV3AdminStatus();
		assertFalse(s.hasValidOAuthUrls());
	}

	@Test
	public void testMissingRegisterUrl() {
		NdexV3AdminStatus s = make(null, "https://auth.example.org/reset");
		assertFalse(s.hasValidOAuthUrls());
	}

	@Test
	public void testMissingResetUrl() {
		NdexV3AdminStatus s = make("https://auth.example.org/register", null);
		assertFalse(s.hasValidOAuthUrls());
	}

	@Test
	public void testBothNull() {
		NdexV3AdminStatus s = make(null, null);
		assertFalse(s.hasValidOAuthUrls());
	}

	@Test
	public void testHttpUrls() {
		NdexV3AdminStatus s = make("http://auth.example.org/register", "http://auth.example.org/reset");
		assertTrue(s.hasValidOAuthUrls());
	}

	@Test
	public void testBlankUrl() {
		NdexV3AdminStatus s = make("", "https://auth.example.org/reset");
		assertFalse(s.hasValidOAuthUrls());
	}

	@Test
	public void testIsValidUrl_null() {
		assertFalse(NdexV3AdminStatus.isValidUrl(null));
	}

	@Test
	public void testIsValidUrl_blank() {
		assertFalse(NdexV3AdminStatus.isValidUrl(""));
	}

	@Test
	public void testIsValidUrl_http() {
		assertTrue(NdexV3AdminStatus.isValidUrl("http://example.org"));
	}

	@Test
	public void testIsValidUrl_https() {
		assertTrue(NdexV3AdminStatus.isValidUrl("https://example.org/path?q=1"));
	}

	@Test
	public void testIsValidUrl_uppercaseHttps() {
		assertTrue(NdexV3AdminStatus.isValidUrl("HTTPS://example.org"));
	}

	@Test
	public void testIsValidUrl_ftp_rejected() {
		assertFalse(NdexV3AdminStatus.isValidUrl("ftp://example.org"));
	}

	@Test
	public void testIsValidUrl_javascript_rejected() {
		assertFalse(NdexV3AdminStatus.isValidUrl("javascript:alert(1)"));
	}

	@Test
	public void testIsValidUrl_noScheme_rejected() {
		assertFalse(NdexV3AdminStatus.isValidUrl("example.org"));
	}
}
