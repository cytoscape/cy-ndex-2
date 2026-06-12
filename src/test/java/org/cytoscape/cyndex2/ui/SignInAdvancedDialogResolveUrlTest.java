package org.cytoscape.cyndex2.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.cytoscape.cyndex2.internal.ui.swing.SignInAdvancedDialog;
import org.junit.Test;

public class SignInAdvancedDialogResolveUrlTest {

    @Test
    public void testHttpSucceeds_noRetry() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);
        String result = SignInAdvancedDialog.resolveServerUrl("symposium.ndexbio.org", candidate -> {
            calls.incrementAndGet();
            return true;
        });
        assertEquals("http://symposium.ndexbio.org", result);
        assertEquals(1, calls.get());
    }

    @Test
    public void testHttpFails_fallsBackToHttps() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);
        String result = SignInAdvancedDialog.resolveServerUrl("symposium.ndexbio.org", candidate -> {
            calls.incrementAndGet();
            if (candidate.toLowerCase().startsWith("https://")) return true;
            throw new IOException("HTTP connection refused");
        });
        assertEquals("https://symposium.ndexbio.org", result);
        assertEquals(2, calls.get());
    }

    @Test
    public void testBothFail_returnsNull() throws Exception {
        String result = SignInAdvancedDialog.resolveServerUrl("bad.host", candidate -> {
            throw new IOException("connection refused");
        });
        assertNull(result);
    }

    @Test
    public void testExplicitHttps_noRetry() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);
        String result = SignInAdvancedDialog.resolveServerUrl("https://symposium.ndexbio.org", candidate -> {
            calls.incrementAndGet();
            return true;
        });
        assertEquals("https://symposium.ndexbio.org", result);
        assertEquals(1, calls.get());
    }

    @Test
    public void testExplicitHttp_noRetry() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);
        String result = SignInAdvancedDialog.resolveServerUrl("http://symposium.ndexbio.org", candidate -> {
            calls.incrementAndGet();
            return true;
        });
        assertEquals("http://symposium.ndexbio.org", result);
        assertEquals(1, calls.get());
    }

    @Test
    public void testLocalhostPort_firstCandidateSucceeds() throws Exception {
        // localhost:8080 must NOT be treated as having a scheme — two candidates should be tried
        AtomicInteger calls = new AtomicInteger(0);
        String result = SignInAdvancedDialog.resolveServerUrl("localhost:8080", candidate -> {
            calls.incrementAndGet();
            return true;
        });
        assertEquals("http://localhost:8080", result);
        assertEquals(1, calls.get());
    }

    @Test
    public void testLocalhostPort_fallsBackToHttps() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);
        String result = SignInAdvancedDialog.resolveServerUrl("localhost:8080", candidate -> {
            calls.incrementAndGet();
            if (candidate.toLowerCase().startsWith("https://")) return true;
            throw new IOException("connection refused");
        });
        assertEquals("https://localhost:8080", result);
        assertEquals(2, calls.get());
    }

    @Test
    public void testIpPort_firstCandidateSucceeds() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);
        String result = SignInAdvancedDialog.resolveServerUrl("127.0.0.1:9999", candidate -> {
            calls.incrementAndGet();
            return true;
        });
        assertEquals("http://127.0.0.1:9999", result);
        assertEquals(1, calls.get());
    }

    @Test
    public void testNull_returnsNull() throws Exception {
        assertNull(SignInAdvancedDialog.resolveServerUrl(null, candidate -> true));
    }

    @Test
    public void testEmpty_returnsNull() throws Exception {
        assertNull(SignInAdvancedDialog.resolveServerUrl("", candidate -> true));
    }

    @Test
    public void testFtpScheme_returnsNull() throws Exception {
        // Non-http scheme must be rejected without producing malformed candidates
        assertNull(SignInAdvancedDialog.resolveServerUrl("ftp://example.com", candidate -> true));
    }

    @Test
    public void testExplicitHttps_directInput_noRetry() throws Exception {
        // Explicit https:// is treated as single candidate — no fallback attempted
        AtomicInteger calls = new AtomicInteger(0);
        String result = SignInAdvancedDialog.resolveServerUrl("https://ndex.example.com", candidate -> {
            calls.incrementAndGet();
            return true;
        });
        assertEquals("https://ndex.example.com", result);
        assertEquals(1, calls.get());
    }

    @Test
    public void testCustomSubpath_explicit_http_noRetry() throws Exception {
        // URL with explicit http and custom path is treated as single candidate
        AtomicInteger calls = new AtomicInteger(0);
        String result = SignInAdvancedDialog.resolveServerUrl("http://host/mypath", candidate -> {
            calls.incrementAndGet();
            return true;
        });
        assertEquals("http://host/mypath", result);
        assertEquals(1, calls.get());
    }
}
