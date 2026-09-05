package org.cytoscape.cyndex2.internal.task.command;

import org.cytoscape.cyndex2.internal.util.NdexProfileResolver;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Publishes {@link NDExListProfilesCommandTask} as a Cytoscape desktop command.
 *
 * {@link #createTaskIterator()} only constructs the task. Cytoscape calls it on every command
 * factory whenever the command list is requested, so doing work here would break command discovery
 * for the whole application.
 */
public class NDExListProfilesCommandTaskFactory extends AbstractTaskFactory {

	private final NdexProfileResolver profileResolver;

	public NDExListProfilesCommandTaskFactory(final NdexProfileResolver profileResolver) {
		this.profileResolver = profileResolver;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new NDExListProfilesCommandTask(profileResolver));
	}
}
