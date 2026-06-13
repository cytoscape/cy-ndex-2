/*
 * Copyright (c) 2014, the Cytoscape Consortium and the Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.cytoscape.cyndex2.internal.ui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.cyndex2.internal.rest.parameter.LoadParameters;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExImportParameters;
import org.cytoscape.cyndex2.internal.util.CxPreferences;
import org.cytoscape.cyndex2.internal.util.ErrorMessage;
import org.cytoscape.cyndex2.internal.util.IconUtil;
import org.cytoscape.cyndex2.internal.util.Server;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cytoscape.cyndex2.internal.util.ServerManager;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.TextIcon;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author David
 * @author David Otasek
 */
public class FindNetworksDialog extends javax.swing.JDialog implements PropertyChangeListener {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;
	private List<NetworkSummary> networkSummaries;

	private static final int SEARCH_MAX_RESULTS = 200;

	static final Font font = IconUtil.getAppFont(32f);
	static final int ICON_SIZE = 32;
	static final Icon NDEX_ICON = new TextIcon(IconUtil.ICON_NDEX_LOGO, font, IconUtil.ICON_COLOR_1, 32, 32);

	final LoadParameters loadParameters;

	/**
	 * Creates new form SimpleSearch
	 */
	public FindNetworksDialog(Frame parent, LoadParameters loadParameters) {
		super(parent, false);
		ServerManager.INSTANCE.addPropertyChangeListener(this);
		this.loadParameters = loadParameters;
		initComponents();
		prepComponents();
	}

	private static FindNetworksDialog loadDialog = null;

	public static void getFindNetworksDialog(LoadParameters loadParameters) {
		if (loadDialog != null && loadDialog.isVisible()) {
			loadDialog.setVisible(false);
		}
		loadDialog = new FindNetworksDialog(null, loadParameters);
		final JFrame parentFrame = CyServiceModule.INSTANCE.getSwingApplication().getJFrame();
		loadDialog.setLocationRelativeTo(parentFrame);
		loadDialog.setVisible(true);
	}

	private TextIcon getSearchIcon() {
		final IconManager iconManager = CyServiceModule.INSTANCE.getService(IconManager.class);
		final TextIcon searchIcon = new TextIcon(iconManager.ICON_SEARCH, iconManager.getIconFont(24), 24, 24);
		return searchIcon;
	}

	private void setSearchEnabled(final boolean enabled) {
		final String tooltip = enabled ? "Search NDEx for networks by text" : "To enable searching, make sure the 'My Networks' checkbox is unselected.";
		
		searchField.setEnabled(enabled);
		searchField.setToolTipText(tooltip);
		
		search.setEnabled(enabled);
		search.setToolTipText(tooltip);
	}
	
