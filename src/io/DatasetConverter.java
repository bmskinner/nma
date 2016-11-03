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
import components.active.DefaultNuclearSignal;
import components.active.DefaultNucleus;
import components.active.DefaultPigSpermNucleus;
import components.active.DefaultRodentSpermNucleus;
import components.active.ProfileableCellularComponent.IndexOutOfBoundsException;
import components.active.VirtualCellCollection;
import components.active.generic.UnavailableBorderTagException;
import components.active.generic.UnavailableProfileTypeException;
import components.generic.IPoint;
import components.generic.ISegmentedProfile;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.INuclearSignal;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import analysis.IAnalysisDataset;
import analysis.profiles.ProfileException;

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
			log("Old dataset version : "+oldDataset.getVersion());
			log("Shiny target version: "+Version.currentVersion());
			log("Beginning conversion");
			
			
			backupOldDataset();

			ICellCollection newCollection = makeNewRootCollection();

			IAnalysisDataset newDataset = new DefaultAnalysisDataset(newCollection, oldDataset.getSavePath());

			newDataset.setAnalysisOptions(oldDataset.getAnalysisOptions());
			newDataset.setDatasetColour(oldDataset.getDatasetColour());

			// arrange root cluster groups
//			log("Creating cluster groups");
			for(IClusterGroup oldGroup : oldDataset.getClusterGroups()){

				IClusterGroup newGroup = new ClusterGroup(oldGroup);

				newDataset.addClusterGroup(newGroup);

			}

//			log("Creating child collections");

			// add the child datasets
			makeVirtualCollections(oldDataset, newDataset);

			return newDataset;

		} catch(Exception e){
			error("Error converting dataset", e);
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

//			log("\tConverting: "+child.getName());
			
			ICellCollection oldCollection = child.getCollection();
			// make a virtual collection for the cells 
			ICellCollection newCollection = new VirtualCellCollection(dest, child.getName(), child.getUUID());
			
			for(ICell c : child.getCollection().getCells()){
				newCollection.addCell(c);
			}
			
			newCollection.createProfileCollection();
			
			// Copy segmentation patterns over
			try {
				oldCollection.getProfileManager().copyCollectionOffsets(newCollection);
			} catch (ProfileException e) {
				error("Unable to copy collection offsets", e);
			}
			
			dest.addChildCollection(newCollection);
			
			IAnalysisDataset destChild = dest.getChildDataset(newCollection.getID());
			
//			log("\tMaking clusters: "+child.getName());
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
			error("Error updating profiles across datasets", e);
			throw new DatasetConversionException("Profiling error in root dataset");
		}
		
		for(UUID id : oldCollection.getSignalGroupIDs()){
			newCollection.addSignalGroup(id, oldCollection.getSignalGroup(id));
		}
		
		
		return newCollection;
		
	}
	
	private ICell createNewCell(ICell oldCell) throws DatasetConversionException{
		ICell newCell = new DefaultCell(oldCell.getId());
		
		// make a new nucleus
		Nucleus newNucleus = createNewNucleus( oldCell.getNucleus()  );
		
		newCell.setNucleus(newNucleus);
		
		return newCell;
		
	}
	
	private Nucleus createNewNucleus(Nucleus n) throws DatasetConversionException{
		
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
	
	private Nucleus makeRoundNucleus(Nucleus n) throws DatasetConversionException{
				
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
	
	private Nucleus makeRodentNucleus(Nucleus n) throws DatasetConversionException{
		
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
	
	private Nucleus makePigNucleus(Nucleus n) throws DatasetConversionException{
		
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
	
	
	private Nucleus copyGenericData(Nucleus template, Nucleus newNucleus) throws DatasetConversionException{
		
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


		fine("\tCopying tags");
		//Copy the existing border tags
		for(Tag t : template.getBorderTags().keySet()){
			
			if(t.equals(Tag.INTERSECTION_POINT)){
				continue;
			}
			
			try {
				
				template.getBorderPoint(t);
				
				
				
				// get the proporitonal index of the old tag
				
				double propIndex = (double) template.getBorderIndex(t) / (double) template.getBorderLength();
				
				int newIndex = (int) ((double) newNucleus.getBorderLength() * propIndex);
				
				fine("\tChanging tag "+t+" to index "+newIndex+" : "+propIndex);
				newNucleus.setBorderTag(t, newIndex);
			} catch (UnavailableBorderTagException | IndexOutOfBoundsException e) {
				fine("Cannot set border tag to requested index", e);
			}
			finer("\tSetting tag "+t);
		}

		// Copy segments from RP
		for(ProfileType type : ProfileType.values()){

			fine("\nCopying profile type "+type);
			try {
				ISegmentedProfile profile = template.getProfile(type, Tag.REFERENCE_POINT);
				fine("\tGot the template profile for "+type);
				ISegmentedProfile target = newNucleus.getProfile(type, Tag.REFERENCE_POINT);
				fine("\tGot the target profile for "+type);
				
				if(profile.size() != target.size()){
					fine("\tNew nucleus profile length of "+target.size()+" : original nucleus was "+profile.size());
					target = profile.interpolate(target.size());
					fine("\tInterpolated profile");
				}

				fine("\tSetting the profile "+type+" in the new nucleus");
				newNucleus.setProfile(type, Tag.REFERENCE_POINT, target);

				
			} catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e1) {
				fine("Error getting profile from template or target nucleus", e1);
				throw new DatasetConversionException("Cannot convert nucleus", e1);
			} 
			fine("Complete profile type "+type);
		}
		
		// Copy signals
		
		
		for(UUID signalGroup : template.getSignalCollection().getSignalGroupIDs()){
			
			for(INuclearSignal s : template.getSignalCollection().getSignals(signalGroup)){
				
				// Get the roi for the old nucleus
				float[] xpoints = new float[s.getBorderLength()], ypoints = new float[s.getBorderLength()];
				
				for(int i=0; i<xpoints.length; i++){
					xpoints[i] = (float) s.getBorderPoint(i).getX();
					ypoints[i] = (float) s.getBorderPoint(i).getY();
				}
				
				PolygonRoi roi = new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.TRACED_ROI);
				
				INuclearSignal newSignal = new DefaultNuclearSignal(roi, 
						s.getSourceFile(), 
						s.getChannel(), s.getPosition(), s.getCentreOfMass());
				
				for(PlottableStatistic st : s.getStatistics()){
					newSignal.setStatistic(st, s.getStatistic(st));;
					
				}
				
				newNucleus.getSignalCollection().addSignal(newSignal, signalGroup);
				
			}
			
		}
		

		fine("Created nucleus "+newNucleus.getNameAndNumber()+"\n");

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
