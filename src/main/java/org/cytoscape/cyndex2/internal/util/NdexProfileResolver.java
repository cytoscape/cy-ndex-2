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
				throw new IllegalArgumentException(noConfiguredProfileMessage());
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

	/**
	 * Resolves a profile the way {@link #resolve(String)} does, but falls back to anonymous public NDEx
	 * when nothing was named and nothing is selected.
	 *
	 * Only for read-only commands: downloading and searching public networks needs no credentials. A
	 * profile that WAS named and does not exist is still an error -- the fallback must never turn a
	 * mistyped profile name into a silent anonymous search against the wrong data.
	 */
	public Server resolveOrAnonymous(final String profile) {
		if (profile != null && !profile.trim().isEmpty()) {
			return resolve(profile);
		}
		final Server selected = selectedServer.get();
		if (selected != null) {
			return selected;
		}
		// A copy, never the singleton: Server is mutable and DEFAULT_SERVER is shared globally.
		return new Server(Server.DEFAULT_SERVER);
	}

	/** Every profile CyNDEx-2 has, in the order the app holds them, with the selected one flagged. */
	public List<NdexProfile> list() {
		final Server selected = selectedServer.get();
		return availableServers.get().stream()
				.map(server -> new NdexProfile(profileNameOf(server), server.getUsername(),
						server.getUrl(), selected != null && selected.equals(server)))
				.collect(Collectors.toList());
	}

	private String noConfiguredProfileMessage() {
		if (availableServers.get().getSize() == 0) {
			return "No NDEx profiles are defined in CyNDEx-2. Sign in through the CyNDEx-2 toolbar in Cytoscape "
					+ "to add one, then retry. Run 'ndex list profiles' to check.";
		}
		return "No NDEx profile is selected in CyNDEx-2. Select one in the CyNDEx-2 toolbar, or name one with "
				+ "the 'profile' parameter as username@serverUrl. Run 'ndex list profiles' to see them.";
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
