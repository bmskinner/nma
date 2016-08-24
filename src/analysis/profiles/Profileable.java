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

package analysis.profiles;

import ij.process.FloatPolygon;

import java.util.List;
import java.util.UUID;

import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.generic.XYPoint;
import components.nuclear.BorderPoint;

/**
 * Object implementing this interface are able to be
 * profiled for morphological analysis
 * @author bms41
 *
 */
public interface Profileable {

	static final int DEFAULT_PROFILE_WINDOW = 15;
	static final double DEFAULT_PROFILE_WINDOW_PROPORTION = 0.05;
	
	/**
	 * Get a copy of the component border points in the border list
	 * @return
	 */
	List<BorderPoint> getBorderList();
	
	
	/**
	 * Get the number of points in the border list
	 * @return
	 */
	int getBorderLength();
	
	
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
	 */
	SegmentedProfile getProfile(ProfileType type);
	
	
	/**
	 * Get the window size for generating the specificed profile
	 * @param type
	 * @return
	 */
	int getWindowSize(ProfileType type);
	
	
	/**
	 * Get the fraction of the perimeter to use for calculating the window size
	 * in pixels
	 * @return
	 */
	double getWindowProportion(ProfileType type);
	
	/**
	 * Set the fraction of the perimeter to use for calculating the window size
	 * in pixels. 
	 * @param d Proportion from 0 to 1
	 */
	public void setWindowProportion(ProfileType type, double d);
	
	/**
	 * Create a float polygon from the border points of the object
	 * @return
	 */
	FloatPolygon createPolygon();
	
	/**
	 * Find the point directly opposite the given point
	 * through the centre of mass of the object
	 * @return
	 */
	BorderPoint findOppositeBorder(BorderPoint p);
	
	/**
	 * Get the centre of mass of the object
	 * @return
	 */
	XYPoint getCentreOfMass();
	
	
	/**
	 * Update the profile of the given type. Since only franken profiles are 
	 * not calculated internally, the other profiles just replace the segment list.
	 * This will replace the segment list on all other profile types, to keep
	 * the segmentation pattern consistent.
	 * @param type
	 * @param profile
	 * @throws Exception
	 */
	void setProfile(ProfileType type, SegmentedProfile profile) throws Exception;
	
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
	 * Reverse the angle profile of the nucleus. Also reverses the distance
	 * profile, the border list and updates the border tags to the new positions
	 * @throws Exception
	 */
	void reverse();
	
	/**
	 * Calculate a new angle profile with the given window size. It
	 * will replace the existing angle profile. It takes the point, 
	 * a point <windowSize> behind and <windowSize> ahead, and calculates
	 * the interior angle between them.
	 * @param angleProfileWindowSize the window size
	 * @throws Exception 
	 */
	void calculateProfiles() throws Exception;
	
}
