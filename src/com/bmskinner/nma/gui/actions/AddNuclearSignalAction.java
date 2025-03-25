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

import java.io.File;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.signals.SignalDetectionMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.dialogs.prober.SignalImageProber;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;

/**
 * Show the setup screen to detect nuclear signals, and run a detection analysis
 * 
 * @author Ben Skinner
 *
 */
public class AddNuclearSignalAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(AddNuclearSignalAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Signal detection";

	public AddNuclearSignalAction(@NonNull IAnalysisDataset dataset,
			@NonNull ProgressBarAcceptor acceptor) {
		this(dataset, acceptor, null);
	}

	public AddNuclearSignalAction(@NonNull IAnalysisDataset dataset,
			@NonNull ProgressBarAcceptor acceptor, @Nullable CountDownLatch latch) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
		setLatch(latch);
	}

	@Override
	public void run() {

		File defaultDir = null;
		Optional<IAnalysisOptions> op = dataset.getAnalysisOptions();
		if (op.isPresent()) {
			Optional<File> of = op.get().getNucleusDetectionFolder();
			if (of.isPresent())
				defaultDir = of.get();
		}

		File folder = null;

		try {
			folder = is.requestFolder("Choose FISH signal image folder", defaultDir);
		} catch (RequestCancelledException e) {
			super.finished();
			return;
		}

		// We have a folder of images, proceed

		try {
			// add dialog for non-default detection options
			SignalImageProber analysisSetup = new SignalImageProber(dataset, folder);

			if (analysisSetup.isOk()) {

				HashOptions options = analysisSetup.getOptions();

				IAnalysisMethod m = new SignalDetectionMethod(dataset, options, folder);

				String name = options.getString(HashOptions.SIGNAL_GROUP_NAME);

				worker = new DefaultAnalysisWorker(m, dataset.getCollection().size());

				this.setProgressMessage("Signal detection: " + name);
				worker.addPropertyChangeListener(this);
				ThreadManager.getInstance().submit(worker);
			} else {
				super.finished();
			}

		} catch (Exception e) {
			super.finished();
			LOGGER.warning("Error in signal detection");
			LOGGER.log(Level.SEVERE, "Error in signal detection", e);
		}
	}

	@Override
	public void finished() {
		LOGGER.finer("Finished signal detection");
		cleanup(); // remove the property change listener
		try {
			IAnalysisResult r = worker.get();
			UserActionController.getInstance().userActionEventReceived(
					new UserActionEvent(this, UserActionEvent.REFOLD_CONSENSUS,
							r.getFirstDataset()));

			// Add the new signal group
			UIController.getInstance().fireDatasetAdded(r.getFirstDataset());

			// Update the signals in the root dataset
			UIController.getInstance().fireNuclearSignalUpdated(dataset);

		} catch (InterruptedException | ExecutionException e) {
			LOGGER.warning("Error getting signal dataset");
			Thread.currentThread().interrupt();
		} finally {
			super.finished();
		}
	}

}
