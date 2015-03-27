package no.components;

import ij.IJ;
import ij.gui.Plot;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.nuclei.INuclearFunctions;

public class ProfileCollection {
	
	private Map<String, ProfileFeature> 	features 	= new HashMap<String, ProfileFeature>();
	private Map<String, Profile> 			profiles 	= new HashMap<String, Profile>(0); 
	private Map<String, ProfileAggregate> 	aggregates 	= new HashMap<String, ProfileAggregate>();
	private Map<String, ProfilePlot> 		plots 		= new HashMap<String, ProfilePlot>();
	
	public ProfileCollection(){
		
	}
	
	// Get features
	
	public ProfileFeature getFeature(String s){
		return features.get(s);
	}
	
	public Profile getProfile(String s){
		if(profiles.containsKey(s)){	
			return profiles.get(s);
		} else {
			throw new IllegalArgumentException("The requested key does not exist");
		}
	}
	
	public ProfileAggregate getAggregate(String s){
		if(aggregates.containsKey(s)){	
			return aggregates.get(s);
		} else {
			throw new IllegalArgumentException("The requested key does not exist");
		}
	}
	
	public ProfilePlot getPlots(String s){
		if(plots.containsKey(s)){	
			return plots.get(s);
		} else {
			throw new IllegalArgumentException("The requested key does not exist");
		}		
	}
	
	// Add or update features
	
	public void addFeature(String s, ProfileFeature p){
		features.put(s, p);
	}
	
	public void addProfile(String s, Profile p){
		profiles.put(s, p);
	}
	
	public void addAggregate(String s, ProfileAggregate p){
		aggregates.put(s, p);
	}
	
	public void addPlots(String s, ProfilePlot p){
		plots.put(s, p);
	}
	
	
	public void createProfileAggregateFromPoint(String pointType, int length){

		ProfileAggregate profileAggregate = new ProfileAggregate(length);
		this.addAggregate(pointType, profileAggregate);
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

		for( String pointType : this.getProfileKeys() ){

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
	
//	public void exportProfilePlots(){
//
//	    for( String pointType : this.getPlotKeys() ){
//	    	
//	    	for(String s: this.getPlots(pointType).getTypes()){
//	    		this.getPlots(pointType).export(s, "plot"+pointType);
//	    		
//	    		 this.getFolder()+
//					File.separator+
//					this.getOutputFolder()+
//					File.separator+name+
//					"."+
//					this.getType()+".tiff"
//					
//					"plot"+pointType+"Norm"
//	    	}
//
//	      Plot normPlot = this.getPlots(pointType).get("norm");
//	      Plot  rawPlot = this.getPlots(pointType).get("raw" );
//
//	      normPlot.export(normPlot, "plot"+pointType+"Norm");
//	      exportProfilePlot(rawPlot , "plot"+pointType+"Raw");
//	    }  
//	  }
	
}
