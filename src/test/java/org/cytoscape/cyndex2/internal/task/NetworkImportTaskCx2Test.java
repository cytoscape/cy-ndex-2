package org.cytoscape.cyndex2.internal.task;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.cytoscape.cyndex2.internal.CxFormat;
import org.cytoscape.cyndex2.internal.CxTaskFactoryManager;
import org.cytoscape.cyndex2.internal.task.NetworkImportTask.NetworkImportException;
import org.cytoscape.io.read.AbstractCyNetworkReader;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.junit.Before;
import org.junit.Test;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

/**
 * Covers the CX2/v3 import branch and the behaviour it must not disturb.
 *
 * Registers mock factories into a fresh CxTaskFactoryManager handed to the task, never into the
 * INSTANCE singleton.
 */
public class NetworkImportTaskCx2Test {

	private static final UUID NETWORK_ID = new UUID(3, 4);

	private NdexRestClientModelAccessLayer mal;
	private CxTaskFactoryManager cxFactories;
	private CyNetworkManager networkManager;
	private AbstractCyNetworkReader reader;
	private InputStream cx2Stream;
	private final TaskMonitor taskMonitor = mock(TaskMonitor.class);

	private static Map<String, String> props(String id) {
		Map<String, String> p = new HashMap<>();
		p.put("id", id);
		return p;
	}

	@Before
	public void setUp() throws Exception {
		mal = mock(NdexRestClientModelAccessLayer.class);
		networkManager = mock(CyNetworkManager.class);
		cx2Stream = mock(InputStream.class);

		NetworkSummary summary = mock(NetworkSummary.class);
		when(summary.getExternalId()).thenReturn(NETWORK_ID);
		when(summary.getModificationTime()).thenReturn(new Timestamp(0));
		when(summary.getSubnetworkIds()).thenReturn(Collections.emptySet());
		when(mal.getNetworkSummaryById(any(UUID.class), anyObject())).thenReturn(summary);
		when(mal.getNetworkAsCX2Stream(any(UUID.class))).thenReturn(cx2Stream);
		when(mal.getNetworkAsCX2Stream(any(UUID.class), any(String.class))).thenReturn(cx2Stream);

		CyNetwork network = mock(CyNetwork.class);
		when(network.getSUID()).thenReturn(668L);
		CyTable table = mock(CyTable.class);
		when(table.getRow(668L)).thenReturn(mock(CyRow.class));
		when(network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS)).thenReturn(table);

		reader = mock(AbstractCyNetworkReader.class);
		when(reader.getNetworks()).thenReturn(new CyNetwork[] { network });

		InputStreamTaskFactory readerFactory = mock(InputStreamTaskFactory.class);
		when(readerFactory.createTaskIterator(any(InputStream.class), anyObject()))
				.thenReturn(new TaskIterator(reader));

		cxFactories = new CxTaskFactoryManager();
		cxFactories.addReaderFactory(readerFactory, props(CxFormat.CX2.getReaderId()));
	}

	private NetworkImportTask task(String accessKey, Boolean createView, CxFormat format) throws Exception {
		return new NetworkImportTask(mal, NETWORK_ID, accessKey, createView, format, cxFactories, networkManager);
	}

	@Test
	public void readsTheCx2StreamAndRegistersTheNetwork() throws Exception {
		task(null, null, CxFormat.CX2).run(taskMonitor);

		verify(mal).getNetworkAsCX2Stream(NETWORK_ID);
		verify(mal, never()).getNetworkAsCXStream(any(UUID.class));
		verify(networkManager).addNetwork(any(CyNetwork.class));
	}

	@Test
	public void accessKeyIsPassedThrough() throws Exception {
		task("key-123", null, CxFormat.CX2).run(taskMonitor);

		verify(mal).getNetworkAsCX2Stream(NETWORK_ID, "key-123");
	}

	@Test
	public void aViewIsBuiltForTheImportedNetwork() throws Exception {
		task(null, Boolean.TRUE, CxFormat.CX2).run(taskMonitor);

		verify(reader).buildCyNetworkView(any(CyNetwork.class));
	}

	@Test
	public void cx1RemainsAvailableForCallersThatWantIt() throws Exception {
		InputStream cx1Stream = mock(InputStream.class);
		when(mal.getNetworkAsCXStream(any(UUID.class))).thenReturn(cx1Stream);
		InputStreamTaskFactory cx1Factory = mock(InputStreamTaskFactory.class);
		when(cx1Factory.createTaskIterator(any(InputStream.class), anyObject()))
				.thenReturn(new TaskIterator(reader));
		cxFactories.addReaderFactory(cx1Factory, props(CxFormat.CX1.getReaderId()));

		task(null, null, CxFormat.CX1).run(taskMonitor);

		verify(mal).getNetworkAsCXStream(NETWORK_ID);
		verify(mal, never()).getNetworkAsCX2Stream(any(UUID.class));
	}

	@Test
	public void aMissingCx2ReaderNamesTheCxSupportVersionNeeded() throws Exception {
		cxFactories = new CxTaskFactoryManager(); // nothing registered
		try {
			task(null, null, CxFormat.CX2).run(taskMonitor);
			fail("expected NetworkImportException");
		} catch (NetworkImportException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("CX Support"));
			assertTrue(e.getMessage(), e.getMessage().contains("2.8.0"));
		}
	}

	@Test
	public void aReaderFailurePropagatesRatherThanBeingSwallowed() throws Exception {
		// The old code wrapped the whole read in invokeAndWait and logged failures away, so an
		// unreadable network looked like an obscure NPE further down.
		org.mockito.Mockito.doThrow(new RuntimeException("bad CX")).when(reader).run(any(TaskMonitor.class));
		try {
			task(null, null, CxFormat.CX2).run(taskMonitor);
			fail("expected the failure to surface");
		} catch (NetworkImportException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("bad CX"));
		}
	}
}
