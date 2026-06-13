package org.cytoscape.cyndex2.internal.rest;

@FunctionalInterface
public interface NdexAdminStatusService {
    NdexV3AdminStatus fetch(String serverUrl);
}
