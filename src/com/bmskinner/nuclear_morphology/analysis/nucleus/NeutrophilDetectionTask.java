/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.DetectionPipeline;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.NeutrophilDetectionPipeline;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;

/**
 * Detection task for neutrophils that splits the images to be scanned recursively
 * @author bms41
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class NeutrophilDetectionTask extends AbstractDetectionTask  {
		
	private NeutrophilDetectionTask(File folder, File[] files, ICellCollection collection, int low, int high, String outputFolder, IAnalysisOptions analysisOptions) {
		super(folder, files, collection, low, high, outputFolder, analysisOptions);
	}

	public NeutrophilDetectionTask(File folder, File[] files, ICellCollection collection, String outputFolder, IAnalysisOptions analysisOptions) {
		this(folder, files, collection, 0, files.length, outputFolder, analysisOptions);

	}

	protected void compute() {
		
		if (high - low < THRESHOLD){
			
			analyseFiles();
			
		} else {
			
			finest("Splitting task");
			
			int mid = (low + high) >>> 1;

			List<NeutrophilDetectionTask> tasks = new ArrayList<NeutrophilDetectionTask>();

			NeutrophilDetectionTask task1 = new NeutrophilDetectionTask(folder, files, collection, low, mid, outputFolder, analysisOptions);
			NeutrophilDetectionTask task2 = new NeutrophilDetectionTask(folder, files, collection, mid, high, outputFolder, analysisOptions);

			task1.addProgressListener(this);
			task2.addProgressListener(this);

			tasks.add(task1);
			tasks.add(task2);

			NeutrophilDetectionTask.invokeAll(tasks);

		}
	}
	

	protected void analyseFile(File file){
		
		finest("Analysing file "+file.getAbsolutePath());
		boolean ok = checkFile(file);

		  if(ok){
			  try {

				  // put folder creation here so we don't make folders we won't use (e.g. empty directory analysed)
				  makeFolder(folder);
				  
				  log("File:  "+file.getName());
				  
				  // Build a pipline for the image
				  DetectionPipeline<ICell> pipe = new NeutrophilDetectionPipeline(analysisOptions.getDetectionOptions(IAnalysisOptions.CYTOPLASM),
						  analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS),
						  file,
						  analysisOptions.getProfileWindowProportion());
				  
				  // Run each step of the pipeline without sampling intermediate results
				  List<ICell> cells = pipe.findInImage();
				  				  
				  if(cells.isEmpty()){
					  log("  No cells detected in image");
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
