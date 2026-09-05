package org.cytoscape.cyndex2.internal.task.command;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.cyndex2.internal.rest.NdexAdminStatusService;
import org.cytoscape.cyndex2.internal.util.NdexProfileResolver;
import org.cytoscape.cyndex2.internal.util.NdexServerCapabilities;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * The one place that knows every ndex command factory.
 *
 * The structural guards all iterate this, so a new command joins them by being added here once
 * rather than in each guard separately -- a guard a new command silently escapes is no guard.
 * {@link NdexCommandPropertiesTest} asserts this list and
 * {@link NdexCommandProperties#all()} stay in step.
 */
final class NdexCommandFixtures {

	private NdexCommandFixtures() {
	}

	/** Built with mocks that are never exercised: constructing a factory must do no work. */
	static List<TaskFactory> allFactories() {
		NdexProfileResolver resolver = mock(NdexProfileResolver.class);
		return Arrays.asList(
				new NDExUploadNetworkCommandTaskFactory(resolver, mock(CyApplicationManager.class)),
				new NDExDownloadNetworkCommandTaskFactory(resolver, mock(CyNetworkManager.class)),
				new NDExSearchNetworksCommandTaskFactory(resolver,
						new NdexServerCapabilities(mock(NdexAdminStatusService.class))),
				new NDExListProfilesCommandTaskFactory(resolver));
	}

	/** Every task the commands produce, one per factory. */
	static List<Task> allTasks() {
		List<Task> tasks = new ArrayList<>();
		for (TaskFactory factory : allFactories()) {
			TaskIterator iterator = factory.createTaskIterator();
			while (iterator.hasNext()) {
				tasks.add(iterator.next());
			}
		}
		return tasks;
	}

	/** The command task classes, for guards that inspect bytecode rather than instances. */
	static List<Class<?>> allTaskClasses() {
		List<Class<?>> classes = new ArrayList<>();
		for (Task task : allTasks()) {
			classes.add(task.getClass());
		}
		return classes;
	}
}
