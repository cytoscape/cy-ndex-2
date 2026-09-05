package org.cytoscape.cyndex2.internal.task.command;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_EXAMPLE_JSON;
import static org.cytoscape.work.ServiceProperties.COMMAND_LONG_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.COMMAND_SUPPORTS_JSON;

import java.util.Properties;

/**
 * Registration metadata for the {@code ndex} desktop commands.
 *
 * Kept apart from {@link org.cytoscape.cyndex2.internal.CyActivator} so the descriptions and the
 * JSON-support flags MCP tooling reads can be asserted without standing up the bundle.
 *
 * A description states what a caller needs, never the name of another command that meets that need:
 * names change, and a caller has its own catalog and the means to search it.
 */
public class NdexCommandProperties {

	public static final String NAMESPACE = "ndex";

	public static final String CREATE_NETWORK = "create network";
	public static final String UPDATE_NETWORK = "update network";
	public static final String DOWNLOAD_NETWORK = "download network";
	public static final String SEARCH_NETWORKS = "search networks";
	public static final String LIST_PROFILES = "list profiles";

	/** The public NDEx server used when no profile is configured. */
	public static final String PUBLIC_NDEX_SERVER = "https://www.ndexbio.org";

	/** Every command talks to the NDEx v3 API; say so in each description. */
	static final String V3_REQUIREMENT = "Requires a minimum of NDEx v3.0.0.";

	/** Shared wording for the optional profile argument, so the four commands cannot drift apart. */
	private static final String PROFILE_NOTE =
			"The optional 'profile' argument names a CyNDEx-2 sign-in profile as username@serverUrl; "
					+ "when omitted the profile currently selected in CyNDEx-2 is used. ";

	private NdexCommandProperties() {
	}

	/** Every command this app publishes. The guards iterate this, so a new command cannot go unguarded. */
	public static java.util.List<Properties> all() {
		return java.util.Arrays.asList(createNetwork(), updateNetwork(), downloadNetwork(), searchNetworks(),
				listProfiles());
	}

	/** The commands that contact an NDEx server, and therefore require v3. */
	public static java.util.List<Properties> serverCommands() {
		return java.util.Arrays.asList(createNetwork(), updateNetwork(), downloadNetwork(), searchNetworks());
	}

	public static Properties listProfiles() {
		return build(LIST_PROFILES,
				"List the CyNDEx-2 sign-in profiles configured in Cytoscape.",
				"Lists the NDEx sign-in profiles the user has configured in the CyNDEx-2 app in Cytoscape "
						+ "Desktop. Each profile is a saved authentication for one NDEx server address, and is "
						+ "named username@serverUrl - the exact form the optional 'profile' argument of the "
						+ "other ndex commands accepts. Call this when more than one profile may exist and you "
						+ "need the user to choose, or to find out whether any exist at all. "
						+ "An empty list is worth acting on: saving a network to NDEx cannot work until the "
						+ "user defines a profile in CyNDEx-2, while reading from NDEx still runs anonymously "
						+ "against the public NDEx server at " + PUBLIC_NDEX_SERVER + " and so can only see "
						+ "public networks. "
						+ "This command reads local configuration only - it contacts no server and works even "
						+ "when NDEx is unreachable.",
				"{\"count\":1,\"profiles\":[{\"name\":\"alice@" + PUBLIC_NDEX_SERVER + "\","
						+ "\"username\":\"alice\",\"serverUrl\":\"" + PUBLIC_NDEX_SERVER + "\","
						+ "\"isCurrent\":true}]}");
	}

