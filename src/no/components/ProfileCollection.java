package no.components;

import ij.IJ;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import no.utility.Utils;

public class ProfileCollection implements Serializable {
		
	private static final long serialVersionUID = 1L;
	public static final int CHART_WINDOW_HEIGHT     = 400;
	public static final int CHART_WINDOW_WIDTH      = 600;
	
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
	
	public void printKeys(){
//		IJ.log("    Plots:");
//		for(String s : this.getPlotKeys()){
//			IJ.log("     "+s);
//		}
		IJ.log("    Profiles:");
		for(String s : this.getProfileKeys()){
			IJ.log("     "+s);
		}
		IJ.log("    Aggregates:");
		for(String s : this.getAggregateKeys()){
			IJ.log("     "+s);
		}
		IJ.log("    Features:");
		for(String s : this.getFeatureKeys()){
			IJ.log("     "+s);
		}
		IJ.log("    Segments:");
		for(String s : this.getSegmentKeys()){
			IJ.log("     "+s);
		}
	}
	
	public void printKeys(File file){
//		IJ.append("    Plots:", file.getAbsolutePath());
//		for(String s : this.getPlotKeys()){
//			IJ.append("     "+s, file.getAbsolutePath());
//		}
		IJ.append("    Profiles:", file.getAbsolutePath());
		for(String s : this.getProfileKeys()){
			IJ.append("     "+s, file.getAbsolutePath());
		}
		IJ.append("    Aggregates:", file.getAbsolutePath());
		for(String s : this.getAggregateKeys()){
			IJ.append("     "+s, file.getAbsolutePath());
		}
		IJ.append("    Features:", file.getAbsolutePath());
		for(String s : this.getFeatureKeys()){
			IJ.append("     "+s, file.getAbsolutePath());
		}
		IJ.append("    Segments:", file.getAbsolutePath());
		for(String s : this.getSegmentKeys()){
			IJ.append("     "+s, file.getAbsolutePath());
		}
	}
	
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

		double[] lowQuartiles    =  profileAggregate.getQuartile(25).asArray();
		double[] uppQuartiles    =  profileAggregate.getQuartile(75).asArray();
		
		double[] iqr = new double[lowQuartiles.length];
		for(int i=0;i<iqr.length;i++){
			iqr[i] = uppQuartiles[i] - lowQuartiles[i]; 
		}
		return new Profile(iqr);
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
	
	// Set up the plots within the collection

//	public void preparePlots(int width, int height, double maxLength){
//		List<String> keys = this.getProfileKeys();
//		if(keys==null || keys.size()==0){
//			keys.addAll(getAggregateKeys()); //backup
//		}
//		for( String pointType : keys ){
//
//			Plot  rawPlot = new Plot( "Raw "       +pointType+"-indexed plot", "Position", "Angle", Plot.Y_GRID | Plot.X_GRID);
//			Plot normPlot = new Plot( "Normalised "+pointType+"-indexed plot", "Position", "Angle", Plot.Y_GRID | Plot.X_GRID);
//			Plot  iqrPlot = new Plot( "IQR "       +pointType+"-indexed plot", "Position", "IQR", Plot.Y_GRID | Plot.X_GRID);
//
//			rawPlot.setLimits(0,maxLength,CHART_SCALE_Y_MIN,CHART_SCALE_Y_MAX);
//			rawPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
//			rawPlot.setYTicks(true);
//			rawPlot.setColor(Color.BLACK);
//			rawPlot.drawLine(0, 180, maxLength, 180); 
//			rawPlot.setColor(Color.LIGHT_GRAY);
//
//			normPlot.setLimits(0,100,CHART_SCALE_Y_MIN,CHART_SCALE_Y_MAX);
//			normPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
//			normPlot.setYTicks(true);
//			normPlot.setColor(Color.BLACK);
//			normPlot.drawLine(0, 180, 100, 180); 
//			normPlot.setColor(Color.LIGHT_GRAY);
//			
//			iqrPlot.setLimits(0,100,0,50);
//			iqrPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
//			iqrPlot.setYTicks(true);
//			iqrPlot.setColor(Color.BLACK);
//			iqrPlot.setColor(Color.LIGHT_GRAY);
//
//			ProfilePlot plotHash = new ProfilePlot();
//			plotHash.add("raw" , rawPlot );
//			plotHash.add("norm", normPlot);
//			plotHash.add("iqr" , iqrPlot);
//			this.addPlots(pointType, plotHash);
//		}
//	}   


//	public void addSignalsToProfileChart(String pointType, List<XYPoint> signals, Color colour){
//		// setup the plot for getting signals
//		Plot plot = this.getPlots(pointType).get("norm");
//		plot.setColor(Color.LIGHT_GRAY);
//		plot.setLineWidth(1);
//		plot.drawLine(0,CHART_SIGNAL_Y_LINE_MIN,100,CHART_SIGNAL_Y_LINE_MIN);
//		plot.drawLine(0,CHART_SIGNAL_Y_LINE_MAX,100,CHART_SIGNAL_Y_LINE_MAX);
//
//		double[] xPoints = new double[signals.size()];
//		double[] yPoints = new double[signals.size()];
//
//		// turn the XYPoints into an array
//		for(int i= 0; i<signals.size();i++){
//			xPoints[i] = signals.get(i).getX();
//			yPoints[i] = signals.get(i).getY();
//		}
//		plot.setColor(colour);
//		plot.setLineWidth(2);
//		plot.addPoints( xPoints, yPoints, Plot.DOT);
//	}
//	
//	public void drawProfilePlots(String pointType, List<Profile> profiles){
//
//		Plot  rawPlot = this.getPlots(pointType).get("raw");
//		Plot normPlot = this.getPlots(pointType).get("norm");
//
//		for(int i=0;i<profiles.size();i++){
//
//			Profile p = profiles.get(i);
//
//			double[] xPointsRaw  = p.getPositions(p.size()).asArray();
//			double[] xPointsNorm = p.getPositions(100).asArray();
//
//			rawPlot.setColor(Color.LIGHT_GRAY);
//			rawPlot.addPoints(xPointsRaw, p.asArray(), Plot.LINE);
//
//			normPlot.setColor(Color.LIGHT_GRAY);
//			normPlot.addPoints(xPointsNorm, p.asArray(), Plot.LINE);
//		}
//
//	}

		


}
