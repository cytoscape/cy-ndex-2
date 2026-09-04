package org.cytoscape.cyndex2.internal.task.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExSaveParameters;
import org.cytoscape.cyndex2.internal.task.NDExExportTaskFactory;
import org.cytoscape.cyndex2.internal.task.NDExImportTaskFactory;
import org.cytoscape.cyndex2.internal.util.NdexProfileResolver;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.json.JSONResult;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The command task must be the only ObservableTask the executor sees, and must carry the
 * documented JSON. Delegating by appending to the running iterator would let the delegate's own
 * result -- a bare UUID string, or a SUID -- overwrite it.
 */
public class NdexCommandResultTest {

	private final TaskMonitor taskMonitor = mock(TaskMonitor.class);

	private static Server signedInServer() {
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

	/** A delegate that is itself an ObservableTask, like the real export and import tasks. */
	private static class ObservableDelegate extends AbstractTask implements ObservableTask {
		boolean ran = false;

		@Override
		public void run(TaskMonitor tm) {
			ran = true;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <R> R getResults(Class<? extends R> type) {
			return String.class.equals(type) ? (R) "delegate-result" : null;
		}
	}

	@Test
	public void uploadReportsItsOwnJsonNotTheDelegatesUuidString() throws Exception {
		UUID uuid = UUID.randomUUID();
		ObservableDelegate delegate = new ObservableDelegate();
		NDExExportTaskFactory exportFactory = mock(NDExExportTaskFactory.class);
		when(exportFactory.createTaskIterator(any(CyNetwork.class))).thenReturn(new TaskIterator(delegate));
		when(exportFactory.getUUID()).thenReturn(uuid);
		when(exportFactory.getFolderId()).thenReturn(null);

		CyApplicationManager appManager = mock(CyApplicationManager.class);
		when(appManager.getCurrentNetwork()).thenReturn(mock(CyNetwork.class));

		NDExUploadNetworkCommandTask task = new NDExUploadNetworkCommandTask(
				resolverFor(signedInServer()), appManager, (params, isUpdate) -> exportFactory);
		task.run(taskMonitor);

		assertTrue("the delegate must actually have run", delegate.ran);
		JsonNode result = new ObjectMapper().readTree(task.getResults(String.class));
		assertEquals(uuid.toString(), result.get("uuid").asText());
		assertEquals("https://www.ndexbio.org/viewer/networks/" + uuid, result.get("url").asText());
		assertEquals("PRIVATE", result.get("visibility").asText());
		assertTrue(result.get("folderId").isNull());

		// and the same content is available as a JSONResult, which is what CyREST reads
		assertEquals(task.getResults(String.class), task.getResults(JSONResult.class).getJSON());
	}

	@Test
	public void uploadPassesVisibilityFolderAndNetworkIdThrough() throws Exception {
		NDExExportTaskFactory exportFactory = mock(NDExExportTaskFactory.class);
		when(exportFactory.createTaskIterator(any(CyNetwork.class)))
				.thenReturn(new TaskIterator(new ObservableDelegate()));
		when(exportFactory.getUUID()).thenReturn(UUID.randomUUID());

		CyApplicationManager appManager = mock(CyApplicationManager.class);
		when(appManager.getCurrentNetwork()).thenReturn(mock(CyNetwork.class));

		ArgumentCaptor<NDExSaveParameters> captured = ArgumentCaptor.forClass(NDExSaveParameters.class);
		NdexTaskFactories.ExportTaskFactorySupplier supplier = (params, isUpdate) -> {
			captured.getAllValues().add(params);
			assertTrue("networkId implies an update", isUpdate);
			return exportFactory;
		};

		NDExUploadNetworkCommandTask task = new NDExUploadNetworkCommandTask(
				resolverFor(signedInServer()), appManager, supplier);
		task.visibility.setSelectedValue("UNLISTED");
		task.folder = "My Project";
		task.networkId = "12345678-abcd-1234-abcd-1234567890ab";
		task.run(taskMonitor);

		NDExSaveParameters params = captured.getAllValues().get(0);
		assertEquals("UNLISTED", params.visibility);
		assertEquals("My Project", params.folder);
		assertEquals("12345678-abcd-1234-abcd-1234567890ab", params.networkId);
		assertNull("the dead legacy flag must stay unset", params.isPublic);
	}

	@Test
	public void downloadReportsItsOwnJsonNotTheDelegatesSuid() throws Exception {
		UUID uuid = UUID.randomUUID();
		ObservableDelegate delegate = new ObservableDelegate();
		NDExImportTaskFactory importFactory = mock(NDExImportTaskFactory.class);
		when(importFactory.createTaskIterator()).thenReturn(new TaskIterator(delegate));
		when(importFactory.getSUID()).thenReturn(52L);

		CyNetwork imported = mock(CyNetwork.class);
		CyRow row = mock(CyRow.class);
		when(imported.getRow(imported)).thenReturn(row);
		when(row.get(CyNetwork.NAME, String.class)).thenReturn("My network");
		CyNetworkManager networkManager = mock(CyNetworkManager.class);
		when(networkManager.getNetwork(52L)).thenReturn(imported);

		NDExDownloadNetworkCommandTask task = new NDExDownloadNetworkCommandTask(
				resolverFor(signedInServer()), networkManager, params -> importFactory);
		task.networkId = uuid.toString();
		task.run(taskMonitor);

		assertTrue(delegate.ran);
		JsonNode result = new ObjectMapper().readTree(task.getResults(String.class));
		assertEquals(52L, result.get("suid").asLong());
		assertEquals(uuid.toString(), result.get("uuid").asText());
		assertEquals("My network", result.get("name").asText());
	}

	@Test
	public void downloadRejectsAMalformedNetworkId() {
		NDExDownloadNetworkCommandTask task = new NDExDownloadNetworkCommandTask(
				resolverFor(signedInServer()), mock(CyNetworkManager.class), params -> null);
		task.networkId = "not-a-uuid";
		try {
			task.run(taskMonitor);
			org.junit.Assert.fail("expected IllegalArgumentException");
		} catch (Exception e) {
			assertTrue(e.getMessage(), e.getMessage().contains("Expected a UUID"));
		}
	}

	@Test
	public void uploadRequiresACurrentNetwork() {
		CyApplicationManager appManager = mock(CyApplicationManager.class);
		NDExUploadNetworkCommandTask task = new NDExUploadNetworkCommandTask(
				resolverFor(signedInServer()), appManager, (params, isUpdate) -> null);
		try {
			task.run(taskMonitor);
			org.junit.Assert.fail("expected IllegalArgumentException");
		} catch (Exception e) {
			assertTrue(e.getMessage(), e.getMessage().contains("No current network"));
		}
	}

	@Test
	public void uploadRequiresASignedInProfile() {
		Server anonymous = new Server();
		anonymous.setUrl("https://www.ndexbio.org/v2");
		CyApplicationManager appManager = mock(CyApplicationManager.class);
		when(appManager.getCurrentNetwork()).thenReturn(mock(CyNetwork.class));

		NDExUploadNetworkCommandTask task = new NDExUploadNetworkCommandTask(
				resolverFor(anonymous), appManager, (params, isUpdate) -> null);
		try {
			task.run(taskMonitor);
			org.junit.Assert.fail("expected IllegalArgumentException");
		} catch (Exception e) {
			assertTrue(e.getMessage(), e.getMessage().contains("not signed in"));
		}
	}

	@Test
	public void cancellingForwardsToTheRunningDelegate() throws Exception {
		final boolean[] delegateCancelled = { false };
		Task delegate = new AbstractTask() {
			@Override
			public void run(TaskMonitor tm) {
			}

			@Override
			public void cancel() {
				delegateCancelled[0] = true;
			}
		};

		AbstractNdexCommandTask task = new AbstractNdexCommandTask() {
			@Override
			public void run(TaskMonitor tm) throws Exception {
				runInline(new TaskIterator(new AbstractTask() {
					@Override
					public void run(TaskMonitor inner) {
						cancel();
					}
				}), tm);
			}
		};
		// exercise the forwarding directly: a delegate captured mid-run receives the cancel
		java.lang.reflect.Field field = AbstractNdexCommandTask.class.getDeclaredField("delegate");
		field.setAccessible(true);
		field.set(task, delegate);
		task.cancel();
		assertTrue(delegateCancelled[0]);
	}
}
