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
import com.bmskinner.nma.analysis.IAnalysisWorker;
import com.bmskinner.nma.analysis.classification.NucleusClusteringMethod;
import com.bmskinner.nma.analysis.classification.PrincipalComponentAnalysis;
import com.bmskinner.nma.analysis.classification.TsneMethod;
import com.bmskinner.nma.analysis.classification.UMAPMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.dialogs.ClusteringSetupDialog;
import com.bmskinner.nma.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;

/**
 * Setup a clustering of the given dataset.
 * 
 * @author Ben Skinner
 *
 */
public class ClusterAutomaticAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(ClusterAutomaticAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Clustering cells";

	public ClusterAutomaticAction(IAnalysisDataset dataset, @NonNull ProgressBarAcceptor acceptor) {
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

			if (setupOptions.getBoolean(HashOptions.CLUSTER_USE_UMAP_KEY)) {
				canRunClusteringDirectly = false;
				runUmap(setupOptions);
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
	 * Run a UMAP method with the given options
	 * 
	 * @param setupOptions
	 */
	private void runUmap(HashOptions setupOptions) {
		IAnalysisMethod m = new UMAPMethod(dataset, setupOptions);
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
		worker.addPropertyChangeListener(ClusterAutomaticAction.this);
		ThreadManager.getInstance().submit(worker);
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
			LOGGER.log(Level.SEVERE, "Error clustering", e);
			Thread.currentThread().interrupt();
		}

		super.finished();
	}
}
