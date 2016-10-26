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
package components.active.generic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import analysis.profiles.ProfileException;
import components.ICellCollection;
import components.generic.BooleanProfile;
import components.generic.IProfile;
import components.generic.IProfileAggregate;
import components.generic.IProfileCollection;
import components.generic.ISegmentedProfile;
import components.generic.Profile;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.generic.Tag;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
//import ij.IJ;

import utility.Constants;

/**
 * Holds the ProfileAggregate with individual nucleus values,
 * and stores the indexes of BorderTags within the profile.
 * Provides methods to get the median and other quartiles from 
 * the collection. The Reference Point is always at the zero index
 * of the collection.
 * 
 * Also stores the NucleusBorderSegments within the profile, and provides
 * methods to transform the segments to fit profiles offset from given 
 * border tags.
 * 
 * An internal ProfileCache holds average profiles to save repeated calculation
 * @author ben
 *
 */
public class DefaultProfileCollection implements IProfileCollection {
		
	private static final long serialVersionUID = 1L;
		
	private transient IProfileAggregate aggregate = null;
	
	private Map<Tag, Integer>    indexes  = new HashMap<Tag, Integer>();
	private List<NucleusBorderSegment> segments = new ArrayList<NucleusBorderSegment>();
	
	
	private transient ProfileCache profileCache           = new ProfileCache();


