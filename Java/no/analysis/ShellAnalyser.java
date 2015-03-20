/*
  -----------------------
  SHELL ANALYSIS
  -----------------------
  Signal positions in round nuclei.
*/  
package no.analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.gui.Roi;
import ij.io.Opener;
import ij.plugin.filter.Analyzer;
import ij.plugin.ChannelSplitter;
import ij.plugin.RoiEnlarger;
import ij.plugin.RGBStackMerge;
import ij.process.ImageStatistics;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.io.File;
import java.util.*;
import no.nuclei.*;
import no.utility.*;
import no.collections.*;
import no.components.*;

public class ShellAnalyser {

	int shellCount = 5;

	ImagePlus image;
	ImagePlus[] channels;
	Roi originalRoi;

	double[] dapiDensities;

	List<Roi> shells = new ArrayList<Roi>(0);

	/**
	*	Create an analyser on an image with a nucleus
	* ROI.
	*
	* @param nucleus the nucleus to analyse
	*/
	public ShellAnalyser(INuclearFunctions n){
		this.originalRoi = n.getRoi();
		this.image = n.getSourceImage();
		ChannelSplitter cs = new ChannelSplitter();
	  this.channels = cs.split(this.image);
	  IJ.log(" Prepping image: "+n.getNucleusNumber());
	}

	/**
	*	Set the number of shells
	*
	* @param i the number of shells
	*/
	public void setNumberOfShells(int i){
		this.shellCount = i;
	}

	public int getNumberOfShells(){
		return this.shellCount;
	}

	/**
	*	Divide the nucleus into shells of equal area. Number of
	* shells is 5 by default
	*/
	public void createShells(){

			IJ.log(" Creating shells");

	    ImagePlus searchImage = channels[Nucleus.BLUE_CHANNEL];
	    ImageProcessor ip = searchImage.getProcessor();

	    ImageStatistics stats = ImageStatistics.getStatistics(ip, Measurements.AREA, searchImage.getCalibration()); 
     	double initialArea = stats.area;

     	Roi shrinkingRoi = (Roi) originalRoi.clone();
     	RoiEnlarger enlarger = new RoiEnlarger();

     	double area = initialArea;

	    for(int i=shellCount; i>0; i--){

	    	IJ.log("   Shell "+i);
	    	IJ.log("   Initiial area: "+initialArea);
	    	IJ.log("   Area: "+area);

	    	while(area>initialArea* (i/shellCount)){
	    		shrinkingRoi = enlarger.enlarge(shrinkingRoi, -1);
	    		
	     		ip.setRoi(shrinkingRoi); 
	     		stats = ImageStatistics.getStatistics(ip, Measurements.AREA, searchImage.getCalibration()); 
	     		area = stats.area;
	     	}
	     	shells.add((Roi)shrinkingRoi.clone());
	    }

	    // find the dapi density in each shell
			this.dapiDensities = getDapiDensities();
			IJ.log(" Shells created");
	}

	/**
	*	Find the proportions of signal within each shell. 
	* createShells() must have been run.
	*
	* @param signal the signal to analyse
	* @return an array of signal proportions in each shell
	*/
	public double[] findShell(NuclearSignal signal, int channel){

		IJ.log(" Finding shells");

    Roi signalRoi = signal.getRoi();
    
    // Get a list of all the points within the ROI
    List<XYPoint> signalPoints = getSignalPoints(signalRoi);

	  // now test each point for which shell it is in
		double[] signalDensities = getSignalDensities(signalPoints, channel);

		// find the proportion of signal within each shell
		double[] proportions = getProportions(signalDensities);

		// normalise the signals to the dapi intensity
		double[] normalisedSignal = normalise(proportions, this.dapiDensities);

		return proportions;
	}

	/**
	*	Find the XYPoints within a signal ROI
	*
	* @param signaRoi the ROI to convert
	* @return a list of XYPoints within the roi
	*/
	private List<XYPoint> getSignalPoints(Roi signalRoi){
		Rectangle signalBounds = signalRoi.getBounds();

    // Get a list of all the points within the ROI
    List<XYPoint> signalPoints = new ArrayList<XYPoint>(0);
    for(int x=(int)signalBounds.getX(); x<signalBounds.getWidth()+signalBounds.getX(); x++){
    	for(int y=(int)signalBounds.getY(); y<signalBounds.getHeight()+signalBounds.getY(); y++){
    		if(signalRoi.contains(x, y)){
    			signalPoints.add(new XYPoint(x, y));
    		}
    	}
    }
    return signalPoints;
	}

	/**
	*	Find overlaps between a signal and shells
	*
	* @param signaPoints the list of XYPoints within the signal ROI
	* @return an array of signal densities per shell, outer to centre
	*/
	private double[] getSignalDensities(List<XYPoint> signalPoints, int channel){
		double[] result = new double[shellCount];

		int i=0;
		for( Roi r : shells){

			double density = 0;

			for(XYPoint p : signalPoints){

				if(r.contains(p.getXAsInt(), p.getYAsInt())){
					// find the value of the signal
					density += (double)channels[channel].getPixel(p.getXAsInt(), p.getYAsInt())[0];	 
				}
			}
			result[i] = density;
			i++;
		}
		return result;
	}

	/**
	*	Find the proportion of the total pixels within
	* each shell. Has to subtract inner shells.
	*
	* @param counts the number of pixels for each shell inwards
	* @param total the total number of pixels in the signal
	* @return a double[] with the fractions of signal per shell, outer to inner
	*/
	private double[] getProportions(double[] counts){
		double[] proportions = new double[shellCount];

		double total = 0;
		for(double d : counts){
			total+=d;
		}

		// subtract inner from outer shells
		for(int i=0; i<shellCount; i++){

			double realCount = i==shellCount-1 ? counts[i] : counts[i] - counts[i+1];
			proportions[i] = realCount / total; // fraction of total pixels
		}
		return proportions;
	}

	/**
	*	Find the DAPI density per shell
	*
	* @return a double[] with the DAPI density per shell, outer to inner
	*/
	private double[] getDapiDensities(){

		double[] densities = new double[shellCount];

		int i=0;
		for(Roi r : shells){
			channels[Nucleus.BLUE_CHANNEL].getProcessor().setRoi(r);	 
			ResultsTable rt = new ResultsTable();
			Analyzer analyser = new Analyzer( channels[Nucleus.BLUE_CHANNEL], Analyzer.INTEGRATED_DENSITY, rt);
			analyser.measure();
			densities[i] = rt.getValue("IntDen",0);
			i++;
		}

		double[] result = new double[shellCount];

		for(int j=0; j<shellCount; j++){
			result[j] = j==shellCount-1 ? densities[j] : densities[j] - densities[j+1];
		}
		return result;
	}

	/**
	*	Find the DAPI-normalised signal density per shell
	*
	* @return a double[] with the normalised signal density per shell, outer to inner
	*/
	private double[] normalise(double[] signals, double[] dapi){
		double[] result = new double[shellCount];
		for(int i=0; i<shellCount; i++){
			result[i] = signals[i] / dapi[i];
		}
		return result;
	}

}