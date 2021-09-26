package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.signals.PairedSignalGroups;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalGroupMergeMethod;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.dialogs.DatasetMergingDialog;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Trigger signal merging action
 * @author bs19022
 * @since 1.16.1
 *
 */
public class MergeSignalsAction extends SingleDatasetResultAction {
	
	private static final Logger LOGGER = Logger.getLogger(MergeSignalsAction.class.getName());
	
	private static final @NonNull String PROGRESS_BAR_LABEL   = "Merging";
    private static final int NUMBER_OF_STEPS = 100;

	/**
	 * Construct with a daataset of signals to be merged
	 * @param dataset
	 * @param acceptor
	 * @param eh
	 */
	public MergeSignalsAction(IAnalysisDataset dataset, ProgressBarAcceptor acceptor,
			EventHandler eh) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor, eh);
	}

	@Override
	public void run() {

		LOGGER.fine("Running signal merging action");
		try {
			// Choose the paired signals
			LOGGER.fine("Creating signal selection dialog");
			List<IAnalysisDataset> datasets = new ArrayList<>();
			datasets.add(dataset);
			DatasetMergingDialog dialog = new DatasetMergingDialog(datasets);
			PairedSignalGroups pairs = dialog.getPairedSignalGroups();

			if(pairs.isEmpty()) {
				LOGGER.fine("No signal pairs chosen for merging, cancelling");
				cancel();
			} else {
				LOGGER.fine("Fetched signal pairs to merge");
				IAnalysisMethod m = new SignalGroupMergeMethod(dataset, pairs);

				worker = new DefaultAnalysisWorker(m, NUMBER_OF_STEPS);
				worker.addPropertyChangeListener(this);
				ThreadManager.getInstance().submit(worker);
			}
		} catch (Exception e) {
			cancel();
		}
	}
	
	 @Override
	    public void finished() {
	        try {
	            worker.get();
	        } catch (InterruptedException | ExecutionException e) {
	            LOGGER.warning("Error merging signals");
	            LOGGER.log(Loggable.STACK, "Error merging signals", e);
	            this.cancel();
	            return;
	        }
	        getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
	        super.finished();
	    }

}
