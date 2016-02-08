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

import ij.IJ;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.RoiEnlarger;
import ij.process.FloatPolygon;
import io.CompositeExporter;
import io.ImageExporter;
import io.ImageImporter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import logging.DebugFileFormatter;
import logging.DebugFileHandler;
import utility.Constants;
//import utility.Logger;
import utility.Utils;
import analysis.AnalysisDataset;
import analysis.AnalysisOptions;
import analysis.AnalysisWorker;
import components.Cell;
import components.CellCollection;
import components.CellularComponent;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;

public class NucleusDetector extends AnalysisWorker {
  
  private static final String spacerString = "---------";
  
  private int progress;

  private File inputFolder;
  protected String outputFolder;
  protected File debugFile;

  protected AnalysisOptions analysisOptions;

//  protected MainWindow mw;
  private Map<File, CellCollection> collectionGroup = new HashMap<File, CellCollection>();
  
  List<AnalysisDataset> datasets;

  /**
   * Construct a detector on the given folder, and output the results to 
   * the given output folder
   * @param outputFolder the name of the folder for results
   * @param programLogger the logger to the log panel
   * @param debugFile the dataset log file
   * @param options the options to detect with
   */
  public NucleusDetector(String outputFolder, Logger programLogger, File debugFile, AnalysisOptions options){
	  super(null, programLogger, debugFile);
	  this.inputFolder 		= options.getFolder();
	  this.outputFolder 	= outputFolder;
	  this.debugFile 		= debugFile;
	  this.analysisOptions 	= options;
	  
	  
	  
	  log(Level.INFO, "Calculating number of images to analyse");
	  int totalImages = NucleusDetector.countSuitableImages(analysisOptions.getFolder());
	  this.setProgressTotal(totalImages);
	  log(Level.INFO, "Analysing "+totalImages+" images");
	  
	  this.progress = 0;
  }
	
	@Override
	protected Boolean doInBackground() throws Exception {

		boolean result = false;
		
		try{
			log(Level.INFO, "Running nucleus detector");
			processFolder(this.inputFolder);

			log(Level.FINE, "Folder processed");
			firePropertyChange("Cooldown", getProgress(), Constants.Progress.COOLDOWN.code());


			log(Level.FINE, "Getting collections");

			List<CellCollection> folderCollection = this.getNucleiCollections();

			// Run the analysis pipeline

			log(Level.FINE,"Analysing collections");

			datasets = analysePopulations(folderCollection);		

			result = true;
			log(Level.FINE, "Analysis complete; return collections");

		} catch(Exception e){
			result = false;
			logError("Error in processing folder", e);
		}
		return result;
	}
		
	public List<AnalysisDataset> getDatasets(){
		return this.datasets;
	}
	
