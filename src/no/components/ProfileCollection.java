package no.components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import no.utility.Utils;

public class ProfileCollection implements Serializable {
		
	private static final long serialVersionUID = 1L;
//	public static final int CHART_WINDOW_HEIGHT     = 400;
//	public static final int CHART_WINDOW_WIDTH      = 600;
	
	private Map<String, ProfileFeature> 	features 	= new HashMap<String, ProfileFeature>();
	private Map<String, Profile> 			profiles 	= new HashMap<String, Profile>(0); 
	private Map<String, ProfileAggregate> 	aggregates 	= new HashMap<String, ProfileAggregate>();
	
	private String collectionName; // the name of the NucleusCollection - e.g analysable, red, not_red
	
	private Map<String, List<NucleusBorderSegment>> segments = new HashMap<String, List<NucleusBorderSegment>>();
	private Map<String, List<Profile>> nucleusProfileList    = new HashMap<String, List<Profile>>();
	
		
	public ProfileCollection(String name){
		this.collectionName = name;
	}
	
	// Get features
	
	public ProfileFeature getFeature(String s){
		if(s==null){
			throw new IllegalArgumentException("The requested feature key is null: "+s);
		}
		if(features.containsKey(s)){	
			return features.get(s);
		} else {
			throw new IllegalArgumentException("The requested feature key does not exist: "+s);
		}
	}
	
	public Profile getProfile(String s){
		if(s==null){
			throw new IllegalArgumentException("The requested profile key is null: "+s);
		}
		if(profiles.containsKey(s)){	
			return profiles.get(s);
		} else {
			throw new IllegalArgumentException("The requested profile key does not exist: "+s);
		}
	}
	
	public ProfileAggregate getAggregate(String s){
		if(s==null){
			throw new IllegalArgumentException("The requested aggregate key is null: "+s);
		}
		if(aggregates.containsKey(s)){	
			return aggregates.get(s);
		} else {
			throw new IllegalArgumentException("The requested aggregate key does not exist: "+s);
		}
	}
		
	public List<NucleusBorderSegment> getSegments(String s){
		if(s==null){
			throw new IllegalArgumentException("The requested segment key is null: "+s);
		}
		if(segments.containsKey(s)){	
			return segments.get(s);
		} else {
			throw new IllegalArgumentException("The requested segment key does not exist: "+s);
		}
	}
	
	public List<Profile> getNucleusProfiles(String s){
		if(s==null){
			throw new IllegalArgumentException("The requested profile list key is null: "+s);
		}
		if(nucleusProfileList.containsKey(s)){	
			return nucleusProfileList.get(s);
		} else {
			throw new IllegalArgumentException("The requested profile list key does not exist: "+s);
		}
	}
	
	// Add or update features
	
	public void addFeature(String s, ProfileFeature p){
		if(s==null || p==null){
			throw new IllegalArgumentException("String or Profile is null");
		}
		features.put(s, p);
	}
	
	public void addProfile(String s, Profile p){
		if(s==null || p==null){
			throw new IllegalArgumentException("String or Profile is null");
		}
		profiles.put(s, p);
	}
	
	public void addAggregate(String s, ProfileAggregate p){
		aggregates.put(s, p);
	}

	public void addSegments(String s, List<NucleusBorderSegment> n){
		if(s==null || n==null || n.isEmpty()){
			throw new IllegalArgumentException("String or segment list is null or empty");
		}
		segments.put(s, n);
	}
	
	/**
	 * Create a list of segments based on an offset of existing segments
	 * This is an alternative to re-segmenting while transition to indexing is in progress
	 * @param pointToAdd the name of the pointType to add
	 * @param referencePoint the name of the pointType to take segments from
	 * @param offset the offset to apply to each segment
	 */
	public void addSegments(String pointToAdd, String referencePoint, int offset){
		if(pointToAdd==null || referencePoint==null || Integer.valueOf(offset)==null){
			throw new IllegalArgumentException("String or offset is null or empty");
		}
		List<NucleusBorderSegment> referenceList =  getSegments(referencePoint);
		List<NucleusBorderSegment> result = new ArrayList<NucleusBorderSegment>(0);
		for(NucleusBorderSegment s : referenceList){
			
			int newStart = Utils.wrapIndex( s.getStartIndex()+ offset , getProfile(referencePoint).size());
			int newEnd = Utils.wrapIndex( s.getEndIndex()+ offset , getProfile(referencePoint).size());
			
			NucleusBorderSegment c = new NucleusBorderSegment(newStart, newEnd);
			c.setSegmentType(s.getSegmentType());
			
			result.add(c);
		}
		
		segments.put(pointToAdd, result);
	}
	
