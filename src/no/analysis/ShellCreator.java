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
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.RoiEnlarger;
import ij.process.FloatPolygon;
import ij.process.ImageStatistics;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.util.*;

import no.nuclei.*;
import no.utility.ImageImporter;
import no.utility.Utils;
import no.components.*;
import no.export.ImageExporter;

public class ShellCreator {

	int shellCount = 5;

	ImageStack image;
	Roi originalRoi;
	INuclearFunctions nucleus;

	double[] dapiDensities;
	double[] signalProportions;

	List<Roi> shells = new ArrayList<Roi>(0);

	/**
	*	Create an analyser on an image with a nucleus
	* ROI.
	*
	* @param nucleus the nucleus to analyse
	*/
	public ShellCreator(INuclearFunctions n){
//		this.originalRoi = n.getRoi();
		FloatPolygon polygon = Utils.createPolygon(n);
		originalRoi = new PolygonRoi(polygon, Roi.POLYGON);
		this.image = ImageImporter.convert(new ImagePlus(n.getOriginalImagePath()));
//		this.image = n.getImagePlanes();
		this.nucleus = n;
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

		ImagePlus searchImage = new ImagePlus(null, image.getProcessor(ImageImporter.COUNTERSTAIN).duplicate()); // blue channel
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
	public double[] findShell(NuclearSignal signal, int channel) throws Exception{

		// IJ.log(" Finding shells");

		Roi signalRoi = signal.getRoi();
//		IJ.log("    Signal ROI: "+signalRoi.getBounds().x+","+signalRoi.getBounds().y);
		
		// Get a list of all the points within the ROI
		List<XYPoint> signalPoints = getXYPoints(signalRoi);

		// now test each point for which shell it is in
		double[] signalDensities = getSignalDensities(signalPoints, channel);

		// find the proportion of signal within each shell
		this.signalProportions = getProportions(signalDensities);

		// normalise the signals to the dapi intensity
		double[] normalisedSignal = normalise(this.signalProportions, this.dapiDensities);

		if(new Double(normalisedSignal[0]).isNaN()){
			throw new Exception("Result is not a number");
		}
		return normalisedSignal;
	}

	
	/**
	 * Draw the shells on the nucleus, and export the image to the Nucleus folder.
	 */
	public void exportImage(){
	  ImagePlus shellImage = ImageExporter.convert(image);
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
                        Nucleus.IMAGE_PREFIX+
                        nucleus.getNucleusNumber()+
                        ".shells.tiff";
        IJ.saveAsTiff(shellImage, outPath);
      }
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
		if(signalPoints.size()==0){
			throw new IllegalArgumentException("No points found in ROI");
		}
		double[] result = new double[shellCount];

		int i=0;
		for( Roi r : shells){

			double density = 0;

			for(XYPoint p : signalPoints){

				if(r.contains(p.getXAsInt(), p.getYAsInt())){
					// find the value of the signal
					ImageProcessor ip = image.getProcessor(channel);
					density += (double) ip.getPixel(p.getXAsInt(), p.getYAsInt());	 
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
		if(new Double(counts[0]).isNaN()){
			throw new IllegalArgumentException("Not a number within ShellAnalyser.getProportions");
		}
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
				ImageProcessor ip = image.getProcessor(ImageImporter.COUNTERSTAIN);
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
		if(new Double(signals[0]).isNaN()){
			throw new IllegalArgumentException("Signal not a number within ShellAnalyser.normalise");
		}
		if(new Double(dapi[0]).isNaN()){
			throw new IllegalArgumentException("DAPI not a number within ShellAnalyser.normalise");
		}
		
		double[] norm = new double[shellCount];
		double total = 0;
//		String line = "";
		// perform the dapi normalisation, and get the signal total
		for(int i=0; i<shellCount; i++){
			norm[i] = signals[i] / dapi[i];
			total += norm[i];
//			line += norm[i]+"  ";
		}
//		IJ.log(line+"  "+total);

		// express the normalised signal as a fraction of the total
//		line = "";
		double[] result = new double[shellCount];
		for(int i=0; i<shellCount; i++){
			result[i] = norm[i] / total;
//			line += result[i]+"  ";
		}
//		IJ.log(line);
		return result;
	}

}