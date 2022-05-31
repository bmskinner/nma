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
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DatasetMergeMethod;
import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.dialogs.DatasetArithmeticSetupDialog;
import com.bmskinner.nma.gui.dialogs.DatasetArithmeticSetupDialog.BooleanOperation;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.logging.Loggable;

/**
 * Trigger methods to perform boolean operations on datasets
 * 
 * @author Ben Skinner
 *
 */
public class BooleanOperationAction extends MultiDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(BooleanOperationAction.class.getName());

	private static final @NonNull String PROGRESS_LBL = "Dataset arithmetic";

	private static final int NUMBER_OF_STEPS = 100;
	private static final @NonNull String DEFAULT_DATASET_NAME = "Boolean_of_datasets";
	private File saveFile;

	public BooleanOperationAction(@NonNull List<IAnalysisDataset> list,
			@NonNull ProgressBarAcceptor acceptor) {
		super(list, PROGRESS_LBL, acceptor);
		this.setProgressBarIndeterminate();
	}

	@Override
	public void run() {
		try {
			// Try to find a sensible ancestor dir of the datasets
			// Otherwise default to the home dir
			File dir = IAnalysisDataset.commonPathOfFiles(datasets);
			if (!dir.exists() || !dir.isDirectory())
				dir = GlobalOptions.getInstance().getDefaultDir();

			/*
			 * Make a dialog with a dropdown for dataset 1, operator, then dropdown for
			 * dataset 2
			 */

			DatasetArithmeticSetupDialog dialog = new DatasetArithmeticSetupDialog(datasets);
			if (dialog.isReadyToRun()) {

				File saveFile = new DefaultInputSupplier().requestFileSave(dir,
						DEFAULT_DATASET_NAME,
						Io.NMD_FILE_EXTENSION_NODOT);

				IAnalysisDataset datasetOne = dialog.getDatasetOne();
				IAnalysisDataset datasetTwo = dialog.getDatasetTwo();
				BooleanOperation operation = dialog.getOperation();

				IAnalysisMethod m = new DatasetMergeMethod(datasets, operation, saveFile);

				worker = new DefaultAnalysisWorker(m, NUMBER_OF_STEPS);
				worker.addPropertyChangeListener(this);
				ThreadManager.getInstance().submit(worker);

			}

		} catch (RequestCancelledException e1) {
			// User request cancelled
			super.finished();
		}
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
			return;
		}
		List<IAnalysisDataset> result = r.getDatasets();

		if (datasets != null && !datasets.isEmpty())
			UserActionController.getInstance().userActionEventReceived(
					new UserActionEvent(this, UserActionEvent.MORPHOLOGY_ANALYSIS_ACTION, result));

		super.finished();
	}
}
