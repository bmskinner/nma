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

import logging.Loggable;
import components.ICellCollection;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.generic.Tag;
import components.nuclear.NucleusBorderSegment;
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
	 * @throws Exception
	 */
	public void assignBorderTagToNucleiViaFrankenProfile(Tag tag) throws ProfileOffsetException {

		int index = collection.getProfileCollection(ProfileType.ANGLE)
				.getIndex(tag); 

		/*
		 * Check that the points exist
		 */
		if( index == -1  ){
			throw new ProfileOffsetException("Cannot find "+tag+" index in median");
		}

		UUID segID;
		try {
			segID = collection.getProfileCollection(ProfileType.ANGLE)
					.getSegmentContaining(tag).getID();
		} catch (ProfileException e) {
			throw new ProfileOffsetException("Cannot find segment with tag "+tag+" in median");
		}




		SegmentedProfile profile = collection.getProfileCollection(ProfileType.ANGLE)
				.getSegmentedProfile(Tag.REFERENCE_POINT);

		
		NucleusBorderSegment segFromRef    = profile.getSegment(segID);


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
			
//			NucleusBorderSegment nucleusSegment = nucleus.getProfile(ProfileType.REGULAR)
//					.getSegment(segName);
			NucleusBorderSegment nucleusSegment = nucleus.getProfile(ProfileType.ANGLE)
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
