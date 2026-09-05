package org.cytoscape.cyndex2.internal.util;

/**
 * One CyNDEx-2 sign-in profile, as reported by the {@code ndex list profiles} command.
 *
 * Carries identity only — which profiles exist and which one is active. It deliberately says nothing
 * about whether a profile will work: that verdict belongs to the layer that actually uses a profile,
 * which reports its own error. Putting it here would leak internals and invite callers to decide
 * something that is not theirs to decide.
 */
public class NdexProfile {

	private final String name;
	private final String username;
	private final String serverUrl;
	private final boolean current;

	NdexProfile(final String name, final String username, final String serverUrl, final boolean current) {
		this.name = name;
		this.username = username;
		this.serverUrl = serverUrl;
		this.current = current;
	}

	/** The {@code username@serverUrl} name, exactly as the {@code profile} command argument accepts it. */
	public String getName() {
		return name;
	}

	/** Null for an anonymous profile. */
	public String getUsername() {
		return username;
	}

	/** The server URL as stored, unnormalized — that is the string that has to round-trip. */
	public String getServerUrl() {
		return serverUrl;
	}

	/** Whether this is the profile currently selected in CyNDEx-2. */
	public boolean isCurrent() {
		return current;
	}
}
