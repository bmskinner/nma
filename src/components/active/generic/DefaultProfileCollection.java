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
import java.util.UUID;

import analysis.profiles.ProfileException;
import components.ICellCollection;
import components.generic.BooleanProfile;
import components.generic.IProfile;
import components.generic.IProfileAggregate;
import components.generic.IProfileCollection;
import components.generic.ISegmentedProfile;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.IBorderSegment;
import components.nuclei.Nucleus;
//import ij.IJ;

import stats.Quartile;

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
 * @since 1.13.3
 *
 */
public class DefaultProfileCollection implements IProfileCollection {
		
	private static final long serialVersionUID = 1L;
		
	private Map<Tag, Integer>    indexes  = new HashMap<Tag, Integer>();
	private IBorderSegment[]     segments = null;
	
	private transient int length;
	private transient Map<ProfileType, IProfileAggregate> map = new HashMap<ProfileType, IProfileAggregate>();

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
	public IProfile getProfile(ProfileType type, Tag tag, double quartile) throws UnavailableBorderTagException, ProfileException, UnavailableProfileTypeException {
		
		if(tag==null){
			throw new IllegalArgumentException("Tag cannot be null");
		}
		
		if(  ! this.hasBorderTag(tag)){
			throw new UnavailableBorderTagException("Tag is not present: "+tag.toString());
		}
		
		if( ! map.containsKey(type)){
			throw new UnavailableProfileTypeException("Profile type is not present: "+type.toString());
		}
		
		IProfileAggregate agg = map.get(type);
		IProfile p;
		try {
			p = agg.getQuartile(quartile);
		} catch (NullPointerException e) {
			warn("Cannot get profile for "+quartile);
			stack("Error fetching quartile", e);
			throw new ProfileException("Null pointer exception getting quartile from aggregate");
		}
		
		int offset = indexes.get(tag);
		
		return p.offset(offset);
	
		
	}
	
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#getSegmentedProfile(components.generic.BorderTagObject)
	 */
	@Override
	public ISegmentedProfile getSegmentedProfile(ProfileType type, Tag tag, double quartile) 
			throws UnavailableBorderTagException, 
				   ProfileException, 
			       UnavailableProfileTypeException, 
			       UnsegmentedProfileException {
		
		if(tag==null || type==null){
			throw new IllegalArgumentException("A profile type and tag is required");
		}
		
		if(quartile < 0 || quartile > 100){
			throw new IllegalArgumentException("Quartile must be between 0-100");
		}
		
		// get the profile array
		IProfile p = getProfile(type, tag, quartile);
		fine("Making segmented profile of type "+type+" from "+tag+" with q"+quartile);

		try {
			if(segments[0]==null){
				throw new UnsegmentedProfileException("No segments assigned to profile collection");
			}
		} catch(NullPointerException e){
			//			error("No segments assigned to profile collection", e);
			throw new UnsegmentedProfileException("No segments assigned to profile collection", e);
		}

		ISegmentedProfile result;
		try {
			result = new SegmentedFloatProfile(p, getSegments(tag));
		} catch(IndexOutOfBoundsException e){
			stack("Cannot create segmented profile due to segment/profile mismatch", e);
			throw new ProfileException("Cannot create segmented profile; segment lengths do not match array", e);
		}
		return result;
	}
	
	
	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#getSegmentIDs()
	 */
	@Override
	public synchronized List<UUID> getSegmentIDs(){
		List<UUID> result = new ArrayList<UUID>();
		if(segments==null){
			return result;
		}
		for(IBorderSegment seg : this.segments){
			result.add(seg.getID());
		}
		return result;
	}
	
