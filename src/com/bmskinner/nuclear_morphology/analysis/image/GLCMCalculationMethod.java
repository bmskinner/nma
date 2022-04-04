package com.bmskinner.nuclear_morphology.analysis.image;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMParameter;
import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMTile;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

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
	 * Calculate the GLCM value across the entire
	 * nucleus image
	 * @throws Exception
	 */
	private void run() throws Exception {
		GLCM glcm = new GLCM();
				
		List<GLCMTile> results = new ArrayList<>();
		
		// Do all cells in an image at a time
		// This should store the image as a weak reference in the nucleus	
		for(File f : dataset.getCollection().getImageFiles()) {
			for(ICell c : dataset.getCollection().getCells(f)) {
				for(Nucleus n : c.getNuclei()) {
					GLCMTile r = glcm.calculate(n);
					results.add(r);
					for(GLCMParameter v : GLCMParameter.values())
						n.setMeasurement(v.toStat(), r.get(v));
				}
				fireProgressEvent();
			}
		}
		
		// Clear stats caches
		for(Measurement stat : GLCMParameter.toStats()) {
			dataset.getCollection().clear(stat, CellularComponent.NUCLEUS);
			for(IAnalysisDataset child : dataset.getAllChildDatasets()) {
				child.getCollection().clear(stat, CellularComponent.NUCLEUS);
			}
		}
	}
}
