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


package com.bmskinner.nuclear_morphology.components;

import ij.gui.Roi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileCreator;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.generic.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderPoint;
import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment;
import com.bmskinner.nuclear_morphology.components.generic.FloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;

/**
 * This is the class of objects that can have profiles applied to them.
 * Positions around the border of the component can be tagged; the profiles will
 * track the tags.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public abstract class ProfileableCellularComponent extends DefaultCellularComponent implements Taggable {

	private static final long serialVersionUID = 1L;

    /** The proportion of the perimeter to use for profiling */
    protected double angleWindowProportion = IAnalysisOptions.DEFAULT_WINDOW_PROPORTION;

    /** The profiles for this object */
    protected Map<ProfileType, ISegmentedProfile> profileMap = new HashMap<>();

    /** The indexes of tags in the profiles and border list */
    protected Map<Tag, Integer> borderTags = new HashMap<>();

    /** allow locking of segments and tags */
    protected boolean segsLocked = false;

    /*  TRANSIENT FIELDS  */

    /** The chosen window size based on the window proportion */
    protected transient int angleProfileWindowSize;

    /**
     * Construct with an ROI, a source image and channel, and the original
     * position in the source image
     * 
     * @param roi
     * @param f
     * @param channel
     * @param position
     * @param centreOfMass
     */
    public ProfileableCellularComponent(@NonNull Roi roi, @NonNull IPoint centreOfMass, @NonNull File f, int channel, int[] position) {
        super(roi, centreOfMass, f, channel, position);
    }

    /**
     * Create a new component based on the given template object. If the object has segments,
     * these will be copied to the new component.
     * @param c
     * @throws UnprofilableObjectException
     */
    public ProfileableCellularComponent(@NonNull final CellularComponent c) throws UnprofilableObjectException {
        super(c);

        if (c instanceof Taggable) {

            Taggable comp = (Taggable) c;

            this.angleWindowProportion = comp.getWindowProportion(ProfileType.ANGLE);
            this.angleProfileWindowSize = comp.getWindowSize(ProfileType.ANGLE);

            for (ProfileType type : ProfileType.values()) {

                try {
                    ISegmentedProfile oldProfile = comp.getProfile(type);
                    this.profileMap.put(type, oldProfile.copy());

                } catch (UnavailableProfileTypeException e) {
                    stack("Cannot get profile type " + type + " from template", e);
                    warn("Error copying profile");
                } catch (ProfileException e) {
                    stack("Cannot make new profile type " + type + " from template", e);
                    warn("Error copying profile");
                }
            }
            this.setBorderTags(comp.getBorderTags());

            this.segsLocked = comp.isLocked();
        } else {
            throw new UnprofilableObjectException("Object is not a profileable object");
        }
        
    }
    
    /**
     * Used when duplicating components
     * @param c
     * @throws UnprofilableObjectException
     */
    protected ProfileableCellularComponent(@NonNull final ProfileableCellularComponent c) throws UnprofilableObjectException {
    	 super(c);

    	 this.angleWindowProportion = c.angleWindowProportion;
         this.angleProfileWindowSize = c.angleProfileWindowSize;
         for(Tag t : c.borderTags.keySet())
        	 borderTags.put(t, c.borderTags.get(t).intValue());
         this.segsLocked = c.segsLocked;
         
         for (ProfileType type : c.profileMap.keySet()) {
             try {
                 this.profileMap.put(type, c.profileMap.get(type).copy());

             } catch (ProfileException e) {
                 stack("Cannot make new profile type " + type + " from template", e);
                 warn("Error copying profile");
             }
         }
    }
    

    /*
     * Finds the key points of interest around the border of the Nucleus. Can
     * use several different methods, and take a best-fit, or just use one. The
     * default in a round nucleus is to get the longest diameter and set this as
     * the head/tail axis.
     */
    @Override
	public abstract void findPointsAroundBorder() throws ComponentCreationException;

    @Override
	public void initialise(double proportion) throws ComponentCreationException {
        if (proportion <= 0 || proportion >= 1)
            throw new ComponentCreationException("Must have a value between 0-1");
        
        angleWindowProportion = proportion;
        double perimeter = getStatistic(PlottableStatistic.PERIMETER);

        double angleWindow = perimeter * proportion;
        angleWindow = angleWindow < 1 ? 1 : angleWindow;

        // calculate profiles
        angleProfileWindowSize = (int) Math.ceil(angleWindow);

        try {
            calculateProfiles();
        } catch (ProfileException e) {
            throw new ComponentCreationException("Could not calculate profiles due to "+e.getMessage(), e);
        }

    }

    public IBorderPoint getPoint(@NonNull Tag tag) throws UnavailableBorderTagException {
        int index = this.getBorderIndex(tag);
        return this.getBorderPoint(index);
    }

    /**
     * Checks if the smoothed array nuclear shape profile has the appropriate
     * orientation.Counts the number of points above 180 degrees in each half of
     * the array.
     * 
     * @return
     * @throws Exception
     */
    public abstract boolean isProfileOrientationOK();

    @Override
    protected double calculateStatistic(PlottableStatistic stat) {

        double result = super.calculateStatistic(stat);

        if (PlottableStatistic.MIN_DIAMETER.equals(stat))
            return this.getNarrowestDiameter();

        if (PlottableStatistic.PATH_LENGTH.equals(stat))
            return this.getPathLength(ProfileType.ANGLE);

        if (PlottableStatistic.PERIMETER.equals(stat)) {
            double perimeter=0;
            for(IBorderPoint p : getBorderList()){
                perimeter += p.getLengthTo(p.nextPoint());
            }
            return perimeter;
        }

        return result;
    }

    /*
     * ############################################# 
     * Methods implementing the Taggable interface 
     * #############################################
     */

    @Override
	public IBorderPoint getBorderTag(@NonNull Tag tag) throws UnavailableBorderTagException {

        IBorderPoint result = new DefaultBorderPoint(0, 0);

        int borderIndex = this.getBorderIndex(tag);

        if (borderIndex < 0 || borderIndex >= this.getBorderLength())
            throw new UnavailableBorderTagException(String.format("No tag '%s'; registered as index %s", tag, borderIndex));

        result = this.getBorderPoint((this.getBorderIndex(tag)));
        return result;
    }

    @Override
	public IBorderPoint getBorderPoint(@NonNull Tag tag) throws UnavailableBorderTagException {
        return getBorderTag(tag);
    }

    @Override
	public Map<Tag, Integer> getBorderTags() {
        Map<Tag, Integer> result = new HashMap<>();
        for (Tag b : borderTags.keySet()) {
            result.put(b, borderTags.get(b));
        }
        return result;
    }

    
    /**
     * Replace the tags in the object with the given tag map
     * @param m
     */
    private void setBorderTags(Map<Tag, Integer> m) {
        if (segsLocked)
            return;
        borderTags.clear();
        for(Tag t : m.keySet())
        	borderTags.put(t, m.get(t).intValue());
    }

    @Override
	public int getBorderIndex(@NonNull Tag tag) throws UnavailableBorderTagException {

        if (borderTags.containsKey(tag))
            return borderTags.get(tag);
        throw new UnavailableBorderTagException("Tag "+tag+" is not present");
    }

    @Override
	public void setBorderTag(@NonNull Tag tag, int i) {
        if (segsLocked)
            return;
        
        if (i < 0 || i >= this.getBorderLength())
            throw new IllegalArgumentException(String.format("Index %s is out of bounds for border length %s", i, getBorderLength()));

        try {

        	borderTags.put(tag, i);
        	
            // When moving the RP, move all segments to match
            if (Tag.REFERENCE_POINT.equals(tag)) {
                ISegmentedProfile p = getProfile(ProfileType.ANGLE);
                int oldRP = getBorderIndex(tag);
                int diff = i - oldRP;
                try {
                    p.nudgeSegments(diff);
                } catch (ProfileException e) {
                    stack("Error nudging segments when assigning RP", e);
                    return;
                }
                setProfile(ProfileType.ANGLE, p);

            }

            // The intersection point should always be opposite the orientation point
            if (Tag.ORIENTATION_POINT.equals(tag)) {
                int intersectionIndex = this.getBorderIndex(this.findOppositeBorder(this.getBorderPoint(i)));
                this.setBorderTag(Tag.INTERSECTION_POINT, intersectionIndex);
            }


        } catch (UnavailableProfileTypeException e) {
        	stack(String.format("Unable to find angle profile in object"), e);
        } catch(UnavailableBorderTagException e) {
        	stack(String.format("Error getting border tag %s for object", tag), e);
        }
    }

    @Override
	public void setBorderTag(@NonNull Tag reference, @NonNull Tag tag, int i) throws UnavailableBorderTagException {
        if (segsLocked)
            return;

        int newIndex = getOffsetBorderIndex(reference, i);
        this.setBorderTag(tag, newIndex);
    }

    @Override
	public void replaceBorderTags(@NonNull Map<Tag, Integer> tagMap) {

        try {

            int oldRP = getBorderIndex(Tag.REFERENCE_POINT);
            ISegmentedProfile p = getProfile(ProfileType.ANGLE);

            this.borderTags = tagMap;

            int newRP = getBorderIndex(Tag.REFERENCE_POINT);
            int diff = newRP - oldRP;
            try {
                p.nudgeSegments(diff);
            } catch (ProfileException e) {
                warn("Cannot offset profile");
                fine("Error moving segments", e);
                return;
            }
            finest("Old RP at " + oldRP);
            finest("New RP at " + newRP);
            finest("Moving segments by" + diff);

            setProfile(ProfileType.ANGLE, p);

            int newOP = getBorderIndex(Tag.ORIENTATION_POINT);
            int intersectionIndex = this.getBorderIndex(this.findOppositeBorder(this.getBorderPoint(newOP)));
            this.borderTags.put(Tag.INTERSECTION_POINT, intersectionIndex);

        } catch (UnavailableProfileTypeException | UnavailableBorderTagException e) {
            stack("Error replacing borser tags", e);
        }

    }

    @Override
	public boolean hasBorderTag(@NonNull Tag tag) {
        return this.borderTags.containsKey(tag);
    }

    @Override
	public boolean hasBorderTag(int index) {
        return this.borderTags.containsValue(index);
    }

    @Override
	public boolean hasBorderTag(@NonNull Tag tag, int index) {

		try {
			int newIndex = getOffsetBorderIndex(tag, index);
			return this.hasBorderTag(newIndex);
		} catch (UnavailableBorderTagException e) {
			stack(e);
			return false;
		}
        
    }

    @Override
	public int getOffsetBorderIndex(@NonNull Tag reference, int index) throws UnavailableBorderTagException {
    	return wrapIndex(index + this.getBorderIndex(reference));
    }

    @Override
	public Tag getBorderTag(@NonNull Tag reference, int index) throws UnavailableBorderTagException {
        int newIndex = getOffsetBorderIndex(reference, index);
        return this.getBorderTag(newIndex);
    }

    @Override
	public Tag getBorderTag(int index) {

        for (Tag b : this.borderTags.keySet()) {
            if (this.borderTags.get(b) == index) {
                return b;
            }
        }
        return null;
    }

    /*
     * ############################################# 
     * Methods implementing the Profileable interface 
     * #############################################
     */

    @Override
	public boolean isLocked() {
        return segsLocked;
    }

    @Override
	public void setLocked(boolean b) {
        segsLocked = b;
    }

    @Override
    public int getWindowSize(@NonNull ProfileType type) {
        return angleProfileWindowSize;
    }

    @Override
	public double getWindowProportion(@NonNull ProfileType type) {
        return angleWindowProportion;
    }

    @Override
	public void setWindowProportion(@NonNull ProfileType type, double d) {
        if (d <= 0 || d >= 1)
            throw new IllegalArgumentException("Angle window proportion must be higher than 0 and less than 1");

        if (segsLocked)
            return;

        if (type.equals(ProfileType.ANGLE)) {

            angleWindowProportion = d;

            double perimeter = this.getStatistic(PlottableStatistic.PERIMETER);
            double angleWindow = perimeter * d;

            // calculate profiles
            angleProfileWindowSize = (int) Math.round(angleWindow);
            finest("Recalculating angle profile");
            ProfileCreator creator = new ProfileCreator(this);
            ISegmentedProfile profile;
            try {
                profile = creator.createProfile(ProfileType.ANGLE);
            } catch (ProfileException e) {
                warn("Unable to set window proportion");
                stack(e);
                return;
            }

            assignProfile(ProfileType.ANGLE, profile);

        }
    }

    @Override
	public ISegmentedProfile getProfile(@NonNull ProfileType type) throws UnavailableProfileTypeException {

        if (!this.hasProfile(type))
            throw new UnavailableProfileTypeException("Cannot get profile type " + type);

        try {
        	fine("Getting profile: "+type);
        	ISegmentedProfile template = profileMap.get(type);
        	return template.copy();
//        	if(template.getSegmentCount()>1)
//        		return new SegmentedFloatProfile(template);
//        	return new SegmentedFloatProfile( (IProfile)template);
        	
        } catch (ProfileException e) {
            throw new UnavailableProfileTypeException("Cannot get profile type " + type, e);
        }
    }

    @Override
	public boolean hasProfile(@NonNull ProfileType type) {
        return this.profileMap.containsKey(type);
    }

    @Override
	public ISegmentedProfile getProfile(@NonNull ProfileType type, @NonNull Tag tag)
            throws ProfileException, UnavailableBorderTagException, UnavailableProfileTypeException {

        // fetch the index of the pointType (the new zero)
        if (!this.hasBorderTag(tag))
            throw new UnavailableBorderTagException("Tag " + tag + " not present");

        int pointIndex = this.borderTags.get(tag);

        // offset the angle profile to start at the pointIndex
        return getProfile(type).offset(pointIndex);
    }
    
    /**
     * 
     * @param p
     * @param pointType
     * @throws UnavailableBorderTagException 
     * @throws UnavailableProfileTypeException 
     */
    @Override
	public void setProfile(@NonNull ProfileType type, @NonNull Tag tag, @NonNull ISegmentedProfile p) throws UnavailableBorderTagException, UnavailableProfileTypeException {

        if (segsLocked)
            return;

        if (!this.hasBorderTag(tag))
            throw new UnavailableBorderTagException("Tag " + tag + " is not present");

        if (this.getBorderLength() != p.size())
        	throw new IllegalArgumentException(String.format("Input profile length (%d) does not match border length (%d)", p.size(), getBorderLength()));

        // fetch the index of the pointType (the zero of the input profile)
        int pointIndex = this.borderTags.get(tag);

        
        // Store the old profile in case
        ISegmentedProfile oldProfile = getProfile(type);
        
        try {
            // remove the offset from the profile, by setting the profile to start from the pointIndex
            assignProfile(type, p.offset(-pointIndex));
        } catch (ProfileException e) {
            stack("Error setting profile " + type + " at " + tag, e);
            assignProfile(type, oldProfile);
        }

    }

    @Override
	public void setProfile(@NonNull ProfileType type, @NonNull ISegmentedProfile profile) {

        if (segsLocked)
            return;

        // Replace frankenprofiles completely
        assignProfile(type, profile);
    }
    
    protected void assignProfile(@NonNull ProfileType type, @NonNull ISegmentedProfile profile) {
    	profileMap.put(type, profile);
    }

    @Override
    public void calculateProfiles() throws ProfileException {

    	ProfileCreator creator = new ProfileCreator(this);

    	for (ProfileType type : ProfileType.values()) {
    		finest("Attempting to create profile "+type);
    		ISegmentedProfile profile = creator.createProfile(type);
    		finest("Assigning profile "+type);
    		assignProfile(type, profile);
    	}
    }

    @Override
	public void setSegmentStartLock(boolean lock, @NonNull UUID segID) {
        if (segID == null)
            throw new IllegalArgumentException("Requested seg id is null");

        for (ISegmentedProfile p : profileMap.values()) {

            if (p.hasSegment(segID)) {
                try {
                    p.getSegment(segID).setLocked(lock);
                } catch (UnavailableComponentException e) {
                    stack(e);
                }
            }
        }
    }

    private double getPathLength(ProfileType type) {
        double pathLength = 0;

        try {

            IProfile profile = this.getProfile(type);

            // First previous point is the last point of the profile
            IPoint prevPoint = IPoint.makeNew(0, profile.get(this.getBorderLength() - 1));

            for (int i = 0; i < this.getBorderLength(); i++) {
                double normalisedX = ((double) i / (double) this.getBorderLength()) * 100; // normalise
                                                                                           // to
                                                                                           // 100
                                                                                           // length

                // We are measuring along the chart of angle vs position
                // Each median angle value is treated as an XYPoint
                IPoint thisPoint = IPoint.makeNew(normalisedX, profile.get(i));
                pathLength += thisPoint.getLengthTo(prevPoint);
                prevPoint = thisPoint;
            }
        } catch (UnavailableProfileTypeException e) {
            stack("Error getting angle profile ", e);
        }
        return pathLength;
    }

    @Override
	public void reverse() {

        super.reverse();

        if (segsLocked) {
            return;
        }
        for (ProfileType type : profileMap.keySet()) {

            ISegmentedProfile profile = profileMap.get(type);
            profile.reverse();
            assignProfile(type, profile);
        }

        // replace the tag positions also
        Set<Tag> keys = borderTags.keySet();
        for (Tag s : keys) {
            int index = borderTags.get(s);
            int newIndex = this.getBorderLength() - index - 1; // if was 0, will
                                                               // now be
                                                               // <length-1>; if
                                                               // was length-1,
                                                               // will be 0
            // update the bordertag map directly to avoid segmentation changes
            // due to RP shift
            borderTags.put(s, newIndex);
        }
    }

    public IBorderPoint getNarrowestDiameterPoint() throws UnavailableBorderPointException {

        try {

            int index = this.getProfile(ProfileType.DIAMETER).getIndexOfMin();
            return IBorderPoint.makeNew(this.getBorderPoint(index));

        } catch (UnavailableProfileTypeException | ProfileException e) {
            stack("Error getting diameter profile minimum", e);
            throw new UnavailableBorderPointException("Error getting diameter profile minimum");
        }

    }

    public double getNarrowestDiameter() {
        try {
            return Arrays.stream(this.getProfile(ProfileType.DIAMETER).toDoubleArray()).min().orElse(0);
        } catch (UnavailableProfileTypeException e) {
            stack("Error getting diameter profile", e);
            return 0;
        }
    }

    /**
     * Go around the border of the object, measuring the angle to the OP. If the
     * angle is closest to target angle, return the distance to the CoM.
     * 
     * @param angle
     *            the target angle
     * @return the distance from the closest border point at the requested angle
     *         to the CoM
     */
    @Override
    public double getDistanceFromCoMToBorderAtAngle(double angle) {

        double bestDiff = 180;
        double bestDistance = 180;

        for (int i = 0; i < getBorderLength(); i++) {
            IPoint p = getBorderPoint(i);
            double distance = p.getLengthTo(getCentreOfMass());
            double pAngle = getCentreOfMass().findSmallestAngle(p, IPoint.makeNew(0, -10));
            if (p.getX() < 0)
                pAngle = 360 - pAngle;

            if (Math.abs(angle - pAngle) < bestDiff) {
                bestDiff = Math.abs(angle - pAngle);
                bestDistance = distance;
            }
        }
        return bestDistance;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

        in.defaultReadObject();
        restoreFieldsAfterDeserialisation();
       
    }
    
    protected void restoreFieldsAfterDeserialisation() {
    	 // set transient fields
        double perimeter = this.getStatistic(PlottableStatistic.PERIMETER);
        if(perimeter==Statistical.ERROR_CALCULATING_STAT)
            perimeter = this.calculateStatistic(PlottableStatistic.PERIMETER);
        
        angleProfileWindowSize = (int) Math.round( perimeter * angleWindowProportion);

        // Check if calculation needed
        boolean isRecalculate = false;
        for (ProfileType type : ProfileType.values()) {
            isRecalculate |= !hasProfile(type);
        }
        
        
        if (isRecalculate) {
            try {
                calculateProfiles();
            } catch (ProfileException e) {
                warn("Error calculating profiles");
                stack(e);
            }
        }
    }
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp;
		temp = Double.doubleToLongBits(angleWindowProportion);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((borderTags == null) ? 0 : borderTags.hashCode());
		result = prime * result + ((profileMap == null) ? 0 : profileMap.hashCode());
		result = prime * result + (segsLocked ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProfileableCellularComponent other = (ProfileableCellularComponent) obj;
		if (Double.doubleToLongBits(angleWindowProportion) != Double.doubleToLongBits(other.angleWindowProportion))
			return false;
		if (borderTags == null) {
			if (other.borderTags != null)
				return false;
		} else if (!borderTags.equals(other.borderTags))
			return false;
		if (profileMap == null) {
			if (other.profileMap != null)
				return false;
		} else if (!profileMap.equals(other.profileMap))
			return false;
		if (segsLocked != other.segsLocked)
			return false;
		return true;
	}
}
