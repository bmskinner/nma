package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.bmskinner.nuclear_morphology.analysis.AbstractProgressAction;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.FluoresentNucleusDetectionPipeline;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;

import ij.ImageStack;
import ij.gui.PolygonRoi;

@SuppressWarnings("serial")
public class FileProcessingTask  extends AbstractProgressAction  {
	
	private final ICellCollection collection;
	private File[] files;
	private static final int THRESHOLD = 5; // number of images to handle per fork
	final int low, high;
//	private final NucleusDetector finder;
	private final String outputFolder;
	private final IAnalysisOptions analysisOptions;
	private final File folder;
	
	private FileProcessingTask(File folder, File[] files, ICellCollection collection, int low, int high, String outputFolder, IAnalysisOptions analysisOptions) {
		this.collection      = collection;
		this.files           = files;
		this.folder          = folder;
		this.low             = low;
		this.high            = high;
		this.outputFolder    = outputFolder;
		this.analysisOptions = analysisOptions;
//		this.finder          = new NucleusDetector( analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS), 
//				analysisOptions.getNucleusType(), 
//				analysisOptions.getProfileWindowProportion());
	}

	public FileProcessingTask(File folder, File[] files, ICellCollection collection, String outputFolder, IAnalysisOptions analysisOptions) {
		this(folder, files, collection, 0, files.length, outputFolder, analysisOptions);

	}

	protected void compute() {
		
		if (high - low < THRESHOLD){
			
			analyseFiles();
			
		} else {
			
			int mid = (low + high) >>> 1;

			List<FileProcessingTask> tasks = new ArrayList<FileProcessingTask>();

			FileProcessingTask task1 = new FileProcessingTask(folder, files, collection, low, mid, outputFolder, analysisOptions);
			FileProcessingTask task2 = new FileProcessingTask(folder, files, collection, mid, high, outputFolder, analysisOptions);

			task1.addProgressListener(this);
			task2.addProgressListener(this);

			tasks.add(task1);
			tasks.add(task2);

			FileProcessingTask.invokeAll(tasks);

		}
	}
	
	void analyseFiles() {
		
		for(int i=low; i<high; i++){
			analyseFile(files[i]);
		}
		
	}
	
	
	private void analyseFile(File file){
		boolean ok = checkFile(file);

		  if(ok){
			  try {

				  // put folder creation here so we don't make folders we won't use (e.g. empty directory analysed)
				  makeFolder(folder);
				  
				  log("File:  "+file.getName());
				  
				  FluoresentNucleusDetectionPipeline pipe = new FluoresentNucleusDetectionPipeline(analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS),
						  file, analysisOptions.getNucleusType(), analysisOptions.getProfileWindowProportion());
				  
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
				  error("Error in image processing: "+e.getMessage(), e);
			  } 
			  
			  fireProgressEvent();
			  
		  } 
	   }

	/**
	 *  Checks that the given file is suitable for analysis.
	 *  Is the file an image. Also check if it is in the 'banned list'.
	 *  These are prefixes that are attached to exported images
	 *  at later stages of analysis. This prevents exported images
	 *  from previous runs being analysed.
	 *
	 *  @param file the File to check
	 *  @return a true or false of whether the file passed checks
	 */
	public static boolean checkFile(File file){

		if(file==null){
			return false;
		}
		
		if( ! file.isFile()){
			return false;
		}

		String fileName = file.getName();

		for( String prefix : ImageImporter.PREFIXES_TO_IGNORE){
			if(fileName.startsWith(prefix)){
				return false;
			}
		}

		for( String fileType : ImageImporter.IMPORTABLE_FILE_TYPES){
			if( fileName.endsWith(fileType) ){
				return true;
			}
		}
		return false;
	}
	
	  /**
	  * Create the output folder for the analysis if required
	  *
	  * @param folder the folder in which to create the analysis folder
	  * @return a File containing the created folder
	  */
	  private File makeFolder(File folder){
	    File output = new File(folder.getAbsolutePath()+File.separator+this.outputFolder);
	    if(!output.exists()){
	      try{
	        output.mkdir();
	      } catch(Exception e) {
	    	  error("Failed to create directory", e);
	      }
	    }
	    return output;
	  }
}