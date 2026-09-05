package org.cytoscape.cyndex2.internal.task.command;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.json.JSONResult;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Shared plumbing for the {@code ndex} desktop commands: a JSON result, and running a delegate
 * task iterator inline.
 *
 * Delegating by appending to the running iterator would break the command's result. Cytoscape's
 * command executor reports each task's result the moment that task finishes, so an appended
 * delegate reports *after* this task has already reported, and the delegate's own result — a bare
 * UUID string, or a SUID — would be the one the caller sees. Running the delegate inline keeps this
 * task the only {@link ObservableTask} the executor observes.
 */
public abstract class AbstractNdexCommandTask extends AbstractTask implements ObservableTask {

	private ObjectNode result;
	private volatile Task delegate;

	protected void setResult(final ObjectNode result) {
		this.result = result;
	}

	/** Runs every task of a delegate iterator on this thread, forwarding cancellation to it. */
	protected void runInline(final TaskIterator iterator, final TaskMonitor taskMonitor) throws Exception {
		try {
			while (iterator.hasNext()) {
				if (cancelled) {
					return;
				}
				final Task task = iterator.next();
				delegate = task;
				task.run(taskMonitor);
			}
		} finally {
			delegate = null;
		}
	}

	@Override
	public void cancel() {
		super.cancel();
		final Task running = delegate;
		if (running != null) {
			running.cancel();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> type) {
		if (result == null) {
			return null;
		}
		if (String.class.equals(type)) {
			return (R) result.toString();
		}
		if (JSONResult.class.equals(type)) {
			final JSONResult json = () -> result.toString();
			return (R) json;
		}
		return null;
	}
}
