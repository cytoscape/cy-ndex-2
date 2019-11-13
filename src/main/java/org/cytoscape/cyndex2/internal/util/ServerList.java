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
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractListModel;

import org.cytoscape.cyndex2.internal.CyServiceModule;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 *
 * @author David Welker
 * @author David Otasek
 */
public class ServerList extends AbstractListModel<Server>
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//A list of servers, both DEFAULT and ADDED, displayed to the user.
    private List<Server> serverList = new ArrayList<>();
    //A list of DEFAULT servers
    private List<Server> defaultServerList = new ArrayList<>();
    
    public ServerList()
    {
        super();
        readServers();
    }
    
    private void readServers()
    {
    	File configDir = CyServiceModule.INSTANCE.getConfigDir();
      File addedServersJsonFile = new File(configDir, FilePath.ADDED_SERVERS);
      Collection<Server> addedServers = readServerCollection(addedServersJsonFile);     
      serverList.addAll(addedServers);
    }
    
    private Collection<Server> readServerCollection(String resourcePath)
    {  
        URL json = ServerList.class.getClassLoader().getResource(resourcePath);
        try
        {
            return readServerCollection( new InputStreamReader (json.openStream()) );
        }
        catch (IOException ex)
        {
            Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
            //Return null because this is an error you want to detect right away.
            return null;
        }
    }
    
   
    
    private Collection<Server> readServerCollection(File jsonFile)
    {
        try
        {
            return readServerCollection( new FileReader(jsonFile) );
        }
        catch (IOException ex)
        {
            Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
            //Return an empty server list, because sometimes a file won't exist and that is perfectly normal.
            return new ArrayList<>();
        }
    }
    
    private Collection<Server> readServerCollection(Reader reader) throws IOException
    {
        try (BufferedReader br = new BufferedReader( reader )) {
        	Gson gson = new Gson();
        	Type collectionType = new TypeToken<Collection<Server>>(){}.getType();
        	Collection<Server> result =  gson.fromJson(br, collectionType);
            return result;        
        }
    }
    
    
    public void save()
    {
        File configDir = CyServiceModule.INSTANCE.getConfigDir();
        File addedServersFile = new File(configDir, FilePath.ADDED_SERVERS);
        saveServerList( serverList, addedServersFile.getAbsolutePath() );
    }
    
    public Stream<Server> stream() { return serverList.stream(); }
    
    private void saveServerList( List<Server> serverList2, String filePath )
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson( serverList2 );
        File serverFile = new File( filePath );
        try
        {
            Files.write(json, serverFile, Charsets.UTF_8);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }    
        
    }
    
    public void delete(Server server)
    {
        if( !serverList.contains(server) )
            throw new IllegalArgumentException("The server to be deleted must exist in the list.");
        int indexOfDeletedServer = serverList.indexOf(server);
        serverList.remove(server);
        this.fireIntervalRemoved(server, indexOfDeletedServer, indexOfDeletedServer);
    }
    
    public void add(Server server) throws Exception
    {
        if( serverList.contains(server) )
            throw new Exception( ErrorMessage.serverNameAlreadyUsed );
        serverList.add(server);
        int indexOfAddedServer = serverList.indexOf(server);
        fireContentsChanged(this, indexOfAddedServer, indexOfAddedServer);
    }
    
    public Server get(int index)
    {
        return serverList.get(index);
    }
    
    public Server getServer(ServerKey serverKey) {
    	List<Server> result = serverList.stream().filter(server -> serverKey.username.equals(server.getUsername()) && serverKey.url.equals(server.getUrl())).collect(Collectors.toList());
    	return result.size() == 1 ? result.get(0) : null;	
    }
    
    @Override
    public int getSize()
    {
        return serverList.size();
    }

    @Override
    public Server getElementAt(int index)
    {
        return serverList.get(index);
    }
    
}