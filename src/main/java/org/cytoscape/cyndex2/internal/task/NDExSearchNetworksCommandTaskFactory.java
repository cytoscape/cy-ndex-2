package org.cytoscape.cyndex2.internal.task;

import java.io.IOException;
import java.util.List;

import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.cyndex2.internal.util.ServerManager;
import org.cytoscape.cyndex2.internal.util.UserAgentUtil;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Cytoscape desktop command task factory for searching networks on NDEx.
 *
 * Registered as a command under namespace "ndex", name "search networks".
 * Supports an optional cy-ndex-2 user profile name parameter so that
 * MCP tooling can discover and invoke this command with a specific profile.
 */
public class NDExSearchNetworksCommandTaskFactory extends AbstractTaskFactory {

    private static final int DEFAULT_MAX_RESULTS = 100;

    @Tunable(description = "Search term to query NDEx networks.",
             longDescription = "The text query to search for networks on NDEx. "
                     + "Leave empty to list networks without filtering by text.",
             exampleStringValue = "cancer signaling",
             required = false)
    public String searchTerm = "";

    @Tunable(description = "NDEx server profile name (username@serverUrl). Uses the currently selected profile if not specified.",
             longDescription = "The cy-ndex-2 user profile to use, expressed as username@serverUrl (e.g. myuser@http://public.ndexbio.org/v2). "
                     + "If omitted, the currently selected profile is used.",
             exampleStringValue = "myuser@http://public.ndexbio.org/v2",
             required = false)
    public String profile = null;

    @Tunable(description = "Filter to only show the authenticated user's own networks.",
             longDescription = "Set to true to search only within the authenticated user's networks. "
                     + "Set to false (default) to search all public networks.",
             exampleStringValue = "false",
             required = false)
    public boolean myNetworksOnly = false;

    @Tunable(description = "Maximum number of results to return.",
             longDescription = "The maximum number of network summaries to return. Defaults to 100.",
             exampleStringValue = "100",
             required = false)
    public int maxResults = DEFAULT_MAX_RESULTS;

    public NDExSearchNetworksCommandTaskFactory() {
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new SearchTask());
    }

    private class SearchTask extends AbstractTask implements ObservableTask {

        private List<NetworkSummary> results = null;

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {
            Server server = resolveServer(profile);

            String serverUrl;
            String username = null;
            String password = null;

            if (server != null) {
                serverUrl = server.getUrl();
                username = server.getUsername();
                password = server.getPassword();
            } else {
                serverUrl = "http://public.ndexbio.org/v2";
            }

            taskMonitor.setTitle("Searching NDEx networks...");
            taskMonitor.setStatusMessage("Connecting to NDEx at " + serverUrl + "...");

            NdexRestClientModelAccessLayer mal = buildMal(username, password, serverUrl);

            taskMonitor.setStatusMessage("Searching for networks...");
            if (myNetworksOnly && username != null) {
                results = mal.getMyNetworks();
                if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                    final String term = searchTerm.trim().toLowerCase();
                    results = results.stream()
                            .filter(ns -> ns.getName() != null && ns.getName().toLowerCase().contains(term))
                            .collect(java.util.stream.Collectors.toList());
                }
            } else {
                String query = (searchTerm == null) ? "" : searchTerm.trim();
                results = mal.findNetworks(query, null, null, true, 0, maxResults).getNetworks();
            }

            int count = results != null ? results.size() : 0;
            taskMonitor.setStatusMessage("Search complete. Found " + count + " network(s).");
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R> R getResults(Class<? extends R> type) {
            if (results == null) {
                return null;
            }
            if (type.equals(String.class)) {
                ObjectMapper mapper = new ObjectMapper();
                try {
                    return (R) mapper.writeValueAsString(results);
                } catch (JsonProcessingException e) {
                    return (R) results.toString();
                }
            }
            if (type.equals(List.class)) {
                return (R) results;
            }
            return null;
        }
    }

    private static Server resolveServer(String profileParam) {
        if (profileParam != null && !profileParam.trim().isEmpty()) {
            final String trimmed = profileParam.trim();
            return ServerManager.INSTANCE.getAvailableServers().stream()
                    .filter(s -> {
                        String key = (s.getUsername() != null ? s.getUsername() : "") + "@" + s.getUrl();
                        return key.equalsIgnoreCase(trimmed);
                    })
                    .findFirst()
                    .orElse(null);
        }
        return ServerManager.INSTANCE.getSelectedServer();
    }

    private static NdexRestClientModelAccessLayer buildMal(String username, String password, String serverUrl)
            throws IOException {
        NdexRestClient client = new NdexRestClient(username, password, serverUrl, UserAgentUtil.getUserAgent());
        return new NdexRestClientModelAccessLayer(client);
    }
}
