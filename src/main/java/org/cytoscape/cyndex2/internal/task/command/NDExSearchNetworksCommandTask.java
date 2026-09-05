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

	/**
	 * Search selects a corpus, and NDEx offers exactly two: PUBLIC and PRIVATE. UNLISTED is not one --
	 * the server answers it with a 400 by design, because unlisted networks are excluded from search.
	 */
	static final String VISIBILITY_PUBLIC = "PUBLIC";
	static final String VISIBILITY_PRIVATE = "PRIVATE";

	@Tunable(description = "Text to search for",
			longDescription = "Text matched against network name, description and owner. "
					+ "Leave empty to list without a text filter.",
			exampleStringValue = "cancer signaling",
			required = false)
	public String searchTerm = "";

	@Tunable(description = "NDEx profile to search as, given as username@serverUrl",
			longDescription = "The CyNDEx-2 sign-in profile to use, written as username@serverUrl. "
					+ "When omitted, the profile currently selected in CyNDEx-2 is used; if none is selected and "
					+ "none are configured, this runs anonymously against the public NDEx server at "
					+ "https://www.ndexbio.org, which can only reach public networks. "
					+ "Run 'ndex list profiles' to see the configured profiles.",
			exampleStringValue = "alice@https://www.ndexbio.org/v2",
			required = false)
	public String profile = null;

	@Tunable(description = "Which networks to search",
			longDescription = "PUBLIC (the default) searches public networks; PRIVATE searches the signed-in "
					+ "user's own networks and requires a signed-in profile. There is no option to search both "
					+ "at once -- NDEx treats this as a choice of corpus, not a filter. UNLISTED is not a "
					+ "search mode either: unlisted networks are reachable by link but are deliberately "
					+ "excluded from search results.",
			exampleStringValue = "PUBLIC",
			required = false)
	public ListSingleSelection<String> visibility =
			new ListSingleSelection<>(VISIBILITY_PUBLIC, VISIBILITY_PRIVATE);

	// Declared as double, not int, and that is deliberate. A JSON caller -- which is how MCP tooling
	// invokes commands -- sends a number, CyREST hands Cytoscape a Double, and the command interceptor
	// assigns it with Field.set. Reflection will unbox a Double into a double field but will not narrow
	// it into an int, so an int tunable fails with "Couldn't parse value from: 100.0". A double accepts
	// both the JSON-number and the string form. Whole-number validation happens in run().
	@Tunable(description = "Maximum number of results to return",
			longDescription = "Maximum number of networks to return. Must be a whole number of at least 1.",
			exampleStringValue = "100",
			required = false)
	public double maxResults = 100;

	@Tunable(description = "Index of the first result to return",
			longDescription = "Zero-based index of the first result, for paging through a large result set. "
					+ "Must be a whole number of 0 or more.",
			exampleStringValue = "0",
			required = false)
	public double startIndex = 0;

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
		this.visibility.setSelectedValue(VISIBILITY_PUBLIC);
	}

	@Override
	public void run(final TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Searching NDEx");

		// Validate the arguments before anything reaches the network, so a typo costs nothing.
		final FileVisibilityType visibilityFilter = parseVisibility();
		final int start = wholeNumber("startIndex", startIndex, 0);
		final int size = wholeNumber("maxResults", maxResults, 1);

		final Server server = profileResolver.resolveOrAnonymous(profile);
		// Keyed off the resolved profile, not off whether the fallback fired: a user can name the
		// anonymous profile that "Log out of Current Profile" leaves behind, and that must fail too.
		if (visibilityFilter == FileVisibilityType.PRIVATE && server.getUsername() == null) {
			throw new IllegalArgumentException("Searching PRIVATE networks requires a signed-in NDEx profile, "
					+ "but the profile in use is anonymous. Sign in through CyNDEx-2, or name a signed-in "
					+ "profile with the 'profile' parameter. Run 'ndex list profiles' to see them.");
		}
		serverCapabilities.requireV3(server.getUrl());

		final SimpleFileQuery query = new SimpleFileQuery();
		query.setSearchString(searchTerm == null ? "" : searchTerm.trim());
		query.setType(FileType.NETWORK);

		final NdexRestClientModelAccessLayer mal = modelAccessLayers.create(server);
		taskMonitor.setStatusMessage("Searching " + server.getUrl());
		final FileSearchResult found = mal.searchFiles(query, visibilityFilter, start, size);

		final ObjectMapper mapper = new ObjectMapper();
		final ObjectNode result = mapper.createObjectNode();
		result.put("numFound", found == null ? 0L : found.getNumFound());
		result.put("start", found == null ? start : found.getStart());
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

	/**
	 * The corpus to search. Never null: NDEx defaults an unset visibility to PUBLIC server-side, so
	 * leaving it off would silently mean "public only" while appearing to mean "everything".
	 */
	private FileVisibilityType parseVisibility() {
		final String selected = visibility.getSelectedValue();
		final String value = (selected == null || selected.trim().isEmpty())
				? VISIBILITY_PUBLIC : selected.trim().toUpperCase();
		try {
			return FileVisibilityType.valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid visibility '" + selected
					+ "'. Expected PUBLIC or PRIVATE. UNLISTED is not a search mode: unlisted networks "
					+ "are excluded from NDEx search results by design.");
		}
	}

	/**
	 * Converts a numeric tunable to the whole number the NDEx API expects, rejecting anything that is
	 * not one rather than silently truncating it.
	 */
	static int wholeNumber(final String name, final double value, final int minimum) {
		if (Double.isNaN(value) || Double.isInfinite(value) || value != Math.floor(value)) {
			throw new IllegalArgumentException("Invalid " + name + " '" + value
					+ "'. Expected a whole number of " + minimum + " or more.");
		}
		if (value < minimum) {
			throw new IllegalArgumentException("Invalid " + name + " '" + (long) value
					+ "'. Expected a whole number of " + minimum + " or more.");
		}
		return (int) value;
	}

	private static void putText(final ObjectNode node, final String field, final String value) {
		if (value == null) {
			node.putNull(field);
		} else {
			node.put(field, value);
		}
	}
}
