package org.cytoscape.cyndex2.internal.task.command;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

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
 * Guards the JSON boundary that the unit tests otherwise never cross.
 *
 * A JSON caller -- which is how MCP tooling invokes commands -- sends numbers and booleans as JSON
 * literals. CyREST turns a JSON number into a Double, and Cytoscape's command interceptor assigns it
 * to the tunable field with {@code Field.set}. Reflection unboxes a Double into a double field but
 * will NOT narrow it into an int, so an {@code int} tunable fails at runtime with
 * "Couldn't parse value from: 100.0" even though the command's advertised schema says "integer".
 *
 * Tests that assign tunable fields directly cannot see this, so assert it structurally instead.
 */
public class NdexCommandTunableMarshallingTest {

	private static List<Task> allCommandTasks() {
		return NdexCommandFixtures.allTasks();
	}

	/**
	 * getFields(), not getDeclaredFields(): arguments shared by the create and update commands are
	 * inherited from a base class, and getDeclaredFields would let an inherited tunable slip past this
	 * guard entirely. getFields() is also what the platform's own tunable discovery uses.
	 */
	private static List<Field> tunablesOf(Task task) {
		List<Field> fields = new ArrayList<>();
		for (Field field : task.getClass().getFields()) {
			if (field.isAnnotationPresent(Tunable.class)) {
				fields.add(field);
			}
		}
		return fields;
	}

	/** The check itself, so the deliberately-wrong fixture below can be run through the same code. */
	private static boolean cannotAcceptAJsonNumber(Class<?> type) {
		return type == int.class || type == Integer.class
				|| type == long.class || type == Long.class
				|| type == short.class || type == Short.class;
	}

	@Test
	public void noTunableUsesATypeThatCannotAcceptAJsonNumber() {
		for (Task task : allCommandTasks()) {
			for (Field field : tunablesOf(task)) {
				Class<?> type = field.getType();
				assertTrue(task.getClass().getSimpleName() + "." + field.getName() + " is declared "
						+ type.getSimpleName() + "; a JSON caller sends a number, which arrives as a Double "
						+ "and cannot be assigned to that type. Use double.",
						!cannotAcceptAJsonNumber(type));
			}
		}
	}

	/** A task whose tunable is inherited and of the type the guard exists to reject. */
	public static class TaskWithBadNumericTunable extends BaseWithIntTunable {
	}

	public static class BaseWithIntTunable {
		@Tunable(description = "arrives as a Double and cannot be assigned")
		public int maxResults = 0;
	}

	/**
	 * A passing guard is not a working guard. Put the shape it exists to catch in front of the same
	 * predicate and the same reflection, and require both to see it.
	 */
	@Test
	public void theGuardStillFiresOnAnInheritedIntTunable() throws Exception {
		Field inherited = TaskWithBadNumericTunable.class.getField("maxResults");
		assertTrue("an inherited @Tunable must not escape this guard",
				inherited.isAnnotationPresent(Tunable.class));
		assertTrue("int must still be rejected", cannotAcceptAJsonNumber(inherited.getType()));

		try {
			inherited.set(new TaskWithBadNumericTunable(), Double.valueOf(10));
			fail("a Double must not be assignable to an int field -- if this ever succeeds, "
					+ "the reason this guard exists has gone away");
		} catch (IllegalArgumentException expected) {
			// exactly what a JSON caller hits at runtime
		}
	}

	@Test
	public void everyNumericTunableAcceptsADoubleTheWayTheInterceptorAssignsIt() throws Exception {
		int checked = 0;
		for (Task task : allCommandTasks()) {
			for (Field field : tunablesOf(task)) {
				if (!isNumeric(field.getType())) {
					continue;
				}
				field.setAccessible(true);
				try {
					// exactly what CommandTunableInterceptorImpl does with a JSON number
					field.set(task, Double.valueOf(10));
					checked++;
				} catch (IllegalArgumentException e) {
					fail(task.getClass().getSimpleName() + "." + field.getName()
							+ " rejects a Double, so a JSON caller cannot set it: " + e.getMessage());
				}
			}
		}
		assertTrue("expected at least one numeric tunable to exercise", checked > 0);
	}

	private static boolean isNumeric(Class<?> type) {
		return type == int.class || type == Integer.class || type == long.class || type == Long.class
				|| type == double.class || type == Double.class || type == float.class || type == Float.class
				|| type == short.class || type == Short.class;
	}
}
