/*
 * Copyright (c) 2014, the Cytoscape Consortium and the Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.cytoscape.cyndex2.internal.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.cytoscape.cyndex2.internal.CyServiceModule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 *
 * @author David Welker
 * @author jingchen
 */
public class ServerManager {
	public static ServerManager INSTANCE = new ServerManager();

	private PropertyChangeSupport mPcs = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		mPcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		mPcs.removePropertyChangeListener(listener);
	}

	private final ServerList availableServers;
	private ServerKey selectedServer;

	private ServerManager() {
		File configDir = CyServiceModule.INSTANCE.getConfigDir();
		File addedServersJsonFile = new File(configDir, FilePath.ADDED_SERVERS);
		availableServers = ServerList.readServerList(addedServersJsonFile);
		selectedServer = readSelectedServer();
		if (selectedServer == null)
			selectNextAvailableServer();
	}

	private void selectNextAvailableServer() {
		{
			if (availableServers.getSize() > 0) {
				selectedServer = new ServerKey(availableServers.getElementAt(0));
			}
		}
	}

	public ServerList getAvailableServers() {
		return availableServers;
	}

	public Server getSelectedServer() {
		return selectedServer != null ? availableServers.getServer(selectedServer) : null;
	}

	public Server getServer() {
		return selectedServer != null ? availableServers.getServer(selectedServer) : Server.DEFAULT_SERVER;
	}

	public static String addHttpProtocol(String url) {
		return UrlUtils.addHttpProtocol(url);
	}

	public static String addHttpsProtocol(String url) {
		return UrlUtils.addHttpsProtocol(url);
	}

	public static String getBaseRoute(String url) {
		return UrlUtils.getBaseRoute(url);
	}

	/**
	 * Adds a new server to the server list and automatically selects it.
	 * 
	 * @param server
	 * @throws Exception
	 */
	public void addServer(final String username, final String password, final String serverUrl) throws Exception {

		final String baseroute = getBaseRoute(serverUrl);


/*		if (availableServers.getServer(new ServerKey(username, serverUrl)) != null) {
			throw new Exception("Server already exists.");
		} */

		if (username == null && password == null) {
			// Anonymous access
		} else if (username != null && password != null) {
			CredentialsProvider provider = new BasicCredentialsProvider();
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);

			provider.setCredentials(AuthScope.ANY, credentials);

			HttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
			final String url = baseroute.concat("/user?valid=true");

			final HttpGet get = new HttpGet(url);

			final HttpResponse response = httpClient.execute(get);

			final HttpEntity entity = response.getEntity();
			final String result = EntityUtils.toString(entity);

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readValue(result, JsonNode.class);

			if (response.getStatusLine().getStatusCode() != 200) {
				try {
					final String message = jsonNode.get("message").asText();
					throw new Exception(message);
				} catch (Exception e) {
					throw new Exception(e);
				}
			}
		} else if (password == null) {
			throw new Exception("Password must be provided");
		} else if (username == null) {
			throw new Exception("Passwords are not used for anonymous access.");
		}

		Server server = availableServers.getServer(new ServerKey(username, serverUrl));
		if (server == null) {
			server = new Server();

			server.setUrl(serverUrl);

			server.setUsername(username);
			server.setPassword(password);

			availableServers.add(server);
			availableServers.writeServerList(getServerListFile());
		}

		setSelectedServer(new ServerKey(server));
		saveSelectedServer();
	}

	private static File getServerListFile() {
		final File configDir = CyServiceModule.INSTANCE.getConfigDir();
		final File addedServersFile = new File(configDir, FilePath.ADDED_SERVERS);
		return addedServersFile;
	}
	
	public void removeServer(Server server) {
		availableServers.delete(server);
		availableServers.writeServerList(getServerListFile());
		ServerKey oldValue = selectedServer;
		selectedServer = null;
		selectNextAvailableServer();
		saveSelectedServer();
		PropertyChangeEvent event = new PropertyChangeEvent(this, "selectedServer", oldValue, selectedServer);
		mPcs.firePropertyChange(event);
	}

	public void saveSelectedServer() {
		File configDir = CyServiceModule.INSTANCE.getConfigDir();
		File selectedServersFile = new File(configDir, FilePath.SELECTED_SERVER);
		saveSelectedServerKey(selectedServer, selectedServersFile.getAbsolutePath());
	}

	private static void saveSelectedServerKey(ServerKey serverKey, String filePath) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(serverKey);
		File serverFile = new File(filePath);
		try {
			Files.asCharSink( serverFile, Charsets.UTF_8).write(json);
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	public void setSelectedServer(ServerKey serverKey) {
		if (availableServers.getServer(serverKey) == null) {
			throw new IllegalArgumentException("No profile exists for " + serverKey);
		}
		final ServerKey oldValue = this.selectedServer;
		this.selectedServer = serverKey;
		this.saveSelectedServer();
		PropertyChangeEvent event = new PropertyChangeEvent(this, "selectedServer", oldValue, serverKey);
		mPcs.firePropertyChange(event);
	}

	private static ServerKey readSelectedServer() {
		File configDir = CyServiceModule.INSTANCE.getConfigDir();
		File selectedServerJsonFile = new File(configDir, FilePath.SELECTED_SERVER);
		return readSelectedServer(selectedServerJsonFile);
	}

	private static ServerKey readSelectedServer(File jsonFile) {
		try (BufferedReader br = new BufferedReader(new FileReader(jsonFile))) {
			Gson gson = new Gson();
			ServerKey result = gson.fromJson(br, ServerKey.class);
			return result;
		} catch (IOException ex) {
			Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

}
