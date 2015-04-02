package no.components;

import ij.IJ;
import ij.gui.Plot;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.analysis.ProfileSegmenter;
import no.utility.Stats;

public class ProfileCollection {
	
	private static final int CHART_TAIL_BOX_Y_MID = 340;
	private static final int CHART_TAIL_BOX_Y_MAX = 355;
	private static final int CHART_TAIL_BOX_Y_MIN = 325;
	private static final int CHART_SIGNAL_Y_LINE_MIN = 275;
	private static final int CHART_SIGNAL_Y_LINE_MAX = 315;
	
	private static final int CHART_SCALE_Y_MIN = 0;
	private static final int CHART_SCALE_Y_MAX = 360;
	
	public static final int CHART_WINDOW_HEIGHT     = 400;
	public static final int CHART_WINDOW_WIDTH      = 600;
	
	private Map<String, ProfileFeature> 	features 	= new HashMap<String, ProfileFeature>();
	private Map<String, Profile> 			profiles 	= new HashMap<String, Profile>(0); 
	private Map<String, ProfileAggregate> 	aggregates 	= new HashMap<String, ProfileAggregate>();
	private Map<String, ProfilePlot> 		plots 		= new HashMap<String, ProfilePlot>();
	private String collectionName; // the name of the NucleusCollection - e.g analysable, red, not_red
	
	private Map<String, List<NucleusBorderSegment>> segments 	= new HashMap<String, List<NucleusBorderSegment>>();
		
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
	
