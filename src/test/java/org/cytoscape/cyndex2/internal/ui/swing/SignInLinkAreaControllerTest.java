package org.cytoscape.cyndex2.internal.ui.swing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Cursor;

import javax.swing.JLabel;

import org.cytoscape.cyndex2.internal.rest.NdexV3AdminStatus;
import org.junit.Before;
import org.junit.Test;

public class SignInLinkAreaControllerTest {

    private JLabel forgotPasswordLabel;
    private JLabel signUpLabel;
    private JLabel needAccountLabel;
    private JLabel needAccountLabel1;
    private SignInLinkAreaController ctrl;

    @Before
    public void setUp() {
        forgotPasswordLabel = new JLabel();
        signUpLabel = new JLabel();
        needAccountLabel = new JLabel();
        needAccountLabel1 = new JLabel();
        ctrl = new SignInLinkAreaController(
                forgotPasswordLabel, signUpLabel, needAccountLabel, needAccountLabel1,
                url -> null); // service irrelevant for direct method tests
    }

    private static NdexV3AdminStatus makeOAuth(String register, String reset) {
        NdexV3AdminStatus s = new NdexV3AdminStatus();
        s.setOauthRegisterUrl(register);
        s.setOauthResetUrl(reset);
        return s;
    }

    @Test
    public void defaultInstructions_setImmediately() {
        ctrl.setDefaultInstructions();

        assertTrue(forgotPasswordLabel.getText().contains("My Account"));
        assertTrue(signUpLabel.getText().contains("swagger"));
        assertTrue(forgotPasswordLabel.isVisible());
        assertTrue(signUpLabel.isVisible());
        assertFalse(needAccountLabel.isVisible());
        assertFalse(needAccountLabel1.isVisible());
        assertEquals(Cursor.DEFAULT_CURSOR, forgotPasswordLabel.getCursor().getType());
        assertNull(ctrl.getRegisterUrl());
        assertNull(ctrl.getResetUrl());
    }

    @Test
    public void processStatus_null_noChange() {
        ctrl.setDefaultInstructions();
        ctrl.processStatus(null);

        assertTrue(forgotPasswordLabel.getText().contains("My Account"));
        assertNull(ctrl.getRegisterUrl());
        assertNull(ctrl.getResetUrl());
    }

    @Test
    public void processStatus_invalidOAuth_noChange() {
        ctrl.setDefaultInstructions();
        ctrl.processStatus(new NdexV3AdminStatus()); // no URLs set

        assertTrue(forgotPasswordLabel.getText().contains("My Account"));
        assertNull(ctrl.getRegisterUrl());
    }

    @Test
    public void processStatus_validOAuth_updatesLabels() {
        ctrl.setDefaultInstructions();
        ctrl.processStatus(makeOAuth("https://auth.example.org/register", "https://auth.example.org/reset"));

        assertEquals("https://auth.example.org/register", ctrl.getRegisterUrl());
        assertEquals("https://auth.example.org/reset", ctrl.getResetUrl());
        assertTrue(forgotPasswordLabel.getText().contains("Click here"));
        assertTrue(signUpLabel.getText().contains("Click here"));
        assertEquals(Cursor.HAND_CURSOR, forgotPasswordLabel.getCursor().getType());
        assertTrue(forgotPasswordLabel.isVisible());
        assertTrue(signUpLabel.isVisible());
        assertTrue(needAccountLabel.isVisible());
        assertTrue(needAccountLabel1.isVisible());
    }

    @Test
    public void processStatus_partialOAuth_noChange() {
        ctrl.setDefaultInstructions();
        // Only register URL set — hasValidOAuthUrls() returns false
        ctrl.processStatus(makeOAuth("https://auth.example.org/register", null));

        assertTrue(forgotPasswordLabel.getText().contains("My Account"));
        assertNull(ctrl.getRegisterUrl());
    }
}
