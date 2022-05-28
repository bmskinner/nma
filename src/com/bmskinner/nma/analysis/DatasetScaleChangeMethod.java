package com.bmskinner.nma.analysis;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Change dataset scale
 * 
 * @author ben
 *
 */
public class DatasetScaleChangeMethod extends MultipleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(DatasetScaleChangeMethod.class.getName());

	private final double newScale;

	public DatasetScaleChangeMethod(IAnalysisDataset dataset, double newScale) {
		super(dataset);
		this.newScale = newScale;
	}

	public DatasetScaleChangeMethod(@NonNull List<IAnalysisDataset> datasets, double newScale) {
		super(datasets);
		this.newScale = newScale;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();
		return new DefaultAnalysisResult(datasets);
	}

	private void run() throws Exception {

		if (newScale <= 0) {
			LOGGER.warning(String.format("New scale (%d) is not valid", newScale));
			return;
		}

		for (IAnalysisDataset d : datasets) {
			d.setScale(newScale);
			fireProgressEvent();
		}
	}

}
