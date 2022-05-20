package com.bmskinner.nma.gui.actions;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.profiles.UpdateLandmarkMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.runnables.SegmentAndRefold;

public class UpdateLandmarkAction extends SingleDatasetResultAction {
	private static final Logger LOGGER = Logger.getLogger(UpdateLandmarkAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Updating landmark";

	private final Landmark lm;
	private final int newIndex;

	/**
	 * Create a landmark update
	 * 
	 * @param dataset  the dataset to update
	 * @param lm       the landmark to update
	 * @param newIndex the new index in the median profile
	 * @param acceptor the progress bar container
	 */
	public UpdateLandmarkAction(@NonNull IAnalysisDataset dataset, @NonNull Landmark lm,
			int newIndex, @NonNull ProgressBarAcceptor acceptor) {
		this(dataset, acceptor, lm, newIndex, null);
	}

	/**
	 * Create a landmark update
	 * 
	 * @param dataset  the dataset to update
	 * @param lm       the landmark to update
	 * @param newIndex the new index in the median profile
	 * @param acceptor the progress bar container
	 * @param latch    a countdown latch
	 */
	public UpdateLandmarkAction(@NonNull IAnalysisDataset dataset,
			@NonNull ProgressBarAcceptor acceptor, @NonNull Landmark lm, int newIndex,
			@Nullable CountDownLatch latch) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
		this.lm = lm;
		this.newIndex = newIndex;
		setLatch(latch);
	}

	@Override
	public void run() {

		IAnalysisMethod m = new UpdateLandmarkMethod(dataset, lm, newIndex);
		worker = new DefaultAnalysisWorker(m, dataset.getAllChildDatasets().size() + 1);

		this.setProgressMessage("Updating " + lm + ": " + dataset.getName());
		worker.addPropertyChangeListener(this);
		ThreadManager.getInstance().submit(worker);

	}

	@Override
	public void finished() {

		cleanup(); // remove the property change listener
		try {

			Landmark rp = dataset.getCollection().getRuleSetCollection()
					.getLandmark(OrientationMark.REFERENCE).get();

			if (rp.equals(lm)) {
				Runnable r = new SegmentAndRefold(dataset, progressAcceptors.get(0));
				ThreadManager.getInstance().execute(r);
			} else {
				UIController.getInstance().fireProfilesUpdated(dataset);
			}
		} finally {
			super.finished();
		}
	}
}
