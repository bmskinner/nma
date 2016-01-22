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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import components.generic.BorderTag;
import components.generic.ProfileCollectionType;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;
import components.nuclei.sperm.RodentSpermNucleus;
import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import analysis.nucleus.DatasetProfiler;
import utility.Constants;
import utility.Version;

public class PopulationImportWorker extends AnalysisWorker {
	
	private File file;
	private AnalysisDataset dataset;
	
	public PopulationImportWorker(Logger programLogger, File f){
		super(null, programLogger);
		this.file = f;
		this.setProgressTotal(1);
	}
	
	public AnalysisDataset getLoadedDataset(){
		return this.dataset;
	}
	
	@Override
	protected Boolean doInBackground() {
		
		try {
			dataset = PopulationImporter.readDataset(file, programLogger);
			
			programLogger.log(Level.FINE, "Read dataset");
			if(checkVersion( dataset.getVersion() )){

				programLogger.log(Level.FINE, "Version check OK");
				dataset.setRoot(true);

				
				// update the log file to the same folder as the dataset
				File logFile = new File(file.getParent()
						+File.separator
						+file.getName().replace(Constants.SAVE_FILE_EXTENSION, Constants.LOG_FILE_EXTENSION));
				
				dataset.getCollection().setDebugFile(logFile);
				programLogger.log(Level.FINE, "Updated log file location");
				
				
				// If rodent sperm, check if the TOP_VERTICAL and BOTTOM_VERTICAL 
				// points have been set, and if not, add them
				if(dataset.getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)){
					
					if(! dataset.getCollection()
							.getProfileCollection(ProfileCollectionType.REGULAR)
							.hasBorderTag(BorderTag.TOP_VERTICAL)  ){
						
						programLogger.log(Level.FINE, "TOP_ and BOTTOM_VERTICAL not assigned; calculating");
						calculateTopAndBottomVerticals(dataset);
						programLogger.log(Level.FINE, "Calculating TOP and BOTTOM for child datasets");
						for(AnalysisDataset child : dataset.getAllChildDatasets()){
							calculateTopAndBottomVerticals(child);
						}
						
					}
					Version version = Version.parseString(dataset.getVersion());
					
					// This is when bugs were fixed for hook hump assignment
					Version testVersion = new Version(1, 11, 6);
					
					/*
					 * Correct hook-hump for older versions
					 * Don't run a load of calculations unnecessarily in newer versions
					 */
					if(version.isOlderThan(testVersion)){
						
						programLogger.log(Level.FINE, "Updating older dataset hook-hump split and signals");
						updateRodentSpermHookHumpSplits(dataset);
						for(AnalysisDataset child : dataset.getAllChildDatasets()){
							updateRodentSpermHookHumpSplits(child);
						}
					}
					
				}
				
				return true;
				
			} else {
				programLogger.log(Level.SEVERE, "Unable to open dataset version: "+ dataset.getVersion());
				return false;
			}
			
			
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
	private void updateRodentSpermHookHumpSplits(AnalysisDataset d) throws Exception{
		
		if(d.getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)){
			for(Nucleus n : d.getCollection().getNuclei()){

				RodentSpermNucleus nucleus = (RodentSpermNucleus) n;
				// recalculate - old datasets have problems
				nucleus.splitNucleusToHeadAndHump();

				// recalculate signal angles - old datasets have problems
				nucleus.calculateSignalAnglesFromPoint(nucleus.getPoint(BorderTag.ORIENTATION_POINT));
			}
		}
		
	}
	
	private void calculateTopAndBottomVerticals(AnalysisDataset dataset) throws Exception {
		
		programLogger.log(Level.FINE, "Detecting flat region");
		DatasetProfiler.TailFinder.assignTopAndBottomVerticalInMouse(dataset.getCollection());
		
		programLogger.log(Level.FINE, "Assigning flat region to nuclei");
		DatasetProfiler.Offsetter.assignFlatRegionToMouseNuclei(dataset.getCollection());
	}
	
	/**
	 * Check a version string to see if the program will be able to open a 
	 * dataset. The major version must be the same, while the revision of the
	 * dataset must be equal to or greater than the program revision. Bugfixing
	 * versions are not checked for.
	 * @param version
	 * @return a pass or fail
	 */
	public boolean checkVersion(String version){
		boolean ok = true;
		
		if(version==null){ // allow for debugging, but warn
			programLogger.log(Level.WARNING, "No version info found: functions may not work as expected");
			return true;
		}
		
		Version v = Version.parseString(version);
		
//		String[] parts = version.split("\\.");
		
		// major version MUST be the same
		if(v.getMajor()!=Constants.VERSION_MAJOR){
			ok = false;
		}
		// dataset revision should be equal or greater to program
		if(v.getMinor()<Constants.VERSION_MINOR){
			programLogger.log(Level.WARNING, "Dataset was created with an older version of the program");
			programLogger.log(Level.WARNING, "Some functionality may not work as expected");
		}
		return ok;
	}
	
	

}
