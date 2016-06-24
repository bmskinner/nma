/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package components.generic;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logging.Loggable;
import components.CellCollection;
import components.generic.BorderTag.BorderTagType;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
//import ij.IJ;

import utility.Constants;

public class ProfileCollection implements Serializable, Loggable {
		
	private static final long serialVersionUID = 1L;
	
	private static final int ZERO_INDEX = 0;
	
	private ProfileAggregate 	       aggregate = null;
	
	private Map<BorderTag, Integer>    offsets  = new HashMap<BorderTag, Integer>();
	private List<NucleusBorderSegment> segments = new ArrayList<NucleusBorderSegment>();
	
	
	private final ProfileCache profileCache           = new ProfileCache();


	/**
	 * Create an empty profile collection
	 */
	public ProfileCollection(){
		
	}
			
	/**
	 * Get the offset needed to transform a profile to start from the given 
	 * point type. Returns -1 if the border tag is not found
	 * @param pointType
	 * @return the offset or -1
	 */
	public int getOffset(BorderTag pointType){
		if(pointType==null){
			throw new IllegalArgumentException("The requested offset key is null: "+pointType);
		}
		if(offsets.containsKey(pointType)){	
			return offsets.get(pointType);
		} else {
			return -1;
//			throw new IllegalArgumentException("The requested offset key does not exist: "+pointType);
		}
	}
	
	/**
	 * Get all the offset keys attached to this profile collection
	 * @return
	 */
	public List<BorderTag> getOffsetKeys(){
		List<BorderTag> result = new ArrayList<BorderTag>();
		for(BorderTag s: offsets.keySet()){
			result.add(s);
		}
		return result;
	}
	
