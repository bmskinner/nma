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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.io.DatasetExportMethod;

/**
 * Export one or more datasets into nmd format
 * 
 * @author bs19022
 *
 */
public class ExportDatasetAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(ExportDatasetAction.class.getName());

	private File saveFile = null;

	private static final @NonNull String PROGRESS_BAR_LABEL = "Saving dataset";

	/**
	 * Constructor to save the current dataset. This gives programmatic access to
	 * saving to non-default locations, without needing to use the save dialog
	 * 
	 * @param dataset    the dataset to save
	 * @param saveFile   the location to save to
	 * @param acceptor   an acceptor that can hold progress bars
	 * @param doneSignal a latch to hold threads until the save is complete
	 */
	public ExportDatasetAction(@NonNull IAnalysisDataset dataset, File saveFile,
			@NonNull final ProgressBarAcceptor acceptor, CountDownLatch doneSignal) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
		setLatch(doneSignal);
		this.setProgressBarIndeterminate();
	}

	/**
	 * Construct with a dataset to save
	 * 
	 * @param dataset            the dataset to save
	 * @param acceptor           an acceptor that can hold progress bars
	 * @param doneSignal         a latch to hold threads until the save is complete
	 * @param chooseSaveLocation save to the default dataset save file, or choose
	 *                           another location
	 */
	public ExportDatasetAction(@NonNull IAnalysisDataset dataset,
			@NonNull final ProgressBarAcceptor acceptor,
			CountDownLatch doneSignal, boolean chooseSaveLocation) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
		if (doneSignal != null)
			setLatch(doneSignal);

		this.setProgressBarIndeterminate();

		if (chooseSaveLocation) {

			try {
				saveFile = is.requestFileSave(dataset.getSavePath().getParentFile(),
						dataset.getName(), "nmd");
			} catch (RequestCancelledException e) {
				cancel();
			}
		} else {
			saveFile = dataset.getSavePath();
		}

	}

	/**
	 * Construct with a list of datasets to save
	 * 
	 * @param list       the datasets to save
	 * @param acceptor   an acceptor that can hold progress bars
	 * @param doneSignal a latch to hold threads until the save is complete
	 */
	public ExportDatasetAction(List<IAnalysisDataset> list,
			@NonNull final ProgressBarAcceptor acceptor,
			CountDownLatch doneSignal) {
		super(list, PROGRESS_BAR_LABEL, acceptor);
		this.setLatch(doneSignal);
		this.setProgressBarIndeterminate();
		dataset = DatasetListManager.getInstance().getRootParent(dataset);
		saveFile = dataset.getSavePath();
	}

	@Override
	public void run() {
		// Only run if there is a save file, and the dataset has changed since last save
		if (saveFile == null) {
			LOGGER.fine(
					() -> "Dataset '%s' has no designated save file; skipping save"
							.formatted(dataset.getName()));
			this.finished();
			return;
		}

		LOGGER.fine(() -> "Dataset '%s' has hashcode before save '%s'".formatted(dataset.getName(),
				dataset.hashCode()));

		if (!DatasetListManager.getInstance().hashCodeChanged(dataset)) {
			LOGGER.fine(
					() -> "Dataset '%s' has not changed since last save; skipping write"
							.formatted(dataset.getName()));
			this.finished();
			return;
		}

		// If there is already a save file, use the length for the progress bar
		long length = saveFile.exists() ? saveFile.length() : 0;
		IAnalysisMethod m = new DatasetExportMethod(dataset, saveFile);
		worker = new DefaultAnalysisWorker(m, length);
		worker.addPropertyChangeListener(this);
		ThreadManager.getInstance().submit(worker);
	}

	@Override
	public void finished() {
		LOGGER.info(() -> "Saved as '%s'".formatted(saveFile.getName()));
		// update the stored hashcode for the dataset
		DatasetListManager.getInstance().updateHashCode(dataset);

		LOGGER.fine(() -> "Dataset '%s' has hashcode after save '%s'".formatted(dataset.getName(),
				dataset.hashCode()));

		Thread thr = new Thread(() -> {

			// if no list was provided, or no more entries remain, finish
			if (!hasRemainingDatasetsToProcess()) {
				countdownLatch();
				ExportDatasetAction.super.finished();
			} else { // otherwise analyse the next item in the list
				cancel(); // remove progress bar
				new ExportDatasetAction(getRemainingDatasetsToProcess(), progressAcceptors.get(0),
						getLatch().get())
								.run();
			}
		});

		thr.start();
	}
}
