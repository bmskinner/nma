package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.DetectionPipeline;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.FluoresentNucleusDetectionPipeline;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;

/**
 * Recursive task to find nuclei in a folder of images.
 * Called by the fluorescence nucleus detection method. Replaces the FileProcessingTask
 * used since 1.12.0
 * @author bms41
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class NucleusDetectionTask extends AbstractDetectionTask  {
		
	private NucleusDetectionTask(File folder, File[] files, ICellCollection collection, int low, int high, String outputFolder, IAnalysisOptions analysisOptions) {
		super(folder, files, collection, low, high, outputFolder, analysisOptions);
	}

	public NucleusDetectionTask(File folder, File[] files, ICellCollection collection, String outputFolder, IAnalysisOptions analysisOptions) {
		this(folder, files, collection, 0, files.length, outputFolder, analysisOptions);

	}

	protected void compute() {
		
		if (high - low < THRESHOLD){
			
			analyseFiles();
			
		} else {
			
			int mid = (low + high) >>> 1;

			List<NucleusDetectionTask> tasks = new ArrayList<NucleusDetectionTask>();

			NucleusDetectionTask task1 = new NucleusDetectionTask(folder, files, collection, low, mid, outputFolder, analysisOptions);
			NucleusDetectionTask task2 = new NucleusDetectionTask(folder, files, collection, mid, high, outputFolder, analysisOptions);

			task1.addProgressListener(this);
			task2.addProgressListener(this);

			tasks.add(task1);
			tasks.add(task2);

			NucleusDetectionTask.invokeAll(tasks);

		}
	}
		
	
	protected void analyseFile(File file){
		boolean ok = checkFile(file);

		  if(ok){
			  try {

				  // put folder creation here so we don't make folders we won't use (e.g. empty directory analysed)
				  makeFolder(folder);
				  
				  log("File:  "+file.getName());
				  
				  // Build a pipline for the image
				  DetectionPipeline<ICell> pipe = new FluoresentNucleusDetectionPipeline(analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS),
						  file, analysisOptions.getNucleusType(), analysisOptions.getProfileWindowProportion());
				  
				  // Run each step of the pipeline without sampling intermediate results
				  List<ICell> cells = pipe.kuwaharaFilter()
						  .flatten()
						  .edgeDetect()
						  .gapClose()
						  .findInImage();
				  				  
				  if(cells.isEmpty()){
					  log("  No nuclei detected in image");
				  } else {

					  for(ICell cell : cells){

						  
						  collection.addCell(cell);
						  log("  Added nucleus "+cell.getNucleus().getNucleusNumber());
					  }
					  log("  Added "+cells.size()+" nuclei");
				  }

			  } catch (Exception e) { 
				  warn("Error processing file");
				  stack("Error in image processing: "+e.getMessage(), e);
			  } 
			  
			  fireProgressEvent();
			  
		  } 
	   }

}