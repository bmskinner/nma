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
import java.lang.reflect.Field;

import static java.nio.file.StandardCopyOption.*;

import java.nio.file.Files;
import java.util.UUID;

import logging.Loggable;
import stats.PlottableStatistic;
import utility.Constants;
import utility.Version;
import components.ClusterGroup;
import components.ICell;
import components.ICellCollection;
import components.IClusterGroup;
import components.active.DefaultAnalysisDataset;
import components.active.DefaultCell;
import components.active.DefaultCellCollection;
import components.active.DefaultCellularComponent;
import components.active.DefaultNucleus;
import components.active.DefaultPigSpermNucleus;
import components.active.DefaultRodentSpermNucleus;
import components.active.VirtualCellCollection;
import components.generic.IPoint;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
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
	public IAnalysisDataset convert() throws DatasetConversionException {

		try{
			log("Beginning conversion from "+oldDataset.getVersion()+" to "+Version.currentVersion());
			backupOldDataset();

			ICellCollection newCollection = makeNewRootCollection();

			IAnalysisDataset newDataset = new DefaultAnalysisDataset(newCollection, oldDataset.getSavePath());

			newDataset.setAnalysisOptions(oldDataset.getAnalysisOptions());
			newDataset.setDatasetColour(oldDataset.getDatasetColour());

			// arrange root cluster groups
			log("Creating cluster groups");
			for(IClusterGroup oldGroup : oldDataset.getClusterGroups()){

				IClusterGroup newGroup = new ClusterGroup(oldGroup);

				newDataset.addClusterGroup(newGroup);

			}

			log("Creating child collections");

			// add the child datasets
			makeVirtualCollections(oldDataset, newDataset);

			return newDataset;

		} catch(Exception e){
			error("Error converting dataset",e);
			throw new DatasetConversionException(e);
		}
	}
	
	/**
	 * Recursively create cell collections for all child datasets
	 * @param template
	 * @param dest
	 */
	private void makeVirtualCollections(IAnalysisDataset template, IAnalysisDataset dest) throws DatasetConversionException{
		
		
		for(IAnalysisDataset child : template.getChildDatasets()){

			log("\tConverting: "+child.getName());
			
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
			
			IAnalysisDataset destChild = dest.getChildDataset(newCollection.getID());
			
			log("\tMaking clusters: "+child.getName());
			for(IClusterGroup oldGroup : child.getClusterGroups()){
				
				IClusterGroup newGroup = new ClusterGroup(oldGroup);
				
				destChild.addClusterGroup(newGroup);
				
			}
			
			// Recurse until complete
			makeVirtualCollections(child, dest.getChildDataset(newCollection.getID()));
			
		}
	}
	
	
	/**
	 * Copy the cells and signal groups from the old collection
	 * @return
	 * @throws DatasetConversionException 
	 */
	private ICellCollection makeNewRootCollection() throws DatasetConversionException{
		fine("Converting root: "+oldDataset.getName());
		ICellCollection oldCollection = oldDataset.getCollection();

		ICellCollection newCollection = new DefaultCellCollection(oldCollection.getFolder(),
				oldCollection.getOutputFolderName(),
				oldCollection.getName(),
				oldCollection.getNucleusType());
		
		for(ICell c : oldCollection.getCells()){
			
			ICell newCell = createNewCell(c);
						
			newCollection.addCell( newCell );
		}
		
		try {

			newCollection.createProfileCollection();
			
			// Copy segmentation patterns over
			oldCollection.getProfileManager().copyCollectionOffsets(newCollection);
		} catch(Exception e){
			fine("Error updating profiles across datasets", e);
			throw new DatasetConversionException("Profiling error in root dataset");
		}
		
		for(UUID id : oldCollection.getSignalGroupIDs()){
			newCollection.addSignalGroup(id, oldCollection.getSignalGroup(id));
		}
		
		
		return newCollection;
		
	}
	
	private ICell createNewCell(ICell oldCell){
		ICell newCell = new DefaultCell(oldCell.getId());
		
		// make a new nucleus
		Nucleus newNucleus = createNewNucleus( oldCell.getNucleus()  );
		
		newCell.setNucleus(newNucleus);
		
		return newCell;
		
	}
	
	private Nucleus createNewNucleus(Nucleus n){
		
		NucleusType type = oldDataset.getCollection().getNucleusType();
		
		fine("\tCreating nucleus: "+n.getNameAndNumber()+"\t"+type);
		
		Nucleus newNucleus;
		
		switch(type){
		case PIG_SPERM:
			newNucleus = makePigNucleus(n);
			break;
		case RODENT_SPERM:
			newNucleus = makeRodentNucleus(n);
			break;
		case ROUND:
			newNucleus = makeRoundNucleus(n);
			break;
		default:
			newNucleus = makeRoundNucleus(n);
			break;
	
		
		}
		
		
		return newNucleus;
		
	}
	
	private Nucleus makeRoundNucleus(Nucleus n){
				
		// Easy stuff
		File f      = n.getSourceFile(); // the source file
		int channel = n.getChannel();// the detection channel
		int number  = n.getNucleusNumber(); // copy over
		IPoint com  = n.getCentreOfMass();
		
		// Position converted down internally
		int[] position = n.getPosition();
		
		// Get the roi for the old nucleus
		float[] xpoints = new float[n.getBorderLength()], ypoints = new float[n.getBorderLength()];
		
		for(int i=0; i<xpoints.length; i++){
			xpoints[i] = (float) n.getBorderPoint(i).getX();
			ypoints[i] = (float) n.getBorderPoint(i).getY();
		}
		
		PolygonRoi roi = new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.TRACED_ROI);
		
		
		// Use the default constructor
		Nucleus newNucleus = new DefaultNucleus(roi, f, channel, position, number, com);

		newNucleus = copyGenericData(n, newNucleus);
		return  newNucleus;

	}
	
	private Nucleus makeRodentNucleus(Nucleus n){
		
		// Easy stuff
		File f      = n.getSourceFile(); // the source file
		int channel = n.getChannel();// the detection channel
		int number  = n.getNucleusNumber(); // copy over
		IPoint com  = n.getCentreOfMass();
		
		// Position converted down internally
		int[] position = n.getPosition();
		
		// Get the roi for the old nucleus
		float[] xpoints = new float[n.getBorderLength()], ypoints = new float[n.getBorderLength()];
		
		for(int i=0; i<xpoints.length; i++){
			xpoints[i] = (float) n.getBorderPoint(i).getX();
			ypoints[i] = (float) n.getBorderPoint(i).getY();
		}
		
		PolygonRoi roi = new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.TRACED_ROI);
		
		finer("\tCreated roi");
		// Use the default constructor
		Nucleus newNucleus = new DefaultRodentSpermNucleus(roi, f, channel, position, number, com);
		
		newNucleus = copyGenericData(n, newNucleus);
		return newNucleus;
	}
	
	private Nucleus makePigNucleus(Nucleus n){
		
		// Easy stuff
		File f      = n.getSourceFile(); // the source file
		int channel = n.getChannel();// the detection channel
		int number  = n.getNucleusNumber(); // copy over
		IPoint com  = n.getCentreOfMass();
		
		// Position converted down internally
		int[] position = n.getPosition();
		
		// Get the roi for the old nucleus
		float[] xpoints = new float[n.getBorderLength()], ypoints = new float[n.getBorderLength()];
		
		for(int i=0; i<xpoints.length; i++){
			xpoints[i] = (float) n.getBorderPoint(i).getX();
			ypoints[i] = (float) n.getBorderPoint(i).getY();
		}
		
		PolygonRoi roi = new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.TRACED_ROI);
		
		
		// Use the default constructor
		Nucleus newNucleus = new DefaultPigSpermNucleus(roi, f, channel, position, number, com);
		
		newNucleus = copyGenericData(n, newNucleus);
		
		return  newNucleus;

	}
	
	
	private Nucleus copyGenericData(Nucleus template, Nucleus newNucleus){
		
		// The nucleus ID is created with the nucleus and is not accessible
		// Use reflection to get access and set the new id to the same as the 
		// template

		try {
//			fine("Created nucleus id is "+newNucleus.getID());			
			Class<DefaultCellularComponent> superClass = DefaultCellularComponent.class;
			Field field = superClass.getDeclaredField("id");
			field.setAccessible(true);
			field.set(newNucleus, template.getID() );
			field.setAccessible(false);


		} catch (NoSuchFieldException e) {
			fine("No field", e);
		} catch (SecurityException e) {
			fine("Security error", e);
		} catch (IllegalArgumentException e) {
			fine("Illegal argument", e);
		} catch (IllegalAccessException e) {
			fine("Illegal access", e);
		} catch(Exception e){
			fine("Unexpected exception", e);
		}

//		fine("New nucleus id is "+newNucleus.getID());

		finer("\tCreated nucleus object");

		for(PlottableStatistic stat : template.getStatistics() ){
			try {
				newNucleus.setStatistic(stat, template.getStatistic(stat, MeasurementScale.PIXELS));
			} catch (Exception e) {
				fine("Error setting statistic: "+stat, e);
				newNucleus.setStatistic(stat, 0);
			}
		}

		newNucleus.setScale(template.getScale());


		// Create the profiles within the nucleus
		finer("\tInitialising");

		newNucleus.initialise(template.getWindowProportion(ProfileType.ANGLE));


		finer("\tCopying tags");
		//Copy the existing border tags
		for(Tag t : template.getBorderTags().keySet()){
			finer("\tSetting tag "+t);
			newNucleus.setBorderTag(t, template.getBorderIndex(t));
			finer("\tSetting tag "+t);
		}

		// TODO: Copy segments



		fine("Created nucleus");

		return  newNucleus;
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
	
	public class DatasetConversionException extends Exception {
		private static final long serialVersionUID = 1L;
		public DatasetConversionException() { super(); }
		public DatasetConversionException(String message) { super(message); }
		public DatasetConversionException(String message, Throwable cause) { super(message, cause); }
		public DatasetConversionException(Throwable cause) { super(cause); }
	}

}
