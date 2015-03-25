package no.components;

import ij.IJ;
import ij.gui.Plot;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ProfileCollection {
	
	private Map<String, ProfileFeature> features = new HashMap<String, ProfileFeature>();
	private Map<String, Profile> profiles = new HashMap<String, Profile>(0); 
	private Map<String, ProfileAggregate> aggregates = new HashMap<String, ProfileAggregate>();
	private Map<String, ProfilePlot> plots = new HashMap<String, ProfilePlot>();
	
	public ProfileCollection(){
		
	}
	
	// Get features
	
	public ProfileFeature getFeature(String s){
		return features.get(s);
	}
	
	public Profile getProfile(String s){
		return profiles.get(s);
	}
	
	public ProfileAggregate getAggregate(String s){
		return aggregates.get(s);
	}
	
	public ProfilePlot getPlots(String s){
		return plots.get(s);
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
	
	public void printProfiles(){
		Set<String> keys = profiles.keySet();
		for(String s : keys){
			IJ.log("   "+s);
		}
	}
	
	// Get keys
	
	public Set<String> getPlotKeys(){
		return plots.keySet();
	}
	
	public Set<String> getProfileKeys(){
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

		Set<String> headings = this.getProfileKeys();
		for( String pointType : headings ){

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

		Set<String> headings = this.getPlotKeys();
		for( String pointType : headings ){

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
	
}
