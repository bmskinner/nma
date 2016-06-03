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

import analysis.nucleus.Indexable;
import utility.Constants;
import logging.Loggable;
import components.AbstractCellularComponent;
import components.CellCollection;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileType;

/**
 * Detect features in a profile. This is designed to identify the 
 * orientation point and reference point in median profiles, and to identify the flat
 * region in mouse sperm nuclei (the top and bottom verticals)
 * @author bms41
 *
 */
public class ProfileFeatureFinder implements Loggable, Indexable {
	
	CellCollection collection;
	
	public ProfileFeatureFinder(CellCollection collection){
		this.collection = collection;
	}

	private void findTailInRodentSpermMedian() {

		
		collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.REFERENCE_POINT, 0);
		
		int opIndex = identifyBorderTagIndex(BorderTag.ORIENTATION_POINT);
		
		if(opIndex == -1){
			warn("Cannot find OP index");
			return;
		}
				
		collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.ORIENTATION_POINT, opIndex);
		
		finer("Added OP at index "+opIndex);
		
		// can't use regular tail detector, because it's based on NucleusBorderPoints
		// get minima in curve, then find the lowest minima / minima furthest from both ends
//		collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.REFERENCE_POINT, 0);
//
//		Profile medianProfile;
//		try {
//			medianProfile = collection.getProfileCollection(ProfileType.REGULAR).getProfile(BorderTag.REFERENCE_POINT, 50);
//		} catch (Exception e) {
//			error("Error getting median profile for collection", e);
//			return;
//		}
//
//		BooleanProfile minima = medianProfile.smooth(2).getLocalMinima(5); // window size 5
//
//		double minAngle = 180;
//		int tailIndex = medianProfile.size() >> 1; // set the default tail at the half way point from the tip. 
//
//		int tipExclusionIndex1 = (int) (medianProfile.size() * 0.2);
//		int tipExclusionIndex2 = (int) (medianProfile.size() * 0.6);
//
//		for(int i = 0; i<minima.size();i++){
//			if( minima.get(i)==true){
//				int index = i;
//
//				double angle = medianProfile.get(index);
//				if(angle<minAngle && index > tipExclusionIndex1 && index < tipExclusionIndex2){ // get the lowest point that is not near the tip
//					minAngle = angle;
//					tailIndex = index;
//				}
//			}
//		}
//
//		collection.getProfileCollection(ProfileType.REGULAR)
//			.addOffset(BorderTag.ORIENTATION_POINT, tailIndex);

	}
	
	/**
	 * Find the flat region of the nucleus under the hook in the median profile, and mark the ends of the region
	 * with BorderTags TOP_VERTICAL and BOTTOM_VERTICAL
	 * @param collection
	 * @throws Exception
	 */
	public void assignTopAndBottomVerticalInMouse() {
		
		
		int tvIndex = identifyBorderTagIndex(BorderTag.TOP_VERTICAL);
		int bvIndex = identifyBorderTagIndex(BorderTag.BOTTOM_VERTICAL);
		
		if(tvIndex == -1){
			fine("Cannot identify top vertical");
			return;
		}
		
		if(bvIndex == -1){
			fine("Cannot identify bottom vertical");
			return;
		}
		
		collection.getProfileCollection(ProfileType.REGULAR)
			.addOffset(BorderTag.TOP_VERTICAL, tvIndex);
		finer("Added TV at index "+tvIndex);
		
		collection.getProfileCollection(ProfileType.REGULAR)
			.addOffset(BorderTag.BOTTOM_VERTICAL, bvIndex);
		finer("Added BV at index "+bvIndex);
				
//		collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.ORIENTATION_POINT, opIndex);
//		 
//		Profile medianProfile;
//		try {
//			medianProfile = collection.getProfileCollection(ProfileType.REGULAR).getProfile(BorderTag.REFERENCE_POINT, 50);
//		} catch (Exception e) {
//			error("Error getting median profile for collection", e);
//			return;
//		}
//
//		/*
//	     * Call to a StraightPointFinder that will find the straight part of the nucleus
//	     * Use this to set the BorderTag.TopVertical and BottomVertical
//	     */
//	    int[] verticalPoints = medianProfile.getConsistentRegionBounds(180, 2, 10);
//	    if(verticalPoints[0]!=-1 && verticalPoints[1]!=-1){
//	    	collection.getProfileCollection(ProfileType.REGULAR)
//	    		.addOffset(BorderTag.TOP_VERTICAL, verticalPoints[0]);
//	    	collection.getProfileCollection(ProfileType.REGULAR)
//	    	.addOffset(BorderTag.BOTTOM_VERTICAL, verticalPoints[1]);
//	    } else {
//	    	log(Level.WARNING, "Dataset "+collection.getName()+": Unable to assign vertical positions in median profile");
//	    }
		
	}

	private void findTailInPigSpermMedian() {
		
		int rpIndex = identifyBorderTagIndex(BorderTag.REFERENCE_POINT);
		
		if(rpIndex == -1){
			warn("Cannot find RP index");
			return;
		}
		
		collection.getProfileCollection(ProfileType.REGULAR)
			.addOffset(BorderTag.REFERENCE_POINT, rpIndex);
		finer("Added RP at index "+rpIndex);
		
		int opIndex = identifyBorderTagIndex(BorderTag.ORIENTATION_POINT);
		
		if(opIndex == -1){
			warn("Cannot find OP index");
			return;
		}
		
		collection.getProfileCollection(ProfileType.REGULAR)
			.addOffset(BorderTag.ORIENTATION_POINT, opIndex);
		finer("Added OP at index "+opIndex);
		
		int ipIndex = identifyBorderTagIndex(BorderTag.INTERSECTION_POINT);
		
		if(ipIndex == -1){
			warn("Cannot find IP index");
			return;
		}
		
		collection.getProfileCollection(ProfileType.REGULAR)
			.addOffset(BorderTag.INTERSECTION_POINT, ipIndex);
		
		finer("Added IP at index "+ipIndex);
//		
//		// ensure we have teh correct offset 
//		tailIndex = collection.getProfileCollection(ProfileType.REGULAR).getOffset(BorderTag.REFERENCE_POINT);
//		
//		collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.ORIENTATION_POINT, tailIndex);
//		finer("Added OP at index "+tailIndex);
//		/*
//		 * Looks like reference point needs to be 0. Check the process aligning the profiles - they must be settling on 
//		 * the RP 
//		 */
//		
//		// set the intersection point half way around from the tail
//		// find the offset to use
//		double length = (double) collection.getProfileCollection(ProfileType.REGULAR).getAggregate().length();		
//		int offset    =  (int) Math.ceil(length / 2d); // ceil to ensure offsets are correct
//		
//		
////		 adjust the intersection point index using the offset
//		int headIndex  = AbstractCellularComponent.wrapIndex( tailIndex - offset, collection.getProfileCollection(ProfileType.REGULAR).getAggregate().length());
//		collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.INTERSECTION_POINT, headIndex);
//		finer("Added IP at index "+headIndex);
		
//		// define the current zero offset at the reference point
//		// It does not matter, it just gives an offset key for the ProfileCollection
//		collection.getProfileCollection(ProfileType.REGULAR)
//			.addOffset(BorderTag.REFERENCE_POINT, 0);
//		
//		// get the profile
//		// This is starting from an arbitrary point?
//		// Starting from the head in test data, so the reference point is correct
//		Profile medianProfile;
//		try {
//			medianProfile = collection.getProfileCollection(ProfileType.REGULAR)
//					.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
//		} catch (Exception e) {
//			error("Error getting median profile for pig sperm", e);
//			return;
//		}
//
//
//		// find local maxima in the median profile over 180
//		BooleanProfile maxima = medianProfile.smooth(2).getLocalMaxima(5, 180); // window size 5, only values over 180
//
//
//		double minAngle = 180;
//		int tailIndex   = 0; // the tail is, by default, the reference point
//
//
//		if(maxima.size()==0){
//			log(Level.WARNING, "Error: no maxima found in median line");
//			
//			tailIndex = medianProfile.size() / 2; // set to roughly the middle of the array for the moment
//
//		} else{  // Maxima were found
//
//			for(int i = 0; i<maxima.size();i++){
//				
//				if(maxima.get(i)==true){ // look at local maxima
//					int index = i;
//
//					double angle = medianProfile.get(index); // get the angle at this maximum
//					
//					// look for the highest local maximum
//					if(angle>minAngle){
//						minAngle = angle;
//						tailIndex = index;
//					}
//				}
//			}
//		}
//
//		// add this index to be the reference point
//		collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.REFERENCE_POINT, tailIndex);
//		finer("Added RP at index "+tailIndex);
//		
//		
//		// ensure we have teh correct offset 
//		tailIndex = collection.getProfileCollection(ProfileType.REGULAR).getOffset(BorderTag.REFERENCE_POINT);
//		
//		collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.ORIENTATION_POINT, tailIndex);
//		finer("Added OP at index "+tailIndex);
//		/*
//		 * Looks like reference point needs to be 0. Check the process aligning the profiles - they must be settling on 
//		 * the RP 
//		 */
//		
//		// set the intersection point half way around from the tail
//		// find the offset to use
//		double length = (double) collection.getProfileCollection(ProfileType.REGULAR).getAggregate().length();		
//		int offset    =  (int) Math.ceil(length / 2d); // ceil to ensure offsets are correct
//		
//		
////		 adjust the intersection point index using the offset
//		int headIndex  = AbstractCellularComponent.wrapIndex( tailIndex - offset, collection.getProfileCollection(ProfileType.REGULAR).getAggregate().length());
//		collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.INTERSECTION_POINT, headIndex);
//		finer("Added IP at index "+headIndex);
	}
	

	private void findTailInRoundMedian() {
		
		int rpIndex = identifyBorderTagIndex(BorderTag.REFERENCE_POINT);
				
		if(rpIndex == -1){
			warn("Cannot find RP index");
			return;
		}
		
		collection.getProfileCollection(ProfileType.REGULAR)
			.addOffset(BorderTag.REFERENCE_POINT, rpIndex);
		finer("Added RP at index "+rpIndex);
		
		int opIndex = identifyBorderTagIndex(BorderTag.ORIENTATION_POINT);
		
		if(opIndex == -1){
			warn("Cannot find OP index");
			return;
		}
		
		collection.getProfileCollection(ProfileType.REGULAR)
			.addOffset(BorderTag.ORIENTATION_POINT, opIndex);
		finer("Added OP at index "+opIndex);
		
//		collection.getProfileCollection(ProfileType.REGULAR)
//			.addOffset(BorderTag.REFERENCE_POINT, 0);
//		
//		
//		collection.getProfileCollection(ProfileType.REGULAR)
//			.addOffset(BorderTag.ORIENTATION_POINT, 0);
	}

	/**
	 * Identify tail in median profile and offset nuclei profiles. For a 
	 * regular round nucleus, the tail is one of the points of longest
	 *  diameter, and lowest angle
	 * @param collection the nucleus collection
	 */
	public void findTailIndexInMedianCurve() {

		switch(collection.getNucleusType()){

			case PIG_SPERM:
				findTailInPigSpermMedian();	
				break;
			case RODENT_SPERM:
				findTailInRodentSpermMedian();
				assignTopAndBottomVerticalInMouse();
				break;
			default:
				findTailInRoundMedian();
				break;
		}
	}
	
	
	
	@Override
	public int identifyBorderTagIndex(BorderTag tag) {
		
		switch(tag){
		
			case REFERENCE_POINT:
				return identifyRPIndex();
			case ORIENTATION_POINT:
				return identifyOPIndex();
			case INTERSECTION_POINT:
				return identifyIPIndex();
			case TOP_VERTICAL:
				return identifyTVIndex();
			case BOTTOM_VERTICAL:
				return identifyBVIndex();
			default:
				return -1;
		}
		
	}
	
	/*
	 * PRIVATE METHODS FOR FINDING FEATURES IN THE PROFILES
	 * 
	 * 
	 * 
	 */
	
	
	/**
	 * Find the RP index using the appropriate method for
	 * the nucleus type
	 * @return
	 */
	private int identifyRPIndex(){
		
		int result = -1;
		switch(collection.getNucleusType()){

			case PIG_SPERM:
				result = identifyRPIndexInPigSpermNucleus();
				break;
			case RODENT_SPERM:
				result = identifyRPIndexInMouseSpermNucleus();
				break;
			default:
				result = identifyRPIndexInRoundNucleus();
				break;
		}
		return result;
	}
	
	/**
	 * Find the OP index using the appropriate method for
	 * the nucleus type
	 * @return
	 */
	private int identifyOPIndex(){
		
		int result = -1;
		switch(collection.getNucleusType()){

			case PIG_SPERM:
				result = identifyOPIndexInPigSpermNucleus();
				break;
			case RODENT_SPERM:
				result = identifyOPIndexInMouseSpermNucleus();
				break;
			default:
				result = identifyOPIndexInRoundNucleus();
				break;
		}
		return result;
	}
	
	/**
	 * Find the OP index using the appropriate method for
	 * the nucleus type
	 * @return
	 */
	private int identifyIPIndex(){
		
		int result = -1;
		switch(collection.getNucleusType()){

			case PIG_SPERM:
				result = identifyIPIndexInPigSpermNucleus();
				break;
			case RODENT_SPERM:
				result = identifyIPIndexInMouseSpermNucleus();
				break;
			default:
				result = identifyIPIndexInRoundNucleus();
				break;
		}
		return result;
	}
	
	/**
	 * Find the top vertical index using the appropriate method for
	 * the nucleus type
	 * @return
	 */
	private int identifyTVIndex(){
		
		int result = -1;
		switch(collection.getNucleusType()){

			case PIG_SPERM:
				result = identifyTopVerticalIndexInPigSpermNucleus();
				break;
			case RODENT_SPERM:
				result = identifyTopVerticalIndexInMouseSpermNucleus();
				break;
			default:
				result = identifyTopVerticalIndexInRoundNucleus();
				break;
		}
		return result;
	}
	
	/**
	 * Find the top vertical index using the appropriate method for
	 * the nucleus type
	 * @return
	 */
	private int identifyBVIndex(){
		
		int result = -1;
		switch(collection.getNucleusType()){

			case PIG_SPERM:
				result = identifyBottomVerticalIndexInPigSpermNucleus();
				break;
			case RODENT_SPERM:
				result = identifyBottomVerticalIndexInMouseSpermNucleus();
				break;
			default:
				result = identifyBottomVerticalIndexInRoundNucleus();
				break;
		}
		return result;
	}
	
	
	/*
	 * ROUND NUCLEUS
	 * 
	 */
	
	
	/**
	 * Find the RP in the profile of a round nucleus. 
	 * This is the index with the longest diameter
	 * @return The RP index or -1 on error
	 */
	private int identifyRPIndexInRoundNucleus(){
		
		try {
			Profile profile = collection.getProfileCollection(ProfileType.DISTANCE)
				.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
			
			return profile.getIndexOfMax();
		} catch (Exception e) {
			error("Error getting median profile", e);
			return -1;
		}
		
	}
	
	/**
	 * Find the OP in the profile of a round nucleus. 
	 * This is the same as the RP.
	 * @return
	 */
	private int identifyOPIndexInRoundNucleus(){
		return identifyRPIndexInRoundNucleus();
	}
	
	/**
	 * Find the IP in the profile of a round nucleus. 
	 * This is the index half way around the profile from the OP
	 * @return The RP index or -1 on error
	 */
	private int identifyIPIndexInRoundNucleus(){
		
		try {
			
			// set the intersection point half way around from the tail
			int opIndex = identifyOPIndexInRoundNucleus();
			
			// find the offset to use
			double length = (double) collection.getProfileCollection(ProfileType.REGULAR).getAggregate().length();		
			int offset    =  (int) Math.ceil(length / 2d); // ceil to ensure offsets are correct
			
			
//			 adjust the intersection point index using the offset
			int ipIndex  = AbstractCellularComponent.wrapIndex( opIndex - offset, collection.getProfileCollection(ProfileType.REGULAR).getAggregate().length());
//			collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.INTERSECTION_POINT, headIndex);
//			finer("Added IP at index "+headIndex);
			return ipIndex;
		} catch (Exception e) {
			error("Error getting median profile", e);
			return -1;
		}
		
	}
	
	
	/**
	 * Find the top vertical in the profile of a round nucleus. 
	 * This is the same as the IP.
	 * @return
	 */
	private int identifyTopVerticalIndexInRoundNucleus(){
		return identifyIPIndexInRoundNucleus();
	}
	
	/**
	 * Find the top vertical in the profile of a round nucleus. 
	 * This is the same as the OP.
	 * @return
	 */
	private int identifyBottomVerticalIndexInRoundNucleus(){
		return identifyOPIndexInRoundNucleus();
	}
	

	/*
	 * PIG SPERM
	 * 
	 */
	
	
	/**
	 * Find the RP in the profile of a pig sperm nucleus. 
	 * This is the index with the longest diameter
	 * @return The RP index or -1 on error
	 */
	private int identifyRPIndexInPigSpermNucleus(){
		
				
		// get the profile
		// This is starting from an arbitrary point?
		// Starting from the head in test data, so the reference point is correct
		Profile medianProfile;
		try {
			medianProfile = collection.getProfileCollection(ProfileType.REGULAR)
					.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
		} catch (Exception e) {
			error("Error getting median profile for pig sperm", e);
			return -1;
		}


		// find local maxima in the median profile over 180
		BooleanProfile maxima = medianProfile.smooth(2).getLocalMaxima(5, 180); // window size 5, only values over 180


		double minAngle = 180;
		int rpIndex     = 0; // the RP is, by default, 0 - this is how the nuclei have been ordered


		if(maxima.size()==0){
			warn("Error: no maxima found in median line");

			rpIndex = medianProfile.size() / 2; // set to roughly the middle of the array for the moment

		} else{  // Maxima were found

			for(int i = 0; i<maxima.size();i++){

				if(maxima.get(i)==true){ // look at local maxima
					int index = i;

					double angle = medianProfile.get(index); // get the angle at this maximum

					// look for the highest local maximum
					if(angle>minAngle){
						minAngle = angle;
						rpIndex = index;
					}
				}
			}
		}

		return rpIndex;



	}
	
	/**
	 * Find the OP in the profile of a pig sperm nucleus. 
	 * This is the same as the RP.
	 * @return
	 */
	private int identifyOPIndexInPigSpermNucleus(){
		
		// ensure we have teh correct offset 
		int opIndex = collection.getProfileCollection(ProfileType.REGULAR).getOffset(BorderTag.REFERENCE_POINT);
		return opIndex;
	}
	
	/**
	 * Find the IP in the profile of a pig sperm nucleus. 
	 * This is the index half way around the profile from the OP
	 * @return The RP index or -1 on error
	 */
	private int identifyIPIndexInPigSpermNucleus(){

		int opIndex = identifyOPIndexInPigSpermNucleus();
		// set the intersection point half way around from the tail
		// find the offset to use
		double length = (double) collection.getProfileCollection(ProfileType.REGULAR).getAggregate().length();		
		int offset    =  (int) Math.ceil(length / 2d); // ceil to ensure offsets are correct
		
		
//		 adjust the intersection point index using the offset
		int ipIndex  = AbstractCellularComponent.wrapIndex( opIndex - offset, collection.getProfileCollection(ProfileType.REGULAR).getAggregate().length());
		return ipIndex;
		
	}
	
	
	/**
	 * Find the top vertical in the profile of a pig sperm nucleus. 
	 * This is the same as the IP.
	 * @return
	 */
	private int identifyTopVerticalIndexInPigSpermNucleus(){
		return identifyIPIndexInPigSpermNucleus();
	}
	
	/**
	 * Find the top vertical in the profile of a pig sperm nucleus. 
	 * This is the same as the OP.
	 * @return
	 */
	private int identifyBottomVerticalIndexInPigSpermNucleus(){
		return identifyOPIndexInPigSpermNucleus();
	}
	
	
	/*
	 * MOUSE SPERM
	 * 
	 */
	/**
	 * Find the RP in the profile of a mouse sperm nucleus. 
	 * This is the index with the lowest angle
	 * @return The RP index or -1 on error
	 */
	private int identifyRPIndexInMouseSpermNucleus(){
		
		Profile profile;
		try {
			profile = collection.getProfileCollection(ProfileType.REGULAR)
					.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
			
			return profile.getIndexOfMin();
		} catch (Exception e) {
			error("Error getting median profile for collection", e);
			return -1;
		}	

	}
	
	/**
	 * Find the OP in the profile of a mouse sperm nucleus. 
	 * This is the same as the RP.
	 * @return
	 */
	private int identifyOPIndexInMouseSpermNucleus(){
		
		// can't use regular tail detector, because it's based on NucleusBorderPoints
				// get minima in curve, then find the lowest minima / minima furthest from both ends

		Profile medianProfile;
		try {
			medianProfile = collection.getProfileCollection(ProfileType.REGULAR)
					.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
		} catch (Exception e) {
			error("Error getting median profile for collection", e);
			return -1;
		}

		BooleanProfile minima = medianProfile.smooth(2).getLocalMinima(5); // window size 5

		double minAngle = 180;
		int opIndex = medianProfile.size() >> 1; // set the default tail at the half way point from the tip. 

				int tipExclusionIndex1 = (int) (medianProfile.size() * 0.2);
				int tipExclusionIndex2 = (int) (medianProfile.size() * 0.6);

				for(int i = 0; i<minima.size();i++){
					if( minima.get(i)==true){
						int index = i;

						double angle = medianProfile.get(index);
						if(angle<minAngle && index > tipExclusionIndex1 && index < tipExclusionIndex2){ // get the lowest point that is not near the tip
							minAngle = angle;
							opIndex = index;
						}
					}
				}

		return opIndex;
	}
	
	/**
	 * Find the IP in the profile of a mouse sperm nucleus. 
	 * Not implemented, returns -1
	 * @return -1 
	 */
	private int identifyIPIndexInMouseSpermNucleus(){

		return -1;
		
	}
	
	/**
	 * Find the top vertical in the profile of a mouse sperm nucleus. 
	 * This is the same as the IP.
	 * @return
	 */
	private int identifyTopVerticalIndexInMouseSpermNucleus(){
		
		Profile medianProfile;
		try {
			medianProfile = collection.getProfileCollection(ProfileType.REGULAR).getProfile(BorderTag.REFERENCE_POINT, 50);
		} catch (Exception e) {
			error("Error getting median profile for collection", e);
			return -1;
		}

		/*
	     * Call to a StraightPointFinder that will find the straight part of the nucleus
	     * Use this to set the BorderTag.TopVertical and BottomVertical
	     */
	    int[] verticalPoints = medianProfile.getConsistentRegionBounds(180, 2, 10);
	    if(verticalPoints[0]!=-1 && verticalPoints[1]!=-1){
	    	
	    	return verticalPoints[0];

	    } else {
	    	warn("Dataset "+collection.getName()+": Unable to assign vertical positions in median profile");
	    	return -1;
	    }
	}
	
	/**
	 * Find the top vertical in the profile of a mouse sperm nucleus. 
	 * This is the same as the OP.
	 * @return
	 */
	private int identifyBottomVerticalIndexInMouseSpermNucleus(){
		Profile medianProfile;
		try {
			medianProfile = collection.getProfileCollection(ProfileType.REGULAR).getProfile(BorderTag.REFERENCE_POINT, 50);
		} catch (Exception e) {
			error("Error getting median profile for collection", e);
			return -1;
		}

		/*
	     * Call to a StraightPointFinder that will find the straight part of the nucleus
	     * Use this to set the BorderTag.TopVertical and BottomVertical
	     */
	    int[] verticalPoints = medianProfile.getConsistentRegionBounds(180, 2, 10);
	    if(verticalPoints[0]!=-1 && verticalPoints[1]!=-1){
	    	
	    	return verticalPoints[1];

	    } else {
	    	warn("Dataset "+collection.getName()+": Unable to assign vertical positions in median profile");
	    	return -1;
	    }
	}
	
	
	
}
