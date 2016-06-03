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
/*
  -----------------------
  SHELL ANALYSIS
  -----------------------
  Signal positions in round nuclei.
*/  
package analysis.signals;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.RoiEnlarger;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import io.ImageExporter;
import io.ImageImporter;
import logging.Loggable;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import utility.Constants;
import utility.StatsMap;
import utility.Utils;
import analysis.detection.Detector;
import components.generic.XYPoint;
import components.nuclear.NuclearSignal;
import components.nuclei.Nucleus;
import components.nuclei.RoundNucleus;

public class ShellCreator implements Loggable {

	int shellCount = 5;

	ImageStack 	nucleusStack; 	// the stack to work on 
	Roi 		nucleusRoi;		// the nuclear roi
	Nucleus 	nucleus;		// the nucleus

	double[] dapiDensities;
	double[] signalProportions;

	List<Roi> shells = new ArrayList<Roi>(0);

	/**
	*	Create an analyser on an image with a nucleus
	* ROI.
	*
	* @param nucleus the nucleus to analyse
	*/
	public ShellCreator(Nucleus n){

		this.nucleus = n;
		
		nucleusRoi = new PolygonRoi(n.createOriginalPolygon(), Roi.POLYGON);
		this.nucleusStack = ImageImporter.getInstance().importImage(n.getSourceFile());
		
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

		log(Level.FINE, "Creating shells");
		ImagePlus searchImage = new ImagePlus(null, nucleusStack.getProcessor(Constants.COUNTERSTAIN).duplicate()); // blue channel
		ImageProcessor ip = searchImage.getProcessor();
		
		Detector detector = new Detector();
		
		ImageProcessor nucleusIp = nucleusStack.getProcessor(Constants.COUNTERSTAIN);
		StatsMap values = detector.measure(nucleusRoi, nucleusIp);

		double initialArea = values.get("Area");
		
//		IJ.log("Nuclear area: "+initialArea);
//		double area = initialArea;

		// start with the entire nucleus, and shrink shell by shell
		for(int i=shellCount; i>0; i--){

			// take the original roi
			Roi shrinkingRoi = (Roi) nucleusRoi.clone();
			
			
			
			// get the maximum faction of the total area this
			// shell should occupy
			// i.e shell 5 of 5 is 1 (all area)
			double maxFraction = (double)i/(double)shellCount;
			
			// the max area for this shell
			double maxArea = initialArea * maxFraction;

			double area = initialArea;
			
//			IJ.log("Shell "+i+":  Max area: "+maxArea+" ("+maxFraction+")");
			
			while(area>maxArea){

//				Roi newRoi = new PolygonRoi(shrinkingPolygon, Roi.POLYGON);
				shrinkingRoi = RoiEnlarger.enlarge(shrinkingRoi, -1);
				
//				shrinkingRoi = RoiEnlarger.enlarge(shrinkingRoi, -1);
				ip.resetRoi();
				ip.setRoi(shrinkingRoi); 
//				ImageStatistics imgStats = ImageStatistics.getStatistics(ip, Measurements.AREA, searchImage.getCalibration()); 
//				area = imgStats.area;
				
				StatsMap statsValues = detector.measure(shrinkingRoi, nucleusIp);
				area =statsValues.get("Area");
				
//				shrinkingPolygon = newRoi.getFloatPolygon();
//				IJ.log("    Area: "+area);
			}
			
//			FloatPolygon shrinkingPolygon = shrinkingRoi.getFloatPolygon();
			FloatPolygon polygon = shrinkingRoi.getFloatPolygon();
			
			shells.add( new PolygonRoi(polygon, Roi.POLYGON));
		}

		// find the dapi density in each shell
		this.dapiDensities = findDapiDensities();
		searchImage.close();
	}

