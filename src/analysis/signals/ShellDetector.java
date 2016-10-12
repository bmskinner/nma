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

import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.RoiEnlarger;
import ij.process.ImageProcessor;
import io.ImageImporter;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


//import stats.Area;
import stats.NucleusStatistic;
import utility.Constants;
import analysis.detection.Detector;
import components.CellularComponent;
import components.generic.XYPoint;
import components.nuclear.NuclearSignal;

/**
 * @author bms41
 *
 */
public class ShellDetector extends Detector {

	public static final int DEFAULT_SHELL_COUNT = 5;

//	private ImageStack 	nucleusStack; 	// the stack to work on 
	private Roi 		nucleusRoi;		// the nuclear roi

	
	/**
	 * The shell ROIs within the template object.
	 * This list begins with the largest shell (index 0)
	 * and ends with the smallest shell. The larger shells
	 * include the area contained within smaller shells.
	 */
	private List<Shell> shells = new ArrayList<Shell>(0);

	/**
	* Create shells in the given nucleus, using the
	* default shell count
	* @param nucleus the nucleus to analyse
	*/
	public ShellDetector(CellularComponent n){

		this(n, ShellDetector.DEFAULT_SHELL_COUNT);
	}
	
	/**
	 * Create shells in the given nucleus, using the
	 * given shell count
	 *
	 * @param nucleus the nucleus to analyse
	 */
	public ShellDetector(CellularComponent n, int shellCount){


		nucleusRoi = new PolygonRoi(n.createOriginalPolygon(), Roi.POLYGON);

		
		this.createShells(n, shellCount);

	}
	
	public List<Shell> getShells(){
		return this.shells;
	}
	
	/**
	 * Find the shell in the template object that the given point
	 * lies within, or -1 if the point is not found
	 * @param p
	 * @return
	 */
	public int findShell(XYPoint p){
		
		int shell = -1;
		for(Shell r : shells){
			if(r.contains(p.getXAsInt(), p.getYAsInt())){
				shell++;
			}
		}
		return shell;
	}
	
	/**
	 * Find the number of pixels per shell within the signal
	 * @param signal
	 * @return
	 */
	public int[] findPixelIntensityPerShell(NuclearSignal signal){
		int[] signalDensities = getSignalIntensities(signal);
		return signalDensities;
	}
	
	/**
	 * Find the number pixels of the signal within each shell
	 * @param signal
	 * @return
	 */
	public int[] findPixelCountPerShell(NuclearSignal signal){
		int[] counts = makeZeroArray();
		
		for(int i=0; i<shells.size(); i++){

			Shell shell = shells.get(i);
			int count = shell.getCount(signal);
			counts[i] = count;

		}
		
		counts = correctNestedIntensities(counts);
		return counts;
	}

	/**
	 * Find the proportions of signal within each shell, normalised against
	 * the DAPI density. Note that this will return values for the cell as a 
	 * whole, not for the individual signal
	 *
	 * @param signal the signal to analyse
	 * @return an array of signal proportions in each shell
	 * @throws Exception 
	 */
	public double[] findProportionPerShell(NuclearSignal signal) throws Exception{

		// Get the pixel intensities per shell for signal channel
//		int[] signalDensities = getSignalIntensities(signal);
		
		int[] signalDensities = getChannelIntensities(signal, signal.getChannel());
				
		// Get the pixel intensities per shell for the dapi channel
		int[] dapiDensities   = getChannelIntensities(signal, Constants.RGB_BLUE);


		// find the proportion of the total signal within each shell
		double[] signalProportions = getProportions(signalDensities);

		// normalise the signals to the dapi intensity
		double[] result = normalise(signalProportions, dapiDensities);

		return result;
	}

	
	/*
	 * PROTECTED AND PRIVATE METHODS
	 * 
	 */
	
	/**
	*	Get the intensities in each shell for the given channel. Correct
	* for nested shells by removing the total for inner shells from outer
	* shells
	*
	* @return Intensity per shell, outer to inner
	*/
	private int[] getChannelIntensities(NuclearSignal signal, int channel){
		int[] result = makeZeroArray();

		// find the total signal in the signal channel
		for(int i=0; i<shells.size(); i++){

			Shell shell = shells.get(i);
			int density = shell.getDensity(signal, channel);
			result[i] = density;

		}
		
		// Correct for nested shells
		result = correctNestedIntensities(result);
		
		return result;
	}
	
