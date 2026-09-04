package org.cytoscape.cyndex2.internal.task.command;

import org.cytoscape.cyndex2.internal.rest.parameter.NDExImportParameters;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExSaveParameters;
import org.cytoscape.cyndex2.internal.task.NDExExportTaskFactory;
import org.cytoscape.cyndex2.internal.task.NDExImportTaskFactory;
import org.cytoscape.cyndex2.internal.util.Server;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

/**
 * Seams for the collaborators the command tasks would otherwise construct inline.
 *
 * Each has a production default that does exactly what the inline construction did; tests supply
 * their own so a command can be exercised without reaching an NDEx server.
 */
public final class NdexTaskFactories {

	private NdexTaskFactories() {
	}

	@FunctionalInterface
	public interface ExportTaskFactorySupplier {
		NDExExportTaskFactory create(NDExSaveParameters params, boolean isUpdate);
	}

	@FunctionalInterface
	public interface ImportTaskFactorySupplier {
		NDExImportTaskFactory create(NDExImportParameters params);
	}

	@FunctionalInterface
	public interface ModelAccessLayerSupplier {
		NdexRestClientModelAccessLayer create(Server server) throws Exception;
	}

	public static final ExportTaskFactorySupplier DEFAULT_EXPORT = NDExExportTaskFactory::new;

	public static final ImportTaskFactorySupplier DEFAULT_IMPORT = NDExImportTaskFactory::new;

	public static final ModelAccessLayerSupplier DEFAULT_MAL = Server::getModelAccessLayer;
}
