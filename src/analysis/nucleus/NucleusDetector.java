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

		for(CellCollection r : folderCollection){
			

			AnalysisDataset dataset = new AnalysisDataset(r);
			dataset.setAnalysisOptions(analysisOptions);
			dataset.setRoot(true);
			File debugFile = r.getDebugFile();

			File folder = r.getFolder();
			log(Level.INFO, "Analysing: "+folder.getName());

			LinkedHashMap<String, Integer> nucleusCounts = new LinkedHashMap<String, Integer>();

			try{

				nucleusCounts.put("input", r.getNucleusCount());
				CellCollection failedNuclei = new CellCollection(folder, 
						r.getOutputFolderName(), 
						"failed", 
						debugFile, 
						analysisOptions.getNucleusType());

//				boolean ok;
				log(Level.INFO, "Filtering collection...");
				boolean ok = CollectionFilterer.run(r, failedNuclei, fileLogger); // put fails into failedNuclei, remove from r
				if(ok){
					log(Level.INFO, "Filtered OK");
				} else {
					log(Level.INFO, "Filtering error");
				}

				if(failedNuclei.getNucleusCount()>0){
					log(Level.INFO, "Exporting failed nuclei...");
					ok = CompositeExporter.run(failedNuclei, fileLogger);
					if(ok){
						log(Level.INFO, "Export OK");
					} else {
						log(Level.INFO, "Export error");
					}
					nucleusCounts.put("failed", failedNuclei.getNucleusCount());
				}
				
			} catch(Exception e){
				log(Level.WARNING, "Cannot create collection: "+e.getMessage());
			}

			programLogger.log(Level.INFO, spacerString);
			
			programLogger.log(Level.INFO, "Population: "+r.getType());
			
			programLogger.log(Level.INFO, "Population: "+r.getNucleusCount()+" nuclei");
			programLogger.log(Level.INFO, spacerString);
			
			fileLogger.log(Level.INFO, "Population: "+r.getType()+" : "+r.getNucleusCount()+" nuclei");
			

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

	  fileLogger.log(Level.FINE, "Getting all collections");

	  List<File> toRemove = new ArrayList<File>(0);

	  fileLogger.log(Level.FINE, "Testing nucleus counts");

	  Set<File> keys = collectionGroup.keySet();
	  for (File key : keys) {
		  CellCollection collection = collectionGroup.get(key);
		  if(collection.size()==0){
			  fileLogger.log(Level.FINE, "Removing collection "+key.toString());
			  toRemove.add(key);
		  }    
	  }

	  fileLogger.log(Level.FINE, "Got collections to remove");

	  Iterator<File> iter = toRemove.iterator();
	  while(iter.hasNext()){
		  collectionGroup.remove(iter.next());
	  }

	  fileLogger.log(Level.FINE, "Removed collections");

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
    	  fileLogger.log(Level.SEVERE, "Failed to create directory", e);
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
			  analysisOptions.getNucleusType());
	  
	  this.collectionGroup.put(folder, folderCollection);

	  for (File file : listOfFiles) {

		  boolean ok = checkFile(file);

		  if(ok){
			  try {

				  ImageStack imageStack = ImageImporter.importImage(file, fileLogger);

				  // put folder creation here so we don't make folders we won't use (e.g. empty directory analysed)
				  makeFolder(folder);
				  
				  programLogger.log(Level.INFO, "File:  "+file.getName());
				  List<Cell> cells = NucleusFinder.getCells(imageStack, analysisOptions, programLogger, file, outputFolder);
				  
				  if(cells.isEmpty()){
					  programLogger.log(Level.INFO, "  No nuclei detected in image");
				  } else {
					  int nucleusNumber = 0;
					  for(Cell cell : cells){
						  folderCollection.addCell(cell);
						  programLogger.log(Level.INFO, "  Added nucleus "+nucleusNumber);
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
							  IJ.saveAsTiff(ImageExporter.convertToRGB(smallRegion), n.getOriginalImagePath());
							  IJ.saveAsTiff(ImageExporter.convertToRGB(largeRegion), n.getEnlargedImagePath());
							  IJ.saveAsTiff(ImageExporter.convertToRGB(smallRegion), n.getAnnotatedImagePath());
						  } catch(Exception e){
							  fileLogger.log(Level.SEVERE, "Error saving original, enlarged or annotated image", e);
						  }
					  }
				  }

			  } catch (Exception e) { // end try
				  fileLogger.log(Level.SEVERE, "Error in image processing: "+e.getMessage(), e);
			  } // end catch
			  
			  publish(progress++); // must be global since this function recurses
		  } else { // if !ok
			  if(file.isDirectory()){ // recurse over any sub folders
				  processFolder(file);
			  } 
		  } // end else if !ok
	  } // end for (File)
  } // end function
}