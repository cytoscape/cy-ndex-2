package org.cytoscape.cyndex2.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.cytoscape.cyndex2.internal.rest.NdexAdminStatusService;
import org.cytoscape.cyndex2.internal.rest.NdexV3AdminStatus;
import org.junit.Test;

/**
 * The admin-status probe is a functional interface, so these use a lambda rather than an HTTP mock.
 */
public class NdexServerCapabilitiesTest {

	private final List<String> probed = new ArrayList<>();

	private NdexServerCapabilities capabilities(boolean v3Present) {
		NdexAdminStatusService probe = url -> {
			probed.add(url);
			return v3Present ? new NdexV3AdminStatus() : null;
		};
		return new NdexServerCapabilities(probe);
	}

	@Test
	public void v3ServerPasses() {
		capabilities(true).requireV3("https://www.ndexbio.org/v2");
	}

	@Test
	public void nonV3ServerThrowsTheMinimumVersionMessage() {
		try {
			capabilities(false).requireV3("https://old.example.org/v2");
			fail("expected IllegalStateException");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage(), e.getMessage().contains(NdexServerCapabilities.MINIMUM_VERSION_MESSAGE));
			assertTrue(e.getMessage(), e.getMessage().contains("https://old.example.org/v2"));
		}
	}

	@Test
	public void versionSuffixedAndBareHostUrlsProbeTheSameEndpoint() {
		NdexServerCapabilities caps = capabilities(true);
		caps.requireV3("https://www.ndexbio.org/v2");
		caps.requireV3("https://www.ndexbio.org/v3/");
		caps.requireV3("https://www.ndexbio.org");

		assertEquals("all three forms should normalize identically", 1, probed.stream().distinct().count());
		assertEquals("https://www.ndexbio.org", probed.get(0));
	}

	@Test
	public void nullOrEmptyUrlIsNotSupported() {
		assertFalse(capabilities(true).supportsV3(null));
		assertFalse(capabilities(true).supportsV3(""));
	}
}
