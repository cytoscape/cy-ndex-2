package org.cytoscape.cyndex2.internal.task.command;

import java.util.List;

import org.cytoscape.cyndex2.internal.util.NdexProfile;
import org.cytoscape.cyndex2.internal.util.NdexProfileResolver;
import org.cytoscape.work.TaskMonitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * {@code ndex list profiles} — lists the CyNDEx-2 sign-in profiles configured in Cytoscape.
 *
 * Takes no arguments and touches no server. That matters: an agent needs this to work precisely when
 * NDEx is unreachable or nothing is configured, which is when it has to explain why the other
 * commands are failing.
 */
public class NDExListProfilesCommandTask extends AbstractNdexCommandTask {

	private final NdexProfileResolver profileResolver;

	public NDExListProfilesCommandTask(final NdexProfileResolver profileResolver) {
		this.profileResolver = profileResolver;
	}

	@Override
	public void run(final TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Listing NDEx profiles");

		// Enumerated here rather than in the constructor: Cytoscape builds a task for every command
		// listing, and that must stay free of side effects.
		final List<NdexProfile> profiles = profileResolver.list();

		final ObjectNode result = new ObjectMapper().createObjectNode();
		result.put("count", profiles.size());
		final ArrayNode rows = result.putArray("profiles");
		for (NdexProfile profile : profiles) {
			final ObjectNode row = rows.addObject();
			row.put("name", profile.getName());
			if (profile.getUsername() == null) {
				row.putNull("username");
			} else {
				row.put("username", profile.getUsername());
			}
			row.put("serverUrl", profile.getServerUrl());
			row.put("isCurrent", profile.isCurrent());
		}

		taskMonitor.setStatusMessage(profiles.isEmpty()
				? "No NDEx profiles are configured in CyNDEx-2."
				: "Found " + profiles.size() + " NDEx profile(s).");
		setResult(result);
	}
}
