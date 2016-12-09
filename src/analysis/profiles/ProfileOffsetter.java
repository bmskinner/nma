/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package analysis.profiles;

import java.util.UUID;

import stats.Quartile;
import logging.Loggable;
import components.ICellCollection;
import components.ProfileableCellularComponent.IndexOutOfBoundsException;
import components.generic.ISegmentedProfile;
import components.generic.ProfileType;
import components.generic.Tag;
import components.generic.UnavailableBorderTagException;
import components.generic.UnavailableProfileTypeException;
import components.generic.UnsegmentedProfileException;
import components.nuclear.IBorderSegment;
import components.nuclei.Nucleus;

/**
 * Offset the profiles of individual nuclei within a CellCollection
 * based on the similarities of the profile to the collection median
 * @author bms41
 *
 */
public class ProfileOffsetter implements Loggable {
	
	final private ICellCollection collection;
	
	public ProfileOffsetter(final ICellCollection collection){
		
		if(collection==null){
			throw new IllegalArgumentException("Collection cannot be null");
		}
				
		this.collection = collection;
	}
		
	
	/**
	 * This method requires the frankenprofiling to be completed
	 * @throws ProfileOffsetException
	 */
	public void assignBorderTagToNucleiViaFrankenProfile(Tag tag) throws ProfileOffsetException {

		int index;
		try {
			index = collection.getProfileCollection().getIndex(tag);
		} catch (UnavailableBorderTagException e2) {
			throw new ProfileOffsetException("Cannot find "+tag+" index in median", e2);
		} 

		
		UUID segID;
		try {
			segID = collection.getProfileCollection()
					.getSegmentContaining(tag).getID();
		} catch (ProfileException | UnsegmentedProfileException e) {
			throw new ProfileOffsetException("Cannot find segment with tag "+tag+" in median");
		}




		ISegmentedProfile profile;
		try {
			profile = collection.getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);
		} catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException | UnsegmentedProfileException e1) {
			fine("Error getting median profile", e1);
			throw new ProfileOffsetException("Cannot get median profile");
		}

		
		IBorderSegment segFromRef    = profile.getSegment(segID);


		/*
		 * Get the proportion of the index through the segment
		 */
		double proportion    = segFromRef.getIndexProportion(index);
		finest("Found "+tag+" at "+proportion+" through median segment "+segFromRef.getID());


		/*
		 * Go through each nucleus and apply the position
		 */
		finer("Updating tag location in nuclei");
		for(Nucleus nucleus : collection.getNuclei()){

			int oldNIndex = nucleus.getBorderIndex(tag);
			if(oldNIndex==-1){
				finer("Border tag does not exist and will be created");
			} else {
				finer("Previous "+tag+" index at "+oldNIndex);
			}
			
			try {
				IBorderSegment nucleusSegment = nucleus.getProfile(ProfileType.ANGLE)
						.getSegment(segID);

				if(nucleusSegment==null){
					warn("Error updating nucleus, segment "+segID+" not found");
				} else {
					finest("Using nucleus segment "+nucleusSegment.getID());
				}

				int newIndex = nucleusSegment.getProportionalIndex(proportion); // find the index in the segment closest to the proportion 

				if(newIndex==-1){
					warn("Cannot find "+tag+" index in nucleus profile at proportion "+proportion);
					continue;
				}

				

				nucleus.setBorderTag(tag, newIndex);
				finest("Set border tag in nucleus to "+newIndex+ " from "+oldNIndex);
			} catch (IndexOutOfBoundsException | UnavailableProfileTypeException e) {
				fine("Cannot set "+tag+" index in nucleus profile", e);
			}		
			
		}
		
	}
	
	/**
	 * Use the proportional segment method to update top and bottom vertical positions
	 * within the dataset
	 * @throws Exception
	 */
	public void reCalculateVerticals() throws ProfileOffsetException {
		assignTopAndBottomVerticalsViaFrankenProfile();
	}
	
	
	/*
	 * 
	 * PRIVATE METHODS
	 * 
	 */
	
	/**
	 * This method requires the frankenprofiling to be completed
	 * @throws Exception
	 */
	private void assignTopAndBottomVerticalsViaFrankenProfile() throws ProfileOffsetException {
				
		
		/*
		 * Franken profile method: segment proportionality
		 */
		
		assignBorderTagToNucleiViaFrankenProfile(Tag.TOP_VERTICAL);
		assignBorderTagToNucleiViaFrankenProfile(Tag.BOTTOM_VERTICAL);
		
		
		for(Nucleus nucleus : collection.getNuclei()){			
			nucleus.updateVerticallyRotatedNucleus();
		}	
		
	}
	

	
	public class ProfileOffsetException extends Exception {
		private static final long serialVersionUID = 1L;
		public ProfileOffsetException() { super(); }
		public ProfileOffsetException(String message) { super(message); }
		public ProfileOffsetException(String message, Throwable cause) { super(message, cause); }
		public ProfileOffsetException(Throwable cause) { super(cause); }
	}

}
