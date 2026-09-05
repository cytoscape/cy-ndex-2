package org.cytoscape.cyndex2.internal.task;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;

import org.cytoscape.cyndex2.internal.CxFormat;
import org.cytoscape.cyndex2.internal.CxTaskFactoryManager;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorBuilder;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorType;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExBasicSaveParameters;
import org.cytoscape.cyndex2.internal.util.NdexServerCapabilities;
import org.cytoscape.cyndex2.internal.util.UserAgentUtil;
import org.cytoscape.io.write.CyNetworkViewWriterFactory;
import org.cytoscape.io.write.CyWriter;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

public class NDExExportTaskFactory implements NetworkViewTaskFactory, NetworkTaskFactory {

	private final NDExBasicSaveParameters params;
	private final boolean isUpdate;
	private final CxTaskFactoryManager cxFactories;
	private final NdexServerCapabilities serverCapabilities;

	private CyWriter writer;
	private NetworkExportTask exporter;

	public NDExExportTaskFactory(NDExBasicSaveParameters params, boolean isUpdate) {
		this(params, isUpdate, CxTaskFactoryManager.INSTANCE,
				new NdexServerCapabilities(CyServiceModule.getAdminStatusService()));
	}

	/** For tests: inject the CX factories and the v3 capability probe instead of reaching for singletons. */
	NDExExportTaskFactory(NDExBasicSaveParameters params, boolean isUpdate, CxTaskFactoryManager cxFactories,
			NdexServerCapabilities serverCapabilities) {
		super();
		this.params = params;
		this.isUpdate = isUpdate;
		this.cxFactories = cxFactories;
		this.serverCapabilities = serverCapabilities;
	}

	private void setTunables(CyWriter writer, boolean collection) {
		final Class<? extends CyWriter> writerClass = writer.getClass();

		final Method[] writerMethods = writerClass.getMethods();

		Method setWriteSiblingsMethod = null;
		Method setUseCxIdMethod = null;

		for (Method method : writerMethods) {
			if (method.getName().equals("setWriteSiblings")) {
				if (method.getParameterTypes().length == 1 && method.getReturnType() == Void.TYPE) {
					if (method.getParameterTypes()[0].equals(Boolean.class)) {
						setWriteSiblingsMethod = method;
					}
				}
			} else if (method.getName().equals("setUseCxId")) {
				if (method.getParameterTypes().length == 1 && method.getReturnType() == Void.TYPE) {
					if (method.getParameterTypes()[0].equals(Boolean.class)) {
						setUseCxIdMethod = method;
					}
				}
			}
		}

		if (setWriteSiblingsMethod != null) {
			try {
				setWriteSiblingsMethod.invoke(writer, collection);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				// reflection call failed — writer does not support this method
			}
		} else {
			
		}

		if (setUseCxIdMethod != null) {
			try {
				setUseCxIdMethod.invoke(writer, !collection);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				// reflection call failed — writer does not support this method
			}
		} else {
			
		}
	}
	
	private AbstractTask getTaskWrapper(CyNetwork network, boolean writeCollection) {
		
		AbstractTask wrapper = new AbstractTask() {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			@Override
			public void run(TaskMonitor taskMonitor) throws Exception {
				// CX2 carries a single network; a collection needs CX1, which can hold sibling sub-networks.
				final CxFormat format = writeCollection ? CxFormat.CX1 : CxFormat.CX2;
				if (format == CxFormat.CX2) {
					serverCapabilities.requireV3(params.serverUrl);
				}
				CyNetworkViewWriterFactory writerFactory = cxFactories.getWriterFactory(format);
				if (writerFactory == null) {
					throw new IllegalStateException("No " + format + " writer is available. Install or update the "
							+ "CX Support app to version " + format.getRequiredCxSupportVersion() + " or newer.");
				}
				writer = writerFactory.createWriter(out, network);
				setTunables(writer, writeCollection);
				
				writer.run(taskMonitor);
				byte[] bytes = out.toByteArray();
				ByteArrayInputStream in = new ByteArrayInputStream(bytes);
				
				NdexRestClient client = new NdexRestClient(params.username, params.password, params.serverUrl,
						 UserAgentUtil.getUserAgent());
				NdexRestClientModelAccessLayer mal = new NdexRestClientModelAccessLayer(client);
				
				exporter = new NetworkExportTask(mal, network.getSUID(), in, params, writeCollection, isUpdate,
						format);
				getTaskIterator().append(exporter);
			}
			@Override
			public void cancel() {
				super.cancel();
				try {
					out.close();
				} catch (IOException e) {
					throw new RuntimeException("Task cancelled");
				}
			}
		};
		return wrapper;
	}
	
	@Override
	public TaskIterator createTaskIterator(CyNetworkView networkView) {
		validateSaveParameters(params);
		CyNetwork network = networkView.getModel();
		
		for (String column : params.metadata.keySet()) {
			saveMetadata(column, params.metadata.get(column), network);
		}
		
		return new TaskIterator(getTaskWrapper(network, false));
	}

	@Override
	public TaskIterator createTaskIterator(CyNetwork network) {
		validateSaveParameters(params);

		for (String column : params.metadata.keySet()) {
			saveMetadata(column, params.metadata.get(column), network);
		}
		
		boolean writeCollection = network instanceof CyRootNetwork;
		if (writeCollection) {
			network = ((CyRootNetwork) network).getBaseNetwork();
		}
		return new TaskIterator(getTaskWrapper(network, writeCollection));
	}
	
	private void validateSaveParameters(final NDExBasicSaveParameters params) {
		ErrorBuilder errorBuilder = CyServiceModule.INSTANCE.getErrorBuilder();
		if (params == null || params.username == null || params.password == null) {
			throw errorBuilder.buildException(Status.BAD_REQUEST,
					"Must provide save parameters (username and password)", ErrorType.INVALID_PARAMETERS);
		}
		if (params.serverUrl == null) {
			params.serverUrl = "http://ndexbio.org/v2";
		}
		if (params.metadata == null) {
			params.metadata = new HashMap<>();
		}
		// Deliberately no default for isPublic. It was dead for years, so callers that omit it have always
		// got whatever visibility NDEx applies by default; defaulting it to true now that visibility is
		// live would start silently publishing their networks.

	}
	
	private final static void saveMetadata(String columnName, String value, CyNetwork network) {

		final CyTable localTable = network.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS);
		final CyRow row = localTable.getRow(network.getSUID());

		// Create new column if it does not exist
		final CyColumn col = localTable.getColumn(columnName);
		if (col == null) {
			if (value == null || value.isEmpty())
				return;
			localTable.createColumn(columnName, String.class, false);
		}

		// Set the value to local table
		row.set(columnName, value);
	}

	/** The folder the network was placed in, or null when none was requested. */
	public UUID getFolderId() {
		return exporter == null ? null : exporter.getFolderId();
	}

	public UUID getUUID() {
		return exporter.getUUID();
	}

	@Override
	public boolean isReady(CyNetwork network) {
		return network != null;
	}

	@Override
	public boolean isReady(CyNetworkView networkView) {
		return networkView != null;
	}

}
