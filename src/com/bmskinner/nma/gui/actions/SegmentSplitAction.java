package com.bmskinner.nma.gui.actions;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.profiles.SegmentSplitMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.events.UIController;

/**
 * Split a segment in a dataset collection and its children, as long as the
 * collection is real.
 * 
 * @author bs19022
 *
 */
public class SegmentSplitAction extends SingleDatasetResultAction {


	private static final @NonNull String PROGRESS_BAR_LABEL = "Splitting segments";

	private final UUID segId;

	/**
	 * Create a segment split action
	 * 
	 * @param dataset  the dataset to update
	 * @param segId    the segment to split
	 * @param acceptor the progress bar container
	 */
	public SegmentSplitAction(@NonNull IAnalysisDataset dataset, @NonNull UUID segId,
			@NonNull ProgressBarAcceptor acceptor) {
		this(dataset, acceptor, segId, null);
	}

	/**
	 * Create a segment split action
	 * 
	 * @param dataset  the dataset to update
	 * @param segId    the segment to split
	 * @param acceptor the progress bar container
	 * @param latch    a countdown latch
	 */
	public SegmentSplitAction(@NonNull IAnalysisDataset dataset,
			@NonNull ProgressBarAcceptor acceptor, @NonNull UUID segId,
			@Nullable CountDownLatch latch) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
		this.segId = segId;
		setLatch(latch);
	}

	@Override
	public void run() {

		IAnalysisMethod m = new SegmentSplitMethod(dataset, segId);

		// Each nucleus, plus the profile collection, plus consensus nuclei
		// plus the main dataset plus one so the bar does not appear to hang
		// on complete
		int progressSteps = dataset.getCollection().size()
				+ (dataset.getAllChildDatasets().size() * 2) + 2;
		worker = new DefaultAnalysisWorker(m, progressSteps);

		this.setProgressMessage("Splitting segment: " + dataset.getName());
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
