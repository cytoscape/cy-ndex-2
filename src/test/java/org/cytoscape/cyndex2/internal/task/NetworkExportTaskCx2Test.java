package org.cytoscape.cyndex2.internal.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import org.cytoscape.cyndex2.internal.CxFormat;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExBasicSaveParameters;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExSaveParameters;
import org.cytoscape.cyndex2.internal.task.NetworkExportTask.NetworkExportException;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.work.TaskMonitor;
import org.junit.Before;
import org.junit.Test;
import org.ndexbio.model.object.MoveNetworksRequest;
import org.ndexbio.model.object.NdexFolder;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

/**
 * Covers the CX2/v3 export branch and the behaviour it must not disturb.
 *
 * Lives in the production package so it can use the package-private constructor and inject the
 * network manager, rather than mutating the CyServiceModule singleton.
 */
public class NetworkExportTaskCx2Test {

	private static final long SUB_SUID = 669L;
	private static final long ROOT_SUID = 668L;
	private static final UUID STORED_UUID = new UUID(1, 2);

	private NdexRestClientModelAccessLayer mal;
	private CyNetworkManager networkManager;
	private CySubNetwork subNetwork;
	private CyRootNetwork rootNetwork;
	private CyRow subNetworkRow;
	private CyRow rootNetworkRow;
	private InputStream cxStream;
	private final TaskMonitor taskMonitor = mock(TaskMonitor.class);

	@Before
	public void setUp() throws Exception {
		mal = mock(NdexRestClientModelAccessLayer.class);
		networkManager = mock(CyNetworkManager.class);
		cxStream = mock(InputStream.class);

		subNetworkRow = mock(CyRow.class);
		when(subNetworkRow.get(CyNetwork.NAME, String.class)).thenReturn("subnetwork");
		when(subNetworkRow.get("NDEx UUID", String.class)).thenReturn(STORED_UUID.toString());
		CyTable subTable = mock(CyTable.class);
		when(subTable.getRow(SUB_SUID)).thenReturn(subNetworkRow);

		rootNetworkRow = mock(CyRow.class);
		when(rootNetworkRow.get(CyNetwork.NAME, String.class)).thenReturn("collection");
		when(rootNetworkRow.get("NDEx UUID", String.class)).thenReturn(STORED_UUID.toString());
		CyTable rootTable = mock(CyTable.class);
		when(rootTable.getRow(ROOT_SUID)).thenReturn(rootNetworkRow);

		subNetwork = mock(CySubNetwork.class);
		when(subNetwork.getSUID()).thenReturn(SUB_SUID);
		when(subNetwork.getRow(subNetwork)).thenReturn(subNetworkRow);
		when(subNetwork.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS)).thenReturn(subTable);