	/**
	*	Find the proportions of signal within each shell. 
	* createShells() must have been run.
	*
	* @param signal the signal to analyse
	* @return an array of signal proportions in each shell
	 * @throws Exception 
	*/
	public double[] findShell(NuclearSignal signal, int channel, ImageStack signalImage) throws Exception{

		FloatPolygon polygon = signal.createPolygon();
		Roi signalRoi = new PolygonRoi(polygon, Roi.POLYGON);
		
		// Get a list of all the points within the ROI
		List<XYPoint> signalPoints = getXYPoints(signalRoi);

		// initialise result as zerod array, in case
		// no signals are found
		double[] result = new double[shellCount];
		for(int i=0;i<shellCount;i++){
			result[i] = 0;
		}
		
		if(!signalPoints.isEmpty()){
			// now test each point for which shell it is in
			double[] signalDensities = getSignalDensities(signalPoints, channel, signalImage);

			// find the proportion of signal within each shell
			this.signalProportions = getProportions(signalDensities);

			// normalise the signals to the dapi intensity
			result = normalise(this.signalProportions, this.dapiDensities);

			if(new Double(result[0]).isNaN()){
				log(Level.SEVERE, "Result is not a number");
				throw new Exception("Result is not a number");
			}
		} else {
//			IJ.log("    Signal roi is empty");
		}
		return result;
	}

	
	/**
	 * Draw the shells on the nucleus, and export the image to the Nucleus folder.
	 */
	public void exportImage(){
	  ImagePlus shellImage = ImageExporter.getInstance().convertToRGB(nucleusStack);
      ImageProcessor ip = shellImage.getProcessor();
      List<Roi> shells = this.getShells();
      if(shells.size()>0){ // check we actually got shells out
        for(Roi r : shells){
          ip.setColor(Color.YELLOW);
          ip.setLineWidth(1);
          r.drawPixels(ip);
        }

        String outPath = nucleus.getNucleusFolder().getAbsolutePath()+
                        File.separator+
                        RoundNucleus.IMAGE_PREFIX+
                        nucleus.getNucleusNumber()+
                        ".shells.tiff";
        IJ.saveAsTiff(shellImage, outPath);
      }
	}
	
	/**
	*	Find the pixels within an roi. Create XYPoints
	*	and return as a list
	*
	* @param roi the ROI to convert
	* @return a list of XYPoints within the roi
	*/
	private List<XYPoint> getXYPoints(Roi roi){
	
		Rectangle roiBounds = roi.getBounds();
		
		FloatPolygon polygon = roi.getInterpolatedPolygon(0.5,true);
		
		// Get a list of all the points within the ROI
		List<XYPoint> result = new ArrayList<XYPoint>(0);
		
		// get the bounding box of the roi
		// make a list of all the pixels in the roi
		int minX = (int) roi.getXBase();
		int maxX = minX + (int) roiBounds.getWidth();
		
		int minY = (int)roi.getYBase();
		int maxY = minY + (int) roiBounds.getHeight();
		
		
//		IJ.log("    X base: "+minX
//				+"  Y base: "+minY
//				+"  X max: "+maxX
//				+"  Y max: "+maxY);
		
		for(int x=minX; x<=maxX; x++){
			for(int y=minY; y<=maxY; y++){
				
				if(polygon.contains(x, y)){
//					IJ.log(x+", "+y);
					result.add(new XYPoint(x, y));
				}
			}
		}
		
		if(result.isEmpty()){
//			IJ.log("    Roi has no pixels");
			log(Level.SEVERE, "No points found in roi");
			log(Level.FINE, "X base: "+minX
					+"  Y base: "+minY
					+"  X max: "+maxX
					+"  Y max: "+maxY);
		} else {
//			IJ.log("    Roi of area "+result.size());
		}
		return result;
	}