	private void load(final NetworkSummary networkSummary) {
		final long networkSize = networkSummary.getNodeCount() + networkSummary.getEdgeCount();
		final long viewThreshold = CxPreferences.getViewThreshold();
		final boolean importNetwork;
		final boolean createView;

		if (networkSize > viewThreshold) {
			LargeNetworkDialog viewCreationDialog = new LargeNetworkDialog(this, true, networkSummary);

			viewCreationDialog.setLocationRelativeTo(this);
			viewCreationDialog.setVisible(true);

			importNetwork = viewCreationDialog.getImportNetwork();
			createView = viewCreationDialog.getCreateView();
		} else {
			importNetwork = true;
			createView = true;
		}

		if (importNetwork) {
			ModalProgressHelper.runWorker(this, "Loading Network", () -> {
				final Server selectedServer = ServerManager.INSTANCE.getServer();

				boolean success;
				try {
					final NdexRestClientModelAccessLayer mal = selectedServer.getModelAccessLayer();
					success = selectedServer.check(mal);
				} catch (IOException | NdexException e1) {
					Logger.getLogger(FindNetworksDialog.class.getName()).log(Level.WARNING, "Server check failed", e1);
					success = false;
				}
				if (success) {
					// The network to copy from.
//                        NetworkSummary networkSummary = NetworkManager.INSTANCE.getSelectedNetworkSummary();
					UUID uuid = networkSummary.getExternalId();
					// System.out.println("NetworkSummary external ID: " + (uuid == null ? null :
					// uuid.toString()));
					try {
						// ProvenanceEntity provenance = mal.getNetworkProvenance(id.toString());

						String REST_URI = "http://localhost:" + CyActivator.getCyRESTPort() + "/cyndex2/v1/networks";
						HttpClient httpClient = HttpClients.createDefault();
						final URI uri = URI.create(REST_URI);
						final HttpPost post = new HttpPost(uri.toString());
						post.setHeader("Content-type", "application/json");

						NDExImportParameters importParameters = new NDExImportParameters(uuid.toString(),
								selectedServer.getUsername(), selectedServer.getPassword(), selectedServer.getUrl(),
								null, null, createView);
						ObjectMapper objectMapper = new ObjectMapper();

						post.setEntity(new StringEntity(objectMapper.writeValueAsString(importParameters)));

						httpClient.execute(post);

					}

					/*
					 * catch (IOException ex) { JOptionPane.showMessageDialog(me,
					 * ErrorMessage.failedToParseJson, "Error", JOptionPane.ERROR_MESSAGE); return
					 * -1; }
					 */
					catch (RuntimeException ex2) {
						Logger.getLogger(FindNetworksDialog.class.getName()).log(Level.WARNING, "Network import failed", ex2);
					JOptionPane.showMessageDialog(null,
								"This network can't be imported to cytoscape. Cause: " + ex2.getMessage(), "Error",
								JOptionPane.ERROR_MESSAGE);
						return -1;
					} catch (IOException e) {
						Logger.getLogger(FindNetworksDialog.class.getName()).log(Level.WARNING, "Network import HTTP request failed", e);
					}
				} else {
					JOptionPane.showMessageDialog(null, ErrorMessage.failedServerCommunication, "Error",
							JOptionPane.ERROR_MESSAGE);
					return -1;
				}
				return 1;
			});
		}
	}

	public void setFocusOnDone() {
		this.getRootPane().setDefaultButton(done);
		done.requestFocus();
	}

	private void prepComponents() {
		this.getRootPane().setDefaultButton(search);

		searchField.setText(loadParameters.searchTerm);
		setSearchEnabled(!loadParameters.userNetworksOnly);

		final Server selectedServer = ServerManager.INSTANCE.getServer();
		NdexRestClientModelAccessLayer mal;
		String pendingError = null;

		try {
			mal = selectedServer.getModelAccessLayer();
		} catch (IOException | NdexException e) {
			mal = null;
			Logger.getLogger(FindNetworksDialog.class.getName()).log(Level.WARNING, "Failed to get model access layer", e);
			pendingError = ErrorMessage.failedServerCommunication + "\n\nError Message: " + e.getMessage();
		}
		if (selectedServer.getUsername() != null && !selectedServer.getUsername().isEmpty()) {
			administeredByMe.setVisible(true);
		} else {
			if (selectedServer.getUsername() != null) {
				try {
					selectedServer.check(mal);
					administeredByMe.setVisible(true);
				} catch (IOException e) {
					Logger.getLogger(FindNetworksDialog.class.getName()).log(Level.WARNING, "Server check failed", e);
					if (pendingError == null)
						pendingError = ErrorMessage.failedServerCommunication + "\n\nError Message: " + e.getMessage();
				}
			} else {
				administeredByMe.setVisible(false);
			}
		}

		try {
			if (selectedServer.check(mal)) {
				try {
					if (loadParameters.userNetworksOnly) {
						networkSummaries = mal != null ? mal.getMyNetworks() : List.of();
					} else {
						networkSummaries = mal != null
								? mal.findNetworks(loadParameters.searchTerm, null, null, true, 0, SEARCH_MAX_RESULTS).getNetworks()
								: List.of();
					}
				} catch (IOException | NdexException ex) {
					Logger.getLogger(FindNetworksDialog.class.getName()).log(Level.WARNING, "Failed to fetch networks", ex);
					if (pendingError == null)
						pendingError = ErrorMessage.failedServerCommunication + "\n\nError Message: " + ex.getMessage();
					networkSummaries = List.of();
				}
			} else {
				if (pendingError == null)
					pendingError = ErrorMessage.failedServerCommunication;
				networkSummaries = List.of();
			}
		} catch (HeadlessException | IOException e) {
			Logger.getLogger(FindNetworksDialog.class.getName()).log(Level.WARNING, "prepComponents failed", e);
			if (pendingError == null)
				pendingError = ErrorMessage.failedServerCommunication + "\n\nError Message: " + e.getMessage();
			networkSummaries = List.of();
		}

		showSearchResults();

		if (pendingError != null) {
			final String msg = pendingError;
			javax.swing.SwingUtilities.invokeLater(() -> {
				if (FindNetworksDialog.this.isDisplayable())
					JOptionPane.showMessageDialog(FindNetworksDialog.this, msg, "Error",
							JOptionPane.ERROR_MESSAGE);
			});
		}
	}