		rootNetwork = mock(CyRootNetwork.class);
		when(rootNetwork.getSUID()).thenReturn(ROOT_SUID);
		when(rootNetwork.getRow(rootNetwork)).thenReturn(rootNetworkRow);
		when(rootNetwork.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS)).thenReturn(rootTable);
		when(subNetwork.getRootNetwork()).thenReturn(rootNetwork);

		when(networkManager.getNetwork(SUB_SUID)).thenReturn(subNetwork);

		NetworkSummary summary = mock(NetworkSummary.class);
		when(summary.getModificationTime()).thenReturn(new Timestamp(0));
		when(mal.getNetworkSummaryById(any(UUID.class))).thenReturn(summary);
		when(mal.createCX2Network(any(InputStream.class), any(VisibilityType.class), any(UUID.class)))
				.thenReturn(STORED_UUID);
		when(mal.createCXNetwork(any(InputStream.class))).thenReturn(STORED_UUID);
	}

	private NDExSaveParameters params() {
		NDExSaveParameters params = new NDExSaveParameters("alice", "secret",
				"https://www.ndexbio.org/v2", new HashMap<>(), false);
		params.isPublic = null;
		return params;
	}

	private NetworkExportTask task(NDExBasicSaveParameters params, boolean writeCollection, boolean isUpdate,
			CxFormat format) throws Exception {
		return new NetworkExportTask(mal, SUB_SUID, cxStream, params, writeCollection, isUpdate, format,
				networkManager);
	}

	// ---------- create ----------

	@Test
	public void singleNetworkCreatesOverCx2WithVisibilityAndFolder() throws Exception {
		UUID folder = UUID.randomUUID();
		NDExSaveParameters params = params();
		params.visibility = "UNLISTED";
		params.folder = folder.toString();

		task(params, false, false, CxFormat.CX2).run(taskMonitor);

		verify(mal).createCX2Network(cxStream, VisibilityType.UNLISTED, folder);
		verify(mal, never()).createCXNetwork(any(InputStream.class));
	}

	@Test
	public void collectionStillCreatesOverV2Cx1() throws Exception {
		task(params(), true, false, CxFormat.CX1).run(taskMonitor);

		verify(mal).createCXNetwork(cxStream);
		verify(mal, never()).createCX2Network(any(InputStream.class), any(VisibilityType.class), any(UUID.class));
	}

	@Test
	public void unsetVisibilitySendsNone() throws Exception {
		task(params(), false, false, CxFormat.CX2).run(taskMonitor);

		verify(mal).createCX2Network(eq(cxStream), eq((VisibilityType) null), eq((UUID) null));
	}

	@Test
	public void legacyIsPublicStillMapsToVisibilityWhenExplicitlySet() throws Exception {
		NDExSaveParameters params = params();
		params.isPublic = Boolean.TRUE;

		task(params, false, false, CxFormat.CX2).run(taskMonitor);

		verify(mal).createCX2Network(eq(cxStream), eq(VisibilityType.PUBLIC), eq((UUID) null));
	}

	// ---------- UUID write-back ----------

	@Test
	public void singleNetworkTagsTheSubNetwork() throws Exception {
		task(params(), false, false, CxFormat.CX2).run(taskMonitor);

		verify(subNetworkRow).set("NDEx UUID", STORED_UUID.toString());
		verify(rootNetworkRow, never()).set(eq("NDEx UUID"), any());
	}

	@Test
	public void collectionTagsTheRootNetwork() throws Exception {
		task(params(), true, false, CxFormat.CX1).run(taskMonitor);

		verify(rootNetworkRow).set("NDEx UUID", STORED_UUID.toString());
		verify(subNetworkRow, never()).set(eq("NDEx UUID"), any());
	}

	@Test
	public void aFailedUploadLeavesTheLocalBindingAlone() throws Exception {
		when(mal.createCX2Network(any(InputStream.class), any(VisibilityType.class), any(UUID.class)))
				.thenThrow(new RuntimeException("NDEx is down"));
		try {
			task(params(), false, false, CxFormat.CX2).run(taskMonitor);
			fail("expected the export to fail");
		} catch (NetworkExportException expected) {
			// nothing written: the network must not end up bound to a network that was never created
			verify(subNetworkRow, never()).set(eq("NDEx UUID"), any());
			verify(rootNetworkRow, never()).set(eq("NDEx UUID"), any());
		}
	}

	// ---------- update ----------

	@Test
	public void updateCarriesVisibilityOnAPlainBasicSaveParameters() throws Exception {
		// The update endpoints take NDExBasicSaveParameters, so visibility has to live on the base
		// class. Using the base type here is what makes a regression back onto the subclass fail.
		NDExBasicSaveParameters params = new NDExBasicSaveParameters();
		params.username = "alice";
		params.password = "secret";
		params.serverUrl = "https://www.ndexbio.org/v2";
		params.metadata = new HashMap<>();
		params.visibility = "PUBLIC";

		task(params, false, true, CxFormat.CX2).run(taskMonitor);

		verify(mal).updateCX2Network(STORED_UUID, cxStream, VisibilityType.PUBLIC);
	}

	@Test
	public void updateWithAFolderMovesTheNetwork() throws Exception {
		UUID folder = UUID.randomUUID();
		NDExSaveParameters params = params();
		params.folder = folder.toString();

		task(params, false, true, CxFormat.CX2).run(taskMonitor);

		org.mockito.ArgumentCaptor<MoveNetworksRequest> move =
				org.mockito.ArgumentCaptor.forClass(MoveNetworksRequest.class);
		verify(mal).moveNetworks(move.capture());
		assertEquals(folder, move.getValue().getTargetFolder());
		assertEquals(Arrays.asList(STORED_UUID), move.getValue().getNetworks());
	}

	@Test
	public void updateWithoutAFolderDoesNotMove() throws Exception {
		task(params(), false, true, CxFormat.CX2).run(taskMonitor);

		verify(mal, never()).moveNetworks(any(MoveNetworksRequest.class));
	}

	@Test
	public void anExplicitNetworkIdOverridesTheStoredUuid() throws Exception {
		UUID target = UUID.randomUUID();
		NDExSaveParameters params = params();
		params.networkId = target.toString();

		task(params, false, true, CxFormat.CX2).run(taskMonitor);

		verify(mal).updateCX2Network(eq(target), eq(cxStream), any(VisibilityType.class));
		// and the local binding is retargeted, but only now that the upload succeeded
		verify(subNetworkRow).set("NDEx UUID", target.toString());
	}

	// ---------- validation ----------

	@Test
	public void visibilityIsRejectedOnACollection() throws Exception {
		NDExSaveParameters params = params();
		params.visibility = "PUBLIC";
		assertMessageContains(params, true, "not supported when saving a collection");
	}

	@Test
	public void folderIsRejectedOnACollection() throws Exception {
		NDExSaveParameters params = params();
		params.folder = "My Project";
		assertMessageContains(params, true, "not supported when saving a collection");
	}

	@Test
	public void anUnrecognisedVisibilityIsRejected() throws Exception {
		NDExSaveParameters params = params();
		params.visibility = "SEMI_PUBLIC";
		assertMessageContains(params, false, "Expected one of PUBLIC, PRIVATE, UNLISTED");
	}

	@Test
	public void aMalformedNetworkIdIsRejected() throws Exception {
		NDExSaveParameters params = params();
		params.networkId = "not-a-uuid";
		NetworkExportTask task = task(params, false, true, CxFormat.CX2);
		try {
			task.run(taskMonitor);
			fail("expected NetworkExportException");
		} catch (NetworkExportException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("Expected a UUID"));
		}
	}

	// ---------- folder name resolution ----------

	@Test
	public void aFolderNameIsResolvedToItsUuid() throws Exception {
		UUID folderId = UUID.randomUUID();
		when(mal.getMyFolders(org.mockito.Matchers.anyInt())).thenReturn(Arrays.asList(folder("My Project", folderId)));

		NDExSaveParameters params = params();
		params.folder = "my project"; // case-insensitive
		task(params, false, false, CxFormat.CX2).run(taskMonitor);

		verify(mal).createCX2Network(eq(cxStream), any(VisibilityType.class), eq(folderId));
	}

	@Test
	public void anUnknownFolderNameListsTheAvailableOnes() throws Exception {
		when(mal.getMyFolders(org.mockito.Matchers.anyInt())).thenReturn(Arrays.asList(folder("Other", UUID.randomUUID())));

		NDExSaveParameters params = params();
		params.folder = "My Project";
		assertMessageContains(params, false, "Available folders: Other");
	}

	@Test
	public void anAmbiguousFolderNameAsksForTheUuid() throws Exception {
		when(mal.getMyFolders(org.mockito.Matchers.anyInt())).thenReturn(
				Arrays.asList(folder("Shared", UUID.randomUUID()), folder("Shared", UUID.randomUUID())));

		NDExSaveParameters params = params();
		params.folder = "Shared";
		assertMessageContains(params, false, "More than one NDEx folder");
	}

	private static NdexFolder folder(String name, UUID id) {
		NdexFolder folder = new NdexFolder();
		folder.setName(name);
		folder.setExternalId(id);
		return folder;
	}

	private void assertMessageContains(NDExBasicSaveParameters params, boolean writeCollection, String expected)
			throws Exception {
		NetworkExportTask task = task(params, writeCollection, false,
				writeCollection ? CxFormat.CX1 : CxFormat.CX2);
		try {
			task.run(taskMonitor);
			fail("expected NetworkExportException containing: " + expected);
		} catch (NetworkExportException e) {
			assertTrue(e.getMessage(), e.getMessage().contains(expected));
		}
	}
}
