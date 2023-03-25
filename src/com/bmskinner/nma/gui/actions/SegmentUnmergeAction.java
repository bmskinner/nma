package com.bmskinner.nma.gui.actions;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.profiles.SegmentUnmergeMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.events.UIController;

/**
 * Unmerge segments in a dataset collection and its children, as long as the
 * collection is real.
 * 
 * @author bs19022
 *
 */
public class SegmentUnmergeAction extends SingleDatasetResultAction {
	private static final Logger LOGGER = Logger.getLogger(SegmentUnmergeAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Merging segments";

	private final UUID segId;

	/**
	 * Create a segment unmerge action
	 * 
	 * @param dataset  the dataset to update
	 * @param segId    the segment to unmerge
	 * @param acceptor the progress bar container
	 */
	public SegmentUnmergeAction(@NonNull IAnalysisDataset dataset, @NonNull UUID segId,
			@NonNull ProgressBarAcceptor acceptor) {
		this(dataset, acceptor, segId, null);
	}

	/**
	 * Create a segment unmerge action
	 * 
	 * @param dataset  the dataset to update
	 * @param segId    the segment to unmerge
	 * @param acceptor the progress bar container
	 * @param latch    a countdown latch
	 */
	public SegmentUnmergeAction(@NonNull IAnalysisDataset dataset,
			@NonNull ProgressBarAcceptor acceptor, @NonNull UUID segId,
			@Nullable CountDownLatch latch) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
		this.segId = segId;
		setLatch(latch);
	}

	@Override
	public void run() {

		IAnalysisMethod m = new SegmentUnmergeMethod(dataset, segId);

		// Each nucleus, plus the profile collection, plus consensus nuclei
		// plus the main dataset plus one so the bar does not appear to hang
		// on complete
		int progressSteps = dataset.getCollection().size()
				+ (dataset.getAllChildDatasets().size() * 2) + 2;
		worker = new DefaultAnalysisWorker(m, progressSteps);

		this.setProgressMessage("Unmerging segment: " + dataset.getName());
		worker.addPropertyChangeListener(this);
		ThreadManager.getInstance().submit(worker);

	}

	@Override
	public void finished() {
		UIController.getInstance().fireProfilesUpdated(dataset);
		UIController.getInstance().fireProfilesUpdated(dataset.getAllChildDatasets());
		super.finished();
	}

}
