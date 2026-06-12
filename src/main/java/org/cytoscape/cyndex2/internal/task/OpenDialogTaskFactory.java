package org.cytoscape.cyndex2.internal.task;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.Frame;
import java.io.IOException;
import java.net.URI;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.cytoscape.cyndex2.external.SaveParameters;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.cyndex2.internal.rest.parameter.LoadParameters;
import org.cytoscape.cyndex2.internal.ui.swing.ExportNetworkDialog;
import org.cytoscape.cyndex2.internal.ui.swing.FindNetworksDialog;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OpenDialogTaskFactory extends AbstractTaskFactory {
	
	protected final String appName;

	
	
	public OpenDialogTaskFactory(final String appName) {
		super();
		this.appName = appName;

	}

	@Override
	public TaskIterator createTaskIterator() {
		TaskIterator ti = new TaskIterator();

		// Store query info
		ExternalAppManager.appName = appName;

		ti.append(new AbstractTask() {

			@Override
			public void run(TaskMonitor taskMonitorParameter) throws Exception {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						String REST_URI = "http://localhost:" + CyActivator.getCyRESTPort() +"/cyndex2/v1/status";
						HttpClient httpClient = HttpClients.createDefault();
						final URI uri = URI.create(REST_URI);
						final HttpGet get = new HttpGet(uri.toString());
						get.setHeader("Content-type", "application/json");
						HttpResponse response;
						try {
							response = httpClient.execute(get);
						
							final HttpEntity entity = response.getEntity();
							final String result = EntityUtils.toString(entity);
							
							ObjectMapper objectMapper = new ObjectMapper();
							JsonNode jsonNode = objectMapper.readValue(result, JsonNode.class);
						
							final String widget = jsonNode.get("data").get("widget").asText();
							
							switch(widget) {
								case "choose": 
									final LoadParameters loadParameters = objectMapper.treeToValue(jsonNode.get("data").get("parameters"), LoadParameters.class);
									FindNetworksDialog.getFindNetworksDialog(loadParameters);
									break;
								case "save" :
									final JFrame parentFrame = CyServiceModule.INSTANCE.getSwingApplication().getJFrame();
									final SaveParameters saveParameters = objectMapper.treeToValue(jsonNode.get("data").get("parameters"), SaveParameters.class);
									final ExportNetworkDialog saveDialog = new ExportNetworkDialog(parentFrame, saveParameters);
									saveDialog.setVisible(true);
									break;
								default : break;
							}	
						} catch (IOException e) {
							Logger.getLogger(OpenDialogTaskFactory.class.getName()).log(Level.WARNING, "Failed to fetch dialog parameters", e);
						}
					}
				});
			}
		});

		return ti;
	}
	
	
	
	@Override
	public boolean isReady() {
		return !ExternalAppManager.loadFailed();
	}

}
