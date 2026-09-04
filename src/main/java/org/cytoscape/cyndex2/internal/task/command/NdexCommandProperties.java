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

	/** Every command talks to the NDEx v3 API; say so in each description. */
	static final String V3_REQUIREMENT = "Requires a minimum of NDEx v3.0.0.";

	private NdexCommandProperties() {
	}

	public static Properties uploadNetwork() {
		return build(UPLOAD_NETWORK,
				"Upload the current network to NDEx.",
				"Uploads the network currently selected in Cytoscape to NDEx, optionally setting its "
						+ "visibility and placing it in a folder. Uploads a single network only - not a "
						+ "network collection. Give networkId to overwrite an existing NDEx network instead "
						+ "of creating a new one. " + V3_REQUIREMENT,
				"{\"uuid\":\"12345678-abcd-1234-abcd-1234567890ab\","
						+ "\"url\":\"https://www.ndexbio.org/viewer/networks/12345678-abcd-1234-abcd-1234567890ab\","
						+ "\"visibility\":\"PRIVATE\",\"folderId\":null}");
	}

	public static Properties downloadNetwork() {
		return build(DOWNLOAD_NETWORK,
				"Download a network from NDEx by UUID.",
				"Downloads the NDEx network with the given UUID and creates it in Cytoscape. " + V3_REQUIREMENT,
				"{\"suid\":52,\"uuid\":\"12345678-abcd-1234-abcd-1234567890ab\",\"name\":\"My network\"}");
	}

	public static Properties searchNetworks() {
		return build(SEARCH_NETWORKS,
				"Search NDEx for networks.",
				"Searches NDEx for networks matching a term, optionally restricted to public or to the "
						+ "signed-in user's own networks. " + V3_REQUIREMENT,
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