	/**
	*	Get the intensities in each shell for the given channel. Correct
	* for nested shells by removing the total for inner shells from outer
	* shells
	*
	* @return Intensity per shell, outer to inner
	*/
	private int[] getSignalIntensities(NuclearSignal signal){
		int[] result = makeZeroArray();

		// find the total signal in the signal channel
		for(int i=0; i<shells.size(); i++){

			Shell shell = shells.get(i);
			int density = shell.getSignalDensity(signal);
			result[i] = density;

		}
		
		// Correct for nested shells
		result = correctNestedIntensities(result);
		
		return result;
	}
	
	private int[] correctNestedIntensities(int[] array){

		if(array.length==0){
			throw new IllegalArgumentException("Array length is zero");
		}
		
		int innerShellTotal = 0;
		
		for(int i=shells.size()-1; i>=0; i--){
			
			int shellTotal = array[i];
			int corrected  = shellTotal - innerShellTotal;
			array[i] = corrected;
			innerShellTotal = shellTotal;
		}
		return array;
	}
	
	/**
	 * Create an array with shellCount entries, each set to 0
	 * @return
	 */
	private int[] makeZeroArray(){
		int[] result = new int[shells.size()];
		for(int i=0;i<shells.size();i++){
			result[i] = 0;
		}
		return result;
	}
	
