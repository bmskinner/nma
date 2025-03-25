package com.bmskinner.nma.gui.runnables;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.actions.ExportDatasetAction;
import com.bmskinner.nma.logging.Loggable;

/**
 * Action to save all open datasets
 * 
 * @author Ben Skinner
 *
 */
public class SaveAllDatasets implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(MorphologyAnalysis.class.getName());

	private final List<IAnalysisDataset> datasets;
	private final ProgressBarAcceptor pa;

	/**
	 * Create with datasets to analyse and an acceptor for progress bars
	 * 
	 * @param datasets
	 * @param pa
	 */
	public SaveAllDatasets(List<IAnalysisDataset> datasets, ProgressBarAcceptor pa) {
		this.datasets = datasets;
		this.pa = pa;
	}

	@Override
	public void run() {

		// Check which datasets are root and which are children. We only
		// want to save to the same path once
		Set<IAnalysisDataset> roots = DatasetListManager.getInstance().getRootParents(datasets);
		LOGGER.fine("Starting save runnable for " + roots.size() + " datasets");
		final CountDownLatch latch = new CountDownLatch(1);
		for (IAnalysisDataset d : roots) {
			new ExportDatasetAction(d, pa, latch, false).run();

			// Save sequentially, not all at once
			try {
				latch.await();
			} catch (InterruptedException e) {
				LOGGER.log(Loggable.STACK, "Interruption to thread", e);
				Thread.currentThread().interrupt();
			}
		}
	}
}
