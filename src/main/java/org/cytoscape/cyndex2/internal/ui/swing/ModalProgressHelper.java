package org.cytoscape.cyndex2.internal.ui.swing;

import java.awt.BorderLayout;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

public class ModalProgressHelper {
	
	public static void runWorker(JDialog parent, String title, IntSupplier intSupplier) {
		runWorker(parent, title, (Supplier<Integer>) intSupplier::getAsInt);
	}

	/**
	 * Runs the supplier on a worker thread behind a modal progress dialog and hands back what it returned,
	 * or null if it threw (which is logged, as before).
	 *
	 * Returning the result is what lets a caller report a failure *after* this call: the modal
	 * {@code setVisible(true)} below only returns once {@code done()} has disposed the dialog, so by then the
	 * worker has finished and the caller is back on the event dispatch thread with no modal in the way.
	 * Showing a dialog from inside the supplier instead would mean touching Swing off the EDT, stacked under
	 * a modal that is still up -- which is how an error ends up invisible behind a spinner that never stops.
	 */
	public static <T> T runWorker(JDialog parent, String title, Supplier<T> supplier) {
		JDialog dlgProgress = new JDialog(parent, title, true);//true means that the dialog created is modal
		dlgProgress.setLocationRelativeTo(parent);
		JProgressBar pbProgress = new JProgressBar(0, 100);
		pbProgress.setIndeterminate(true); //we'll use an indeterminate progress bar

		dlgProgress.add(BorderLayout.CENTER, pbProgress);
		dlgProgress.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // prevent the user from closing the dialog
		dlgProgress.setSize(300, 90);

		final java.util.concurrent.atomic.AtomicReference<T> result = new java.util.concurrent.atomic.AtomicReference<>();

		SwingWorker<T, T> worker = new SwingWorker<T, T>() {

			@Override
			protected T doInBackground() throws Exception {
				return supplier.get();
			}
			
			@Override
			protected void done() {
				try {
					result.set(get());
				} catch (java.util.concurrent.ExecutionException ex) {
					Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
					Logger.getLogger(ModalProgressHelper.class.getName()).log(Level.WARNING, "Worker '" + title + "' failed", cause);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				} finally {
					dlgProgress.dispose();
				}
			}
		};
		worker.execute();
		dlgProgress.setVisible(true);
		return result.get();
	}
}
