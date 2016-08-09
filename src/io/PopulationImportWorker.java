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
package io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.logging.Level;

import components.generic.BorderTag;
import components.generic.BorderTagObject;
import components.generic.ProfileType;
import components.nuclear.NucleusType;
import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import utility.Constants;
import utility.Version;

public class PopulationImportWorker extends AnalysisWorker {
	
	private File file;
	private AnalysisDataset dataset = null;
	
	public PopulationImportWorker(final File f){
		super(null);
		this.file = f;
		this.setProgressTotal(1);
		finest("Created instance of "+this.getClass().getSimpleName());
	}
	
	public AnalysisDataset getLoadedDataset(){
		return this.dataset;
	}
	
	@Override
	protected Boolean doInBackground() {
		finest("Beginning background work");
		try {
			dataset = readDataset(file);
			
			if(dataset == null){
				warn("Unable to open dataset");
				return false;
			}
			
			fine("Read dataset");
			
			Version v = null;
			
			
			try {
				v = dataset.getVersion();
			} catch (Exception e) {
				error("Error getting version from dataset", e);
				return false;
			}
			
			
			
			if(checkVersion( v )){

				fine("Version check OK");
				dataset.setRoot(true);

				
				// update the log file to the same folder as the dataset
				File logFile = new File(file.getParent()
						+File.separator
						+file.getName().replace(Constants.SAVE_FILE_EXTENSION, Constants.LOG_FILE_EXTENSION));
				
				dataset.setDebugFile(logFile);
				fine("Updated log file location");
				
				
				// If rodent sperm, check if the TOP_VERTICAL and BOTTOM_VERTICAL 
				// points have been set, and if not, add them
				if(dataset.getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)){
					
					if(! dataset.getCollection()
							.getProfileCollection(ProfileType.ANGLE)
							.hasBorderTag(BorderTagObject.TOP_VERTICAL)  ){
						
						fine("TOP_ and BOTTOM_VERTICAL not assigned; calculating");
						dataset.getCollection().getProfileManager().calculateTopAndBottomVerticals();
						fine("Calculating TOP and BOTTOM for child datasets");
						for(AnalysisDataset child : dataset.getAllChildDatasets()){
							child.getCollection().getProfileManager().calculateTopAndBottomVerticals();
						}
						
					}
					
				}
				
				return true;
				
			} else {
				warn("Unable to open dataset version: "+ dataset.getVersion());
				return false;
			}
			
			
		} catch (IllegalArgumentException e){
			warn("Unable to open file: "+e.getMessage());
			return false;
		} catch(Exception e){
			error("Unable to open file", e);
			return false;
		}
	}
	

	

	
	/**
	 * Check a version string to see if the program will be able to open a 
	 * dataset. The major version must be the same, while the revision of the
	 * dataset must be equal to or greater than the program revision. Bugfixing
	 * versions are not checked for.
	 * @param version
	 * @return a pass or fail
	 */
	public boolean checkVersion(Version version){

		
		if(version==null){ // allow for debugging, but warn
			warn("No version info found: functions may not work as expected");
			return true;
		}
				
		
		// major version MUST be the same
		if(version.getMajor()!=Constants.VERSION_MAJOR){
			warn("Major version difference");
			return false;
		}
		// dataset revision should be equal or greater to program
		if(version.getMinor()<Constants.VERSION_MINOR){
			warn("Dataset was created with an older version of the program");
			warn("Some functionality may not work as expected");
		}
		return true;
	}
	
	private AnalysisDataset readDataset(File inputFile) {

		finest("Checking input file");
		
		if( ! inputFile.exists()){
			throw new IllegalArgumentException("Requested file does not exist");
		}
		
		if( ! inputFile.getName().endsWith(".nmd")){
			throw new IllegalArgumentException("File is not nmd format");
		}

		AnalysisDataset dataset = null;
		FileInputStream fis     = null;
		ObjectInputStream ois   = null;
				
		try {
			
			finest("Attempting to read file");
			
			fis = new FileInputStream(inputFile.getAbsolutePath());
			finest("Created file stream");
			ois = new ObjectInputStream(fis);
			finest("Created object stream");
			
			
			finest("Attempting to read object");
			dataset = (AnalysisDataset) ois.readObject();
			finest("Read object as analysis dataset");	
			
			// Replace existing save file path with the path to the file that has been opened
			finest("Checking file path");
			if(!dataset.getSavePath().equals(inputFile)){
				updateSavePath(inputFile, dataset);
			}
			
		} catch (FileNotFoundException e1) {
			error("File not found: "+inputFile.getAbsolutePath(), e1);
			dataset = null;
		} catch (IOException e1) {
			error("File IO error: "+inputFile.getAbsolutePath(), e1);
			dataset = null;
		} catch (ClassNotFoundException e1) {
			error("Class not found error: "+inputFile.getAbsolutePath(), e1);
			dataset = null;
		} catch(Exception e1){
			error("Unexpected exception opening dataset: "+inputFile.getAbsolutePath(), e1);
			dataset = null;
		} catch(StackOverflowError e){
			error("StackOverflow opening dataset: "+inputFile.getAbsolutePath(), e);
			dataset = null;
		} finally {
			finest("Closing file stream");
			try {
				ois.close();
				fis.close();
			} catch(Exception e){
				error("Error closing file stream", e);
			}
		}
		finest("Returning opened dataset");
		return dataset;
	}
	
	/**
	 * Check if the image folders are present in the correct relative directories
	 * If so, update the CellCollection image paths
	 * should be /ImageDir/AnalysisDir/dataset.nmd
	 * @param inputFile the file being opened
	 * @param dataset the dataset being opened
	 */
	private void updateSavePath(File inputFile, AnalysisDataset dataset) {
		
		fine("File path has changed: attempting to relocate images");

		dataset.setSavePath(inputFile);
		
		if(!dataset.hasMergeSources()){

			// This should be /ImageDir/DateTimeDir/
			File expectedAnalysisDirectory = inputFile.getParentFile();

			// This should be /ImageDir/
			File expectedImageDirectory = expectedAnalysisDirectory.getParentFile();
			
			try {
			dataset.updateSourceImageDirectory(expectedImageDirectory);
			} catch (IllegalArgumentException e){
				warn("Cannot update save path: "+e.getMessage());
			}
//			updateSourceImageDirectory(expectedImageDirectory, dataset);

		}else {
			warn("Dataset is a merge");
			warn("Unable to find single source image directory");
		}
	}
	
