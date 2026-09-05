package org.cytoscape.cyndex2.internal.task.command;

import org.cytoscape.cyndex2.internal.util.NdexProfileResolver;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Publishes {@link NDExDownloadNetworkCommandTask} as a Cytoscape desktop command.
 *
 * {@link #createTaskIterator()} only constructs the task. Cytoscape calls it on every command
 * factory whenever the command list is requested, so doing NDEx I/O or throwing here would break
 * command discovery for the whole application.
 */
public class NDExDownloadNetworkCommandTaskFactory extends AbstractTaskFactory {

	private final NdexProfileResolver profileResolver;
	private final CyNetworkManager networkManager;

	public NDExDownloadNetworkCommandTaskFactory(NdexProfileResolver profileResolver, CyNetworkManager networkManager) {
		this.profileResolver = profileResolver;
		this.networkManager = networkManager;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new NDExDownloadNetworkCommandTask(profileResolver, networkManager));
	}
}
