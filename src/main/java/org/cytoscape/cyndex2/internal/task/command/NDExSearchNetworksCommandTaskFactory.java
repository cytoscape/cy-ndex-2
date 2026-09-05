package org.cytoscape.cyndex2.internal.task.command;

import org.cytoscape.cyndex2.internal.util.NdexProfileResolver;
import org.cytoscape.cyndex2.internal.util.NdexServerCapabilities;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Publishes {@link NDExSearchNetworksCommandTask} as a Cytoscape desktop command.
 *
 * {@link #createTaskIterator()} only constructs the task. Cytoscape calls it on every command
 * factory whenever the command list is requested, so doing NDEx I/O or throwing here would break
 * command discovery for the whole application.
 */
public class NDExSearchNetworksCommandTaskFactory extends AbstractTaskFactory {

	private final NdexProfileResolver profileResolver;
	private final NdexServerCapabilities serverCapabilities;

	public NDExSearchNetworksCommandTaskFactory(NdexProfileResolver profileResolver, NdexServerCapabilities serverCapabilities) {
		this.profileResolver = profileResolver;
		this.serverCapabilities = serverCapabilities;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new NDExSearchNetworksCommandTask(profileResolver, serverCapabilities));
	}
}
