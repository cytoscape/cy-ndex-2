package org.cytoscape.cyndex2.internal.task;

import java.util.UUID;

import org.cytoscape.cyndex2.internal.rest.parameter.NDExImportParameters;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.cyndex2.internal.util.ServerManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

/**
 * Cytoscape desktop command task factory for downloading a network from NDEx
 * by UUID and creating a new network view.
 *
 * Registered as a command under namespace "ndex", name "download network".
 * Supports an optional cy-ndex-2 user profile name parameter so that
 * MCP tooling can discover and invoke this command with a specific profile.
 */
public class NDExDownloadNetworkCommandTaskFactory extends AbstractTaskFactory {

    @Tunable(description = "UUID of the NDEx network to download.",
             longDescription = "The UUID of the NDEx network to download and import into Cytoscape.",
             exampleStringValue = "12345678-abcd-1234-abcd-1234567890ab",
             required = true)
    public String networkId = null;

    @Tunable(description = "NDEx server profile name (username@serverUrl). Uses the currently selected profile if not specified.",
             longDescription = "The cy-ndex-2 user profile to use, expressed as username@serverUrl (e.g. myuser@http://public.ndexbio.org/v2). "
                     + "If omitted, the currently selected profile is used.",
             exampleStringValue = "myuser@http://public.ndexbio.org/v2",
             required = false)
    public String profile = null;

    @Tunable(description = "NDEx access key for private networks.",
             longDescription = "An optional access key required to download private networks that have been shared via an access key.",
             exampleStringValue = "",
             required = false)
    public String accessKey = null;

    @Tunable(description = "Create a view for the imported network.",
             longDescription = "Set to true to create a network view after import, false to import without a view. "
                     + "Leave unset to use the default CX reader behaviour.",
             exampleStringValue = "true",
             required = false)
    public Boolean createView = null;

    public NDExDownloadNetworkCommandTaskFactory() {
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new AbstractTask() {
            @Override
            public void run(TaskMonitor taskMonitor) throws Exception {
                if (networkId == null || networkId.trim().isEmpty()) {
                    throw new RuntimeException("networkId parameter is required.");
                }

                UUID uuid;
                try {
                    uuid = UUID.fromString(networkId.trim());
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid networkId: '" + networkId + "'. Must be a valid UUID.");
                }

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

                taskMonitor.setTitle("Downloading network from NDEx...");
                taskMonitor.setStatusMessage("Connecting to NDEx at " + serverUrl + "...");

                NDExImportParameters params = new NDExImportParameters(
                        uuid.toString(), username, password, serverUrl, accessKey, null, createView);

                NDExImportTaskFactory importFactory = new NDExImportTaskFactory(params);
                TaskIterator iter = importFactory.createTaskIterator();
                getTaskIterator().append(iter);
            }
        });
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
}
