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
package com.bmskinner.nma.gui.actions;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import javax.swing.JProgressBar;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.analysis.IAnalysisWorker;
import com.bmskinner.nma.core.InputSupplier;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.ProgressBarAcceptor;

/**
 * The base of all progressible actions. Handles progress bars and workers
 * 
 * @author bms41
 * @since 1.13.6
 *
 */
public abstract class VoidResultAction implements PropertyChangeListener, Runnable {

	protected IAnalysisWorker worker = null;

	protected List<JProgressBar> progressBars = new ArrayList<>(); // jcomponents can't be shared
																	// across components
	protected List<ProgressBarAcceptor> progressAcceptors = new ArrayList<>();

	private Optional<CountDownLatch> latch = Optional.empty(); // allow threads to wait

	protected final InputSupplier is = new DefaultInputSupplier();

	/**
	 * Constructor
	 * 
	 * @param barMessage the message to display in the progress bar
	 * @param mw         the main window
	 */
	protected VoidResultAction(@NonNull String barMessage, @NonNull ProgressBarAcceptor acceptor) {

		progressAcceptors.add(acceptor);
		createProgressBar(barMessage);
	}

	protected VoidResultAction(@NonNull String barMessage,
			@NonNull List<ProgressBarAcceptor> acceptors) {
		progressAcceptors.addAll(acceptors);
		createProgressBar(barMessage);
	}

	private void createProgressBar(String barMessage) {
		for (ProgressBarAcceptor a : progressAcceptors) {
			JProgressBar progressBar = new JProgressBar(0, 100);
			progressBar.setString(barMessage);
			progressBar.setStringPainted(true);
			progressBar.setIndeterminate(true);
			progressBar.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getSource() == progressBar && e.getClickCount() == 2) {
						worker.cancel(true);
						cleanup();
					}
				}
			});

			a.addProgressBar(progressBar);
			progressBars.add(progressBar);
		}
	}

	protected void setLatch(@Nullable final CountDownLatch latch) {
		this.latch = Optional.ofNullable(latch);
	}

	protected Optional<CountDownLatch> getLatch() {
		return latch;
	}

	/**
	 * If the latch is present, count down by one
	 */
	protected void countdownLatch() {
		if (latch.isPresent())
			latch.get().countDown();
	}

	/**
	 * Change the progress message from the default in the constructor
	 * 
	 * @param messsage the string to display
	 */
	public void setProgressMessage(String messsage) {
		for (JProgressBar b : progressBars) {
			b.setString(messsage);
		}
	}

	private void removeProgressBar() {
		for (ProgressBarAcceptor a : progressAcceptors) {
			for (JProgressBar b : progressBars) {
				a.removeProgressBar(b);
			}
		}
	}

	/**
	 * Remove the progress bar and dataset and interface listeners
	 */
	public void cancel() {
		removeProgressBar();
	}

	protected void setProgressBarVisible(boolean b) {
		for (JProgressBar bar : progressBars) {
			bar.setVisible(b);
		}
	}

	/**
	 * Use to manually remove the progress bar after an action is complete
	 */
	public void cleanup() {
		if (this.worker.isDone() || this.worker.isCancelled()) {
			this.worker.removePropertyChangeListener(this);
			this.removeProgressBar();
		}
	}

	@Override
	public synchronized void propertyChange(PropertyChangeEvent evt) {

		int value = 0;

		Object newValue = evt.getNewValue();

		if (newValue instanceof Integer) {
			value = (int) newValue;
		}

		if (value >= 0 && value <= 100) {

			for (JProgressBar bar : progressBars) {
				if (bar.isIndeterminate())
					bar.setIndeterminate(false);
				bar.setValue(value);
			}

		}

		if (evt.getPropertyName().equals(IAnalysisWorker.FINISHED_MSG))
			finished();

		if (evt.getPropertyName().equals(IAnalysisWorker.ERROR_MSG))
			removeProgressBar();

		if (evt.getPropertyName().equals(IAnalysisWorker.INDETERMINATE_MSG))
			setProgressBarIndeterminate();
	}

	/**
	 * The method run when the analysis has completed
	 */
	public void finished() {
		if (worker != null)
			worker.removePropertyChangeListener(this);
		cancel();
		countdownLatch();
	}

	/**
	 * Runs if a cooldown signal is received. Use to set progress bars to an
	 * indeterminate state when no reliable progress metric is available
	 */
	public void setProgressBarIndeterminate() {
		for (JProgressBar bar : progressBars) {
			bar.setIndeterminate(true);
		}
	}

	public synchronized boolean isDone() {
		return worker.isDone();
	}

}
