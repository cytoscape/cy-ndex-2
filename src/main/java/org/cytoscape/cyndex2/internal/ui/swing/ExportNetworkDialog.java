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

import java.awt.Container;
import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import static org.cytoscape.util.swing.IconManager.ICON_COG;

import org.cytoscape.cyndex2.external.SaveParameters;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.cyndex2.internal.rest.SimpleNetworkSummary;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExSaveParameters;
import org.cytoscape.cyndex2.internal.rest.response.SummaryResponse;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.cyndex2.internal.util.ServerManager;
import org.cytoscape.cyndex2.internal.util.UpdateUtil;
import org.cytoscape.cyndex2.internal.util.UserAgentUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.util.swing.IconManager;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author David
 */
public class ExportNetworkDialog extends javax.swing.JDialog implements PropertyChangeListener {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	final SaveParameters saveParameters;

	private boolean isUUIDChanged = false;

	final int ICON_FONT_SIZE = 22;

	/**
	 * Creates new form ExportNetwork
	 */
	public ExportNetworkDialog(Frame parent, SaveParameters saveParameters) {
		super(parent, true);
		this.saveParameters = saveParameters;
		ServerManager.INSTANCE.addPropertyChangeListener(this);
		initComponents();
		prepComponents();
	}

	private SimpleNetworkSummary getCurrentCollectionSummary(SummaryResponse summaryResponse) {
		return summaryResponse.currentRootNetwork;
	}

	private SimpleNetworkSummary getCurrentNetworkSummary(SummaryResponse summaryResponse) {

		for (SimpleNetworkSummary networkSummary : summaryResponse.members) {
			if (networkSummary.suid.longValue() == summaryResponse.currentNetworkSuid.longValue()) {
				return networkSummary;
			}
		}
		return null;
	}

	private boolean isCollection() {
		return "collection".equals(saveParameters.saveType);
	}

	private String getEquivalentKey(String key, Map<String, Object> map) {
		final String upperCaseKey = key.toUpperCase();
		final String equivalentKey = map.keySet().stream().reduce((String) null, (retVal,
				current) -> retVal != null ? retVal : current.toUpperCase().equals(upperCaseKey) ? current : null);
		return equivalentKey;
	}

