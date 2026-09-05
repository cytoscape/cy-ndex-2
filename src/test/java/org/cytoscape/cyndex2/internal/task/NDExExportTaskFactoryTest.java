package org.cytoscape.cyndex2.internal.task;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.cytoscape.cyndex2.internal.CxFormat;
import org.cytoscape.cyndex2.internal.CxTaskFactoryManager;
import org.cytoscape.cyndex2.internal.rest.NdexAdminStatusService;
import org.cytoscape.cyndex2.internal.rest.NdexV3AdminStatus;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExBasicSaveParameters;
import org.cytoscape.cyndex2.internal.util.NdexServerCapabilities;
import org.cytoscape.io.write.CyNetworkViewWriterFactory;
import org.cytoscape.io.write.CyWriter;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers the format the factory picks and the guards around it.
 *
 * Each case stops the wrapper task at the writer, before it would build an NDEx client, so nothing
 * here touches the network.
 */
public class NDExExportTaskFactoryTest {

	/** Thrown by the stub writer to end the task once the interesting decision has been made. */
	private static class StopHere extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	/** A writer exposing setWriteSiblings but NOT setUseCxId, which used to trip an NPE. */
	public static class WriterWithoutUseCxId implements CyWriter {
		Boolean writeSiblings;

		public void setWriteSiblings(Boolean value) {
			this.writeSiblings = value;
		}

		@Override
		public void run(TaskMonitor taskMonitor) throws Exception {
			throw new StopHere();
		}

		@Override
		public void cancel() {
		}
	}

	private CxTaskFactoryManager cxFactories;
	private CyNetworkViewWriterFactory cx1Writers;
	private CyNetworkViewWriterFactory cx2Writers;
	private CySubNetwork subNetwork;
	private CyRootNetwork rootNetwork;
	private final TaskMonitor taskMonitor = mock(TaskMonitor.class);

	private static Map<String, String> props(String id) {
		Map<String, String> p = new HashMap<>();
		p.put("id", id);
		return p;
	}

	private static NdexServerCapabilities v3Available() {
		NdexAdminStatusService probe = url -> new NdexV3AdminStatus();
		return new NdexServerCapabilities(probe);
	}

	@Before
	public void setUp() throws Exception {
		CyWriter stopper = mock(CyWriter.class);
		doThrow(new StopHere()).when(stopper).run(any(TaskMonitor.class));

		cx1Writers = mock(CyNetworkViewWriterFactory.class);
		cx2Writers = mock(CyNetworkViewWriterFactory.class);
		when(cx1Writers.createWriter(any(OutputStream.class), any(CyNetwork.class))).thenReturn(stopper);
		when(cx2Writers.createWriter(any(OutputStream.class), any(CyNetwork.class))).thenReturn(stopper);

		cxFactories = new CxTaskFactoryManager();
		cxFactories.addWriterFactory(cx1Writers, props(CxFormat.CX1.getWriterId()));
		cxFactories.addWriterFactory(cx2Writers, props(CxFormat.CX2.getWriterId()));

		CyRow row = mock(CyRow.class);
		when(row.get(CyNetwork.NAME, String.class)).thenReturn("network");

		subNetwork = mock(CySubNetwork.class);
		when(subNetwork.getSUID()).thenReturn(669L);
		when(subNetwork.getRow(subNetwork)).thenReturn(row);

		rootNetwork = mock(CyRootNetwork.class);
		when(rootNetwork.getSUID()).thenReturn(668L);
		when(rootNetwork.getRow(rootNetwork)).thenReturn(row);
		when(rootNetwork.getBaseNetwork()).thenReturn(subNetwork);
		when(subNetwork.getRootNetwork()).thenReturn(rootNetwork);
	}

	private static NDExBasicSaveParameters params() {
		NDExBasicSaveParameters params = new NDExBasicSaveParameters();
		params.username = "alice";
		params.password = "secret";
		params.serverUrl = "https://www.ndexbio.org/v2";
		params.metadata = new HashMap<>();
		return params;
	}

