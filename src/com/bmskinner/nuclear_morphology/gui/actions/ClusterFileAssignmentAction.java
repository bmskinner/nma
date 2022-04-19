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
package com.bmskinner.nuclear_morphology.gui.actions;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.ClusterAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.classification.ClusterFileAssignmentMethod;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UIController;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Action to trigger assignment of nuclei to clusters based on an input
 * definition file
 * 
 * @author Ben Skinner
 *
 */
public class ClusterFileAssignmentAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(ClusterFileAssignmentAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Assigning clustered cells";

	public ClusterFileAssignmentAction(IAnalysisDataset dataset, @NonNull ProgressBarAcceptor acceptor) {
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
			UIController.getInstance().fireDatasetAdded(r.getDatasets());
			UIController.getInstance().fireClusterGroupsUpdated(dataset);
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.warning("Error clustering");
			LOGGER.log(Loggable.STACK, "Error clustering", e);
			Thread.currentThread().interrupt();
		}

		super.finished();

	}
}
