package org.cytoscape.cyndex2.internal.ui.swing;

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.cytoscape.cyndex2.internal.util.IconUtil;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.cyndex2.internal.util.ServerManager;
import org.cytoscape.cyndex2.internal.util.ServerUtil;
import org.cytoscape.util.swing.TextIcon;

public class SignInButtonHelper {

	public static JButton createSignInButton(JDialog frame, JComponent parentComponent){
		 //Create the popup menu.
        final JPopupMenu popup = new ProfilePopupMenu(frame, parentComponent);
      	Font font = IconUtil.getAppFont(23f);
    		int iconSize = 24;
      	Icon icon = new TextIcon(IconUtil.ICON_NDEX_ACCOUNT, font, iconSize, iconSize);
        
        JButton signInButton = new JButton();
        signInButton.setIcon(icon);
        signInButton.setText(getSignInText());
       
        signInButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        return signInButton;
	}
	
    public static JButton createSignInButton(JDialog frame) {
       return createSignInButton(frame, null);
    }

    public static String getSignInText() {
    	final Server selectedServer = ServerManager.INSTANCE.getSelectedServer();
    	
    	if (selectedServer == null) {
    		return "Sign in";
    	} else {
    		return "<html>" + ServerUtil.getDisplayUsernameHTML(selectedServer.getUsername()) + "@" + selectedServer.getUrl() + " &#9662;</html>";
    	}
    }
}
