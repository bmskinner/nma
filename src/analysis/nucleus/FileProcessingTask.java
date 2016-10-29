package analysis.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import analysis.AbstractProgressAction;
import analysis.IAnalysisOptions;
import components.CellularComponent;
import components.ICell;
import components.ICellCollection;
import components.nuclei.Nucleus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import io.ImageImporter;
import utility.Constants;

@SuppressWarnings("serial")
public class FileProcessingTask  extends AbstractProgressAction  {
	
	private final ICellCollection collection;
	private File[] files;
	private static final int THRESHOLD = 5; // number of images to handle per fork
	final int low, high;
	private final NucleusDetector finder;
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
		this.finder          = new NucleusDetector( analysisOptions, outputFolder);
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

				  ImageStack imageStack = new ImageImporter(file).importImage();

				  // put folder creation here so we don't make folders we won't use (e.g. empty directory analysed)
				  makeFolder(folder);
				  
				  log(Level.INFO, "File:  "+file.getName());
				  List<ICell> cells = finder.getCells(imageStack, file);
				  
				  if(cells.isEmpty()){
					  log(Level.INFO, "  No nuclei detected in image");
				  } else {
					  int nucleusNumber = 0;
					  for(ICell cell : cells){
						  addAndProcessCell(cell, imageStack, nucleusNumber++);
					  }
				  }

			  } catch (Exception e) { 
				  logError("Error in image processing: "+e.getMessage(), e);
			  } 
			  
			  fireProgressEvent();
			  
		  } 
	   }


	private void addAndProcessCell(ICell cell, ImageStack imageStack, int nucleusNumber ) throws Exception{
		collection.addCell(cell);
		log("  Added nucleus "+nucleusNumber);

		 
		  // save out the image stacks rather than hold within the nucleus
		  Nucleus n 			 = cell.getNucleus();
		  PolygonRoi nucleus 	 = new PolygonRoi(n.createPolygon(), PolygonRoi.POLYGON);
		  
		  int[] position = n.getPosition();
		  nucleus.setLocation(position[CellularComponent.X_BASE],position[CellularComponent.Y_BASE]); // translate the roi to the image coordinates
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

		for( String prefix : Constants.PREFIXES_TO_IGNORE){
			if(fileName.startsWith(prefix)){
				return false;
			}
		}

		for( String fileType : Constants.IMPORTABLE_FILE_TYPES){
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
	    	  logError("Failed to create directory", e);
	      }
	    }
	    return output;
	  }
}