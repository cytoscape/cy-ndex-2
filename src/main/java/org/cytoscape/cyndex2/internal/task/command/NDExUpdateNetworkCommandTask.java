package org.cytoscape.cyndex2.internal.task.command;

import java.util.UUID;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.cyndex2.internal.util.NdexProfileResolver;
import org.cytoscape.work.Tunable;

/**
 * {@code ndex update network} — replaces the content of an existing NDEx network with the current
 * network.
 *
 * Tunables live on the task, not the factory: Cytoscape's command executor reads arguments only
 * from the tasks of the iterator, so tunables on a factory are invisible to {@code /v1/commands}
 * and to the Swagger that MCP tooling reads. The inherited arguments are declared on
 * {@link AbstractNdexNetworkWriteTask} and are discovered because they are public.
 */
public class NDExUpdateNetworkCommandTask extends AbstractNdexNetworkWriteTask {

	@Tunable(description = "UUID of the NDEx network to replace",
			longDescription = "The UUID of the existing NDEx network whose content is replaced by the current "
					+ "Cytoscape network. Required.",
			exampleStringValue = "12345678-abcd-1234-abcd-1234567890ab",
			required = true)
	public String networkId = null;

	public NDExUpdateNetworkCommandTask(final NdexProfileResolver profileResolver,
			final CyApplicationManager applicationManager) {
		this(profileResolver, applicationManager, NdexTaskFactories.DEFAULT_EXPORT);
	}

	/** For tests: supply the export factory instead of reaching an NDEx server. */
	NDExUpdateNetworkCommandTask(final NdexProfileResolver profileResolver,
			final CyApplicationManager applicationManager,
			final NdexTaskFactories.ExportTaskFactorySupplier exportFactories) {
		super(profileResolver, applicationManager, exportFactories);
	}

	/**
	 * {@code required = true} reaches the generated schema but is never enforced before the task runs
	 * -- the executor binds only the arguments it was given and never notices an absent one. Without
	 * this check a missing id would surface as an opaque failure deep in the export path.
	 */
	@Override
	protected String targetNetworkId() {
		if (networkId == null || networkId.trim().isEmpty()) {
			throw new IllegalArgumentException(
					"networkId is required: give the UUID of the NDEx network to replace.");
		}
		final String trimmed = networkId.trim();
		try {
			UUID.fromString(trimmed);
		} catch (final IllegalArgumentException e) {
			throw new IllegalArgumentException(
					"networkId '" + trimmed + "' is not a UUID. Give the UUID of the NDEx network to replace.");
		}
		return trimmed;
	}

	@Override
	protected String progressTitle() {
		return "Updating network on NDEx";
	}
}