	public static Properties createNetwork() {
		return build(CREATE_NETWORK,
				"Save the current network to NDEx as a new network.",
				"Saves the network currently selected in Cytoscape to NDEx as a NEW network, optionally "
						+ "setting its visibility and placing it in a folder. Saves a single network only - not "
						+ "a network collection. This always creates another network on NDEx, even when the "
						+ "Cytoscape network came from NDEx, and Cytoscape then tracks the new one; it never "
						+ "changes an existing NDEx network. If the user meant to save edits back to the "
						+ "network they started from, that is a different operation and this one would leave "
						+ "them with a duplicate, so establish which they want before calling this. "
						+ PROFILE_NOTE
						+ "Saving to NDEx requires a signed-in profile, so this command fails if none is "
						+ "selected. " + V3_REQUIREMENT,
				"{\"uuid\":\"12345678-abcd-1234-abcd-1234567890ab\","
						+ "\"url\":\"https://www.ndexbio.org/viewer/networks/12345678-abcd-1234-abcd-1234567890ab\","
						+ "\"visibility\":\"PRIVATE\",\"folderId\":null}");
	}

	public static Properties updateNetwork() {
		return build(UPDATE_NETWORK,
				"Replace the content of an existing NDEx network with the current network.",
				"Replaces the content of the existing NDEx network given by networkId with the network "
						+ "currently selected in Cytoscape, optionally setting its visibility and placing it in "
						+ "a folder. Saves a single network only - not a network collection. The networkId "
						+ "argument is required and must be the UUID of the network to replace: this "
						+ "overwrites that network, so confirm the target is the one the user means. It never "
						+ "creates a network, so if the user wants a separate copy left alongside the "
						+ "original, that is a different operation. " + PROFILE_NOTE
						+ "Saving to NDEx requires a signed-in profile, so this command fails if none is "
						+ "selected. " + V3_REQUIREMENT,
				"{\"uuid\":\"12345678-abcd-1234-abcd-1234567890ab\","
						+ "\"url\":\"https://www.ndexbio.org/viewer/networks/12345678-abcd-1234-abcd-1234567890ab\","
						+ "\"visibility\":\"PRIVATE\",\"folderId\":null}");
	}

	public static Properties downloadNetwork() {
		return build(DOWNLOAD_NETWORK,
				"Download a network from NDEx by UUID.",
				"Downloads the NDEx network with the given UUID and creates it in Cytoscape. " + PROFILE_NOTE
						+ "If no profile is selected and none are configured, the download runs anonymously "
						+ "against the public NDEx server at " + PUBLIC_NDEX_SERVER + ", which can only reach "
						+ "public networks. Check which sign-in profiles are configured in CyNDEx-2 if a "
						+ "private network is expected. " + V3_REQUIREMENT,
				"{\"suid\":52,\"uuid\":\"12345678-abcd-1234-abcd-1234567890ab\",\"name\":\"My network\"}");
	}

	public static Properties searchNetworks() {
		return build(SEARCH_NETWORKS,
				"Search NDEx for networks.",
				"Searches NDEx for networks matching a term, in either the public corpus (the default) or the "
						+ "signed-in user's own private networks. " + PROFILE_NOTE
						+ "If no profile is selected and none are configured, the search runs anonymously "
						+ "against the public NDEx server at " + PUBLIC_NDEX_SERVER + ", and searching PRIVATE "
						+ "networks then fails because it needs credentials. Check which sign-in profiles are "
						+ "configured in CyNDEx-2 if a private search is expected. " + V3_REQUIREMENT,
				"{\"numFound\":1,\"start\":0,\"networks\":[{\"uuid\":\"12345678-abcd-1234-abcd-1234567890ab\","
						+ "\"name\":\"My network\",\"owner\":\"alice\",\"visibility\":\"PRIVATE\","
						+ "\"edges\":42,\"modificationTime\":\"2026-01-01 00:00:00.0\"}]}");
	}

	private static Properties build(final String command, final String description,
			final String longDescription, final String exampleJson) {
		final Properties props = new Properties();
		props.setProperty(COMMAND_NAMESPACE, NAMESPACE);
		props.setProperty(COMMAND, command);
		props.setProperty(COMMAND_DESCRIPTION, description);
		props.setProperty(COMMAND_LONG_DESCRIPTION, longDescription);
		props.setProperty(COMMAND_SUPPORTS_JSON, "true");
		props.setProperty(COMMAND_EXAMPLE_JSON, exampleJson);
		return props;
	}
}
