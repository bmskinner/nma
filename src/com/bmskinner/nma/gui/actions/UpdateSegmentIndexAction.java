package com.bmskinner.nma.gui.actions;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.profiles.UpdateSegmentIndexMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.events.UIController;

public class UpdateSegmentIndexAction extends SingleDatasetResultAction {
	private static final Logger LOGGER = Logger.getLogger(AddNuclearSignalAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Signal detection";

	private final UUID segmentId;
	private final int newIndex;

	/**
	 * Create a landmark update
	 * 
	 * @param dataset  the dataset to update
	 * @param lm       the landmark to update
	 * @param newIndex the new index in the median profile
	 * @param acceptor the progress bar container
	 */
	public UpdateSegmentIndexAction(@NonNull IAnalysisDataset dataset, @NonNull UUID segmentId,
			int newIndex, @NonNull ProgressBarAcceptor acceptor) {
		this(dataset, acceptor, segmentId, newIndex, null);
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
	public UpdateSegmentIndexAction(@NonNull IAnalysisDataset dataset,
			@NonNull ProgressBarAcceptor acceptor, @NonNull UUID segmentId, int newIndex,
			@Nullable CountDownLatch latch) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
		this.segmentId = segmentId;
		this.newIndex = newIndex;
		setLatch(latch);
	}

	@Override
	public void run() {

		IAnalysisMethod m = new UpdateSegmentIndexMethod(dataset, segmentId, newIndex);
		worker = new DefaultAnalysisWorker(m,
				dataset.getAllChildDatasets().size() + dataset.getCollection().size());

		this.setProgressMessage("Updating segment: " + dataset.getName());
		worker.addPropertyChangeListener(this);
		ThreadManager.getInstance().submit(worker);

	}

	@Override
	public void finished() {
		UIController.getInstance().fireProfilesUpdated(dataset);
		super.finished();
	}

}
