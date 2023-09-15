package com.bmskinner.nma.gui.actions;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.image.CellHistogramCalculationMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.events.UIController;

/**
 * Create an action to run histogram calculation and signal the UI
 * 
 * @author ben
 *
 */
public class CalculateCellHistogramAction extends SingleDatasetResultAction {

	private static final @NonNull String PROGRESS_BAR_LABEL = "Calculating histograms";

	public CalculateCellHistogramAction(@NonNull List<IAnalysisDataset> datasets, @NonNull CountDownLatch latch,
			@NonNull ProgressBarAcceptor acceptor) {
		super(datasets, PROGRESS_BAR_LABEL, acceptor);
		this.setLatch(latch);
	}

	public CalculateCellHistogramAction(@NonNull IAnalysisDataset dataset, @NonNull ProgressBarAcceptor acceptor) {
		super(dataset, dataset.getName() + ": " + PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {
		IAnalysisMethod m = new CellHistogramCalculationMethod(dataset);
		worker = new DefaultAnalysisWorker(m, dataset.getCollection().size());
		worker.addPropertyChangeListener(this);
		ThreadManager.getInstance().submit(worker);
	}

	@Override
	public void finished() {
		UIController.getInstance().fireGLCMDataAdded(List.of(dataset));
		if (!hasRemainingDatasetsToProcess()) {
			super.finished();
			countdownLatch();
		} else {
			// otherwise analyse the next item in the list
			cancel(); // remove progress bar
			new CalculateCellHistogramAction(getRemainingDatasetsToProcess(), getLatch().get(),
					progressAcceptors.get(0)).run();

		}

	}
}
