package org.cytoscape.cyndex2.internal.util;

import java.io.IOException;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * The one place an NDEx server URL becomes an SDK client.
 *
 * {@code NdexRestClient} normalizes the host it is given three different ways, and only two of them leave the
 * trailing slash that its own path building depends on: a host with no scheme becomes {@code https://host/},
 * and a scheme-qualified host ending in {@code /v2} or {@code /v3} keeps its slash -- but a scheme-qualified
 * host with no version suffix is taken verbatim. Routes are appended with no separator, so that third form
 * produces {@code https://www.ndexbio.orgv2/network/...}, which does not resolve.
 *
 * Server URLs reach us in all three shapes: profiles store whatever the user typed, the sign-in dialogs
 * resolve scheme-qualified URLs, and {@code Server.DEFAULT_SERVER} carries one of its own. Normalizing here
 * means no caller has to know which shape is safe, and {@code NdexClientsGuardTest} keeps it that way.
 */
public final class NdexClients {

	private NdexClients() {
	}

	public static NdexRestClient create(final String username, final String password, final String serverUrl)
			throws JsonProcessingException, IOException, NdexException {
		return new NdexRestClient(username, password, UrlUtils.toSdkHost(serverUrl),
				UserAgentUtil.getUserAgent());
	}

	/**
	 * Anonymous access. The SDK's credentialed constructor signs in -- and so declares checked exceptions --
	 * only when both a username and a password are given; with neither there is nothing to fail, so this
	 * takes the plain constructor and stays callable from places that cannot handle those exceptions.
	 */
	public static NdexRestClient createAnonymous(final String serverUrl) {
		final NdexRestClient client = new NdexRestClient(UrlUtils.toSdkHost(serverUrl));
		client.setAdditionalUserAgent(UserAgentUtil.getUserAgent());
		return client;
	}

	public static NdexRestClientModelAccessLayer modelAccessLayer(final String username, final String password,
			final String serverUrl) throws JsonProcessingException, IOException, NdexException {
		return new NdexRestClientModelAccessLayer(create(username, password, serverUrl));
	}
}