	private JTable getResultsTable() {
		return new JTable() {

			public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
				final Component comp = super.prepareRenderer(renderer, row, col);
				final JComponent jcomp = (JComponent) comp;
				if (comp == jcomp) {
					if (col != 0) {
						if (row < super.getRowCount() && 0 < super.getColumnCount()) {
							try {
								final NetworkSummary networkSummary = (NetworkSummary) getValueAt(row, 0);

								final String toolTip = networkSummary.getDescription() == null ? null
										: "<html>" + networkSummary.getDescription() + "</html>";
								jcomp.setToolTipText(toolTip);
							} catch (Exception e) {
								// ignored — tooltip is best-effort
							}
						}
					}
				}
				return comp;
			}
		};
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */

	// <editor-fold defaultstate="collapsed" desc="Generated
	// <editor-fold defaultstate="collapsed" desc="Generated
	// <editor-fold defaultstate="collapsed" desc="Generated
	// <editor-fold defaultstate="collapsed" desc="Generated
	// <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        resultsTable = getResultsTable();
        done = new javax.swing.JButton();
        search = new javax.swing.JButton(getSearchIcon());
        searchField = new javax.swing.JTextField();
        administeredByMe = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jButton1 = SignInButtonHelper.createSignInButton(this);
        ndexLogo = new javax.swing.JLabel("NDEx", NDEX_ICON, SwingConstants.LEFT);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Find Networks");

        jScrollPane2.setBorder(null);

        resultsTable.setAutoCreateRowSorter(true);
        resultsTable.setIntercellSpacing(new java.awt.Dimension(6, 2));
        resultsTable.setRowHeight(24);
        resultsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(resultsTable);

