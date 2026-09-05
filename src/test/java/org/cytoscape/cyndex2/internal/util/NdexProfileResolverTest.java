package org.cytoscape.cyndex2.internal.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import org.junit.Test;

/**
 * Collaborators are injected through the package-private constructor, so nothing here touches the
 * ServerManager singleton.
 */
public class NdexProfileResolverTest {

	private static Server server(String username, String url) {
		Server s = new Server();
		s.setUsername(username);
		s.setUrl(url);
		return s;
	}

	private static ServerList listOf(Server... servers) throws Exception {
		ServerList list = new ServerList();
		for (Server s : servers) {
			list.add(s);
		}
		return list;
	}

	private static NdexProfileResolver resolver(ServerList list, Server selected) {
		return new NdexProfileResolver(() -> list, () -> selected);
	}

	@Test
	public void resolvesNamedProfile() throws Exception {
		Server alice = server("alice", "https://www.ndexbio.org/v2");
		Server bob = server("bob", "https://www.ndexbio.org/v2");
		NdexProfileResolver r = resolver(listOf(alice, bob), null);

		assertSame(bob, r.resolve("bob@https://www.ndexbio.org/v2"));
	}

	@Test
	public void resolvesAnonymousProfileWithEmptyUsername() throws Exception {
		Server anonymous = server(null, "https://www.ndexbio.org/v2");
		Server alice = server("alice", "https://www.ndexbio.org/v2");
		NdexProfileResolver r = resolver(listOf(anonymous, alice), null);

		assertSame(anonymous, r.resolve("@https://www.ndexbio.org/v2"));
	}

	@Test
	public void matchesEvenWhenTheUrlItselfContainsAnAt() throws Exception {
		Server withAt = server("alice", "https://user@host.example.org/v2");
		NdexProfileResolver r = resolver(listOf(withAt), null);

		assertSame(withAt, r.resolve("alice@https://user@host.example.org/v2"));
	}

	@Test
	public void blankProfileFallsBackToSelectedServer() throws Exception {
		Server selected = server("alice", "https://www.ndexbio.org/v2");
		NdexProfileResolver r = resolver(listOf(selected), selected);

		assertSame(selected, r.resolve(null));
		assertSame(selected, r.resolve("   "));
	}

