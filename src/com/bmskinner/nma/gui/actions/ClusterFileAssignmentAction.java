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
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.ClusterAnalysisResult;
import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.classification.ClusterFileAssignmentMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.components.FileSelector;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;
import com.bmskinner.nma.logging.Loggable;

/**
 * Action to trigger assignment of nuclei to clusters based on an input
 * definition file
 * 
 * @author Ben Skinner
 *
 */
public class ClusterFileAssignmentAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger
			.getLogger(ClusterFileAssignmentAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Assigning clustered cells";

	public ClusterFileAssignmentAction(@NonNull IAnalysisDataset dataset,
			@NonNull ProgressBarAcceptor acceptor) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {

		File clusterMapFile = FileSelector.chooseFile(dataset.getSavePath().getParentFile());

		if (clusterMapFile != null) {

			IAnalysisMethod m = new ClusterFileAssignmentMethod(dataset, clusterMapFile);
			worker = new DefaultAnalysisWorker(m);

			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);

		} else {
			this.cancel();
		}
	}

	@Override
	public void finished() {

		this.setProgressBarVisible(false);

		try {
			ClusterAnalysisResult r = (ClusterAnalysisResult) worker.get();
			int size = r.getGroup().size();
			LOGGER.info("Found " + size + " clusters");

			UserActionController.getInstance().userActionEventReceived(
					new UserActionEvent(this, UserActionEvent.REFOLD_CONSENSUS,
							r.getDatasets()));

			UserActionController.getInstance()
					.userActionEventReceived(
							new UserActionEvent(this, UserActionEvent.SAVE, dataset));

			UIController.getInstance().fireClusterGroupAdded(dataset, r.getGroup());
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.warning("Error clustering");
			LOGGER.log(Loggable.STACK, "Error clustering", e);
			Thread.currentThread().interrupt();
		}

		super.finished();

	}
}
