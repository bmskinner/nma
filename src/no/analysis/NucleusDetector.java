/*
  -----------------------
  NUCLEUS DETECTOR
  -----------------------
  Contains the variables for opening
  folders and files and detecting nuclei
  within them
*/  
package no.analysis;

import ij.IJ;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.RoiEnlarger;
import ij.process.FloatPolygon;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import cell.Cell;
import utility.Constants;
import utility.Logger;
import utility.Utils;
import no.collections.*;
import no.components.*;
import no.export.CompositeExporter;
import no.export.ImageExporter;
import no.gui.MainWindow;
import no.imports.ImageImporter;
import no.nuclei.Nucleus;

public class NucleusDetector extends SwingWorker<Boolean, Integer> {
  
  private static final String spacerString = "---------";

  // counts of nuclei processed
  protected int totalNuclei        = 0;
  
  protected int totalImages;
  
  private int progress;
  
  private boolean debug = false;


  private File inputFolder;
  protected String outputFolder;
  protected File debugFile;

  protected AnalysisOptions analysisOptions;

  protected Logger logger;

  protected MainWindow mw;
  private Map<File, CellCollection> collectionGroup = new HashMap<File, CellCollection>();
  
  List<AnalysisDataset> datasets;


  /**
  * Construct a detector on the given folder, and output the results to 
  * the given output folder
  *
  * @param inputFolder the folder to analyse
  * @param outputFolder the name of the folder for results
  */
  public NucleusDetector(String outputFolder, MainWindow mw, File debugFile, AnalysisOptions options){
	  this.inputFolder 		= options.getFolder();
	  this.outputFolder 	= outputFolder;
	  this.mw 				= mw;
	  this.debugFile 		= debugFile;
	  this.analysisOptions 	= options;
	  
	  logger = new Logger(debugFile, "NucleusDetector");
  }

  
  @Override
	protected void process( List<Integer> integers ) {
		//update the number of entries added
		int amount = integers.get( integers.size() - 1 );
		int percent = (int) ( (double) amount / (double) totalImages * 100);
		setProgress(percent); // the integer representation of the percent
	}
	
	@Override
	protected Boolean doInBackground() throws Exception {

		boolean result = false;
		progress = 0;
		
		this.totalImages = NucleusDetector.countSuitableImages(analysisOptions.getFolder());
		try{
			  logger.log("Running nucleus detector");
			  processFolder(this.inputFolder);
			  
			  if(debug){
				  logger.log("Folder processed", Logger.DEBUG);
			  }
			  firePropertyChange("Cooldown", getProgress(), Constants.Progress.COOLDOWN.code());
			  
			  if(debug){
				  logger.log("Getting collections", Logger.DEBUG);
			  }
			  List<CellCollection> folderCollection = this.getNucleiCollections();

			  // Run the analysis pipeline
			  if(debug){
				  logger.log("Analysing collections", Logger.DEBUG);
			  }
			  datasets = analysePopulations(folderCollection);		
			 
			  result = true;
			  
			  
		  } catch(Exception e){
			  result = false;
			  logger.error("Error in processing folder", e);
		  }
		return result;
	}
	
	@Override
	public void done() {

		try {
			if(this.get()){
				firePropertyChange("Finished", getProgress(), Constants.Progress.FINISHED.code());			
				
			} else {
				firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
			}
		} catch (InterruptedException e) {
			logger.error("Error in nucleus detection", e);

		} catch (ExecutionException e) {
			logger.error("Error in nucleus detection", e);
		}

	} 
	
	
	public List<AnalysisDataset> getDatasets(){
		return this.datasets;
	}
	
	public List<AnalysisDataset> analysePopulations(List<CellCollection> folderCollection){
		mw.log("Beginning analysis");
		 if(debug){
			 logger.log("Beginning analysis", Logger.DEBUG);
		 }
		List<AnalysisDataset> result = new ArrayList<AnalysisDataset>();

		for(CellCollection r : folderCollection){
			

			AnalysisDataset dataset = new AnalysisDataset(r);
			dataset.setAnalysisOptions(analysisOptions);
			dataset.setRoot(true);

			File folder = r.getFolder();
			logger.log("Analysing: "+folder.getName());

			LinkedHashMap<String, Integer> nucleusCounts = new LinkedHashMap<String, Integer>();

			try{

				nucleusCounts.put("input", r.getNucleusCount());
				CellCollection failedNuclei = new CellCollection(folder, r.getOutputFolderName(), "failed", logger.getLogfile(), analysisOptions.getNucleusClass());

//				boolean ok;
				mw.logc("Filtering collection...");
				boolean ok = CollectionFilterer.run(r, failedNuclei); // put fails into failedNuclei, remove from r
				if(ok){
					mw.log("OK");
				} else {
					mw.log("Error");
				}

				if(failedNuclei.getNucleusCount()>0){
					mw.logc("Exporting failed nuclei...");
					ok = CompositeExporter.run(failedNuclei);
					if(ok){
						mw.log("OK");
					} else {
						mw.log("Error");
					}
					nucleusCounts.put("failed", failedNuclei.getNucleusCount());
				}
				
			} catch(Exception e){
				logger.log("Cannot create collection: "+e.getMessage(), Logger.ERROR);
			}

			 mw.log(spacerString);
			mw.log("Population: "+r.getType());
			mw.log("Population: "+r.getNucleusCount()+" nuclei");
			logger.log("Population: "+r.getType()+" : "+r.getNucleusCount()+" nuclei");
			mw.log(spacerString);

			result.add(dataset);

		}
		return result;
	}

