package org.cytoscape.cyndex2.internal.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorBuilder;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Minimalistic NDEx V2 client
 *
 */
public class NdexClient {

	private static final Logger logger = LoggerFactory.getLogger(NdexClient.class);

	private static final String PUBLIC_NDEX_URL = "http://www.ndexbio.org/v2";

	private final ObjectMapper mapper;
	private final ErrorBuilder errorBuilder;

	public NdexClient(final ErrorBuilder errorBuilder) {
		mapper = new ObjectMapper();
		this.errorBuilder = errorBuilder;

	}

	private final String getAuth(final String id, final String pw) {
		String credentials = id + ":" + pw;
		return "Basic " + new String(new Base64().encode(credentials.getBytes()));
	}

	private final CloseableHttpClient getClient(String id, String pw) {

		if (id == null || id.isEmpty() || pw == null || pw.isEmpty()) {
			return HttpClients.custom().build();
		}

		final Header header = new BasicHeader(HttpHeaders.AUTHORIZATION, getAuth(id, pw));
		final List<Header> headers = new ArrayList<>();
		headers.add(header);
		return HttpClients.custom().setDefaultHeaders(headers).build();
	}

	public Map<String, ?> getSummary(String url, String uuid) throws Exception {
		return getSummary(url, uuid, null, null);
	}

	/**
	 * Call network summary API
	 */
	@SuppressWarnings("unchecked")
	public Map<String, ?> getSummary(String url, String uuid, String userId, String pw) throws WebApplicationException {

		String serverUrl = null;
		if (url == null || url.isEmpty()) {
			serverUrl = PUBLIC_NDEX_URL + "/network/" + uuid + "/summary";
		} else {
			serverUrl = url + "/network/" + uuid + "/summary";
		}

		CloseableHttpClient client = getClient(userId, pw);
		HttpGet httpget = new HttpGet(serverUrl);

		CloseableHttpResponse response = null;
		try {
			response = client.execute(httpget);
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, "Could not get network summary from NDEx.",
					ErrorType.NDEX_API);
		}

		// Check response
		getError(response);

		Map<String, ?> result = null;
		String val;
		try {
			val = EntityUtils.toString(response.getEntity());
			result = mapper.readValue(val, Map.class);
			response.close();
			client.close();
		} catch (ParseException | IOException e) {
			logger.error(e.getMessage(), e);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, "Could not build network summary object.",
					ErrorType.INTERNAL);
		}

		return result;
	}

	private final void getError(CloseableHttpResponse response) throws WebApplicationException {
		final int code = response.getStatusLine().getStatusCode();

		if (code == Status.NOT_FOUND.getStatusCode()) {
			throw errorBuilder.buildException(Status.NOT_FOUND, "Resource not found", ErrorType.INVALID_PARAMETERS);
		}

		if (code == Status.UNAUTHORIZED.getStatusCode()) {
			throw errorBuilder.buildException(Status.UNAUTHORIZED, "Autholization failed", ErrorType.AUTH_FAILED);
		}

		if (code == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, "Remote service call failed.",
					ErrorType.INTERNAL);
		}
	}

	public void setVisibility(String url, String uuid, boolean isPublic, String id, String pw) {
		final String endpoint = url + "/network/" + uuid + "/systemproperty";

		String visibility;

		if (isPublic) {
			visibility = "PUBLIC";
		} else {
			visibility = "PRIVATE";
		}

		final Map<String, String> propMap = new HashMap<>();
		propMap.put("visibility", visibility);

		CloseableHttpClient client = getClient(id, pw);
		HttpPut httpput = new HttpPut(endpoint);

		String props = null;
		try {
			props = mapper.writeValueAsString(propMap);
		} catch (JsonProcessingException e1) {
			final String message = "Given parameters are invalid";
			logger.error(message, e1);
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INTERNAL);
		}
		StringEntity params = new StringEntity(props, "UTF-8");
		params.setContentType("application/json");
		httpput.setEntity(params);

		CloseableHttpResponse response = null;

		try {
			response = client.execute(httpput);
		} catch (Exception e) {
			final String message = "Could not update visibility.";
			logger.error(message, e);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.NDEX_API);
		}
		getError(response);
	}

}
