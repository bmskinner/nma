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


package com.bmskinner.nuclear_morphology.components.generic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
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
 * @since 1.13.3
 *
 */
public class DefaultProfileCollection implements IProfileCollection {

    private static final long serialVersionUID = 1L;

    private Map<Tag, Integer> indexes  = new HashMap<>(); // indexes of tags in the profile. Assumes the RP is at zero.
    private IBorderSegment[]  segments = null;

    private transient int                                 length;
    private transient Map<ProfileType, IProfileAggregate> map   = new HashMap<>(); // the aggregates for each profile type
    private transient ProfileCache                        cache = new ProfileCache(); // cached median profiles etc

    /**
     * Create an empty profile collection. The RP is set to the zero index by default.
     */
    public DefaultProfileCollection() {
        indexes.put(Tag.REFERENCE_POINT, ZERO_INDEX);
    }
    
    @Override
    public int segmentCount() {
    	if(segments==null)
    		return 0;
    	return segments.length;
    }
    
    @Override
    public boolean hasSegments() {
    	return segmentCount()>0;
    }

    @Override
    public int getIndex(@NonNull Tag pointType) {
        if (pointType == null)
            throw new IllegalArgumentException("The requested offset key is null: " + pointType);
        if (indexes.containsKey(pointType))
            return indexes.get(pointType);
		return -1;
    }

    @Override
    public List<Tag> getBorderTags() {
        List<Tag> result = new ArrayList<Tag>();
        for (Tag s : indexes.keySet()) {
            result.add(s);
        }
        return result;
    }

    @Override
    public boolean hasBorderTag(@NonNull Tag tag) {
        return indexes.keySet().contains(tag);
    }

    @Override
    public IProfile getProfile(@NonNull ProfileType type, @NonNull Tag tag, double quartile)
            throws UnavailableBorderTagException, ProfileException, UnavailableProfileTypeException {

        if (type == null)
            throw new IllegalArgumentException("Type cannot be null");
        if (tag == null)
            throw new IllegalArgumentException("Tag cannot be null");
        if (!this.hasBorderTag(tag))
            throw new UnavailableBorderTagException("Tag is not present: " + tag.toString());
        if (!map.containsKey(type))
            throw new UnavailableProfileTypeException("Profile type is not present: " + type.toString());

        if(!cache.hasProfile(type, quartile, tag)) {
        	IProfileAggregate agg = map.get(type);
        	IProfile p = agg.getQuartile(quartile);
            int offset = indexes.get(tag);
            p = p.offset(offset);
            cache.addProfile(type, quartile, tag, p);
        }
        	
        return cache.getProfile(type, quartile, tag);
    }

    @Override
    public ISegmentedProfile getSegmentedProfile(@NonNull ProfileType type, @NonNull Tag tag, double quartile)
            throws UnavailableBorderTagException, ProfileException, UnavailableProfileTypeException,
            UnsegmentedProfileException {

        if (tag == null || type == null)
            throw new IllegalArgumentException("A profile type and tag is required");
        if (quartile < 0 || quartile > 100)
            throw new IllegalArgumentException("Quartile must be between 0-100");

        // get the profile array
        IProfile p = getProfile(type, tag, quartile);
        if (segments[0] == null)
        	throw new UnsegmentedProfileException("No segments assigned to profile collection");

        try {
            return new SegmentedFloatProfile(p, getSegments(tag));
        } catch (IndexOutOfBoundsException e) {
            stack("Cannot create segmented profile due to segment/profile mismatch", e);
            throw new ProfileException("Cannot create segmented profile; segment lengths do not match array", e);
        }
    }


    @Override
    public synchronized List<UUID> getSegmentIDs() {
        List<UUID> result = new ArrayList<>();
        if (segments == null)
            return result;
        for (IBorderSegment seg : this.segments) {
            result.add(seg.getID());
        }
        return result;
    }

    @Override
    public synchronized IBorderSegment getSegmentAt(@NonNull Tag tag, int position) {
        return this.getSegments(tag).get(position);
    }

    @Override
    public int length() {
        return length;
    }


