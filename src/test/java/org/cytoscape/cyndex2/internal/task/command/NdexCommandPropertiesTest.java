package org.cytoscape.cyndex2.internal.task.command;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_EXAMPLE_JSON;
import static org.cytoscape.work.ServiceProperties.COMMAND_LONG_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.COMMAND_SUPPORTS_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class NdexCommandPropertiesTest {

	private static List<Properties> all() {
		return Arrays.asList(NdexCommandProperties.uploadNetwork(),
				NdexCommandProperties.downloadNetwork(),
				NdexCommandProperties.searchNetworks());
	}

	@Test
	public void eachCommandRegistersUnderTheNdexNamespaceWithItsName() {
		assertEquals("ndex", NdexCommandProperties.uploadNetwork().getProperty(COMMAND_NAMESPACE));
		assertEquals("upload network", NdexCommandProperties.uploadNetwork().getProperty(COMMAND));
		assertEquals("download network", NdexCommandProperties.downloadNetwork().getProperty(COMMAND));
		assertEquals("search networks", NdexCommandProperties.searchNetworks().getProperty(COMMAND));
		for (Properties props : all()) {
			assertEquals("ndex", props.getProperty(COMMAND_NAMESPACE));
		}
	}

	@Test
	public void everyCommandSupportsJsonSoMcpToolingGetsAStructuredResult() {
		for (Properties props : all()) {
			assertEquals(props.getProperty(COMMAND), "true", props.getProperty(COMMAND_SUPPORTS_JSON));
		}
	}

	@Test
	public void everyCommandHasShortAndLongDescriptions() {
		for (Properties props : all()) {
			final String name = props.getProperty(COMMAND);
			assertTrue(name, props.getProperty(COMMAND_DESCRIPTION).trim().length() > 0);
			assertTrue(name, props.getProperty(COMMAND_LONG_DESCRIPTION).trim().length() > 0);
		}
	}

	@Test
	public void everyLongDescriptionStatesTheV3Requirement() {
		for (Properties props : all()) {
			assertTrue(props.getProperty(COMMAND),
					props.getProperty(COMMAND_LONG_DESCRIPTION).contains("NDEx v3.0.0"));
		}
	}

	@Test
	public void uploadStatesTheSingleNetworkConstraint() {
		final String longDescription = NdexCommandProperties.uploadNetwork().getProperty(COMMAND_LONG_DESCRIPTION);
		assertTrue(longDescription, longDescription.contains("single network"));
		assertTrue(longDescription, longDescription.contains("not a network collection"));
	}

	@Test
	public void everyExampleJsonIsValidJson() throws Exception {
		final ObjectMapper mapper = new ObjectMapper();
		for (Properties props : all()) {
			mapper.readTree(props.getProperty(COMMAND_EXAMPLE_JSON));
		}
	}
}
