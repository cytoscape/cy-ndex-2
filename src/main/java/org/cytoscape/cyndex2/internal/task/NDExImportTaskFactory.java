package org.cytoscape.cyndex2.internal.task;

import java.io.IOException;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;

import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.CxFormat;
import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.cyndex2.internal.util.NdexServerCapabilities;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorBuilder;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorType;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExImportParameters;
import org.cytoscape.cyndex2.internal.util.UserAgentUtil;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

public class NDExImportTaskFactory extends AbstractTaskFactory {

	private ErrorBuilder errorBuilder;
	private NDExImportParameters params;

	private NetworkImportTask importer;

	private final CxFormat format;
	private final NdexServerCapabilities serverCapabilities;

	public NDExImportTaskFactory(NDExImportParameters params) {
		this(params, CxFormat.CX2, CyServiceModule.INSTANCE.getErrorBuilder(),
				new NdexServerCapabilities(CyServiceModule.getAdminStatusService()));
	}

	/** For tests: inject the CX format, error builder and v3 capability probe. */
	NDExImportTaskFactory(NDExImportParameters params, CxFormat format, ErrorBuilder errorBuilder,
			NdexServerCapabilities serverCapabilities) {
		super();
		this.params = params;
		this.format = format;
		this.errorBuilder = errorBuilder;
		this.serverCapabilities = serverCapabilities;
	}

	private NetworkImportTask buildImportTask() throws IOException, NdexException {
		UUID uuid = validateImportParameters(params);

		if (params.username != null && params.password != null) {
			final String serverUrl = params.serverUrl == null ? "http://ndexbio.org/v2/" : params.serverUrl;

			final NdexRestClient client = new NdexRestClient(params.username, params.password, serverUrl,
					UserAgentUtil.getUserAgent());
			final NdexRestClientModelAccessLayer mal = new NdexRestClientModelAccessLayer(client);
			requireServerSupport(serverUrl);
			return new NetworkImportTask(mal, uuid, params.accessKey, params.createView, format);
		} else {
			final NdexRestClient client = new NdexRestClient(null, null, params.serverUrl,
					UserAgentUtil.getUserAgent());
			if (params.idToken != null)
				client.signIn(params.idToken);

			final NdexRestClientModelAccessLayer mal = new NdexRestClientModelAccessLayer(client);

			requireServerSupport(params.serverUrl);
			return new NetworkImportTask(mal, uuid, params.accessKey, params.createView, format);
		}
	}

	/** CX2 is only readable from an NDEx v3 server; CX1 works against any of them. */
	private void requireServerSupport(String serverUrl) {
		if (format == CxFormat.CX2) {
			serverCapabilities.requireV3(serverUrl);
		}
	}

	@Override
	public TaskIterator createTaskIterator() {
		try {
			importer = buildImportTask();
			return new TaskIterator(importer);
		} catch (IOException | NdexException e) {
			final String message = "Failed to connect to server and retrieve network. " + e.getMessage();
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	private UUID validateImportParameters(NDExImportParameters params) {
		if (params == null) {
			final String message = "No import parameters found.";
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INVALID_PARAMETERS);
		}
		if (params.serverUrl == null) {
			params.serverUrl = "http://ndexbio.org/v2";
		}
		if (params.uuid == null) {
			final String message = "Must provide a uuid to import a network";
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INVALID_PARAMETERS);
		}
		try {
			return UUID.fromString(params.uuid);
		} catch (IllegalArgumentException e) {
			String message = "Invalid UUID parameter: " + params.uuid + ". Must conform to UUID standards";
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INVALID_PARAMETERS);
		}
	}

	public long getSUID() {
		return importer.getSUID();
	}

}
