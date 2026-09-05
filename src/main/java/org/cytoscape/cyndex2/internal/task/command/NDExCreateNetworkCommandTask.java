package org.cytoscape.cyndex2.internal.task.command;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.cyndex2.internal.util.NdexProfileResolver;

/**
 * {@code ndex create network} — saves the current network to NDEx as a NEW network.
 *
 * Tunables live on the task, not the factory: Cytoscape's command executor reads arguments only
 * from the tasks of the iterator, so tunables on a factory are invisible to {@code /v1/commands}
 * and to the Swagger that MCP tooling reads. The inherited arguments are declared on
 * {@link AbstractNdexNetworkWriteTask} and are discovered because they are public.
 */
public class NDExCreateNetworkCommandTask extends AbstractNdexNetworkWriteTask {

	public NDExCreateNetworkCommandTask(final NdexProfileResolver profileResolver,
			final CyApplicationManager applicationManager) {
		this(profileResolver, applicationManager, NdexTaskFactories.DEFAULT_EXPORT);
	}

	/** For tests: supply the export factory instead of reaching an NDEx server. */
	NDExCreateNetworkCommandTask(final NdexProfileResolver profileResolver,
			final CyApplicationManager applicationManager,
			final NdexTaskFactories.ExportTaskFactorySupplier exportFactories) {
		super(profileResolver, applicationManager, exportFactories);
	}

	/** Nothing to overwrite: a null target is what makes the export create a network. */
	@Override
	protected String targetNetworkId() {
		return null;
	}

	@Override
	protected String progressTitle() {
		return "Creating network on NDEx";
	}
}
