package org.cytoscape.cyndex2.internal.ui.swing;

import java.awt.BorderLayout;
import java.util.function.IntSupplier;

import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

public class ModalProgressHelper {
	
	public static void runWorker(JDialog parent, String title, IntSupplier intSupplier) {
		JDialog dlgProgress = new JDialog(parent, title, true);//true means that the dialog created is modal
		dlgProgress.setLocationRelativeTo(parent);
		JProgressBar pbProgress = new JProgressBar(0, 100);
		pbProgress.setIndeterminate(true); //we'll use an indeterminate progress bar

		dlgProgress.add(BorderLayout.CENTER, pbProgress);
		dlgProgress.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // prevent the user from closing the dialog
		dlgProgress.setSize(300, 90);

		SwingWorker<Integer, Integer> worker = new SwingWorker<Integer, Integer>() {

			@Override
			protected Integer doInBackground() throws Exception {
				return intSupplier.getAsInt();
			}
			
			@Override 
			protected void done() {
				dlgProgress.dispose();
			}
		};
		worker.execute();
		dlgProgress.setVisible(true);
	}
}
