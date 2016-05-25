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
import components.generic.ProfileType;
import components.nuclear.NucleusType;
import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import analysis.nucleus.ProfileFeatureFinder;
import analysis.nucleus.ProfileOffsetter;
import utility.Constants;
import utility.Version;

public class PopulationImportWorker extends AnalysisWorker {
	
	private File file;
	private AnalysisDataset dataset;
	
	public PopulationImportWorker(final File f){
		super(null);
		this.file = f;
		this.setProgressTotal(1);
	}
	
	public AnalysisDataset getLoadedDataset(){
		return this.dataset;
	}
	
	@Override
	protected Boolean doInBackground() {
		
		try {
			dataset = readDataset(file);
			
			log(Level.FINE, "Read dataset");
			if(checkVersion( dataset.getVersion() )){

				log(Level.FINE, "Version check OK");
				dataset.setRoot(true);

				
				// update the log file to the same folder as the dataset
				File logFile = new File(file.getParent()
						+File.separator
						+file.getName().replace(Constants.SAVE_FILE_EXTENSION, Constants.LOG_FILE_EXTENSION));
				
				dataset.setDebugFile(logFile);
				log(Level.FINE, "Updated log file location");
				
				
				// If rodent sperm, check if the TOP_VERTICAL and BOTTOM_VERTICAL 
				// points have been set, and if not, add them
				if(dataset.getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)){
					
					if(! dataset.getCollection()
							.getProfileCollection(ProfileType.REGULAR)
							.hasBorderTag(BorderTag.TOP_VERTICAL)  ){
						
						log(Level.FINE, "TOP_ and BOTTOM_VERTICAL not assigned; calculating");
						calculateTopAndBottomVerticals(dataset);
						log(Level.FINE, "Calculating TOP and BOTTOM for child datasets");
						for(AnalysisDataset child : dataset.getAllChildDatasets()){
							calculateTopAndBottomVerticals(child);
						}
						
					}
//					Version version = dataset.getVersion();
//					
//					// This is when bugs were fixed for hook hump assignment
//					Version testVersion = new Version(1, 12, 1); //TODO: change this when fixed
					
					/*
					 * Correct hook-hump for older versions
					 * Don't run a load of calculations unnecessarily in newer versions
					 */
//					if(version.isOlderThan(testVersion)){
//						
//						programLogger.log(Level.FINE, "Updating older dataset hook-hump split and signals");
//						updateRodentSpermHookHumpSplits(dataset);
//						for(AnalysisDataset child : dataset.getAllChildDatasets()){
//							updateRodentSpermHookHumpSplits(child);
//						}
//					}
					
				}
				
				return true;
				
			} else {
				log(Level.SEVERE, "Unable to open dataset version: "+ dataset.getVersion());
				return false;
			}
			
			
		} catch (IllegalArgumentException e){
			warn("Unable to open file: "+e.getMessage());
			return false;
		} catch(Exception e){
			logError("Unable to open file", e);
			return false;
		}
	}
	
	/**
	 * Recalculate the hook-hunp split, and signal angle measurements for the 
	 * given dataset of rodent sperm nuclei
	 * @param d
	 * @throws Exception
	 */
//	private void updateRodentSpermHookHumpSplits(AnalysisDataset d) throws Exception{
//		
//		if(d.getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)){
//			for(Nucleus n : d.getCollection().getNuclei()){
//
//				RodentSpermNucleus nucleus = (RodentSpermNucleus) n;
//				// recalculate - old datasets have problems
//				nucleus.splitNucleusToHeadAndHump();
//
//				// recalculate signal angles - old datasets have problems
//				nucleus.calculateSignalAnglesFromPoint(nucleus.getPoint(BorderTag.ORIENTATION_POINT));
//			}
//		}
//		
//	}
	
	private void calculateTopAndBottomVerticals(AnalysisDataset dataset) throws Exception {
		
		log(Level.FINE, "Detecting flat region");

		ProfileFeatureFinder finder = new ProfileFeatureFinder(dataset.getCollection());
		finder.assignTopAndBottomVerticalInMouse();
		
		log(Level.FINE, "Assigning flat region to nuclei");
		ProfileOffsetter offsetter = new ProfileOffsetter(dataset.getCollection());
		offsetter.calculateVerticals();
//		DatasetProfiler.Offsetter.assignFlatRegionToMouseNuclei(dataset.getCollection());
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
		boolean ok = true;
		
		if(version==null){ // allow for debugging, but warn
			log(Level.WARNING, "No version info found: functions may not work as expected");
			return true;
		}
				
//		String[] parts = version.split("\\.");
		
		// major version MUST be the same
		if(version.getMajor()!=Constants.VERSION_MAJOR){
			ok = false;
		}
		// dataset revision should be equal or greater to program
		if(version.getMinor()<Constants.VERSION_MINOR){
			log(Level.WARNING, "Dataset was created with an older version of the program");
			log(Level.WARNING, "Some functionality may not work as expected");
		}
		return ok;
	}
	
	private AnalysisDataset readDataset(File inputFile) {

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
			fis = new FileInputStream(inputFile.getAbsolutePath());

			ois = new ObjectInputStream(fis);
			
			dataset = (AnalysisDataset) ois.readObject();
						
			// Replace existing save file path with the path to the file that has been opened
			
			if(!dataset.getSavePath().equals(inputFile)){
				updateSavePath(inputFile, dataset);
			}
			
		} catch (FileNotFoundException e1) {
			logError("File not found: "+inputFile.getAbsolutePath(), e1);
		} catch (IOException e1) {
			logError("File IO error: "+inputFile.getAbsolutePath(), e1);
		} catch (ClassNotFoundException e1) {
			logError("Class not found error: "+inputFile.getAbsolutePath(), e1);
		} finally {
			
			try {
				ois.close();
				fis.close();
			} catch(Exception e){
				error("Error closing file stream", e);
			}
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
	private void updateSavePath(File inputFile, AnalysisDataset dataset) {
		
		log(Level.FINE, "File path has changed: attempting to relocate images");

		dataset.setSavePath(inputFile);
		
		if(!dataset.hasMergeSources()){

			// This should be /ImageDir/DateTimeDir/
			File expectedAnalysisDirectory = inputFile.getParentFile();

			// This should be /ImageDir/
			File expectedImageDirectory = expectedAnalysisDirectory.getParentFile();
			
			try {
			dataset.updateSourceImageDirectory(expectedImageDirectory);
			} catch (IllegalArgumentException e){
				log(Level.WARNING, e.getMessage());
			}
//			updateSourceImageDirectory(expectedImageDirectory, dataset);

		}else {
			log(Level.WARNING, "Dataset is a merge");
			log(Level.WARNING, "Unable to find single source image directory");
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