//	/**
//	 * Update the source image paths in the dataset and its children
//	 * to use the given directory 
//	 * @param expectedImageDirectory
//	 * @param dataset
//	 * @throws Exception
//	 */
//	private void updateSourceImageDirectory(File expectedImageDirectory, AnalysisDataset dataset) throws Exception{
//		log(Level.FINE, "Searching "+expectedImageDirectory.getAbsolutePath());
//
//		if(expectedImageDirectory.exists()){
//
//			// Is the name of the expectedImageDirectory the same as the dataset image directory?
//			if(checkName(expectedImageDirectory, dataset)){
//				log(Level.FINE, "Dataset name matches new folder");
//
//				// Does expectedImageDirectory contain image files?
//				if(checkHasImages(expectedImageDirectory)){
//					log(Level.FINE, "Target folder contains at least one image");
//
//					log(Level.FINE, "Updating dataset image paths");
//					boolean ok = dataset.getCollection().updateSourceFolder(expectedImageDirectory);
//					if(!ok){
//						log(Level.WARNING, "Error updating dataset image paths; update cancelled");
//					}
//
//					log(Level.FINE, "Updating child dataset image paths");
//					for(AnalysisDataset child : dataset.getAllChildDatasets()){
//						ok = child.getCollection().updateSourceFolder(expectedImageDirectory);
//						if(!ok){
//							log(Level.SEVERE, "Error updating child dataset image paths; update cancelled");
//						}
//					}
//
//					log(Level.INFO, "Updated image paths to new folder location");
//				} else {
//					log(Level.WARNING, "Target folder contains no images; unable to update paths");
//				}
//			} else {
//				log(Level.WARNING, "Dataset name does not match new folder; unable to update paths");
//			}
//
//		} else {
//			log(Level.WARNING, "Unable to locate image directory and/or analysis directory; unable to update paths");
//		}
//	}
	
//	/**
//	 * Check that the new image directory has the same name as the old image directory.
//	 * If the nmd has been copied to the wrong folder, don't update nuclei
//	 * @param expectedImageDirectory
//	 * @param dataset
//	 * @return
//	 */
//	private boolean checkName(File expectedImageDirectory, AnalysisDataset dataset){
//		if(dataset.getCollection().getFolder().getName().equals(expectedImageDirectory.getName())){
//			return true;
//		} else {
//			return false;
//		}
//		
//	}
//	
//	/**
//	 * Check that the given directory contains >0 image files
//	 * suitable for the morphology analysis
//	 * @param expectedImageDirectory
//	 * @return
//	 */
//	private boolean checkHasImages(File expectedImageDirectory){
//
//		File[] listOfFiles = expectedImageDirectory.listFiles();
//
//		int result = 0;
//
//		for (File file : listOfFiles) {
//
//			boolean ok = NucleusDetector.checkFile(file);
//
//			if(ok){
//				result++;
//			}
//		} 
//		
//		if(result>0){
//			return true;
//		} else {
//			return false;
//		}
//	}

}
