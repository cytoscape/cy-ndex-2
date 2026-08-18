package org.cytoscape.cyndex2.internal.task;

import java.util.HashMap;
import java.util.UUID;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.cyndex2.internal.util.NDExNetworkManager;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.cyndex2.internal.util.ServerManager;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExSaveParameters;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

/**
 * Cytoscape desktop command task factory for uploading the current network to NDEx.
 *
 * Registered as a command under namespace "ndex", name "upload network".
 * Supports an optional cy-ndex-2 user profile name parameter so that
 * MCP tooling can discover and invoke this command with a specific profile.
 */
public class NDExUploadNetworkCommandTaskFactory extends AbstractTaskFactory {

    private final CyApplicationManager appManager;

    @Tunable(description = "NDEx server profile name (username@serverUrl). Uses the currently selected profile if not specified.",
             longDescription = "The cy-ndex-2 user profile to use, expressed as username@serverUrl (e.g. myuser@http://public.ndexbio.org/v2). "
                     + "If omitted, the currently selected profile is used.",
             exampleStringValue = "myuser@http://public.ndexbio.org/v2",
             required = false)
    public String profile = null;

    @Tunable(description = "Make the uploaded network publicly visible on NDEx.",
             longDescription = "Set to true to make the network public, false to keep it private. Defaults to false.",
             exampleStringValue = "false",
             required = false)
    public boolean isPublic = false;

    @Tunable(description = "UUID of an existing NDEx network to overwrite. If omitted, a new network is created.",
             longDescription = "Provide the UUID of an existing NDEx network to overwrite it. "
                     + "If omitted, a new network is created on NDEx.",
             exampleStringValue = "12345678-abcd-1234-abcd-1234567890ab",
             required = false)
    public String networkId = null;

    @Tunable(description = "Upload the entire network collection instead of just the current sub-network.",
             longDescription = "Set to true to upload the entire root network collection. Defaults to false.",
             exampleStringValue = "false",
             required = false)
    public boolean writeCollection = false;

    public NDExUploadNetworkCommandTaskFactory(CyApplicationManager appManager) {
        this.appManager = appManager;
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new UploadTask());
    }

    private class UploadTask extends AbstractTask implements ObservableTask {

        private String uploadedUuid = null;

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {
            CyNetwork network = appManager.getCurrentNetwork();
            if (network == null) {
                throw new RuntimeException("No current network selected. Please select a network to upload.");
            }

            Server server = resolveServer(profile);
            if (server == null) {
                throw new RuntimeException(
                        "No NDEx profile found. Please sign in to NDEx via CyNDEx-2 or specify a profile parameter.");
            }

            String serverUrl = server.getUrl();
            String username = server.getUsername();
            String password = server.getPassword();

            if (username == null || password == null) {
                throw new RuntimeException(
                        "Selected NDEx profile has no credentials. Please sign in via CyNDEx-2.");
            }

            taskMonitor.setTitle("Uploading network to NDEx...");

            NDExSaveParameters params = new NDExSaveParameters(username, password, serverUrl, new HashMap<>(), writeCollection);
            params.isPublic = isPublic;

            boolean isUpdate = networkId != null && !networkId.trim().isEmpty();

            // When writeCollection=true, pass the root network so NDExExportTaskFactory detects it
            CyNetwork networkToExport = network;
            if (writeCollection && network instanceof CySubNetwork) {
                networkToExport = ((CySubNetwork) network).getRootNetwork();
            }
            if (isUpdate) {
                UUID uuid = UUID.fromString(networkId.trim());
                // Pre-save the UUID to the reference network so NetworkExportTask can find it during update.
                // NDExExportTaskFactory uses the rootNetwork as reference when writeCollection=true,
                // and the sub-network itself when writeCollection=false.
                NDExNetworkManager.saveUUID(networkToExport, uuid, null);
            }

            NDExExportTaskFactory exportFactory = new NDExExportTaskFactory(params, isUpdate);
            TaskIterator iter = exportFactory.createTaskIterator(networkToExport);
            getTaskIterator().append(iter);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R> R getResults(Class<? extends R> type) {
            if (uploadedUuid == null) {
                return null;
            }
            if (type.equals(String.class)) {
                return (R) uploadedUuid;
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
        return ServerManager.INSTANCE.getServer();
    }

    @Override
    public boolean isReady() {
        return appManager.getCurrentNetwork() != null;
    }
}

