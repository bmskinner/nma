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
package com.bmskinner.nuclear_morphology.components.generic;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Holds the ProfileAggregate with individual nucleus values, and stores the
 * indexes of BorderTags within the profile. Provides methods to get the median
 * and other quartiles from the collection. The Reference Point is always at the
 * zero index of the collection.
 * 
 * Also stores the NucleusBorderSegments within the profile, and provides
 * methods to transform the segments to fit profiles offset from given border
 * tags.
 * 
 * An internal ProfileCache holds average profiles to save repeated calculation
 * 
 * @author ben
 *
 */
@Deprecated
public class ProfileCollection implements IProfileCollection {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);

    private static final long serialVersionUID = 1L;

    private IProfileAggregate aggregate = null;

    private Map<BorderTagObject, Integer> indexes  = new HashMap<>();
    private List<IBorderSegment>          segments = new ArrayList<>();

    private final ProfileCache profileCache = new ProfileCache();

    private transient ProfileType type = null; // added in with 1.13.3 to allow
                                               // conversion to new format

    /**
     * Create an empty profile collection
     */
    public ProfileCollection() {
        indexes.put(Tag.REFERENCE_POINT, ZERO_INDEX);
    }
    
	@Override
	public IProfileCollection duplicate() {
//		DefaultProfileCollection pc = new DefaultProfileCollection();
//		
//		pc.length = length;
//		for(Tag t : indexes.keySet())
//			pc.indexes.put(t, indexes.get(t));
//		
//		pc.segments = new IBorderSegment[segments.length];
//		for(int i=0; i<segments.length; i++)
//			pc.segments[i] = segments[i].copy();
//		
//		return pc;
		return null;
	}
    
    @Override
    public int segmentCount() {
    	return segments.size();
    }
    
    @Override
    public boolean hasSegments() {
    	return segmentCount()>0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileCollection#getIndex(components.generic.
     * BorderTagObject)
     */
    @Override
    public int getIndex(Tag pointType) {
        if (pointType == null) {
            throw new IllegalArgumentException("The requested offset key is null: " + pointType);
        }
        if (indexes.containsKey(pointType)) {
            return indexes.get(pointType);
        } else {
            return -1;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileCollection#getBorderTags()
     */
    @Override
    public List<Tag> getBorderTags() {
        List<Tag> result = new ArrayList<Tag>();
        for (Tag s : indexes.keySet()) {
            result.add(s);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.IProfileCollection#hasBorderTag(components.generic.
     * BorderTagObject)
     */
    @Override
    public boolean hasBorderTag(Tag tag) {
        return indexes.keySet().contains(tag);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileCollection#getProfile(components.generic.
     * BorderTagObject, double)
     */
    public Profile getProfile(Tag tag, double quartile) throws UnavailableBorderTagException {

        if (tag == null) {
            throw new IllegalArgumentException("Tag is null");
        }

        if (!this.hasBorderTag(tag)) {
            throw new UnavailableBorderTagException("Tag is not present: " + tag.toString());
        }

        // If the profile is not in the cache, make it and add to the cache
        if (!profileCache.hasProfile(tag, quartile)) {

            int indexOffset = indexes.get(tag);
            IProfile profile;
            try {
                profile = getAggregate().getQuartile(quartile).offset(indexOffset);
            } catch (ProfileException e) {
                LOGGER.log(Loggable.STACK, "Unable to get profile for " + tag + " quartile " + quartile, e);
                return null;
            }
            profileCache.setProfile(tag, quartile, profile);

        }
        return profileCache.getProfile(tag, quartile);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.IProfileCollection#getSegmentedProfile(components.
     * generic.BorderTagObject)
     */
    public ISegmentedProfile getSegmentedProfile(Tag tag) throws UnavailableBorderTagException, ProfileException {
        if (tag == null) {
            throw new IllegalArgumentException("A profile key is required");
        }

        if (!this.hasBorderTag(tag)) {
            throw new UnavailableBorderTagException("Tag is not present: " + tag.toString());
        }

        ISegmentedProfile result = new SegmentedFloatProfile(getProfile(tag, Stats.MEDIAN), getSegments(tag));
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileCollection#getAggregate()
     */
    public IProfileAggregate getAggregate() {
        return aggregate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileCollection#hasAggregate()
     */
    public boolean hasAggregate() {
        if (aggregate == null) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileCollection#length()
     */
    @Override
    public int length() {
        if (this.hasAggregate()) {
            return aggregate.length();
        } else {
            return 0;
        }
    }


    @Override
    public List<IBorderSegment> getSegments(Tag tag) throws ProfileException {
        if (tag == null) {
            throw new IllegalArgumentException("The requested segment key is null: " + tag);
        }

        // this must be negative offset for segments
        // since we are moving the pointIndex back to the beginning
        // of the array
        int offset = -getIndex(tag);
        
        List<IBorderSegment> result = IBorderSegment.copy(segments);
        for(IBorderSegment s : result) {
        	s.offset(offset);
        }

//        List<IBorderSegment> result = IBorderSegment.nudge(segments, offset);

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.IProfileCollection#hasSegmentStartingWith(components.
     * generic.BorderTagObject)
     */
    @Override
    public boolean hasSegmentStartingWith(Tag tag) {

        if (getSegmentStartingWith(tag) == null) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.IProfileCollection#getSegmentStartingWith(components.
     * generic.BorderTagObject)
     */
    @Override
    public IBorderSegment getSegmentStartingWith(Tag tag) {
        List<IBorderSegment> segments;
        try {
            segments = this.getSegments(tag);
        } catch (ProfileException e) {
            LOGGER.log(Loggable.STACK, "Cannot get segments", e);
            return null;
        }

        IBorderSegment result = null;
        // get the name of the segment with the tag at the start
        for (IBorderSegment seg : segments) {

            if (seg.getStartIndex() == ZERO_INDEX) {
                result = seg;
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.IProfileCollection#hasSegmentEndingWith(components.
     * generic.BorderTagObject)
     */
    @Override
    public boolean hasSegmentEndingWith(Tag tag) {

        if (getSegmentEndingWith(tag) == null) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.IProfileCollection#getSegmentEndingWith(components.
     * generic.BorderTagObject)
     */
    @Override
    public IBorderSegment getSegmentEndingWith(Tag tag) {
        List<IBorderSegment> segments;
        try {
            segments = this.getSegments(tag);
        } catch (ProfileException e) {
            LOGGER.log(Loggable.STACK, "Cannot get segments", e);
            return null;
        }

        IBorderSegment result = null;
        // get the name of the segment with the tag at the start
        for (IBorderSegment seg : segments) {

            if (seg.getEndIndex() == ZERO_INDEX) {
                result = seg;
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileCollection#getSegmentContaining(int)
     */
    @Override
    public IBorderSegment getSegmentContaining(int index) {
        List<IBorderSegment> segments;
        try {
            segments = this.getSegments(Tag.REFERENCE_POINT);
        } catch (ProfileException e) {
            LOGGER.log(Loggable.STACK, "Cannot get segments", e);
            return null;
        }

        IBorderSegment result = null;
        // get the name of the segment with the tag at the start
        for (IBorderSegment seg : segments) {

            if (seg.contains(index)) {
                result = seg;
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.IProfileCollection#getSegmentContaining(components.
     * generic.BorderTagObject)
     */
    @Override
    public IBorderSegment getSegmentContaining(Tag tag) throws ProfileException {
        List<IBorderSegment> segments = this.getSegments(tag);

        IBorderSegment result = null;
        // get the name of the segment with the tag at the start
        for (IBorderSegment seg : segments) {

            if (seg.contains(ZERO_INDEX)) {
                result = seg;
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileCollection#addIndex(components.generic.
     * BorderTagObject, int)
     */
    @Override
    public void addIndex(Tag tag, int offset) {
        if (tag == null) {
            throw new IllegalArgumentException("BorderTagObject is null");
        }

        // Cannot move the RP from zero
        if (tag.equals(Tag.REFERENCE_POINT)) {
            return;
        }

        if (tag instanceof BorderTagObject) {
            indexes.put((BorderTagObject) tag, offset);
        }

        profileCache.clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileCollection#addSegments(java.util.List)
     */
    @Override
    public void addSegments(List<IBorderSegment> n) {
        if (n == null || n.isEmpty()) {
            throw new NullPointerException("String or segment list is null or empty");
        }

        if (this.length() != n.get(0).getProfileLength()) {
            throw new IllegalArgumentException("Segments total length (" + n.get(0).getProfileLength()
                    + ") does not fit aggregate (" + +this.length() + ")");
        }

        this.segments = n;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.IProfileCollection#addSegments(components.generic.
     * BorderTagObject, java.util.List)
     */
    @Override
    public void addSegments(Tag tag, List<IBorderSegment> n) throws ProfileException, UnavailableBorderTagException {
        if (n == null || n.isEmpty()) {
            throw new NullPointerException("String or segment list is null or empty");
        }

        if (!this.hasBorderTag(tag)) {
            throw new UnavailableBorderTagException("Tag " + tag + " not present");
        }

        if (this.length() != n.get(0).getProfileLength()) {
            throw new IllegalArgumentException("Segments total length (" + n.get(0).getProfileLength()
                    + ") does not fit aggregate (" + +this.length() + ")");
        }

        /*
         * The segments coming in are zeroed to the given pointType pointIndex
         * This means the indexes must be moved forwards appropriately. Hence,
         * add a positive offset.
         */
        int offset = getIndex(tag);

//        List<IBorderSegment> result = IBorderSegment.nudge(n, offset);
        List<IBorderSegment> result = IBorderSegment.copy(segments);
        for(IBorderSegment s : result) {
        	s.offset(offset);
        }
        this.segments = result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.IProfileCollection#createProfileAggregate(components.
     * CellCollection, components.generic.ProfileType, int)
     */
    public void createProfileAggregate(ICellCollection collection, ProfileType type, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Requested profile aggregate length is zero or negative");
        }
        if (collection == null || type == null) {
            throw new IllegalArgumentException("CellCollection or ProfileType is null");
        }
        profileCache.clear();
        aggregate = new ProfileAggregate(length);

        try {
            for (Nucleus n : collection.getNuclei()) {

                switch (type) {
                case FRANKEN:

                    aggregate.addValues(n.getProfile(type));

                    break;
                default:
                    aggregate.addValues(n.getProfile(type, Tag.REFERENCE_POINT));
                    break;

                }
            }
        } catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
            LOGGER.log(Loggable.STACK, "Error making profile aggregates", e);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.IProfileCollection#createProfileAggregate(components.
     * CellCollection, components.generic.ProfileType)
     */
    public void createProfileAggregate(ICellCollection collection, ProfileType type) {

        createProfileAggregate(collection, type, (int) collection.getMedianArrayLength());

    }

    @Override
    public void createProfileAggregate(ICellCollection collection, int length) {
        if (type != null) {
            // for(ProfileType type : ProfileType.values()){
            this.createProfileAggregate(collection, type, length);
            // }
        }

    }

    // @Override
    public void createProfileAggregate(ICellCollection collection) {
        if (type != null) {
            this.createProfileAggregate(collection, type);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileCollection#tagString()
     */
    @Override
    public String tagString() {

        StringBuilder builder = new StringBuilder();

        builder.append("\tPoint types:");
        for (Tag tag : this.indexes.keySet()) {
            builder.append("\t");
            builder.append(tag);
            builder.append(": ");
            builder.append(this.indexes.get(tag));
        }
        return builder.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileCollection#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.tagString());
        if (this.segments.isEmpty()) {
            builder.append("\r\nNo segments in profile collection");
        } else {
            try {

                for (Tag tag : this.indexes.keySet()) {
                    if (tag.type()
                            .equals(com.bmskinner.nuclear_morphology.components.generic.BorderTag.BorderTagType.CORE)) {
                        builder.append("\r\nSegments from " + tag + ":\r\n");
                        for (IBorderSegment s : this.getSegments(tag)) {
                            builder.append(s.toString() + "\r\n");
                        }
                    }
                }

            } catch (Exception e) {
                builder.append("\r\nError fetching segments");
            }
        }
        return builder.toString();

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.IProfileCollection#getIQRProfile(components.generic.
     * BorderTagObject)
     */
    public IProfile getIQRProfile(Tag tag) throws UnavailableBorderTagException {

        if (!this.hasBorderTag(tag)) {
            throw new UnavailableBorderTagException("Tag " + tag + " not present");
        }

        IProfile q25 = getProfile(tag, Stats.LOWER_QUARTILE);
        IProfile q75 = getProfile(tag, Stats.UPPER_QUARTILE);

        if (q25 == null || q75 == null) { // if something goes wrong, return a
                                          // zero profile

            LOGGER.warning("Problem calculating the IQR - setting to zero");
            return new Profile(0, aggregate.length());
        }

        return q75.subtract(q25);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.IProfileCollection#findMostVariableRegions(components.
     * generic.BorderTagObject)
     */
    public List<Integer> findMostVariableRegions(Tag tag) throws UnavailableBorderTagException {
        List<Integer> result = new ArrayList<Integer>();
        // get the IQR and maxima
        IProfile iqrProfile = getIQRProfile(tag);
        // iqrProfile.print();
        // iqrProfile.smooth(3).print();
        BooleanProfile maxima = iqrProfile.smooth(3).getLocalMaxima(3);
        // maxima.print();
        // Profile displayMaxima = maxima.multiply(50);

        // given the list of maxima, find the highest 3 regions
        // store the rank (1-3) and the index of the position at this rank
        // To future me: I am sorry about this.
        Map<Integer, Integer> values = new HashMap<Integer, Integer>(0);
        int minIndex = -1;
        try {
            minIndex = iqrProfile.getIndexOfMin();
        } catch (ProfileException e) {
            LOGGER.log(Loggable.STACK, "Error getting index", e);
            return result;
        } // ensure that our has begins with lowest data

        values.put(1, minIndex);
        values.put(2, minIndex);
        values.put(3, minIndex);
        for (int i = 0; i < maxima.size(); i++) {
            if (maxima.get(i) == true) {
                if (iqrProfile.get(i) > iqrProfile.get(values.get(1))) {
                    values.put(3, values.get(2));
                    values.put(2, values.get(1));
                    values.put(1, i);
                } else {
                    if (iqrProfile.get(i) > iqrProfile.get(values.get(2))) {
                        values.put(3, values.get(2));
                        values.put(2, i);
                    } else {
                        if (iqrProfile.get(i) > iqrProfile.get(values.get(3))) {
                            values.put(3, i);
                        }
                    }

                }
            }
        }

        for (int i : values.keySet()) {
            result.add(values.get(i));
            // IJ.log(" Variable index "+values.get(i));
        }
        return result;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        // finest("\tReading profile collection");
        in.defaultReadObject();

        // Object obj = in.readObject();
        //
        // if (Boolean.TRUE.equals(obj)) {
        //
        // // this is using the BetterProfileAggregate
        // BetterProfileAggregate a = (BetterProfileAggregate) in.readObject();
        // aggregate = a;
        // } else {
        // // this is using the old ProfileAggregate
        // ProfileAggregate a = (ProfileAggregate) obj;
        // aggregate = new BetterProfileAggregate(a.length(), 10);
        // // Cannot convert because the information is not present
        //
        // }
        //
        // indexes = (Map<BorderTagObject, Integer>) in.readObject();
        // segments = (List<NucleusBorderSegment>) in.readObject();
        //// profileCache = (ProfileCache) in.readObject();

        Map<BorderTagObject, Integer> newIndexes = new HashMap<BorderTagObject, Integer>();

        Iterator<?> it = indexes.keySet().iterator();

        while (it.hasNext()) {
            Object tag = it.next();
            if (tag instanceof BorderTag) {
                LOGGER.fine("Deserialization has no BorderTagObject for " + tag.toString() + ", creating");

                newIndexes.put(new BorderTagObject((BorderTag) tag), indexes.get(tag));
            }

        }

        if (!newIndexes.isEmpty()) {
            indexes = newIndexes;
        }

        type = null;

        // finest("\tRead profile collection");
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        // finest("Writing profile collection");

        out.defaultWriteObject();
        // finest("Wrote profile collection");
    }

    public class ProfileCache implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Store the median profiles from the profile aggregate to save
         * calculation time with large datasets
         */
        private Map<BorderTagObject, Map<Double, Profile>> cache = new HashMap<BorderTagObject, Map<Double, Profile>>();

        public ProfileCache() {

        }

        /**
         * Store the given statistic
         * 
         * @param stat
         * @param scale
         * @param d
         */
        public void setProfile(Tag tag, Double quartile, IProfile profile) {

            Map<Double, Profile> map;

            if (cache.containsKey(tag)) {

                map = cache.get(tag);

            } else {

                map = new HashMap<Double, Profile>();

                if (tag instanceof BorderTagObject) {
                    cache.put((BorderTagObject) tag, map);
                }

            }

            if (profile instanceof Profile) {

                map.put(quartile, (Profile) profile);
            } else {
                LOGGER.warning("Cannot cast IProfile to Profile in profile cache");
            }

        }

        /**
         * Get the given profile from the cache, or null if not present
         * 
         * @param tag
         * @param quartile
         * @return
         */
        public Profile getProfile(Tag tag, Double quartile) {

            if (this.hasProfile(tag, quartile)) {
                // IJ.log("Found "+tag+" - "+quartile+" in profile cache");
                return cache.get(tag).get(quartile);

            } else {

                return null;

            }
        }

        /**
         * Check if the given profile is in the cache
         * 
         * @param tag
         * @param quartile
         * @return
         */
        public boolean hasProfile(Tag tag, Double quartile) {
            Map<Double, Profile> map;

            if (cache.containsKey(tag)) {

                map = cache.get(tag);

            } else {

                return false;

            }

            if (map.containsKey(quartile)) {

                return true;
            } else {
                return false;
            }

        }

        /**
         * Empty the cache - all values must be recalculated
         */
        public void clear() {
            // IJ.log("Clearing cache");
            cache = null;
            cache = new HashMap<BorderTagObject, Map<Double, Profile>>();

        }

        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            // finest("\tReading profile cache");

            in.defaultReadObject();

            Map<BorderTagObject, Map<Double, Profile>> newCache = new HashMap<BorderTagObject, Map<Double, Profile>>();

            Iterator<?> it = cache.keySet().iterator();

            while (it.hasNext()) {
                Object tag = it.next();
                if (tag instanceof BorderTag) {
                    LOGGER.fine("Deserialization has no BorderTagObject for " + tag.toString() + ", creating");

                    newCache.put(new BorderTagObject((BorderTag) tag), cache.get(tag));
                }

            }

            if (!newCache.isEmpty()) {
                cache = newCache;
            }

            // finest("\tRead profile cache");
        }

        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            // finest("\tWriting profile cache");
            out.defaultWriteObject();
            // finest("\tWrote profile cache");
        }

    }

    @Override
    public IProfile getProfile(ProfileType type, Tag tag, double quartile) throws UnavailableBorderTagException {
        if (this.type == null) {
            this.type = type;
        }
        return getProfile(tag, quartile);
    }

    @Override
    public ISegmentedProfile getSegmentedProfile(ProfileType type, Tag tag, double quartile)
            throws UnavailableBorderTagException, ProfileException {
        if (this.type == null) {
            this.type = type;
        }
        return getSegmentedProfile(tag);
    }

    @Override
    public List<UUID> getSegmentIDs() {
        List<UUID> result = new ArrayList<UUID>(segments.size());
        for (IBorderSegment seg : segments) {
            result.add(seg.getID());
        }
        return result;
    }

    @Override
    public IBorderSegment getSegmentAt(Tag tag, int position) throws ProfileException {
        return this.getSegments(tag).get(position);
    }

    @Override
    public void createAndRestoreProfileAggregate(ICellCollection collection) {

        if (segments == null) {
            createProfileAggregate(collection, collection.getMedianArrayLength());
        } else {
            int length = segments.get(0).getProfileLength();
            createProfileAggregate(collection, length);
        }
    }

    @Override
    public IProfile getIQRProfile(ProfileType type, Tag tag) throws UnavailableBorderTagException {
        if (this.type == null) {
            this.type = type;
        }
        return getIQRProfile(tag);
    }

    @Override
    public List<Integer> findMostVariableRegions(ProfileType type, Tag tag) throws UnavailableBorderTagException {
        if (this.type == null) {
            this.type = type;
        }
        return findMostVariableRegions(tag);
    }

    @Override
    public double[] getValuesAtPosition(ProfileType type, double position) {
        if (this.type == null) {
            this.type = type;
        }
        double[] result = null;
        try {
            result = this.getAggregate().getValuesAtPosition(position);
        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, e.getMessage(), e);
        }

        return result;
    }
    
	@Override
	public double getProportionOfIndex(int index) {
		if (index < 0 || index >= aggregate.length())
            throw new IllegalArgumentException("Index out of bounds: " + index);
        return (double) index / (double) aggregate.length();
	}

	@Override
	public double getProportionOfIndex(@NonNull Tag tag) throws UnavailableBorderTagException {
		return getProportionOfIndex(getIndex(tag));
	}

	@Override
	public int getIndexOfProportion(double proportion) {
		if (proportion < 0 || proportion > 1)
			throw new IllegalArgumentException("Proportion must be between 0-1: " + proportion);
		if(proportion==0)
			return 0;
		if(proportion==1)
			return aggregate.length()-1;
		
		double desiredDistanceFromStart = (double) aggregate.length() * proportion;
		int target = (int) desiredDistanceFromStart;
		return target;
	}
}
