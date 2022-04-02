///*******************************************************************************
// * Copyright (C) 2018 Ben Skinner
// * 
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// * 
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// * 
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// ******************************************************************************/
//package com.bmskinner.nuclear_morphology.components.profiles;
//
//import java.util.ArrayList;
//import java.util.EnumMap;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Objects;
//import java.util.UUID;
//import java.util.logging.Logger;
//
//import org.eclipse.jdt.annotation.NonNull;
//import org.jdom2.Element;
//
//import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
//import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
//import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
//import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
//import com.bmskinner.nuclear_morphology.io.XmlSerializable;
//import com.bmskinner.nuclear_morphology.logging.Loggable;
//import com.bmskinner.nuclear_morphology.stats.Stats;
//
///**
// * Holds the ProfileAggregate with individual nucleus values, and stores the
// * indexes of landmarks within the profile. Provides methods to get the median
// * and other quartiles from the collection. The Reference Point is always at the
// * zero index of the collection.
// *
// * Also stores the NucleusBorderSegments within the profile, and provides
// * methods to transform the segments to fit profiles offset from given border
// * tags.
// *
// * An internal ProfileCache holds average profiles to save repeated calculation
// * 
// * @author ben
// * @since 1.13.3
// *
// */
//public class DefaultProfileCollection implements IProfileCollection {
//	
//	private static final Logger LOGGER = Logger.getLogger(DefaultProfileCollection.class.getName());
//
//    /** indexes of landmarks in the median profile with RP at zero */
//    private Map<Landmark, Integer> indexes  = new HashMap<>();
//    
//    /** segments in the median profile with RP at zero */
//    private List<IProfileSegment> segments = new ArrayList<>();
//
//    /** length of the median profile */
//    private int length;
//            
//    /** cached median profiles for quicker access */
//    private ProfileCache cache = new ProfileCache();
//
//    /**
//     * Create an empty profile collection. The RP is set to the zero index by default.
//     */
//    public DefaultProfileCollection() {
//        indexes.put(Landmark.REFERENCE_POINT, ZERO_INDEX);
//    }
//        
//    /**
//     * Construct from an XML element. Use for 
//     * unmarshalling. The element should conform
//     * to the specification in {@link XmlSerializable}.
//     * @param e the XML element containing the data.
//     */
//    public DefaultProfileCollection(Element e) {
//    	
//    	for(Element el : e.getChildren("Landmark")){
//    		indexes.put(Landmark.of(el.getAttribute("name").getValue(), 
//					LandmarkType.valueOf(el.getAttribute("type").getValue())), 
//					Integer.parseInt(el.getText()));
//		}
//		
//		for(Element el : e.getChildren("Segment")){
//			segments.add(new DefaultProfileSegment(el));
//		}
//		
//		
//		
//		if(!segments.isEmpty())
//			length = segments.get(0).getProfileLength();
//    }
//    
//    /**
//     * Used for duplicating
//     * @param p
//     */
//    private DefaultProfileCollection(DefaultProfileCollection p) {
//    	for(Landmark t : p.indexes.keySet())
//			indexes.put(t, p.indexes.get(t));
//    	
//    	for(IProfileSegment s : p.segments)
//			segments.add(s.copy());
//    	
//    	length = p.length;
//    	
//    	cache = p.cache.duplicate();
//    }
//
//	@Override
//	public IProfileCollection duplicate() {
//		return new DefaultProfileCollection(this);
//	}
//    
//    @Override
//    public int segmentCount() {
//    	if(segments==null)
//    		return 0;
//    	return segments.size();
//    }
//    
//    @Override
//    public boolean hasSegments() {
//    	return segmentCount()>0;
//    }
//
//    @Override
//    public int getIndex(@NonNull Landmark pointType) throws MissingLandmarkException {
//        if(indexes.containsKey(pointType))
//            return indexes.get(pointType);
//		throw new MissingLandmarkException(pointType+" is not present in this profile collection");
//    }
//
//    @Override
//    public List<Landmark> getLandmarks() {
//        List<Landmark> result = new ArrayList<>();
//        for (Landmark s : indexes.keySet()) {
//            result.add(s);
//        }
//        return result;
//    }
//
//    @Override
//    public boolean hasLandmark(@NonNull Landmark tag) {
//        return indexes.keySet().contains(tag);
//    }
//
//    @Override
//    public IProfile getProfile(@NonNull ProfileType type, @NonNull Landmark tag, double quartile, ICellCollection c)
//            throws MissingLandmarkException, ProfileException, MissingProfileException {
//        if (!this.hasLandmark(tag))
//            throw new MissingLandmarkException("Tag is not present: " + tag.toString());
//
//        if(!cache.hasProfile(type, quartile, tag)) {
//        	IProfileAggregate agg = createProfileAggregate(collection);
//        	
//        	IProfile p = agg.getQuartile(quartile);
//            int offset = indexes.get(tag);
//            p = p.startFrom(offset);
//            cache.addProfile(type, quartile, tag, p);
//        }
//        	
//        return cache.getProfile(type, quartile, tag);
//    }
//
//    @Override
//    public ISegmentedProfile getSegmentedProfile(@NonNull ProfileType type, @NonNull Landmark tag, 
//    		double quartile)
//            throws MissingLandmarkException, ProfileException, MissingProfileException {
//
//        if (quartile < 0 || quartile > 100)
//            throw new IllegalArgumentException("Quartile must be between 0-100");
//
//        // get the profile array
//        IProfile p = getProfile(type, tag, quartile);
//        if (segments.isEmpty())
//        	throw new UnsegmentedProfileException("No segments assigned to profile collection");
//
//        try {
//            return new DefaultSegmentedProfile(p, getSegments(tag));
//        } catch (IndexOutOfBoundsException e) {
//            LOGGER.log(Loggable.STACK, "Cannot create segmented profile due to segment/profile mismatch", e);
//            throw new ProfileException("Cannot create segmented profile; segment lengths do not match array", e);
//        }
//    }
//    
//    @Override
//    public void updateCache(@NonNull ICellCollection collection) {
//    	
//    }
//
//
//    @Override
//    public synchronized List<UUID> getSegmentIDs() {
//        List<UUID> result = new ArrayList<>();
//        if (segments == null)
//            return result;
//        for (IProfileSegment seg : this.segments) {
//            result.add(seg.getID());
//        }
//        return result;
//    }
//
//    @Override
//    public synchronized IProfileSegment getSegmentAt(@NonNull Landmark tag, int position) throws MissingLandmarkException {
//        return this.getSegments(tag).get(position);
//    }
//
//    @Override
//    public int length() {
//        return length;
//    }
//
//
//    @Override
//    public synchronized List<IProfileSegment> getSegments(@NonNull Landmark tag) throws MissingLandmarkException {
//
//        // this must be negative offset for segments
//        // since we are moving the pointIndex back to the beginning
//        // of the array
//        int offset = -getIndex(tag);
//        
//        List<IProfileSegment> result = new ArrayList<>();        
//        
//        for(IProfileSegment s : segments) {
//        	result.add(s.copy().offset(offset));
//        }
//        
//        try {
//        	IProfileSegment.linkSegments(result);
//        	return result;
//        } catch (ProfileException e) {
//        	LOGGER.log(Loggable.STACK, "Could not get segments from "+tag, e);
//        	e.printStackTrace();
//        	 return new ArrayList<>();     
//        }
//    }
//
//
//    @Override
//    public boolean hasSegmentStartingWith(@NonNull Landmark tag) throws UnsegmentedProfileException, MissingLandmarkException {
//    	return getSegmentStartingWith(tag) != null;
//    }
//
//    @Override
//    public IProfileSegment getSegmentStartingWith(@NonNull Landmark tag) throws UnsegmentedProfileException, MissingLandmarkException {
//        List<IProfileSegment> segments = this.getSegments(tag);
//
//        if (segments.size() == 0) {
//            throw new UnsegmentedProfileException("No segments assigned to profile collection");
//        }
//
//        IProfileSegment result = null;
//        // get the name of the segment with the tag at the start
//        for (IProfileSegment seg : segments) {
//            if (seg.getStartIndex() == ZERO_INDEX) {
//                result = seg;
//            }
//        }
//
//        return result;
//    }
//
//
//    @Override
//    public boolean hasSegmentEndingWith(@NonNull Landmark tag) throws UnsegmentedProfileException, MissingLandmarkException {
//    	return getSegmentEndingWith(tag) != null;
//    }
//
//
//    @Override
//    public IProfileSegment getSegmentEndingWith(@NonNull Landmark tag) throws UnsegmentedProfileException, MissingLandmarkException {
//        List<IProfileSegment> segments = this.getSegments(tag);
//
//        if (segments.isEmpty())
//            throw new UnsegmentedProfileException("No segments assigned to profile collection");
//
//        IProfileSegment result = null;
//        // get the name of the segment with the tag at the start
//        for (IProfileSegment seg : segments) {
//
//            if (seg.getEndIndex() == ZERO_INDEX) {
//                result = seg;
//            }
//        }
//
//        return result;
//    }
//
//
//    @Override
//    public IProfileSegment getSegmentContaining(int index) throws UnsegmentedProfileException, MissingLandmarkException {
//        List<IProfileSegment> segments = this.getSegments(Landmark.REFERENCE_POINT);
//
//        if (segments.isEmpty()) 
//            throw new UnsegmentedProfileException("No segments assigned to profile collection");
//
//        IProfileSegment result = null;
//        for (IProfileSegment seg : segments) {
//
//            if (seg.contains(index))
//            	return seg;
//        }
//
//        return result;
//    }
//
//
//    @Override
//    public IProfileSegment getSegmentContaining(@NonNull Landmark tag) throws ProfileException, MissingLandmarkException {
//        List<IProfileSegment> segments = this.getSegments(tag);
//
//        IProfileSegment result = null;
//        for (IProfileSegment seg : segments) {
//            if (seg.contains(ZERO_INDEX)) 
//                return seg;
//        }
//
//        return result;
//    }
//
//    @Override
//    public void setLandmark(@NonNull Landmark tag, int newIndex) {
//        // Cannot move the RP from zero
//        if (tag.equals(Landmark.REFERENCE_POINT))
//            return;
//        cache.remove(tag);
//        indexes.put(tag, newIndex);
//
//    }
//
//    @Override
//    public void addSegments(@NonNull List<IProfileSegment> n) throws MissingLandmarkException {
//        if (n.isEmpty())
//            throw new IllegalArgumentException("String or segment list is empty");
//
//        if (this.length() != n.get(0).getProfileLength())
//        	throw new IllegalArgumentException(String.format("Segment profile length (%d) does not fit aggregate length (%d)", n.get(0).getProfileLength(), length()));
//
//        /*
//         * The segments coming in are zeroed to the given pointType pointIndex
//         * This means the indexes must be moved forwards appropriately. Hence,
//         * add a positive offset.
//         */
//        int offset = getIndex(Landmark.REFERENCE_POINT);
//
//    	for(IProfileSegment s : n) {
//    		s.offset(offset);
//    	}
//    	
//    	
//    	segments = new ArrayList<>();
//    	for(IProfileSegment s : n) {
//    		segments.add(s.copy());
//    	}
//    }
//
//    private IProfileAggregate createProfileAggregate(@NonNull ICellCollection collection, int length) throws ProfileException, MissingLandmarkException, MissingProfileException {
//        if (length <= 0)
//            throw new IllegalArgumentException("Requested profile aggregate length is zero or negative");
//        if (collection.isEmpty())
//            throw new IllegalArgumentException("Cell collection is empty");
//        
//        this.length = length;
//        cache.clear();
//        
//        // If there are segments, not just the default segment, and the segment 
//        // profile length is different to the required length. Interpolation needed.
//        if (segments.size()>1 && length != segments.get(0).getProfileLength()) {
//        	LOGGER.fine("Segments already exist, interpolating");
//        	return createProfileAggregateOfDifferentLength(collection, length);
//        }
//        
//        // No current segments are present. Make a default segment spanning the entire profile
//        if(segments.isEmpty()) {
//        	segments.add(new DefaultProfileSegment(0, 0, length, IProfileCollection.DEFAULT_SEGMENT_ID));
//        }
//        
//        
//        for (ProfileType type : ProfileType.values()) {
//            IProfileAggregate agg = new DefaultProfileAggregate(length, collection.size());
//
//            profileMap.put(type, agg);
//            for (Nucleus n : collection.getNuclei()) {
//            	agg.addValues(n.getProfile(type, Landmark.REFERENCE_POINT));
//
//            }
//        }
//
//    }
//    
//    /**
//     * Allow a profile aggregate to be created and segments copied when median profile lengths have
//     * changed.
//     * @param collection
//     * @param length
//     */
//    private IProfileAggregate createProfileAggregateOfDifferentLength(@NonNull ICellCollection collection, int length) throws ProfileException {
//    	indexes.put(Landmark.REFERENCE_POINT, ZERO_INDEX);
//    	try {
//    		    		
//    		// Copy any existing segments, adjusting the lengths using profile interpolation
//    		
//    		List<IProfileSegment> interpolatedSegments;
//    		if(!profileMap.isEmpty()) {
//    			ISegmentedProfile sourceMedian = getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);
//    			interpolatedSegments = sourceMedian.interpolate(length).getSegments();
//    		} else {
//    			
//    			// If the map of profile type to aggregate was empty - such as on deserialisation - 
//    			// we have no profile to use to interpolate segments.
//        		// Create an arbitrary profile with the original length.
//    			
//    			List<IProfileSegment> originalSegList = new ArrayList<>();
//    			for(IProfileSegment s : segments)
//    				originalSegList.add(s);
//        		IProfile template = new DefaultProfile(0, segments.get(0).getProfileLength());
//        		ISegmentedProfile segTemplate = new DefaultSegmentedProfile(template, originalSegList);
//        		
//        		// Now use the interpolation method to adjust the segment lengths
//        		interpolatedSegments = segTemplate.interpolate(length).getSegments();
//    		}
//    		
//    		for (ProfileType type : ProfileType.values()) {
//
//                IProfileAggregate agg = new DefaultProfileAggregate(length, collection.size());
//                try {
//                    for (Nucleus n : collection.getNuclei())
//                         agg.addValues(n.getProfile(type, Landmark.REFERENCE_POINT));
//                } catch (ProfileException | MissingLandmarkException | MissingProfileException e) {
//                    LOGGER.log(Loggable.STACK, "Error making aggregate", e);
//                }
////                profileMap.put(type, agg);
//            } 	    		
//    		
//    		addSegments(interpolatedSegments);
//    	} catch(Exception e) {
//    		LOGGER.log(Loggable.STACK, e.getMessage(), e);
//    		throw new ProfileException(e);
//    	}
//    }
//
//
//    public void createProfileAggregate(@NonNull ICellCollection collection) throws ProfileException, MissingLandmarkException, MissingProfileException {
//        createProfileAggregate(collection, collection.getMedianArrayLength());
//    }
//
//    @Override
//    public String toString() {
//    	StringBuilder builder = new StringBuilder("Length: "+this.length+"\n");
//    	builder.append("\tPoint types:\r\n");
//    	for (Entry<Landmark, Integer> e : indexes.entrySet()) {
//    		builder.append("\t" + e.getKey() + ": " + e.getValue()+"\r\n");
//    	}
//
//    	try {
//
//    		for (Landmark tag : this.indexes.keySet()) {
//    			if (tag.type()
//    					.equals(LandmarkType.CORE)) {
//    				builder.append("\r\nSegments from " + tag + ":\r\n");
//    				for (IProfileSegment s : this.getSegments(tag)) {
//    					builder.append(s.toString() + "\r\n");
//    				}
//    			}
//    		}
//
//    	} catch (Exception e) {
//    		builder.append("\r\nError fetching segments");
//    	}
//
//    	return builder.toString();
//
//    }
//
//    @Override
//    public IProfile getIQRProfile(@NonNull ProfileType type, @NonNull Landmark tag)
//            throws MissingLandmarkException, ProfileException, MissingProfileException {
//
//        IProfile q25 = getProfile(type, tag, Stats.LOWER_QUARTILE);
//        IProfile q75 = getProfile(type, tag, Stats.UPPER_QUARTILE);
//
//        if (q25 == null || q75 == null) { // if something goes wrong, return a
//                                          // zero profile
//
//            LOGGER.warning("Problem calculating the IQR - setting to zero");
//            return new DefaultProfile(0, length);
//        }
//
//        return q75.subtract(q25);
//    }
//
//    @Override
//	public int hashCode() {
//		return Objects.hash(indexes, segments);
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		DefaultProfileCollection other = (DefaultProfileCollection) obj;
//		return Objects.equals(indexes, other.indexes) && Objects.equals(segments, other.segments);
//	}
//
//	@Override
//	public Element toXmlElement() {
//		Element e = new Element("ProfileCollection");
//				
//		for(IProfileSegment s : segments) {
//			e.addContent(s.toXmlElement());
//		}
//		
//		for(Entry<Landmark, Integer> entry : indexes.entrySet()) {
//			e.addContent(new Element("Landmark")
//					.setAttribute("name", entry.getKey().toString())
//					.setAttribute("type", entry.getKey().type().toString())
//					.setText(String.valueOf(entry.getValue())));
//		}
//		
//		return e;
//	}
//
//	/**
//     * The cache for profiles
//     * 
//     * @author bms41
//     * @since 1.13.4
//     *
//     */
//    private class ProfileCache {
//
//        /**
//         * The key used to store values in the cache
//         * 
//         * @author bms41
//         * @since 1.13.4
//         *
//         */
//        private class ProfileKey {
//            private final ProfileType type;
//            private final double      quartile;
//            private final Landmark         tag;
//
//            public ProfileKey(final ProfileType type, final double quartile, final Landmark tag) {
//
//                this.type = type;
//                this.quartile = quartile;
//                this.tag = tag;
//            }
//
//            public boolean has(ProfileType t) {
//                return type.equals(t);
//            }
//
//            public boolean has(Landmark t) {
//                return tag.equals(t);
//            }
//
//            @Override
//            public int hashCode() {
//                final int prime = 31;
//                int result = 1;
//                result = prime * result + getOuterType().hashCode();
//                long temp;
//                temp = Double.doubleToLongBits(quartile);
//                result = prime * result + (int) (temp ^ (temp >>> 32));
//                result = prime * result + ((tag == null) ? 0 : tag.hashCode());
//                result = prime * result + ((type == null) ? 0 : type.hashCode());
//                return result;
//            }
//
//            @Override
//            public boolean equals(Object obj) {
//                if (this == obj)
//                    return true;
//                if (obj == null)
//                    return false;
//                if (getClass() != obj.getClass())
//                    return false;
//                ProfileKey other = (ProfileKey) obj;
//                if (!getOuterType().equals(other.getOuterType()))
//                    return false;
//                if (Double.doubleToLongBits(quartile) != Double.doubleToLongBits(other.quartile))
//                    return false;
//                if (tag == null) {
//                    if (other.tag != null)
//                        return false;
//                } else if (!tag.equals(other.tag))
//                    return false;
//                if (type != other.type)
//                    return false;
//                return true;
//            }
//
//            private DefaultProfileCollection getOuterType() {
//                return DefaultProfileCollection.this;
//            }
//
//        }
//
//        private Map<ProfileKey, IProfile> map = new HashMap<>();
//
//        public ProfileCache() {
//        }
//        
//        public ProfileCache duplicate() {
//        	ProfileCache result = new ProfileCache();
//        	try {
//        		for(ProfileKey k : map.keySet()) {
//        			IProfile p = map.get(k);
//        			if(p!=null)
//        				result.map.put(k, p.copy());
//        		}
//        	} catch(ProfileException e) {
//
//        	}
//        	return result;
//        }
//
//        /**
//         * Add a profile with the given keys
//         * 
//         * @param type the profile type
//         * @param quartile the quartile of the dataset
//         * @param tag the tag
//         * @param profile the profile to save
//         */
//        public void addProfile(final ProfileType type, final double quartile, final Landmark tag, IProfile profile) {
//            ProfileKey key = new ProfileKey(type, quartile, tag);
//            map.put(key, profile);
//        }
//
//        public boolean hasProfile(final ProfileType type, final double quartile, final Landmark tag) {
//            ProfileKey key = new ProfileKey(type, quartile, tag);
//            return map.containsKey(key);
//        }
//
//        public IProfile getProfile(final ProfileType type, final double quartile, final Landmark tag) {
//            ProfileKey key = new ProfileKey(type, quartile, tag);
//            return map.get(key);
//        }
//
//        public void remove(final ProfileType type, final double quartile, final Landmark tag) {
//            ProfileKey key = new ProfileKey(type, quartile, tag);
//            map.remove(key);
//        }
//        
//        /**
//         * Remove all profiles from the cache
//         */
//        public void clear() {
//        	map.clear();
//        }
//
//        public void remove(final Landmark t) {
//
//            Iterator<ProfileKey> it = map.keySet().iterator();
//            while (it.hasNext()) {
//                ProfileKey k = it.next();
//                if (k.has(t)) {
//                    it.remove();
//                }
//            }
//
//        }
//    }
//
//	@Override
//	public double getProportionOfIndex(int index) {
//		if (index < 0 || index >= length)
//            throw new IllegalArgumentException("Index out of bounds: " + index);
//		if(index==0)
//			return 0;
//		if(index==length-1)
//			return 1;
//        return (double) index / (double) (length-1);
//	}
//
//	@Override
//	public double getProportionOfIndex(@NonNull Landmark tag) throws MissingLandmarkException {
//		return getProportionOfIndex(getIndex(tag));
//	}
//
//	@Override
//	public int getIndexOfProportion(double proportion) {
//		if (proportion < 0 || proportion > 1)
//			throw new IllegalArgumentException("Proportion must be between 0-1: " + proportion);
//		if(proportion==0)
//			return 0;
//		if(proportion==1)
//			return length-1;
//		
//		double desiredDistanceFromStart = (double) length * proportion;
//		int target = (int) desiredDistanceFromStart;
//		return target;
//	}
//}
