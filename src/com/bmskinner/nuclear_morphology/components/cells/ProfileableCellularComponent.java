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
package com.bmskinner.nuclear_morphology.components.cells;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileCreator;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.profiles.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.gui.Roi;

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
	
	private static final Logger LOGGER = Logger.getLogger(ProfileableCellularComponent.class.getName());

	private static final long serialVersionUID = 1L;

    /** The proportion of the perimeter to use for profiling */
    protected double windowProportion = IAnalysisOptions.DEFAULT_WINDOW_PROPORTION;

    /** The profiles for this object */
    protected volatile Map<ProfileType, ISegmentedProfile> profileMap = new ConcurrentHashMap<>();

    /** The indexes of landmarks in the profiles and border list */
    protected Map<Landmark, Integer> profileLandmarks = new HashMap<>();

    /** allow locking of segments and landmarks */
    protected boolean isLocked = false;

    /*  TRANSIENT FIELDS  */

    /** The chosen window size in pixels based on the window proportion */
    protected transient int windowSize;

    
    /**
     * Construct with an ROI, a source image and channel, and the original
     * position in the source image. It sets the immutable original centre of
     * mass, and the mutable current centre of mass. It also assigns a random ID
     * to the component.
     * 
     * @param roi the roi of the object
     * @param centerOfMass the original centre of mass of the component
     * @param source the image file the component was found in
     * @param channel the RGB channel the component was found in
     * @param position the bounding position of the component in the original image
     */
    protected ProfileableCellularComponent(@NonNull Roi roi, @NonNull IPoint centreOfMass, 
    		File source, int channel, int[] position) {
    	super(roi, centreOfMass, source, channel, position);
    }
    
    /**
     * Construct with an ROI, a source image and channel, and the original
     * position in the source image. It sets the immutable original centre of
     * mass, and the mutable current centre of mass. It also assigns a random ID
     * to the component.
     * 
     * @param roi the roi of the object
     * @param centerOfMass the original centre of mass of the component
     * @param source the image file the component was found in
     * @param channel the RGB channel the component was found in
     * @param position the bounding position of the component in the original image
     * @param id the id of the component. Only use when deserialising!
     */
    protected ProfileableCellularComponent(@NonNull Roi roi, @NonNull IPoint centreOfMass, 
    		File source, int channel, int[] position, @Nullable UUID id) {
        super(roi, centreOfMass, source, channel, position, id);
    }

    /**
     * Create a new component based on the given template object. If the object has segments,
     * these will be copied to the new component.
     * @param c
     * @throws UnprofilableObjectException
     */
    protected ProfileableCellularComponent(@NonNull final CellularComponent c) throws UnprofilableObjectException {
        super(c);

        if (c instanceof Taggable) {

            Taggable comp = (Taggable) c;

            this.windowProportion = comp.getWindowProportion(ProfileType.ANGLE);
            this.windowSize = comp.getWindowSize(ProfileType.ANGLE);

            for (ProfileType type : ProfileType.values()) {

                try {
                    profileMap.put(type, comp.getProfile(type).copy());
                } catch (UnavailableProfileTypeException e) {
                	// not present in this profile; possibly a deprecated type; ignore and continue
                } catch (ProfileException e) {
                    LOGGER.log(Loggable.STACK, "Cannot copy " + type + " from template", e);
                }
            }
            this.setBorderTags(comp.getBorderTags());

            this.isLocked = comp.isLocked();
        } else {
            throw new UnprofilableObjectException("Object is not a profileable object");
        }
        
    }
    
    /**
     * Faster constructor used when duplicating components
     * @param c
     * @throws UnprofilableObjectException
     */
    protected ProfileableCellularComponent(@NonNull final ProfileableCellularComponent c) {
    	 super(c);

    	 this.windowProportion = c.windowProportion;
         this.windowSize = c.windowSize;
         for(Landmark t : c.profileLandmarks.keySet())
        	 profileLandmarks.put(t, c.profileLandmarks.get(t));
         this.isLocked = c.isLocked;
         
         for (ProfileType type : c.profileMap.keySet()) {
             try {
                 this.profileMap.put(type, c.profileMap.get(type).copy());

             } catch (ProfileException e) {
                 LOGGER.log(Loggable.STACK, "Cannot make new profile type " + type + " from template", e);
                 LOGGER.warning("Error copying profile");
             }
         }
    }
    
    @Override
	public void initialise(double proportion) throws ComponentCreationException {
        if (proportion <= 0 || proportion >= 1)
            throw new ComponentCreationException("Must have a value between 0-1");
        
        windowProportion = proportion;
        double perimeter = getStatistic(Measurement.PERIMETER);

        double angleWindow = perimeter * proportion;
        angleWindow = angleWindow < 1 ? 1 : angleWindow;

        // calculate profiles
        windowSize = (int) Math.ceil(angleWindow);

        try {
            calculateProfiles();
        } catch (ProfileException e) {
            throw new ComponentCreationException("Could not calculate profiles due to "+e.getMessage(), e);
        }

    }

    public IBorderPoint getPoint(@NonNull Landmark tag) throws UnavailableBorderTagException {
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
    protected void calculateStatistic(Measurement stat) {

       super.calculateStatistic(stat);

        if (Measurement.MIN_DIAMETER.equals(stat))
            setStatistic(stat, getNarrowestDiameter());

        if (Measurement.PATH_LENGTH.equals(stat))
        	setStatistic(stat, getPathLength(ProfileType.ANGLE));

        if (Measurement.PERIMETER.equals(stat)) {
            double perimeter=0;
            for(IBorderPoint p : getBorderList()){
                perimeter += p.getLengthTo(p.nextPoint());
            }
            setStatistic(stat, perimeter);
        }

    }

    /*
     *  
     * Methods implementing the Taggable interface 
     *
     */

    @Override
    public IBorderPoint getBorderPoint(@NonNull Landmark tag) throws UnavailableBorderTagException {
    	int borderIndex = this.getBorderIndex(tag);

    	if (borderIndex < 0 || borderIndex >= this.getBorderLength())
    		throw new UnavailableBorderTagException(String.format("No tag '%s'; registered as index %s", tag, borderIndex));

    	return getBorderPoint(borderIndex);
    }

    @Override
	public Map<Landmark, Integer> getBorderTags() {
        Map<Landmark, Integer> result = new HashMap<>();
        for (Landmark b : profileLandmarks.keySet()) {
            result.put(b, profileLandmarks.get(b));
        }
        return result;
    }
    
    @Override
	public Landmark getBorderTag(@NonNull Landmark reference, int index) throws UnavailableBorderTagException {
        int newIndex = getOffsetBorderIndex(reference, index);
        return this.getBorderTag(newIndex);
    }

    @Override
	public Landmark getBorderTag(int index) {
        for (Entry<Landmark, Integer> entry : profileLandmarks.entrySet()) {
            if (entry.getValue() == index)
                return entry.getKey();
        }
        return null;
    }
    
    @Override
	public int getBorderIndex(@NonNull Landmark tag) throws UnavailableBorderTagException {
        if (profileLandmarks.containsKey(tag))
            return profileLandmarks.get(tag);
        throw new UnavailableBorderTagException("Tag "+tag+" is not present");
    }

    
    /**
     * Replace the tags in the object with the given tag map
     * @param m
     */
    private void setBorderTags(Map<Landmark, Integer> m) {
        if (isLocked)
            return;
        profileLandmarks.clear();
        for(Entry<Landmark, Integer> entry : m.entrySet())
        	profileLandmarks.put(entry.getKey(), entry.getValue());
    }

    @Override
	public void setBorderTag(@NonNull Landmark tag, int i) {
        if (isLocked)
            return;
        
        if (i < 0 || i >= this.getBorderLength())
            throw new IllegalArgumentException(String.format("Index %s is out of bounds for border length %s", i, getBorderLength()));

        try {

        	profileLandmarks.put(tag, i);
        	
            // When moving the RP, move all segments to match
            if (Landmark.REFERENCE_POINT.equals(tag)) {
                ISegmentedProfile p = getProfile(ProfileType.ANGLE);
                int oldRP = getBorderIndex(tag);
                int diff = i - oldRP;
                try {
                    p.nudgeSegments(diff);
                } catch (ProfileException e) {
                    LOGGER.log(Loggable.STACK, "Error nudging segments when assigning RP", e);
                    return;
                }
                setProfile(ProfileType.ANGLE, p);

            }

            // The intersection point should always be opposite the orientation point
            if (Landmark.ORIENTATION_POINT.equals(tag)) {
                int intersectionIndex = this.getBorderIndex(this.findOppositeBorder(this.getBorderPoint(i)));
                this.setBorderTag(Landmark.INTERSECTION_POINT, intersectionIndex);
            }


        } catch (UnavailableProfileTypeException e) {
        	LOGGER.log(Loggable.STACK, "Unable to find angle profile in object", e);
        } catch(UnavailableBorderTagException e) {
        	LOGGER.log(Loggable.STACK, String.format("Error getting border tag %s for object", tag), e);
        }
    }

    @Override
	public void setBorderTag(@NonNull Landmark reference, @NonNull Landmark tag, int i) throws UnavailableBorderTagException {
        if (isLocked)
            return;
        int newIndex = getOffsetBorderIndex(reference, i);
        setBorderTag(tag, newIndex);
    }

    @Override
	public boolean hasBorderTag(@NonNull Landmark tag) {
        return this.profileLandmarks.containsKey(tag);
    }

    @Override
	public boolean hasBorderTag(int index) {
        return this.profileLandmarks.containsValue(index);
    }

    @Override
	public boolean hasBorderTag(@NonNull Landmark tag, int index) {

		try {
			int newIndex = getOffsetBorderIndex(tag, index);
			return this.hasBorderTag(newIndex);
		} catch (UnavailableBorderTagException e) {
			LOGGER.log(Loggable.STACK, e.getMessage(), e);
			return false;
		}
        
    }

    @Override
	public int getOffsetBorderIndex(@NonNull Landmark reference, int index) throws UnavailableBorderTagException {
    	return wrapIndex(index + this.getBorderIndex(reference));
    }

    /*
     * ############################################# 
     * Methods implementing the Profileable interface 
     * #############################################
     */

    @Override
	public boolean isLocked() {
        return isLocked;
    }

    @Override
	public void setLocked(boolean b) {
        isLocked = b;
    }

    @Override
    public int getWindowSize(@NonNull ProfileType type) {
        return windowSize;
    }

    @Override
	public double getWindowProportion(@NonNull ProfileType type) {
        return windowProportion;
    }

    @Override
	public void setWindowProportion(@NonNull ProfileType type, double d) {
        if (d <= 0 || d >= 1)
            throw new IllegalArgumentException("Angle window proportion must be higher than 0 and less than 1");

        if (isLocked)
            return;

        if (type.equals(ProfileType.ANGLE)) {

            windowProportion = d;

            double perimeter = this.getStatistic(Measurement.PERIMETER);
            double angleWindow = perimeter * d;

            // calculate profiles
            windowSize = (int) Math.round(angleWindow);
            LOGGER.finest( "Recalculating angle profile");
            ProfileCreator creator = new ProfileCreator(this);
            ISegmentedProfile profile;
            try {
                profile = creator.createProfile(ProfileType.ANGLE);
            } catch (ProfileException e) {
                LOGGER.warning("Unable to set window proportion");
                LOGGER.log(Loggable.STACK, e.getMessage(), e);
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
        	LOGGER.finer( "Getting profile: "+type);
        	ISegmentedProfile template = profileMap.get(type);
        	return template.copy();
        	
        } catch (ProfileException e) {
            throw new UnavailableProfileTypeException("Cannot get profile type " + type, e);
        }
    }

    @Override
	public boolean hasProfile(@NonNull ProfileType type) {
        return this.profileMap.containsKey(type);
    }

    @Override
	public ISegmentedProfile getProfile(@NonNull ProfileType type, @NonNull Landmark tag)
            throws ProfileException, UnavailableBorderTagException, UnavailableProfileTypeException {

        if (!this.hasBorderTag(tag))
            throw new UnavailableBorderTagException("Tag " + tag + " not present");

        // fetch the index of the pointType (the new zero)
        int tagIndex = profileLandmarks.get(tag);
        
        // offset the angle profile to start at the pointIndex
        ISegmentedProfile profile = getProfile(type);
        return profile.offset(tagIndex);
    }
    
    /**
     * 
     * @param p
     * @param pointType
     * @throws UnavailableBorderTagException 
     * @throws UnavailableProfileTypeException 
     */
    @Override
	public void setProfile(@NonNull ProfileType type, @NonNull Landmark tag, @NonNull ISegmentedProfile p) throws UnavailableBorderTagException, UnavailableProfileTypeException {

        if (isLocked)
            return;

        if (!this.hasBorderTag(tag))
            throw new UnavailableBorderTagException("Tag " + tag + " is not present");

        if (this.getBorderLength() != p.size())
        	throw new IllegalArgumentException(String.format("Input profile length (%d) does not match border length (%d)", p.size(), getBorderLength()));

        // fetch the index of the pointType (the zero of the input profile)
        int tagIndex = this.profileLandmarks.get(tag);
        
        // Store the old profile in case
        ISegmentedProfile oldProfile = getProfile(type);
        
        try {
            // remove the offset from the profile, by setting the profile to start from the pointIndex
            assignProfile(type, p.offset(-tagIndex));
        } catch (ProfileException e) {
            LOGGER.log(Loggable.STACK, "Error setting profile " + type + " at " + tag, e);
            assignProfile(type, oldProfile);
        }

    }

    @Override
	public void setProfile(@NonNull ProfileType type, @NonNull ISegmentedProfile profile) {

        if (isLocked)
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
    		LOGGER.finest( "Attempting to create profile "+type);
    		ISegmentedProfile profile = creator.createProfile(type);
    		LOGGER.finest( "Assigning profile "+type);
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
                    LOGGER.log(Loggable.STACK, e.getMessage(), e);
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
            LOGGER.log(Loggable.STACK, "Error getting angle profile ", e);
        }
        return pathLength;
    }

    @Override
	public void reverse() {

        super.reverse();

        if (isLocked) {
            return;
        }
        for (Entry<ProfileType, ISegmentedProfile> entry : profileMap.entrySet()) {
            ISegmentedProfile profile = entry.getValue();
            profile.reverse();
            assignProfile(entry.getKey(), profile);
        }

        // replace the tag positions also
        for (Entry<Landmark, Integer> entry : profileLandmarks.entrySet()) {
            int index = entry.getValue();

            // if was 0, will be  <length-1>; if
            // was length-1,  will be 0
            int newIndex = this.getBorderLength() - index - 1;
            
            // update the bordertag map directly to avoid segmentation changes
            // due to RP shift
            profileLandmarks.put(entry.getKey(), newIndex);
        }
    }

    public IBorderPoint getNarrowestDiameterPoint() throws UnavailableBorderPointException {

        try {

            int index = this.getProfile(ProfileType.DIAMETER).getIndexOfMin();
            return IBorderPoint.makeNew(this.getBorderPoint(index));

        } catch (UnavailableProfileTypeException | ProfileException e) {
            LOGGER.log(Loggable.STACK, "Error getting diameter profile minimum", e);
            throw new UnavailableBorderPointException("Error getting diameter profile minimum");
        }

    }

    public double getNarrowestDiameter() {
        try {
            return Arrays.stream(this.getProfile(ProfileType.DIAMETER).toDoubleArray()).min().orElse(0);
        } catch (UnavailableProfileTypeException e) {
            LOGGER.log(Loggable.STACK, "Error getting diameter profile", e);
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
        double perimeter = this.getStatistic(Measurement.PERIMETER);
        if(perimeter==Statistical.ERROR_CALCULATING_STAT)
            calculateStatistic(Measurement.PERIMETER);
        
        windowSize = (int) Math.round( perimeter * windowProportion);

        // Check if calculation needed
        boolean isRecalculate = false;
        for (ProfileType type : ProfileType.values()) {
            isRecalculate |= !hasProfile(type);
        }
        
        
        if (isRecalculate) {
            try {
                calculateProfiles();
            } catch (ProfileException e) {
                LOGGER.warning("Error calculating profiles");
                LOGGER.log(Loggable.STACK, e.getMessage(), e);
            }
        }
    }
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp;
		temp = Double.doubleToLongBits(windowProportion);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((profileLandmarks == null) ? 0 : profileLandmarks.hashCode());
		result = prime * result + ((profileMap == null) ? 0 : profileMap.hashCode());
		result = prime * result + (isLocked ? 1231 : 1237);
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
		if (Double.doubleToLongBits(windowProportion) != Double.doubleToLongBits(other.windowProportion))
			return false;
		if (profileLandmarks == null) {
			if (other.profileLandmarks != null)
				return false;
		} else if (!profileLandmarks.equals(other.profileLandmarks))
			return false;
		if (profileMap == null) {
			if (other.profileMap != null)
				return false;
		} else if (!profileMap.equals(other.profileMap))
			return false;
		if (isLocked != other.isLocked)
			return false;
		return true;
	}
}
