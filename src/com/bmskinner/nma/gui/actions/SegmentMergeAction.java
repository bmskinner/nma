package com.bmskinner.nma.gui.actions;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.profiles.SegmentMergeMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.events.UIController;

/**
 * Action to merge segments in a dataset
 * 
 * @author bs19022
 *
 */
public class SegmentMergeAction extends SingleDatasetResultAction {
	private static final Logger LOGGER = Logger.getLogger(SegmentMergeAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Merging segments";

	private final UUID seg0Id;
	private final UUID seg1Id;

	/**
	 * Create a segment merge action
	 * 
	 * @param dataset  the dataset to update
	 * @param lm       the landmark to update
	 * @param newIndex the new index in the median profile
	 * @param acceptor the progress bar container
	 */
	public SegmentMergeAction(@NonNull IAnalysisDataset dataset, @NonNull UUID segId0,
			UUID segId1, @NonNull ProgressBarAcceptor acceptor) {
		this(dataset, acceptor, segId0, segId1, null);
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
	public SegmentMergeAction(@NonNull IAnalysisDataset dataset,
			@NonNull ProgressBarAcceptor acceptor, @NonNull UUID segId0,
			UUID segId1, @Nullable CountDownLatch latch) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
		this.seg0Id = segId0;
		this.seg1Id = segId1;
		setLatch(latch);
	}

	@Override
	public void run() {

		IAnalysisMethod m = new SegmentMergeMethod(dataset, seg0Id, seg1Id);

		// Each nucleus, plus the profile collection, plus consensus nuclei
		// plus the main dataset plus one so the bar does not appear to hang
		// on complete
		int progressSteps = dataset.getCollection().size()
				+ (dataset.getAllChildDatasets().size() * 2) + 2;
		worker = new DefaultAnalysisWorker(m, progressSteps);

		this.setProgressMessage("Merging segments: " + dataset.getName());
		worker.addPropertyChangeListener(this);
		ThreadManager.getInstance().submit(worker);

	}

	@Override
	public void finished() {
		UIController.getInstance().fireProfilesUpdated(dataset);
		super.finished();
	}

}
