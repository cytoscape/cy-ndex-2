package org.cytoscape.cyndex2.internal.task.command;

import java.util.HashMap;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExSaveParameters;
import org.cytoscape.cyndex2.internal.task.NDExExportTaskFactory;
import org.cytoscape.cyndex2.internal.util.NdexProfileResolver;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.cyndex2.internal.util.UrlUtils;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * {@code ndex upload network} — uploads the current network to NDEx.
 *
 * Tunables live on the task, not the factory: Cytoscape's command executor reads arguments only
 * from the tasks of the iterator, so tunables on a factory are invisible to {@code /v1/commands}
 * and to the Swagger that MCP tooling reads.
 */
public class NDExUploadNetworkCommandTask extends AbstractNdexCommandTask {

	@Tunable(description = "NDEx profile to upload as, given as username@serverUrl",
			longDescription = "The CyNDEx-2 sign-in profile to use, written as username@serverUrl. "
					+ "When omitted, the profile currently selected in CyNDEx-2 is used. Uploading requires a "
					+ "signed-in profile, so this fails if none is selected. "
					+ "Run 'ndex list profiles' to see the configured profiles.",
			exampleStringValue = "alice@https://www.ndexbio.org/v2",
			required = false)
	public String profile = null;

	@Tunable(description = "UUID of an existing NDEx network to overwrite",
			longDescription = "The UUID of an NDEx network to overwrite. When omitted, a new network is created.",
			exampleStringValue = "12345678-abcd-1234-abcd-1234567890ab",
			required = false)
	public String networkId = null;

	@Tunable(description = "Visibility of the network on NDEx",
			longDescription = "PRIVATE keeps the network to the owner, PUBLIC makes it visible and searchable "
					+ "by everyone, and UNLISTED makes it reachable by link but excluded from search.",
			exampleStringValue = "PRIVATE",
			required = false)
	public ListSingleSelection<String> visibility =
			new ListSingleSelection<>("PRIVATE", "PUBLIC", "UNLISTED");

	@Tunable(description = "NDEx folder to place the network in, as a folder name or UUID",
			longDescription = "The NDEx folder to place the network in, given as either a folder name or a "
					+ "folder UUID. Requires a signed-in profile.",
			exampleStringValue = "My Project",
			required = false)
	public String folder = null;

	private final NdexProfileResolver profileResolver;
	private final CyApplicationManager applicationManager;
	private final NdexTaskFactories.ExportTaskFactorySupplier exportFactories;

	public NDExUploadNetworkCommandTask(final NdexProfileResolver profileResolver,
			final CyApplicationManager applicationManager) {
		this(profileResolver, applicationManager, NdexTaskFactories.DEFAULT_EXPORT);
	}

	/** For tests: supply the export factory instead of reaching an NDEx server. */
	NDExUploadNetworkCommandTask(final NdexProfileResolver profileResolver,
			final CyApplicationManager applicationManager,
			final NdexTaskFactories.ExportTaskFactorySupplier exportFactories) {
		this.profileResolver = profileResolver;
		this.applicationManager = applicationManager;
		this.exportFactories = exportFactories;
		this.visibility.setSelectedValue("PRIVATE");
	}

	@Override
	public void run(final TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Uploading network to NDEx");

		final CyNetwork network = applicationManager.getCurrentNetwork();
		if (network == null) {
			throw new IllegalArgumentException(
					"No current network. Select a network in Cytoscape before uploading.");
		}

		final Server server = profileResolver.resolve(profile);
		if (server.getUsername() == null || server.getPassword() == null) {
			throw new IllegalArgumentException("Profile '" + profileName(server)
					+ "' is not signed in. Uploading to NDEx requires a signed-in profile.");
		}

		final NDExSaveParameters params = new NDExSaveParameters(server.getUsername(), server.getPassword(),
				server.getUrl(), new HashMap<>(), false);
		params.isPublic = null;
		params.visibility = visibility.getSelectedValue();
		params.folder = folder;
		params.networkId = networkId;

		final boolean isUpdate = networkId != null && !networkId.trim().isEmpty();
		final NDExExportTaskFactory exportFactory = exportFactories.create(params, isUpdate);

		taskMonitor.setStatusMessage("Sending network to " + server.getUrl());
		runInline(exportFactory.createTaskIterator(network), taskMonitor);

		final ObjectMapper mapper = new ObjectMapper();
		final ObjectNode result = mapper.createObjectNode();
		putText(result, "uuid", exportFactory.getUUID() == null ? null : exportFactory.getUUID().toString());
		putText(result, "url", networkUrl(server, exportFactory.getUUID()));
		putText(result, "visibility", params.visibility);
		putText(result, "folderId",
				exportFactory.getFolderId() == null ? null : exportFactory.getFolderId().toString());
		setResult(result);
	}

	/** Jackson's put(String, String) is ambiguous against a bare null, so be explicit about the type. */
	private static void putText(final ObjectNode node, final String field, final String value) {
		if (value == null) {
			node.putNull(field);
		} else {
			node.put(field, value);
		}
	}

	private static String profileName(final Server server) {
		return (server.getUsername() == null ? "" : server.getUsername()) + "@" + server.getUrl();
	}

	private static String networkUrl(final Server server, final java.util.UUID uuid) {
		if (uuid == null) {
			return null;
		}
		// Profile URLs are stored exactly as the user typed them, so a bare host like "www.ndexbio.org"
		// is normal. Add a scheme, or the result is not a usable link.
		final String host = UrlUtils.addHttpsProtocol(UrlUtils.stripApiVersion(server.getUrl()));
		return host + "/viewer/networks/" + uuid;
	}
}
