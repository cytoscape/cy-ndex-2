package org.cytoscape.cyndex2.internal.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExImportParameters;
import org.junit.Test;

/**
 * The failure this class exists for is an HTTP 500 -- a *successful* exchange that throws nothing. A caller
 * watching only for exceptions sees a healthy round trip and reports nothing, which is how the download arrow
 * came to do nothing at all.
 */
public class NdexImportRequestTest {

	private static final String ENDPOINT = "http://localhost:1234/cyndex2/v1/networks";

	private static NDExImportParameters params() {
		return new NDExImportParameters("12345678-abcd-1234-abcd-1234567890ab", null, null,
				"https://www.ndexbio.org", null, null, true);
	}

	private static HttpResponse response(int status, String body) throws IOException {
		BasicHttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, status, "");
		if (body != null) {
			response.setEntity(new StringEntity(body));
		}
		return response;
	}

	@Test
	public void aSuccessfulImportSucceeds() throws Exception {
		NdexImportRequest.Outcome outcome = new NdexImportRequest(
				post -> response(200, "{\"data\":{\"suid\":52},\"errors\":[]}")).send(ENDPOINT, params());
		assertTrue(outcome.succeeded());
	}

	@Test
	public void aServerErrorIsReportedWithTheReasonTheServerGave() throws Exception {
		String body = "{\"data\":null,\"errors\":[{\"status\":500,"
				+ "\"message\":\"Failed to connect to server and retrieve network. www.ndexbio.orgv2\"}]}";
		NdexImportRequest.Outcome outcome = new NdexImportRequest(post -> response(500, body))
				.send(ENDPOINT, params());

		assertFalse(outcome.succeeded());
		// the CIError message is the only place the cause appears -- nothing on this path reaches the log
		assertEquals("Failed to connect to server and retrieve network. www.ndexbio.orgv2", outcome.getMessage());
	}

	@Test
	public void anUnparseableErrorBodyStillProducesAUsableMessage() throws Exception {
		NdexImportRequest.Outcome outcome = new NdexImportRequest(post -> response(500, "<html>gateway</html>"))
				.send(ENDPOINT, params());

		assertFalse(outcome.succeeded());
		assertTrue(outcome.getMessage(), outcome.getMessage().contains("500"));
	}

	@Test
	public void anErrorWithNoBodyStillProducesAUsableMessage() throws Exception {
		NdexImportRequest.Outcome outcome = new NdexImportRequest(post -> response(503, null))
				.send(ENDPOINT, params());

		assertFalse(outcome.succeeded());
		assertTrue(outcome.getMessage(), outcome.getMessage().contains("503"));
	}

	@Test
	public void anEmptyErrorsArrayFallsBackToTheStatus() throws Exception {
		NdexImportRequest.Outcome outcome = new NdexImportRequest(
				post -> response(500, "{\"data\":null,\"errors\":[]}")).send(ENDPOINT, params());

		assertFalse(outcome.succeeded());
		assertTrue(outcome.getMessage(), outcome.getMessage().contains("500"));
	}

	@Test
	public void anUnreachableCyRestIsReportedRatherThanThrown() {
		NdexImportRequest.Outcome outcome = new NdexImportRequest(post -> {
			throw new IOException("Connection refused");
		}).send(ENDPOINT, params());

		assertFalse(outcome.succeeded());
		assertNotNull(outcome.getMessage());
		assertTrue(outcome.getMessage(), outcome.getMessage().contains("Connection refused"));
	}

	@Test
	public void theRequestCarriesTheParametersAsJson() throws Exception {
		final String[] sent = { null };
		new NdexImportRequest(post -> {
			sent[0] = org.apache.http.util.EntityUtils.toString(post.getEntity());
			return response(200, "{}");
		}).send(ENDPOINT, params());

		assertTrue(sent[0], sent[0].contains("12345678-abcd-1234-abcd-1234567890ab"));
		assertTrue(sent[0], sent[0].contains("https://www.ndexbio.org"));
	}
}
