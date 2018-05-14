/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
 * Performs angle and distance profiling on Profileable objects
 * 
 * @author bms41
 * @since 1.13.2
 *
 */
public class ProfileCreator implements Loggable {

    Taggable target;

    public ProfileCreator(Taggable target) {
        this.target = target;
    }

    /**
     * Create a profile for the desired profile type on the template object
     * 
     * @param type the profile type
     * @return a segmented profile of the requested type.
     * @throws ProfileException
     */
    public ISegmentedProfile createProfile(ProfileType type) throws ProfileException {
        try {
            switch (type) {
	            case ANGLE:        return calculateAngleProfile();
	            case DIAMETER:     return calculateDiameterProfile();
	            case RADIUS:       return calculateRadiusProfile();
	            case ZAHN_ROSKIES: return calculateZahnRoskiesProfile();
	            default:           return calculateAngleProfile();
            }
        } catch (UnavailableBorderPointException | UnavailableBorderTagException e) {
            stack("Cannot create profile", e);
            throw new ProfileException("Cannot make profile " + type, e);
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

        ISegmentedProfile templateProfile = null;
        try {
            if (target.hasProfile(ProfileType.ANGLE)) {
            	templateProfile = target.getProfile(ProfileType.ANGLE);
                if (templateProfile.hasSegments())
                    segments = templateProfile.getSegments();
            }
        } catch (UnavailableProfileTypeException e) {
        	fine("No profile angle type");
            stack("Profile type angle not found", e);
        }

        return segments;
    }

    private ISegmentedProfile calculateAngleProfile() throws UnavailableBorderPointException {

        List<IBorderSegment> segments = getExistingSegments();

        float[] angles = new float[target.getBorderLength()];

        Shape s = target.toShape();

        int index = 0;
        List<IBorderPoint> borderList = target.getBorderList();

        if (borderList == null)
            throw new UnavailableBorderPointException("Null border list in target");

        int pointOffset = target.getWindowSize(ProfileType.ANGLE);

        if (pointOffset == 0)
            throw new UnavailableBorderPointException("Window size has not been set in Profilable object");

        Iterator<IBorderPoint> it = borderList.iterator();

        while (it.hasNext()) {

            IBorderPoint point = it.next();

            IBorderPoint pointBefore = point.prevPoint(pointOffset);

            IBorderPoint pointAfter = point.nextPoint(pointOffset);

            // Get the smallest angle between the points
            float angle = (float) point.findAngle(pointBefore, pointAfter);

            // Now discover if this measured angle is inside or outside the
            // object

            // find the halfway point between the first and last points.
            // is this within the roi?
            // if yes, keep min angle as interior angle
            // if no, 360-min is interior
            float midX = (float) ((pointBefore.getX() + pointAfter.getX()) / 2);
            float midY = (float) ((pointBefore.getY() + pointAfter.getY()) / 2);

            // Check if the polygon contains the point
            if (s.contains(midX, midY)) {
                angles[index] = angle;
            } else {
                angles[index] = 360 - angle;
            }
            index++;
        }

        // Make a new profile. This will have two segments by default
        ISegmentedProfile newProfile = new SegmentedFloatProfile(angles);

        // Reapply any segments that were present in the original profile
        if (!segments.isEmpty()) 
            reapplySegments(segments, newProfile);
        return newProfile;
    }

    private void reapplySegments(List<IBorderSegment> segments, ISegmentedProfile profile) {
        // If the border list has changed, the profile lengths will be different
        // In this case, add and normalise the segment lengths
        if (segments.get(0).getTotalLength() != target.getBorderLength()) {

            try {
                segments = IBorderSegment.scaleSegments(segments, target.getBorderLength());
            } catch (ProfileException e) {
                warn("Error scaling segments");
                stack("Error scaling segments when profiling", e);
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
    private ISegmentedProfile calculateZahnRoskiesProfile()
            throws UnavailableBorderPointException, UnavailableBorderTagException {

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

        return new SegmentedFloatProfile(profile);
    }

    private ISegmentedProfile calculateDiameterProfile() throws UnavailableBorderPointException {

        float[] profile = new float[target.getBorderLength()];

        int index = 0;
        Iterator<IBorderPoint> it = target.getBorderList().iterator();
        while (it.hasNext()) {

            IBorderPoint point = it.next();
            IBorderPoint opp = target.findOppositeBorder(point);

            profile[index++] = (float) point.getLengthTo(opp);

        }

        return new SegmentedFloatProfile(profile);
    }

    private ISegmentedProfile calculateRadiusProfile() throws UnavailableBorderPointException {

        float[] profile = new float[target.getBorderLength()];

        int index = 0;
        Iterator<IBorderPoint> it = target.getBorderList().iterator();
        while (it.hasNext()) {

            IBorderPoint point = it.next();
            profile[index++] = (float) point.getLengthTo(target.getCentreOfMass());

        }

        return new SegmentedFloatProfile(profile);
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

        // finer("Iterating border");
        while (it.hasNext()) {
            // finest("Getting points");

            IBorderPoint point = it.next();

            IBorderPoint pointBefore = point.prevPoint(pointOffset);

            // IBorderPoint pointAfter = point.nextPoint(pointOffset);

            double distance = point.getLengthTo(pointBefore);

            profile[index] = (float) distance;

            index++;
        }
        // finer("Making new profile");
        // Make a new profile. This will have two segments by default
        ISegmentedProfile newProfile = new SegmentedFloatProfile(profile);

        return newProfile;

    }

}
