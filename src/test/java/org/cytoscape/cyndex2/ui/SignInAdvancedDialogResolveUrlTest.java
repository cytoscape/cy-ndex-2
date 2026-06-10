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
        assertEquals("symposium.ndexbio.org", result);
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
}
