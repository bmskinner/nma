package com.bmskinner.nma.analysis;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Change dataset scale
 * 
 * @author Ben Skinner
 *
 */
public class DatasetScaleChangeMethod extends MultipleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(DatasetScaleChangeMethod.class.getName());

	private final double newScale;

	/**
	 * Create with the dataset to update, and the new scale
	 * 
	 * @param dataset  the dataset to update
	 * @param newScale the new scale in pixels per micron
	 */
	public DatasetScaleChangeMethod(@NonNull IAnalysisDataset dataset, double newScale) {
		super(dataset);
		this.newScale = newScale;
	}

	/**
	 * Create with the datasets to update, and the new scale
	 * 
	 * @param dataset  the datasets to update
	 * @param newScale the new scale in pixels per micron
	 */
	public DatasetScaleChangeMethod(@NonNull List<IAnalysisDataset> datasets, double newScale) {
		super(datasets);
		this.newScale = newScale;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();
		return new DefaultAnalysisResult(datasets);
	}

	private void run() {

		if (newScale <= 0) {
			LOGGER.warning(() -> "New scale (%d) is not valid, ignoring".formatted(newScale));
			return;
		}

		for (IAnalysisDataset d : datasets) {
			d.setScale(newScale);
			fireProgressEvent();
		}
	}

}
