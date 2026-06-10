package org.cytoscape.cyndex2.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.cytoscape.cyndex2.internal.rest.NdexV3AdminStatus;

import org.junit.Before;
import org.junit.Test;

public class ServerManagerFetchStatusTest {

	private HttpClient mockClient;
	private HttpResponse mockResponse;
	private StatusLine mockStatusLine;
	private HttpEntity mockEntity;

	@Before
	public void setUp() {
		mockClient = mock(HttpClient.class);
		mockResponse = mock(HttpResponse.class);
		mockStatusLine = mock(StatusLine.class);
		mockEntity = mock(HttpEntity.class);

		when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
		when(mockResponse.getEntity()).thenReturn(mockEntity);
	}

	private void setupResponse(int statusCode, String body) throws IOException {
		when(mockStatusLine.getStatusCode()).thenReturn(statusCode);
		when(mockEntity.getContent()).thenReturn(
				new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
		when(mockEntity.getContentLength()).thenReturn((long) body.length());
		when(mockEntity.getContentEncoding()).thenReturn(null);
		when(mockEntity.getContentType()).thenReturn(null);
		when(mockClient.execute(any(HttpUriRequest.class))).thenReturn(mockResponse);
	}

	@Test
	public void testHappyPath() throws IOException {
		String json = "{\"oauth_register_url\":\"https://auth.example.org/register\","
				+ "\"oauth_reset_url\":\"https://auth.example.org/reset\"}";
		setupResponse(200, json);

		NdexV3AdminStatus result = NdexV3AdminStatus.fetch("public.ndexbio.org", mockClient);

		assertNotNull(result);
		assertEquals("https://auth.example.org/register", result.getOauthRegisterUrl());
		assertEquals("https://auth.example.org/reset", result.getOauthResetUrl());
		assertTrue(result.hasValidOAuthUrls());
	}

	@Test
	public void testMissingOauthFields() throws IOException {
		String json = "{\"version\":\"3.0\",\"build\":\"abc\"}";
		setupResponse(200, json);

		NdexV3AdminStatus result = NdexV3AdminStatus.fetch("public.ndexbio.org", mockClient);

		assertNotNull(result);
		assertNull(result.getOauthRegisterUrl());
		assertNull(result.getOauthResetUrl());
		assertFalse(result.hasValidOAuthUrls());
	}

	@Test
	public void testNon200Response() throws IOException {
		when(mockStatusLine.getStatusCode()).thenReturn(404);
		when(mockClient.execute(any(HttpUriRequest.class))).thenReturn(mockResponse);

		NdexV3AdminStatus result = NdexV3AdminStatus.fetch("public.ndexbio.org", mockClient);

		assertNull(result);
	}

	@Test
	public void testNetworkException() throws IOException {
		when(mockClient.execute(any(HttpUriRequest.class))).thenThrow(new IOException("Connection refused"));

		NdexV3AdminStatus result = NdexV3AdminStatus.fetch("public.ndexbio.org", mockClient);

		assertNull(result);
	}

	@Test
	public void testMalformedJson() throws IOException {
		setupResponse(200, "not-valid-json{{");

		NdexV3AdminStatus result = NdexV3AdminStatus.fetch("public.ndexbio.org", mockClient);

		assertNull(result);
	}

	@Test
	public void testNonHttpsUrlsRejected() throws IOException {
		String json = "{\"oauth_register_url\":\"http://auth.example.org/register\","
				+ "\"oauth_reset_url\":\"https://auth.example.org/reset\"}";
		setupResponse(200, json);

		NdexV3AdminStatus result = NdexV3AdminStatus.fetch("public.ndexbio.org", mockClient);

		assertNotNull(result);
		assertFalse(result.hasValidOAuthUrls());
	}

}
