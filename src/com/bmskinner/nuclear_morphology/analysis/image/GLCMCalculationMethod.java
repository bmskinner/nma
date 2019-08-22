package com.bmskinner.nuclear_morphology.analysis.image;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMResult;
import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMValue;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

public class GLCMCalculationMethod extends SingleDatasetAnalysisMethod {

	public GLCMCalculationMethod(IAnalysisDataset dataset) {
		super(dataset);
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();
		return new DefaultAnalysisResult(dataset);
	}

	private void run() throws Exception {
		GLCM glcm = new GLCM();
				
		List<GLCMResult> results = new ArrayList<>();
		
		// Do all cells in an image at a time
		// Should store the image as a weak reference in the nucleus	
		for(File f : dataset.getCollection().getImageFiles()) {
			for(ICell c : dataset.getCollection().getCells(f)) {
				for(Nucleus n : c.getNuclei()) {
					GLCMResult r = glcm.calculate(n);
					results.add(r);
					for(GLCMValue v : GLCMValue.values())
						n.setStatistic(v.toStat(), r.get(v));
				}
				fireProgressEvent();
			}
		}
		
		// Clear stats caches
		for(PlottableStatistic stat : GLCMValue.toStats()) {
			dataset.getCollection().clear(stat, CellularComponent.NUCLEUS);
			for(IAnalysisDataset child : dataset.getAllChildDatasets()) {
				child.getCollection().clear(stat, CellularComponent.NUCLEUS);
			}
		}
	}
}
