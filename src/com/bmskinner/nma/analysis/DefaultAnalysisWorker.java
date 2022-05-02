/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.analysis;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import com.bmskinner.nma.logging.Loggable;

/**
 * The default implementation of IAnalysisWorker, using a SwingWorker.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DefaultAnalysisWorker extends SwingWorker<IAnalysisResult, Long>
		implements IAnalysisWorker {

	private static final String ERROR_PROPERTY = "Error";

	private static final Logger LOGGER = Logger.getLogger(DefaultAnalysisWorker.class.getName());

	private long progressTotal; // the maximum value for the progress bar
	private long progressCount = 0; // the current value for the progress bar

	protected final IAnalysisMethod method;

	/**
	 * Construct with a method. The progress bar total will be set to -1 - i.e. the
	 * bar will remain indeterminate until the method completes
	 * 
	 * @param m the method to run
	 */
	public DefaultAnalysisWorker(final IAnalysisMethod m) {
		this(m, -1);
	}

	/**
	 * Construct with a method and a total for the progress bar.
	 * 
	 * @param m        the method to run
	 * @param progress the length of the progress bar. If negative, the bar will be
	 *                 indeterminate.
	 */
	public DefaultAnalysisWorker(final IAnalysisMethod m, final long progress) {
		method = m;
		method.addProgressListener(this);
		progressTotal = progress;
	}

	@Override
	protected final IAnalysisResult doInBackground() throws Exception {

		// Set the bar
		fireIndeterminate();

		// do the analysis and wait for the result
		return method.call();
	}

	@Override
	public final void progressEventReceived(final ProgressEvent event) {

		if (this.isCancelled())
			method.cancel();

		if (event.getMessage() == ProgressEvent.SET_TOTAL_PROGRESS) {
			progressTotal = event.getValue();
			return;
		}

		if (event.getMessage() == ProgressEvent.SET_INDETERMINATE) {
			fireIndeterminate();
			return;
		}

		if (event.getMessage() == ProgressEvent.INCREASE_BY_VALUE)
			progressCount = event.getValue();
		else
			progressCount++;

		if (progressTotal >= 0)
			publish(progressCount);
	}

	@Override
	protected final void process(List<Long> integers) {
		long amount = integers.get(integers.size() - 1);
		int percent = (int) ((double) amount / (double) progressTotal * 100);
		if (percent >= 0 && percent <= 100) {
			setProgress(percent); // the integer representation of the percent
		}
	}

	private void fireIndeterminate() {
		firePropertyChange(IAnalysisWorker.INDETERMINATE_MSG, getProgress(),
				IAnalysisWorker.INDETERMINATE);
	}

	@Override
	public void done() {
		try {

			if (this.get() != null) {
				firePropertyChange(FINISHED_MSG, getProgress(), IAnalysisWorker.FINISHED);

			} else {
				firePropertyChange(ERROR_MSG, getProgress(), IAnalysisWorker.ERROR);
			}

		} catch (StackOverflowError e) {
			LOGGER.warning("Stack overflow detected! Close the software and restart.");
			LOGGER.log(Loggable.STACK, "Stack overflow in worker", e);
			firePropertyChange(ERROR_PROPERTY, getProgress(), IAnalysisWorker.ERROR);
		} catch (InterruptedException e) {
			LOGGER.warning("Task was interrupted: " + e.getMessage());
			LOGGER.log(Loggable.STACK, "Interruption to swing worker", e);
			firePropertyChange(ERROR_PROPERTY, getProgress(), IAnalysisWorker.ERROR);
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			LOGGER.warning("Error completing task: " + e.getCause().getMessage());
			firePropertyChange(ERROR_PROPERTY, getProgress(), IAnalysisWorker.ERROR);
		}

	}

}
