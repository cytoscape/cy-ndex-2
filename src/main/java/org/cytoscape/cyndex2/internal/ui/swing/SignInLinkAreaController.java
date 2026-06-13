package org.cytoscape.cyndex2.internal.ui.swing;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Window;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import org.cytoscape.cyndex2.internal.rest.NdexAdminStatusService;
import org.cytoscape.cyndex2.internal.rest.NdexV3AdminStatus;

class SignInLinkAreaController {

    private final JLabel forgotPasswordLabel;
    private final JLabel signUpLabel;
    private final JLabel needAccountLabel;
    private final JLabel needAccountLabel1;
    private final NdexAdminStatusService service;

    private String registerUrl;
    private String resetUrl;
    private String swaggerUrl;

    SignInLinkAreaController(
            JLabel forgotPasswordLabel,
            JLabel signUpLabel,
            JLabel needAccountLabel,
            JLabel needAccountLabel1,
            NdexAdminStatusService service) {
        this.forgotPasswordLabel = forgotPasswordLabel;
        this.signUpLabel = signUpLabel;
        this.needAccountLabel = needAccountLabel;
        this.needAccountLabel1 = needAccountLabel1;
        this.service = service;
    }

    String getRegisterUrl() { return registerUrl; }
    String getResetUrl()     { return resetUrl; }
    String getSwaggerUrl()   { return swaggerUrl; }

    void setDefaultInstructions() {
        registerUrl = null;
        resetUrl = null;
        swaggerUrl = null;
        needAccountLabel1.setVisible(false);
        needAccountLabel.setVisible(false);
        forgotPasswordLabel.setText("<html><body style='width:260px'>"
                + "To register or reset your password, visit the <b>My Account</b> page "
                + "on your NDEx server's website."
                + "</body></html>");
        forgotPasswordLabel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        forgotPasswordLabel.setForeground(UIManager.getColor("Label.foreground"));
        forgotPasswordLabel.setVisible(true);
        signUpLabel.setText("<html><body style='width:260px'>"
                + "Alternatively, can use the REST API directly to change password "
                + "or create new user. Refer to the swagger docs published at url "
                + "path of <tt>/swagger/index.html</tt> on the NDEx server for the "
                + "<b>V2 - user</b> section which has details for endpoints."
                + "</body></html>");
        signUpLabel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        signUpLabel.setForeground(UIManager.getColor("Label.foreground"));
        signUpLabel.setVisible(true);
        repack();
    }

    void processStatus(NdexV3AdminStatus status) {
        if (status != null && status.hasValidOAuthUrls()) {
            registerUrl = status.getOauthRegisterUrl();
            resetUrl = status.getOauthResetUrl();
            needAccountLabel1.setVisible(true);
            needAccountLabel.setVisible(true);
            forgotPasswordLabel.setText("Click here to reset it in browser");
            forgotPasswordLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            forgotPasswordLabel.setForeground(new Color(51, 122, 183));
            signUpLabel.setText("Click here to Sign Up in browser");
            signUpLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            signUpLabel.setForeground(new Color(51, 122, 183));
            forgotPasswordLabel.setVisible(true);
            signUpLabel.setVisible(true);
            repack();
        }
        // else: default instructions already visible — nothing to do
    }

    void refresh(String serverUrl) {
        setDefaultInstructions();
        new SwingWorker<NdexV3AdminStatus, Void>() {
            @Override
            protected NdexV3AdminStatus doInBackground() {
                return service.fetch(serverUrl);
            }
            @Override
            protected void done() {
                NdexV3AdminStatus status = null;
                try {
                    status = get();
                } catch (InterruptedException | ExecutionException e) {
                    Logger.getLogger(SignInLinkAreaController.class.getName())
                            .log(Level.WARNING, "fetchV3AdminStatus interrupted", e);
                }
                processStatus(status);
            }
        }.execute();
    }

    private void repack() {
        Window w = SwingUtilities.getWindowAncestor(forgotPasswordLabel);
        if (w != null) w.pack();
    }
}
