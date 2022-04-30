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

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.ClusterAnalysisResult;
import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.dialogs.ClusterTreeDialog;
import com.bmskinner.nma.gui.dialogs.HierarchicalTreeSetupDialog;
import com.bmskinner.nma.gui.dialogs.SubAnalysisSetupDialog;

/**
 * Action for constructing hierarchical trees based on dataset parameters
 * 
 * @author ben
 *
 */
public class BuildHierarchicalTreeAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(BuildHierarchicalTreeAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Building tree";

	public BuildHierarchicalTreeAction(@NonNull IAnalysisDataset dataset, @NonNull ProgressBarAcceptor acceptor) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {

		SubAnalysisSetupDialog clusterSetup = new HierarchicalTreeSetupDialog(dataset);

		if (clusterSetup.isReadyToRun()) { // if dialog was cancelled, skip
			IAnalysisMethod m = clusterSetup.getMethod();

			worker = new DefaultAnalysisWorker(m);
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);

		} else {
			this.cancel();
		}
		clusterSetup.dispose();
	}

	/*
	 * (non-Javadoc) Overrides because we need to carry out the morphology
	 * reprofiling on each cluster
	 * 
	 * @see no.gui.MainWindow.ProgressableAction#finished()
	 */
	@Override
	public void finished() {

		try {
			ClusterAnalysisResult r = (ClusterAnalysisResult) worker.get();

			new ClusterTreeDialog(dataset, r.getGroup());

			cleanup(); // do not cancel, we need the MainWindow listener to
						// remain attached

		} catch (InterruptedException | ExecutionException e) {
			LOGGER.log(Level.SEVERE, "Error making cluster tree dialog", e);
			Thread.currentThread().interrupt();
		}
	}
}
