package com.bmskinner.nma.gui.actions;

import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DatasetScaleChangeMethod;
import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.events.UIController;

public class DatasetScaleChangeAction extends MultiDatasetResultAction {

	private static final @NonNull String PROGRESS_BAR_LABEL = "Updating scale";

	/**
	 * Create a scale update
	 * 
	 * @param dataset  the dataset to update
	 * @param newScale the new scale
	 * @param acceptor the progress bar container
	 */
	public DatasetScaleChangeAction(@NonNull IAnalysisDataset dataset,
			@NonNull ProgressBarAcceptor acceptor) {
		this(List.of(dataset), acceptor);
	}

	/**
	 * Create a scale update
	 * 
	 * @param datasets the datasets to update
	 * @param newScale the new scale
	 * @param acceptor the progress bar container
	 */
	public DatasetScaleChangeAction(@NonNull List<IAnalysisDataset> datasets,
			@NonNull ProgressBarAcceptor acceptor) {
		super(datasets, PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {

		try {
			double d0scale = 1;
			double currentScale = 1;

			// is there a common scale in the datasets already?
			// Get the first dataset scale
			Optional<IAnalysisOptions> d0Options = datasets.get(0).getAnalysisOptions();
			if (d0Options.isPresent()) {
				Optional<HashOptions> d0NucleusOptions = d0Options.get()
						.getNucleusDetectionOptions();
				if (d0NucleusOptions.isPresent()) {
					d0scale = d0NucleusOptions.get().getDouble(HashOptions.SCALE);
				}
			}

			// check any other datasets match
			final double d0scaleFinal = d0scale;
			boolean allMatch = datasets.stream().allMatch(d -> {
				Optional<IAnalysisOptions> dOptions = d.getAnalysisOptions();
				if (dOptions.isPresent()) {
					Optional<HashOptions> dNucleusOptions = dOptions.get()
							.getNucleusDetectionOptions();
					if (dNucleusOptions.isPresent()) {
						return dNucleusOptions.get().getDouble(HashOptions.SCALE) == d0scaleFinal;
					}
				}
				return false;
			});
			if (allMatch)
				currentScale = d0scale;

			// request the new scale
			double newScale = new DefaultInputSupplier().requestDouble("Pixels per micron",
					currentScale, 1d, 100000d, 1d);
			if (newScale <= 0) { // don't allow a scale to cause divide by zero errors
				this.cancel();
				return;
			}

			IAnalysisMethod m = new DatasetScaleChangeMethod(datasets, newScale);

			// Each nucleus, plus the profile collection, plus consensus nuclei
			// plus the main dataset plus one so the bar does not appear to hang
			// on complete
			int progressSteps = datasets.size() + 1;
			worker = new DefaultAnalysisWorker(m, progressSteps);
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);

		} catch (RequestCancelledException e) {
			this.cancel();
		}

	}

	@Override
	public void finished() {
		UIController.getInstance().fireScaleUpdated(datasets);
		super.finished();
	}

}
