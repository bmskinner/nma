package com.bmskinner.nma.gui.actions;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.nucleus.CellCollectionFilteringMethod;
import com.bmskinner.nma.analysis.nucleus.PoorEdgeDetectionProfilePredicate;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;

public class FilterPoorEdgeDetectionCellsAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger
			.getLogger(FilterPoorEdgeDetectionCellsAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Filtering poor edge detection";

	public FilterPoorEdgeDetectionCellsAction(@NonNull List<IAnalysisDataset> datasets,
			@NonNull CountDownLatch latch,
			@NonNull ProgressBarAcceptor acceptor) {
		super(datasets, PROGRESS_BAR_LABEL, acceptor);
		this.setLatch(latch);
	}

	public FilterPoorEdgeDetectionCellsAction(@NonNull IAnalysisDataset dataset,
			@NonNull ProgressBarAcceptor acceptor) {
		super(dataset, dataset.getName() + ": " + PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {

		LOGGER.fine("Filtering on poor edge detection");
		try {
			Predicate<ICell> profilePredicate = new PoorEdgeDetectionProfilePredicate(
					dataset.getAnalysisOptions().get().getRuleSetCollection().getOtherOptions());

			IAnalysisMethod m = new CellCollectionFilteringMethod(dataset, profilePredicate,
					dataset.getName() + "_passing_edge_detection");
			worker = new DefaultAnalysisWorker(m, dataset.getCollection().size());
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);
		} catch (IllegalArgumentException | MissingOptionException e) {
			LOGGER.log(Level.SEVERE, "Unable to create edge filterer: %s".formatted(e.getMessage()),
					e);
			cancel();
		}
	}

	@Override
	public void finished() {

		try {
			IAnalysisResult r = worker.get();

			LOGGER.fine("Dataset has " + dataset.getChildCount() + " children");

			for (IAnalysisDataset d : r.getDatasets()) {
				for (IAnalysisDataset c : d.getAllChildDatasets()) {

					if (!c.getCollection().hasConsensus())
						UserActionController.getInstance().userActionEventReceived(
								new UserActionEvent(this, UserActionEvent.REFOLD_CONSENSUS,
										c));

					UIController.getInstance().fireDatasetAdded(c);
				}
			}

			if (!hasRemainingDatasetsToProcess()) {
				super.finished();
				countdownLatch();
			} else {
				// otherwise analyse the next item in the list
				cancel(); // remove progress bar
				new FilterPoorEdgeDetectionCellsAction(getRemainingDatasetsToProcess(),
						getLatch().get(),
						progressAcceptors.get(0)).run();

			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,
					"Unable to filter cells with poor edge detection: " + e.getMessage(), e);
		}

	}
}
