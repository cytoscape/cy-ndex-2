package org.cytoscape.cyndex2.internal.task.command;

import java.util.UUID;

import org.cytoscape.cyndex2.internal.rest.parameter.NDExImportParameters;
import org.cytoscape.cyndex2.internal.task.NDExImportTaskFactory;
import org.cytoscape.cyndex2.internal.util.NdexProfileResolver;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * {@code ndex download network} — downloads a network from NDEx by UUID.
 */
public class NDExDownloadNetworkCommandTask extends AbstractNdexCommandTask {

	@Tunable(description = "UUID of the NDEx network to download",
			longDescription = "The UUID of the NDEx network to download into Cytoscape.",
			exampleStringValue = "12345678-abcd-1234-abcd-1234567890ab",
			required = true)
	public String networkId = null;

	@Tunable(description = "NDEx profile to download as, given as username@serverUrl",
			longDescription = "The CyNDEx-2 sign-in profile to use, written as username@serverUrl. "
					+ "When omitted, the profile currently selected in CyNDEx-2 is used; if none is selected and "
					+ "none are configured, this runs anonymously against the public NDEx server at "
					+ "https://www.ndexbio.org, which can only reach public networks. "
					+ "Run 'ndex list profiles' to see the configured profiles.",
			exampleStringValue = "alice@https://www.ndexbio.org/v2",
			required = false)
	public String profile = null;

	@Tunable(description = "Access key for a network shared by link",
			longDescription = "An NDEx access key, for downloading a network that has been shared by link.",
			exampleStringValue = "",
			required = false)
	public String accessKey = null;

	@Tunable(description = "Create a view for the downloaded network",
			longDescription = "Whether to build a network view after import. Left unset, the CX reader's own "
					+ "default applies.",
			exampleStringValue = "true",
			required = false)
	public Boolean createView = null;

	private final NdexProfileResolver profileResolver;
	private final CyNetworkManager networkManager;
	private final NdexTaskFactories.ImportTaskFactorySupplier importFactories;

	public NDExDownloadNetworkCommandTask(final NdexProfileResolver profileResolver,
			final CyNetworkManager networkManager) {
		this(profileResolver, networkManager, NdexTaskFactories.DEFAULT_IMPORT);
	}

	/** For tests: supply the import factory instead of reaching an NDEx server. */
	NDExDownloadNetworkCommandTask(final NdexProfileResolver profileResolver,
			final CyNetworkManager networkManager,
			final NdexTaskFactories.ImportTaskFactorySupplier importFactories) {
		this.profileResolver = profileResolver;
		this.networkManager = networkManager;
		this.importFactories = importFactories;
	}

	@Override
	public void run(final TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Downloading network from NDEx");

		if (networkId == null || networkId.trim().isEmpty()) {
			throw new IllegalArgumentException("networkId is required. Give the UUID of an NDEx network.");
		}
		final UUID uuid;
		try {
			uuid = UUID.fromString(networkId.trim());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid networkId '" + networkId + "'. Expected a UUID.");
		}

		final Server server = profileResolver.resolveOrAnonymous(profile);
		final NDExImportParameters params = new NDExImportParameters(uuid.toString(), server.getUsername(),
				server.getPassword(), server.getUrl(), emptyToNull(accessKey), null, createView);

		final NDExImportTaskFactory importFactory = importFactories.create(params);
		taskMonitor.setStatusMessage("Reading network from " + server.getUrl());
		runInline(importFactory.createTaskIterator(), taskMonitor);

		final Long suid = importFactory.getSUID();
		final ObjectMapper mapper = new ObjectMapper();
		final ObjectNode result = mapper.createObjectNode();
		if (suid == null) {
			result.putNull("suid");
		} else {
			result.put("suid", suid);
		}
		result.put("uuid", uuid.toString());
		final String name = nameOf(suid);
		if (name == null) {
			result.putNull("name");
		} else {
			result.put("name", name);
		}
		setResult(result);
	}

	/**
	 * The imported network's name. The import task exposes only the SUID, so look the network up
	 * through the network manager rather than plumbing the CyNetwork out of it.
	 */
	private String nameOf(final Long suid) {
		if (suid == null) {
			return null;
		}
		final CyNetwork network = networkManager.getNetwork(suid);
		return network == null ? null : network.getRow(network).get(CyNetwork.NAME, String.class);
	}

	private static String emptyToNull(final String value) {
		return value == null || value.trim().isEmpty() ? null : value.trim();
	}
}