	public void addNucleusProfiles(String s, List<Profile> n){
		if(s==null || n==null || n.isEmpty()){
			throw new IllegalArgumentException("String or segment list is null or empty");
		}
		nucleusProfileList.put(s, n);
	}
	
	
	public void createProfileAggregateFromPoint(String pointType, int length){
		if(pointType==null){
			throw new IllegalArgumentException("Point type is null");
		}
		if(length<0){
			throw new IllegalArgumentException("Requested length is negative");
		}

		ProfileAggregate profileAggregate = this.getAggregate(pointType);
		Profile medians = profileAggregate.getMedian();
		Profile q25     = profileAggregate.getQuartile(25);
		Profile q75     = profileAggregate.getQuartile(75);
		this.addProfile(pointType, medians);
		this.addProfile(pointType+"25", q25);
		this.addProfile(pointType+"75", q75);
	}
	
	public String printKeys(){
		
		StringBuilder builder = new StringBuilder();

		builder.append("    Profiles:\t");
		for(String s : this.getProfileKeys()){
			builder.append("     "+s+"\t");
		}
		builder.append("    Aggregates:\t");
		for(String s : this.getAggregateKeys()){
			builder.append("     "+s+"\t");
		}
		builder.append("    Features:\t");
		for(String s : this.getFeatureKeys()){
			builder.append("     "+s+"\t");
		}
		builder.append("    Segments:\t");
		for(String s : this.getSegmentKeys()){
			builder.append("     "+s+"\t");
		}
		return builder.toString();
	}
	
//	public void printKeys(File file){
////		IJ.append("    Plots:", file.getAbsolutePath());
////		for(String s : this.getPlotKeys()){
////			IJ.append("     "+s, file.getAbsolutePath());
////		}
//		IJ.append("    Profiles:", file.getAbsolutePath());
//		for(String s : this.getProfileKeys()){
//			IJ.append("     "+s, file.getAbsolutePath());
//		}
//		IJ.append("    Aggregates:", file.getAbsolutePath());
//		for(String s : this.getAggregateKeys()){
//			IJ.append("     "+s, file.getAbsolutePath());
//		}
//		IJ.append("    Features:", file.getAbsolutePath());
//		for(String s : this.getFeatureKeys()){
//			IJ.append("     "+s, file.getAbsolutePath());
//		}
//		IJ.append("    Segments:", file.getAbsolutePath());
//		for(String s : this.getSegmentKeys()){
//			IJ.append("     "+s, file.getAbsolutePath());
//		}
//	}
	
	// Get keys
//	public Set<String> getPlotKeys(){
//		return plots.keySet();
//	}
	
	// get the profile keys without IQR headings
	public List<String> getProfileKeys(){
		List<String> result = new ArrayList<String>();
		for(String s : profiles.keySet()){
			if(!s.endsWith("5")){
				result.add(s);
			}
		}
		return result;
	}
	
	public Set<String> getProfileKeysPlusIQRs(){
		return profiles.keySet();
	}
	
	public Set<String> getAggregateKeys(){
		return aggregates.keySet();
	}
	
	public Set<String> getFeatureKeys(){
		return features.keySet();
	}
	
	public Set<String> getSegmentKeys(){
		return segments.keySet();
	}
	
	/**
	 * Turn the IQR (difference between Q25, Q75) of the median into a profile.
	 * @param pointType the profile type to use
	 * @return the profile
	 */
	public Profile getIQRProfile(String pointType){
		
		ProfileAggregate profileAggregate = this.getAggregate(pointType);
		
		return profileAggregate.getQuartile(75).subtract(profileAggregate.getQuartile(25));

//		double[] lowQuartiles    =  profileAggregate.getQuartile(25).asArray();
//		double[] uppQuartiles    =  profileAggregate.getQuartile(75).asArray();
//		
//		double[] iqr = new double[lowQuartiles.length];
//		for(int i=0;i<iqr.length;i++){
//			iqr[i] = uppQuartiles[i] - lowQuartiles[i]; 
//		}
//		return new Profile(iqr);
	}
	
	/**
	 * Find the points in the profile that are most variable
	 */
	public List<Integer> findMostVariableRegions(String pointType){
		
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
