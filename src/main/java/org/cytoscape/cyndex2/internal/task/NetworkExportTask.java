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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.cytoscape.cyndex2.internal.CxFormat;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExSaveParameters;
import org.ndexbio.model.object.MoveNetworksRequest;
import org.ndexbio.model.object.NdexFolder;
import org.ndexbio.model.object.network.VisibilityType;
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

	private static final int FOLDER_LOOKUP_LIMIT = 500;

	private final InputStream cxStream;
	private final NDExBasicSaveParameters params;
	private final Long suid;
	private final boolean isUpdate;
	private final NdexRestClientModelAccessLayer mal;
	private final boolean writeCollection;
	private final CxFormat format;
	private final CyNetworkManager networkManager;
	
	private UUID networkUUID = null;
	private UUID folderId = null;
	
	
	public NetworkExportTask(NdexRestClientModelAccessLayer mal, Long suid, InputStream cxStream, NDExBasicSaveParameters params, boolean writeCollection, boolean isUpdate) throws JsonProcessingException, IOException, NdexException 
			 {
		this(mal, suid, cxStream, params, writeCollection, isUpdate, CxFormat.CX1);
	}

	public NetworkExportTask(NdexRestClientModelAccessLayer mal, Long suid, InputStream cxStream, NDExBasicSaveParameters params, boolean writeCollection, boolean isUpdate, CxFormat format) throws JsonProcessingException, IOException, NdexException 
			 {
		this(mal, suid, cxStream, params, writeCollection, isUpdate, format,
				CyServiceModule.getService(CyNetworkManager.class));
	}

	/** For tests: inject the network manager instead of resolving it from the CyServiceModule singleton. */
	NetworkExportTask(NdexRestClientModelAccessLayer mal, Long suid, InputStream cxStream, NDExBasicSaveParameters params, boolean writeCollection, boolean isUpdate, CxFormat format, CyNetworkManager networkManager) throws JsonProcessingException, IOException, NdexException 
			 {
		super();
		this.params = params;
		this.writeCollection = writeCollection;
		this.isUpdate = isUpdate;
		this.cxStream = cxStream;
		this.suid = suid;
		this.mal = mal;
		this.format = format;
		this.networkManager = networkManager;
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
		CyNetwork network = networkManager.getNetwork(suid);
		
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
			final VisibilityType visibility = resolveVisibility();
			folderId = resolveFolderId();

			if (!isUpdate) {
				networkUUID = (format == CxFormat.CX2)
						? mal.createCX2Network(cxStream, visibility, folderId)
						: mal.createCXNetwork(cxStream);
				NetworkSummary networkSummary = mal.getNetworkSummaryById(networkUUID);
				
				NDExNetworkManager.saveUUID(referenceNetwork, networkUUID, networkSummary.getModificationTime());
			} else {
				// An explicitly named network wins over the one this network was last saved as, so a
				// caller can retarget an upload. The local binding is only rewritten once the upload
				// has actually succeeded -- writing it up front would leave the network pointing at
				// someone else's NDEx entry if the upload failed.
				final UUID requestedUUID = parseRequestedNetworkId();
				networkUUID = requestedUUID != null ? requestedUUID : NDExNetworkManager.getUUID(referenceNetwork);
				if (networkUUID == null) {
					throw new NetworkUpdateException("No UUID found for " + network);
				}
				if (format == CxFormat.CX2) {
					mal.updateCX2Network(networkUUID, cxStream, visibility);
					// Folder placement is a query parameter on create only, so moving is a separate call.
					if (folderId != null) {
						MoveNetworksRequest move = new MoveNetworksRequest();
						move.setTargetFolder(folderId);
						move.setNetworks(Collections.singletonList(networkUUID));
						mal.moveNetworks(move);
					}
				} else {
					mal.updateCXNetwork(networkUUID, cxStream);
				}
				NetworkSummary networkSummary = mal.getNetworkSummaryById(networkUUID);
				if (requestedUUID != null && !requestedUUID.equals(NDExNetworkManager.getUUID(referenceNetwork))) {
					NDExNetworkManager.saveUUID(referenceNetwork, networkUUID, networkSummary.getModificationTime());
				} else {
					NDExNetworkManager.updateModificationTimeStamp(referenceNetwork, networkSummary.getModificationTime());
				}
			}
		} catch (NetworkExportException e) {
			// Already a precise, user-facing message (bad visibility, unknown folder, collection misuse):
			// let it through rather than re-wrapping it in the generic text below.
			throw e;
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

	/** The NDEx network the caller asked to overwrite, or null to use this network's stored UUID. */
	private UUID parseRequestedNetworkId() {
		final String requested = params.networkId;
		if (requested == null || requested.trim().isEmpty()) {
			return null;
		}
		try {
			return UUID.fromString(requested.trim());
		} catch (IllegalArgumentException e) {
			throw new NetworkExportException("Invalid networkId '" + requested + "'. Expected a UUID.");
		}
	}

	/** The folder the network was placed in, or null when none was requested. */
	public UUID getFolderId() {
		return folderId;
	}

	/**
	 * Maps the requested visibility onto the NDEx enum. Validated here rather than left to the server,
	 * which answers an unparseable value with a 500 instead of a 400.
	 */
	private VisibilityType resolveVisibility() {
		String requested = params.visibility;
		if (requested == null || requested.trim().isEmpty()) {
			// Legacy field: only honoured when explicitly set, so existing callers that omit it keep
			// whatever default the server applies.
			if (params instanceof NDExSaveParameters) {
				Boolean isPublic = ((NDExSaveParameters) params).isPublic;
				if (isPublic != null) {
					return isPublic ? VisibilityType.PUBLIC : VisibilityType.PRIVATE;
				}
			}
			return null;
		}
		if (writeCollection) {
			throw new NetworkExportException(
					"'visibility' is not supported when saving a collection; it applies to single networks only.");
		}
		try {
			return VisibilityType.valueOf(requested.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new NetworkExportException("Invalid visibility '" + requested
					+ "'. Expected one of PUBLIC, PRIVATE, UNLISTED.");
		}
	}

	/**
	 * Resolves the requested folder, given as either a UUID or a folder name, to its UUID.
	 * Lives here rather than in a caller so the REST endpoints and the desktop command share it.
	 */
	private UUID resolveFolderId() throws Exception {
		final String requested = params.folder;
		if (requested == null || requested.trim().isEmpty()) {
			return null;
		}
		if (writeCollection) {
			throw new NetworkExportException(
					"'folder' is not supported when saving a collection; it applies to single networks only.");
		}
		final String trimmed = requested.trim();
		try {
			return UUID.fromString(trimmed);
		} catch (IllegalArgumentException notAUuid) {
			// fall through to a name lookup
		}
		final List<NdexFolder> folders = mal.getMyFolders(FOLDER_LOOKUP_LIMIT);
		final List<NdexFolder> matches = folders == null ? Collections.emptyList()
				: folders.stream()
						.filter(f -> trimmed.equalsIgnoreCase(f.getName()))
						.collect(Collectors.toList());
		if (matches.size() == 1) {
			return matches.get(0).getExternalId();
		}
		final String available = folders == null || folders.isEmpty() ? "(none)"
				: folders.stream().map(NdexFolder::getName).collect(Collectors.joining(", "));
		if (matches.isEmpty()) {
			throw new NetworkExportException("No NDEx folder named '" + trimmed
					+ "'. Available folders: " + available + ". A folder UUID may be given instead.");
		}
		throw new NetworkExportException("More than one NDEx folder is named '" + trimmed
				+ "'. Give the folder UUID instead. Available folders: " + available + ".");
	}

}
