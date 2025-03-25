package com.bmskinner.nma.gui.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.signals.PairedSignalGroups;
import com.bmskinner.nma.analysis.signals.SignalGroupMergeMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.dialogs.SignalPairMergingDialog;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.logging.Loggable;

/**
 * Trigger signal merging action
 * 
 * @author Ben Skinner
 * @since 1.16.1
 *
 */
public class MergeSignalsAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(MergeSignalsAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Merging";
	private static final int NUMBER_OF_STEPS = 100;

	/**
	 * Construct with a daataset of signals to be merged
	 * 
	 * @param dataset
	 * @param acceptor
	 * @param eh
	 */
	public MergeSignalsAction(IAnalysisDataset dataset, ProgressBarAcceptor acceptor) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {

		LOGGER.fine("Running signal merging action");
		try {
			// Choose the paired signals
			LOGGER.fine("Creating signal selection dialog");
			List<IAnalysisDataset> datasets = new ArrayList<>();
			datasets.add(dataset);
			SignalPairMergingDialog dialog = new SignalPairMergingDialog(datasets);
			PairedSignalGroups pairs = dialog.getPairedSignalGroups();

			if (pairs.isEmpty()) {
				LOGGER.fine("No signal pairs chosen for merging, cancelling");
				super.finished();
			} else {
				LOGGER.fine("Fetched signal pairs to merge");
				IAnalysisMethod m = new SignalGroupMergeMethod(dataset, pairs);

				worker = new DefaultAnalysisWorker(m, NUMBER_OF_STEPS);
				worker.addPropertyChangeListener(this);
				ThreadManager.getInstance().submit(worker);
			}
		} catch (Exception e) {
			LOGGER.fine("Error in signal merging action: " + e.getMessage());
			super.finished();
		}
	}

	@Override
	public void finished() {
		try {
			worker.get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.log(Loggable.STACK, "Error merging signals", e);
			this.cancel();
			Thread.currentThread().interrupt();
			return;
		}
		UIController.getInstance().fireDatasetAdded(dataset);
		UIController.getInstance().fireNuclearSignalUpdated(dataset);
		super.finished();
	}

}
