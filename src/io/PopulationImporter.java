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
package io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;




//import utility.Logger;
import analysis.AnalysisDataset;
import analysis.nucleus.NucleusDetector;

public class PopulationImporter {
	
	static Logger programLogger;
		
	public static AnalysisDataset readDataset(File inputFile, Logger programLogger) throws Exception {

		if(!inputFile.exists()){
			throw new IllegalArgumentException("Requested file does not exist");
		}

		AnalysisDataset dataset = null;
		PopulationImporter.programLogger = programLogger;

		FileInputStream fis = null;
		ObjectInputStream ois = null;
				
		try {
			fis = new FileInputStream(inputFile.getAbsolutePath());

			ois = new ObjectInputStream(fis);
			
			dataset = (AnalysisDataset) ois.readObject();
			
			// TODO: Validate the profile collection length matches the median array length,
			// and recalculate if needed
			
			// Replace existing save file path with the path to the file that has been opened
			
			if(!dataset.getSavePath().equals(inputFile)){
				updateSavePath(inputFile, dataset);
			}

		} catch (FileNotFoundException e1) {
			programLogger.log(Level.SEVERE, "File not found: "+inputFile.getAbsolutePath(), e1);
		} catch (IOException e1) {
			programLogger.log(Level.SEVERE, "File IO error: "+inputFile.getAbsolutePath(), e1);
		} catch (ClassNotFoundException e1) {
			programLogger.log(Level.SEVERE, "Class not found error: "+inputFile.getAbsolutePath(), e1);
		} finally {
			ois.close();
			fis.close();
		}
		return dataset;
	}
	
	/**
	 * Check if the image folders are present in the correct relative directories
	 * If so, update the CellCollection image paths
	 * should be /ImageDir/AnalysisDir/dataset.nmd
	 * @param inputFile the file being opened
	 * @param dataset the dataset being opened
	 */
	private static void updateSavePath(File inputFile, AnalysisDataset dataset) throws Exception {
		
		programLogger.log(Level.FINE, "File path has changed: attempting to relocate images");

		dataset.setSavePath(inputFile);
		
		if(!dataset.hasMergeSources()){

			// This should be /ImageDir/DateTimeDir/
			File expectedAnalysisDirectory = inputFile.getParentFile();

			// This should be /ImageDir/
			File expectedImageDirectory = expectedAnalysisDirectory.getParentFile();

			updateSourceImageDirectory(expectedImageDirectory, dataset);

		}else {
			programLogger.log(Level.WARNING, "Dataset is a merge");
			programLogger.log(Level.WARNING, "Unable to find single source image directory");
		}
	}
	
	/**
	 * Update the source image paths in the dataset and its children
	 * to use the given directory 
	 * @param expectedImageDirectory
	 * @param dataset
	 * @throws Exception
	 */
	public static void updateSourceImageDirectory(File expectedImageDirectory, AnalysisDataset dataset) throws Exception{
		programLogger.log(Level.FINE, "Searching "+expectedImageDirectory.getAbsolutePath());

		if(expectedImageDirectory.exists()){

			// Is the name of the expectedImageDirectory the same as the dataset image directory?
			if(checkName(expectedImageDirectory, dataset)){
				programLogger.log(Level.FINE, "Dataset name matches new folder");

				// Does expectedImageDirectory contain image files?
				if(checkHasImages(expectedImageDirectory)){
					programLogger.log(Level.FINE, "Target folder contains at least one image");

					programLogger.log(Level.FINE, "Updating dataset image paths");
					boolean ok = dataset.getCollection().updateSourceFolder(expectedImageDirectory);
					if(!ok){
						programLogger.log(Level.WARNING, "Error updating dataset image paths; update cancelled");
					}

					programLogger.log(Level.FINE, "Updating child dataset image paths");
					for(AnalysisDataset child : dataset.getAllChildDatasets()){
						ok = child.getCollection().updateSourceFolder(expectedImageDirectory);
						if(!ok){
							programLogger.log(Level.SEVERE, "Error updating child dataset image paths; update cancelled");
						}
					}

					programLogger.log(Level.INFO, "Updated image paths to new folder location");
				} else {
					programLogger.log(Level.WARNING, "Target folder contains no images; unable to update paths");
				}
			} else {
				programLogger.log(Level.WARNING, "Dataset name does not match new folder; unable to update paths");
			}

		} else {
			programLogger.log(Level.WARNING, "Unable to locate image directory and/or analysis directory; unable to update paths");
		}
	}
	
	/**
	 * Check that the new image directory has the same name as the old image directory.
	 * If the nmd has been copied to the wrong folder, don't update nuclei
	 * @param expectedImageDirectory
	 * @param dataset
	 * @return
	 */
	private static boolean checkName(File expectedImageDirectory, AnalysisDataset dataset){
		if(dataset.getCollection().getFolder().getName().equals(expectedImageDirectory.getName())){
			return true;
		} else {
			return false;
		}
		
	}
	
	/**
	 * Check that the given directory contains >0 image files
	 * suitable for the morphology analysis
	 * @param expectedImageDirectory
	 * @return
	 */
	private static boolean checkHasImages(File expectedImageDirectory){

		File[] listOfFiles = expectedImageDirectory.listFiles();

		int result = 0;

		for (File file : listOfFiles) {

			boolean ok = NucleusDetector.checkFile(file);

			if(ok){
				result++;
			}
		} 
		
		if(result>0){
			return true;
		} else {
			return false;
		}
	}


}