package org.cytoscape.cyndex2.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * `Server.DEFAULT_SERVER` had no test coverage at all before the anonymous fallback started handing
 * it out, so its URL and its shape are pinned here.
 */
public class DefaultServerTest {

	@Test
	public void theDefaultServerIsThePublicNdexHostOverTls() {
		assertEquals("https://www.ndexbio.org", Server.DEFAULT_SERVER.getUrl());
	}

	@Test
	public void theDefaultServerCarriesNoCredentials() {
		assertNull(Server.DEFAULT_SERVER.getUsername());
		assertNull(Server.DEFAULT_SERVER.getPassword());
	}

	@Test
	public void theDefaultServerUrlSurvivesTheRoutingHelpers() {
		// getBaseRoute appends /v2 for the NDEx client, and stripApiVersion takes it back off for the
		// /v3/admin/status probe. Both have to land somewhere sane or the fallback cannot talk to NDEx.
		final String url = Server.DEFAULT_SERVER.getUrl();
		assertEquals("https://www.ndexbio.org/v2", UrlUtils.getBaseRoute(url));
		assertEquals("https://www.ndexbio.org", UrlUtils.stripApiVersion(UrlUtils.getBaseRoute(url)));
		assertEquals("https://www.ndexbio.org", UrlUtils.stripApiVersion(url));
	}

	@Test
	public void copyingTheDefaultServerYieldsAnIndependentInstance() {
		final Server copy = new Server(Server.DEFAULT_SERVER);
		copy.setUrl("https://example.org/mutated");

		assertEquals("https://www.ndexbio.org", Server.DEFAULT_SERVER.getUrl());
	}
}
