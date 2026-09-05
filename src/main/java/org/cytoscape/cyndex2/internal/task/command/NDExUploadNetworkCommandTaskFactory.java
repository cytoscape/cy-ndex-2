package org.cytoscape.cyndex2.internal.task.command;

import org.cytoscape.cyndex2.internal.util.NdexProfileResolver;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Publishes {@link NDExUploadNetworkCommandTask} as a Cytoscape desktop command.
 *
 * {@link #createTaskIterator()} only constructs the task. Cytoscape calls it on every command
 * factory whenever the command list is requested, so doing NDEx I/O or throwing here would break
 * command discovery for the whole application.
 */
public class NDExUploadNetworkCommandTaskFactory extends AbstractTaskFactory {

	private final NdexProfileResolver profileResolver;
	private final CyApplicationManager applicationManager;

	public NDExUploadNetworkCommandTaskFactory(NdexProfileResolver profileResolver, CyApplicationManager applicationManager) {
		this.profileResolver = profileResolver;
		this.applicationManager = applicationManager;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new NDExUploadNetworkCommandTask(profileResolver, applicationManager));
	}
}
