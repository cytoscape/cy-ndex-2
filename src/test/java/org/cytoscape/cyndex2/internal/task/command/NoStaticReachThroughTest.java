package org.cytoscape.cyndex2.internal.task.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * The command classes take their collaborators through constructors. Reaching back into a singleton
 * would compile and run, but would quietly make the class untestable again -- so assert it in the
 * bytecode rather than trusting review to catch it.
 *
 * Scoped to the command classes: {@code NdexProfileResolver} and the task factories legitimately
 * name the singletons in their production default constructors.
 */
public class NoStaticReachThroughTest {

	private static final List<String> FORBIDDEN = Arrays.asList(
			"org/cytoscape/cyndex2/internal/CyServiceModule",
			"org/cytoscape/cyndex2/internal/util/ServerManager");

	private static List<Class<?>> commandClasses() {
		List<Class<?>> classes = new ArrayList<>(NdexCommandFixtures.allTaskClasses());
		classes.add(AbstractNdexCommandTask.class);
		// the shared run sequence of the create and update commands lives here, not on either task
		classes.add(AbstractNdexNetworkWriteTask.class);
		classes.add(NdexCommandProperties.class);
		return classes;
	}

	private static String bytecodeOf(Class<?> type) throws IOException {
		final String resource = type.getName().replace('.', '/') + ".class";
		try (InputStream in = type.getClassLoader().getResourceAsStream(resource)) {
			assertTrue("could not read bytecode for " + type, in != null);
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final byte[] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			return new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
		}
	}

	@Test
	public void commandClassesDoNotReferenceServiceLocatorSingletons() throws Exception {
		for (Class<?> type : commandClasses()) {
			final String bytecode = bytecodeOf(type);
			for (String forbidden : FORBIDDEN) {
				assertFalse(type.getSimpleName() + " reaches through to " + forbidden
						+ "; take the collaborator through its constructor instead",
						bytecode.contains(forbidden));
			}
		}
	}

	@Test
	public void theScanActuallyFindsReferencesWhenTheyExist() throws Exception {
		// guards the assertion above from passing because the scan is broken: this class does
		// reference the command classes, so their names must be findable in its own bytecode.
		assertTrue(bytecodeOf(NoStaticReachThroughTest.class)
				.contains("org/cytoscape/cyndex2/internal/task/command/AbstractNdexNetworkWriteTask"));
	}
}
