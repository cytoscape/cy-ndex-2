package org.cytoscape.cyndex2.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
	public void blankProfileWithNoSelectedServerFailsClearly() throws Exception {
		NdexProfileResolver r = resolver(listOf(), null);
		try {
			r.resolve(null);
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("No NDEx profile is selected"));
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

	@Test
	public void urlComparisonIgnoresCase() throws Exception {
		Server alice = server("alice", "https://WWW.ndexbio.org/v2");
		NdexProfileResolver r = resolver(listOf(alice), null);

		assertEquals(alice, r.resolve("alice@https://www.ndexbio.org/v2"));
	}
}