    @Override
    public synchronized List<IBorderSegment> getSegments(@NonNull Tag tag) {
        if (tag == null)
            throw new IllegalArgumentException("The requested segment key is null: " + tag);

        // this must be negative offset for segments
        // since we are moving the pointIndex back to the beginning
        // of the array
        int offset = -getIndex(tag);
        
        System.out.println(tag+" offset is "+offset);
        List<IBorderSegment> result = new ArrayList<>();        
        
        for(IBorderSegment s : segments) {
        	
        	IBorderSegment sc = s.copy(); 
        	sc.offset(offset);
        	result.add(sc);
        }
        
        try {
        	IBorderSegment.linkSegments(result);
        	System.out.println("Found "+result.size()+" segments after linking");
        	return result;

//        try {
//        	IBorderSegment[] result = IBorderSegment.copy(segments);
//        	for(IBorderSegment s : result) {
//        		s.offset(offset);
//        	}
//        	return Arrays.asList(result);
        } catch (ProfileException e) {
        	error("Could not get segments from "+tag, e);
        	e.printStackTrace();
        	 return new ArrayList<>();     
        }
     
    }


    @Override
    public boolean hasSegmentStartingWith(@NonNull Tag tag) throws UnsegmentedProfileException {
    	return getSegmentStartingWith(tag) != null;
    }

