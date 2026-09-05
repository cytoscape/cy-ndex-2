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
 */
public class NdexCommandProperties {

	public static final String NAMESPACE = "ndex";

	public static final String UPLOAD_NETWORK = "upload network";
	public static final String DOWNLOAD_NETWORK = "download network";
	public static final String SEARCH_NETWORKS = "search networks";
	public static final String LIST_PROFILES = "list profiles";

	/** The public NDEx server used when no profile is configured. */
	public static final String PUBLIC_NDEX_SERVER = "https://www.ndexbio.org";

	/** Every command talks to the NDEx v3 API; say so in each description. */
	static final String V3_REQUIREMENT = "Requires a minimum of NDEx v3.0.0.";

	private NdexCommandProperties() {
	}

	/** Every command this app publishes. The guards iterate this, so a new command cannot go unguarded. */
	public static java.util.List<Properties> all() {
		return java.util.Arrays.asList(uploadNetwork(), downloadNetwork(), searchNetworks(), listProfiles());
	}

	/** The commands that contact an NDEx server, and therefore require v3. */
	public static java.util.List<Properties> serverCommands() {
		return java.util.Arrays.asList(uploadNetwork(), downloadNetwork(), searchNetworks());
	}

	public static Properties listProfiles() {
		return build(LIST_PROFILES,
				"List the CyNDEx-2 sign-in profiles configured in Cytoscape.",
				"Lists the NDEx sign-in profiles the user has configured in the CyNDEx-2 app in Cytoscape "
						+ "Desktop. Each profile is a saved authentication for one NDEx server address, and is "
						+ "named username@serverUrl - the exact form the optional 'profile' argument of the "
						+ "other ndex commands accepts. Call this when more than one profile may exist and you "
						+ "need the user to choose, or to find out whether any exist at all. "
						+ "An empty list is worth acting on: it means 'ndex upload network' cannot work until "
						+ "the user defines a profile in CyNDEx-2, while 'ndex download network' and "
						+ "'ndex search networks' will still run anonymously against the public NDEx server at "
						+ PUBLIC_NDEX_SERVER + " and so can only see public networks. "
						+ "This command reads local configuration only - it contacts no server and works even "
						+ "when NDEx is unreachable.",
				"{\"count\":1,\"profiles\":[{\"name\":\"alice@" + PUBLIC_NDEX_SERVER + "\","
						+ "\"username\":\"alice\",\"serverUrl\":\"" + PUBLIC_NDEX_SERVER + "\","
						+ "\"isCurrent\":true}]}");
	}

	public static Properties uploadNetwork() {
		return build(UPLOAD_NETWORK,
				"Upload the current network to NDEx.",
				"Uploads the network currently selected in Cytoscape to NDEx, optionally setting its "
						+ "visibility and placing it in a folder. Uploads a single network only - not a "
						+ "network collection. Give networkId to overwrite an existing NDEx network instead "
						+ "of creating a new one. "
						+ "The optional 'profile' argument names a CyNDEx-2 sign-in profile as "
						+ "username@serverUrl; when omitted the profile currently selected in CyNDEx-2 is used. "
						+ "Uploading requires a signed-in profile, so this command fails if none is selected - "
						+ "run 'ndex list profiles' to see what is available. " + V3_REQUIREMENT,
				"{\"uuid\":\"12345678-abcd-1234-abcd-1234567890ab\","
						+ "\"url\":\"https://www.ndexbio.org/viewer/networks/12345678-abcd-1234-abcd-1234567890ab\","
						+ "\"visibility\":\"PRIVATE\",\"folderId\":null}");
	}

	public static Properties downloadNetwork() {
		return build(DOWNLOAD_NETWORK,
				"Download a network from NDEx by UUID.",
				"Downloads the NDEx network with the given UUID and creates it in Cytoscape. "
						+ "The optional 'profile' argument names a CyNDEx-2 sign-in profile as "
						+ "username@serverUrl; when omitted the profile currently selected in CyNDEx-2 is used. "
						+ "If no profile is selected and none are configured, the download runs anonymously "
						+ "against the public NDEx server at " + PUBLIC_NDEX_SERVER + ", which can only reach "
						+ "public networks. Run 'ndex list profiles' to see what is available. " + V3_REQUIREMENT,
				"{\"suid\":52,\"uuid\":\"12345678-abcd-1234-abcd-1234567890ab\",\"name\":\"My network\"}");
	}

	public static Properties searchNetworks() {
		return build(SEARCH_NETWORKS,
				"Search NDEx for networks.",
				"Searches NDEx for networks matching a term, in either the public corpus (the default) or the "
						+ "signed-in user's own private networks. "
						+ "The optional 'profile' argument names a CyNDEx-2 sign-in profile as "
						+ "username@serverUrl; when omitted the profile currently selected in CyNDEx-2 is used. "
						+ "If no profile is selected and none are configured, the search runs anonymously "
						+ "against the public NDEx server at " + PUBLIC_NDEX_SERVER + ", and searching PRIVATE "
						+ "networks then fails because it needs credentials. Run 'ndex list profiles' to see "
						+ "what is available. " + V3_REQUIREMENT,
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
