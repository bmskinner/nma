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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import components.CellCollection;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
import ij.IJ;
import utility.Constants;

public class ProfileCollection implements Serializable {
		
	private static final long serialVersionUID = 1L;
	
	private static final int ZERO_INDEX = 0;
	
	private ProfileAggregate 	aggregate = null;
	
	private Map<BorderTag, Integer> offsets 		= new HashMap<BorderTag, Integer>();
	private List<NucleusBorderSegment> segments = new ArrayList<NucleusBorderSegment>();
	private List<Profile> nucleusProfileList    = new ArrayList<Profile>();
	
	
	public ProfileCollection(){
	}
			
	/**
	 * Get the offset needed to transform a profile to start from the given 
	 * point type
	 * @param pointType
	 * @return
	 */
	public int getOffset(BorderTag pointType){
		if(pointType==null){
			throw new IllegalArgumentException("The requested offset key is null: "+pointType);
		}
		if(offsets.containsKey(pointType)){	
			return offsets.get(pointType);
		} else {
			throw new IllegalArgumentException("The requested offset key does not exist: "+pointType);
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
	 * Get the requested profile. Generates it dynamically from the
	 * appropriate ProfileAggregate each time. 
	 * @param tag the BorderTag to use as index zero
	 * @param quartile the collection quartile to return (0-100) 
	 * @return the quartile profile from the given tag
	 * @throws Exception
	 */
	public Profile getProfile(BorderTag tag, double quartile) throws Exception {
		if(tag==null){
			throw new IllegalArgumentException("A profile key is required");
		}
		
		if(this.hasBorderTag(tag)){

			int indexOffset = offsets.get(tag);
			return getAggregate().getQuartile(quartile).offset(indexOffset);
			
		} else {
			throw new IllegalArgumentException("The requested tag is not present: "+tag.toString());
		}
	}
	
	
	/**
	 * Get the requested segmented profile. Generates it dynamically from the
	 * appropriate ProfileAggregate. 
	 * @param s the pointType of the profile to find
	 * @return the profile
	 * @throws Exception
	 */
	public SegmentedProfile getSegmentedProfile(BorderTag tag) throws Exception {
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
	public List<NucleusBorderSegment> getSegments(BorderTag tag) throws Exception {
		if(tag==null){
			throw new IllegalArgumentException("The requested segment key is null: "+tag);
		}

		// this must be negative offset for segments
		// since we are moving the pointIndex back to the beginning
		// of the array
		int offset = -getOffset(tag);
		
//		IJ.log("");
		
//		IJ.log("Existing segments:");
//		IJ.log(NucleusBorderSegment.toString(this.segments));
//		IJ.log("Border tag "+tag+"    Offset "+offset);
//		IJ.log("Nudging segments");
		List<NucleusBorderSegment> result = NucleusBorderSegment.nudge(segments, offset);
//		IJ.log(NucleusBorderSegment.toString(result));
//		IJ.log("");
		return result;
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
	 * Get the nucleus profiles offset to the requested key. 
	 * This will not be as accurate as a nucleus by nucleus approach, 
	 * in which the keys are linked directly to indexes. Use only when no choice
	 * i.e for frankenprofiles. With frankenprofiles, the profiles are mapped to
	 * the median already, so the offsets should match up
	 * @param s the key to offset to 
	 * @return a list of profiles offset to the given key
	 */
	public List<Profile> getNucleusProfiles(BorderTag tag) throws Exception {
		if(tag==null){
			throw new IllegalArgumentException("The requested profile list key is null: "+tag);
		}
		List<Profile> result = new ArrayList<Profile>();
		for(Profile p : nucleusProfileList){
			result.add(new Profile(p.offset(this.getOffset(tag))));
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
	public void addSegments(BorderTag tag, List<NucleusBorderSegment> n) throws Exception {
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
	 * Add individual nucleus profiles to teh collection.
	 * This allows frankenmedian creation
	 * @param profiles the list of profiles
	 */
	public void addNucleusProfiles(List<Profile> profiles){
		if(profiles==null || profiles.isEmpty()){
			throw new IllegalArgumentException("String or segment list is null or empty");
		}
		nucleusProfileList = profiles;
	}
	
	/**
	 * Create the profile aggregate from the given collection, with a set length.
	 * By default, the profiles are zeroed on the reference point
	 * @param collection the Cellcollection
	 * @param length the length of the aggregate
	 * @throws Exception 
	 */
	public void createProfileAggregate(CellCollection collection, int length) throws Exception{
		if(length<0){
			throw new IllegalArgumentException("Requested length is negative");
		}

		aggregate = new ProfileAggregate(length);
		for(Nucleus n : collection.getNuclei()){
			aggregate.addValues(n.getAngleProfile(BorderTag.REFERENCE_POINT));
		}
	}
	
	
	/**
	 * Create the profile aggregate from the given collection, with using the 
	 * collection median length to determine bin sizes
	 * @param collection the Cellcollection
	 * @throws Exception 
	 */
	public void createProfileAggregate(CellCollection collection) throws Exception{
		
		createProfileAggregate(collection, (int)collection.getMedianArrayLength());

	}
	
	/**
	 * This allows the generation of a frankenMedian from stored FrankenProfiles.
	 * Rather than using the real nucleus profile data, it calls the list of 
	 * recombined profiles previously generated. the profiles should also all start
	 * from the reference point.
	 */
	public void createProfileAggregateFromInternalProfiles(int length){
		if(this.nucleusProfileList==null || this.nucleusProfileList.isEmpty()){
			throw new IllegalArgumentException("The internal profile list is empty");
		}
		aggregate = new ProfileAggregate(length);
		for(Profile profile : nucleusProfileList){
			aggregate.addValues(profile);
		}
		
	}
	
	
	/**
	 * Get the points associated with offsets currently present
	 * @return a string with the points
	 */
	public String printKeys(){
		
		StringBuilder builder = new StringBuilder();

		builder.append("\tPoint types:");
		for(BorderTag tag : this.offsets.keySet()){
			builder.append("\t"+tag+": "+this.offsets.get(tag));
		}
		return builder.toString();
	}
	
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append(this.printKeys());
		if(this.segments.isEmpty()){
			builder.append("\r\nNo segments in profile collection");
		} else {
			try {
				builder.append("\r\nRaw segments:\r\n");
				for(NucleusBorderSegment s : this.getSegments(BorderTag.REFERENCE_POINT)){
					builder.append(s.toString()+"\r\n");
				}

				for(BorderTag tag : this.offsets.keySet()){
					builder.append("\r\nSegments from "+tag+":\r\n");
					for(NucleusBorderSegment s : this.getSegments(tag)){
						builder.append(s.toString()+"\r\n");
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
		
		int offset = getOffset(tag);
		Profile q25 = getAggregate().getQuartile(Constants.LOWER_QUARTILE).offset(offset);
		Profile q75 = getAggregate().getQuartile(Constants.UPPER_QUARTILE).offset(offset);
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
}
