package com.bmskinner.nuclear_morphology.analysis.nucleus;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.IHoughDetectionOptions;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

import ij.process.ImageProcessor;


/**
 * This method finds lobes within nuclei. It is designed to work on 
 * neutrophils.
 * @author ben
 * @since 1.13.4
 *
 */
public class LobeDetectionMethod extends AbstractAnalysisMethod {

	private IHoughDetectionOptions options;
	
	public LobeDetectionMethod(IAnalysisDataset dataset, IHoughDetectionOptions op) {
		super(dataset);
		options = op;
	}

	@Override
	public IAnalysisResult call() throws Exception {

		if(dataset.getAnalysisOptions().getNucleusType().equals(NucleusType.NEUTROPHIL)){
			run();	
		} else {
			warn("Not a lobed nucleus type; cannot run lobe detection");
		}
			
		IAnalysisResult r = new DefaultAnalysisResult(dataset);
		return r;
	}
	
	private void run() {
		// For each cell
		// get the cytoplasm component image
		// get the circles based on threshold
		// print
		for(ICell cell : dataset.getCollection().getCells()){
			
			log("Running on cell "+cell.getId());
			
			ImageProcessor ip;
			try {
				
				log(cell.getCytoplasm().getSourceFileName());
				ip = cell.getCytoplasm().getImage();
				ImageFilterer ft = new ImageFilterer(ip)
						.runHoughCircleDetection(options);
				
				
			} catch (UnloadableImageException e) {
				warn("Unable to load cell image");
			}
			
			
			
		}
		
		
	}
	
	

}