	public List<AnalysisDataset> analysePopulations(List<CellCollection> folderCollection){
//		programLogger.log(Level.INFO, "Beginning analysis");
		log(Level.INFO, "Beginning analysis");

		List<AnalysisDataset> result = new ArrayList<AnalysisDataset>();

		for(CellCollection collection : folderCollection){
			

			AnalysisDataset dataset = new AnalysisDataset(collection);
			dataset.setAnalysisOptions(analysisOptions);
			dataset.setRoot(true);
//			File debugFile = dataset.getDebugFile();

			File folder = collection.getFolder();
			log(Level.INFO, "Analysing: "+folder.getName());

			try{

				CellCollection failedNuclei = new CellCollection(folder, 
						collection.getOutputFolderName(), 
						collection.getName()+" - failed", 
						NucleusType.ROUND);


				log(Level.INFO, "Filtering collection...");
				boolean ok = CollectionFilterer.run(collection, failedNuclei, fileLogger); // put fails into failedNuclei, remove from r
				if(ok){
					log(Level.INFO, "Filtered OK");
				} else {
					log(Level.INFO, "Filtering error");
				}
				
				/*
				 * Keep the failed nuclei - they can be manually assessed later
				 */

				if(analysisOptions.isKeepFailedCollections()){
					AnalysisDataset failed = new AnalysisDataset(failedNuclei);
					AnalysisOptions failedOptions = new AnalysisOptions(analysisOptions);
					failedOptions.setNucleusType(NucleusType.ROUND);
					failed.setAnalysisOptions(failedOptions);
					failed.setRoot(true);
					result.add(failed);
				} else {
				
					if(failedNuclei.getNucleusCount()>0){
						log(Level.INFO, "Exporting failed nuclei...");
						ok = CompositeExporter.run(failedNuclei, fileLogger);
						if(ok){
							log(Level.INFO, "Export OK");
						} else {
							log(Level.INFO, "Export error");
						}
					}
				}
				log(Level.INFO, spacerString);
				
				log(Level.INFO, "Population: "+collection.getName());
				log(Level.INFO, "Passed: "+collection.getNucleusCount()+" nuclei");
				log(Level.INFO, "Failed: "+failedNuclei.getNucleusCount()+" nuclei");
				
				log(Level.INFO, spacerString);
				
				result.add(dataset);
				
				
			} catch(Exception e){
				log(Level.WARNING, "Cannot create collection: "+e.getMessage());
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

	  log(Level.FINE, "Getting all collections");

	  List<File> toRemove = new ArrayList<File>(0);

	  log(Level.FINE, "Testing nucleus counts");

	  Set<File> keys = collectionGroup.keySet();
	  for (File key : keys) {
		  CellCollection collection = collectionGroup.get(key);
		  if(collection.cellCount()==0){
			  log(Level.FINE, "Removing collection "+key.toString());
			  toRemove.add(key);
		  }    
	  }

	  log(Level.FINE, "Got collections to remove");

	  Iterator<File> iter = toRemove.iterator();
	  while(iter.hasNext()){
		  collectionGroup.remove(iter.next());
	  }

	  log(Level.FINE, "Removed collections");

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
			  analysisOptions.getNucleusType());
	  
	  this.collectionGroup.put(folder, folderCollection);
	  
	  
	  FileProcessingTask task = new FileProcessingTask(folder, listOfFiles, folderCollection, outputFolder, programLogger, analysisOptions);
	  task.invoke();
	  
//	  NucleusFinder finder = new NucleusFinder(programLogger, analysisOptions, outputFolder);
//
//	  for (File file : listOfFiles) {
//
//		  boolean ok = checkFile(file);
//
//		  if(ok){
//			  try {
//
//				  ImageStack imageStack = ImageImporter.importImage(file, fileLogger);
//
//				  // put folder creation here so we don't make folders we won't use (e.g. empty directory analysed)
//				  makeFolder(folder);
//				  
//				  log(Level.INFO, "File:  "+file.getName());
//				  List<Cell> cells = finder.getCells(imageStack, file);
//				  
//				  if(cells.isEmpty()){
//					  log(Level.INFO, "  No nuclei detected in image");
//				  } else {
//					  int nucleusNumber = 0;
//					  for(Cell cell : cells){
//						  folderCollection.addCell(cell);
//						  log(Level.INFO, "  Added nucleus "+nucleusNumber);
//						  nucleusNumber++;
//						 
//						  // save out the image stacks rather than hold within the nucleus
//						  Nucleus n 			 = cell.getNucleus();
//						  PolygonRoi nucleus 	 = new PolygonRoi(n.createPolygon(), PolygonRoi.POLYGON);
//						  
//						  double[] position = n.getPosition();
//						  nucleus.setLocation(position[CellularComponent.X_BASE],position[CellularComponent.Y_BASE]); // translate the roi to the image coordinates
//						  
//						  ImageStack smallRegion = NucleusFinder.getRoiAsStack(nucleus, imageStack);
//						  
//						  try{
//							  IJ.saveAsTiff(ImageExporter.convertToRGB(smallRegion), n.getAnnotatedImagePath());
//						  } catch(Exception e){
//							  logError("Error saving original, enlarged or annotated image", e);
//						  }
//					  }
//				  }
//
//			  } catch (Exception e) { // end try
//				  logError("Error in image processing: "+e.getMessage(), e);
//			  } // end catch
//			  
//			  publish(progress++); // must be global since this function recurses
//		  } else { // if !ok
//			  if(file.isDirectory()){ // recurse over any sub folders
//				  processFolder(file);
//			  } 
//		  } // end else if !ok
//	  } // end for (File)
  } // end function
}