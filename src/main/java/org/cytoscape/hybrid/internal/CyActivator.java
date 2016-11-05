package org.cytoscape.hybrid.internal;

import static org.cytoscape.work.ServiceProperties.ENABLE_FOR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.CyShutdownListener;
import org.cytoscape.application.swing.ActionEnableSupport;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.hybrid.events.WSHandler;
import org.cytoscape.hybrid.internal.electron.NativeAppInstaller;
import org.cytoscape.hybrid.internal.electron.NdexAppStateManager;
import org.cytoscape.hybrid.internal.login.LoginManager;
import org.cytoscape.hybrid.internal.login.NdexLoginMessageHandler;
import org.cytoscape.hybrid.internal.task.OpenExternalAppTaskFactory;
import org.cytoscape.hybrid.internal.ui.SearchBox;
import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.SaveMessageHandler;
import org.cytoscape.hybrid.internal.ws.WSClient;
import org.cytoscape.hybrid.internal.ws.WSServer;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CyActivator extends AbstractCyActivator {

	private static final Logger logger = LoggerFactory.getLogger(CyActivator.class);
	private WSServer server;
	private JToolBar toolBar;

	private JPanel panel;
		
	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {
		// Importing Service
		final CySwingApplication desktop = getService(bc, CySwingApplication.class);
		final CyApplicationConfiguration config = getService(bc, CyApplicationConfiguration.class);
		final CyApplicationManager appManager = getService(bc, CyApplicationManager.class);
		final CyEventHelper eventHelper = getService(bc, CyEventHelper.class);
		final CyRootNetworkManager rootManager = getService(bc, CyRootNetworkManager.class);
		@SuppressWarnings("unchecked")
		final CyProperty<Properties> cyProp = getService(bc,CyProperty.class,"(cyPropertyName=cytoscape3.props)");

		// Local components
		final LoginManager loginManager = new LoginManager();
		final NdexAppStateManager appStateManager = new NdexAppStateManager(config, loginManager);
		
		final ExternalAppManager pm = new ExternalAppManager();
		final WSClient client = new WSClient(desktop, pm, eventHelper, loginManager, cyProp);
		final NativeAppInstaller installer = new NativeAppInstaller(config);

		// Start server
		this.server = new WSServer();
		registerAllServices(bc, server, new Properties());
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				server.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		// Menu item for NDEx Save
		final OpenExternalAppTaskFactory ndexSaveTaskFactory = new OpenExternalAppTaskFactory("ndex-save", client, pm, installer.getCommand());
		final OpenExternalAppTaskFactory ndexLoginTaskFactory = new OpenExternalAppTaskFactory("ndex-login", client, pm, installer.getCommand());
		
		final Properties ndexSaveTaskFactoryProps = new Properties();
		ndexSaveTaskFactoryProps.setProperty(ENABLE_FOR, ActionEnableSupport.ENABLE_FOR_NETWORK);
		ndexSaveTaskFactoryProps.setProperty(PREFERRED_MENU, "File.Export");
		ndexSaveTaskFactoryProps.setProperty(MENU_GRAVITY, "0.0");
		ndexSaveTaskFactoryProps.setProperty(TITLE, "Network Collection to NDEx...");
		registerAllServices(bc, ndexSaveTaskFactory, ndexSaveTaskFactoryProps);
		
		final Properties ndexLoginTaskFactoryProps = new Properties();
		registerAllServices(bc, ndexLoginTaskFactory, ndexLoginTaskFactoryProps);
	
		// WebSocket event handlers
		
		final WSHandler saveHandler = new SaveMessageHandler(appManager, loginManager, rootManager, cyProp);
		final WSHandler loginHandler = new NdexLoginMessageHandler(loginManager);
		client.getSocket().addHandler(saveHandler);
		client.getSocket().addHandler(loginHandler);
		
		// Export as service
		panel = new SearchBox(client, pm, installer.getCommand());
		Properties metadata = new Properties();
		metadata.put("id", "searchPanel");
		
		toolBar = desktop.getJToolBar();
		toolBar.add(panel);
		
		// Login manager
		registerService(bc, appStateManager, CyShutdownListener.class, new Properties());
	}

	@Override
	public void shutDown() {
		logger.info("Shutting down NDEx Valet...");
		server.stop();
		toolBar.remove(panel);
		panel = null;
		server = null;
	}
}