package com.bmskinner.nma.gui.actions;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.profiles.TextDatasetProfilingMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;

public class TextDatasetProfilingAction  extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(RunProfilingAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Profiling";

	public TextDatasetProfilingAction(@NonNull final IAnalysisDataset dataset,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
	}

	public TextDatasetProfilingAction(@NonNull final List<IAnalysisDataset> list,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(list, PROGRESS_BAR_LABEL, acceptor);
	}

	public TextDatasetProfilingAction(@NonNull final IAnalysisDataset dataset, 
			@NonNull final ProgressBarAcceptor acceptor, CountDownLatch latch) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
		this.setLatch(latch);

	}

	public TextDatasetProfilingAction(@NonNull final List<IAnalysisDataset> list, 
			@NonNull final ProgressBarAcceptor acceptor, CountDownLatch latch) {
		super(list, PROGRESS_BAR_LABEL, acceptor);
		this.setLatch(latch);

	}



	@Override
	public void run() {
		try {
			this.setProgressMessage("Profiling: " + dataset.getName());
			IAnalysisMethod method = new TextDatasetProfilingMethod(dataset);
			worker = new DefaultAnalysisWorker(method);
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error in text profiling: %s".formatted(e.getMessage()), e);
			this.cancel();
		}
	}

	@Override
	public void finished() {

		// ensure the progress bar gets hidden even if it is not removed
		this.setProgressBarVisible(false);

		Runnable task = () -> {
			
			DatasetListManager.getInstance().addDataset(dataset);
			UIController.getInstance().fireDatasetAdded(dataset);
			
			if(dataset.getAnalysisOptions().get()
					.getProfilingOptions()
					.getBoolean(HashOptions.IS_SEGMENT_PROFILES)) {
				UserActionController.getInstance().userActionEventReceived(
						new UserActionEvent(this, UserActionEvent.SEGMENTATION_ACTION,
								dataset));
				
			}

			
			UserActionController.getInstance().userActionEventReceived(
					new UserActionEvent(this, UserActionEvent.SAVE,
							dataset));

			// if no list was provided, or no more entries remain,
			// call the finish
			if (!hasRemainingDatasetsToProcess()) {

				cancel();
				countdownLatch();

			} else {
				// otherwise analyse the next item in the list
				cancel(); // remove progress bar

				Runnable p = new TextDatasetProfilingAction(getRemainingDatasetsToProcess(), progressAcceptors.get(0),
						getLatch().get());
				p.run();

			}
		};

		ThreadManager.getInstance().execute(task);

	}

}