	private void runFirstTask(NDExExportTaskFactory factory, CyNetwork network) throws Exception {
		TaskIterator iterator = factory.createTaskIterator(network);
		Task first = iterator.next();
		first.run(taskMonitor);
	}

	@Test
	public void aSingleNetworkIsWrittenAsCx2() throws Exception {
		NDExExportTaskFactory factory = new NDExExportTaskFactory(params(), false, cxFactories, v3Available());
		try {
			runFirstTask(factory, subNetwork);
		} catch (StopHere expected) {
			// reached the writer, which is all this asserts
		}
		verify(cx2Writers).createWriter(any(OutputStream.class), any(CyNetwork.class));
		verify(cx1Writers, never()).createWriter(any(OutputStream.class), any(CyNetwork.class));
	}

	@Test
	public void aCollectionIsStillWrittenAsCx1() throws Exception {
		NDExExportTaskFactory factory = new NDExExportTaskFactory(params(), false, cxFactories, v3Available());
		try {
			runFirstTask(factory, rootNetwork);
		} catch (StopHere expected) {
		}
		verify(cx1Writers).createWriter(any(OutputStream.class), any(CyNetwork.class));
		verify(cx2Writers, never()).createWriter(any(OutputStream.class), any(CyNetwork.class));
	}

	@Test
	public void aMissingCx2WriterNamesTheCxSupportVersionNeeded() throws Exception {
		CxTaskFactoryManager empty = new CxTaskFactoryManager();
		NDExExportTaskFactory factory = new NDExExportTaskFactory(params(), false, empty, v3Available());
		try {
			runFirstTask(factory, subNetwork);
			fail("expected IllegalStateException");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("CX Support"));
			assertTrue(e.getMessage(), e.getMessage().contains("2.8.0"));
		}
	}

	@Test
	public void aNonV3ServerIsRejectedBeforeAnythingIsWritten() throws Exception {
		NdexServerCapabilities noV3 = new NdexServerCapabilities(url -> null);
		NDExExportTaskFactory factory = new NDExExportTaskFactory(params(), false, cxFactories, noV3);
		try {
			runFirstTask(factory, subNetwork);
			fail("expected IllegalStateException");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("NDEx v3.0.0"));
		}
		verify(cx2Writers, never()).createWriter(any(OutputStream.class), any(CyNetwork.class));
	}

	@Test
	public void aCollectionDoesNotRequireV3SinceItStaysOnV2() throws Exception {
		NdexServerCapabilities noV3 = new NdexServerCapabilities(url -> null);
		NDExExportTaskFactory factory = new NDExExportTaskFactory(params(), false, cxFactories, noV3);
		try {
			runFirstTask(factory, rootNetwork);
		} catch (StopHere expected) {
		}
		verify(cx1Writers).createWriter(any(OutputStream.class), any(CyNetwork.class));
	}

	@Test
	public void aWriterWithoutSetUseCxIdDoesNotBlowUp() throws Exception {
		// setTunables used to guard the setUseCxId call on setWriteSiblings being present, so a writer
		// exposing only the former threw a NullPointerException.
		WriterWithoutUseCxId writer = new WriterWithoutUseCxId();
		when(cx2Writers.createWriter(any(OutputStream.class), any(CyNetwork.class))).thenReturn(writer);

		NDExExportTaskFactory factory = new NDExExportTaskFactory(params(), false, cxFactories, v3Available());
		try {
			runFirstTask(factory, subNetwork);
			fail("expected the stub writer to stop the task");
		} catch (StopHere expected) {
			assertTrue("setWriteSiblings should still have been applied", Boolean.FALSE.equals(writer.writeSiblings));
		} catch (NullPointerException e) {
			fail("setTunables threw NPE for a writer without setUseCxId");
		}
	}
}
