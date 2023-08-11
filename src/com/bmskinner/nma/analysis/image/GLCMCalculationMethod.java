package com.bmskinner.nma.analysis.image;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.analysis.image.GLCM.GLCMParameter;
import com.bmskinner.nma.analysis.image.GLCM.GLCMTile;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;

public class GLCMCalculationMethod extends SingleDatasetAnalysisMethod {

	public GLCMCalculationMethod(IAnalysisDataset dataset) {
		super(dataset);
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();
		return new DefaultAnalysisResult(dataset);
	}

	/**
	 * Calculate the GLCM value across the entire nucleus image
	 * 
	 * @throws Exception
	 */
	private void run() throws Exception {
		GLCM glcm = new GLCM(GLCM.defaultOptions());

		List<GLCMTile> results = new ArrayList<>();

		// Do all cells in an image at a time
		// This should store the image as a weak reference in the nucleus
		for (File f : dataset.getCollection().getImageFiles()) {
			for (ICell c : dataset.getCollection().getCells(f)) {
				for (Nucleus n : c.getNuclei()) {
					GLCMTile r = glcm.calculate(n);
					results.add(r);
					for (GLCMParameter v : GLCMParameter.values())
						n.setMeasurement(v.toMeasurement(), r.get(v));
				}
				fireProgressEvent();
			}
		}

		// Clear stats caches
		for (Measurement stat : GLCMParameter.toStats()) {
			dataset.getCollection().clear(stat, CellularComponent.NUCLEUS);
			for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
				child.getCollection().clear(stat, CellularComponent.NUCLEUS);
			}
		}
	}
}
