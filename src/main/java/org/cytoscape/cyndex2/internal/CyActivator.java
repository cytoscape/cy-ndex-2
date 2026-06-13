package org.cytoscape.cyndex2.internal;

import static org.cytoscape.work.ServiceProperties.ENABLE_FOR;
import static org.cytoscape.work.ServiceProperties.ID;
import static org.cytoscape.work.ServiceProperties.INSERT_SEPARATOR_BEFORE;
import static org.cytoscape.work.ServiceProperties.IN_NETWORK_PANEL_CONTEXT_MENU;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.io.File;
import java.util.Dictionary;
import java.util.Properties;

import javax.swing.Icon;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.cyndex2.internal.rest.NdexAdminStatusService;
import org.cytoscape.cyndex2.internal.rest.NdexClient;
import org.cytoscape.cyndex2.internal.rest.NdexV3AdminStatus;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexBaseResource;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexNetworkResource;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexStatusResource;
import org.cytoscape.cyndex2.internal.rest.endpoints.impl.NdexBaseResourceImpl;
import org.cytoscape.cyndex2.internal.rest.endpoints.impl.NdexNetworkResourceImpl;
import org.cytoscape.cyndex2.internal.rest.endpoints.impl.NdexStatusResourceImpl;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorBuilder;
import org.cytoscape.cyndex2.internal.task.OpenBrowseTaskFactory;
import org.cytoscape.cyndex2.internal.task.OpenSaveCollectionTaskFactory;
import org.cytoscape.cyndex2.internal.task.OpenSaveTaskFactory;
import org.cytoscape.cyndex2.internal.ui.ImportUserNetworkFromNDExTaskFactory;
import org.cytoscape.cyndex2.internal.ui.ImportNetworkFromNDExTaskFactory;
import org.cytoscape.cyndex2.internal.ui.MainToolBarAction;
import org.cytoscape.cyndex2.internal.ui.SaveNetworkToNDExTaskFactory;
import org.cytoscape.cyndex2.internal.util.CIServiceManager;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.cyndex2.internal.util.IconUtil;
import org.cytoscape.cyndex2.internal.util.StringResources;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.write.CyNetworkViewWriterFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkCollectionTaskFactory;
import org.cytoscape.task.RootNetworkCollectionTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CyActivator extends AbstractCyActivator {

	// Logger for this activator
	private static final Logger logger = LoggerFactory.getLogger(CyActivator.class);
	
	private static CyProperty<Properties> cyProps;

	private static String appVersion;
	private static String cytoscapeVersion;
	private static String appName;
	private static boolean hasCyNDEx1;

	private CIServiceManager ciServiceManager;
	public static TaskManager<?, ?> taskManager;

	public CyActivator() {
		super();
		
		hasCyNDEx1 = false;
	}
	
	public static String getProperty(String prop) {
		return cyProps.getProperties().getProperty(prop);
	}

	public static String getCyRESTPort() {
		String port = cyProps.getProperties().getProperty("rest.port");
		if (port == null) {
			return "1234";
		}
		return port;
	}
	
	public static String getCytoscapeVersion() {
		String version = cyProps.getProperties().getProperty("cytoscape.version.number");
		return version;
	}
	
	public static boolean useDefaultBrowser() {
		String val = cyProps.getProperties().getProperty("cyndex2.defaultBrowser");
		return Boolean.parseBoolean(val);
	}
	

	@Override
	@SuppressWarnings("unchecked")
	public void start(BundleContext bc) throws InvalidSyntaxException {
		
		for (Bundle b : bc.getBundles()) {
			// System.out.println(b.getSymbolicName());
			if (b.getSymbolicName().equals("org.cytoscape.api-bundle")) {
				cytoscapeVersion = b.getVersion().toString();
				// break;
			} else if (b.getSymbolicName().equals("org.cytoscape.ndex.cyNDEx")) {
				/*
				 * Version v = b.getVersion(); System.out.println(v); int st = b.getState();
				 * System.out.println(st);
				 */
				hasCyNDEx1 = true;
			}
		}
		Bundle currentBundle = bc.getBundle();

		appVersion = currentBundle.getVersion().toString();

		Dictionary<?, ?> d = currentBundle.getHeaders();
		appName = (String) d.get("Bundle-name");

		
		// Import dependencies
		final CyServiceRegistrar serviceRegistrar = getService(bc, CyServiceRegistrar.class);
		CyServiceModule.setServiceRegistrar(serviceRegistrar);
		NdexAdminStatusService adminStatusSvc = url -> NdexV3AdminStatus.fetch(url, null);
		CyServiceModule.setAdminStatusService(adminStatusSvc);
		final CyApplicationConfiguration config = getService(bc, CyApplicationConfiguration.class);
		final CyApplicationManager appManager = getService(bc, CyApplicationManager.class);
		final CySwingApplication swingApplication = getService(bc, CySwingApplication.class);
		CyServiceModule.setSwingApplication(swingApplication);
		
		cyProps = getService(bc, CyProperty.class, "(cyPropertyName=cytoscape3.props)");
		taskManager = getService(bc, TaskManager.class);
		
		
	    File configDir = config.getAppConfigurationDirectoryLocation(CyActivator.class); 
	    configDir.mkdirs(); 
		
		// For loading network
	    CxTaskFactoryManager tfManager = CxTaskFactoryManager.INSTANCE;
		registerServiceListener(bc, tfManager, "addReaderFactory", "removeReaderFactory", InputStreamTaskFactory.class);
		registerServiceListener(bc, tfManager, "addWriterFactory", "removeWriterFactory",
				CyNetworkViewWriterFactory.class);

		ciServiceManager = new CIServiceManager(bc);

		// Create subdirectories in config dir for jxbrowser
		final CyNetworkManager netmgr = getService(bc, CyNetworkManager.class);
		//File jxBrowserDir = new File(config.getConfigurationDirectoryLocation(), "jxbrowser");
		//jxBrowserDir.mkdir();
		//BrowserManager.setDataDirectory(new File(jxBrowserDir, "data"));
		
		// TF for NDEx Save Network
		final OpenSaveTaskFactory ndexSaveNetworkTaskFactory = new OpenSaveTaskFactory(appManager);
		final Properties ndexSaveNetworkTaskFactoryProps = new Properties();

		ndexSaveNetworkTaskFactoryProps.setProperty(PREFERRED_MENU, "File.Export");
		ndexSaveNetworkTaskFactoryProps.setProperty(MENU_GRAVITY, "0.0");
		ndexSaveNetworkTaskFactoryProps.setProperty(TITLE, "Network to NDEx...");
		registerService(bc, ndexSaveNetworkTaskFactory, TaskFactory.class, ndexSaveNetworkTaskFactoryProps);

		
		ImportNetworkFromNDExTaskFactory importFromNDExTaskFactory = new ImportNetworkFromNDExTaskFactory(ExternalAppManager.APP_NAME_LOAD);
		ImportUserNetworkFromNDExTaskFactory importUserNetworkTaskFactory = new ImportUserNetworkFromNDExTaskFactory(ExternalAppManager.APP_NAME_LOAD);
		
		SaveNetworkToNDExTaskFactory saveToNDExTaskFactory = new SaveNetworkToNDExTaskFactory(appManager, ExternalAppManager.APP_NAME_SAVE);

		MainToolBarAction action = new MainToolBarAction(importFromNDExTaskFactory, importUserNetworkTaskFactory, saveToNDExTaskFactory, serviceRegistrar);
		registerService(bc, action, CyAction.class);
		
		// TF for NDEx Load
		Icon icon = IconUtil.getNdexIcon();
		
		final OpenBrowseTaskFactory ndexTaskFactory = new OpenBrowseTaskFactory(icon);
		final Properties ndexTaskFactoryProps = new Properties();
		// ndexTaskFactoryProps.setProperty(IN_MENU_BAR, "false");
		ndexTaskFactoryProps.setProperty(PREFERRED_MENU, "File.Import");
		ndexTaskFactoryProps.setProperty(MENU_GRAVITY, "0.0");
		ndexTaskFactoryProps.setProperty(TITLE, "Network from NDEx...");
		registerAllServices(bc, ndexTaskFactory, ndexTaskFactoryProps);

		// Expose CyREST endpoints
		final ErrorBuilder errorBuilder = new ErrorBuilder(ciServiceManager, config);
		final NdexClient ndexClient = new NdexClient(errorBuilder);
		CyServiceModule.setErrorBuilder(errorBuilder);
		
		// Base
		registerService(bc,
				new NdexBaseResourceImpl(bc.getBundle().getVersion().toString(), ciServiceManager),
				NdexBaseResource.class, new Properties());

		// Status
		registerService(bc, new NdexStatusResourceImpl(ciServiceManager), NdexStatusResource.class,
				new Properties());

		// Network IO
		registerService(bc, new NdexNetworkResourceImpl(ndexClient, appManager, netmgr, ciServiceManager),
				NdexNetworkResource.class, new Properties());

		OpenSaveTaskFactory saveNetworkToNDExContextMenuTaskFactory = new OpenSaveTaskFactory(appManager);
		Properties saveNetworkToNDExContextMenuProps = new Properties();
		saveNetworkToNDExContextMenuProps.setProperty(ID, "exportToNDEx");
		saveNetworkToNDExContextMenuProps.setProperty(TITLE, StringResources.NDEX_SAVE.concat("..."));
		saveNetworkToNDExContextMenuProps.setProperty(IN_NETWORK_PANEL_CONTEXT_MENU, "true");
		saveNetworkToNDExContextMenuProps.setProperty(INSERT_SEPARATOR_BEFORE, "true");
		saveNetworkToNDExContextMenuProps.setProperty(ENABLE_FOR, "network");

		registerService(bc, saveNetworkToNDExContextMenuTaskFactory, NetworkCollectionTaskFactory.class,
				saveNetworkToNDExContextMenuProps);

	}

	@Override
	public void shutDown() {
		logger.info("Shutting down CyNDEx-2...");

		if (ciServiceManager != null) {
			ciServiceManager.close();
		}
		
		super.shutDown();
	}

	public static String getCyVersion() {
		return cytoscapeVersion;
	}

	public static String getAppName() {
		return appName;
	}

	public static boolean hasCyNDEx1() {
		return hasCyNDEx1;
	}

	public static void setHasCyNDEX1(boolean hasCyNDEx1) {
		CyActivator.hasCyNDEx1 = hasCyNDEx1;
	}

	public static String getAppVersion() {
		return appVersion;
	}

}