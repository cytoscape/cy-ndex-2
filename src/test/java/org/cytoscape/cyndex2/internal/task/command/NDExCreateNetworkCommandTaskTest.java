package org.cytoscape.cyndex2.internal.task.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
 * {@code ndex create network} always creates. The command exists as its own name so that a caller
 * handed an ambiguous "save this to NDEx" has no default to fall back on; these assert the half of
 * that contract that lives in code rather than in the description.
 */
public class NDExCreateNetworkCommandTaskTest {

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

	private NDExCreateNetworkCommandTask taskReturning(UUID uuid, UUID folderId) {
		NDExExportTaskFactory exportFactory = mock(NDExExportTaskFactory.class);
		when(exportFactory.createTaskIterator(any(CyNetwork.class)))
				.thenReturn(new TaskIterator(new AbstractTask() {
					@Override
					public void run(TaskMonitor tm) {
					}
				}));
		when(exportFactory.getUUID()).thenReturn(uuid);
		when(exportFactory.getFolderId()).thenReturn(folderId);

		CyApplicationManager appManager = mock(CyApplicationManager.class);
		when(appManager.getCurrentNetwork()).thenReturn(mock(CyNetwork.class));

		return new NDExCreateNetworkCommandTask(resolverFor(signedIn()), appManager,
				(params, isUpdate) -> {
					captured.add(params);
					updateFlags.add(isUpdate);
					return exportFactory;
				});
	}

	@Test
	public void createAsksForACreateAndNamesNoExistingNetwork() throws Exception {
		taskReturning(UUID.randomUUID(), null).run(taskMonitor);

		assertEquals(java.util.Collections.singletonList(Boolean.FALSE), updateFlags);
		assertNull("naming an existing network would make this an overwrite", captured.get(0).networkId);
	}

	@Test
	public void createReportsTheNewNetworkItMade() throws Exception {
		UUID uuid = UUID.randomUUID();
		UUID folderId = UUID.randomUUID();
		NDExCreateNetworkCommandTask task = taskReturning(uuid, folderId);
		task.visibility.setSelectedValue("PUBLIC");
		task.folder = "My Project";
		task.run(taskMonitor);

		JsonNode result = new ObjectMapper().readTree(task.getResults(String.class));
		assertEquals(uuid.toString(), result.get("uuid").asText());
		assertEquals("https://www.ndexbio.org/viewer/networks/" + uuid, result.get("url").asText());
		assertEquals("PUBLIC", result.get("visibility").asText());
		assertEquals(folderId.toString(), result.get("folderId").asText());

		assertEquals("PUBLIC", captured.get(0).visibility);
		assertEquals("My Project", captured.get(0).folder);
	}

	/**
	 * The default has to be the conservative one: a caller that says nothing about visibility must not
	 * end up publishing the user's network.
	 */
	@Test
	public void visibilityDefaultsToPrivate() throws Exception {
		NDExCreateNetworkCommandTask task = taskReturning(UUID.randomUUID(), null);
		task.run(taskMonitor);
		assertEquals("PRIVATE", captured.get(0).visibility);
	}

	@Test
	public void createOffersNoWayToNameAnExistingNetwork() {
		List<String> arguments = new ArrayList<>();
		for (java.lang.reflect.Field field : NDExCreateNetworkCommandTask.class.getFields()) {
			if (field.isAnnotationPresent(org.cytoscape.work.Tunable.class)) {
				arguments.add(field.getName());
			}
		}
		// non-empty, so this cannot pass because the reflection found nothing at all
		assertTrue("expected the inherited arguments to be visible", arguments.contains("profile"));
		assertFalse("create must not take a networkId; that is what makes it unambiguous",
				arguments.contains("networkId"));
	}
}
