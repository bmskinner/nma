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

import no.nuclei.INuclearFunctions;
import no.utility.Stats;
import no.utility.Utils;

public class ProfileCollection {
	
	private static final double CHART_TAIL_BOX_Y_MID = 340;
	private static final double CHART_TAIL_BOX_Y_MAX = 355;
	private static final double CHART_TAIL_BOX_Y_MIN = 325;
	
	private Map<String, ProfileFeature> 	features 	= new HashMap<String, ProfileFeature>();
	private Map<String, Profile> 			profiles 	= new HashMap<String, Profile>(0); 
	private Map<String, ProfileAggregate> 	aggregates 	= new HashMap<String, ProfileAggregate>();
	private Map<String, ProfilePlot> 		plots 		= new HashMap<String, ProfilePlot>();
	private String collectionName; // the name of the NucleusCollection - e.g analysable, red, not_red
	
	public ProfileCollection(String name){
		this.collectionName = name;
	}
	
	// Get features
	
	public ProfileFeature getFeature(String s){
		if(features.containsKey(s)){	
			return features.get(s);
		} else {
			throw new IllegalArgumentException("The requested feature key does not exist: "+s);
		}
	}
	
	public Profile getProfile(String s){
		if(profiles.containsKey(s)){	
			return profiles.get(s);
		} else {
			throw new IllegalArgumentException("The requested profile key does not exist: "+s);
		}
	}
	
	public ProfileAggregate getAggregate(String s){
		if(aggregates.containsKey(s)){	
			return aggregates.get(s);
		} else {
			throw new IllegalArgumentException("The requested aggregate key does not exist: "+s);
		}
	}
	
	public ProfilePlot getPlots(String s){
		if(plots.containsKey(s)){	
			return plots.get(s);
		} else {
			throw new IllegalArgumentException("The requested plot key does not exist: "+s);
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
	
	// Set up the plots within the collection

	public void preparePlots(int width, int height, double maxLength){
		List<String> keys = this.getProfileKeys();
		if(keys==null || keys.size()==0){
			keys.addAll(getAggregateKeys()); //backup
		}
		for( String pointType : keys ){

			Plot  rawPlot = new Plot( "Raw "       +pointType+"-indexed plot", "Position", "Angle", Plot.Y_GRID | Plot.X_GRID);
			Plot normPlot = new Plot( "Normalised "+pointType+"-indexed plot", "Position", "Angle", Plot.Y_GRID | Plot.X_GRID);

			rawPlot.setLimits(0,maxLength,-50,360);
			rawPlot.setSize(width,height);
			rawPlot.setYTicks(true);
			rawPlot.setColor(Color.BLACK);
			rawPlot.drawLine(0, 180, maxLength, 180); 
			rawPlot.setColor(Color.LIGHT_GRAY);

			normPlot.setLimits(0,100,-50,360);
			normPlot.setSize(width,height);
			normPlot.setYTicks(true);
			normPlot.setColor(Color.BLACK);
			normPlot.drawLine(0, 180, 100, 180); 
			normPlot.setColor(Color.LIGHT_GRAY);

			ProfilePlot plotHash = new ProfilePlot();
			plotHash.add("raw" , rawPlot );
			plotHash.add("norm", normPlot);
			this.addPlots(pointType, plotHash);
		}
	}   


	public void addMedianLinesToPlots(){

		for( String pointType : this.getPlotKeys() ){

			Plot plot = this.getPlots(pointType).get("norm");

			ProfileAggregate profileAggregate = this.getAggregate(pointType);

			double[] xmedians        =  profileAggregate.getXPositions().asArray();
			double[] ymedians        =  profileAggregate.getMedian().asArray();
			double[] lowQuartiles    =  profileAggregate.getQuartile(25).asArray();
			double[] uppQuartiles    =  profileAggregate.getQuartile(75).asArray();

			// add the median lines to the chart
			plot.setColor(Color.BLACK);
			plot.setLineWidth(3);
			plot.addPoints(xmedians, ymedians, Plot.LINE);

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
	
	// TODO: make this work with pointTypes
	public void drawProfilePlots(String pointType, List<Profile> profiles){

//		for( String pointType : this.getPlotKeys() ){

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
//		}   
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
	
}