	@Test
	public void noProfilesDefinedAtAllSaysSo() throws Exception {
		NdexProfileResolver r = resolver(listOf(), null);
		try {
			r.resolve(null);
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("No NDEx profiles are defined"));
			assertTrue(e.getMessage(), e.getMessage().contains("ndex list profiles"));
		}
	}

	@Test
	public void profilesDefinedButNoneSelectedSaysSomethingDifferent() throws Exception {
		// Different situation, different remedy: select one rather than create one.
		NdexProfileResolver r = resolver(listOf(server("alice", "https://www.ndexbio.org")), null);
		try {
			r.resolve(null);
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("No NDEx profile is selected"));
			assertTrue(e.getMessage(), e.getMessage().contains("ndex list profiles"));
		}
	}

	@Test
	public void theTwoNoProfileMessagesDiffer() throws Exception {
		String noneDefined = messageFrom(resolver(listOf(), null));
		String noneSelected = messageFrom(resolver(listOf(server("alice", "https://www.ndexbio.org")), null));
		org.junit.Assert.assertNotEquals("the two situations need different remedies", noneDefined, noneSelected);
	}

	private static String messageFrom(NdexProfileResolver r) {
		try {
			r.resolve(null);
			throw new AssertionError("expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			return e.getMessage();
		}
	}

	@Test
	public void unknownProfileListsTheAvailableOnes() throws Exception {
		NdexProfileResolver r = resolver(listOf(server("alice", "https://www.ndexbio.org/v2")), null);
		try {
			r.resolve("carol@https://www.ndexbio.org/v2");
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("alice@https://www.ndexbio.org/v2"));
		}
	}

	@Test
	public void unmatchedProfileIsRejected() throws Exception {
		NdexProfileResolver r = resolver(listOf(), null);
		try {
			r.resolve("alice");
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("No NDEx profile 'alice'"));
		}
	}

	// ---------- list() ----------

	@Test
	public void listIsEmptyWhenNoProfilesAreDefined() throws Exception {
		assertTrue(resolver(listOf(), null).list().isEmpty());
	}

	@Test
	public void listPreservesInsertionOrder() throws Exception {
		Server alice = server("alice", "https://www.ndexbio.org");
		Server bob = server("bob", "https://www.ndexbio.org");
		List<NdexProfile> profiles = resolver(listOf(alice, bob), null).list();

		assertEquals(2, profiles.size());
		assertEquals("alice@https://www.ndexbio.org", profiles.get(0).getName());
		assertEquals("bob@https://www.ndexbio.org", profiles.get(1).getName());
	}

	@Test
	public void listMarksExactlyTheSelectedProfile() throws Exception {
		Server alice = server("alice", "https://www.ndexbio.org");
		Server bob = server("bob", "https://www.ndexbio.org");
		List<NdexProfile> profiles = resolver(listOf(alice, bob), bob).list();

		assertFalse(profiles.get(0).isCurrent());
		assertTrue(profiles.get(1).isCurrent());
	}

	@Test
	public void listMarksNothingCurrentWhenNothingIsSelected() throws Exception {
		for (NdexProfile profile : resolver(listOf(server("alice", "https://www.ndexbio.org")), null).list()) {
			assertFalse(profile.isCurrent());
		}
	}

	@Test
	public void listReportsAnonymousProfilesWithANullUsername() throws Exception {
		List<NdexProfile> profiles = resolver(listOf(server(null, "https://www.ndexbio.org")), null).list();

		assertNull(profiles.get(0).getUsername());
		assertEquals("@https://www.ndexbio.org", profiles.get(0).getName());
	}

	@Test
	public void listReportsTheServerUrlVerbatim() throws Exception {
		// stored unnormalized, and that exact string is what has to round-trip
		List<NdexProfile> profiles = resolver(listOf(server("alice", "www.ndexbio.org")), null).list();

		assertEquals("www.ndexbio.org", profiles.get(0).getServerUrl());
	}

	@Test
	public void everyListedNameRoundTripsBackThroughResolve() throws Exception {
		// The guarantee the list command rests on: the name it prints is a name resolve accepts, and it
		// resolves back to that same profile -- including the anonymous one and an unnormalized URL.
		Server alice = server("alice", "https://www.ndexbio.org");
		Server anonymous = server(null, "https://www.ndexbio.org");
		Server carol = server("carol", "www.ndexbio.org");
		NdexProfileResolver r = resolver(listOf(alice, anonymous, carol), null);

		List<Server> expected = java.util.Arrays.asList(alice, anonymous, carol);
		List<NdexProfile> profiles = r.list();
		assertEquals(expected.size(), profiles.size());
		for (int i = 0; i < profiles.size(); i++) {
			final String name = profiles.get(i).getName();
			assertSame("listed name '" + name + "' must resolve back to its own profile",
					expected.get(i), r.resolve(name));
		}
	}

	// ---------- resolveOrAnonymous() ----------

	@Test
	public void anonymousFallbackIsUsedWhenNothingIsConfigured() throws Exception {
		Server resolved = resolver(listOf(), null).resolveOrAnonymous(null);

		assertEquals(Server.DEFAULT_SERVER.getUrl(), resolved.getUrl());
		assertNull(resolved.getUsername());
	}

	@Test
	public void anonymousFallbackHandsOutACopyNotTheSingleton() throws Exception {
		Server resolved = resolver(listOf(), null).resolveOrAnonymous(null);
		resolved.setUrl("https://example.org/mutated");

		assertEquals("the shared default must not be mutable through the fallback",
				"https://www.ndexbio.org", Server.DEFAULT_SERVER.getUrl());
	}

	@Test
	public void aSelectedProfileStillWinsOverTheAnonymousFallback() throws Exception {
		Server alice = server("alice", "https://www.ndexbio.org");
		assertSame(alice, resolver(listOf(alice), alice).resolveOrAnonymous(null));
	}

	@Test
	public void anExplicitlyNamedProfileStillWinsOverTheAnonymousFallback() throws Exception {
		Server alice = server("alice", "https://www.ndexbio.org");
		assertSame(alice, resolver(listOf(alice), null).resolveOrAnonymous("alice@https://www.ndexbio.org"));
	}

	@Test
	public void anExplicitlyNamedProfileThatDoesNotExistStillThrows() throws Exception {
		// the fallback must not turn a mistyped name into a silent anonymous session
		try {
			resolver(listOf(), null).resolveOrAnonymous("typo@https://www.ndexbio.org");
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("No NDEx profile"));
		}
	}

	@Test
	public void urlComparisonIgnoresCase() throws Exception {
		Server alice = server("alice", "https://WWW.ndexbio.org/v2");
		NdexProfileResolver r = resolver(listOf(alice), null);

		assertEquals(alice, r.resolve("alice@https://www.ndexbio.org/v2"));
	}
}
