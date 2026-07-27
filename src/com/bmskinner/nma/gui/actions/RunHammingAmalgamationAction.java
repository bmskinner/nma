package com.bmskinner.nma.gui.actions;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.ClusterAnalysisResult;
import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.classification.NonunimodalRegionClusteringMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;

/**
 * Create an action to run Hamming amalgamation
 * 
 * @author Ben Skinner
 *
 */
public class RunHammingAmalgamationAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(RunHammingAmalgamationAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Running Hamming amalgamation";

	public RunHammingAmalgamationAction(@NonNull List<IAnalysisDataset> datasets,
			@NonNull CountDownLatch latch,
			@NonNull ProgressBarAcceptor acceptor) {
		super(datasets, PROGRESS_BAR_LABEL, acceptor);
		this.setLatch(latch);
	}

	public RunHammingAmalgamationAction(@NonNull IAnalysisDataset dataset,
			@NonNull ProgressBarAcceptor acceptor) {
		super(dataset, dataset.getName() + ": " + PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {

		final IAnalysisMethod m = new NonunimodalRegionClusteringMethod(dataset);
		worker = new DefaultAnalysisWorker(m);
		worker.addPropertyChangeListener(this);
		ThreadManager.getInstance().submit(worker);
	}

	@Override
	public void finished() {

		LOGGER.log(Level.FINE, "Completed Hamming amalgamation");
		this.setProgressBarVisible(false);

		try {
			final ClusterAnalysisResult r = (ClusterAnalysisResult) worker.get();

			LOGGER.info("Found %s clusters in %s".formatted(r.getGroup().size(), dataset.getName()));

			UserActionController.getInstance().userActionEventReceived(
					new UserActionEvent(this, UserActionEvent.REFOLD_CONSENSUS,
							r.getDatasets()));

			UserActionController.getInstance()
					.userActionEventReceived(
							new UserActionEvent(this, UserActionEvent.SAVE, dataset));

			UIController.getInstance().fireClusterGroupAdded(dataset, r.getGroup());

		} catch (InterruptedException | ExecutionException e) {
			LOGGER.log(Level.SEVERE, "Error clustering dataset: %s".formatted(e.getMessage()), e);
			Thread.currentThread().interrupt();
		}

		super.finished();

	}


}