    @Override
    public IBorderSegment getSegmentStartingWith(@NonNull Tag tag) throws UnsegmentedProfileException {
        List<IBorderSegment> segments = this.getSegments(tag);

        if (segments.size() == 0) {
            throw new UnsegmentedProfileException("No segments assigned to profile collection");
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


    @Override
    public boolean hasSegmentEndingWith(@NonNull Tag tag) throws UnsegmentedProfileException {
    	return getSegmentEndingWith(tag) != null;
    }


    @Override
    public IBorderSegment getSegmentEndingWith(@NonNull Tag tag) throws UnsegmentedProfileException {
        List<IBorderSegment> segments = this.getSegments(tag);

        if (segments.size() == 0)
            throw new UnsegmentedProfileException("No segments assigned to profile collection");

        IBorderSegment result = null;
        // get the name of the segment with the tag at the start
        for (IBorderSegment seg : segments) {

            if (seg.getEndIndex() == ZERO_INDEX) {
                result = seg;
            }
        }

        return result;
    }


    @Override
    public IBorderSegment getSegmentContaining(int index) throws UnsegmentedProfileException {
        List<IBorderSegment> segments = this.getSegments(Tag.REFERENCE_POINT);

        if (segments.size() == 0) 
            throw new UnsegmentedProfileException("No segments assigned to profile collection");

        IBorderSegment result = null;
        for (IBorderSegment seg : segments) {

            if (seg.contains(index))
            	return seg;
        }

        return result;
    }


    @Override
    public IBorderSegment getSegmentContaining(@NonNull Tag tag) throws ProfileException {
        List<IBorderSegment> segments = this.getSegments(tag);

        IBorderSegment result = null;
        for (IBorderSegment seg : segments) {
            if (seg.contains(ZERO_INDEX)) 
                return seg;
        }

        return result;
    }

    @Override
    public void addIndex(@NonNull Tag tag, int offset) {
        if (tag == null)
            throw new IllegalArgumentException("BorderTagObject is null");

        // Cannot move the RP from zero
        if (tag.equals(Tag.REFERENCE_POINT))
            return;
        cache.remove(tag);
        indexes.put(tag, offset);

    }

    @Override
    public void addSegments(@NonNull List<IBorderSegment> n) {
        if (n == null || n.isEmpty())
            throw new IllegalArgumentException("Segment list is null or empty");

        if (this.length() != n.get(0).getProfileLength())
            throw new IllegalArgumentException(String.format("Segment profile length (%d) does not fit aggregate length (%d)", n.get(0).getProfileLength(), length()));

        this.segments = new IBorderSegment[n.size()];

        for (int i = 0; i < segments.length; i++) {
            segments[i] = n.get(i);
        }
    }

    @Override
    public void addSegments(@NonNull Tag tag, @NonNull List<IBorderSegment> n) {
        if (n == null || n.isEmpty())
            throw new IllegalArgumentException("String or segment list is null or empty");

        if (this.length() != n.get(0).getProfileLength())
        	throw new IllegalArgumentException(String.format("Segment profile length (%d) does not fit aggregate length (%d)", n.get(0).getProfileLength(), length()));


        /*
         * The segments coming in are zeroed to the given pointType pointIndex
         * This means the indexes must be moved forwards appropriately. Hence,
         * add a positive offset.
         */
        int offset = getIndex(tag);


    	for(IBorderSegment s : n) {
    		s.offset(offset);
    	}
    	
//        List<IBorderSegment> result;
//        try {
//            result = IBorderSegment.nudge(n, offset);
//        } catch (ProfileException e) {
//            stack("Error offsetting segments", e);
//            return;
//        }

        this.segments = new IBorderSegment[n.size()];

        for (int i = 0; i < segments.length; i++) {
            segments[i] = n.get(i);
        }
    }

    @Override
    public double[] getValuesAtPosition(@NonNull ProfileType type, double position) throws UnavailableProfileTypeException {

        double[] result = map.get(type).getValuesAtPosition(position);

        if (result == null) {

            result = new double[length()];
            for (int i = 0; i < result.length; i++) {
                result[i] = 0;
            }
        }

        return result;
    }

    @Override
    public void createProfileAggregate(@NonNull ICellCollection collection, int length) throws ProfileException {
        if (length <= 0)
            throw new IllegalArgumentException("Requested profile aggregate length is zero or negative");
        if (collection.size() == 0)
            throw new IllegalArgumentException("Cell collection is empty");

        this.length = length;
        if (segments != null && length != segments[0].getProfileLength()) 
            throw new ProfileException("Creating profile aggregate will invalidate segments");
        
        if(segments==null) {
        	segments = new IBorderSegment[1];
        	segments[0] = new DefaultBorderSegment(0, 0, length, IProfileCollection.DEFAULT_SEGMENT_ID);
        }
        
        for (ProfileType type : ProfileType.values()) {

            IProfileAggregate agg = new DefaultProfileAggregate(length, collection.size());

            map.put(type, agg);
            try {
                for (Nucleus n : collection.getNuclei()) {
                    switch (type) {
                    case FRANKEN: agg.addValues(n.getProfile(type));
                        break;
                    default: agg.addValues(n.getProfile(type, Tag.REFERENCE_POINT));
                        break;
                    }
                }
            } catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
                stack("Error making aggregate", e);
            }
        }
        cache.clear();
    }


    public void createProfileAggregate(@NonNull ICellCollection collection) throws ProfileException {
        createProfileAggregate(collection, collection.getMedianArrayLength());
    }

    @Override
    public void createAndRestoreProfileAggregate(@NonNull ICellCollection collection) throws ProfileException {

        if (segments == null) {
            createProfileAggregate(collection, collection.getMedianArrayLength());
        } else {
            int length = segments[0].getProfileLength();
            createProfileAggregate(collection, length);
        }
    }

    @Override
    public String tagString() {

        StringBuilder builder = new StringBuilder();

        builder.append("\tPoint types:");
        for (Tag tag : this.indexes.keySet()) {
            builder.append("\t" + tag + ": " + this.indexes.get(tag));
        }
        return builder.toString();
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.tagString());

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

        return builder.toString();

    }

    @Override
    public IProfile getIQRProfile(@NonNull ProfileType type, @NonNull Tag tag)
            throws UnavailableBorderTagException, ProfileException, UnavailableProfileTypeException {

        IProfile q25 = getProfile(type, tag, Stats.LOWER_QUARTILE);
        IProfile q75 = getProfile(type, tag, Stats.UPPER_QUARTILE);

        if (q25 == null || q75 == null) { // if something goes wrong, return a
                                          // zero profile

            warn("Problem calculating the IQR - setting to zero");
            return new FloatProfile(0, length);
        }

        return q75.subtract(q25);
    }

