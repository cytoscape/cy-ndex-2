package org.cytoscape.cyndex2.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class UrlUtilsStripApiVersionTest {

	@Test
	public void stripsTrailingApiVersion() {
		assertEquals("http://public.ndexbio.org", UrlUtils.stripApiVersion("http://public.ndexbio.org/v2"));
		assertEquals("http://public.ndexbio.org", UrlUtils.stripApiVersion("http://public.ndexbio.org/v2/"));
		assertEquals("https://www.ndexbio.org", UrlUtils.stripApiVersion("https://www.ndexbio.org/v3"));
		assertEquals("https://www.ndexbio.org", UrlUtils.stripApiVersion("https://www.ndexbio.org/V3/"));
	}

	@Test
	public void leavesUrlsWithoutAnApiVersionAlone() {
		assertEquals("https://www.ndexbio.org", UrlUtils.stripApiVersion("https://www.ndexbio.org"));
		assertEquals("www.ndexbio.org", UrlUtils.stripApiVersion("www.ndexbio.org"));
		assertEquals("https://host/v22", UrlUtils.stripApiVersion("https://host/v22"));
	}

	@Test
	public void toleratesNullAndEmpty() {
		assertNull(UrlUtils.stripApiVersion(null));
		assertEquals("", UrlUtils.stripApiVersion(""));
	}
}
