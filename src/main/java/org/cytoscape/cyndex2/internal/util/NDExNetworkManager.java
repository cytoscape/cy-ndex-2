package org.cytoscape.cyndex2.internal.util;

import java.sql.Timestamp;
import java.util.UUID;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

public class NDExNetworkManager {
	public static final String UUID_COLUMN = "NDEx UUID";
	public static final String MODIFICATION_COLUMN = "NDEx Modification Timestamp";
	
	public static void saveUUID(CyNetwork network, UUID uuid, Timestamp timestamp) {
		CyTable table = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
		
		if (table.getColumn(UUID_COLUMN) == null) {
			table.createColumn(UUID_COLUMN, String.class, false);
		}
		
		if (table.getColumn(MODIFICATION_COLUMN) == null) {
			table.createColumn(MODIFICATION_COLUMN, String.class, false);
		}
		
		CyRow row = table.getRow(network.getSUID());
		row.set(UUID_COLUMN, uuid.toString());
		row.set(MODIFICATION_COLUMN, timestamp.toString());
	}
	
	public static void updateModificationTimeStamp(CyNetwork network, Timestamp timestamp) {
		CyTable table = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
		CyRow row = table.getRow(network.getSUID());
		row.set(MODIFICATION_COLUMN, timestamp.toString());
	}
	
	public static Timestamp getModificationTimeStamp(CyNetwork network) {
		CyTable table = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
		CyRow row = table.getRow(network.getSUID());
		if (row == null) {
			return null;
		}
		String timestamp = row.get(MODIFICATION_COLUMN, String.class);
		if (timestamp == null || timestamp.isEmpty()) {
			return null;
		}
		return Timestamp.valueOf(timestamp);
	}
	
	public static UUID getUUID(CyNetwork network) {
		CyTable table = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
		CyRow row = table.getRow(network.getSUID());
		if (row == null) {
			return null;
		}
		String uuid = row.get(UUID_COLUMN, String.class);
		if (uuid == null || uuid.isEmpty()) {
			return null;
		}
		return UUID.fromString(uuid);
	}
}
