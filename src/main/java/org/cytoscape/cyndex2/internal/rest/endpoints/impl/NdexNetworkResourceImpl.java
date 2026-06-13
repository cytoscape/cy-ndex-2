package org.cytoscape.cyndex2.internal.rest.endpoints.impl;

import java.io.InputStream;
import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.SwingUtilities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ci.CIWrapping;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.cyndex2.internal.CxTaskFactoryManager;
import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.cyndex2.internal.rest.NdexClient;
import org.cytoscape.cyndex2.internal.rest.SimpleNetworkSummary;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexNetworkResource;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorBuilder;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorType;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExBasicSaveParameters;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExImportParameters;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExSaveParameters;
import org.cytoscape.cyndex2.internal.rest.response.NdexBaseResponse;
import org.cytoscape.cyndex2.internal.rest.response.SummaryResponse;
import org.cytoscape.cyndex2.internal.task.NDExExportTaskFactory;
import org.cytoscape.cyndex2.internal.task.NDExImportTaskFactory;
import org.cytoscape.cyndex2.internal.util.CIServiceManager;
import org.cytoscape.cyndex2.internal.util.NDExNetworkManager;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.cyndex2.internal.util.ServerManager;
import org.cytoscape.cyndex2.internal.util.UpdateUtil;
import org.cytoscape.cyndex2.internal.util.UserAgentUtil;
import org.cytoscape.io.read.AbstractCyNetworkReader;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.work.util.ListSingleSelection;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NdexNetworkResourceImpl implements NdexNetworkResource {

	private static final Logger logger = LoggerFactory.getLogger(NdexNetworkResourceImpl.class);

	private final NdexClient client;

	private final CyNetworkManager networkManager;
	private final CyApplicationManager appManager;
	private final CIServiceManager ciServiceManager;

	private final ErrorBuilder errorBuilder;

	public NdexNetworkResourceImpl(final NdexClient client, CyApplicationManager appManager,
			CyNetworkManager networkManager, CIServiceManager ciServiceTracker) {

		this.client = client;
		this.ciServiceManager = ciServiceTracker;

		this.errorBuilder = CyServiceModule.INSTANCE.getErrorBuilder();

		this.networkManager = networkManager;
		this.appManager = appManager;

		// this.tfManager = tfManager;
	}

	private CyNetwork getCurrentNetwork() {
		CyNetwork network = appManager.getCurrentNetwork();
		if (network == null) {
			final String message = "Current network does not exist. Select a network or specify an SUID.";
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INVALID_PARAMETERS);
		}
		return network;
	}

	private CyNetwork getNetworkFromSUID(Long suid) throws WebApplicationException {
		/*
		 * Attempt to get the CyNetwork object from an SUID. If the SUID is null, get
		 * the currently selected CySubNetwork. An SUID may specify a subnetwork or a
		 * collection object in Cytoscape.
		 * 
		 * If there is not network with the given SUID, or the current network is null,
		 * throw a WebApplicationException. This function will not return null
		 */
		if (suid == null) {
			logger.error("SUID is missing");
			throw errorBuilder.buildException(Status.BAD_REQUEST, "SUID is not specified.",
					ErrorType.INVALID_PARAMETERS);
		}
		CyNetwork network = networkManager.getNetwork(suid.longValue());

		if (network == null) {
			// Check if the suid points to a collection
			for (CyNetwork net : networkManager.getNetworkSet()) {
				CyRootNetwork root = ((CySubNetwork) net).getRootNetwork();
				Long rootSUID = root.getSUID();
				if (rootSUID.compareTo(suid) == 0) {
					network = root;
					break;
				}
			}
		}
		if (network == null) {
			// Network is not available
			final String message = "Network/Collection with SUID " + String.valueOf(suid) + " does not exist.";
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INVALID_PARAMETERS);
		}
		return network;
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse createNetworkFromNdex(final NDExImportParameters params) {

		try {
			NDExImportTaskFactory importFactory = getNDExImportTaskFactory(params);
			TaskIterator iter = importFactory.createTaskIterator();

			execute(iter);

			final NdexBaseResponse response = new NdexBaseResponse(importFactory.getSUID(), params.uuid);

			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	public NDExExportTaskFactory getNDExExportTaskFactory(final NDExBasicSaveParameters params, boolean isUpdate) {
		return new NDExExportTaskFactory(params, isUpdate);
	}

	public NDExImportTaskFactory getNDExImportTaskFactory(final NDExImportParameters params) {
		return new NDExImportTaskFactory(params);
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse saveNetworkToNdex(final Long suid, final NDExSaveParameters params) {
		try {
			NDExExportTaskFactory exportFactory = getNDExExportTaskFactory(params, false);
			CyNetwork network = getNetworkFromSUID(suid);

			TaskIterator iter = exportFactory.createTaskIterator(network);

			execute(iter);

			UUID newUUID = exportFactory.getUUID();
			if (newUUID == null) {
				final String message = "No UUID returned from NDEx API.";
				logger.error(message);
				throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
			}

			final NdexBaseResponse response = new NdexBaseResponse(suid, newUUID.toString());
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);

		} catch (InstantiationException | IllegalAccessException e2) {
			final String message = "Could not create wrapped CI JSON response. Error: " + e2.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}

	}

	@Override
	@CIWrapping
	public CINdexBaseResponse saveCurrentNetworkToNdex(NDExSaveParameters params) {
		final CyNetwork network = getCurrentNetwork();
		return saveNetworkToNdex(network.getSUID(), params);
	}

	@CIWrapping
	@Override
	public CISummaryResponse getCurrentNetworkSummary() {
		final CyNetwork network = getCurrentNetwork();
		final CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
		final SummaryResponse response = buildSummary(root, (CySubNetwork) network);
		try {
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CISummaryResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	@CIWrapping
	@Override
	public CISummaryResponse getNetworkSummary(Long suid) {
		CyNetwork network = networkManager.getNetwork(suid.longValue());
		CyRootNetwork rootNetwork = null;
		if (network == null) {
			// Check if the suid points to a collection
			for (CyNetwork net : networkManager.getNetworkSet()) {
				if (net instanceof CySubNetwork) {
					CyRootNetwork root = ((CySubNetwork) net).getRootNetwork();
					Long rootSUID = root.getSUID();
					if (rootSUID.compareTo(suid) == 0) {
						rootNetwork = root;
						break;
					}
				}
			}
		} else {
			rootNetwork = ((CySubNetwork) network).getRootNetwork();
		}

		if (rootNetwork == null) {
			// Current network is not available
			final String message = "Cannot find collection/network with SUID " + String.valueOf(suid) + ".";
			logger.error(message);
			final CIError ciError = ciServiceManager.getCIErrorFactory().getCIError(Status.BAD_REQUEST.getStatusCode(),
					"urn:cytoscape:ci:ndex:v1:errors:1", message, URI.create("file:///log"));
			throw ciServiceManager.getCIExceptionFactory().getCIException(Status.BAD_REQUEST.getStatusCode(),
					new CIError[] { ciError });
		}

		final SummaryResponse response = buildSummary(rootNetwork, (CySubNetwork) network);
		try {
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CISummaryResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	private final static SummaryResponse buildSummary(final CyRootNetwork root, final CySubNetwork network) {
		final SummaryResponse summary = new SummaryResponse();

		// Network local table
		final SimpleNetworkSummary rootSummary = buildNetworkSummary(root, root.getDefaultNetworkTable(),
				root.getSUID());
		if (network != null)
			summary.currentNetworkSuid = network.getSUID();
		summary.currentRootNetwork = rootSummary;
		List<SimpleNetworkSummary> members = new ArrayList<>();
		root.getSubNetworkList().stream().forEach(
				subnet -> members.add(buildNetworkSummary(subnet, subnet.getDefaultNetworkTable(), subnet.getSUID())));
		summary.members = members;

		return summary;
	}

	private final static SimpleNetworkSummary buildNetworkSummary(CyNetwork network, CyTable table, Long networkSuid) {

		SimpleNetworkSummary summary = new SimpleNetworkSummary();
		CyRow row = table.getRow(networkSuid);
		summary.suid = network.getSUID();
		// Get NAME from local table because this is always local.
		summary.name = network.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS).getRow(network.getSUID())
				.get(CyNetwork.NAME, String.class);

		UUID uuid = NDExNetworkManager.getUUID(network);
		if (uuid != null)
			summary.uuid = uuid.toString();

		final Collection<CyColumn> columns = table.getColumns();
		final Map<String, Object> props = new HashMap<>();

		columns.stream().forEach(col -> props.put(col.getName(), row.get(col.getName(), col.getType())));
		summary.props = props;

		return summary;
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse updateNetworkInNdex(Long suid, NDExBasicSaveParameters params) {

		CyNetwork network = getNetworkFromSUID(suid);
		// Check UUID
		UUID uuid;
		try {
			final NdexRestClient nc = new NdexRestClient(params.username, params.password, params.serverUrl,
					UserAgentUtil.getUserAgent());
			final NdexRestClientModelAccessLayer mal = new NdexRestClientModelAccessLayer(nc);
			uuid = UpdateUtil.updateIsPossibleHelper(suid, network instanceof CyRootNetwork, nc, mal);
		} catch (Exception e) {
			final String message = "Unable to update network in NDEx. " + e.getMessage()
					+ " Try saving as a new network.";
			logger.error(message);
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INVALID_PARAMETERS);

		}

		boolean success = updateLoop(network, params);

		if (!success) {
			final String message = "Could not update existing NDEx entry.  NDEx server did not accept your request.";
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}

		final String uuidStr = uuid.toString();

		final NdexBaseResponse response = new NdexBaseResponse(suid, uuidStr);
		try {
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	private final boolean updateExistingNetwork(final CyNetwork network, final NDExBasicSaveParameters params) {

		NDExExportTaskFactory exportFactory = getNDExExportTaskFactory(params, true);
		TaskIterator iter = exportFactory.createTaskIterator(network);
		execute(iter);
		return true;
	}

	private boolean updateLoop(CyNetwork network, NDExBasicSaveParameters params) {
		int retryCount = 0;
		boolean success = false;
		while (retryCount <= 3) {
			try {
				// takes a subnetwork
				success = updateExistingNetwork(network, params);
				if (success) {
					return true;
				}
			} catch (Exception e) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					logger.warn("Retry sleep interrupted", e1);
					Thread.currentThread().interrupt();
				}
				logger.warn("updateNetworkInNdex attempt failed, retrying", e);

			} finally {
				retryCount++;
			}
		}
		return false;
	}
	
	
	@Override
	@CIWrapping
	public CINdexBaseResponse updateNdexUUIDOfNetwork( Long suid,
			final Map<String,String> rec) {
		
		
		Timestamp serverTimestamp;
		UUID verifiedUUID = null;
		String uuidString = rec.get("uuid");
		
		UUID potentialUUID = UUID.fromString(uuidString);
		CyNetwork network = UpdateUtil.getNetworkForSUID(suid,false);
		Server selectedServer =ServerManager.INSTANCE.getSelectedServer();

		try {

			final NdexRestClient nc = new NdexRestClient(selectedServer.getUsername(), selectedServer.getPassword(),
					selectedServer.getUrl(), UserAgentUtil.getUserAgent());
			final NdexRestClientModelAccessLayer mal = new NdexRestClientModelAccessLayer(nc);

			
			verifiedUUID = UpdateUtil.updateIsPossible(network, potentialUUID, nc, mal, false);
			NetworkSummary ns = mal.getNetworkSummaryById(verifiedUUID);
			serverTimestamp = ns.getModificationTime();

		} catch (Exception e) {
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, "Error validating UUID: " + e.getMessage(),
					 ErrorType.INTERNAL);
			
					//"<html><body>Error validating UUID: <br>" + e.getMessage() + "</html></body>", "Invalid UUID",
					//JOptionPane.WARNING_MESSAGE);
			//verifiedUUID = null;
			//serverTimestamp = null;
		}

		if (verifiedUUID != null && serverTimestamp != null) {
			NDExNetworkManager.saveUUID(network, verifiedUUID, serverTimestamp);
		}
		
		final NdexBaseResponse response = new NdexBaseResponse(suid, uuidString);
		try {
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}


	@Override
	@CIWrapping
	public CINdexBaseResponse updateCurrentNetworkInNdex(NDExBasicSaveParameters params) {
		final CyNetwork network = getCurrentNetwork();
		return updateNetworkInNdex(network.getSUID(), params);
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse createNetworkFromCx(final InputStream in) {

		InputStreamTaskFactory taskFactory = CxTaskFactoryManager.INSTANCE.getCxReaderFactory();
		TaskIterator iter = taskFactory.createTaskIterator(in, null);

		// Get task to get SUID
		AbstractCyNetworkReader reader = (AbstractCyNetworkReader) iter.next();
		reader.setRootNetworkList(new ListSingleSelection<String>());
		iter.append(reader);

		execute(iter);

		for (CyNetwork net : reader.getNetworks()) {
			networkManager.addNetwork(net);
		}
		reader.buildCyNetworkView(reader.getNetworks()[0]);

		Long suid = reader.getNetworks()[0].getSUID();
		final NdexBaseResponse response = new NdexBaseResponse(suid, "");
		try {
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	private void execute(TaskIterator iter) {
		DialogTaskManager tm = CyServiceModule.getService(DialogTaskManager.class);
//		SynchronousTaskManager<?> tm = CyServiceModule.getService(SynchronousTaskManager.class);

		Object lock = new Object();
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				tm.execute(iter, new TaskObserver() {

					@Override
					public void taskFinished(ObservableTask task) {

					}

					@Override
					public void allFinished(FinishStatus finishStatus) {
						synchronized (lock) {
							lock.notify();
						}
					}
				});
			}
		};

		try {
			SwingUtilities.invokeAndWait(runner);
			synchronized (lock) {
				lock.wait();
			}
		} catch (Exception e) {
			logger.warn("invokeAndWait failed", e);
		}
	}

}
