/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
/*
  -----------------------
  NUCLEUS DETECTOR
  -----------------------
  Contains the variables for opening
  folders and files and detecting nuclei
  within them
*/  
package analysis.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import utility.Constants;
import analysis.AnalysisOptions;
import analysis.AnalysisWorker;
import analysis.IAnalysisDataset;
import analysis.IAnalysisOptions;
import analysis.ProgressEvent;
import analysis.ProgressListener;
import components.ICellCollection;
import components.active.DefaultAnalysisDataset;
import components.active.DefaultCellCollection;
import components.nuclear.NucleusType;

public class NucleusDetectionWorker extends AnalysisWorker  implements ProgressListener {
  
  private static final String spacerString = "---------";
  
  private int progress = 0;

  private final String outputFolder;
  
  private final IAnalysisOptions analysisOptions;

  private Map<File, ICellCollection> collectionGroup = new HashMap<File, ICellCollection>();
  
  List<IAnalysisDataset> datasets;

  /**
   * Construct a detector on the given folder, and output the results to 
   * the given output folder
   * @param outputFolder the name of the folder for results
   * @param programLogger the logger to the log panel
   * @param debugFile the dataset log file
   * @param options the options to detect with
   */
  public NucleusDetectionWorker(String outputFolder, File debugFile, IAnalysisOptions options){
	  super(null, debugFile);
	  this.outputFolder 	= outputFolder;
	  this.analysisOptions 	= options;
  }
  
  private void getTotalImagesToAnalyse(){
	  log("Calculating number of images to analyse");
	  int totalImages = countSuitableImages(analysisOptions.getFolder());
	  this.setProgressTotal(totalImages);
	  log("Analysing "+totalImages+" images");
  }
	
	@Override
	protected Boolean doInBackground() throws Exception {

		boolean result = false;
		
		try{
			
			getTotalImagesToAnalyse();
			
			log("Running nucleus detector");
			processFolder(analysisOptions.getFolder());

			fine("Detected nuclei in "+analysisOptions.getFolder().getAbsolutePath());
			firePropertyChange("Cooldown", getProgress(), Constants.Progress.COOLDOWN.code());


			fine( "Creating cell collections");

			List<ICellCollection> folderCollection = this.getNucleiCollections();

			// Run the analysis pipeline

			fine("Analysing collections");

			datasets = analysePopulations(folderCollection);		

			result = true;
			fine( "Analysis complete; return collections");

		} catch(Exception e){
			result = false;
			stack("Error in processing folder", e);
		}
		return result;
	}
		
	public List<IAnalysisDataset> getDatasets(){
		return this.datasets;
	}
	
	public List<IAnalysisDataset> analysePopulations(List<ICellCollection> folderCollection){

		log("Creating cell collections");

		List<IAnalysisDataset> result = new ArrayList<IAnalysisDataset>();

		for(ICellCollection collection : folderCollection){
			
			IAnalysisDataset dataset = new DefaultAnalysisDataset(collection);
			dataset.setAnalysisOptions(analysisOptions);
			dataset.setRoot(true);

			File folder = collection.getFolder();
			log("Analysing: "+folder.getName());

			try{

				ICellCollection failedNuclei = new DefaultCellCollection(folder, 
						collection.getOutputFolderName(), 
						collection.getName()+" - failed", 
						collection.getNucleusType());


				log("Filtering collection...");
				boolean ok = new CollectionFilterer().run(collection, failedNuclei); // put fails into failedNuclei, remove from r
				if(ok){
					log("Filtered OK");
				} else {
					log("Filtering error");
				}
				
				/*
				 * Keep the failed nuclei - they can be manually assessed later
				 */

				if(analysisOptions.isKeepFailedCollections()){
					log("Keeping failed nuclei as new collection");
					IAnalysisDataset failed = new DefaultAnalysisDataset(failedNuclei);
					IAnalysisOptions failedOptions = new AnalysisOptions(analysisOptions);
					failedOptions.setNucleusType(NucleusType.ROUND);
					failed.setAnalysisOptions(failedOptions);
					failed.setRoot(true);
					result.add(failed);
				}
				
				log(spacerString);
				
				log("Population: "+collection.getName());
				log("Passed: "+collection.size()+" nuclei");
				log("Failed: "+failedNuclei.size()+" nuclei");
				
				log(spacerString);
				
				result.add(dataset);
				
				
			} catch(Exception e){
				warn("Cannot create collection: "+e.getMessage());
				stack("Error in nucleus detection", e);
			}

//			

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
  public void addNucleusCollection(File file, ICellCollection collection){
    this.collectionGroup.put(file, collection);
  }


  /**
  * Get the Map of NucleusCollections to the folder from
  * which they came. Any folders with no nuclei are removed
  * before returning.
  *
  *  @return a Map of a folder to its nuclei
  */
  public List<ICellCollection> getNucleiCollections(){
	  // remove any empty collections before returning

	  fine( "Getting all collections");

	  List<File> toRemove = new ArrayList<File>(0);

	  fine( "Testing nucleus counts");

	  Set<File> keys = collectionGroup.keySet();
	  for (File key : keys) {
		  ICellCollection collection = collectionGroup.get(key);
		  if(collection.size()==0){
			  fine( "Removing collection "+key.toString());
			  toRemove.add(key);
		  }    
	  }

	  fine( "Got collections to remove");

	  Iterator<File> iter = toRemove.iterator();
	  while(iter.hasNext()){
		  collectionGroup.remove(iter.next());
	  }

	  fine( "Removed collections");

	  List<ICellCollection> result = new ArrayList<ICellCollection>();
	  for(ICellCollection c : collectionGroup.values()){
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
  * Go through the input folder. Check if each file is
  * suitable for analysis, and if so, call the analyser.
  *
  * @param folder the folder of images to be analysed
  */
  protected void processFolder(File folder){

	  File[] listOfFiles = folder.listFiles();

	  ICellCollection folderCollection = new DefaultCellCollection(folder, 
			  outputFolder, 
			  folder.getName(), 
			  analysisOptions.getNucleusType());
	  
	  this.collectionGroup.put(folder, folderCollection);


	  FileProcessingTask task = new FileProcessingTask(folder, listOfFiles, folderCollection, outputFolder, analysisOptions);
	  task.addProgressListener(this);
	  mainPool.invoke(task);
	  
	  for(File f : listOfFiles){
		  if(f.isDirectory()){
			  processFolder(f); // recurse over each folder
		  }
	  }


  } // end function

  @Override
  public void progressEventReceived(ProgressEvent event) {
	  publish(++progress);

  }
  
  
}