	/**
	* Find overlaps between a signal and shells. Returns a zerod 
	* array if nothing found
	*
	* @param signaPoints the list of XYPoints within the signal ROI
	* @return an array of signal densities per shell, outer to centre
	*/
	private double[] getSignalDensities(List<XYPoint> signalPoints, int channel, ImageStack signalImage){
		if(signalPoints.isEmpty()){
			throw new IllegalArgumentException("No points found in ROI");
		}
		
		int stackNumber = Constants.rgbToStack(channel);
		
		// create result array
		double[] result = new double[shellCount];

		try {
			for(int i=0;i<shellCount;i++){

				Roi roi = shells.get(i);
				double xbase = roi.getXBase();
				double ybase = roi.getYBase();
				
				double maxX = roi.getBounds().getWidth()+xbase;
				double maxY = roi.getBounds().getHeight()+ybase;
				
//				IJ.log("    Shell: X base: "+xbase
//						+"  Y base: "+ybase
//						+"  X max: "+maxX
//						+"  Y max: "+maxY);
				
				int density = 0;

				for(XYPoint p : signalPoints){
					
					int x = p.getXAsInt();
					int y = p.getYAsInt();
					
					if(roi.contains(x, y)){
						// find the value of the signal
//						IJ.log("    Roi contains pixel "+x+", "+y);
						ImageProcessor ip = signalImage.getProcessor(stackNumber);
						density += ip.getPixel(x, y);	 
//						IJ.log("    Stack  "+stackNumber+" (channel "+channel+"): "+ip.getPixel(x, y));
					} else {
//						IJ.log("    Roi does not contain pixel "+x+", "+y);
					}
				}
				result[i] = (double) density;
//				IJ.log("    Density in shell "+i+": "+density);
			}
		} catch (Exception e) {
			logError( "Error getting signal densities", e);
			
			// zero result
			for(int i=0;i<shellCount;i++){
				result[i] = 0;
			}
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
		if(new Double(counts[0]).isNaN()){
			log(Level.SEVERE, "Not a number within ShellAnalyser.getProportions");
			throw new IllegalArgumentException("Not a number within ShellAnalyser.getProportions");
		}
		
		double[] result = new double[shellCount];

		try {
			double total = 0; // the total number of pixels in the nucleus
			for(double d : counts){
				total+=d;
			}
			
			if(total==0){
				log(Level.FINE, "No pixels found when getting proportions");
			}

			// subtract inner from outer shells
			for(int i=0; i<shellCount; i++){

				// if this is the last shell, use the given number
				// otherwise, subtract from the shell above
				double realCount = i==shellCount-1 ? counts[i] : counts[i] - counts[i+1];
				
				result[i] 	= total==0 			 // if there are no pixels
							? 0					 // return a 0
							: realCount / total; // otherwise fraction of total pixels
//				IJ.log("    Proportion in shell "+i+": "+result[i]);
			}
		} catch (Exception e) {
			logError( "Error getting signal proportions", e);
			
			// zero result
			for(int i=0;i<shellCount;i++){
				result[i] = 0;
			}
		}
		return result;
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
				ImageProcessor ip = nucleusStack.getProcessor(Constants.COUNTERSTAIN);
				density += (double)ip.getPixel(p.getXAsInt(), p.getYAsInt());	 
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
		
		for(int i=0; i<shellCount; i++){
		
			if(new Double(signals[i]).isNaN()){
				log(Level.WARNING, "Signal is NaN: setting to zero");
				signals[i] = 0;
//				throw new IllegalArgumentException("Signal not a number within ShellAnalyser.normalise");
			}
			if(new Double(dapi[i]).isNaN()){
				log(Level.WARNING, "DAPI is NaN: setting to zero");
				dapi[i] = 0;
//				throw new IllegalArgumentException("DAPI not a number within ShellAnalyser.normalise");
			}
		}
		
		double[] norm = new double[shellCount];
		double total = 0;

		// perform the dapi normalisation, and get the signal total
		for(int i=0; i<shellCount; i++){
			if(dapi[i]==0){
				norm[i]=0;
			} else {
				norm[i] = signals[i] / dapi[i];
			}
			total += norm[i];
		}

		// express the normalised signal as a fraction of the total

		double[] result = new double[shellCount];
		for(int i=0; i<shellCount; i++){
			
			
			result[i] =  total==0 		 // if the total is 0
					  ? 0 				 // don't try dividing by 0
					  : norm[i] / total; // otherwise get the fraction of the total signal
		}

		return result;
	}

}