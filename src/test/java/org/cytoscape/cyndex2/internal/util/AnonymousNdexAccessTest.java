package org.cytoscape.cyndex2.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


import org.junit.Test;

/**
 * With no sign-in profile configured, reading from NDEx falls back to the public server -- the search
 * dialog's download button and the read-only commands both take this route. The whole chain is asserted here
 * because each link was individually correct while the composition was not: the resolver handed back the
 * default server, whose URL the routing helper would have normalized, but the URL was handed to the SDK
 * without ever passing through it, producing requests against {@code www.ndexbio.orgv2}.
 */
public class AnonymousNdexAccessTest {

	private static NdexProfileResolver noProfilesConfigured() {
		final ServerList empty = new ServerList();
		return new NdexProfileResolver(() -> empty, () -> null);
	}

	@Test
	public void withNoProfilesTheFallbackReachesThePublicNdexServer() {
		Server server = noProfilesConfigured().resolveOrAnonymous(null);

		assertNull("the fallback must stay anonymous", server.getUsername());
		assertEquals("https://www.ndexbio.org/",
				NdexClients.createAnonymous(server.getUrl()).getBaseroute());
	}

	@Test
	public void theFallbackBuildsTheRequestUrlNdexActuallyServes() {
		Server server = noProfilesConfigured().resolveOrAnonymous(null);
		String baseroute = NdexClients.createAnonymous(server.getUrl()).getBaseroute();

		// how the SDK composes a network request; the bug produced www.ndexbio.orgv2/network/...
		assertEquals("https://www.ndexbio.org/v2/network/1234/summary", baseroute + "v2/network/1234/summary");
		assertTrue(baseroute + " must not run the host into the path", baseroute.endsWith(".org/"));
	}
}
