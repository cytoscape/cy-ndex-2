package org.cytoscape.cyndex2.internal.task.command;

import java.util.HashMap;
import java.util.UUID;

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
 * Shared behaviour for the two commands that write the current network to NDEx.
 *
 * Creating a new network and replacing an existing one are separate commands on purpose. When the
 * choice was a single optional argument, a caller that did not specify simply got one of them --
 * and the quiet default produced a duplicate network. Two names mean a caller who does not know
 * which is wanted has nothing to fall back on, and has to establish the intent first.
 *
 * The arguments here are inherited by both commands. Cytoscape discovers tunables with
 * {@code Class.getFields()}, which includes inherited public fields, so they must stay public.
 */
public abstract class AbstractNdexNetworkWriteTask extends AbstractNdexCommandTask {

	@Tunable(description = "NDEx profile to save as, given as username@serverUrl",
			longDescription = "The CyNDEx-2 sign-in profile to use, written as username@serverUrl. "
					+ "When omitted, the profile currently selected in CyNDEx-2 is used. Saving to NDEx "
					+ "requires a signed-in profile, so this fails if none is selected. Check which sign-in "
					+ "profiles are configured in CyNDEx-2 if you need one.",
			exampleStringValue = "alice@https://www.ndexbio.org/v2",
			required = false)
	public String profile = null;

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

	protected AbstractNdexNetworkWriteTask(final NdexProfileResolver profileResolver,
			final CyApplicationManager applicationManager,
			final NdexTaskFactories.ExportTaskFactorySupplier exportFactories) {
		this.profileResolver = profileResolver;
		this.applicationManager = applicationManager;
		this.exportFactories = exportFactories;
		this.visibility.setSelectedValue("PRIVATE");
	}

	/**
	 * The NDEx network this command writes to, or null to create a new one. Implementations validate
	 * their own arguments here and throw with a usable message -- {@code required = true} on a tunable
	 * reaches the generated schema but is never enforced before the task runs.
	 */
	protected abstract String targetNetworkId();

	/** Wording for the progress line, since "creating" and "updating" are different operations. */
	protected abstract String progressTitle();

	@Override
	public void run(final TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle(progressTitle());

		final String target = targetNetworkId();

		final CyNetwork network = applicationManager.getCurrentNetwork();
		if (network == null) {
			throw new IllegalArgumentException(
					"No current network. Select a network in Cytoscape before saving it to NDEx.");
		}

		final Server server = profileResolver.resolve(profile);
		if (server.getUsername() == null || server.getPassword() == null) {
			throw new IllegalArgumentException("Profile '" + profileName(server)
					+ "' is not signed in. Saving to NDEx requires a signed-in profile.");
		}

		final NDExSaveParameters params = new NDExSaveParameters(server.getUsername(), server.getPassword(),
				server.getUrl(), new HashMap<>(), false);
		params.isPublic = null;
		params.visibility = visibility.getSelectedValue();
		params.folder = folder;
		params.networkId = target;

		final NDExExportTaskFactory exportFactory = exportFactories.create(params, target != null);

		taskMonitor.setStatusMessage("Sending network to " + server.getUrl());
		runInline(exportFactory.createTaskIterator(network), taskMonitor);

		final ObjectNode result = new ObjectMapper().createObjectNode();
		putText(result, "uuid", exportFactory.getUUID() == null ? null : exportFactory.getUUID().toString());
		putText(result, "url", networkUrl(server, exportFactory.getUUID()));
		putText(result, "visibility", params.visibility);
		putText(result, "folderId",
				exportFactory.getFolderId() == null ? null : exportFactory.getFolderId().toString());
		setResult(result);
	}

	/** Jackson's put(String, String) is ambiguous against a bare null, so be explicit about the type. */
	protected static void putText(final ObjectNode node, final String field, final String value) {
		if (value == null) {
			node.putNull(field);
		} else {
			node.put(field, value);
		}
	}

	private static String profileName(final Server server) {
		return (server.getUsername() == null ? "" : server.getUsername()) + "@" + server.getUrl();
	}

	private static String networkUrl(final Server server, final UUID uuid) {
		if (uuid == null) {
			return null;
		}
		// Profile URLs are stored exactly as the user typed them, so a bare host like "www.ndexbio.org"
		// is normal. Add a scheme, or the result is not a usable link.
		return UrlUtils.addHttpsProtocol(UrlUtils.stripApiVersion(server.getUrl())) + "/viewer/networks/" + uuid;
	}
}