	/**
	 * Test if the given tag is present in the collection
	 * @param tag
	 * @return
	 */
	public boolean hasBorderTag(BorderTag tag){
		if(offsets.keySet().contains(tag)){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Get the requested profile from the cached profiles, or
	 * generate it from the ProfileAggregate if it is not cached.
	 * @param tag the BorderTag to use as index zero
	 * @param quartile the collection quartile to return (0-100) 
	 * @return the quartile profile from the given tag
	 */
	public Profile getProfile(BorderTag tag, double quartile) {
		
		if(tag==null){
			throw new IllegalArgumentException("BorderTag is null");
		}
		
		if(  ! this.hasBorderTag(tag)){
			throw new IllegalArgumentException("BorderTag is not present: "+tag.toString());
		}
		
		// If the profile is not in the cache, make it and add to the cache
		if( ! profileCache.hasProfile(tag, quartile)){

			int indexOffset = offsets.get(tag);
			Profile profile = getAggregate().getQuartile(quartile).offset(indexOffset);
			profileCache.setProfile(tag, quartile, profile );

		}
		return profileCache.getProfile(tag, quartile);	
		
	}
	
	
	/**
	 * Get the requested segmented profile. Generates it dynamically from the
	 * appropriate ProfileAggregate. 
	 * @param s the pointType of the profile to find
	 * @return the profile
	 * @throws Exception
	 */
	public SegmentedProfile getSegmentedProfile(BorderTag tag) {
		if(tag==null){
			throw new IllegalArgumentException("A profile key is required");
		}

		SegmentedProfile result = new SegmentedProfile(getProfile(tag, Constants.MEDIAN), getSegments(tag));
		return result;
	}

	/**
	 * Get the profile aggregate for this collection
	 * @return the aggregate
	 */
	public ProfileAggregate getAggregate(){
		return aggregate;
	}
	
	/**
	 * Test if the profile aggregate has been created
	 * @return
	 */
	public boolean hasAggregate(){
		if (aggregate==null){
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Get the length of the profile aggregate (this is the
	 * integer value of the median CellCollection array length)
	 * @return Length, or zero if the aggregate is not yet created
	 */
	public int length(){
		if(this.hasAggregate()){
			return aggregate.length();
		} else {
			return 0;
		}
	}
	
	/**
	 * Create a list of segments based on an offset of existing segments.
	 * @param s the name of the tag
	 * @return a copy of the segments in the profile, offset to start at the tag
	 */
	public List<NucleusBorderSegment> getSegments(BorderTag tag) {
		if(tag==null){
			throw new IllegalArgumentException("The requested segment key is null: "+tag);
		}

		// this must be negative offset for segments
		// since we are moving the pointIndex back to the beginning
		// of the array
		int offset = -getOffset(tag);

		List<NucleusBorderSegment> result = NucleusBorderSegment.nudge(segments, offset);

		return result;
	}
	
	/**
	 * Test if the collection contains a segment beginning at the given tag
	 * @param tag
	 * @return
	 * @throws Exception
	 */
	public boolean hasSegmentStartingWith(BorderTag tag) throws Exception {
		
		if(getSegmentStartingWith( tag) == null){
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Fetch the segment from the profile beginning at the given tag;
	 * i.e. the segment with a start index of zero, when the profile is offset 
	 * to the tag. 
	 * @param tag the border tag
	 * @return a copy of the segment with the tag at its start index, or null
	 */
	public NucleusBorderSegment getSegmentStartingWith(BorderTag tag) throws Exception {
		List<NucleusBorderSegment> segments = this.getSegments(tag);

		NucleusBorderSegment result = null;
		// get the name of the segment with the tag at the start
		for(NucleusBorderSegment seg : segments){

			if(  seg.getStartIndex()==ZERO_INDEX ){
				result = seg;
			}
		}
		
		return result;
	}
	
	/**
	 * Test if the collection contains a segment beginning at the given tag
	 * @param tag
	 * @return
	 * @throws Exception
	 */
	public boolean hasSegmentEndingWith(BorderTag tag) throws Exception {
		
		if(getSegmentEndingWith( tag) == null){
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Fetch the segment from the profile beginning at the given tag;
	 * i.e. the segment with a start index of zero, when the profile is offset 
	 * to the tag. 
	 * @param tag the border tag
	 * @return a copy of the segment with the tag at its start index, or null
	 */
	public NucleusBorderSegment getSegmentEndingWith(BorderTag tag) throws Exception {
		List<NucleusBorderSegment> segments = this.getSegments(tag);

		NucleusBorderSegment result = null;
		// get the name of the segment with the tag at the start
		for(NucleusBorderSegment seg : segments){

			if(  seg.getEndIndex()==ZERO_INDEX ){
				result = seg;
			}
		}
		
		return result;
	}
	
	/**
	 * Fetch the segment from the profile containing the given index.
	 * The zero index is the reference point
	 * @param index
	 * @return a copy of the segment with the index inside, or null
	 */
	public NucleusBorderSegment getSegmentContaining(int index) throws Exception {
		List<NucleusBorderSegment> segments = this.getSegments(BorderTag.REFERENCE_POINT);

		NucleusBorderSegment result = null;
		// get the name of the segment with the tag at the start
		for(NucleusBorderSegment seg : segments){

			if(  seg.contains(index) ){
				result = seg;
			}
		}
		
		return result;
	}
	
	/**
	 * Fetch the segment from the profile containing at the given tag;
	 * @param tag the border tag
	 * @return a copy of the segment with the tag index inside, or null
	 */
	public NucleusBorderSegment getSegmentContaining(BorderTag tag) throws Exception {
		List<NucleusBorderSegment> segments = this.getSegments(tag);

		NucleusBorderSegment result = null;
		// get the name of the segment with the tag at the start
		for(NucleusBorderSegment seg : segments){

			if(  seg.contains(ZERO_INDEX) ){
				result = seg;
			}
		}
		
		return result;
	}
			
	/**
	 * Add an offset for the given point type. The offset is used
	 * to fetch profiles the begin at the point of interest.
	 * @param pointType the point
	 * @param offset the position of the point in the profile
	 */
	public void addOffset(BorderTag tag, int offset){
		if(tag==null){
			throw new IllegalArgumentException("String is null");
		}
		offsets.put(tag, offset);
		profileCache.clear();
	}
	
	/**
	 * Add a list of segments for the profile. The segments must
	 * have the correct offset to be added directly
	 * @param n the segment list
	 */
	public void addSegments(List<NucleusBorderSegment> n){
		if(n==null || n.isEmpty()){
			throw new NullPointerException("String or segment list is null or empty");
		}
		
		if(this.length() != n.get(0).getTotalLength() ){
			throw new IllegalArgumentException("Segments total length ("
							+n.get(0).getTotalLength()
							+") does not fit aggregate ("+
							+this.length()
							+")");
		}
		
		this.segments = n;
	}
	
	/**
	 * Add a list of segments for the profile, where the segments are
	 * zeroed to the given point type. The indexes will be corrected for
	 * storage. I previously disabled this - unknown why.
	 * @param pointType the point with the zero index in the segments
	 * @param n the segment list
	 */
	public void addSegments(BorderTag tag, List<NucleusBorderSegment> n) {
		if(n==null || n.isEmpty()){
			throw new NullPointerException("String or segment list is null or empty");
		}
		
		if(this.length() != n.get(0).getTotalLength() ){
			throw new IllegalArgumentException("Segments total length ("
							+n.get(0).getTotalLength()
							+") does not fit aggregate ("+
							+this.length()
							+")");
		}
		
		/*
		 * The segments coming in are zeroed to the given pointType pointIndex
		 * This means the indexes must be moved forwards appropriately.
		 * Hence, add a positive offset.
		 */
		int offset = getOffset(tag);

		List<NucleusBorderSegment> result = NucleusBorderSegment.nudge(n, offset);

		this.segments = result;
	}
			
	/**
	 * Create the profile aggregate from the given collection, with a set length, and
	 * containing the given type of nucleus profile
	 * By default, the profiles are zeroed on the reference point
	 * @param collection the CellCollection
	 * @param type the profile type to fetch from the nuclei
	 * @param length the length of the aggregate
	 * @throws Exception 
	 */
	public void createProfileAggregate(CellCollection collection, ProfileType type, int length) {
		if(length<0){
			throw new IllegalArgumentException("Requested length is negative");
		}
		if(collection == null || type == null ){
			throw new IllegalArgumentException("CellCollection or ProfileType is null");
		}
		profileCache.clear();
		aggregate = new ProfileAggregate(length);
//		IJ.log("Making new aggregate: "+type);
		for(Nucleus n : collection.getNuclei()){
			
			
			// Franken profiles have a different length
			
			switch(type){
				case FRANKEN:
					aggregate.addValues(n.getProfile(type));
					break;
				default:
					aggregate.addValues(n.getProfile(type, BorderTag.REFERENCE_POINT)); 
					break;
			
			}
		}
		
	}
	
	
	/**
	 * Create the profile aggregate from the given collection, with using the 
	 * collection median length to determine bin sizes
	 * @param collection the Cellcollection
	 * @throws Exception 
	 */
	public void createProfileAggregate(CellCollection collection, ProfileType type) {
		
		createProfileAggregate(collection, type, (int)collection.getMedianArrayLength());

	}
		
	
	/**
	 * Get the points associated with offsets currently present
	 * @return a string with the points
	 */
	public String tagString(){
		
		StringBuilder builder = new StringBuilder();

		builder.append("\tPoint types:");
		for(BorderTag tag : this.offsets.keySet()){
			builder.append("\t"+tag+": "+this.offsets.get(tag));
		}
		return builder.toString();
	}
	
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append(this.tagString());
		if(this.segments.isEmpty()){
			builder.append("\r\nNo segments in profile collection");
		} else {
			try {

				for(BorderTag tag : this.offsets.keySet()){
					if(tag.type().equals(BorderTagType.CORE)){
						builder.append("\r\nSegments from "+tag+":\r\n");
						for(NucleusBorderSegment s : this.getSegments(tag)){
							builder.append(s.toString()+"\r\n");
						}
					}
				}

			} catch (Exception e) {
				builder.append("\r\nError fetching segments");
			}
		}
		return builder.toString();
		
	}
	
	/**
	 * Turn the IQR (difference between Q25, Q75) of the median into a profile.
	 * @param pointType the profile type to use
	 * @return the profile
	 */
	public Profile getIQRProfile(BorderTag tag) throws Exception {
		
//		int offset = getOffset(tag);
		
		Profile q25 = getProfile(tag, Constants.LOWER_QUARTILE);
		Profile q75 = getProfile(tag, Constants.UPPER_QUARTILE);
		
//		Profile q25 = getAggregate().getQuartile(Constants.LOWER_QUARTILE).offset(offset);
//		Profile q75 = getAggregate().getQuartile(Constants.UPPER_QUARTILE).offset(offset);
		return q75.subtract(q25);
	}
	
	/**
	 * Find the points in the profile that are most variable
	 */
	public List<Integer> findMostVariableRegions(BorderTag tag) throws Exception {
		
		// get the IQR and maxima
		Profile iqrProfile = getIQRProfile(tag);
//		iqrProfile.print();
//		iqrProfile.smooth(3).print();
		BooleanProfile maxima = iqrProfile.smooth(3).getLocalMaxima(3);
//		maxima.print();
//		Profile displayMaxima = maxima.multiply(50);
		
		// given the list of maxima, find the highest 3 regions
		// store the rank (1-3) and the index of the position at this rank
		// To future me: I am sorry about this.
		Map<Integer, Integer> values = new HashMap<Integer, Integer>(0);
		int minIndex = iqrProfile.getIndexOfMin(); // ensure that our has begins with lowest data
		values.put(1, minIndex);
		values.put(2, minIndex);
		values.put(3, minIndex);
		for(int i=0; i<maxima.size();i++ ){
			if(maxima.get(i)==true){
				if(iqrProfile.get(i)>iqrProfile.get(values.get(1))){
					values.put(3,  values.get(2));
					values.put(2,  values.get(1));
					values.put(1, i);
				} else {
					if(iqrProfile.get(i)>iqrProfile.get(values.get(2))){
						values.put(3,  values.get(2));
						values.put(2, i);
					} else {
						if(iqrProfile.get(i)>iqrProfile.get(values.get(3))){
							values.put(3, i);
						}
					}

				}
			}
		}
		List<Integer> result = new ArrayList<Integer>(0);
		for(int i : values.keySet()){
			result.add(values.get(i));
//			IJ.log("    Variable index "+values.get(i));
		}		
		return result;
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		finest("\tReading profile collection");
		in.defaultReadObject();
		finest("\tRead profile collection");
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		finest("Writing profile collection");
		out.defaultWriteObject();
		finest("Wrote profile collection");
	}
	
	private class ProfileCache implements Serializable, Loggable {
		
		private static final long serialVersionUID = 1L;
		
		/**
		 * Store the median profiles from the profile aggregate to save calculation time with large datasets
		 */
		private Map<BorderTag, Map<Double, Profile>> cache = new HashMap<BorderTag, Map<Double, Profile>>();
		
		public ProfileCache(){
			
		}
		
		  /**
		   * Store the given statistic
		   * @param stat
		   * @param scale
		   * @param d
		   */
		  public void setProfile(BorderTag tag, Double quartile, Profile profile){

			  Map<Double, Profile> map;

			  if(cache.containsKey(tag)){

				  map = cache.get(tag);

			  } else {

				  map = new HashMap<Double, Profile>();
				  cache.put(tag, map);

			  }

			  map.put(quartile, profile);
//			  IJ.log("Added "+tag+" - "+quartile+" to profile cache");

		  }

		  /**
		   * Get the given profile from the cache, or null if not present
		 * @param tag
		 * @param quartile
		 * @return
		 */
		  public Profile getProfile(BorderTag tag, Double quartile){

			  if(this.hasProfile(tag, quartile)){
//				  IJ.log("Found "+tag+" - "+quartile+" in profile cache");
				  return cache.get(tag).get(quartile);

			  } else  {
				  
				  return null;

			  }
		  }

		  /**
		   * Check if the given profile is in the cache
		 * @param tag
		 * @param quartile
		 * @return
		 */
		public boolean hasProfile(BorderTag tag, Double quartile){
			  Map<Double, Profile> map;

			  if(cache.containsKey(tag)){

				  map = cache.get(tag);

			  } else {

				  return false;

			  }

			  if(map.containsKey(quartile)){

				  return true;
			  } else {
				  return false;
			  }

		  }
		  
		  /**
		   * Empty the cache - all values must be recalculated
		   */
		  public void clear(){
//			  IJ.log("Clearing cache");
			  cache = null;
			  cache = new HashMap<BorderTag, Map<Double, Profile>>();

		  }
		  
		  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
			  finest("\tReading profile cache");
			  in.defaultReadObject();
			  finest("\tRead profile cache");
		  }

		  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
			  finest("\tWriting profile cache");
			  out.defaultWriteObject();
			  finest("\tWrote profile cache");
		  }
		 
	}
}
