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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.AbstractListModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 *
 * @author David Welker
 * @author David Otasek
 */
public class ServerList extends AbstractListModel<Server> {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	public static final String KEY = "YlKMdJocN6eNNlCY";

	// A list of servers
	private List<Server> serverList = new ArrayList<>();

	public ServerList() {
		super();
	}
	
	public static ServerList readServerList(File file) {
		final ServerList serverList = new ServerList();
		Collection<Server> addedServers = readServerCollection(file);
		serverList.serverList.addAll(addedServers);
		return serverList;
	}

	private static Collection<Server> readServerCollection(File jsonFile) {
		try {
			return readServerCollection(new FileReader(jsonFile));
		} catch (IOException ex) {
			Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
			// Return an empty server list, because sometimes a file won't exist and that is
			// perfectly normal.
			return new ArrayList<>();
		}
	}

	private static Collection<Server> readServerCollection(Reader reader) throws IOException {
		try (BufferedReader br = new BufferedReader(reader)) {
			GsonBuilder builder = new GsonBuilder();
			builder.registerTypeAdapter(Server.class,
					(JsonDeserializer<Server>) (JsonElement json, Type typeOfT, JsonDeserializationContext context) -> {
						final JsonObject jsonObject = json.getAsJsonObject();
						final Server server = new Server();

						JsonElement usernameElement = jsonObject.get("username");
						server.setUsername(usernameElement != null ? usernameElement.getAsString() : null);
						server.setUrl(jsonObject.get("url").getAsString());

						SecretKey key = new SecretKeySpec(KEY.getBytes(), "AES");

						Cipher cipher;
						try {
							cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
							cipher.init(Cipher.DECRYPT_MODE, key);
							final JsonElement passwordElement = jsonObject.get("password");

							final String password = passwordElement != null
									? new String(cipher.doFinal(Base64.getDecoder().decode(passwordElement.getAsString())), "UTF-8")
									: null;
							server.setPassword(password);
						} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
								| IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
							Logger.getLogger(ServerList.class.getName()).log(Level.WARNING, "Failed to decrypt password", e);
						}

						return server;
					});
			Gson gson = builder.create();
			Type collectionType = new TypeToken<Collection<Server>>() {
			}.getType();
			Collection<Server> result = gson.fromJson(br, collectionType);
			return result;
		}
	}

	public void writeServerList(File file) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Server.class,
				(JsonSerializer<Server>) (Server server, Type type, JsonSerializationContext context) -> {
					JsonObject obj = new JsonObject();

					obj.addProperty("username", server.getUsername());
					obj.addProperty("url", server.getUrl());

					if (server.getPassword() != null) {
						SecretKey key = new SecretKeySpec(KEY.getBytes(), "AES");
						Cipher cipher;
						try {
							cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
							cipher.init(Cipher.ENCRYPT_MODE, key);

							obj.addProperty("password",
									Base64.getEncoder().encodeToString(cipher.doFinal(server.getPassword().getBytes("UTF-8"))));
						} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
								| IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
							Logger.getLogger(ServerList.class.getName()).log(Level.WARNING, "Failed to encrypt password", e);
						}
					}
					return obj;
				});
		Gson gson = gsonBuilder.setPrettyPrinting().create();
		String json = gson.toJson(serverList);
		
		try {
			Files.write(json, file, Charsets.UTF_8);
		} catch (IOException ex) {
			Logger.getLogger(ServerList.class.getName()).log(Level.WARNING, "Failed to write server list", ex);
		}
	}

	public Stream<Server> stream() {
		return serverList.stream();
	}

	public void delete(Server server) {
		if (!serverList.contains(server))
			throw new IllegalArgumentException("The server to be deleted must exist in the list.");
		int indexOfDeletedServer = serverList.indexOf(server);
		serverList.remove(server);
		this.fireIntervalRemoved(server, indexOfDeletedServer, indexOfDeletedServer);
	}

	public void add(Server server) throws Exception {
		if (serverList.contains(server))
			throw new Exception(ErrorMessage.serverNameAlreadyUsed);
		serverList.add(server);
		int indexOfAddedServer = serverList.indexOf(server);
		fireContentsChanged(this, indexOfAddedServer, indexOfAddedServer);
	}

	public Server getServer(ServerKey serverKey) {
		List<Server> result = serverList.stream()
				.filter(server -> serverKey.equals(new ServerKey(server.getUsername(), server.getUrl())))
				.collect(Collectors.toList());
		return result.size() == 1 ? result.get(0) : null;
	}

	@Override
	public int getSize() {
		return serverList.size();
	}

	@Override
	public Server getElementAt(int index) {
		return serverList.get(index);
	}

}
