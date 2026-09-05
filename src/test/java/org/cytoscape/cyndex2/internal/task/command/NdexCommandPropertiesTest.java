package org.cytoscape.cyndex2.internal.task.command;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_EXAMPLE_JSON;
import static org.cytoscape.work.ServiceProperties.COMMAND_LONG_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.COMMAND_SUPPORTS_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class NdexCommandPropertiesTest {

	private static List<Properties> all() {
		return NdexCommandProperties.all();
	}

	private static String longDescriptionOf(Properties props) {
		return props.getProperty(COMMAND_LONG_DESCRIPTION);
	}

	@Test
	public void eachCommandRegistersUnderTheNdexNamespaceWithItsName() {
		assertEquals("ndex", NdexCommandProperties.createNetwork().getProperty(COMMAND_NAMESPACE));
		assertEquals("create network", NdexCommandProperties.createNetwork().getProperty(COMMAND));
		assertEquals("update network", NdexCommandProperties.updateNetwork().getProperty(COMMAND));
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

	/**
	 * Scoped to the commands that contact a server. `list profiles` reads local configuration and must
	 * work when NDEx is unreachable, so claiming a server requirement there would be wrong.
	 */
	@Test
	public void everyServerCommandStatesTheV3Requirement() {
		for (Properties props : NdexCommandProperties.serverCommands()) {
			assertTrue(props.getProperty(COMMAND), longDescriptionOf(props).contains("NDEx v3.0.0"));
		}
	}

	@Test
	public void listProfilesDoesNotClaimToNeedAServer() {
		String description = longDescriptionOf(NdexCommandProperties.listProfiles());
		assertFalse(description, description.contains("NDEx v3.0.0"));
		assertTrue(description, description.contains("contacts no server"));
	}

	// ---------- the descriptions an agent reads to decide what to do ----------

	@Test
	public void bothWriteCommandsStateThatTheyNeedASignedInProfile() {
		for (Properties props : Arrays.asList(NdexCommandProperties.createNetwork(),
				NdexCommandProperties.updateNetwork())) {
			assertTrue(props.getProperty(COMMAND), longDescriptionOf(props).contains("signed-in profile"));
		}
	}

	/**
	 * The split exists so a caller handed an ambiguous "save this to NDEx" cannot silently pick one.
	 * Each description has to say which of the two it is, in terms a reader can act on.
	 */
	@Test
	public void createSaysItMakesANewNetworkAndUpdateSaysItReplacesAnExistingOne() {
		String create = longDescriptionOf(NdexCommandProperties.createNetwork());
		assertTrue(create, create.contains("NEW network"));
		assertTrue(create, create.contains("never changes an existing NDEx network"));

		String update = longDescriptionOf(NdexCommandProperties.updateNetwork());
		assertTrue(update, update.contains("Replaces the content"));
		assertTrue(update, update.contains("never creates a network"));
	}

	/** Neither may present itself as the safe default: an ambiguous request has to go back to the user. */
	@Test
	public void neitherWriteCommandOffersItselfAsTheDefaultForAnAmbiguousRequest() {
		for (Properties props : Arrays.asList(NdexCommandProperties.createNetwork(),
				NdexCommandProperties.updateNetwork())) {
			String description = longDescriptionOf(props);
			assertTrue(props.getProperty(COMMAND), description.contains("different operation"));
		}
	}

	@Test
	public void updateStatesThatNetworkIdIsRequired() {
		String update = longDescriptionOf(NdexCommandProperties.updateNetwork());
		assertTrue(update, update.contains("networkId argument is required"));
	}

	@Test
	public void readOnlyCommandsDocumentTheAnonymousFallbackAndNameTheHost() {
		for (Properties props : Arrays.asList(NdexCommandProperties.downloadNetwork(),
				NdexCommandProperties.searchNetworks())) {
			String description = longDescriptionOf(props);
			assertTrue(props.getProperty(COMMAND), description.contains("anonymously"));
			// naming the host here means the docs and the fallback fail together if it ever changes
			assertTrue(props.getProperty(COMMAND), description.contains(NdexCommandProperties.PUBLIC_NDEX_SERVER));
		}
	}

	@Test
	public void listProfilesExplainsWhatAnEmptyListMeans() {
		String description = longDescriptionOf(NdexCommandProperties.listProfiles());
		assertTrue(description, description.contains("empty list"));
		assertTrue(description, description.contains("saving a network to NDEx cannot work"));
		assertTrue(description, description.contains("anonymously"));
	}

	/**
	 * Naming a sibling command in a description couples it to a name that can change, and tells a
	 * caller which tool to reach for when it has its own catalog and can search it. Descriptions state
	 * the need instead. Asserted over every command so a cross-reference cannot creep back in.
	 */
	@Test
	public void noDescriptionNamesAnotherNdexCommand() {
		List<String> commandNames = Arrays.asList(
				NdexCommandProperties.CREATE_NETWORK, NdexCommandProperties.UPDATE_NETWORK,
				NdexCommandProperties.DOWNLOAD_NETWORK, NdexCommandProperties.SEARCH_NETWORKS,
				NdexCommandProperties.LIST_PROFILES);
		for (Properties props : all()) {
			String self = props.getProperty(COMMAND);
			for (String other : commandNames) {
				if (other.equals(self)) {
					continue;
				}
				String qualified = NdexCommandProperties.NAMESPACE + " " + other;
				assertFalse(self + " names the '" + qualified + "' command",
						longDescriptionOf(props).contains(qualified));
				assertFalse(self + " names the '" + qualified + "' command",
						props.getProperty(COMMAND_DESCRIPTION).contains(qualified));
			}
		}
	}

	// ---------- anti-drift ----------

	@Test
	public void everyRegisteredCommandHasAFactoryAndViceVersa() {
		java.util.Set<String> declared = new java.util.HashSet<>();
		for (Properties props : all()) {
			declared.add(props.getProperty(COMMAND));
		}
		assertEquals("NdexCommandProperties.all() and NdexCommandFixtures must list the same commands",
				NdexCommandFixtures.allFactories().size(), declared.size());
		assertEquals(new java.util.HashSet<>(Arrays.asList(
				NdexCommandProperties.CREATE_NETWORK, NdexCommandProperties.UPDATE_NETWORK,
				NdexCommandProperties.DOWNLOAD_NETWORK, NdexCommandProperties.SEARCH_NETWORKS,
				NdexCommandProperties.LIST_PROFILES)), declared);
	}

	@Test
	public void bothWriteCommandsStateTheSingleNetworkConstraint() {
		for (Properties props : Arrays.asList(NdexCommandProperties.createNetwork(),
				NdexCommandProperties.updateNetwork())) {
			final String longDescription = props.getProperty(COMMAND_LONG_DESCRIPTION);
			assertTrue(longDescription, longDescription.contains("single network"));
			assertTrue(longDescription, longDescription.contains("not a network collection"));
		}
	}

	@Test
	public void everyExampleJsonIsValidJson() throws Exception {
		final ObjectMapper mapper = new ObjectMapper();
		for (Properties props : all()) {
			mapper.readTree(props.getProperty(COMMAND_EXAMPLE_JSON));
		}
	}
}