	public ProfilePlot getPlots(String s){
		if(s==null){
			throw new IllegalArgumentException("The requested plot key is null: "+s);
		}
		if(plots.containsKey(s)){	
			return plots.get(s);
		} else {
			throw new IllegalArgumentException("The requested plot key does not exist: "+s);
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
	
	public void addPlots(String s, ProfilePlot p){
		if(s==null || p==null){
			throw new IllegalArgumentException("String or Profile is null");
		}
		plots.put(s, p);
	}
	
	public void addSegments(String s, List<NucleusBorderSegment> n){
		if(s==null || n==null || n.isEmpty()){
			throw new IllegalArgumentException("String or segment list is null or empty");
		}
		segments.put(s, n);
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
		IJ.log("    Plots:");
		for(String s : this.getPlotKeys()){
			IJ.log("     "+s);
		}
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
	
	// Get keys
	public Set<String> getPlotKeys(){
		return plots.keySet();
	}
	
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
	private Profile getIQRProfile(String pointType){
		
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
		Profile maxima = iqrProfile.smooth(3).getLocalMaxima(3);
		Profile displayMaxima = maxima.multiply(50);
		
		// given the list of maxima, find the highest 3 regions
		// store the rank (1-3) and the index of the position at this rank
		// To future me: I am sorry about this.
		Map<Integer, Integer> values = new HashMap<Integer, Integer>(0);
		values.put(1, 0);
		values.put(2, 0);
		values.put(3, 0);
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
		}
		
		// draw the IQR - only needed during debugging. Can be removed later.
		Plot  plot = this.getPlots(pointType).get("iqr");
		double[] xPoints  = iqrProfile.getPositions(100).asArray();
		plot.setColor(Color.DARK_GRAY);
		plot.addPoints(xPoints, iqrProfile.asArray(), Plot.LINE);
		plot.setColor(Color.LIGHT_GRAY);
		plot.addPoints(xPoints, iqrProfile.smooth(3).asArray(), Plot.LINE);
		plot.setColor(Color.DARK_GRAY);
		for(int i=0;i<maxima.size();i++){
			double x = xPoints[i];
			if(maxima.get(i)==1){
				plot.drawLine(x, 0, x, iqrProfile.get(i) );
			}
		}
		plot.setColor(Color.RED);
		for(int i : values.keySet()){
			int index = values.get(i);
			double x = xPoints[index];
			plot.drawLine(x, iqrProfile.get(index), x, displayMaxima.get(index));
		}
		// end of stuff that can be removed
		
		
		return result;
	}
	
	// Set up the plots within the collection

	public void preparePlots(int width, int height, double maxLength){
		List<String> keys = this.getProfileKeys();
		if(keys==null || keys.size()==0){
			keys.addAll(getAggregateKeys()); //backup
		}
		for( String pointType : keys ){

			Plot  rawPlot = new Plot( "Raw "       +pointType+"-indexed plot", "Position", "Angle", Plot.Y_GRID | Plot.X_GRID);
			Plot normPlot = new Plot( "Normalised "+pointType+"-indexed plot", "Position", "Angle", Plot.Y_GRID | Plot.X_GRID);
			Plot  iqrPlot = new Plot( "IQR "       +pointType+"-indexed plot", "Position", "IQR", Plot.Y_GRID | Plot.X_GRID);

			rawPlot.setLimits(0,maxLength,CHART_SCALE_Y_MIN,CHART_SCALE_Y_MAX);
			rawPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
			rawPlot.setYTicks(true);
			rawPlot.setColor(Color.BLACK);
			rawPlot.drawLine(0, 180, maxLength, 180); 
			rawPlot.setColor(Color.LIGHT_GRAY);

			normPlot.setLimits(0,100,CHART_SCALE_Y_MIN,CHART_SCALE_Y_MAX);
			normPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
			normPlot.setYTicks(true);
			normPlot.setColor(Color.BLACK);
			normPlot.drawLine(0, 180, 100, 180); 
			normPlot.setColor(Color.LIGHT_GRAY);
			
			iqrPlot.setLimits(0,100,0,50);
			iqrPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
			iqrPlot.setYTicks(true);
			iqrPlot.setColor(Color.BLACK);
			iqrPlot.setColor(Color.LIGHT_GRAY);

			ProfilePlot plotHash = new ProfilePlot();
			plotHash.add("raw" , rawPlot );
			plotHash.add("norm", normPlot);
			plotHash.add("iqr" , iqrPlot);
			this.addPlots(pointType, plotHash);
		}
	}   


	public void addMedianLinesToPlots(){

		for( String pointType : this.getPlotKeys() ){

			Plot plot = this.getPlots(pointType).get("norm");

			ProfileAggregate profileAggregate = this.getAggregate(pointType);

			double[] xmedians        =  profileAggregate.getXPositions().asArray();
//			double[] ymedians        =  profileAggregate.getMedian().asArray();
			double[] lowQuartiles    =  profileAggregate.getQuartile(25).asArray();
			double[] uppQuartiles    =  profileAggregate.getQuartile(75).asArray();

			// add the median lines to the chart
//			plot.setColor(Color.BLACK);
//			plot.setLineWidth(3);
//			plot.addPoints(xmedians, ymedians, Plot.LINE);
			this.appendSegmentsToPlot(	this.getPlots(pointType).get("norm"), 
					this.getProfile(pointType),
					this.getSegments(pointType));

			// add the IQR
			plot.setColor(Color.DARK_GRAY);
			plot.setLineWidth(2);
			plot.addPoints(xmedians, lowQuartiles, Plot.LINE);
			plot.addPoints(xmedians, uppQuartiles, Plot.LINE);
	    }
	}

	
	public void addBoxplot(String pointType, List<Double> indexes){

		double[] xPoints = new double[indexes.size()];
		for(int i= 0; i<indexes.size();i++){
			xPoints[i] = indexes.get(i);
		}
		// get the tail positions with the head offset applied
		double[] yPoints = new double[xPoints.length];
		Arrays.fill(yPoints, CHART_TAIL_BOX_Y_MID); // all dots at y=CHART_TAIL_BOX_Y_MID

		Plot plot = this.getPlots(pointType).get("norm");
		plot.setColor(Color.LIGHT_GRAY);
		plot.addPoints(xPoints, yPoints, Plot.DOT);

		// median tail positions
		double tailQ50 = Stats.quartile(xPoints, 50);
		double tailQ25 = Stats.quartile(xPoints, 25);
		double tailQ75 = Stats.quartile(xPoints, 75);

		plot.setColor(Color.DARK_GRAY);
		plot.setLineWidth(1);
		plot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MAX, tailQ75, CHART_TAIL_BOX_Y_MAX);
		plot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MIN, tailQ75, CHART_TAIL_BOX_Y_MIN);
		plot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MIN, tailQ25, CHART_TAIL_BOX_Y_MAX);
		plot.drawLine(tailQ75, CHART_TAIL_BOX_Y_MIN, tailQ75, CHART_TAIL_BOX_Y_MAX);
		plot.drawLine(tailQ50, CHART_TAIL_BOX_Y_MIN, tailQ50, CHART_TAIL_BOX_Y_MAX);
	}
	

	public void addSignalsToProfileChart(String pointType, List<XYPoint> signals, Color colour){
		// setup the plot for getting signals
		Plot plot = this.getPlots(pointType).get("norm");
		plot.setColor(Color.LIGHT_GRAY);
		plot.setLineWidth(1);
		plot.drawLine(0,CHART_SIGNAL_Y_LINE_MIN,100,CHART_SIGNAL_Y_LINE_MIN);
		plot.drawLine(0,CHART_SIGNAL_Y_LINE_MAX,100,CHART_SIGNAL_Y_LINE_MAX);

		double[] xPoints = new double[signals.size()];
		double[] yPoints = new double[signals.size()];

		// turn the XYPoints into an array
		for(int i= 0; i<signals.size();i++){
			xPoints[i] = signals.get(i).getX();
			yPoints[i] = signals.get(i).getY();
		}
		plot.setColor(colour);
		plot.setLineWidth(2);
		plot.addPoints( xPoints, yPoints, Plot.DOT);
	}
	
	public void drawProfilePlots(String pointType, List<Profile> profiles){

		Plot  rawPlot = this.getPlots(pointType).get("raw");
		Plot normPlot = this.getPlots(pointType).get("norm");

		for(int i=0;i<profiles.size();i++){

			Profile p = profiles.get(i);

			double[] xPointsRaw  = p.getPositions(p.size()).asArray();
			double[] xPointsNorm = p.getPositions(100).asArray();

			rawPlot.setColor(Color.LIGHT_GRAY);
			rawPlot.addPoints(xPointsRaw, p.asArray(), Plot.LINE);

			normPlot.setColor(Color.LIGHT_GRAY);
			normPlot.addPoints(xPointsNorm, p.asArray(), Plot.LINE);
		}

	}

	public void exportProfilePlots(String folder, String nucleusCollectionType){

		for( String pointType : this.getPlotKeys() ){

			for(String key: this.getPlots(pointType).getKeys()){
				String filename = folder+
						File.separator+
						"plot."+
						pointType+
						"."+
						this.collectionName+
						"."+
						nucleusCollectionType;
				this.getPlots(pointType).export(key, filename);

			}
		}  
	}
	
	public void segmentProfiles(){
		for( String pointType : this.getProfileKeys() ){
			Profile medianToCompare = this.getProfile(pointType);

			ProfileSegmenter segmenter = new ProfileSegmenter(medianToCompare);		  
			List<NucleusBorderSegment> segments = segmenter.segment();

			IJ.log("    Found "+segments.size()+" segments in "+pointType+" profile");
			this.addSegments(pointType, segments);
//			this.appendSegmentsToPlot(	this.getPlots(pointType).get("norm"), 
//										this.getProfile(pointType),
//										segments);
		}
	}
	
	public void appendSegmentsToPlot(Plot segPlot, Profile profile, List<NucleusBorderSegment> segments){

		int narrowLine = 2;
		int wideLine = 10;
		int verticalLine = 2;

		int baseLineY = 5;

		// draw 180 degree line
//		segPlot.setLineWidth(narrowLine);
//		segPlot.setColor(Color.DARK_GRAY);
////		segPlot.drawLine(0, 180, profile.size(),180);			

		
		double[] xpoints = profile.getPositions(100).asArray();
		double[] ypoints = profile.asArray();
		// draw the background black median line for contrast
//		segPlot.setLineWidth(wideLine);
//		segPlot.addPoints(xpoints, ypoints, Plot.LINE);
//		segPlot.setLineWidth(narrowLine);

		// draw the coloured segments
		int i=0;
		for(NucleusBorderSegment b : segments){

			//			IJ.log("Segment "+i);
			//			b.print();

			segPlot.setLineWidth(4);
			if(i==0 && segments.size()==ProfileSegmenter.colourList.size()+1){ // avoid colour wrapping when segment number is 1 more than the colour list
				segPlot.setColor(Color.MAGENTA);
			} else{
				segPlot.setColor(ProfileSegmenter.getColor(i));
			}

			//			IJ.log("    Colour: "+getColor(i));
			if(b.getStartIndex()<b.getEndIndex()){

				// draw the coloured line at the base of the plot
				segPlot.drawLine(xpoints[b.getStartIndex()], baseLineY, xpoints[b.getEndIndex()], baseLineY);
				//				IJ.log("    Line from "+b.getStartIndex()+" to "+b.getEndIndex());

				// draw the section of the profile
				double[] xPart = Arrays.copyOfRange(xpoints, b.getStartIndex(), b.getEndIndex());
				double[] yPart = Arrays.copyOfRange(ypoints, b.getStartIndex(), b.getEndIndex());
				segPlot.setLineWidth(narrowLine);
				segPlot.addPoints(xPart, yPart, Plot.LINE);
				segPlot.setLineWidth(4);

			} else { // handle wrap arounds
				segPlot.drawLine(0, baseLineY, xpoints[b.getEndIndex()], baseLineY);
				segPlot.drawLine(xpoints[b.getStartIndex()], baseLineY, 100, baseLineY);
				//				IJ.log("    Line from 0 to "+b.getEndIndex()+" and "+b.getStartIndex()+" to "+profile.size());

				double[] xPart = Arrays.copyOfRange(xpoints, b.getStartIndex(), profile.size()-1);
				double[] yPart = Arrays.copyOfRange(ypoints, b.getStartIndex(), profile.size()-1);
				segPlot.setLineWidth(narrowLine);
				segPlot.addPoints(xPart, yPart, Plot.LINE);
				xPart = Arrays.copyOfRange(xpoints, 0, b.getEndIndex());
				yPart = Arrays.copyOfRange(ypoints, 0, b.getEndIndex());
				segPlot.addPoints(xPart, yPart, Plot.LINE);
				segPlot.setLineWidth(4);

			}
			// draw the vertical lines
			segPlot.setColor(Color.LIGHT_GRAY);
			segPlot.setLineWidth(verticalLine);
			segPlot.drawLine(xpoints[b.getStartIndex()], 0, xpoints[b.getStartIndex()],360);			
			segPlot.setLineWidth(1);
			i++;
		}				
	}

}
