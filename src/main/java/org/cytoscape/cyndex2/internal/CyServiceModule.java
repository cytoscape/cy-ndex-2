package org.cytoscape.cyndex2.internal;

import java.io.File;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.cyndex2.internal.rest.NdexAdminStatusService;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorBuilder;
import org.cytoscape.service.util.CyServiceRegistrar;

public class CyServiceModule {
	
	public static CyServiceModule INSTANCE = new CyServiceModule();
	
	private CyServiceRegistrar registrar;

	private CySwingApplication swingApplication;

	private ErrorBuilder errorBuilder;

	private NdexAdminStatusService adminStatusService;
	
	private CyServiceModule() {
		
	}
	public static void setServiceRegistrar(CyServiceRegistrar registrar) {
		INSTANCE.registrar = registrar;
	}
	
	public static final <S> S getService(Class<S> serviceClass, String filter) {
		return INSTANCE.registrar != null ? INSTANCE.registrar.getService(serviceClass, filter) : null;
	}
	
	public static <T> T getService(Class<T> clz) {
		return INSTANCE.registrar.getService(clz);
	}
	public static void setErrorBuilder(ErrorBuilder errorBuilder) {
		INSTANCE.errorBuilder = errorBuilder;
	}

	public static void setAdminStatusService(NdexAdminStatusService svc) {
		INSTANCE.adminStatusService = svc;
	}

	public static NdexAdminStatusService getAdminStatusService() {
		return INSTANCE.adminStatusService;
	}
	
	public static void setSwingApplication(CySwingApplication swingApplication) {
		INSTANCE.swingApplication = swingApplication;
	}
	
	public CySwingApplication getSwingApplication() {
		return swingApplication;
	}
	
	public ErrorBuilder getErrorBuilder() {
		return errorBuilder;
	}
	
	public CyApplicationConfiguration getConfig() {
		return registrar.getService(CyApplicationConfiguration.class);
	}
	
	public File getConfigDir() {
		return getConfig().getAppConfigurationDirectoryLocation(CyActivator.class);
	}
}
