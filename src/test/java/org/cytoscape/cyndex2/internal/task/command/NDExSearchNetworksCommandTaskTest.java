package org.cytoscape.cyndex2.internal.task.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.cytoscape.cyndex2.internal.rest.NdexAdminStatusService;
import org.cytoscape.cyndex2.internal.rest.NdexV3AdminStatus;
import org.cytoscape.cyndex2.internal.util.NdexProfileResolver;
import org.cytoscape.cyndex2.internal.util.NdexServerCapabilities;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.work.TaskMonitor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.ndexbio.model.object.FileItemSummary;
import org.ndexbio.model.object.FileSearchResult;
import org.ndexbio.model.object.FileVisibilityType;
import org.ndexbio.model.object.SimpleFileQuery;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NDExSearchNetworksCommandTaskTest {

	private NdexRestClientModelAccessLayer mal;
	private NdexProfileResolver resolver;
	private final TaskMonitor taskMonitor = mock(TaskMonitor.class);

	@Before
	public void setUp() throws Exception {
		mal = mock(NdexRestClientModelAccessLayer.class);
		Server server = new Server();
		server.setUsername("alice");
		server.setUrl("https://www.ndexbio.org/v2");
		resolver = mock(NdexProfileResolver.class);
		// search resolves through resolveOrAnonymous so it can fall back to public NDEx
		when(resolver.resolveOrAnonymous(any(String.class))).thenReturn(server);
		when(resolver.resolveOrAnonymous(null)).thenReturn(server);
		when(mal.searchFiles(any(SimpleFileQuery.class), any(FileVisibilityType.class), anyInt(), anyInt()))
				.thenReturn(new FileSearchResult(0L, 0L, Arrays.asList()));
	}

	private NDExSearchNetworksCommandTask task() {
		NdexAdminStatusService v3Present = url -> new NdexV3AdminStatus();
		return new NDExSearchNetworksCommandTask(resolver, new NdexServerCapabilities(v3Present),
				server -> mal);
	}

	private JsonNode resultOf(NDExSearchNetworksCommandTask task) throws Exception {
		return new ObjectMapper().readTree(task.getResults(String.class));
	}

	@Test
	public void defaultsToSearchingPublicNetworks() throws Exception {
		// NDEx defaults an unset visibility to PUBLIC server-side, so the command sends PUBLIC
		// explicitly rather than omitting it and appearing to mean "everything".
		NDExSearchNetworksCommandTask task = task();
		task.run(taskMonitor);

		verify(mal).searchFiles(any(SimpleFileQuery.class), eq(FileVisibilityType.PUBLIC), anyInt(), anyInt());
	}

	@Test
	public void thereIsNoAllOption() {
		List<String> options = task().visibility.getPossibleValues();
		assertEquals("NDEx offers two search corpora, not three", Arrays.asList("PUBLIC", "PRIVATE"), options);
	}

	@Test
	public void privateIsPassedThrough() throws Exception {
		NDExSearchNetworksCommandTask task = task();
		task.visibility.setSelectedValue("PRIVATE");
		task.run(taskMonitor);

		verify(mal).searchFiles(any(SimpleFileQuery.class), eq(FileVisibilityType.PRIVATE), anyInt(), anyInt());
	}

	@Test
	public void unlistedIsRejectedBecauseItIsNotASearchMode() throws Exception {
		NDExSearchNetworksCommandTask task = task();
		task.visibility = new org.cytoscape.work.util.ListSingleSelection<>("UNLISTED");
		task.visibility.setSelectedValue("UNLISTED");
		assertRejected(task, "UNLISTED is not a search mode");
	}

	@Test
	public void searchingPrivateAnonymouslyIsRejectedWithACredentialsMessage() throws Exception {
		// keyed off the resolved profile being anonymous, not off whether the fallback fired
		Server anonymous = new Server();
		anonymous.setUrl("https://www.ndexbio.org");
		when(resolver.resolveOrAnonymous(any(String.class))).thenReturn(anonymous);
		when(resolver.resolveOrAnonymous(null)).thenReturn(anonymous);

		NDExSearchNetworksCommandTask task = task();
		task.visibility.setSelectedValue("PRIVATE");
		assertRejected(task, "requires a signed-in NDEx profile");
	}

	@Test
	public void searchingPublicAnonymouslyIsFine() throws Exception {
		Server anonymous = new Server();
		anonymous.setUrl("https://www.ndexbio.org");
		when(resolver.resolveOrAnonymous(null)).thenReturn(anonymous);

		task().run(taskMonitor);

		verify(mal).searchFiles(any(SimpleFileQuery.class), eq(FileVisibilityType.PUBLIC), anyInt(), anyInt());
	}

	// ---------- numeric arguments ----------

	@Test
	public void pagingArgumentsAcceptWholeNumbers() throws Exception {
		NDExSearchNetworksCommandTask task = task();
		task.startIndex = 20;
		task.maxResults = 5;
		task.run(taskMonitor);

		verify(mal).searchFiles(any(SimpleFileQuery.class), any(FileVisibilityType.class), eq(20), eq(5));
	}

	@Test
	public void aFractionalCountIsRejectedRatherThanTruncated() throws Exception {
		NDExSearchNetworksCommandTask task = task();
		task.maxResults = 10.5;
		assertRejected(task, "Invalid maxResults");
	}

	@Test
	public void aCountBelowOneIsRejected() throws Exception {
		NDExSearchNetworksCommandTask task = task();
		task.maxResults = 0;
		assertRejected(task, "Invalid maxResults");
	}

	@Test
	public void aNegativeStartIndexIsRejected() throws Exception {
		NDExSearchNetworksCommandTask task = task();
		task.startIndex = -1;
		assertRejected(task, "Invalid startIndex");
	}

	@Test
	public void argumentsAreValidatedBeforeAnythingReachesTheNetwork() throws Exception {
		NDExSearchNetworksCommandTask task = task();
		task.maxResults = -3;
		assertRejected(task, "Invalid maxResults");

		verifyZeroInteractions(mal);
	}

	private void assertRejected(NDExSearchNetworksCommandTask task, String expected) throws Exception {
		try {
			task.run(taskMonitor);
			fail("expected IllegalArgumentException containing: " + expected);
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage(), e.getMessage().contains(expected));
		}
	}

	@Test
	public void searchTermAndPagingMapOntoTheQuery() throws Exception {
		NDExSearchNetworksCommandTask task = task();
		task.searchTerm = "  cancer signaling  ";
		task.run(taskMonitor);

		ArgumentCaptor<SimpleFileQuery> query = ArgumentCaptor.forClass(SimpleFileQuery.class);
		verify(mal).searchFiles(query.capture(), any(FileVisibilityType.class), anyInt(), anyInt());
		assertEquals("cancer signaling", query.getValue().getSearchString());
	}

	@Test
	public void resultsAreProjectedOntoTheDocumentedFields() throws Exception {
		FileItemSummary summary = new FileItemSummary();
		UUID uuid = UUID.randomUUID();
		summary.setUuid(uuid);
		summary.setName("My network");
		summary.setOwner("alice");
		summary.setVisibility("PRIVATE");
		summary.setEdges(42);
		summary.setModificationTime(new Timestamp(0L));
		when(mal.searchFiles(any(SimpleFileQuery.class), any(FileVisibilityType.class), anyInt(), anyInt()))
				.thenReturn(new FileSearchResult(1L, 0L, Arrays.asList(summary)));

		NDExSearchNetworksCommandTask task = task();
		task.run(taskMonitor);

		JsonNode result = resultOf(task);
		assertEquals(1, result.get("numFound").asLong());
		assertEquals(0, result.get("start").asLong());
		JsonNode network = result.get("networks").get(0);
		assertEquals(uuid.toString(), network.get("uuid").asText());
		assertEquals("My network", network.get("name").asText());
		assertEquals("alice", network.get("owner").asText());
		assertEquals("PRIVATE", network.get("visibility").asText());
		assertEquals(42L, network.get("edges").asLong());
		assertTrue(network.has("modificationTime"));
		assertEquals("only the documented fields are projected", 6, network.size());
	}

	@Test
	public void aNonV3ServerIsRejected() throws Exception {
		NdexAdminStatusService noV3 = url -> null;
		NDExSearchNetworksCommandTask task = new NDExSearchNetworksCommandTask(resolver,
				new NdexServerCapabilities(noV3), server -> mal);
		try {
			task.run(taskMonitor);
			fail("expected IllegalStateException");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("NDEx v3.0.0"));
		}
	}

	@Test
	public void resultIsNullBeforeTheTaskRuns() {
		assertNull(task().getResults(String.class));
	}
}