	private void prepComponents() {
		final boolean isCollection = isCollection();
		setModal(true);

		rootPane.setDefaultButton(exportButton);

		String REST_URI = "http://localhost:" + CyActivator.getCyRESTPort() + "/cyndex2/v1/networks/"
				+ saveParameters.suid;
		HttpClient httpClient = HttpClients.createDefault();
		final URI uri = URI.create(REST_URI);
		final HttpGet get = new HttpGet(uri.toString());
		get.setHeader("Content-type", "application/json");
		HttpResponse response;
		try {
			response = httpClient.execute(get);

			final HttpEntity entity = response.getEntity();
			final String result = EntityUtils.toString(entity);

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readValue(result, JsonNode.class);
			// System.out.println(jsonNode);

			SummaryResponse summaryResponse = objectMapper.treeToValue(jsonNode.get("data"), SummaryResponse.class);

			// CyNetwork cyNetwork = CyObjectManager.INSTANCE.getCurrentNetwork();
			updateUpdateButton();

			authorField.setEnabled(!isCollection);
			organismField.setEnabled(!isCollection);
			diseaseField.setEnabled(!isCollection);
			tissueField.setEnabled(!isCollection);
			rightsHolderField.setEnabled(!isCollection);
			versionField.setEnabled(!isCollection);
			referenceField.setEnabled(!isCollection);
			descriptionTextArea.setEnabled(!isCollection);

			final SimpleNetworkSummary summary = isCollection ? getCurrentCollectionSummary(summaryResponse)
					: getCurrentNetworkSummary(summaryResponse);

			final String authorKey = getEquivalentKey("author", summary.props);
			final String organismKey = getEquivalentKey("organism", summary.props);
			final String diseaseKey = getEquivalentKey("disease", summary.props);
			final String tissueKey = getEquivalentKey("tissue", summary.props);
			final String rightsHolderKey = getEquivalentKey("rightsHolder", summary.props);
			final String versionKey = getEquivalentKey("version", summary.props);
			final String referenceKey = getEquivalentKey("reference", summary.props);
			final String descriptionKey = getEquivalentKey("description", summary.props);

			nameField.setText(summary.name);
			authorField.setText(isCollection ? "" : authorKey != null ? summary.props.get(authorKey).toString() : "");
			organismField
					.setText(isCollection ? "" : organismKey != null ? summary.props.get(organismKey).toString() : "");
			diseaseField
					.setText(isCollection ? "" : diseaseKey != null ? summary.props.get(diseaseKey).toString() : "");
			tissueField.setText(isCollection ? "" : tissueKey != null ? summary.props.get(tissueKey).toString() : "");
			rightsHolderField.setText(
					isCollection ? "" : rightsHolderKey != null ? summary.props.get(rightsHolderKey).toString() : "");
			versionField
					.setText(isCollection ? "" : versionKey != null ? summary.props.get(versionKey).toString() : "");
			referenceField.setText(
					isCollection ? "" : referenceKey != null ? summary.props.get(referenceKey).toString() : "");
			descriptionTextArea.setText(
					isCollection ? "" : descriptionKey != null ? summary.props.get(descriptionKey).toString() : "");

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Server selectedServer = ServerManager.INSTANCE.getSelectedServer();

	}

	/*
	 * private boolean updateIsPossible() { return updateIsPossibleHelper() != null;
	 * }
	 * 
	 * private NetworkSummary updateIsPossibleHelper() { CyNetwork cyNetwork =
	 * CyObjectManager.INSTANCE.getCurrentNetwork();
	 * 
	 * UUID ndexNetworkId = null; CXInfoHolder cxInfo =
	 * NetworkManager.INSTANCE.getCXInfoHolder(cyNetwork.getSUID()); if (cxInfo !=
	 * null) { ndexNetworkId = cxInfo.getNetworkId(); } else { ndexNetworkId =
	 * NetworkManager.INSTANCE.getNdexNetworkId(cyNetwork.getSUID()); }
	 * 
	 * if (ndexNetworkId == null) return null;
	 * 
	 * Server selectedServer = ServerManager.INSTANCE.getSelectedServer();
	 * NdexRestClientModelAccessLayer mal = selectedServer.getModelAccessLayer();
	 * UUID userId = ServerManager.INSTANCE.getSelectedServer().getUserId();
	 * 
	 * try { Map<String, Permissions> permissionTable =
	 * mal.getUserNetworkPermission(userId, ndexNetworkId, false); if
	 * (permissionTable == null || permissionTable.get(ndexNetworkId.toString()) ==
	 * Permissions.READ) return null;
	 * 
	 * } catch (IOException e) { return null; }
	 * 
	 * NetworkSummary ns = null; try { ns =
	 * mal.getNetworkSummaryById(ndexNetworkId.toString()); if (ns.getIsReadOnly())
	 * return null;
	 * 
	 * } catch (IOException e) { return null; } catch (NdexException e) { return
	 * null; } return ns; }
	 */
	/*
	 * private void updateModificationTimeLocally() { CyNetwork cyNetwork =
	 * CyObjectManager.INSTANCE.getCurrentNetwork(); CyRootNetwork rootNetwork =
	 * ((CySubNetwork)cyNetwork).getRootNetwork(); CyRow r =
	 * rootNetwork.getRow(rootNetwork); String modificationTime =
	 * r.get("ndex:modificationTime", String.class); String networkId =
	 * r.get("ndex:uuid", String.class); if( modificationTime == null || networkId
	 * == null ) return; Server selectedServer =
	 * ServerManager.INSTANCE.getSelectedServer(); NdexRestClientModelAccessLayer
	 * mal = selectedServer.getModelAccessLayer(); NetworkSummary ns = null; try {
	 * ns = mal.getNetworkSummaryById(networkId); } catch (IOException |
	 * NdexException e) { return; } r.set("ndex:modificationTime",
	 * ns.getModificationTime().toString()); }
	 */

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated
	// <editor-fold defaultstate="collapsed" desc="Generated
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		jButton1 = new javax.swing.JButton();
		jPanel1 = new javax.swing.JPanel();
		organismField = new javax.swing.JTextField();
		authorField = new javax.swing.JTextField();
		diseaseField = new javax.swing.JTextField();
		nameField = new javax.swing.JTextField();
		rightsHolderField = new javax.swing.JTextField();
		jLabel7 = new javax.swing.JLabel();
		exportButton = new javax.swing.JButton();
		jLabel11 = new javax.swing.JLabel();
		tissueField = new javax.swing.JTextField();
		updateCheckbox = new javax.swing.JCheckBox();
		cancelButton = new javax.swing.JButton();
		profileButton = SignInButtonHelper.createSignInButton(this);
		jLabel9 = new javax.swing.JLabel();
		referenceField = new javax.swing.JTextField();
		jLabel6 = new javax.swing.JLabel();
		jLabel13 = new javax.swing.JLabel();
		versionField = new javax.swing.JTextField();
		jLabel8 = new javax.swing.JLabel();
		jLabel5 = new javax.swing.JLabel();
		jLabel12 = new javax.swing.JLabel();
		jLabel10 = new javax.swing.JLabel();
		jScrollPane3 = new javax.swing.JScrollPane();
		descriptionTextArea = new javax.swing.JTextArea();
		updateErrorLabel = new javax.swing.JLabel();
		updateSettingsButton = new JButton(ICON_COG);

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("Export Network to NDEx");

		organismField.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				organismFieldActionPerformed(evt);
			}
		});

		nameField.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				nameFieldActionPerformed(evt);
			}
		});

		jLabel7.setText("Organism");

		exportButton.setText("Export " + (isCollection() ? "Collection" : "Network") + " to NDEx");
		exportButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				exportButtonActionPerformed(evt);
			}
		});

		jLabel11.setText("Version");

		updateCheckbox.setText("Update Existing Network");
		updateCheckbox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		updateCheckbox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				updateCheckboxActionPerformed(evt);
			}
		});

		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cancelButtonActionPerformed(evt);
			}
		});

		profileButton.setText(SignInButtonHelper.getSignInText());
		profileButton.setMaximumSize(new java.awt.Dimension(200, 30));
		profileButton.setMinimumSize(new java.awt.Dimension(48, 30));

		jLabel9.setText("Tissue");

		referenceField.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				referenceFieldActionPerformed(evt);
			}
		});

		jLabel6.setText("Author");

		jLabel13.setText("Description");

		jLabel8.setText("Disease");

		jLabel5.setText("Name:");

		jLabel12.setText("Reference");

		jLabel10.setText("Rights Holder");

		descriptionTextArea.setColumns(20);
		descriptionTextArea.setLineWrap(true);
		descriptionTextArea.setRows(5);
		jScrollPane3.setViewportView(descriptionTextArea);

		updateErrorLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		updateErrorLabel.setText("Unable to update network.");
		updateErrorLabel.setEnabled(false);
		updateErrorLabel.setFocusable(false);

		updateSettingsButton.setFont(CyServiceModule.getService(IconManager.class).getIconFont(ICON_FONT_SIZE * 4 / 5));
		updateSettingsButton.setToolTipText("Update options...");
		updateSettingsButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
		updateSettingsButton.setBorderPainted(false);
		updateSettingsButton.setContentAreaFilled(false);
		updateSettingsButton.setFocusPainted(false);
		updateSettingsButton.setMaximumSize(new java.awt.Dimension(32, 32));
		updateSettingsButton.setMinimumSize(new java.awt.Dimension(24, 24));
		updateSettingsButton.setPreferredSize(new java.awt.Dimension(24, 24));
		updateSettingsButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				updateSettingsButtonActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
		jPanel1.setLayout(jPanel1Layout);
		jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(jPanel1Layout.createSequentialGroup()
								.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
										.addComponent(jLabel7).addComponent(jLabel6).addComponent(jLabel5)
										.addComponent(jLabel8).addComponent(jLabel9).addComponent(jLabel10)
										.addComponent(jLabel11).addComponent(jLabel12).addComponent(jLabel13))
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
										.addComponent(organismField).addComponent(diseaseField)
										.addComponent(tissueField).addComponent(rightsHolderField)
										.addComponent(versionField)
										.addComponent(referenceField, javax.swing.GroupLayout.Alignment.TRAILING)
										.addComponent(nameField).addComponent(authorField).addComponent(jScrollPane3,
												javax.swing.GroupLayout.DEFAULT_SIZE, 447, Short.MAX_VALUE)))
						.addGroup(jPanel1Layout.createSequentialGroup().addComponent(cancelButton)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(exportButton))
						.addComponent(updateErrorLabel, javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
								jPanel1Layout.createSequentialGroup().addGap(0, 0, Short.MAX_VALUE).addComponent(
										profileButton, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
						.addContainerGap())
				.addGroup(jPanel1Layout.createSequentialGroup().addGap(180, 180, 180).addComponent(updateCheckbox)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(updateSettingsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 24,
								javax.swing.GroupLayout.PREFERRED_SIZE)
						.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel1Layout.createSequentialGroup().addContainerGap()
						.addComponent(profileButton, javax.swing.GroupLayout.PREFERRED_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel5).addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 14, Short.MAX_VALUE)
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel6).addComponent(authorField, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 14, Short.MAX_VALUE)
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel7)
								.addComponent(organismField, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 14, Short.MAX_VALUE)
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel8).addComponent(diseaseField,
										javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 14, Short.MAX_VALUE)
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel9).addComponent(tissueField, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 14, Short.MAX_VALUE)
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel10).addComponent(rightsHolderField,
										javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 14, Short.MAX_VALUE)
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel11).addComponent(versionField,
										javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 14, Short.MAX_VALUE)
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel12).addComponent(referenceField,
										javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 14, Short.MAX_VALUE)
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(jLabel13)
								.addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addGap(4, 4, 4)
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
								.addComponent(updateCheckbox).addComponent(updateSettingsButton,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
										Short.MAX_VALUE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(updateErrorLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 17, Short.MAX_VALUE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(cancelButton).addComponent(exportButton))
						.addContainerGap()));

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap().addComponent(jPanel1,
						javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(
				jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void updateSettingsButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_updateSettingsButtonActionPerformed

		CyNetwork network = UpdateUtil.getNetworkForSUID(saveParameters.suid,
				saveParameters.saveType.equals("collection"));

		UpdateSettingsDialog updateSettingsDialog = new UpdateSettingsDialog(this, network,
				ServerManager.INSTANCE.getSelectedServer(), !updateCheckbox.isEnabled());
		updateSettingsDialog.setLocationRelativeTo(this);
		updateSettingsDialog.setVisible(true);

		UUID newUUID = updateSettingsDialog.getNewUUID();
		isUUIDChanged |= updateSettingsDialog.isChanged();
		updateSettingsDialog.dispose();

		if (newUUID != null) {
			updateUpdateButton();
			updateCheckbox.setSelected(true);
		}

	}// GEN-LAST:event_updateSettingsButtonActionPerformed

	private void nameFieldActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_nameFieldActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_nameFieldActionPerformed

	private void organismFieldActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_organismFieldActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_organismFieldActionPerformed

	private void referenceFieldActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_referenceFieldActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_referenceFieldActionPerformed

	private void prepareToWriteNetworkToCXStream(CyNetwork cyNetwork, PipedOutputStream out, boolean isUpdate) {
		/*
		 * VisualMappingManager vmm =
		 * CyObjectManager.INSTANCE.getVisualMappingManager(); final
		 * CyNetworkViewManager nvm = CyObjectManager.INSTANCE.getNetworkViewManager();
		 * final CyGroupManager gm = CyObjectManager.INSTANCE.getCyGroupManager(); final
		 * VisualLexicon lexicon = CyObjectManager.INSTANCE.getDefaultVisualLexicon();
		 * CxNetworkWriter writer = new CxNetworkWriter(out, cyNetwork, vmm, nvm, gm,
		 * lexicon, isUpdate); boolean writeEntireCollection =
		 * networkOrCollectionCombo.getSelectedIndex() == 1;
		 * writer.setWriteSiblings(writeEntireCollection); TaskIterator ti = new
		 * TaskIterator(writer); TaskManager tm =
		 * CyObjectManager.INSTANCE.getTaskManager(); tm.execute(ti);
		 */
	}

	private void exportButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_uploadActionPerformed
		final Server server = ServerManager.INSTANCE.getServer();
		if (server.getUsername() == null) {
			SignInDialog signInDialog = new SignInDialog(null);
			signInDialog.setLocationRelativeTo(this);
			signInDialog.setVisible(true);
			
			final Server selectedServer = ServerManager.INSTANCE.getServer();
			if (selectedServer.getUsername() == null || selectedServer.getUsername().isEmpty()) {
				signInDialog.dispose();
				return;
			} else {
				signInDialog.dispose();
			}
		}
		
		Container container = this.getParent();
		ModalProgressHelper.runWorker(this, "Exporting to NDEx", () -> {

			final Server selectedServer = ServerManager.INSTANCE.getSelectedServer();
			final boolean update = updateCheckbox.isEnabled() && updateCheckbox.isSelected();
			final String REST_URI = "http://localhost:" + CyActivator.getCyRESTPort() + "/cyndex2/v1/networks/"
					+ saveParameters.suid;

			HttpClient httpClient = HttpClients.createDefault();
			final URI uri = URI.create(REST_URI);
			final HttpEntityEnclosingRequestBase request = update ? new HttpPut(uri.toString())
					: new HttpPost(uri.toString());
			request.setHeader("Content-type", "application/json");

			final Map<String, String> metadata = new HashMap<>();
			metadata.put("name", nameField.getText());
			metadata.put("author", authorField.getText());
			metadata.put("organism", organismField.getText());
			metadata.put("disease", diseaseField.getText());
			metadata.put("tissue", tissueField.getText());
			metadata.put("rightsHolder", rightsHolderField.getText());
			metadata.put("version", versionField.getText());
			metadata.put("reference", referenceField.getText());
			metadata.put("description", descriptionTextArea.getText());

			NDExSaveParameters saveParams = new NDExSaveParameters(selectedServer.getUsername(),
					selectedServer.getPassword(), selectedServer.getUrl(), metadata, false);
			final ObjectMapper objectMapper = new ObjectMapper();

			try {

				request.setEntity(new StringEntity(objectMapper.writeValueAsString(saveParams)));
				final HttpResponse response = httpClient.execute(request);
				final int statusCode = response.getStatusLine().getStatusCode();

				if (statusCode == 200) {

					final String result = EntityUtils.toString(response.getEntity());

					JsonNode jsonNode = objectMapper.readValue(result, JsonNode.class);
					final String uuid = jsonNode.get("data").get("uuid").asText();

					JOptionPane.showMessageDialog(container, "Export to NDEx successful.\n\nUUID: " + uuid,
							"Export Complete", JOptionPane.PLAIN_MESSAGE);
				} else {
					final String result = EntityUtils.toString(response.getEntity());

					JsonNode jsonNode = objectMapper.readValue(result, JsonNode.class);
					final String errorMessage = jsonNode.get("errors").get(0).get("message").asText();

					JOptionPane.showMessageDialog(container,
							"Export to NDEx failed with the following message:\n\n" + errorMessage, "Export Error",
							JOptionPane.ERROR_MESSAGE);
				}

			} catch (IOException e) {
				// upload failed — dialog already shown for non-200 status; other errors fall through silently
			}
			return 1;
		});

		this.setVisible(false);

	}// GEN-LAST:event_uploadActionPerformed

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_cancelActionPerformed
	{// GEN-HEADEREND:event_cancelActionPerformed
		this.setVisible(false);
	}// GEN-LAST:event_cancelActionPerformed

	private void updateCheckboxActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_updateCheckboxActionPerformed
	{// GEN-HEADEREND:event_updateCheckboxActionPerformed
	}// GEN-LAST:event_updateCheckboxActionPerformed

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (isVisible()) {
			profileButton.setText(SignInButtonHelper.getSignInText());
			updateUpdateButton();
		}
	}

	private static String htmlWrap(String input) {
		return "<html><body><div style=\"width: 186px; word-wrap: break-word; text-align: center\">" + input + "</div></body></html>";
	}

	private void updateUpdateButton() {
		final Server selectedServer = ServerManager.INSTANCE.getServer();
		boolean updatePossible;
		if (selectedServer == Server.DEFAULT_SERVER) {
			updatePossible = false;
			updateErrorLabel.setText("Update not possible. Please sign in to a valid NDEx account");
		} else {
			try {
				final NdexRestClient nc = new NdexRestClient(selectedServer.getUsername(), selectedServer.getPassword(),
						selectedServer.getUrl(), UserAgentUtil.getUserAgent());
				final NdexRestClientModelAccessLayer mal = new NdexRestClientModelAccessLayer(nc);
				updatePossible = UpdateUtil.updateIsPossibleHelper(saveParameters.suid,
						saveParameters.saveType.equals("collection"), nc, mal, !isUUIDChanged) != null;
				updateErrorLabel.setText(
						updatePossible ? "Update the existing network in NDEx" : "Update not possible, unknown error.");
			} catch (Exception e) {
				updateErrorLabel.setText(htmlWrap(e.getMessage()));
				updatePossible = false;
			}
		}
		updateCheckbox.setSelected(false);
		updateCheckbox.setEnabled(updatePossible);
		updateErrorLabel.setVisible(!updatePossible);

	}

	/**
	 * @param args the command line arguments
	 */
	/*
	 * public static void main(String args[]) { // Set the Nimbus look and feel
	 * //<editor-fold defaultstate="collapsed"
	 * desc=" Look and feel setting code (optional) "> // If Nimbus (introduced in
	 * Java SE 6) is not available, stay with the default look and feel. // For
	 * details see
	 * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
	 * 
	 * try { for (javax.swing.UIManager.LookAndFeelInfo info :
	 * javax.swing.UIManager.getInstalledLookAndFeels()) { if
	 * ("Nimbus".equals(info.getName())) {
	 * javax.swing.UIManager.setLookAndFeel(info.getClassName()); break; } } } catch
	 * (ClassNotFoundException ex) {
	 * java.util.logging.Logger.getLogger(ExportNetworkDialog.class.getName()).log(
	 * java.util.logging.Level.SEVERE, null, ex); } catch (InstantiationException
	 * ex) {
	 * java.util.logging.Logger.getLogger(ExportNetworkDialog.class.getName()).log(
	 * java.util.logging.Level.SEVERE, null, ex); } catch (IllegalAccessException
	 * ex) {
	 * java.util.logging.Logger.getLogger(ExportNetworkDialog.class.getName()).log(
	 * java.util.logging.Level.SEVERE, null, ex); } catch
	 * (javax.swing.UnsupportedLookAndFeelException ex) {
	 * java.util.logging.Logger.getLogger(ExportNetworkDialog.class.getName()).log(
	 * java.util.logging.Level.SEVERE, null, ex); } //</editor-fold>
	 * //</editor-fold>
	 * 
	 * // Create and display the dialog java.awt.EventQueue.invokeLater(new
	 * Runnable() { public void run() { ExportNetworkDialog dialog = new
	 * ExportNetworkDialog(new javax.swing.JFrame()); dialog.addWindowListener(new
	 * java.awt.event.WindowAdapter() {
	 * 
	 * @Override public void windowClosing(java.awt.event.WindowEvent e) {
	 * System.exit(0); } }); dialog.setVisible(true); } }); }
	 */

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JTextField authorField;
	private javax.swing.JButton cancelButton;
	private javax.swing.JTextArea descriptionTextArea;
	private javax.swing.JTextField diseaseField;
	private javax.swing.JButton exportButton;
	private javax.swing.JButton jButton1;
	private javax.swing.JLabel jLabel10;
	private javax.swing.JLabel jLabel11;
	private javax.swing.JLabel jLabel12;
	private javax.swing.JLabel jLabel13;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabel7;
	private javax.swing.JLabel jLabel8;
	private javax.swing.JLabel jLabel9;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JScrollPane jScrollPane3;
	private javax.swing.JTextField nameField;
	private javax.swing.JTextField organismField;
	private javax.swing.JButton profileButton;
	private javax.swing.JTextField referenceField;
	private javax.swing.JTextField rightsHolderField;
	private javax.swing.JTextField tissueField;
	private javax.swing.JCheckBox updateCheckbox;
	private javax.swing.JLabel updateErrorLabel;
	private javax.swing.JButton updateSettingsButton;
	private javax.swing.JTextField versionField;
	// End of variables declaration//GEN-END:variables

}