    @Override
    public List<Integer> findMostVariableRegions(@NonNull ProfileType type, @NonNull Tag tag) {

        List<Integer> result = new ArrayList<Integer>(0);

        // get the IQR and maxima

        IProfile iqrProfile;
        try {
            iqrProfile = getIQRProfile(type, tag);
        } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException e) {
            stack("Error getting variable regions", e);
            return result;
        }

        BooleanProfile maxima = iqrProfile.smooth(3).getLocalMaxima(3);

        // given the list of maxima, find the highest 3 regions
        // store the rank (1-3) and the index of the position at this rank
        // To future me: I am sorry about this.
        Map<Integer, Integer> values = new HashMap<Integer, Integer>(0);

        int minIndex = -1;
        try {
            minIndex = iqrProfile.getIndexOfMin();
        } catch (ProfileException e) {
            stack("Error getting index", e);
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
        in.defaultReadObject();
        map = new HashMap<ProfileType, IProfileAggregate>();
        cache = new ProfileCache();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    /**
     * The cache for profiles
     * 
     * @author bms41
     * @since 1.13.4
     *
     */
    private class ProfileCache {

        /**
         * The key used to store values in the cache
         * 
         * @author bms41
         * @since 1.13.4
         *
         */
        private class ProfileKey {
            private final ProfileType type;
            private final double      quartile;
            private final Tag         tag;

            public ProfileKey(final ProfileType type, final double quartile, final Tag tag) {

                this.type = type;
                this.quartile = quartile;
                this.tag = tag;
            }

            public boolean has(ProfileType t) {
                return type.equals(t);
            }

            public boolean has(Tag t) {
                return tag.equals(t);
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + getOuterType().hashCode();
                long temp;
                temp = Double.doubleToLongBits(quartile);
                result = prime * result + (int) (temp ^ (temp >>> 32));
                result = prime * result + ((tag == null) ? 0 : tag.hashCode());
                result = prime * result + ((type == null) ? 0 : type.hashCode());
                return result;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                ProfileKey other = (ProfileKey) obj;
                if (!getOuterType().equals(other.getOuterType()))
                    return false;
                if (Double.doubleToLongBits(quartile) != Double.doubleToLongBits(other.quartile))
                    return false;
                if (tag == null) {
                    if (other.tag != null)
                        return false;
                } else if (!tag.equals(other.tag))
                    return false;
                if (type != other.type)
                    return false;
                return true;
            }

            private DefaultProfileCollection getOuterType() {
                return DefaultProfileCollection.this;
            }

        }

        private Map<ProfileKey, IProfile> map = new HashMap<ProfileKey, IProfile>();

        public ProfileCache() {
        }

        /**
         * Add a profile with the given keys
         * 
         * @param type
         *            the profile type
         * @param quartile
         *            the quartile of the dataset
         * @param tag
         *            the tag
         * @param profile
         *            the profile to save
         */
        public void addProfile(final ProfileType type, final double quartile, final Tag tag, IProfile profile) {
            ProfileKey key = new ProfileKey(type, quartile, tag);
            map.put(key, profile);
        }

        public boolean hasProfile(final ProfileType type, final double quartile, final Tag tag) {
            ProfileKey key = new ProfileKey(type, quartile, tag);
            return map.containsKey(key);
        }

        public IProfile getProfile(final ProfileType type, final double quartile, final Tag tag) {
            ProfileKey key = new ProfileKey(type, quartile, tag);
            return map.get(key);
        }

        public void remove(final ProfileType type, final double quartile, final Tag tag) {
            ProfileKey key = new ProfileKey(type, quartile, tag);
            map.remove(key);
        }
        
        /**
         * Remove all profiles from the cache
         */
        public void clear() {
        	map.clear();
        }

        public void remove(final Tag t) {

            Iterator<ProfileKey> it = map.keySet().iterator();
            while (it.hasNext()) {
                ProfileKey k = it.next();
                if (k.has(t)) {
                    it.remove();
                }
            }

        }
    }

}
