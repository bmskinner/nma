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

package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;

/**
 * Object implementing this interface are able to be
 * profiled for morphological analysis. This extends the 
 * CellularComponent interface; thus anything implementing
 * Profileable will have a border list and associated methods
 * @author bms41
 *
 */
public interface Profileable extends CellularComponent {

	static final int    DEFAULT_PROFILE_WINDOW = 15;
	static final double DEFAULT_PROFILE_WINDOW_PROPORTION = 0.05;
		
	
	/**
	 * Calculate profiles based on the desired window proportion
	 * @param proportion
	 * @throws ComponentCreationException 
	 */
	void initialise(double proportion) throws ComponentCreationException;
	
	/*
	* Finds the key points of interest around the border
	* of the Nucleus. Can use several different methods, and 
	* take a best-fit, or just use one. The default in a round 
	* nucleus is to get the longest diameter and set this as
	*  the head/tail axis.
	*/
	public void findPointsAroundBorder() throws ComponentCreationException;
	
	/**
	 * Check if the object has a profile of the given type
	 * @param type
	 * @return
	 */
	boolean hasProfile(ProfileType type);
	
	/**
	 * Get a copy of the angle profile. The first index of the profile
	 * is the first border point in the border list. That is, there is no
	 * consistency to the order of values across multiple nuclei. If consistency
	 * is needed, specify a pointType
	 * @return
	 * @throws  
	 */
	ISegmentedProfile getProfile(ProfileType type) throws UnavailableProfileTypeException;
	
	/**
	 * Update the profile of the given type. Since only franken profiles are 
	 * not calculated internally, the other profiles just replace the segment list.
	 * This will replace the segment list on all other profile types, to keep
	 * the segmentation pattern consistent.
	 * @param type
	 * @param profile
	 * @throws Exception
	 */
	void setProfile(ProfileType type, ISegmentedProfile profile);
	
	
	/**
	 * Get the window size for generating the specificed profile
	 * @param type
	 * @return
	 */
	int getWindowSize(ProfileType type);
	
	
	/**
	 * Get the fraction of the perimeter to use for calculating the window size
	 * in pixels
	 * @return a fraction between 0 and 1
	 * @throws UnavailableProfileTypeException 
	 */
	double getWindowProportion(ProfileType type);
	
	/**
	 * Set the fraction of the perimeter to use for calculating the window size
	 * in pixels. 
	 * @param d Proportion from 0 to 1, or Profileable.DEFAULT_PROFILE_WINDOW_PROPORTION if not previously set
	 */
	public void setWindowProportion(ProfileType type, double d);
			
	/**
	 * Check if the segments and tags are able to be
	 * modified
	 * @return
	 */
	boolean isLocked();
	
	/**
	 * Set if the segments and tags are able to be
	 * modified
	 * @param b
	 */
	void setLocked(boolean b);
	
	/**
	 * Set the lock on this segment of all the profile types
	 * @param lock
	 * @param segID
	 */
	void setSegmentStartLock(boolean lock, UUID segID);

	/**
	 * Reverse the angle profile of the object. Also reverses the distance
	 * profile, the border list and updates the border tags to the new positions
	 */
	void reverse();
	
	/**
	 * Calculate new profiles for the object. Angle profiles are calculated
	 * with the internal window size - change this with setWindowSize(ProfileType.ANGLE). 
	 * It will replace the existing profiles.
	 * @throws Exception 
	 */
	void calculateProfiles() throws ProfileException;
	
	/**
	 * Calculate the distance from point to point around the 
	 * periphery of the nucleus.
	 * @return
	 */
//	double getPathLength(ProfileType type) throws UnavailableProfileTypeException;
	
	/**
	 * Go around the border of the object, measuring the angle to the OP. 
	 * If the angle is closest to target angle, return the distance to the CoM.
	 * @param angle the target angle
	 * @return the distance from the closest border point at the requested angle to the CoM
	 */
	double getDistanceFromCoMToBorderAtAngle(double angle);
		
}
