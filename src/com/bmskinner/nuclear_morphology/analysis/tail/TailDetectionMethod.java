package com.bmskinner.nuclear_morphology.analysis.tail;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.components.Flagellum;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.IMutableCell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;

public class TailDetectionMethod extends AbstractAnalysisMethod {
	
	private final File folder;
	private final int channel;
		
	public TailDetectionMethod(IAnalysisDataset dataset, File folder, int channel){
		super(dataset);
		this.folder = folder;
		this.channel = channel;
	}
	
	@Override
	public IAnalysisResult call() throws Exception {

		run();		
		IAnalysisResult r = new DefaultAnalysisResult(dataset);
		return r;
	}
	
	private  void run() {	
	
		log("Beginning tail detection");

		try{
			int progress = 0;
			for(ICell c : dataset.getCollection().getCells()){

				IMutableCell cell = (IMutableCell) c;
				
				Nucleus n = c.getNucleus();
				log("Looking for tails associated with nucleus "+n.getSourceFileName()+"-"+n.getNucleusNumber());
				
				// get the image in the folder with the same name as the
				// nucleus source image
				File imageFile = new File(folder + File.separator + n.getSourceFileName());
				fine("Tail in: "+imageFile.getAbsolutePath());
//				SpermTail tail = null;
				
				TailDetector finder = new TailDetector(dataset.getAnalysisOptions().getDetectionOptions(IAnalysisOptions.SPERM_TAIL).getCannyOptions(), 
						channel);
				
				// attempt to detect the tails in the image
				try{
					List<Flagellum> tails = finder.detectTail(imageFile, n);
					
					for(Flagellum tail : tails){
						cell.addFlagellum(tail);
					}
					
				} catch(Exception e){
					error("Error detecting tail", e);
				}
				
				fireProgressEvent();
			}
		} catch (Exception e){
			error("Error in tubulin tail detection", e);
		}
	}

}
