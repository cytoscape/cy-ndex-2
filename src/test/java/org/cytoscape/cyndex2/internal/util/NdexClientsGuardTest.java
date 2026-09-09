package org.cytoscape.cyndex2.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

/**
 * A URL that reaches the SDK without normalization builds requests against a host that does not exist, and
 * nothing about that failure points back at the URL. The protection is that exactly one place in the app
 * constructs an SDK client -- so assert that, rather than trusting each new call site to remember.
 *
 * Surefire runs with the project directory as its working directory, so the source tree is readable here.
 */
public class NdexClientsGuardTest {

	private static final Path MAIN_SOURCES = Paths.get("src", "main", "java");

	@Test
	public void onlyNdexClientsConstructsAnSdkClient() throws IOException {
		List<String> offenders = new ArrayList<>();
		try (Stream<Path> sources = Files.walk(MAIN_SOURCES)) {
			for (Path source : (Iterable<Path>) sources.filter(p -> p.toString().endsWith(".java"))::iterator) {
				if (source.getFileName().toString().equals("NdexClients.java")) {
					continue;
				}
				String body = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
				if (body.contains("new NdexRestClient(")) {
					offenders.add(MAIN_SOURCES.relativize(source).toString());
				}
			}
		}
		assertEquals("these construct an NdexRestClient directly, bypassing URL normalization; "
				+ "use NdexClients.create/createAnonymous instead: " + offenders,
				0, offenders.size());
	}

	/** Guards the scan itself: it must be looking at a real source tree with the expected file in it. */
	@Test
	public void theScanReadsARealSourceTree() throws IOException {
		Path ndexClients = MAIN_SOURCES.resolve(
				Paths.get("org", "cytoscape", "cyndex2", "internal", "util", "NdexClients.java"));
		assertTrue("expected to find " + ndexClients + " relative to " + Paths.get("").toAbsolutePath(),
				Files.exists(ndexClients));
		assertTrue(new String(Files.readAllBytes(ndexClients), StandardCharsets.UTF_8)
				.contains("new NdexRestClient("));
	}
}
