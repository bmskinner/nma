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
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DatasetMergeMethod;
import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.signals.PairedSignalGroups;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.dialogs.DatasetArithmeticSetupDialog.BooleanOperation;
import com.bmskinner.nma.gui.dialogs.SignalPairMergingDialog;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.utility.FileUtils;

/**
 * Carry out a merge of datasets
 * 
 * @author ben
 *
 */
public class MergeCollectionAction extends MultiDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(MergeCollectionAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Merging";
	private static final @NonNull String DEFAULT_DATASET_NAME = "Merge_of_datasets";
	private static final int NUMBER_OF_STEPS = 100;

	public MergeCollectionAction(@NonNull final List<IAnalysisDataset> datasets,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(datasets, PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {

		if (!datasetsAreMergeable()) {
			super.finished();
			return;
		}

		// Try to find a sensible ancestor dir of the datasets
		// Otherwise default to the home dir
		File dir = FileUtils.commonPathOfDatasets(datasets);
		if (!dir.exists() || !dir.isDirectory())
			dir = GlobalOptions.getInstance().getDefaultDir();

		try {
			File saveFile = is.requestFileSave(dir, DEFAULT_DATASET_NAME,
					Io.NMD_FILE_EXTENSION_NODOT);

			if (saveFile.exists()) {
				boolean overwrite = is.requestApproval(
						"The selected file exists; do you want to overwrite?",
						"Overwrite file?");
				if (overwrite) {
					Files.delete(saveFile.toPath());
				} else {
					super.finished();
					return;
				}
			}

			IAnalysisMethod m;

			if (hasMoreThanOneSignalGroup()) {

				SignalPairMergingDialog dialog = new SignalPairMergingDialog(datasets);
				PairedSignalGroups pairs = dialog.getPairedSignalGroups();
				m = new DatasetMergeMethod(datasets, BooleanOperation.OR, saveFile, pairs);

			} else {
				// no signals to merge
				m = new DatasetMergeMethod(datasets, BooleanOperation.OR, saveFile);
			}

			worker = new DefaultAnalysisWorker(m, NUMBER_OF_STEPS);
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);

		} catch (RequestCancelledException | IOException e) {
			super.finished();
		}
	}

	/**
	 * Check for signals in >1 dataset
	 * 
	 * @return
	 */
	private boolean hasMoreThanOneSignalGroup() {
		int numSignals = 0;
		for (IAnalysisDataset d : datasets) {
			if (d.getCollection().getSignalManager().hasSignals())
				numSignals++;
		}
		return numSignals > 1;
	}

	/**
	 * Check datasets are valid to be merged
	 * 
	 * @param datasets
	 * @return
	 */
	private boolean datasetsAreMergeable() {

		if (datasets.size() == 2 && (datasets.get(0).hasDirectChild(datasets.get(1))
				|| datasets.get(1).hasDirectChild(datasets.get(0)))) {
			LOGGER.warning("No. Merging parent and child is silly.");
			return false;
		}
		return true;
	}

	@Override
	public void finished() {

		IAnalysisResult r;
		try {
			r = worker.get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.warning("Error merging datasets");
			LOGGER.log(Loggable.STACK, "Error merging datasets", e);
			this.cancel();
			Thread.currentThread().interrupt();
			return;
		}
		List<IAnalysisDataset> result = r.getDatasets();

		if (datasets != null && !datasets.isEmpty())
			UserActionController.getInstance().userActionEventReceived(
					new UserActionEvent(this, UserActionEvent.MORPHOLOGY_ANALYSIS_ACTION, result));

		super.finished();
	}
}
