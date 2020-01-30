/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.SegmentedCellularComponent;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.generic.DoubleEquation;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.LineEquation;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.utility.AngleTools;

/**
 * Performs angle and distance profiling on Taggable objects
 * 
 * @author bms41
 * @since 1.13.2
 *
 */
public class ProfileCreator {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private Taggable target;

    public ProfileCreator(@NonNull Taggable target) {
        this.target = target;
    }

    /**
     * Create a profile for the desired profile type on the template object
     * 
     * @param type the profile type
     * @return a segmented profile of the requested type.
     * @throws ProfileException
     */
    public ISegmentedProfile createProfile(@NonNull ProfileType type) throws ProfileException {
        try {
            switch (type) {
	            case ANGLE:        return calculateAngleProfile();
	            case DIAMETER:     return calculateDiameterProfile();
	            case RADIUS:       return calculateRadiusProfile();
	            case ZAHN_ROSKIES: return calculateZahnRoskiesProfile();
	            case FRANKEN:      LOGGER.finest( "Frankenprofile");
	            default:           return calculateAngleProfile();
            }
        } catch (UnavailableBorderPointException e) {
            throw new ProfileException("Cannot create profile " + type, e);
        } catch(Exception e) {
        	throw new ProfileException("Unexpected exception creating profile " + type+" due to "+e.getMessage(), e);
        }
    }

    /**
     * Get the existing segments from the template angle profile. 
     * Returns an empty list if the profile is not present
     * 
     * @return
     */
    private List<IBorderSegment> getExistingSegments() {
        List<IBorderSegment> segments = new ArrayList<>();
        LOGGER.finest( "Getting existing segments from angle profile");
        if(!target.hasProfile(ProfileType.ANGLE))
        	return segments;

        try {

        	ISegmentedProfile templateProfile = target.getProfile(ProfileType.ANGLE);
        	LOGGER.finest( "Fetched angle profile");
        	if (templateProfile.hasSegments()) {
        		LOGGER.finest( "Angle profile has "+templateProfile.getSegmentCount()+" segments");
        		segments = templateProfile.getSegments();
        	}

        } catch (UnavailableProfileTypeException e) {
        	LOGGER.log(Loggable.STACK, "No profile angle type: "+e.getMessage(), e);
        }

        return segments;
    }

    private ISegmentedProfile calculateAngleProfile() throws UnavailableBorderPointException {

        List<IBorderSegment> segments = getExistingSegments();

        float[] angles = new float[target.getBorderLength()];

        Shape s = target.toShape();

        List<IBorderPoint> borderList = target.getBorderList();

        if (borderList == null)
            throw new UnavailableBorderPointException("Null border list in target");

        int pointOffset = target.getWindowSize(ProfileType.ANGLE);

        if (pointOffset == 0)
            throw new UnavailableBorderPointException("Window size has not been set in Profilable object");

        for(int index=0; index<borderList.size(); index++) {

        	IBorderPoint point       = borderList.get(index);
            IBorderPoint pointBefore = point.prevPoint(pointOffset);
            IBorderPoint pointAfter  = point.nextPoint(pointOffset);

            // Find the smallest angle between the points
            float angle = (float) point.findSmallestAngle(pointBefore, pointAfter);
            
            // Is the measured angle is inside or outside the object?
            // Take the midpoint between the before and after points.
            // If it is within the ROI, the angle is the interior angle
            // if no, 360-min is the interior angle
            float midX = (float) ((pointBefore.getX() + pointAfter.getX()) / 2);
            float midY = (float) ((pointBefore.getY() + pointAfter.getY()) / 2);
            angles[index] = s.contains(midX, midY) ? angle : 360 - angle;
        }

        // Make a new profile. If possible, use the internal segmentation type of the component
        ISegmentedProfile newProfile;
        if(target instanceof SegmentedCellularComponent) {
        	newProfile = ((SegmentedCellularComponent)target).new DefaultSegmentedProfile(angles);
        } else {
        	newProfile = new SegmentedFloatProfile(angles);
        }

        // Reapply any segments that were present in the original profile
        if (!segments.isEmpty()) 
            reapplySegments(segments, newProfile);
        return newProfile;
    }

    private void reapplySegments(List<IBorderSegment> segments, ISegmentedProfile profile) {

        // If the border list has changed, the profile lengths will be different
        // In this case, add and normalise the segment lengths
        if (segments.get(0).getProfileLength() != target.getBorderLength()) {
            try {
                segments = IBorderSegment.scaleSegments(segments, target.getBorderLength());
            } catch (ProfileException e) {
                LOGGER.warning("Error scaling segments");
                LOGGER.log(Loggable.STACK, "Error scaling segments when profiling", e);
            }

        }

        profile.setSegments(segments);
    }