	@Override
	public synchronized IBorderSegment getSegmentAt(Tag tag, int position){
		return this.getSegments(tag).get(position);
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#length()
	 */
	@Override
	public int length(){
		return length;
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#getSegments(components.generic.BorderTagObject)
	 */
	@Override
	public synchronized List<IBorderSegment> getSegments(Tag tag) {
		if(tag==null){
			throw new IllegalArgumentException("The requested segment key is null: "+tag);
		}

		// this must be negative offset for segments
		// since we are moving the pointIndex back to the beginning
		// of the array
		int offset = -getIndex(tag);

		List<IBorderSegment> result;
		if(segments==null){
			return new ArrayList<IBorderSegment>(0);
		}
		
		try {
			result = IBorderSegment.nudge(segments, offset);
		} catch (ProfileException e) {
			stack("Error offsetting segments", e);
			return new ArrayList<IBorderSegment>(0);
		}

		return result;
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#hasSegmentStartingWith(components.generic.BorderTagObject)
	 */
	@Override
	public boolean hasSegmentStartingWith(Tag tag) throws UnsegmentedProfileException {
		
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
	public IBorderSegment getSegmentStartingWith(Tag tag) throws UnsegmentedProfileException {
		List<IBorderSegment> segments = this.getSegments(tag);
		
		if(segments.size()==0){
			throw new UnsegmentedProfileException("No segments assigned to profile collection");
		}

		IBorderSegment result = null;
		// get the name of the segment with the tag at the start
		for(IBorderSegment seg : segments){

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
	public boolean hasSegmentEndingWith(Tag tag) throws UnsegmentedProfileException {
		
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
	public IBorderSegment getSegmentEndingWith(Tag tag) throws UnsegmentedProfileException {
		List<IBorderSegment> segments = this.getSegments(tag);

		if(segments.size()==0){
			throw new UnsegmentedProfileException("No segments assigned to profile collection");
		}
		
		IBorderSegment result = null;
		// get the name of the segment with the tag at the start
		for(IBorderSegment seg : segments){

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
	public IBorderSegment getSegmentContaining(int index) throws UnsegmentedProfileException {
		List<IBorderSegment> segments = this.getSegments(Tag.REFERENCE_POINT);
		
		if(segments.size()==0){
			throw new UnsegmentedProfileException("No segments assigned to profile collection");
		}
		
		IBorderSegment result = null;
		// get the name of the segment with the tag at the start
		for(IBorderSegment seg : segments){

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
	public IBorderSegment getSegmentContaining(Tag tag) throws ProfileException {
		List<IBorderSegment> segments = this.getSegments(tag);

		IBorderSegment result = null;
		// get the name of the segment with the tag at the start
		for(IBorderSegment seg : segments){

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
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#addSegments(java.util.List)
	 */
	@Override
	public void addSegments(List<IBorderSegment> n){
		if(n==null || n.isEmpty()){
			throw new IllegalArgumentException("Segment list is null or empty");
		}
		
		if(this.length() != n.get(0).getTotalLength() ){
			throw new IllegalArgumentException("Segments total length ("
							+n.get(0).getTotalLength()
							+") does not fit aggregate ("+
							+this.length()
							+")");
		}
		
		this.segments = new IBorderSegment[n.size()];
		
		for(int i=0; i<segments.length; i++){
			segments[i] = n.get(i);
		}
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#addSegments(components.generic.BorderTagObject, java.util.List)
	 */
	@Override
	public void addSegments(Tag tag, List<IBorderSegment> n) {
		if(n==null || n.isEmpty()){
			throw new IllegalArgumentException("String or segment list is null or empty");
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

		List<IBorderSegment> result;
		try {
			result = IBorderSegment.nudge(n, offset);
		} catch (ProfileException e) {
			stack("Error offsetting segments", e);
			return;
		}
		
		this.segments = new IBorderSegment[n.size()];

		for(int i=0; i<segments.length; i++){
			segments[i] = result.get(i);
		}
	}
	
	@Override
	public double[] getValuesAtPosition(ProfileType type, double position) throws UnavailableProfileTypeException{
		
		double[] result = map.get(type).getValuesAtPosition(position);
		
		if(result==null){
			
			result = new double[length()];
			for(int i=0; i<result.length; i++){
				result[i] = 0;
			}
		}

		return result;
	}
	
	@Override
	public List<Double> getXKeyset(ProfileType type){
		return map.get(type).getXKeyset();
	}
			
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#createProfileAggregate(components.CellCollection, components.generic.ProfileType, int)
	 */
	@Override
	public void createProfileAggregate(ICellCollection collection, int length) {
		if(length<=0){
			throw new IllegalArgumentException("Requested profile aggregate length is zero or negative");
		}
		if(collection == null){
			throw new IllegalArgumentException("CellCollection or ProfileType is null");
		}
		
		this.length = length;
		
		if(segments!=null && length != segments[0].getTotalLength()){
			
			Exception e = new Exception("Creating profile aggregate will invalidate segments");
			stack("Segments exist at different length to created aggregate", e);
			segments = null;
		}

		for(ProfileType type : ProfileType.values()){

			IProfileAggregate agg = new DefaultProfileAggregate(length, collection.size());

			map.put(type, agg);
			try {
				for(Nucleus n : collection.getNuclei()){

					switch(type){
					case FRANKEN:

						agg.addValues(n.getProfile(type));

						break;
					default:
						agg.addValues(n.getProfile(type, Tag.REFERENCE_POINT)); 
						break;

					}
				}

			} catch(ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e){
				stack("Error making aggregate", e);
			}

		}
		
	}
	
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#createProfileAggregate(components.CellCollection, components.generic.ProfileType)
	 */
//	@Override
	public void createProfileAggregate(ICellCollection collection) {
		
		createProfileAggregate(collection, collection.getMedianArrayLength());

	}
	
	public void createAndRestoreProfileAggregate(ICellCollection collection) {

		if(segments==null){
			createProfileAggregate(collection, collection.getMedianArrayLength());
		} else {
			int length = segments[0].getTotalLength();
			createProfileAggregate(collection, length);
		}
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

		try {

			for(Tag tag : this.indexes.keySet()){
				if(tag.type().equals(components.generic.BorderTag.BorderTagType.CORE)){
					builder.append("\r\nSegments from "+tag+":\r\n");
					for(IBorderSegment s : this.getSegments(tag)){
						builder.append(s.toString()+"\r\n");
					}
				}
			}

		} catch (Exception e) {
			builder.append("\r\nError fetching segments");
		}

		return builder.toString();
		
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#getIQRProfile(components.generic.BorderTagObject)
	 */
	@Override
	public IProfile getIQRProfile(ProfileType type, Tag tag) throws UnavailableBorderTagException, ProfileException, UnavailableProfileTypeException {
				
		IProfile q25 = getProfile(type, tag, Quartile.LOWER_QUARTILE);
		IProfile q75 = getProfile(type, tag, Quartile.UPPER_QUARTILE);
		
		if(q25==null || q75==null){ // if something goes wrong, return a zero profile
			
			warn("Problem calculating the IQR - setting to zero");			
			return new FloatProfile(0, length);
		}
		

		return q75.subtract(q25);
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfileCollection#findMostVariableRegions(components.generic.BorderTagObject)
	 */
	@Override
	public List<Integer> findMostVariableRegions(ProfileType type, Tag tag) {
		
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

		for(int i : values.keySet()){
			result.add(values.get(i));
//			IJ.log("    Variable index "+values.get(i));
		}		
		return result;
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		map = new HashMap<ProfileType, IProfileAggregate>();
		
//		if(length != segments[0].getTotalLength()){
//			log("Segment length "+segments[0].getTotalLength() );
//		}
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

}