        done.setText("Close Dialog");
        done.setToolTipText("Click to close this dialog when done downloading");
        done.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doneActionPerformed(evt);
            }
        });

        search.setMargin(new java.awt.Insets(2, 2, 2, 2));
        search.setEnabled(!loadParameters.userNetworksOnly);
        search.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchActionPerformed(evt);
            }
        });

        searchField.setEnabled(!loadParameters.userNetworksOnly);

        administeredByMe.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        administeredByMe.setText("My Networks");
        administeredByMe.setToolTipText("Enable this to view the networks in your account");
        administeredByMe.setSelected(loadParameters.userNetworksOnly);
        administeredByMe.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                administeredByMeActionPerformed(evt);
            }
        });

        jLabel1.setText("Results");

        jLabel4.setText("WARNING: In some cases, not all network information stored in NDEx will be available within Cytoscape after loading.");

        jButton1.setText(SignInButtonHelper.getSignInText());
        jButton1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jButton1.setMaximumSize(new java.awt.Dimension(200, 30));
        jButton1.setMinimumSize(new java.awt.Dimension(48, 30));

        ndexLogo.setFont(new java.awt.Font("Ubuntu", 0, 24)); // NOI18N
        ndexLogo.setText("NDEx");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jLabel4)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(0, 856, Short.MAX_VALUE))
                            .addComponent(jScrollPane1)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(searchField)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(search, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(ndexLogo, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(done, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap())))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(administeredByMe)
                .addGap(398, 398, 398))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ndexLogo))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(search, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(searchField))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(administeredByMe)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 287, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(done)
                .addContainerGap())
        );

        jScrollPane2.setViewportView(jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void getMyNetworks() {
		final Server selectedServer = ServerManager.INSTANCE.getSelectedServer();
		try {
			NdexRestClientModelAccessLayer mal = selectedServer.getModelAccessLayer();

			if (selectedServer.check(mal)) {
				try {
					networkSummaries = mal.getMyNetworks();
					javax.swing.SwingUtilities.invokeLater(this::showSearchResults);
				} catch (IOException | NdexException ex) {
					networkSummaries = List.of();
					javax.swing.SwingUtilities.invokeLater(() -> {
						showSearchResults();
						JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					});
					return;
				}
			} else {
				networkSummaries = List.of();
				javax.swing.SwingUtilities.invokeLater(() -> {
					showSearchResults();
					JOptionPane.showMessageDialog(this, ErrorMessage.failedServerCommunication, "Error",
							JOptionPane.ERROR_MESSAGE);
				});
			}
		} catch (HeadlessException | IOException | NdexException e) {
			Logger.getLogger(FindNetworksDialog.class.getName()).log(Level.WARNING, "getMyNetworks failed", e);
			networkSummaries = List.of();
			javax.swing.SwingUtilities.invokeLater(() -> {
				showSearchResults();
				JOptionPane.showMessageDialog(this, ErrorMessage.failedServerCommunication, "Error",
						JOptionPane.ERROR_MESSAGE);
			});
		} catch (Exception e) {
			Logger.getLogger(FindNetworksDialog.class.getName()).log(Level.WARNING, "getMyNetworks unexpected exception", e);
			networkSummaries = List.of();
			javax.swing.SwingUtilities.invokeLater(this::showSearchResults);
		}
	}

	private void doneActionPerformed(java.awt.event.ActionEvent evt) {
		this.setVisible(false);
	}

	private void search() {

		Server selectedServer = ServerManager.INSTANCE.getServer();

		String searchText = searchField.getText();
		if (searchText.isEmpty())
			searchText = "";

		try {
			NdexRestClientModelAccessLayer mal = selectedServer.getModelAccessLayer();

			if (selectedServer.check(mal)) {
				try {
					if (administeredByMe.isSelected()) {
						networkSummaries = mal.getMyNetworks();
					} else {
						networkSummaries = mal.findNetworks(searchText, null, null, true, 0, SEARCH_MAX_RESULTS).getNetworks();
					}
				} catch (IOException | NdexException ex) {
					networkSummaries = List.of();
					javax.swing.SwingUtilities.invokeLater(() -> {
						showSearchResults();
						JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					});
					return;
				}
				javax.swing.SwingUtilities.invokeLater(this::showSearchResults);
			} else {
				networkSummaries = List.of();
				javax.swing.SwingUtilities.invokeLater(() -> {
					showSearchResults();
					JOptionPane.showMessageDialog(this, ErrorMessage.failedServerCommunication, "Error",
							JOptionPane.ERROR_MESSAGE);
				});
			}
		} catch (HeadlessException | IOException | NdexException e) {
			Logger.getLogger(FindNetworksDialog.class.getName()).log(Level.WARNING, "search failed", e);
			networkSummaries = List.of();
			javax.swing.SwingUtilities.invokeLater(() -> {
				showSearchResults();
				JOptionPane.showMessageDialog(this, ErrorMessage.failedServerCommunication, "Error",
						JOptionPane.ERROR_MESSAGE);
			});
		} catch (Exception e) {
			Logger.getLogger(FindNetworksDialog.class.getName()).log(Level.WARNING, "search unexpected exception", e);
			networkSummaries = List.of();
			javax.swing.SwingUtilities.invokeLater(this::showSearchResults);
		}
	}

	private void searchActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_searchActionPerformed
	{// GEN-HEADEREND:event_searchActionPerformed
		ModalProgressHelper.runWorker(this, "Searching", () -> {
			search();
			return 1;
		});
	}

	private void administeredByMeActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_administeredByMeActionPerformed
	{// GEN-HEADEREND:event_administeredByMeActionPerformed
		ModalProgressHelper.runWorker(this, "Updating Networks", () -> {
			if (administeredByMe.isSelected()) {
				getMyNetworks();
			} else {
				search();
			}
			
			setSearchEnabled(!administeredByMe.isSelected());
			return 1;
		});
	}// GEN-LAST:event_administeredByMeActionPerformed

	private List<NetworkSummary> displayedNetworkSummaries = new ArrayList<>();

	private void showSearchResults() {
		NetworkSummaryTableModel model = new NetworkSummaryTableModel(networkSummaries, this::load);
		displayedNetworkSummaries.clear();
		for (NetworkSummary networkSummary : networkSummaries) {
			displayedNetworkSummaries.add(networkSummary);
		}
		resultsTable.setModel(model);
		resultsTable.getColumnModel().getColumn(NetworkSummaryTableModel.IMPORT_COL).setPreferredWidth(24);
		resultsTable.getColumnModel().getColumn(NetworkSummaryTableModel.NAME_COL).setPreferredWidth(200);
		resultsTable.getColumnModel().getColumn(NetworkSummaryTableModel.VISIBILITY_COL).setPreferredWidth(20);
		resultsTable.getColumnModel().getColumn(NetworkSummaryTableModel.OWNER_COL).setPreferredWidth(40);
		resultsTable.getColumnModel().getColumn(NetworkSummaryTableModel.NODES_COL).setPreferredWidth(20);
		resultsTable.getColumnModel().getColumn(NetworkSummaryTableModel.EDGES_COL).setPreferredWidth(20);
		resultsTable.setDefaultRenderer(NetworkSummary.class, new NetworkSummaryTableModel.ImportButtonRenderer());
		resultsTable.setDefaultRenderer(VisibilityType.class, new NetworkSummaryTableModel.VisibilityTypeRenderer());
		resultsTable.setDefaultRenderer(Timestamp.class, new NetworkSummaryTableModel.TimestampRenderer());
		resultsTable.setShowGrid(false);
		resultsTable.setDefaultEditor(NetworkSummary.class, model.new ImportButtonEditor(new JCheckBox()));
		resultsTable.getSelectionModel().setSelectionInterval(0, 0);

		resultsTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					load(networkSummaries.get(resultsTable.getSelectedRow()));
				}
			}
		});

		resultsTable.getTableHeader().setPreferredSize(new Dimension(resultsTable.getPreferredSize().width, 24));
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		/* Set the Nimbus look and feel */
		// <editor-fold defaultstate="collapsed" desc=" Look and feel setting code
		// (optional) ">
		/*
		 * If Nimbus (introduced in Java SE 6) is not available, stay with the default
		 * look and feel. For details see
		 * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
		 */
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(FindNetworksDialog.class.getName()).log(java.util.logging.Level.SEVERE,
					null, ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(FindNetworksDialog.class.getName()).log(java.util.logging.Level.SEVERE,
					null, ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(FindNetworksDialog.class.getName()).log(java.util.logging.Level.SEVERE,
					null, ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(FindNetworksDialog.class.getName()).log(java.util.logging.Level.SEVERE,
					null, ex);
		}
		// </editor-fold>

		/* Create and display the dialog */
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				FindNetworksDialog dialog = new FindNetworksDialog(new javax.swing.JFrame(), null);
				dialog.addWindowListener(new java.awt.event.WindowAdapter() {
					@Override
					public void windowClosing(java.awt.event.WindowEvent e) {
						System.exit(0);
					}
				});
				dialog.setVisible(true);
			}
		});
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox administeredByMe;
    private javax.swing.JButton done;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel ndexLogo;
    private javax.swing.JTable resultsTable;
    private javax.swing.JButton search;
    private javax.swing.JTextField searchField;
    // End of variables declaration//GEN-END:variables

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (isVisible()) {
			ModalProgressHelper.runWorker(this, "Loading Profile", () -> {
				jButton1.setText(SignInButtonHelper.getSignInText());
				Server selectedServer = ServerManager.INSTANCE.getServer();
				if (administeredByMe.isSelected()) {
					administeredByMe.setSelected(
							selectedServer.getUsername() != null && !selectedServer.getUsername().isEmpty());
				}
				administeredByMe
						.setVisible(selectedServer.getUsername() != null && !selectedServer.getUsername().isEmpty());
				this.setSearchEnabled(!administeredByMe.isVisible() || !administeredByMe.isSelected());
				search();
				return 1;
			});
		}
	}
}
