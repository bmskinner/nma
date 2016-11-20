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

import java.awt.Shape;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import logging.Loggable;
import components.active.generic.SegmentedFloatProfile;
import components.active.generic.UnavailableProfileTypeException;
import components.generic.ISegmentedProfile;
import components.generic.ProfileType;
import components.nuclear.IBorderPoint;
import components.nuclear.IBorderSegment;

/**
 * Performs angle and distance profiling on Profileable
 * objects
 * @author bms41
 * @since 1.13.2
 *
 */
public class ProfileCreator implements Loggable {
	
	Profileable target;
	
	public ProfileCreator(Profileable target){
		this.target = target;
	}
	
	/**
	 * Create a profile for the desired profile type on the template object
	 * @param type the profile type
	 * @return a segmented profile of the requested type.
	 */
	public ISegmentedProfile createProfile(ProfileType type){
		
		
		switch(type){
			case ANGLE: { 
				return calculateAngleProfile();
			}
			
			case DIAMETER:{
				return calculateDiameterProfile();
			}
			
			case RADIUS:{
				return calculateRadiusProfile();
			}
			
			
			default:{
				return calculateAngleProfile(); // Franken profiles will be angle until modified
			}
		}
	}
	
	/**
	 * Get the existing segments from the template angle profile
	 * @return
	 */
	private List<IBorderSegment> getExistingSegments(){
		List<IBorderSegment> segments = new ArrayList<IBorderSegment>();
		
		ISegmentedProfile templateProfile = null;
		// store segments to reapply later

		try {
			if(target.hasProfile(ProfileType.ANGLE)){

				if(target.getProfile(ProfileType.ANGLE).hasSegments()){
					templateProfile = target.getProfile(ProfileType.ANGLE);
					segments = templateProfile.getSegments();

				}
			}
		} catch (UnavailableProfileTypeException e) {
			stack("Profile type angle not found", e);
		}
		
		return segments;
	}
	
	private ISegmentedProfile calculateAngleProfile() {

		List<IBorderSegment> segments = getExistingSegments();
		
		float[] angles = new float[target.getBorderLength()];
		
//		FloatPolygon polygon = target.createPolygon();
		Shape s = target.toShape();

		
		int index = 0;
		Iterator<IBorderPoint> it = target.getBorderList().iterator();
		while(it.hasNext()){

			IBorderPoint point = it.next();
			IBorderPoint pointBefore = point.prevPoint(target.getWindowSize(ProfileType.ANGLE));
			IBorderPoint pointAfter  = point.nextPoint(target.getWindowSize(ProfileType.ANGLE));

			// Get the smallest angle between the points
			float angle = (float) point.findAngle(pointBefore, pointAfter);

			// Now discover if this measured angle is inside or outside the object
			
			// find the halfway point between the first and last points.
				// is this within the roi?
				// if yes, keep min angle as interior angle
				// if no, 360-min is interior
			float midX = (float) ((pointBefore.getX()+pointAfter.getX())/2);
			float midY = (float) ((pointBefore.getY()+pointAfter.getY())/2);
			
			// Check if the polygon contains the point
			if(s.contains( midX,  midY)){
			
				angles[index] = angle;
			} else {
				angles[index] = 360-angle;
			}
			index++;
		}
		
		// Make a new profile. This will have two segments by default
		ISegmentedProfile newProfile = new SegmentedFloatProfile(angles);
		
		// Reapply any segments that were present in the original profile
		if( ! segments.isEmpty()){
			
			reapplySegments(segments, newProfile);
		}

		return newProfile;
	}
	
	private void reapplySegments(List<IBorderSegment> segments, ISegmentedProfile profile){
		// If the border list has changed, the profile lengths will be different
		// In this case, add and normalise the segment lengths
		if(segments.get(0).getTotalLength() != target.getBorderLength() ){

			try {
				segments = IBorderSegment.scaleSegments(segments, target.getBorderLength());
			} catch (ProfileException e) {
				warn("Error scaling segments");
				stack("Error scaling segments when profiling", e);
			}


		}

		profile.setSegments(segments);
	}

	private ISegmentedProfile calculateDiameterProfile() {

		float[] profile = new float[target.getBorderLength()];
			
		int index = 0;
		Iterator<IBorderPoint> it = target.getBorderList().iterator();
		while(it.hasNext()){

			IBorderPoint point = it.next();
			IBorderPoint opp = target.findOppositeBorder(point);

			profile[index++] = (float) point.getLengthTo(opp); 
			
		}

		return new SegmentedFloatProfile(profile);
	}
	
	private ISegmentedProfile calculateRadiusProfile() {

		float[] profile = new float[target.getBorderLength()];
		
		int index = 0;
		Iterator<IBorderPoint> it = target.getBorderList().iterator();
		while(it.hasNext()){

			IBorderPoint point = it.next();
			profile[index++] = (float) point.getLengthTo(target.getCentreOfMass()); 
			
		}

		return new SegmentedFloatProfile(profile);
	}
	
}
