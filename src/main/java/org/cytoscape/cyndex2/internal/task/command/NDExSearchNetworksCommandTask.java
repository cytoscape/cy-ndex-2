package org.cytoscape.cyndex2.internal.task.command;

import java.util.List;

import org.cytoscape.cyndex2.internal.util.NdexProfileResolver;
import org.cytoscape.cyndex2.internal.util.NdexServerCapabilities;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;
import org.ndexbio.model.object.FileItemSummary;
import org.ndexbio.model.object.FileSearchResult;
import org.ndexbio.model.object.FileType;
import org.ndexbio.model.object.FileVisibilityType;
import org.ndexbio.model.object.SimpleFileQuery;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * {@code ndex search networks} — searches NDEx for networks.
 *
 * Unlike upload and download this does not go through the shared transfer path, so it builds its
 * own client from the resolved profile and checks the server itself.
 */
public class NDExSearchNetworksCommandTask extends AbstractNdexCommandTask {

	/** Search selects a corpus, and UNLISTED is not one: NDEx answers it with a 400 by design. */
	static final String VISIBILITY_ALL = "ALL";

	@Tunable(description = "Text to search for",
			longDescription = "Text matched against network name, description and owner. "
					+ "Leave empty to list without a text filter.",
			exampleStringValue = "cancer signaling",
			required = false)
	public String searchTerm = "";

	@Tunable(description = "NDEx profile to search as, given as username@serverUrl",
			longDescription = "The CyNDEx-2 sign-in profile to use, written as username@serverUrl. "
					+ "When omitted, the currently selected profile is used.",
			exampleStringValue = "alice@https://www.ndexbio.org/v2",
			required = false)
	public String profile = null;

	@Tunable(description = "Which networks to search",
			longDescription = "ALL searches without a visibility filter, PUBLIC searches public networks, and "
					+ "PRIVATE searches the signed-in user's own networks. UNLISTED is not a search mode: "
					+ "unlisted networks are deliberately excluded from search results.",
			exampleStringValue = "ALL",
			required = false)
	public ListSingleSelection<String> visibility =
			new ListSingleSelection<>(VISIBILITY_ALL, "PUBLIC", "PRIVATE");

	@Tunable(description = "Maximum number of results to return",
			longDescription = "Maximum number of networks to return.",
			exampleStringValue = "100",
			required = false)
	public int maxResults = 100;

	@Tunable(description = "Index of the first result to return",
			longDescription = "Zero-based index of the first result, for paging through a large result set.",
			exampleStringValue = "0",
			required = false)
	public int startIndex = 0;

	private final NdexProfileResolver profileResolver;
	private final NdexServerCapabilities serverCapabilities;
	private final NdexTaskFactories.ModelAccessLayerSupplier modelAccessLayers;

	public NDExSearchNetworksCommandTask(final NdexProfileResolver profileResolver,
			final NdexServerCapabilities serverCapabilities) {
		this(profileResolver, serverCapabilities, NdexTaskFactories.DEFAULT_MAL);
	}

	/** For tests: supply the NDEx client instead of opening a real connection. */
	NDExSearchNetworksCommandTask(final NdexProfileResolver profileResolver,
			final NdexServerCapabilities serverCapabilities,
			final NdexTaskFactories.ModelAccessLayerSupplier modelAccessLayers) {
		this.profileResolver = profileResolver;
		this.serverCapabilities = serverCapabilities;
		this.modelAccessLayers = modelAccessLayers;
		this.visibility.setSelectedValue(VISIBILITY_ALL);
	}

	@Override
	public void run(final TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Searching NDEx");

		final Server server = profileResolver.resolve(profile);
		serverCapabilities.requireV3(server.getUrl());

		final FileVisibilityType visibilityFilter = parseVisibility();

		final SimpleFileQuery query = new SimpleFileQuery();
		query.setSearchString(searchTerm == null ? "" : searchTerm.trim());
		query.setType(FileType.NETWORK);

		final NdexRestClientModelAccessLayer mal = modelAccessLayers.create(server);
		taskMonitor.setStatusMessage("Searching " + server.getUrl());
		final FileSearchResult found = mal.searchFiles(query, visibilityFilter, startIndex, maxResults);

		final ObjectMapper mapper = new ObjectMapper();
		final ObjectNode result = mapper.createObjectNode();
		result.put("numFound", found == null ? 0L : found.getNumFound());
		result.put("start", found == null ? startIndex : found.getStart());
		final ArrayNode networks = result.putArray("networks");
		final List<FileItemSummary> files = found == null ? null : found.getFiles();
		if (files != null) {
			for (FileItemSummary file : files) {
				final ObjectNode entry = networks.addObject();
				putText(entry, "uuid", file.getUuid() == null ? null : file.getUuid().toString());
				putText(entry, "name", file.getName());
				putText(entry, "owner", file.getOwner());
				// FileItemSummary types this as a plain String, unlike every other visibility field
				// in the NDEx model, so pass it through rather than parsing it.
				putText(entry, "visibility", file.getVisibility());
				entry.put("edges", file.getEdges());
				putText(entry, "modificationTime",
						file.getModificationTime() == null ? null : file.getModificationTime().toString());
			}
		}
		taskMonitor.setStatusMessage("Found " + networks.size() + " network(s).");
		setResult(result);
	}

	private FileVisibilityType parseVisibility() {
		final String selected = visibility.getSelectedValue();
		if (selected == null || VISIBILITY_ALL.equalsIgnoreCase(selected)) {
			return null;
		}
		try {
			return FileVisibilityType.valueOf(selected.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid visibility '" + selected
					+ "'. Expected ALL, PUBLIC or PRIVATE. UNLISTED is not a search mode: unlisted networks "
					+ "are excluded from NDEx search results by design.");
		}
	}

	private static void putText(final ObjectNode node, final String field, final String value) {
		if (value == null) {
			node.putNull(field);
		} else {
			node.put(field, value);
		}
	}
}
