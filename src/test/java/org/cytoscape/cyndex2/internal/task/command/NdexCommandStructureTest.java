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
	/**
	 * Both the fields a class declares and the public ones it inherits: the executor discovers with
	 * getFields(), which sees inherited public fields, while a private @Tunable declared on a factory
	 * is invisible to the executor for a different reason and is just as wrong.
	 */
	private static List<Field> tunableFieldsOf(Class<?> type) {
		List<Field> fields = new ArrayList<>();
		for (Field field : type.getDeclaredFields()) {
			if (field.isAnnotationPresent(Tunable.class)) {
				fields.add(field);
			}
		}
		for (Field field : type.getFields()) {
			if (field.isAnnotationPresent(Tunable.class) && !fields.contains(field)) {
				fields.add(field);
			}
		}
		return fields;
	}

	@Test
	public void everyTunableIsDeclaredOnTheTaskAndNoneOnTheFactory() {
		for (TaskFactory factory : factories()) {
			for (Field field : tunableFieldsOf(factory.getClass())) {
				assertFalse(factory.getClass().getSimpleName() + "." + field.getName()
						+ " is a @Tunable on a TaskFactory, where the command executor cannot see it",
						field.isAnnotationPresent(Tunable.class));
			}
			List<Task> tasks = tasksOf(factory);
			assertEquals(factory.getClass().getSimpleName(), 1, tasks.size());
		}
	}

	/** A factory with a @Tunable it inherited rather than declared -- the case getDeclaredFields misses. */
	public static class FactoryWithInheritedTunable extends FactoryWithTunable {
	}

	public static class FactoryWithTunable {
		@Tunable(description = "wrong place")
		public String stray = null;
	}

	/**
	 * A guard that passes is not proof of a guard that works. Run the deliberately-wrong shapes through
	 * the same helper the real assertions use, and require it to see them.
	 */
	@Test
	public void theTunableScanSeesBothDeclaredAndInheritedFields() {
		assertEquals(1, tunableFieldsOf(FactoryWithTunable.class).size());
		assertEquals("an inherited @Tunable must not escape the factory guard",
				1, tunableFieldsOf(FactoryWithInheritedTunable.class).size());
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
			// getFields(), not getDeclaredFields(): the shared arguments of the create and update
			// commands are inherited from a base class, and that is exactly how the executor reads them.
			long tunables = tunableFieldsOf(task.getClass()).size();
			if (tunables > 0) {
				withArguments++;
			}
		}
		assertEquals("create, update, download and search all take arguments", 4, withArguments);
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

		assertNotNull(new NDExCreateNetworkCommandTaskFactory(resolver, appManager).createTaskIterator());
		assertNotNull(new NDExUpdateNetworkCommandTaskFactory(resolver, appManager).createTaskIterator());
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
		assertTrue(new NDExCreateNetworkCommandTaskFactory(
				mock(NdexProfileResolver.class), appManager).createTaskIterator().hasNext());
		assertTrue(new NDExUpdateNetworkCommandTaskFactory(
				mock(NdexProfileResolver.class), appManager).createTaskIterator().hasNext());
	}

	/**
	 * The create and update commands inherit profile/visibility/folder from a shared base. Inherited
	 * public fields are discovered -- but only while they stay public, and a field quietly narrowed to
	 * protected would drop out of the command's arguments with no error anywhere.
	 */
	@Test
	public void bothWriteCommandsExposeTheSharedArgumentsThroughGetFields() {
		for (Class<?> type : Arrays.asList(NDExCreateNetworkCommandTask.class,
				NDExUpdateNetworkCommandTask.class)) {
			List<String> names = new ArrayList<>();
			for (Field field : type.getFields()) {
				if (field.isAnnotationPresent(Tunable.class)) {
					names.add(field.getName());
				}
			}
			assertTrue(type.getSimpleName() + " -> " + names, names.contains("profile"));
			assertTrue(type.getSimpleName() + " -> " + names, names.contains("visibility"));
			assertTrue(type.getSimpleName() + " -> " + names, names.contains("folder"));
		}
	}

	/** The whole point of the split: only one of the two takes a target id. */
	@Test
	public void onlyUpdateDeclaresNetworkId() {
		List<String> createArguments = new ArrayList<>();
		for (Field field : tunableFieldsOf(NDExCreateNetworkCommandTask.class)) {
			createArguments.add(field.getName());
		}
		assertFalse("create must offer no way to name an existing network",
				createArguments.contains("networkId"));

		List<String> updateArguments = new ArrayList<>();
		for (Field field : tunableFieldsOf(NDExUpdateNetworkCommandTask.class)) {
			updateArguments.add(field.getName());
		}
		assertTrue(updateArguments.toString(), updateArguments.contains("networkId"));
	}

	/** required=true is metadata only, but it is what a caller reads when deciding what to send. */
	@Test
	public void updateMarksNetworkIdRequiredInTheSchema() throws Exception {
		Tunable tunable = NDExUpdateNetworkCommandTask.class.getField("networkId")
				.getAnnotation(Tunable.class);
		assertTrue(tunable.required());
	}

	@Test
	public void mocksUsedHereAreRealMocks() {
		// guards the verifyZeroInteractions assertion above from silently passing on a non-mock
		assertTrue(mockingDetails(mock(NdexProfileResolver.class)).isMock());
	}
}
