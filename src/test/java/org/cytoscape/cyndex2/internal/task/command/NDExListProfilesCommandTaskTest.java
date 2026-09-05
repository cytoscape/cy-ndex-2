package org.cytoscape.cyndex2.internal.task.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.cytoscape.cyndex2.internal.util.NdexProfile;
import org.cytoscape.cyndex2.internal.util.NdexProfileResolver;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.json.JSONResult;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The JSON projection. Resolver semantics -- ordering, isCurrent, round-tripping -- live in
 * NdexProfileResolverTest, where both list() and resolve() are the real implementations.
 */
public class NDExListProfilesCommandTaskTest {

	private final TaskMonitor taskMonitor = mock(TaskMonitor.class);

	private static NdexProfile profile(String name, String username, String url, boolean current) {
		NdexProfile p = mock(NdexProfile.class);
		when(p.getName()).thenReturn(name);
		when(p.getUsername()).thenReturn(username);
		when(p.getServerUrl()).thenReturn(url);
		when(p.isCurrent()).thenReturn(current);
		return p;
	}

	private static JsonNode run(List<NdexProfile> profiles) throws Exception {
		NdexProfileResolver resolver = mock(NdexProfileResolver.class);
		when(resolver.list()).thenReturn(profiles);
		NDExListProfilesCommandTask task = new NDExListProfilesCommandTask(resolver);
		task.run(mock(TaskMonitor.class));
		return new ObjectMapper().readTree(task.getResults(String.class));
	}

	@Test
	public void anEmptyListIsAnEmptyResultNotAnError() throws Exception {
		JsonNode result = run(Collections.emptyList());

		assertEquals(0, result.get("count").asInt());
		assertTrue(result.get("profiles").isArray());
		assertEquals(0, result.get("profiles").size());
	}

	@Test
	public void aProfileIsProjectedOntoTheDocumentedFields() throws Exception {
		JsonNode result = run(Collections.singletonList(
				profile("alice@https://www.ndexbio.org", "alice", "https://www.ndexbio.org", true)));

		JsonNode row = result.get("profiles").get(0);
		assertEquals("alice@https://www.ndexbio.org", row.get("name").asText());
		assertEquals("alice", row.get("username").asText());
		assertEquals("https://www.ndexbio.org", row.get("serverUrl").asText());
		assertTrue(row.get("isCurrent").asBoolean());
	}

	@Test
	public void exactlyTheDocumentedFieldsAndNoOthers() throws Exception {
		// Pins the contract: nothing about credentials or validity may creep into this listing.
		JsonNode row = run(Collections.singletonList(
				profile("alice@https://www.ndexbio.org", "alice", "https://www.ndexbio.org", true)))
				.get("profiles").get(0);

		assertEquals(4, row.size());
		for (Iterator<String> it = row.fieldNames(); it.hasNext();) {
			String field = it.next();
			assertTrue("unexpected field '" + field + "' in the profile listing",
					Arrays.asList("name", "username", "serverUrl", "isCurrent").contains(field));
		}
	}

	@Test
	public void noCredentialStateOrSecretAppearsAnywhere() throws Exception {
		String json = jsonFor(profile("alice@https://www.ndexbio.org", "alice",
				"https://www.ndexbio.org", true));

		assertFalse(json, json.toLowerCase().contains("password"));
		assertFalse(json, json.contains("s3cr3t"));
		assertFalse(json, json.toLowerCase().contains("signedin"));
		assertFalse(json, json.toLowerCase().contains("credential"));
	}

	private static String jsonFor(NdexProfile p) throws Exception {
		NdexProfileResolver resolver = mock(NdexProfileResolver.class);
		when(resolver.list()).thenReturn(Collections.singletonList(p));
		NDExListProfilesCommandTask task = new NDExListProfilesCommandTask(resolver);
		task.run(mock(TaskMonitor.class));
		return task.getResults(String.class);
	}

	@Test
	public void anAnonymousProfileReportsANullUsername() throws Exception {
		JsonNode row = run(Collections.singletonList(
				profile("@https://www.ndexbio.org", null, "https://www.ndexbio.org", false)))
				.get("profiles").get(0);

		assertTrue(row.get("username").isNull());
		assertEquals("@https://www.ndexbio.org", row.get("name").asText());
	}

	@Test
	public void countMatchesTheNumberOfRowsAndOrderIsPreserved() throws Exception {
		JsonNode result = run(Arrays.asList(
				profile("alice@https://www.ndexbio.org", "alice", "https://www.ndexbio.org", false),
				profile("bob@https://www.ndexbio.org", "bob", "https://www.ndexbio.org", true)));

		assertEquals(2, result.get("count").asInt());
		assertEquals(2, result.get("profiles").size());
		assertEquals("alice@https://www.ndexbio.org", result.get("profiles").get(0).get("name").asText());
		assertEquals("bob@https://www.ndexbio.org", result.get("profiles").get(1).get("name").asText());
	}

	@Test
	public void jsonResultAndStringResultAgree() throws Exception {
		NdexProfileResolver resolver = mock(NdexProfileResolver.class);
		when(resolver.list()).thenReturn(Collections.emptyList());
		NDExListProfilesCommandTask task = new NDExListProfilesCommandTask(resolver);
		task.run(taskMonitor);

		assertEquals(task.getResults(String.class), task.getResults(JSONResult.class).getJSON());
	}

	@Test
	public void resultIsNullBeforeTheTaskRuns() {
		assertNull(new NDExListProfilesCommandTask(mock(NdexProfileResolver.class))
				.getResults(String.class));
	}

	@Test
	public void theTaskNeedsNoServerCollaboratorAtAll() throws Exception {
		// Constructed with only a resolver: no capabilities probe, no NDEx client. This command has to
		// work when NDEx is unreachable, because that is when it explains why nothing else does.
		NdexProfileResolver resolver = mock(NdexProfileResolver.class);
		when(resolver.list()).thenReturn(Collections.emptyList());

		new NDExListProfilesCommandTask(resolver).run(taskMonitor);
	}
}
