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

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.ClusterAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.classification.NucleusClusteringMethod;
import com.bmskinner.nuclear_morphology.analysis.classification.PrincipalComponentAnalysis;
import com.bmskinner.nuclear_morphology.analysis.classification.TsneMethod;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.dialogs.ClusteringSetupDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nuclear_morphology.gui.events.UserActionEvent;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UIController;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UserActionController;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Setup a clustering of the given dataset.
 * 
 * @author ben
 *
 */
public class ClusterAnalysisAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(ClusterAnalysisAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Clustering cells";

	public ClusterAnalysisAction(IAnalysisDataset dataset, @NonNull ProgressBarAcceptor acceptor) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {

		SubAnalysisSetupDialog clusterSetup = new ClusteringSetupDialog(dataset);

		if (clusterSetup.isReadyToRun()) { // if dialog was cancelled, skip
			HashOptions setupOptions = clusterSetup.getOptions();
			boolean canRunClusteringDirectly = true;

			if (setupOptions.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY)) {
				canRunClusteringDirectly = false;
				runTsne(setupOptions);
			}

			if (clusterSetup.getOptions().getBoolean(HashOptions.CLUSTER_USE_PCA_KEY)) {
				canRunClusteringDirectly = false;
				runPca(setupOptions);
			}

			// Only run clustering on profiles if no dimensionality reduction
			if (canRunClusteringDirectly)
				runClustering(setupOptions);

		} else {
			this.cancel();
		}
		clusterSetup.dispose();
	}

	/**
	 * Run a t-SNE method with the given options
	 * 
	 * @param setupOptions
	 */
	private void runTsne(HashOptions setupOptions) {
		IAnalysisMethod m = new TsneMethod(dataset, setupOptions);
		worker = new DefaultAnalysisWorker(m);
		worker.addPropertyChangeListener(e -> {
			if (e.getPropertyName().equals(IAnalysisWorker.FINISHED_MSG)) {
				runClustering(setupOptions);
			}
		});
		ThreadManager.getInstance().submit(worker);
	}

	/**
	 * Run a PCA method with the given options
	 * 
	 * @param setupOptions
	 */
	private void runPca(HashOptions setupOptions) {
		IAnalysisMethod m = new PrincipalComponentAnalysis(dataset, setupOptions);
		worker = new DefaultAnalysisWorker(m);
		worker.addPropertyChangeListener(e -> {
			if (e.getPropertyName().equals(IAnalysisWorker.FINISHED_MSG)) {
				runClustering(setupOptions);
			}
		});
		ThreadManager.getInstance().submit(worker);
	}

	/**
	 * Run clustering on the nuclear profiles with the given options
	 * 
	 * @param setupOptions
	 */
	private void runClustering(HashOptions options) {
		IAnalysisMethod m2 = new NucleusClusteringMethod(dataset, options);
		worker = new DefaultAnalysisWorker(m2);
		worker.addPropertyChangeListener(ClusterAnalysisAction.this);
		ThreadManager.getInstance().submit(worker);
	}

	@Override
	public void finished() {

		this.setProgressBarVisible(false);

		try {
			ClusterAnalysisResult r = (ClusterAnalysisResult) worker.get();
			int size = r.getGroup().size();
			LOGGER.info("Found " + size + " clusters");

			UIController.getInstance().fireClusterGroupAdded(dataset, r.getGroup());

		} catch (InterruptedException | ExecutionException e) {
			LOGGER.warning("Error clustering");
			LOGGER.log(Loggable.STACK, "Error clustering", e);
			Thread.currentThread().interrupt();
		}

		UserActionController.getInstance()
				.userActionEventReceived(new UserActionEvent(this, UserActionEvent.SAVE, List.of(dataset)));
		super.finished();
	}
}
