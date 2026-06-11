package org.cytoscape.cyndex2.internal.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NdexV3AdminStatus {

	@JsonProperty("oauth_register_url")
	private String oauth_register_url;

	@JsonProperty("oauth_reset_url")
	private String oauth_reset_url;

	public NdexV3AdminStatus() {
	}

	public String getOauthRegisterUrl() {
		return oauth_register_url;
	}

	public void setOauthRegisterUrl(String oauth_register_url) {
		this.oauth_register_url = oauth_register_url;
	}

	public String getOauthResetUrl() {
		return oauth_reset_url;
	}

	public void setOauthResetUrl(String oauth_reset_url) {
		this.oauth_reset_url = oauth_reset_url;
	}

	public static boolean isValidUrl(String url) {
		if (url == null) return false;
		try {
			String scheme = new URI(url).getScheme();
			return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
		} catch (URISyntaxException e) {
			return false;
		}
	}

	public boolean hasValidOAuthUrls() {
		return isValidUrl(oauth_register_url) && isValidUrl(oauth_reset_url);
	}

	public static NdexV3AdminStatus fetch(String serverUrl, HttpClient httpClient) {
		if (httpClient == null) {
			int timeoutMs = 5000;
			RequestConfig config = RequestConfig.custom()
					.setConnectTimeout(timeoutMs)
					.setSocketTimeout(timeoutMs)
					.build();
			httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
		}
		try {
			String baseUrl = isValidUrl(serverUrl) ? serverUrl : "https://" + serverUrl;
			final HttpGet get = new HttpGet(baseUrl + "/v3/admin/status");
			final HttpResponse response = httpClient.execute(get);

			if (response.getStatusLine().getStatusCode() != 200) {
				return null;
			}

			final HttpEntity entity = response.getEntity();
			final String body = EntityUtils.toString(entity);

			return new ObjectMapper().readValue(body, NdexV3AdminStatus.class);
		} catch (Exception e) {
			Logger.getLogger(NdexV3AdminStatus.class.getName()).log(Level.WARNING,
					"Could not fetch /v3/admin/status: " + e.getMessage());
			return null;
		}
	}
}