    /**
     * Calculate a modified ZR profile. This uses the same window size as the
     * angle profile, so is not a true ZR transform.
     * 
     * @return
     * @throws UnavailableBorderPointException
     * @throws UnavailableBorderTagException
     */
    private ISegmentedProfile calculateZahnRoskiesProfile() {

        float[] profile = new float[target.getBorderLength()];
        int index = 0;

        Iterator<IBorderPoint> it = target.getBorderList().iterator();
        while (it.hasNext()) {

            IBorderPoint point = it.next();

            IBorderPoint prev = point.prevPoint(1);
            IBorderPoint next = point.nextPoint(1);

            // Get the equation between the first two points
            LineEquation eq = new DoubleEquation(prev, point);

            // Move out along line
            IPoint p = eq.getPointOnLine(point, point.getLengthTo(prev)); 

            // Don't go the wrong way along the line
            if (p.getLengthTo(prev) < point.getLengthTo(prev))
                p = eq.getPointOnLine(point, -point.getLengthTo(prev));

            // Get the angle between the points
            double rad = AngleTools.angleBetweenLines(point, p, point, next);

            double angle = Math.toDegrees(rad);

            if (angle > 180)
                angle = -180 + (angle - 180);

            if (angle < -180)
                angle = 180 + (angle + 180);

            profile[index++] = (float) angle;

        }

        // invert if needed

        if (profile[0] < 0) {
            for (int i = 0; i < profile.length; i++) {
                profile[i] = 0 - profile[i];
            }
        }
        
        // Make a new profile. If possible, use the internal segmentation type of the component
        ISegmentedProfile newProfile;
        if(target instanceof SegmentedCellularComponent) {
        	newProfile = ((SegmentedCellularComponent)target).new DefaultSegmentedProfile(profile);
        } else {
        	newProfile = new SegmentedFloatProfile(profile);
        }
        return newProfile;
    }

    private ISegmentedProfile calculateDiameterProfile() throws UnavailableBorderPointException {

        float[] profile = new float[target.getBorderLength()];
        
        try {
        	List<IBorderPoint> points = target.getBorderList();
        	for(int index=0; index<points.size(); index++) {
        		try {
        			IBorderPoint point = points.get(index);
        			IBorderPoint opp   = target.findOppositeBorder(point);
        			profile[index] = (float) point.getLengthTo(opp);
        		} catch(Exception e) {
        			LOGGER.log(Loggable.STACK, "Error finding opposite border in index "+index, e);
        			profile[index] = 0;
        		}
        	}
        } catch(Exception e) {
        	LOGGER.log(Loggable.STACK, "Error creating diameter profile", e);
        	LOGGER.warning("profile length "+profile.length);
        }

     // Make a new profile. If possible, use the internal segmentation type of the component
        ISegmentedProfile newProfile;
        if(target instanceof SegmentedCellularComponent) {
        	newProfile = ((SegmentedCellularComponent)target).new DefaultSegmentedProfile(profile);
        } else {
        	newProfile = new SegmentedFloatProfile(profile);
        }
        return newProfile;
    }

    private ISegmentedProfile calculateRadiusProfile() {

        float[] profile = new float[target.getBorderLength()];

        int index = 0;
        Iterator<IBorderPoint> it = target.getBorderList().iterator();
        while (it.hasNext()) {

            IBorderPoint point = it.next();
            profile[index++] = (float) point.getLengthTo(target.getCentreOfMass());

        }

     // Make a new profile. If possible, use the internal segmentation type of the component
        ISegmentedProfile newProfile;
        if(target instanceof SegmentedCellularComponent) {
        	newProfile = ((SegmentedCellularComponent)target).new DefaultSegmentedProfile(profile);
        } else {
        	newProfile = new SegmentedFloatProfile(profile);
        }
        return newProfile;
    }

    /**
     * Calculate the distance between points separated by the windowsize
     * 
     * @return
     * @throws UnavailableBorderPointException
     */
    public ISegmentedProfile calculatePointToPointDistanceProfile() throws UnavailableBorderPointException {
        float[] profile = new float[target.getBorderLength()];

        int index = 0;
        Iterator<IBorderPoint> it = target.getBorderList().iterator();

        int pointOffset = target.getWindowSize(ProfileType.ANGLE);

        if (pointOffset == 0) {
            throw new UnavailableBorderPointException("Window size has not been set in Profilable object");
        }

        while (it.hasNext()) {

            IBorderPoint point = it.next();

            IBorderPoint pointBefore = point.prevPoint(pointOffset);
            double distance = point.getLengthTo(pointBefore);

            profile[index] = (float) distance;
            index++;
        }

        // Make a new profile. If possible, use the internal segmentation type of the component
        ISegmentedProfile newProfile;
        if(target instanceof SegmentedCellularComponent) {
        	newProfile = ((SegmentedCellularComponent)target).new DefaultSegmentedProfile(profile);
        } else {
        	newProfile = new SegmentedFloatProfile(profile);
        }
        return newProfile;
    }

}
