package org.cytoscape.cyndex2.internal.task.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.lang.reflect.Field;
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
import org.cytoscape.work.Tunable;
import org.junit.Test;

/**
 * Structural guards for the desktop commands. These assert the two properties that make the
 * commands usable from CyREST and MCP tooling, and that are otherwise only observable by running
 * Cytoscape.
 */
public class NdexCommandStructureTest {

	private List<TaskFactory> factories() {
		return NdexCommandFixtures.allFactories();
	}

	private static List<Task> tasksOf(TaskFactory factory) {
		List<Task> tasks = new ArrayList<>();
		TaskIterator iterator = factory.createTaskIterator();
		while (iterator.hasNext()) {
			tasks.add(iterator.next());
		}
		return tasks;
	}

	/**
	 * Cytoscape's command executor reads arguments only from the tasks of the iterator. A @Tunable on
	 * a factory is silently invisible to /v1/commands and to the Swagger MCP tooling reads, so the
	 * parameters would vanish without any compile or runtime error.
	 */
	@Test
	public void everyTunableIsDeclaredOnTheTaskAndNoneOnTheFactory() {
		for (TaskFactory factory : factories()) {
			for (Field field : factory.getClass().getDeclaredFields()) {
				assertFalse(factory.getClass().getSimpleName() + "." + field.getName()
						+ " is a @Tunable on a TaskFactory, where the command executor cannot see it",
						field.isAnnotationPresent(Tunable.class));
			}
			List<Task> tasks = tasksOf(factory);
			assertEquals(factory.getClass().getSimpleName(), 1, tasks.size());
		}
	}

	/**
	 * Commands that take arguments must declare them on the task. `list profiles` takes none, so this
	 * is scoped rather than applied to every command -- otherwise a correctly argument-free command
	 * would fail a guard.
	 */
	@Test
	public void everyArgumentTakingCommandDeclaresItsTunablesOnTheTask() {
		int withArguments = 0;
		for (Task task : NdexCommandFixtures.allTasks()) {
			long tunables = Arrays.stream(task.getClass().getDeclaredFields())
					.filter(f -> f.isAnnotationPresent(Tunable.class))
					.count();
			if (tunables > 0) {
				withArguments++;
			}
		}
		assertEquals("upload, download and search all take arguments", 3, withArguments);
	}

	/**
	 * AvailableCommands calls createTaskIterator() on every registered command factory whenever the
	 * command list is requested. Doing I/O or throwing here would break command discovery for the
	 * whole application, including other apps' commands.
	 */
	@Test
	public void creatingTheTaskIteratorTouchesNoCollaborators() {
		NdexProfileResolver resolver = mock(NdexProfileResolver.class);
		CyApplicationManager appManager = mock(CyApplicationManager.class);
		CyNetworkManager networkManager = mock(CyNetworkManager.class);
		NdexAdminStatusService adminStatus = mock(NdexAdminStatusService.class);

		assertNotNull(new NDExUploadNetworkCommandTaskFactory(resolver, appManager).createTaskIterator());
		assertNotNull(new NDExDownloadNetworkCommandTaskFactory(resolver, networkManager).createTaskIterator());
		assertNotNull(new NDExSearchNetworksCommandTaskFactory(resolver,
				new NdexServerCapabilities(adminStatus)).createTaskIterator());
		assertNotNull(new NDExListProfilesCommandTaskFactory(resolver).createTaskIterator());

		// no profile lookup, no current-network read, no HTTP probe -- and, for list profiles, no
		// enumeration of the server list either
		verifyZeroInteractions(resolver, appManager, networkManager, adminStatus);
	}

	@Test
	public void creatingTheTaskIteratorSucceedsWithNoCurrentNetwork() {
		CyApplicationManager appManager = mock(CyApplicationManager.class); // getCurrentNetwork() -> null
		TaskIterator iterator = new NDExUploadNetworkCommandTaskFactory(
				mock(NdexProfileResolver.class), appManager).createTaskIterator();
		assertTrue(iterator.hasNext());
	}

	@Test
	public void mocksUsedHereAreRealMocks() {
		// guards the verifyZeroInteractions assertion above from silently passing on a non-mock
		assertTrue(mockingDetails(mock(NdexProfileResolver.class)).isMock());
	}
}
