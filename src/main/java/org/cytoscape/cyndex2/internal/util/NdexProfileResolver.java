package org.cytoscape.cyndex2.internal.util;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Resolves a CyNDEx-2 sign-in profile, named as {@code username@serverUrl}, to its {@link Server}.
 *
 * That spelling is what {@link Server#toString()} already produces, so a profile name can be copied
 * straight out of the sign-in UI.
 *
 * Collaborators are constructor-injected so tests can supply their own server list instead of
 * touching the {@code ServerManager.INSTANCE} singleton, whose static initializer performs file I/O.
 */
public class NdexProfileResolver {

	private final Supplier<ServerList> availableServers;
	private final Supplier<Server> selectedServer;

	public NdexProfileResolver() {
		this(() -> ServerManager.INSTANCE.getAvailableServers(),
			 () -> ServerManager.INSTANCE.getSelectedServer());
	}

	/** For tests: supply the server list and selected profile directly. */
	NdexProfileResolver(final Supplier<ServerList> availableServers, final Supplier<Server> selectedServer) {
		this.availableServers = availableServers;
		this.selectedServer = selectedServer;
	}

	/**
	 * @param profile a {@code username@serverUrl} profile name, or null/blank for the currently selected profile
	 * @return the matching server, never null
	 * @throws IllegalArgumentException if no profile matches, or nothing is signed in
	 */
	public Server resolve(final String profile) {
		if (profile == null || profile.trim().isEmpty()) {
			final Server selected = selectedServer.get();
			if (selected == null) {
				throw new IllegalArgumentException(
						"No NDEx profile is selected. Sign in through CyNDEx-2, or name a profile with the "
						+ "'profile' parameter as username@serverUrl.");
			}
			return selected;
		}

		final String trimmed = profile.trim();
		// Match against each known profile's own name rather than splitting the input. A server URL's
		// authority may legitimately contain '@' (userinfo), so there is no split position that is right
		// in every case -- but comparing whole keys never has to guess.
		final List<Server> matches = availableServers.get().stream()
				.filter(server -> profileNameOf(server).equalsIgnoreCase(trimmed))
				.collect(Collectors.toList());

		if (matches.isEmpty()) {
			throw new IllegalArgumentException(
					"No NDEx profile '" + profile + "'. Available profiles: " + describeAvailable() + ".");
		}
		return matches.get(0);
	}

	/** The {@code username@serverUrl} name of a profile; anonymous profiles have an empty username. */
	private static String profileNameOf(final Server server) {
		return (server.getUsername() == null ? "" : server.getUsername()) + "@" + server.getUrl();
	}

	private String describeAvailable() {
		final String names = availableServers.get().stream()
				.map(NdexProfileResolver::profileNameOf)
				.collect(Collectors.joining(", "));
		return names.isEmpty() ? "(none)" : names;
	}

	private static String emptyToNull(final String value) {
		if (value == null) {
			return null;
		}
		final String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
