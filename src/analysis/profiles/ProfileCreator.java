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

import java.util.Iterator;
import java.util.List;

import utility.Utils;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.nuclear.BorderPoint;
import components.nuclear.NucleusBorderSegment;

/**
 * Performs angle and distance profiling on Profileable
 * objects
 * @author bms41
 *
 */
public class ProfileCreator {
	
	Profileable target;
	
	public ProfileCreator(Profileable target){
		this.target = target;
	}
	
	public SegmentedProfile createProfile(ProfileType type){
		
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
	
	private SegmentedProfile calculateAngleProfile() {

		List<NucleusBorderSegment> segments = null;
				
		// store segments to reapply later
		if(target.hasProfile(ProfileType.ANGLE)){
			if(target.getProfile(ProfileType.ANGLE).hasSegments()){
				segments = target.getProfile(ProfileType.ANGLE).getSegments();
			}
		}
		
		double[] angles = new double[target.getBorderLength()];

//		for(int i=0; i<this.getBorderLength();i++){
		
		int index = 0;
		Iterator<BorderPoint> it = target.getBorderList().iterator();
		while(it.hasNext()){

			BorderPoint point = it.next();
			BorderPoint pointBefore = point.prevPoint(target.getWindowSize(ProfileType.ANGLE));
			BorderPoint pointAfter  = point.nextPoint(target.getWindowSize(ProfileType.ANGLE));

			double angle = Utils.findAngleBetweenXYPoints(pointBefore, point, pointAfter);

			// find the halfway point between the first and last points.
				// is this within the roi?
				// if yes, keep min angle as interior angle
				// if no, 360-min is interior
			double midX = (pointBefore.getX()+pointAfter.getX())/2;
			double midY = (pointBefore.getY()+pointAfter.getY())/2;
			
			// create a polygon from the border list - we are not storing the polygon directly
//			FloatPolygon polygon = this.createPolygon();
			if(target.createPolygon().contains((float) midX, (float) midY)){
			
//			if(polygon.contains( (float) midX, (float) midY)){
				angles[index] = angle;
			} else {
				angles[index] = 360-angle;
			}
			index++;
		}
		SegmentedProfile newProfile = new SegmentedProfile(angles);
		if(segments!=null){
			newProfile.setSegments(segments);
		}
		return newProfile;
	}

	private SegmentedProfile calculateDiameterProfile() {

		double[] profile = new double[target.getBorderLength()];
			
		int index = 0;
		Iterator<BorderPoint> it = target.getBorderList().iterator();
		while(it.hasNext()){

			BorderPoint point = it.next();
			BorderPoint opp = target.findOppositeBorder(point);

			profile[index++] = point.getLengthTo(opp); 
			
		}

		return new SegmentedProfile(profile);
	}
	
	private SegmentedProfile calculateRadiusProfile() {

		double[] profile = new double[target.getBorderLength()];
		
		int index = 0;
		Iterator<BorderPoint> it = target.getBorderList().iterator();
		while(it.hasNext()){

			BorderPoint point = it.next();
			profile[index++] = point.getLengthTo(target.getCentreOfMass()); 
			
		}

		return new SegmentedProfile(profile);
	}
	
}
