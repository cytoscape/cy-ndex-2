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
	public void testHttpNotHttps() {
		NdexV3AdminStatus s = make("http://auth.example.org/register", "https://auth.example.org/reset");
		assertFalse(s.hasValidOAuthUrls());
	}

	@Test
	public void testBlankUrl() {
		NdexV3AdminStatus s = make("", "https://auth.example.org/reset");
		assertFalse(s.hasValidOAuthUrls());
	}

	@Test
	public void testIsValidHttpsUrl_null() {
		assertFalse(NdexV3AdminStatus.isValidHttpsUrl(null));
	}

	@Test
	public void testIsValidHttpsUrl_blank() {
		assertFalse(NdexV3AdminStatus.isValidHttpsUrl(""));
	}

	@Test
	public void testIsValidHttpsUrl_http() {
		assertFalse(NdexV3AdminStatus.isValidHttpsUrl("http://example.org"));
	}

	@Test
	public void testIsValidHttpsUrl_valid() {
		assertTrue(NdexV3AdminStatus.isValidHttpsUrl("https://example.org/path?q=1"));
	}

	@Test
	public void testIsValidHttpsUrl_uppercaseHttps() {
		assertTrue(NdexV3AdminStatus.isValidHttpsUrl("HTTPS://example.org"));
	}
}