  /**
  * Add a NucleusCollection to the group, using the source folder
  * name as a key.
  *
  *  @param file a folder to be analysed
  *  @param collection the collection of nuclei found
  */
  public void addNucleusCollection(File file, CellCollection collection){
    this.collectionGroup.put(file, collection);
  }


  /**
  * Get the Map of NucleusCollections to the folder from
  * which they came. Any folders with no nuclei are removed
  * before returning.
  *
  *  @return a Map of a folder to its nuclei
  */
  public List<CellCollection> getNucleiCollections(){
	  // remove any empty collections before returning
	  if(debug){
		  logger.log("Getting all collections", Logger.DEBUG);
	  }
	  List<File> toRemove = new ArrayList<File>(0);
	  
	  if(debug){
		  logger.log("Testing nucleus counts", Logger.DEBUG);
	  }
	  Set<File> keys = collectionGroup.keySet();
	  for (File key : keys) {
		  CellCollection collection = collectionGroup.get(key);
		  if(collection.size()==0){
			  logger.log("Removing collection "+key.toString(), Logger.DEBUG);
			  toRemove.add(key);
		  }    
	  }
	  if(debug){
		  logger.log("Got collections to remove", Logger.DEBUG);
	  }

	  Iterator<File> iter = toRemove.iterator();
	  while(iter.hasNext()){
		  collectionGroup.remove(iter.next());
	  }
	  if(debug){
		  logger.log("Removed collections", Logger.DEBUG);
	  }
	  List<CellCollection> result = new ArrayList<CellCollection>();
	  for(CellCollection c : collectionGroup.values()){
		  result.add(c);
	  }
	  return result;

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
    boolean ok = false;
    if (file.isFile()) {

      String fileName = file.getName();

      for( String fileType : Constants.IMPORTABLE_FILE_TYPES){
        if( fileName.endsWith(fileType) ){
          ok = true;
        }
      }

      for( String prefix : Constants.PREFIXES_TO_IGNORE){
        if(fileName.startsWith(prefix)){
          ok = false;
        }
      }
    }
    return ok;
  }
  
  /**
   * Count the number of images in the given folder
   * that are suitable for analysis. Rcursive over 
   * subfolders.
   * @param folder the folder to count
   * @return the number of analysable image files
   */
  public static int countSuitableImages(File folder){

	  File[] listOfFiles = folder.listFiles();

	  int result = 0;

	  for (File file : listOfFiles) {

		  boolean ok = checkFile(file);

		  if(ok){
			  result++;

		  } else { 
			  if(file.isDirectory()){ // recurse over any sub folders
				  result += countSuitableImages(file);
			  }
		  } 
	  } 
	  return result;
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
        logger.error("Failed to create directory", e);
      }
    }
    return output;
  }
  
  /**
  * Go through the input folder. Check if each file is
  * suitable for analysis, and if so, call the analyser.
  *
  * @param folder the folder of images to be analysed
  */
  protected void processFolder(File folder){

	  File[] listOfFiles = folder.listFiles();
	  
	  CellCollection folderCollection = new CellCollection(folder, 
			  outputFolder, 
			  folder.getName(), 
			  this.debugFile,
			  analysisOptions.getNucleusClass());
	  
	  this.collectionGroup.put(folder, folderCollection);

	  for (File file : listOfFiles) {

		  boolean ok = checkFile(file);

		  if(ok){
			  try {

				  ImageStack imageStack = ImageImporter.importImage(file, logger.getLogfile());

				  // put folder creation here so we don't make folders we won't use (e.g. empty directory analysed)
				  makeFolder(folder);
				  
				  mw.log("File:  "+file.getName());
				  List<Cell> cells = NucleusFinder.getCells(imageStack, analysisOptions, debugFile, file, outputFolder);
				  
				  if(cells.isEmpty()){
					  mw.log("  No nuclei detected in image");
				  } else {
					  int nucleusNumber = 0;
					  for(Cell cell : cells){
						  folderCollection.addCell(cell);
						  mw.log("  Added nucleus "+nucleusNumber);
						  nucleusNumber++;
						 
						  // save out the image stacks rather than hold within the nucleus
						  Nucleus n 			 = cell.getNucleus();
						  FloatPolygon polygon 	 = Utils.createPolygon(n.getBorderList());
						  PolygonRoi nucleus 	 = new PolygonRoi(polygon, PolygonRoi.POLYGON);
						  
						  double[] position = n.getPosition();
						  nucleus.setLocation(position[Nucleus.X_BASE],position[Nucleus.Y_BASE]); // translate the roi to the image coordinates
						  
						  ImageStack smallRegion = NucleusFinder.getRoiAsStack(nucleus, imageStack);
						  Roi largeRoi 			 = RoiEnlarger.enlarge(nucleus, 20);
						  ImageStack largeRegion = NucleusFinder.getRoiAsStack(largeRoi, imageStack);
						  try{
							  IJ.saveAsTiff(ImageExporter.convert(smallRegion), n.getOriginalImagePath());
							  IJ.saveAsTiff(ImageExporter.convert(largeRegion), n.getEnlargedImagePath());
							  IJ.saveAsTiff(ImageExporter.convert(smallRegion), n.getAnnotatedImagePath());
						  } catch(Exception e){
							  logger.error("Error saving original, enlarged or annotated image", e);
						  }
					  }
				  }

			  } catch (Exception e) { // end try
				  logger.log("Error in image processing: "+e.getMessage(), Logger.ERROR);
			  } // end catch
			  
			  progress++;
			  publish(progress);
		  } else { // if !ok
			  if(file.isDirectory()){ // recurse over any sub folders
				  processFolder(file);
			  } 
		  } // end else if !ok
	  } // end for (File)
  } // end function
}