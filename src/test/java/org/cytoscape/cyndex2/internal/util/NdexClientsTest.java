package org.cytoscape.cyndex2.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.ndexbio.rest.client.NdexRestClient;

/**
 * The choke point every NDEx server URL passes through on its way to the SDK.
 *
 * Nothing here reaches the network: {@code NdexRestClient}'s constructor only parses the host, and the
 * anonymous variant never signs in.
 */
public class NdexClientsTest {

	/** Every shape a server URL arrives in: typed by a user, resolved by a sign-in dialog, or the default. */
	private static final List<String> SERVER_URL_FORMS = Arrays.asList(
			"www.ndexbio.org",
			"https://www.ndexbio.org",
			"http://www.ndexbio.org",
			"https://www.ndexbio.org/",
			"https://www.ndexbio.org/v2",
			"https://www.ndexbio.org/v3",
			"http://localhost:8080");

	@Test
	public void everyServerUrlFormBecomesAnAppendableBaseRoute() {
		for (String url : SERVER_URL_FORMS) {
			String baseroute = NdexClients.createAnonymous(url).getBaseroute();
			assertTrue(url + " -> " + baseroute, baseroute.endsWith("/"));
		}
	}

	@Test
	public void theDefaultServerReachesTheRealNdexHost() {
		// the no-profile case: this is the URL the anonymous fallback and the search dialog both send
		String baseroute = NdexClients.createAnonymous(Server.DEFAULT_SERVER.getUrl()).getBaseroute();
		assertEquals("https://www.ndexbio.org/", baseroute);
		assertEquals("https://www.ndexbio.org/v2/network/1234/summary", baseroute + "v2/network/1234/summary");
	}

	@Test
	public void aProfileUrlAndTheDefaultServerUrlReachTheSameHost() {
		// a profile stores the bare host, the default carries a scheme; both must end up in the same place
		assertEquals(NdexClients.createAnonymous("www.ndexbio.org").getBaseroute(),
				NdexClients.createAnonymous("https://www.ndexbio.org").getBaseroute());
	}

	/**
	 * Normalizing must not quietly move a profile off TLS. A bare host is left for the SDK to resolve, which
	 * it does as https; inventing a scheme here would hand it http instead.
	 */
	@Test
	public void aBareHostKeepsTheSdkHttpsDefault() {
		assertTrue(NdexClients.createAnonymous("www.ndexbio.org").getBaseroute().startsWith("https://"));
	}

	/** An explicit scheme is the caller's choice and is preserved -- local servers often have no TLS. */
	@Test
	public void anExplicitSchemeIsPreserved() {
		assertEquals("http://localhost:8080/", NdexClients.createAnonymous("http://localhost:8080").getBaseroute());
	}

	@Test
	public void anonymousClientsCarryNoCredentials() throws Exception {
		NdexRestClient client = NdexClients.createAnonymous("www.ndexbio.org");
		assertNull(client.getUsername());
	}

	@Test
	public void credentialedClientsNormalizeTheSameWay() throws Exception {
		// null password means the SDK skips sign-in, so this stays an offline test
		assertEquals(NdexClients.createAnonymous("https://www.ndexbio.org").getBaseroute(),
				NdexClients.create("alice", null, "https://www.ndexbio.org").getBaseroute());
	}

	/**
	 * Two normalizers exist: this one produces a versioned base for the SDK, while
	 * {@link NdexServerCapabilities} strips back to the server root for {@code /v3/admin/status}. They must
	 * keep agreeing about which server is being addressed.
	 */
	@Test
	public void theSdkBaseAndTheV3ProbeAddressTheSameHost() {
		for (String url : SERVER_URL_FORMS) {
			String sdkHost = hostOf(NdexClients.createAnonymous(url).getBaseroute());
			// NdexV3AdminStatus.fetch supplies its own scheme, so compare the host, not the scheme
			String probeHost = hostOf(UrlUtils.addHttpsProtocol(UrlUtils.stripApiVersion(url)));
			assertEquals(url, probeHost, sdkHost);
		}
	}

	private static String hostOf(String url) {
		return java.net.URI.create(url).getHost() + ":" + java.net.URI.create(url).getPort();
	}
}
