/*
  -----------------------
  SHELL ANALYSIS
  -----------------------
  Signal positions in round nuclei.
*/  
package no.analysis;

import ij.ImagePlus;
import ij.measure.Measurements;
import ij.gui.Roi;
import ij.plugin.ChannelSplitter;
import ij.plugin.RoiEnlarger;
import ij.process.ImageStatistics;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.util.*;
import no.nuclei.*;
import no.components.*;

public class ShellAnalyser {

	int shellCount = 5;

	ImagePlus image;
	ImagePlus[] channels;
	Roi originalRoi;

	double[] dapiDensities;
	double[] signalProportions;

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
//		ChannelSplitter cs = new ChannelSplitter();
		this.channels = ChannelSplitter.split(this.image);
	}

	/**
	*	Set the number of shells
	*
	* @param i the number of shells
	*/
	public void setNumberOfShells(int i){
		this.shellCount = i;
	}

	/**
	*	Get the number of shells
	*
	* @return the number of shells
	*/
	public int getNumberOfShells(){
		return this.shellCount;
	}

	/**
	*	Get the DAPI densities in each shell
	*
	* @return DAPI density per shell, outer to inner
	*/
	public double[] getDapiDensities(){
		return this.dapiDensities;
	}

	/**
	*	Get the proportion of total signal per shell
	*	before DAPI normalisation
	*
	* @return signal density per shell, outer to inner
	*/
	public double[] getSignalProportions(){
		return this.signalProportions;
	}

	/**
	*	Get the ROIs for the shells
	*
	* @return list of ROIs
	*/
	public List<Roi> getShells(){
		return this.shells;
	}

	/**
	*	Divide the nucleus into shells of equal area. Number of
	* shells is 5 by default. Use setNumberOfShells to change.
	*/
	public void createShells(){

		ImagePlus searchImage = channels[Nucleus.BLUE_CHANNEL];
		ImageProcessor ip = searchImage.getProcessor();

		ImageStatistics stats = ImageStatistics.getStatistics(ip, Measurements.AREA, searchImage.getCalibration()); 
		double initialArea = stats.area;
		

		double area = initialArea;

		for(int i=shellCount; i>0; i--){

//			RoiEnlarger enlarger = new RoiEnlarger();
			Roi shrinkingRoi = (Roi) originalRoi.clone();

			double maxArea = initialArea * ((double)i/(double)shellCount);

			while(area>maxArea){

				shrinkingRoi = RoiEnlarger.enlarge(shrinkingRoi, -1);
				ip.resetRoi();
				ip.setRoi(shrinkingRoi); 
				stats = ImageStatistics.getStatistics(ip, Measurements.AREA, searchImage.getCalibration()); 
				area = stats.area;
			}
			shells.add((Roi)shrinkingRoi.clone());
		}

		// find the dapi density in each shell
		this.dapiDensities = findDapiDensities();
	}

	/**
	*	Find the proportions of signal within each shell. 
	* createShells() must have been run.
	*
	* @param signal the signal to analyse
	* @return an array of signal proportions in each shell
	*/
	public double[] findShell(NuclearSignal signal, int channel){

		// IJ.log(" Finding shells");

		Roi signalRoi = signal.getRoi();
		
		// Get a list of all the points within the ROI
		List<XYPoint> signalPoints = getXYPoints(signalRoi);

		// now test each point for which shell it is in
		double[] signalDensities = getSignalDensities(signalPoints, channel);

		// find the proportion of signal within each shell
		this.signalProportions = getProportions(signalDensities);

		// normalise the signals to the dapi intensity
		double[] normalisedSignal = normalise(this.signalProportions, this.dapiDensities);

		return normalisedSignal;
	}

	/**
	*	Find the XYPoints within an ROI
	*
	* @param signaRoi the ROI to convert
	* @return a list of XYPoints within the roi
	*/
	private List<XYPoint> getXYPoints(Roi roi){
	
		Rectangle roiBounds = roi.getBounds();

		// Get a list of all the points within the ROI
		List<XYPoint> roiPoints = new ArrayList<XYPoint>(0);
		for(int x=(int)roiBounds.getX(); x<roiBounds.getWidth()+roiBounds.getX(); x++){
			for(int y=(int)roiBounds.getY(); y<roiBounds.getHeight()+roiBounds.getY(); y++){
				if(roi.contains(x, y)){
					roiPoints.add(new XYPoint(x, y));
				}
			}
		}
		return roiPoints;
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
	private double[] findDapiDensities(){

		double[] densities = new double[shellCount];

		int i=0;
		for(Roi r : shells){

			List<XYPoint> points = getXYPoints(r);
			double density = 0;

			for(XYPoint p : points){
				// find the value of the signal
				density += (double)channels[Nucleus.BLUE_CHANNEL].getPixel(p.getXAsInt(), p.getYAsInt())[0];	 
			}
			densities[i] = density;
			i++;
		}

		// correct for the shells stacking
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
		
		double[] norm = new double[shellCount];
		double total = 0;
		// perform the dapi normalisation, and get the signal total
		for(int i=0; i<shellCount; i++){
			norm[i] = signals[i] / dapi[i];
			total += norm[i];
		}

		// express the normalised signal as a fraction of the total
		double[] result = new double[shellCount];
		for(int i=0; i<shellCount; i++){
			result[i] = norm[i] / total;
		}
		return result;
	}

}