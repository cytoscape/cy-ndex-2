package org.cytoscape.cyndex2.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.ndexbio.rest.client.NdexRestClient;

/**
 * Pins the SDK behaviour this app's URL normalization exists for.
 *
 * {@code NdexRestClient} appends routes to its base with no separator, so the base must end in a slash. Its
 * constructor produces one for a bare host and for a scheme-qualified host carrying a version segment, but
 * takes a scheme-qualified host with no version segment verbatim -- and that form then builds
 * {@code https://www.ndexbio.orgv2/network/...}, which does not resolve.
 *
 * The constructor does no I/O, so the real SDK can be exercised directly here. If a future SDK release starts
 * normalizing this itself, {@link #theUnnormalizedFormIsStillBroken} fails and this app's workaround can go.
 */
public class NdexRestClientBaserouteContractTest {

	private static final List<String> SERVER_URL_FORMS = Arrays.asList(
			"www.ndexbio.org",
			"https://www.ndexbio.org",
			"http://www.ndexbio.org",
			"https://www.ndexbio.org/",
			"https://www.ndexbio.org/v2",
			"https://www.ndexbio.org/v3",
			"http://localhost:8080");

	@Test
	public void everyFormWeNormalizeYieldsABaseRouteTheSdkCanAppendTo() {
		for (String url : SERVER_URL_FORMS) {
			String baseroute = new NdexRestClient(UrlUtils.toSdkHost(url)).getBaseroute();
			assertTrue(url + " -> " + baseroute + " (routes would be appended straight onto the host)",
					baseroute.endsWith("/"));
			assertTrue(url + " -> " + baseroute, baseroute.startsWith("http://") || baseroute.startsWith("https://"));
		}
	}

	@Test
	public void aNormalizedUrlProducesTheRequestUrlWeExpect() {
		String baseroute = new NdexRestClient(UrlUtils.toSdkHost("https://www.ndexbio.org")).getBaseroute();
		// exactly how NdexRestClientModelAccessLayer builds a network request
		assertEquals("https://www.ndexbio.org/v2/network/1234/summary", baseroute + "v2/network/1234/summary");
	}

	@Test
	public void theUnnormalizedFormIsStillBroken() {
		String baseroute = new NdexRestClient("https://www.ndexbio.org").getBaseroute();
		assertFalse("the SDK now normalizes this itself -- NdexClients' workaround can be revisited",
				baseroute.endsWith("/"));
		assertEquals("https://www.ndexbio.orgv2/network/1234/summary", baseroute + "v2/network/1234/summary");
	}

	/** The bare-host form a profile stores was always safe; that is why the bug only hit the default server. */
	@Test
	public void theBareHostFormWasAlwaysSafeEvenUnnormalized() {
		assertTrue(new NdexRestClient("www.ndexbio.org").getBaseroute().endsWith("/"));
	}
}
