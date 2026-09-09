package org.cytoscape.cyndex2.internal.rest;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExImportParameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Posts an import request to CyNDEx-2's own CyREST endpoint and says what came back.
 *
 * Split out of the search dialog so the outcome of the call can be asserted without a running Cytoscape:
 * the failure this exists for -- a 500 whose body carries the real reason -- is a perfectly successful HTTP
 * exchange, so it is invisible to a caller that only watches for thrown exceptions.
 *
 * Deliberately free of Swing. The dialog decides what to show and when; this only reports.
 */
public class NdexImportRequest {

	/** What the endpoint said. A failed outcome always carries a message worth showing a user. */
	public static final class Outcome {

		private final boolean succeeded;
		private final String message;

		private Outcome(final boolean succeeded, final String message) {
			this.succeeded = succeeded;
			this.message = message;
		}

		public static Outcome ok() {
			return new Outcome(true, null);
		}

		public static Outcome failed(final String message) {
			return new Outcome(false, message);
		}

		public boolean succeeded() {
			return succeeded;
		}

		public String getMessage() {
			return message;
		}
	}

	/** The HTTP call, as a seam: tests supply a canned response instead of a server. */
	public interface Exchange {
		HttpResponse execute(HttpPost post) throws IOException;
	}

	private static final Exchange DEFAULT_EXCHANGE = post -> HttpClients.createDefault().execute(post);

	private final Exchange exchange;
	private final ObjectMapper mapper = new ObjectMapper();

	public NdexImportRequest() {
		this(DEFAULT_EXCHANGE);
	}

	/** For tests: supply the exchange instead of reaching a CyREST server. */
	public NdexImportRequest(final Exchange exchange) {
		this.exchange = exchange;
	}

	public Outcome send(final String endpointUrl, final NDExImportParameters params) {
		try {
			final HttpPost post = new HttpPost(endpointUrl);
			post.setHeader("Content-type", "application/json");
			post.setEntity(new StringEntity(mapper.writeValueAsString(params)));

			final HttpResponse response = exchange.execute(post);
			final int status = response.getStatusLine().getStatusCode();
			if (status >= 200 && status < 300) {
				return Outcome.ok();
			}
			return Outcome.failed(describe(status, response));
		} catch (final IOException e) {
			return Outcome.failed("Could not reach Cytoscape's own NDEx service: " + e.getMessage());
		}
	}

	/**
	 * CyREST wraps errors as {@code {"errors":[{"message": ...}]}}. That message is the only place the real
	 * cause appears -- nothing on this path is written to the log -- so dig it out, and fall back to the
	 * status when the body is not the shape we expect.
	 */
	private String describe(final int status, final HttpResponse response) {
		final String fallback = "NDEx import failed (HTTP " + status + ").";
		final HttpEntity entity = response.getEntity();
		if (entity == null) {
			return fallback;
		}
		try {
			final String body = EntityUtils.toString(entity);
			final JsonNode errors = mapper.readTree(body).get("errors");
			if (errors == null || !errors.isArray() || errors.size() == 0) {
				return fallback;
			}
			final JsonNode message = errors.get(0).get("message");
			if (message == null || message.asText().trim().isEmpty()) {
				return fallback;
			}
			return message.asText();
		} catch (final IOException | RuntimeException e) {
			return fallback;
		}
	}
}
