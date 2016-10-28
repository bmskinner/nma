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
import java.io.IOException;

import static java.nio.file.StandardCopyOption.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import logging.Loggable;
import utility.Constants;
import components.ICell;
import components.ICellCollection;
import components.active.DefaultAnalysisDataset;
import components.active.DefaultCell;
import components.active.DefaultCellCollection;
import components.active.VirtualCellCollection;
import analysis.IAnalysisDataset;

/**
 * This class will take old format datasets
 * and convert them to use the newer objects.
 * @author bms41
 *
 */
public class DatasetConverter implements Loggable {
	
	private IAnalysisDataset oldDataset;
	
	public DatasetConverter(IAnalysisDataset old){
		this.oldDataset = old;
	}
	
	/**
	 * Run the converter and make a new DefaultAnalysisDataset from the root,
	 * and ChildAnalysisDatasets from children.
	 * @return
	 */
	public IAnalysisDataset convert(){
		
		backupOldDataset();
		
		ICellCollection newCollection = makeNewRootCollection();
		
		IAnalysisDataset newDataset = new DefaultAnalysisDataset(newCollection, oldDataset.getSavePath());
		
		newDataset.setAnalysisOptions(oldDataset.getAnalysisOptions());
		newDataset.setDatasetColour(oldDataset.getDatasetColour());
		
		// add the child datasets
		makeVirtualCollections(oldDataset, newDataset);
		
		return newDataset;
	}
	
	/**
	 * Recursively create cell collections for all child datasets
	 * @param template
	 * @param dest
	 */
	private void makeVirtualCollections(IAnalysisDataset template, IAnalysisDataset dest){
		
		
		for(IAnalysisDataset child : template.getChildDatasets()){
			
			log("Converting: "+child.getName());
			
			ICellCollection oldCollection = child.getCollection();
			// make a virtual collection for the cells 
			ICellCollection newCollection = new VirtualCellCollection(dest, child.getName(), child.getUUID());
			
			for(ICell c : child.getCollection().getCells()){
				newCollection.addCell(c);
			}
			
			newCollection.createProfileCollection();
			
			// Copy segmentation patterns over
			oldCollection.getProfileManager().copyCollectionOffsets(newCollection);
			
			dest.addChildCollection(newCollection);
			
			// Recurse until complete
			makeVirtualCollections(child, dest.getChildDataset(newCollection.getID()));
			
		}
	}
	
	
	/**
	 * Copy the cells and signal groups from the old collection
	 * @return
	 */
	private ICellCollection makeNewRootCollection(){
		log("Converting root: "+oldDataset.getName());
		ICellCollection oldCollection = oldDataset.getCollection();

		ICellCollection newCollection = new DefaultCellCollection(oldCollection.getFolder(),
				oldCollection.getOutputFolderName(),
				oldCollection.getName(),
				oldCollection.getNucleusType());
		
		for(ICell c : oldCollection.getCells()){
			newCollection.addCell( new DefaultCell(c));
		}
		
		newCollection.createProfileCollection();
		// Copy segmentation patterns over
		oldCollection.getProfileManager().copyCollectionOffsets(newCollection);
		
		for(UUID id : oldCollection.getSignalGroupIDs()){
			newCollection.addSignalGroup(id, oldCollection.getSignalGroup(id));
		}
		
		return newCollection;
		
	}
	
	/**
	 * Save a copy of the old dataset by renaming the nmd 
	 * file to a backup file. 
	 */
	private void backupOldDataset(){
		
		File saveFile = oldDataset.getSavePath();
		
		if(saveFile.exists()){
			
			String newFileName = saveFile.getAbsolutePath().replace(Constants.SAVE_FILE_EXTENSION, Constants.BAK_FILE_EXTENSION);
			
			log("Renaming to "+newFileName);
			
			File newFile = new File(newFileName);
			
			if(newFile.exists()){
				
				warn("Backup file exists, overwriting");
				
			}


			try {
				
				Files.copy(saveFile.toPath(), newFile.toPath(), REPLACE_EXISTING);
				
				log("Backup file created");
			} catch (IOException e) {
				warn("Unable to make backup file");
				fine("Error copying file", e);
			}

			
		}
		
	}

}