	private double[] makeZeroDoubleArray(){
		double[] result = new double[shells.size()];
		for(int i=0;i<shells.size();i++){
			result[i] = 0;
		}
		return result;
	}
	
	
	/**
	*	Divide the nucleus into shells of equal area. Number of
	* shells is 5 by default. Use setNumberOfShells to change.
	*/
	private void createShells(CellularComponent c, int shellCount){
				
		
		fine("Creating shells");

//		double initialArea = c.getStatistic(NucleusStatistic.AREA); 
		
//		log("By stat: "+initialArea);
		double initialArea = new stats.Area(nucleusRoi).doubleValue();
//		log("By calc: "+initialArea);
		
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
						
			while(area>maxArea){

				shrinkingRoi = RoiEnlarger.enlarge(shrinkingRoi, -1);

				area = new stats.Area(shrinkingRoi).doubleValue();

			}

			Polygon polygon = shrinkingRoi.getPolygon();
			Roi r = new PolygonRoi(polygon, Roi.POLYGON);
			
			shells.add( new Shell(r) );
		}

	}

	/**
	*	Find the proportion of the total pixels within
	* each shell. Has to subtract inner shells.
	*
	* @param counts the number of pixels for each shell inwards
	* @param total the total number of pixels in the signal
	* @return a double[] with the fractions of signal per shell, outer to inner
	*/
	private double[] getProportions(int[] counts){
		
		if(counts.length==0){
			throw new IllegalArgumentException("Array length is zero");
		}
		
		int total = 0; // the total number of pixels in the nucleus
		for(int d : counts){
			total+=d;
		}
		
		double[] result = makeZeroDoubleArray();
		
		if(total==0){
			fine( "No pixels found when getting proportions");
			return result;
		}

		for(int i=0; i<counts.length; i++){
			result[i] = (double) counts[i] / (double) total;

		}

		return result;
	}

	/**
	 *	Find the DAPI-normalised signal density per shell
	 *
	 * @return a double[] with the normalised signal density per shell, outer to inner
	 */
	private double[] normalise(double[] signals, int[] dapi){

		if(signals.length != dapi.length){
			throw new IllegalArgumentException("Array lengths are not equal");
		}
		
		if(signals.length==0){
			throw new IllegalArgumentException("Array length is zero");
		}
		
		for(int i=0; i<signals.length; i++){

			if(new Double(signals[i]).isNaN()){
				warn("Signal is NaN: setting to zero");
				signals[i] = 0;
			}
			if(new Double(dapi[i]).isNaN()){
				warn("DAPI is NaN: setting to zero");
				dapi[i] = 0;
			}
		}

		double[] norm = makeZeroDoubleArray();
		double total = 0;

		// perform the dapi normalisation, and get the signal total
		for(int i=0; i<signals.length; i++){
			if(dapi[i]==0){
				norm[i]=0;
			} else {
				norm[i] = signals[i] / dapi[i];
			}
			total += norm[i];
		}

		// re-express  the normalised signal as a fraction of the total

		double[] result = new double[shells.size()];
		for(int i=0; i<shells.size(); i++){


			result[i] =  total==0 		 // if the total is 0
                      ? 0 				 // don't try dividing by 0
                      : norm[i] / total; // otherwise get the fraction of the total signal
		}

		return result;
	}
	
	public class Shell {
		
		/**
		 * The roi of the shell at the original position in the source image of the component
		 */
		private Roi r;
		
		public Shell(Roi r){
			this.r = r;
		}
		
		/**
		 * Test if this shell contains the given pixel
		 * @param x
		 * @param y
		 * @return
		 */
		public boolean contains(int x, int y){
			return r.contains(x, y);
		}
		
		/**
		 * Count the number of pixels within the signal that are
		 * also within this shell.
		 * @param s
		 * @param channel
		 * @return
		 */
		public int getCount(NuclearSignal s){
			
			Area signalArea = new Area(s.toOriginalShape());
			Area shellArea  = this.toArea();
			
			// Keep pixels that are in both shapes
			signalArea.intersect(shellArea);

			int count = new stats.Area(signalArea).intValue();

			return count;
		}
		
		/**
		* Find the sum of all pixel intensities within this shell 
		* from the given channel.
		*
		*@param s the signal with the image to measure
		* @param channel the channel to measure
		* @return the sum of intensities in the shell
		*/
		public int getDensity(NuclearSignal s, int channel){

			List<XYPoint> signalPoints = this.getPixelsAsPoints();

			if(signalPoints.isEmpty()){
				return 0;
			}

			ImageStack st = new ImageImporter(s.getSourceFile()).importImage();
			int stackNumber = Constants.rgbToStack(channel);
			ImageProcessor ip = st.getProcessor(stackNumber);




			// create result array
			int result = 0;

			try {

				int density = 0;

				for(XYPoint p : signalPoints){

					int x = p.getXAsInt();
					int y = p.getYAsInt();

					if(r.contains(x, y)){
						// find the value of the signal
						density += ip.getPixel(x, y);	 
					}
				}
				
				result = density;

			} catch (Exception e) {
				error( "Error getting signal densities", e);

				return 0;
			}
			return result;
		}
		
		/**
		* Find the sum of pixel intensities in the signal channel
		* within this shell which also lie within the given signal.
		*
		* @param s the signal
		* @return the sum of signal intensities in the signal
		*/
		public int getSignalDensity(NuclearSignal s){

			List<XYPoint> signalPoints = s.getPixelsAsPoints();

			if(signalPoints.isEmpty()){
				return 0;
			}

			ImageStack st = new ImageImporter(s.getSourceFile()).importImage();
			int stackNumber = Constants.rgbToStack(s.getChannel());
			ImageProcessor ip = st.getProcessor(stackNumber);




			// create result array
			int result = 0;

			try {

				int density = 0;

				// Go through every pixel in the signal
				for(XYPoint p : signalPoints){

					int x = p.getXAsInt();
					int y = p.getYAsInt();

					// Test if the point is within this shell
					if(r.contains(x, y)){
						
//						if(s.containsOriginalPoint(x, y)){
							// find the value of the signal
							density += ip.getPixel(x, y);	
//						}
					}
				}
				
				result = density;

			} catch (Exception e) {
				error( "Error getting signal densities", e);

				return 0;
			}
			return result;
		}
		
		/**
		 * Get the pixels within this shell as points
		 * @return
		 */
		public List<XYPoint> getPixelsAsPoints(){


			Rectangle roiBounds = this.getBounds();


			// Get a list of all the points within the ROI
			List<XYPoint> result = new ArrayList<XYPoint>(0);

			// get the bounding box of the roi
			// make a list of all the pixels in the roi
			int minX = (int) roiBounds.getX();
			int maxX = minX + (int) roiBounds.getWidth();

			int minY = (int) roiBounds.getY();
			int maxY = minY + (int) roiBounds.getHeight();

			for(int x=minX; x<=maxX; x++){
				for(int y=minY; y<=maxY; y++){

					if(r.contains(x, y)){
						result.add(new XYPoint(x, y));
					}
				}
			}

			if(result.isEmpty()){
				//					IJ.log("    Roi has no pixels");
				log(Level.SEVERE, "No points found in roi");
				log(Level.FINE, "X base: "+minX
						+"  Y base: "+minY
						+"  X max: "+maxX
						+"  Y max: "+maxY);
			} else {
				//					IJ.log("    Roi of area "+result.size());
			}
			return result;


		}


		/**
		 * Get the position of the shell as described in the 
		 * CellularComponent interface
		 * @return
		 */
		public double[] getPosition() {
			double[] result =  { r.getBounds().getX(), 
					r.getBounds().getY(), 
					r.getBounds().getWidth(),
					r.getBounds().getHeight()
			};
			return result;
		}

		/**
		 * Get the bounds of the shell
		 * @return
		 */
		public Rectangle getBounds() {
			return r.getBounds();
		}
		
		public Shape toShape(){
			return r.getPolygon();
		}
		
		public Area toArea(){
			return new Area(this.toShape());
		}
		
		public String toString(){
			
			return this.getBounds().toString();
			
		}

		
	}

}