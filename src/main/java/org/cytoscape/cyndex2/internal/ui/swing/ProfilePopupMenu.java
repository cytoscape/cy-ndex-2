package org.cytoscape.cyndex2.internal.ui.swing;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.cytoscape.cyndex2.internal.util.IconUtil;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.cyndex2.internal.util.ServerKey;
import org.cytoscape.cyndex2.internal.util.ServerList;
import org.cytoscape.cyndex2.internal.util.ServerManager;
import org.cytoscape.cyndex2.internal.util.ServerUtil;
import org.cytoscape.util.swing.TextIcon;

public class ProfilePopupMenu extends JPopupMenu {

	static final Font font = IconUtil.getAppFont(23f);
	static int iconSize = 24;
	
	static final Icon ADD_PROFILE_ICON = new TextIcon(IconUtil.ICON_NDEX_ACCOUNT_PLUS, font, iconSize, iconSize);
	static final Icon REMOVE_PROFILE_ICON = new TextIcon(IconUtil.ICON_NDEX_ACCOUNT_MINUS, font, iconSize, iconSize);
 	static final Icon PROFILE_ICON = new TextIcon(IconUtil.ICON_NDEX_ACCOUNT, font, iconSize, iconSize);
	
 	final JDialog parent;
 	
	public ProfilePopupMenu(JDialog frame) {
		this.parent = frame;
	}

	public void show(Component invoker, int x, int y) {
		while (getSubElements().length > 0) {
			remove(0);
		}

		final ServerList serverList = ServerManager.INSTANCE.getAvailableServers();
		final Server selectedServer = ServerManager.INSTANCE.getSelectedServer();

		if (selectedServer == null) {

		} else {
			if ( selectedServer.getUsername() != null) {
				add(new JMenuItem(new AbstractAction(" Log out of Current Profile") {

					@Override
					public void actionPerformed(ActionEvent e) {
						ServerKey key = new ServerKey(null, selectedServer.getUrl());
						Server anonymousConn = ServerManager.INSTANCE.getAvailableServers().getServer(key);
						if (anonymousConn != null) {
							ServerManager.INSTANCE.setSelectedServer(key);
						} else {
							try {
								ServerManager.INSTANCE.addServer(null, null, selectedServer.getUrl());
							} catch (Exception e1) {
								Logger.getLogger(ProfilePopupMenu.class.getName()).log(Level.WARNING, "Failed to add anonymous server", e1);
							}
						}
					}
				}));
				addSeparator();
			}
			add(new JMenuItem(new AbstractAction("Remove Current Profile", REMOVE_PROFILE_ICON) {
				@Override
				public void actionPerformed(ActionEvent e) {
					ServerManager.INSTANCE.removeServer(selectedServer);
				}
			}));
			addSeparator();
		}

		final List<Server> serverItems = serverList.stream().filter(server -> !server.equals(selectedServer))
				.collect(Collectors.toList());

		if (serverItems.size() > 0) {
			serverItems.stream().forEach(server -> {
				
				final JMenuItem jMenuItem = new JMenuItem(new AbstractAction(
						"<HTML>" + "<b>" + ServerUtil.getDisplayUsernameHTML(server.getUsername()) + "</b>" + "<br>" + server.getUrl() + "</HTML>", PROFILE_ICON) {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						ServerManager.INSTANCE.setSelectedServer(new ServerKey(server));
					}
				});
				add(jMenuItem);
			});

			addSeparator();
		}

		add(new JMenuItem(new AbstractAction("Add new profile...", ADD_PROFILE_ICON) {
			public void actionPerformed(ActionEvent e) {
				SignInDialog signInDialog = new SignInDialog(parent);
				signInDialog.setLocationRelativeTo(parent);
				signInDialog.setVisible(true);
			}
		}));

		super.show(invoker, x, y);
	}

}
