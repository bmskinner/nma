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
package com.bmskinner.nuclear_morphology.gui.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.dialogs.FishRemappingDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.FishRemappingProber;

import ij.io.DirectoryChooser;

/**
 * Compare morphology images with post-FISH images, and select nuclei into new
 * sub-populations
 */
public class FishRemappingAction extends ProgressableAction {
	
	private static final String SELECT_FOLDER_LBL      = "Select directory of post-FISH images...";
	
	private static final String CANNOT_USE_FOLDER      = "Cannot use folder";
    private static final String NOT_A_FOLDER_ERROR     = "The selected item is not a folder";
    private static final String FOLDER_NOT_FOUND_ERROR = "The folder does not exist";
    private static final String FILES_NOT_FOUND_ERROR  = "The folder contains no files";
	
	private File fishDir;

	public FishRemappingAction(final List<IAnalysisDataset> datasets, final MainWindow mw) {
		super(datasets, "Remapping", mw);
		
	}
	
	@Override
	public void run(){
		try{
//			
//			if(datasets.size()>1){
//				log( "Multiple datasets selected, cancelling");
//				cancel();
//				return;
//			}
//	
//			final IAnalysisDataset dataset = datasets.get(0);

			if(dataset.hasMergeSources()){
				warn("Cannot remap merged datasets");
				cancel();
				return;
			}
			
			if( ! getPostFISHDirectory()){
				log("Remapping cancelled");
				cancel();
				return;
			}
			
			if( fishDir==null){
				warn("Null FISH directory");
				cancel();
				return;
			}

			FishRemappingProber fishMapper = new FishRemappingProber(dataset, fishDir);
//			FishRemappingDialog fishMapper = new FishRemappingDialog(mw, dataset);

			if(fishMapper.isOk()){

				log("Fetching collections...");
				final List<IAnalysisDataset> newList = fishMapper.getNewDatasets();

				if(newList.isEmpty()){
					log("No collections returned");
					cancel();
					return;
				}
					
				log("Reapplying morphology...");

				new RunSegmentationAction(newList, dataset, MainWindow.ADD_POPULATION, mw);
				finished();


			} else {
				log("Remapping cancelled");
				cancel();
			}



		} catch(Exception e){
			warn("Error in FISH remapping: "+e.getMessage());
			stack("Error in FISH remapping: "+e.getMessage(), e);
		}
	}
	
	@Override
	public void finished(){
		// Do not use super.finished(), or it will trigger another save action
		fine("FISH mapping complete");
		cancel();		
		this.removeInterfaceEventListener(mw);
		this.removeDatasetEventListener(mw);		
	}
	
	/**
	 * Choose the directory containing the post-FISH images
	 * @return true if the directory is valid, false otherwise
	 */
	private boolean getPostFISHDirectory(){
		DirectoryChooser.setDefaultDirectory(dataset.getAnalysisOptions()
				.getDetectionOptions(IAnalysisOptions.NUCLEUS).getFolder().getAbsolutePath());
		DirectoryChooser dc = new DirectoryChooser(SELECT_FOLDER_LBL);
				
	    String folderName = dc.getDirectory();

	    if(folderName==null) return false; // user cancelled
	   
	    File folder =  new File(folderName);
	    
	    if(!folder.isDirectory() ){
	    	JOptionPane.showMessageDialog(null, NOT_A_FOLDER_ERROR, CANNOT_USE_FOLDER, JOptionPane.ERROR_MESSAGE); 
	    	return false;
	    }
	    if(!folder.exists()){
	    	JOptionPane.showMessageDialog(null, FOLDER_NOT_FOUND_ERROR, CANNOT_USE_FOLDER, JOptionPane.ERROR_MESSAGE); 
	    	return false; // check folder is ok
	    }
	    
	    if(!containsFiles(folder)){
	    	
	    	JOptionPane.showMessageDialog(null, FILES_NOT_FOUND_ERROR, CANNOT_USE_FOLDER, JOptionPane.ERROR_MESSAGE); 
	    	return false; // check folder has something in it
	    }
	    

	    fishDir = folder;
	    finer("Selected "+fishDir.getAbsolutePath()+" as post-FISH image directory");
	    return true;
	}
	
	/**
	 * Check if the given folder has files (not just directories)
	 * @param folder
	 * @return
	 */
	private boolean containsFiles(File folder){
				
		File[] files = folder.listFiles();
		
		// There must be items in the folder
		if(files.length==0){
			return false;
		}
		
		int countFiles=0;
		
		// Some of the items must be files
		for(File f : files){
			if(f.isFile()){
				countFiles++;
			}
		}
		
		if(countFiles==0){
			return false;
		}
		
		return true;
		
	}
}
