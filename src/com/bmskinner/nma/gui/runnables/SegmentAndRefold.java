package com.bmskinner.nma.gui.runnables;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.actions.CreateConsensusAction;
import com.bmskinner.nma.gui.actions.RunSegmentationAction;

/**
 * Utility runnable that resegments the given dataset from scratch, and builds a
 * new consensus nucleus
 * 
 * @author Ben Skinner
 *
 */
public class SegmentAndRefold implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(SegmentAndRefold.class.getName());

	private final IAnalysisDataset dataset;
	private final ProgressBarAcceptor acceptor;

	public SegmentAndRefold(@NonNull IAnalysisDataset dataset,
			@NonNull ProgressBarAcceptor acceptor) {
		this.dataset = dataset;
		this.acceptor = acceptor;
	}

	@Override
	public void run() {
		final CountDownLatch segmentLatch = new CountDownLatch(1);

		new Thread(() -> { // run segmentation
			new RunSegmentationAction(dataset,
					MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH, acceptor,
					segmentLatch).run();
		}).start();

		new Thread(() -> { // wait for segmentation and run refolding
			try {
				segmentLatch.await();
				new CreateConsensusAction(dataset, acceptor, null).run();
			} catch (InterruptedException e) {
				LOGGER.warning("Error making consensus in " + dataset.getName());
				Thread.currentThread().interrupt();
				return;
			}
		}).start();

	}

}
