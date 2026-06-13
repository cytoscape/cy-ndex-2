/*
 * Copyright (c) 2014, the Cytoscape Consortium and the Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.cytoscape.cyndex2.internal.task;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExBasicSaveParameters;
import org.cytoscape.cyndex2.internal.util.NDExNetworkManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cytoscape.work.TaskMonitor;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.core.JsonProcessingException;

public class NetworkExportTask extends AbstractTask implements ObservableTask{

	private final InputStream cxStream;
	private final NDExBasicSaveParameters params;
	private final Long suid;
	private final boolean isUpdate;
	private final NdexRestClientModelAccessLayer mal;
	private final boolean writeCollection;
	
	private UUID networkUUID = null;
	
	
	public NetworkExportTask(NdexRestClientModelAccessLayer mal, Long suid, InputStream cxStream, NDExBasicSaveParameters params, boolean writeCollection, boolean isUpdate) throws JsonProcessingException, IOException, NdexException 
			 {
		super();
		this.params = params;
		this.writeCollection = writeCollection;
		this.isUpdate = isUpdate;
		this.cxStream = cxStream;
		this.suid = suid;
		this.mal = mal;
	
	}

	@Override
	public void cancel() {
		super.cancel();
		try {
			cxStream.close();
		} catch (IOException e) {
			Logger.getLogger(NetworkExportTask.class.getName()).log(Level.WARNING, "Failed to close CX stream on cancel", e);
		}
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws NetworkExportException, InvocationTargetException, InterruptedException, IOException {
		networkUUID = null;
		taskMonitor.setTitle("Exporting CX network to NDEx...");
		CyNetworkManager net_manager = CyServiceModule.getService(CyNetworkManager.class);
		CyNetwork network = net_manager.getNetwork(suid);
		
		CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();

		String collectionName = rootNetwork.getRow(rootNetwork).get(CyNetwork.NAME, String.class);
		String networkName = network.getRow(network).get(CyNetwork.NAME, String.class);

		String uploadName = (params.metadata != null && params.metadata.containsKey(CyNetwork.NAME))
				? params.metadata.get(CyNetwork.NAME)
				: (writeCollection ? collectionName : networkName);
				
		// Set root or network name
		if (writeCollection) {
			rootNetwork.getRow(rootNetwork).set(CyNetwork.NAME, uploadName);
			rootNetwork.getRow(rootNetwork).set(CyRootNetwork.SHARED_NAME, uploadName);
		} else {
			network.getRow(network).set(CyNetwork.NAME, uploadName);
			network.getRow(network).set(CyRootNetwork.SHARED_NAME, uploadName);
		}
		
		try {
			if (cancelled) {
				return;
			}
			taskMonitor.setProgress(.5);
			taskMonitor.setStatusMessage("Uploading network to NDEx");
			
			final CyNetwork referenceNetwork = writeCollection ? rootNetwork : network;
			if (!isUpdate) {
				networkUUID = mal.createCXNetwork(cxStream);
				NetworkSummary networkSummary = mal.getNetworkSummaryById(networkUUID);
				
				NDExNetworkManager.saveUUID(referenceNetwork, networkUUID, networkSummary.getModificationTime());
			} else {
				networkUUID = NDExNetworkManager.getUUID(referenceNetwork);
				if (networkUUID == null) {
					throw new NetworkUpdateException("No UUID found for " + network);
				}
				mal.updateCXNetwork(networkUUID, cxStream);
				NetworkSummary networkSummary = mal.getNetworkSummaryById(networkUUID);
				NDExNetworkManager.updateModificationTimeStamp(referenceNetwork, networkSummary.getModificationTime());
			}
		} catch (NetworkUpdateException e) {
			throw new NetworkExportException("Only networks imported from CyNDEx2 can be updated. Error: " + e.getMessage());
		} catch (IOException e) {
			throw new NetworkExportException("Failed to create CX stream for network. Error: " + e.getMessage());
		} catch (Exception e) {
			throw new NetworkExportException("An error occurred loading the network to NDEx. Error: " + e.getMessage());
		} finally {

			if (cancelled) {
				return;
			}	
		}
		
		if (networkUUID == null) {
			throw new NetworkExportException("There was a problem exporting the network! No UUID found.");
		}
		taskMonitor.setProgress(.9);
		taskMonitor.setStatusMessage("Saving changes to network in Cytoscape");

		//TODO : Update... metadata? any aspects need updating? Apply metadata?
		
		taskMonitor.setProgress(1.0f);
		
	}

	public class NetworkExportException extends RuntimeException {
		/**
		 * 
		 */
		private static final long serialVersionUID = -4168495871463038598L;

		public NetworkExportException(String message) {
			super(message);
		}
	}

	public class NetworkUpdateException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public NetworkUpdateException(String message) {
			super(message);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		if (networkUUID == null)
			return null;
		
		if (type.equals(String.class)) {
			return (R) networkUUID.toString();
		}
		return null;
	}

	public UUID getUUID() {
		return networkUUID;
	}

}
