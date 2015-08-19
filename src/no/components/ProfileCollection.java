package no.components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.collections.CellCollection;
import no.nuclei.Nucleus;
import utility.Utils;

public class ProfileCollection implements Serializable {
		
	private static final long serialVersionUID = 1L;
	
	private ProfileAggregate 	aggregate;
	
	private Map<String, Integer> offsets 		= new HashMap<String, Integer>();
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
	public int getOffset(String pointType){
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
	public List<String> getOffsetKeys(){
		List<String> result = new ArrayList<String>();
		for(String s: offsets.keySet()){
			result.add(s);
		}
		return result;
	}
	
	/**
	 * Get the requested profile. Generates it dynamically from the
	 * appropriate ProfileAggregate each time
	 * @param s the profile to find
	 * @return the profile
	 */
	public Profile getProfile(String s) throws Exception {
		if(s==null){
			throw new IllegalArgumentException("The requested profile key is null: "+s);
		}
		String pointType = s;
		if(s.endsWith("25")){
			pointType = s.replace("25", "");
			int indexOffset = offsets.get(pointType);
			return getAggregate().getQuartile(25).offset(indexOffset);
		}
		
		if( s.endsWith("75")){
			pointType = s.replace("75", "");
			int indexOffset = offsets.get(pointType);
			return getAggregate().getQuartile(75).offset(indexOffset);
		}
		
		int indexOffset = offsets.get(pointType);
		return getAggregate().getMedian().offset(indexOffset);
	}
	
	
	public SegmentedProfile getSegmentedProfile(String s) throws Exception {
		if(s==null){
			throw new IllegalArgumentException("The requested profile key is null: "+s);
		}
		SegmentedProfile result = new SegmentedProfile(getProfile(s), getSegments(s));
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
	 * Create a list of segments based on an offset of existing segments
	 * @param s the name of the point type
	 */
	public List<NucleusBorderSegment> getSegments(String s){
		if(s==null){
			throw new IllegalArgumentException("The requested segment key is null: "+s);
		}

		// this must be negative offset for segments
		int offset = -getOffset(s);

		List<NucleusBorderSegment> result = null;
		try {
			result = NucleusBorderSegment.nudge(segments, offset);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	public List<Profile> getNucleusProfiles(String s) throws Exception {
		if(s==null){
			throw new IllegalArgumentException("The requested profile list key is null: "+s);
		}
		List<Profile> result = new ArrayList<Profile>();
		for(Profile p : nucleusProfileList){
			result.add(new Profile(p.offset(this.getOffset(s))));
		}
		return result;
	}
		
	/**
	 * Add an offset for the given point type. The offset is used
	 * to fetch profiles the begin at the point of interest.
	 * @param pointType the point
	 * @param offset the position of the point in the profile
	 */
	public void addOffset(String pointType, int offset){
		if(pointType==null){
			throw new IllegalArgumentException("String is null");
		}
		offsets.put(pointType, offset);
	}
	
	/**
	 * Add a list of segments for the profile
	 * @param n the segment list
	 */
	public void addSegments(List<NucleusBorderSegment> n){
		if(n==null || n.isEmpty()){
			throw new IllegalArgumentException("String or segment list is null or empty");
		}
		segments = n;
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
	 * Create the profile aggregate from the given collection, with a set length
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
			aggregate.addValues(n.getAngleProfile(collection.getReferencePoint()));
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

		builder.append("    Point types:\t");
		for(String s : this.offsets.keySet()){
			builder.append("     "+s+"\t");
		}
		return builder.toString();
	}
	
	/**
	 * Turn the IQR (difference between Q25, Q75) of the median into a profile.
	 * @param pointType the profile type to use
	 * @return the profile
	 */
	public Profile getIQRProfile(String pointType) throws Exception {
		
		int offset = getOffset(pointType);
		Profile q25 = getAggregate().getQuartile(25).offset(offset);
		Profile q75 = getAggregate().getQuartile(75).offset(offset);
		return q75.subtract(q25);
	}
	
	/**
	 * Find the points in the profile that are most variable
	 */
	public List<Integer> findMostVariableRegions(String pointType) throws Exception {
		
		// get the IQR and maxima
		Profile iqrProfile = getIQRProfile(pointType);
//		iqrProfile.print();
//		iqrProfile.smooth(3).print();
		Profile maxima = iqrProfile.smooth(3).getLocalMaxima(3);
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
			if(maxima.get(i)==1){
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
