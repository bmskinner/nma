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
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
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
     * @param type
     *            the profile type
     * @return a segmented profile of the requested type.
     * @throws ProfileException
     */
    public ISegmentedProfile createProfile(ProfileType type) throws ProfileException {

        try {

            switch (type) {
            case ANGLE: {
                return calculateAngleProfile();
            }

            case DIAMETER: {
                return calculateDiameterProfile();
            }

            case RADIUS: {
                return calculateRadiusProfile();
            }

            case ZAHN_ROSKIES: {
                return calculateZahnRoskieProfile();
            }

            // case P2P:{
            // return calculatePointToPointDistanceProfile();
            // }

            default: {
                return calculateAngleProfile(); // Franken profiles will be
                                                // angle until modified
            }
            }
        } catch (UnavailableBorderPointException | UnavailableBorderTagException e) {
            warn("Cannot make profile " + type);
            stack("Cannot create profile", e);
            throw new ProfileException("Error getting border point", e);
        }
    }

    /**
     * Get the existing segments from the template angle profile
     * 
     * @return
     */
    private List<IBorderSegment> getExistingSegments() {
        List<IBorderSegment> segments = new ArrayList<IBorderSegment>();

        ISegmentedProfile templateProfile = null;
        // store segments to reapply later

        try {
            if (target.hasProfile(ProfileType.ANGLE)) {

                if (target.getProfile(ProfileType.ANGLE).hasSegments()) {
                    templateProfile = target.getProfile(ProfileType.ANGLE);
                    segments = templateProfile.getSegments();

                }
            }
        } catch (UnavailableProfileTypeException e) {
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

        if (borderList == null) {
            throw new UnavailableBorderPointException("Null border list in target");
        }

        int pointOffset = target.getWindowSize(ProfileType.ANGLE);

        if (pointOffset == 0) {
            throw new UnavailableBorderPointException("Window size has not been set in Profilable object");
        }
        // finest("Point offset: "+pointOffset );
        Iterator<IBorderPoint> it = borderList.iterator();
        // finer("Iterating border");
        while (it.hasNext()) {
            // finest("Getting points");

            IBorderPoint point = it.next();

            IBorderPoint pointBefore = point.prevPoint(pointOffset);

            IBorderPoint pointAfter = point.nextPoint(pointOffset);

            // double rad = AngleTools.angleBetweenLines(pointBefore, point,
            // point, pointAfter);
            // float angle = (float) Math.toDegrees(rad);
            // angles[index] = angle;

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
            // finest("Checking position in shape");
            if (s.contains(midX, midY)) {

                angles[index] = angle;
            } else {
                angles[index] = 360 - angle;
            }
            index++;
        }
        // finer("Making new profile");
        // Make a new profile. This will have two segments by default
        ISegmentedProfile newProfile = new SegmentedFloatProfile(angles);

        // Reapply any segments that were present in the original profile
        if (!segments.isEmpty()) {
            // finer("Applying segments");
            reapplySegments(segments, newProfile);
        }
        // finer("Returning profile");
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
    private ISegmentedProfile calculateZahnRoskieProfile()
            throws UnavailableBorderPointException, UnavailableBorderTagException {

        // Ensure all nuclei point in the same direction

        // Taggable template;
        // if(target instanceof Nucleus){
        // This does not work, the vertical nucleus has not been established
        // when profiling begins
        // template = ((Nucleus)target).getVerticallyRotatedNucleus();
        // } else {
        // template = target;
        // }

        float[] profile = new float[target.getBorderLength()];
        int window = target.getWindowSize(ProfileType.ANGLE);
        int index = 0;

        Iterator<IBorderPoint> it = target.getBorderList().iterator();
        while (it.hasNext()) {

            IBorderPoint point = it.next();

            IBorderPoint prev = point.prevPoint(window);
            IBorderPoint next = point.nextPoint(window);

            // IBorderPoint prev = point.prevPoint();
            // IBorderPoint next = point.nextPoint();

            // Get the equation between the first two points
            LineEquation eq = new DoubleEquation(prev, point);

            IPoint p = eq.getPointOnLine(point, point.getLengthTo(prev)); // move
                                                                          // out
                                                                          // along
                                                                          // line

            // Don't go the wrong way along the line
            if (p.getLengthTo(prev) < point.getLengthTo(prev)) {
                p = eq.getPointOnLine(point, -point.getLengthTo(prev));
            }

            // Get the angle between the points
            double rad = AngleTools.angleBetweenLines(point, p, point, next);

            double angle = Math.toDegrees(rad);

            if (angle > 180) {
                angle = -180 + (angle - 180);
            }

            if (angle < -180) {
                angle = 180 + (angle + 180);
                // angle = -180-angle;
            }
            //
            // if(angle < 0){
            // angle = 0-angle;
            // }

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
