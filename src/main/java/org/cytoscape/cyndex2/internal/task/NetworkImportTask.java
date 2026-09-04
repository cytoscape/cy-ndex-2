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
import java.lang.reflect.Method;
import java.util.UUID;

import javax.swing.SwingUtilities;

import org.cytoscape.cyndex2.internal.CxFormat;
import org.cytoscape.cyndex2.internal.CxTaskFactoryManager;
import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.cyndex2.internal.util.HeadlessTaskMonitor;
import org.cytoscape.cyndex2.internal.util.NDExNetworkManager;
import org.cytoscape.io.read.AbstractCyNetworkReader;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.write.CyWriter;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CySubNetwork;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

public class NetworkImportTask extends AbstractTask implements ObservableTask {

	final NdexRestClientModelAccessLayer mal;
	final NetworkSummary networkSummary;
	private UUID uuid = null;
	private Long suid = null;
	private String accessKey = null;
	protected InputStream cxStream;
	private Boolean createView = null;
	private final CxFormat format;
	private final CxTaskFactoryManager cxFactories;
	private final CyNetworkManager networkManager;

	public NetworkImportTask(final NdexRestClientModelAccessLayer mal, UUID uuid, String accessKey, final Boolean createView)
			throws IOException, NdexException {
		this(mal, uuid, accessKey, createView, CxFormat.CX1);
	}

	public NetworkImportTask(final NdexRestClientModelAccessLayer mal, UUID uuid, String accessKey, final Boolean createView,
			final CxFormat format) throws IOException, NdexException {
		this(mal, uuid, accessKey, createView, format, CxTaskFactoryManager.INSTANCE,
				CyServiceModule.getService(CyNetworkManager.class));
	}

	/** For tests: inject the CX factories and network manager rather than resolving them from singletons. */
	NetworkImportTask(final NdexRestClientModelAccessLayer mal, UUID uuid, String accessKey, final Boolean createView,
			final CxFormat format, final CxTaskFactoryManager cxFactories, final CyNetworkManager networkManager)
			throws IOException, NdexException {
		super();
		this.uuid = uuid;
		this.mal = mal;
		networkSummary = mal.getNetworkSummaryById(uuid, accessKey);
		this.accessKey = accessKey;
		cxStream = null;
		this.createView = createView;
		this.format = format;
		this.cxFactories = cxFactories;
		this.networkManager = networkManager;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws NetworkImportException {

		// For entire network, we will query again, hence will check
		// credential
		// boolean success = true; // selectedServer.check(mal);
		// if (success) {
		try {
			taskMonitor.setStatusMessage("Fetching network from NDEx");
			if (cxStream == null) {
				UUID id = networkSummary.getExternalId();
				if (format == CxFormat.CX2) {
					cxStream = (accessKey == null) ? mal.getNetworkAsCX2Stream(id)
							: mal.getNetworkAsCX2Stream(id, accessKey);
				} else {
					cxStream = (accessKey == null) ? mal.getNetworkAsCXStream(id)
							: mal.getNetworkAsCXStream(id, accessKey);
				}
			}
			if (cxStream == null) {
				throw new NdexException("Unable to get network as CX stream");
			}
			taskMonitor.setProgress(.4);
			
			final InputStreamTaskFactory cxReaderFactory = cxFactories.getReaderFactory(format);
			if (cxReaderFactory == null) {
				throw new NetworkImportException("No " + format + " reader is available. Install or update the "
						+ "CX Support app to version " + format.getRequiredCxSupportVersion() + " or newer.");
			}
			
			taskMonitor.setStatusMessage("Importing network with CX Reader");
			TaskIterator ti = cxReaderFactory.createTaskIterator(cxStream, null);
			AbstractCyNetworkReader task = (AbstractCyNetworkReader) ti.next();
			// Run the read on the calling thread. Wrapping it in invokeAndWait swallowed every failure
			// into a log warning and threw outright when called from the EDT -- which the desktop
			// commands are not on. Only view construction below needs the EDT.
			try {
				Method setCreateViewMethod = task.getClass().getMethod("setCreateView", Boolean.class);
				setCreateViewMethod.invoke(task, createView);
			} catch (java.lang.NoSuchMethodException e) {
				Logger.getLogger(NetworkImportTask.class.getName()).warning("Unable to explicitly set view creation. Make sure a current version of the CX Support app is installed.");
			}
			task.run(new HeadlessTaskMonitor());
			
			if (cancelled) {
				return;
			}
			
			taskMonitor.setProgress(.7);
			
			int i = 1;
			for (CyNetwork network : task.getNetworks()) {
				if (cancelled) {
					return;
				}
				taskMonitor.setStatusMessage(String.format("Registering network %s/%s...", i, task.getNetworks().length));
				networkManager.addNetwork(network);
				buildViewOnEdt(task, network);
				i++;
			}
			taskMonitor.setProgress(.9);
			final CyNetwork network = task.getNetworks()[0];
			suid = network.getSUID();
			
			if (networkSummary.getSubnetworkIds().size() > 0) {	
				NDExNetworkManager.saveUUID(((CySubNetwork)network).getRootNetwork(), uuid, networkSummary.getModificationTime());
			} else {
				NDExNetworkManager.saveUUID(network, uuid, networkSummary.getModificationTime());
			}
			
		} catch (NetworkImportException e) {
			// Already a precise, user-facing message: let it through rather than re-wrapping it below.
			throw e;
		} catch (IOException ex) {
			throw new NetworkImportException("Failed to parse JSON from NDEx source.");
		} catch (RuntimeException ex2) {
			throw new NetworkImportException(ex2.getMessage());
		} catch (NdexException e) {
			throw new NetworkImportException("Unable to read network from NDEx: " + e.getMessage());
		} catch(Exception e) {
			throw new RuntimeException("Failed to import: " + e.getMessage());
		}
	}
	

	
	/**
	 * Builds the network view on the EDT, or directly when already on it. Confined to view construction:
	 * everything else runs on the calling thread so failures propagate instead of being logged and dropped.
	 */
	private static void buildViewOnEdt(final AbstractCyNetworkReader reader, final CyNetwork network)
			throws InvocationTargetException, InterruptedException {
		if (SwingUtilities.isEventDispatchThread()) {
			reader.buildCyNetworkView(network);
		} else {
			SwingUtilities.invokeAndWait(() -> reader.buildCyNetworkView(network));
		}
	}

	@Override
	public void cancel() {
		super.cancel();
		try {
			cxStream.close();
		} catch (IOException e) {
			Logger.getLogger(NetworkImportTask.class.getName()).log(Level.WARNING, "Failed to close CX stream on cancel", e);
		}
	}

	public class NetworkImportException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = -1186105413302386171L;

		public NetworkImportException(String message) {
			super(message);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		if (suid == null) {
			return null;
		}
		if (type.equals(Long.class)) {
			return (R) suid;
		}
		return null;
	}

	public long getSUID() {
		return suid;
	}

}
