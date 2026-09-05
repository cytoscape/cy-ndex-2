package org.cytoscape.cyndex2.internal.task.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExSaveParameters;
import org.cytoscape.cyndex2.internal.task.NDExExportTaskFactory;
import org.cytoscape.cyndex2.internal.util.NdexProfileResolver;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@code ndex update network} always overwrites, and needs to be told what. {@code required = true}
 * is metadata that reaches the generated schema and nothing else -- Cytoscape's executor binds only
 * the arguments it was given and never notices an absent one -- so the check that actually protects
 * a caller lives in the task, and is asserted here.
 */
public class NDExUpdateNetworkCommandTaskTest {

	private static final String TARGET = "12345678-abcd-1234-abcd-1234567890ab";

	private final TaskMonitor taskMonitor = mock(TaskMonitor.class);
	private final List<NDExSaveParameters> captured = new ArrayList<>();
	private final List<Boolean> updateFlags = new ArrayList<>();

	private static Server signedIn() {
		Server server = new Server();
		server.setUsername("alice");
		server.setPassword("secret");
		server.setUrl("https://www.ndexbio.org/v2");
		return server;
	}

	private static NdexProfileResolver resolverFor(Server server) {
		NdexProfileResolver resolver = mock(NdexProfileResolver.class);
		when(resolver.resolve(any(String.class))).thenReturn(server);
		when(resolver.resolve(null)).thenReturn(server);
		return resolver;
	}

	private NDExUpdateNetworkCommandTask task(UUID reported) {
		NDExExportTaskFactory exportFactory = mock(NDExExportTaskFactory.class);
		when(exportFactory.createTaskIterator(any(CyNetwork.class)))
				.thenReturn(new TaskIterator(new AbstractTask() {
					@Override
					public void run(TaskMonitor tm) {
					}
				}));
		when(exportFactory.getUUID()).thenReturn(reported);

		CyApplicationManager appManager = mock(CyApplicationManager.class);
		when(appManager.getCurrentNetwork()).thenReturn(mock(CyNetwork.class));

		return new NDExUpdateNetworkCommandTask(resolverFor(signedIn()), appManager,
				(params, isUpdate) -> {
					captured.add(params);
					updateFlags.add(isUpdate);
					return exportFactory;
				});
	}

	@Test
	public void updateAsksForAnOverwriteOfTheNetworkItWasGiven() throws Exception {
		UUID uuid = UUID.fromString(TARGET);
		NDExUpdateNetworkCommandTask task = task(uuid);
		task.networkId = TARGET;
		task.run(taskMonitor);

		assertEquals(java.util.Collections.singletonList(Boolean.TRUE), updateFlags);
		assertEquals(TARGET, captured.get(0).networkId);

		JsonNode result = new ObjectMapper().readTree(task.getResults(String.class));
		assertEquals(TARGET, result.get("uuid").asText());
		assertEquals("https://www.ndexbio.org/viewer/networks/" + TARGET, result.get("url").asText());
	}

	/** Whitespace around a pasted UUID is normal and must not reach the server. */
	@Test
	public void updateTrimsTheNetworkId() throws Exception {
		NDExUpdateNetworkCommandTask task = task(UUID.fromString(TARGET));
		task.networkId = "  " + TARGET + "  ";
		task.run(taskMonitor);
		assertEquals(TARGET, captured.get(0).networkId);
	}

	@Test
	public void updateWithoutANetworkIdSaysWhatIsMissing() {
		assertRejects(null);
		assertRejects("");
		assertRejects("   ");
	}

	@Test
	public void updateWithANonUuidNetworkIdSaysSo() {
		NDExUpdateNetworkCommandTask task = task(null);
		task.networkId = "My network";
		try {
			task.run(taskMonitor);
			fail("expected IllegalArgumentException");
		} catch (Exception e) {
			assertTrue(e.getMessage(), e.getMessage().contains("is not a UUID"));
			assertTrue("the message must name the offending value", e.getMessage().contains("My network"));
		}
		assertTrue("nothing may be sent for an unusable target", captured.isEmpty());
	}

	private void assertRejects(String networkId) {
		NDExUpdateNetworkCommandTask task = task(null);
		task.networkId = networkId;
		try {
			task.run(taskMonitor);
			fail("expected IllegalArgumentException for networkId=" + networkId);
		} catch (Exception e) {
			assertTrue(e.getMessage(), e.getMessage().contains("networkId is required"));
		}
		assertTrue("nothing may be sent without a target", captured.isEmpty());
	}
}
