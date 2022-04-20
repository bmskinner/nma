package com.bmskinner.nuclear_morphology.gui.runnables;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.actions.ExportDatasetAction;
import com.bmskinner.nuclear_morphology.gui.actions.RefoldNucleusAction;
import com.bmskinner.nuclear_morphology.gui.actions.RunProfilingAction;
import com.bmskinner.nuclear_morphology.gui.actions.RunSegmentationAction;
import com.bmskinner.nuclear_morphology.gui.actions.SingleDatasetResultAction;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UIController;

/**
 * A runnable linking the main actions of an analysis: profiling, segmentation,
 * refolding and saving.
 * 
 * @author ben
 *
 */
public class MorphologyAnalysis implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(MorphologyAnalysis.class.getName());

	private final List<IAnalysisDataset> datasets;
	private final ProgressBarAcceptor pa;

	/**
	 * Create with datasets to analyse and an acceptor for progress bars
	 * 
	 * @param datasets
	 * @param pa
	 */
	public MorphologyAnalysis(List<IAnalysisDataset> datasets, ProgressBarAcceptor pa) {
		this.datasets = datasets;
		this.pa = pa;
	}

	@Override
	public void run() {
		LOGGER.finer("Starting morphology runnable");
		final CountDownLatch profileLatch = new CountDownLatch(1);
		final CountDownLatch segmentLatch = new CountDownLatch(1);
		final CountDownLatch refoldLatch = new CountDownLatch(1);
		final CountDownLatch saveLatch = new CountDownLatch(1);

		new Thread(() -> { // run profiling
			LOGGER.fine("Starting profiling");
			new RunProfilingAction(datasets, SingleDatasetResultAction.NO_FLAG, pa, profileLatch).run();

		}).start();

		new Thread(() -> { // wait for profiling and run segmentation
			try {
				profileLatch.await();
				LOGGER.fine("Starting segmentation");
				new RunSegmentationAction(datasets, MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH, SingleDatasetResultAction.NO_FLAG, pa,
						segmentLatch).run();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}).start();

		new Thread(() -> { // wait for segmentation and run refolding
			try {
				segmentLatch.await();
				LOGGER.finer("Starting refolding action");
				new RefoldNucleusAction(datasets, pa, refoldLatch).run();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}).start();

		new Thread(() -> { // wait for refolding and run save
			try {
				refoldLatch.await();
				LOGGER.finer("Starting save action");
				new ExportDatasetAction(datasets, pa, saveLatch).run();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}).start();

		new Thread(() -> { // wait for save and recache charts
			try {
				saveLatch.await();
				LOGGER.finer("Adding new dataset");
				UIController.getInstance().fireDatasetAdded(datasets);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}).start();

	}

}