	/**
	 * Create an empty profile collection
	 */
	public DefaultProfileCollection(){
		indexes.put(Tag.REFERENCE_POINT, ZERO_INDEX);
	}
			
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#getIndex(components.generic.BorderTagObject)
	 */
	@Override
	public int getIndex(Tag pointType){
		if(pointType==null){
			throw new IllegalArgumentException("The requested offset key is null: "+pointType);
		}
		if(indexes.containsKey(pointType)){	
			return indexes.get(pointType);
		} else {
			return -1;
		}
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#getBorderTags()
	 */
	@Override
	public List<Tag> getBorderTags(){
		List<Tag> result = new ArrayList<Tag>();
		for(Tag s: indexes.keySet()){
			result.add(s);
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#hasBorderTag(components.generic.BorderTagObject)
	 */
	@Override
	public boolean hasBorderTag(Tag tag){
		return indexes.keySet().contains(tag);
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#getProfile(components.generic.BorderTagObject, double)
	 */
	@Override
	public IProfile getProfile(Tag tag, double quartile) {
		
		if(tag==null){
			throw new IllegalArgumentException("BorderTagObject is null");
		}
		
		if(  ! this.hasBorderTag(tag)){
			throw new IllegalArgumentException("BorderTagObject is not present: "+tag.toString());
		}

		// If the profile is not in the cache, make it and add to the cache
		if( ! profileCache.hasProfile(tag, quartile)){

			int indexOffset = indexes.get(tag);			
			IProfile profile;
			try {
				profile = getAggregate().getQuartile(quartile).offset(indexOffset);
			} catch (ProfileException e) {
				error("Unable to get profile for "+tag+" quartile "+quartile, e);
				return null;
			}
			profileCache.setProfile(tag, quartile, profile );

		}
		return profileCache.getProfile(tag, quartile);	
		
	}
	
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#getSegmentedProfile(components.generic.BorderTagObject)
	 */
	@Override
	public ISegmentedProfile getSegmentedProfile(Tag tag) {
		if(tag==null){
			throw new IllegalArgumentException("A profile key is required");
		}

		ISegmentedProfile result = new SegmentedFloatProfile(getProfile(tag, Constants.MEDIAN), getSegments(tag));
		return result;
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#getAggregate()
	 */
	@Override
	public IProfileAggregate getAggregate(){
		return (DefaultProfileAggregate) aggregate;
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#hasAggregate()
	 */
	@Override
	public boolean hasAggregate(){
		if (aggregate==null){
			return false;
		} else {
			return true;
		}
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#length()
	 */
	@Override
	public int length(){
		if(this.hasAggregate()){
			return aggregate.length();
		} else {
			return 0;
		}
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#getSegments(components.generic.BorderTagObject)
	 */
	@Override
	public List<NucleusBorderSegment> getSegments(Tag tag) {
		if(tag==null){
			throw new IllegalArgumentException("The requested segment key is null: "+tag);
		}

		// this must be negative offset for segments
		// since we are moving the pointIndex back to the beginning
		// of the array
		int offset = -getIndex(tag);

		List<NucleusBorderSegment> result = NucleusBorderSegment.nudge(segments, offset);

		return result;
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#hasSegmentStartingWith(components.generic.BorderTagObject)
	 */
	@Override
	public boolean hasSegmentStartingWith(Tag tag) throws Exception {
		
		if(getSegmentStartingWith( tag) == null){
			return false;
		} else {
			return true;
		}
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#getSegmentStartingWith(components.generic.BorderTagObject)
	 */
	@Override
	public NucleusBorderSegment getSegmentStartingWith(Tag tag) throws Exception {
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
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#hasSegmentEndingWith(components.generic.BorderTagObject)
	 */
	@Override
	public boolean hasSegmentEndingWith(Tag tag) throws Exception {
		
		if(getSegmentEndingWith( tag) == null){
			return false;
		} else {
			return true;
		}
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#getSegmentEndingWith(components.generic.BorderTagObject)
	 */
	@Override
	public NucleusBorderSegment getSegmentEndingWith(Tag tag) throws Exception {
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
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#getSegmentContaining(int)
	 */
	@Override
	public NucleusBorderSegment getSegmentContaining(int index) throws Exception {
		List<NucleusBorderSegment> segments = this.getSegments(Tag.REFERENCE_POINT);

		NucleusBorderSegment result = null;
		// get the name of the segment with the tag at the start
		for(NucleusBorderSegment seg : segments){

			if(  seg.contains(index) ){
				result = seg;
			}
		}
		
		return result;
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#getSegmentContaining(components.generic.BorderTagObject)
	 */
	@Override
	public NucleusBorderSegment getSegmentContaining(Tag tag) throws ProfileException {
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
			
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#addIndex(components.generic.BorderTagObject, int)
	 */
	@Override
	public void addIndex(Tag tag, int offset){
		if(tag==null){
			throw new IllegalArgumentException("BorderTagObject is null");
		}
		
		// Cannot move the RP from zero
		if(tag.equals(Tag.REFERENCE_POINT)){
			return;
		}
		indexes.put(tag, offset);
		profileCache.clear();
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#addSegments(java.util.List)
	 */
	@Override
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
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#addSegments(components.generic.BorderTagObject, java.util.List)
	 */
	@Override
	public void addSegments(Tag tag, List<NucleusBorderSegment> n) {
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
		int offset = getIndex(tag);

		List<NucleusBorderSegment> result = NucleusBorderSegment.nudge(n, offset);

		this.segments = result;
	}
			
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#createProfileAggregate(components.CellCollection, components.generic.ProfileType, int)
	 */
	@Override
	public void createProfileAggregate(ICellCollection collection, ProfileType type, int length) {
		if(length<=0){
			throw new IllegalArgumentException("Requested profile aggregate length is zero or negative");
		}
		if(collection == null || type == null ){
			throw new IllegalArgumentException("CellCollection or ProfileType is null");
		}
		profileCache.clear();
		aggregate = new DefaultProfileAggregate(length, collection.size());

		
		try {
		for(Nucleus n : collection.getNuclei()){
			
			switch(type){
				case FRANKEN:
				
					aggregate.addValues(n.getProfile(type));
				
					break;
				default:
					aggregate.addValues(n.getProfile(type, Tag.REFERENCE_POINT)); 
					break;
			
			}
		}
		
		} catch(ProfileException e){
			error("Error making aggregate", e);
		}
		
	}
	
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#createProfileAggregate(components.CellCollection, components.generic.ProfileType)
	 */
	@Override
	public void createProfileAggregate(ICellCollection collection, ProfileType type) {
		
		createProfileAggregate(collection, type, collection.getMedianArrayLength());

	}
		
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#tagString()
	 */
	@Override
	public String tagString(){
		
		StringBuilder builder = new StringBuilder();

		builder.append("\tPoint types:");
		for(Tag tag : this.indexes.keySet()){
			builder.append("\t"+tag+": "+this.indexes.get(tag));
		}
		return builder.toString();
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#toString()
	 */
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append(this.tagString());
		if(this.segments.isEmpty()){
			builder.append("\r\nNo segments in profile collection");
		} else {
			try {

				for(Tag tag : this.indexes.keySet()){
					if(tag.type().equals(components.generic.BorderTag.BorderTagType.CORE)){
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
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#getIQRProfile(components.generic.BorderTagObject)
	 */
	@Override
	public IProfile getIQRProfile(Tag tag) {
				
		IProfile q25 = getProfile(tag, Constants.LOWER_QUARTILE);
		IProfile q75 = getProfile(tag, Constants.UPPER_QUARTILE);
		
		if(q25==null || q75==null){ // if something goes wrong, return a zero profile
			
			warn("Problem calculating the IQR - setting to zero");			
			return new FloatProfile(0, aggregate.length());
		}
		

		return q75.subtract(q25);
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#findMostVariableRegions(components.generic.BorderTagObject)
	 */
	@Override
	public List<Integer> findMostVariableRegions(Tag tag) throws Exception {
		
		// get the IQR and maxima
		IProfile iqrProfile = getIQRProfile(tag);
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
		List<Integer> result = new ArrayList<Integer>(values.size());
		for(int i : values.keySet()){
			result.add(values.get(i));
//			IJ.log("    Variable index "+values.get(i));
		}		
		return result;
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		profileCache = new ProfileCache();
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}
	
	private class ProfileCache {
				
		/**
		 * Store the median profiles from the profile aggregate to save calculation time with large datasets
		 */
		private Map<Tag, Map<Double, IProfile>> cache = new HashMap<Tag, Map<Double, IProfile>>();
		
		public ProfileCache(){
			
		}
		
		  /**
		   * Store the given statistic
		   * @param stat
		   * @param scale
		   * @param d
		   */
		  public void setProfile(Tag tag, Double quartile, IProfile profile){

			  Map<Double, IProfile> map;

			  if(cache.containsKey(tag)){

				  map = cache.get(tag);

			  } else {

				  map = new HashMap<Double, IProfile>();
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
		  public IProfile getProfile(Tag tag, Double quartile){

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
		public boolean hasProfile(Tag tag, Double quartile){
			  Map<Double, IProfile> map;

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
			  cache = new HashMap<Tag, Map<Double, IProfile>>();
		  }

	}
}
