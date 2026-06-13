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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.UUID;

import org.ndexbio.model.exceptions.NdexException;
import org.cytoscape.cyndex2.internal.util.UrlUtils;
import org.ndexbio.model.object.NdexStatus;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

/**
 *
 * @author David Welker
 * @author David Otasek
 */
public class Server
{
  	public static final Server DEFAULT_SERVER = Server.getDefaultServer();
    
  	private static final Server getDefaultServer() {
  		final Server server = new Server();
  		server.url = "http://public.ndexbio.org/v2";
  		return server;
  	}
  			
    private String url;
    private String username;
    private String password;
    private UUID userId;
    
    /**
     * Default constructor,
     */
    public Server()
    {
    }
    
    /**
     * Copy constructor/
     * @param s The server to be copied.
     */
    public Server(Server s)
    {
        url = s.url;
        username = s.username;
        password = s.password;
        userId = s.getUserId();
    }

    public boolean isRunningNdexServer(NdexRestClientModelAccessLayer mal)
    {
    		try {
					final NdexStatus ndexStatus = mal.getServerStatus();
					return ndexStatus != null;
				} catch (IOException | NdexException e) {
					Logger.getLogger(Server.class.getName()).log(Level.WARNING, "NDEx server check failed", e);
					return false;
				}
    	}
    
    public boolean check(NdexRestClientModelAccessLayer mal) throws IOException
    {
        boolean usernamePresent = username != null && !username.isEmpty();
        boolean passwordPresent = password != null && !password.isEmpty();
        if( !usernamePresent && !passwordPresent )
        {
            return true;
        }
        else
        {
            return true;
        }
    }
    
    @Override
    public boolean equals(Object o) {
    	if (o instanceof Server) {
    		final Server b = (Server) o;
    		final boolean sameUrl = this.url == null ? b.url == null : this.url.equals(b.url);
    		final boolean sameUser = this.username == null ? b.username == null : this.username.equals(b.username);
    		return sameUrl && sameUser; 
    	}
    	return false;
    }
    
  
 /*   public String show()
    {
        String result = "";
        result += "Name: " + name +"\n";
        result += "URL: " + url +"\n";
        result += "Username: " + username +"\n";
        result += "Password: " + password +"\n";
        result += "Type: " + type +"\n";
        result += "Authenticated: " + authenticated +"\n";
        result += "UUID: " + userId +"\n";
        return result;
    } */
    
    public NdexRestClientModelAccessLayer getModelAccessLayer() throws IOException, NdexException
    {
    	final String apiUrl = UrlUtils.getBaseRoute(url);
    	final NdexRestClient client = new NdexRestClient(username,password,apiUrl, UserAgentUtil.getUserAgent());
			return new NdexRestClientModelAccessLayer(client);
    }
   
    @Override
    public String toString()
    {
        return username + "@" + url;
    }

    public String getUrl()
    {
        return url;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public void setUsername(String username)
    {
        if( username != null && username.trim().equals("") )
            this.username = null;
        else
            this.username = username;
    }

    public void setPassword(String password)
    {
        if( password != null && password.trim().equals("") )
            this.password = null;
        else
            this.password = password;
    }

  

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}
    
}
