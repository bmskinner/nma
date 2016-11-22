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
package analysis.signals;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import analysis.detection.Detector;
import analysis.signals.INuclearSignalOptions.SignalDetectionMode;
import components.CellularComponent;
import components.active.DefaultNuclearSignal;
import components.active.generic.FloatPoint;
import components.active.generic.FloatProfile;
import components.generic.BooleanProfile;
import components.generic.IPoint;
import components.generic.IProfile;
import components.generic.Profile;
import components.generic.XYPoint;
import components.nuclear.INuclearSignal;
import components.nuclear.NuclearSignal;
import components.nuclei.Nucleus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import stats.NucleusStatistic;
import stats.SignalStatistic;
import utility.Constants;
import utility.StatsMap;

public class SignalDetector extends Detector {
	
	private IMutableNuclearSignalOptions options;
	private  int channel;
	private int minThreshold;
	
	/**
	 * Create a detector with the desired options
	 * @param options the size and circularity parameters
	 * @param channel the RGB channel
	 */
	public SignalDetector(IMutableNuclearSignalOptions options, int channel){
		this.options = options;
		this.channel = channel;
		this.minThreshold = options.getThreshold();
	}
	
	/**
	 * Call the appropriate signal detection method based on the analysis options
	 * @param sourceFile the file the image came from
	 * @param stack the imagestack
	 * @param n the nucleus
	 * @throws Exception 
	 */
	public List<INuclearSignal> detectSignal(File sourceFile, ImageStack stack, Nucleus n) throws Exception{
		
		options.setThreshold(minThreshold); // reset to default;
		
		if(options==null || options.getDetectionMode().equals(SignalDetectionMode.FORWARD)){
			finest("Running forward detection");
			return detectForwardThresholdSignal(sourceFile, stack, n);
		}
		
		if(options.getDetectionMode().equals(SignalDetectionMode.REVERSE)){
			finest( "Running reverse detection");
			return detectReverseThresholdSignal(sourceFile, stack, n);
		}
		
		if(options.getDetectionMode().equals(SignalDetectionMode.ADAPTIVE)){
			finest( "Running adaptive detection");
			return detectHistogramThresholdSignal(sourceFile, stack, n);
		}
		finest( "No mode specified");
		return null;
	}
	
	
	/*
	 * PROTECTED AND PRIVATE METHODS
	 * 
	 */
	
	/**
	 * Given a new threshold value, update the options
	 * if the value is not below the previously defined
	 * minimum
	 * @param newThreshold
	 */
	private void updateThreshold(int newThreshold){
		// only use the calculated threshold if it is larger than
		// the given minimum
		if(newThreshold > minThreshold){
			fine( "Threshold set at: "+newThreshold);
			options.setThreshold(newThreshold);
		} else {
			fine( "Threshold kept at minimum: "+minThreshold);
			options.setThreshold(minThreshold);
		}
	}
	
	/**
	 * Detect a signal in a given stack by standard forward thresholding
	 * and add to the given nucleus
	 * @param sourceFile the file the image came from
	 * @param stack the imagestack
	 * @param n the nucleus
	 * @throws Exception 
	 */
	private List<INuclearSignal> detectForwardThresholdSignal(File sourceFile, ImageStack stack, Nucleus n) throws Exception{
//		SignalCollection signalCollection = n.getSignalCollection();
		
		// choose the right stack number for the channel
		int stackNumber = Constants.rgbToStack(channel);
		
		setMaxSize(n.getStatistic(NucleusStatistic.AREA) * options.getMaxFraction());
		setMinSize(options.getMinSize());
		setMinCirc(options.getMinCirc());
		setMaxCirc(options.getMaxCirc());
		setThreshold(options.getThreshold());

		List<Roi> roiList = new ArrayList<Roi>();
		
		try{
			
			ImageProcessor ip = stack.getProcessor(stackNumber);
			roiList = detectRois(ip);
			
		} catch(Exception e){
			error("Error in signal detection", e);
		}


		List<INuclearSignal> signals = new ArrayList<INuclearSignal>(0);

		if(!roiList.isEmpty()){
			
			fine( roiList.size()+" signals in stack "+stackNumber);

			for( Roi r : roiList){
				ImageProcessor ip = stack.getProcessor(stackNumber);
				StatsMap values = measure(r, ip);
				
				
				int xbase     = (int) r.getXBase();
				int ybase     = (int) r.getYBase();
				Rectangle bounds = r.getBounds();
				int[] originalPosition = {xbase, ybase, (int) bounds.getWidth(), (int) bounds.getHeight() };
				
				try {
					INuclearSignal s = new DefaultNuclearSignal( r,
							IPoint.makeNew(values.get("XM").floatValue(), values.get("YM").floatValue()), 
							sourceFile, 
							channel, 
							originalPosition);

					s.setStatistic(SignalStatistic.AREA,      values.get("Area"));
					s.setStatistic(SignalStatistic.MAX_FERET, values.get("Feret"));
					s.setStatistic(SignalStatistic.PERIMETER, values.get("Perim"));

					/*
			    Assuming the signal were a perfect circle of area equal
			    to the measured area, get the radius for that circle
					 */
					s.setStatistic(SignalStatistic.RADIUS,  Math.sqrt(values.get("Area")/Math.PI));



					// only keep the signal if it is within the nucleus
					if(n.containsOriginalPoint(s.getCentreOfMass())){

						// Offset the centre of mass and border points of the signal to match the nucleus offset
						s.offset(-n.getPosition()[CellularComponent.X_BASE], 
								-n.getPosition()[CellularComponent.Y_BASE]);

						signals.add(s);


					}
				} catch(IllegalArgumentException e){
					stack("Cannot make signal", e);
					continue;
				}
				
			}
		} else {
			fine( "No signal in stack "+stackNumber);
		}
		return signals;
	}
	

	
	/**
	 * Detect a signal in a given stack by reverse thresholding
	 * and add to the given nucleus. Find the brightest pixels in
	 * the nuclear roi. If < maxSignalFraction, get dimmer pixels and
	 * remeasure. Continue until signal size is met. Works best with 
	 * maxSignalFraction of ~0.1 for a chromosome paint
	 * TODO: assumes there is only one signal. Check that the detector picks
	 * up an object of MIN_SIGNAL_SIZE before setting the threshold.
	 * @param sourceFile the file the image came from
	 * @param stack the imagestack
	 * @param n the nucleus
	 * @throws Exception 
	 */
	private List<INuclearSignal> detectReverseThresholdSignal(File sourceFile, ImageStack stack, Nucleus n) throws Exception{
		
//		SignalCollection signalCollection = n.getSignalCollection();
		finest( "Beginning reverse detection for nucleus");
		// choose the right stack number for the channel
		int stackNumber = Constants.rgbToStack(channel);
		
		ImageProcessor ip = stack.getProcessor(stackNumber);
		FloatPolygon polygon = n.createOriginalPolygon();
		
		// map brightness to count
		Map<Integer, Integer> counts = new HashMap<Integer, Integer>(0);
		for(int i=0;i<256;i++){
			counts.put(i, 0);
		}
		
		
//		get the region bounded by the nuclear roi
//		for max intensity (255) , downwards, count pixels with that intensity
//		if count / area < fraction, continue
		
		//sort the pixels in the roi to bins
		for(int width = 0; width<ip.getWidth();width++){
			for(int height = 0; height<ip.getHeight();height++){
				
				if(polygon.contains( (float) width, (float) height)){
					int brightness = ip.getPixel(width, height);
					int oldCount = counts.get(brightness);
					counts.put(brightness, oldCount+1);
				}
				
			}
		}
		
//		logger.log("Counts created", Logger.DEBUG);
//		for(int i=0;i<256;i++){
//			logger.log("Level "+i+": "+counts.get(i), Logger.DEBUG);
//		}
		
		// find the threshold from the bins
		int area = (int) ( n.getStatistic(NucleusStatistic.AREA) * options.getMaxFraction());
		int total = 0;
		int threshold = 0; // the value to threshold at
		
		for(int brightness = 255; brightness>0; brightness--){
			
			total += counts.get(brightness); 
			
			if(total>area){
				threshold = brightness+1;
				break;
			}
		}
		
		
		updateThreshold(threshold);
		
		// now we have the reverse threshold value, do the thresholding 
		// and find signal rois
		return detectForwardThresholdSignal(sourceFile, stack, n);

	}
	
	
	
	/**
	 * This method uses the histogram of pixel intensities in the signal
	 * channel within the bounding box of the nucleus. The histogram shows a drop
	 * at the point where background transitions to real signal. We detect this drop, 
	 * and set it as the appropriate forward threshold for the nucleus.  
	 * @throws Exception 
	 */
	private List<INuclearSignal> detectHistogramThresholdSignal(File sourceFile, ImageStack stack, Nucleus n) throws Exception{
		fine( "Beginning histogram detection for nucleus");

		// choose the right stack number for the channel
		int stackNumber = Constants.rgbToStack(channel);
		
		ImageProcessor ip = stack.getProcessor(stackNumber);
		int[] positions = n.getPosition();
		Rectangle boundingBox = new Rectangle( (int) positions[CellularComponent.X_BASE],
				(int) positions[CellularComponent.Y_BASE],
				(int) positions[CellularComponent.WIDTH],
				(int) positions[CellularComponent.HEIGHT]);
		
		ip.setRoi(boundingBox);
		ImageStatistics statistics = ImageStatistics.getStatistics(ip, Measurements.AREA, new Calibration());
		long[] histogram = statistics.getHistogram();
		
		float[] d = new float[histogram.length];

		for(int i =0; i<histogram.length; i++){
			d[i] = histogram[i];

		}
		
		/* trim the histogram to the minimum signal intensity.
		 * No point looking lower, and the black pixels increase the
		 * total range making it harder to carry out the range based minima
		 * detection below
		 */
		finest( "Initial histo threshold: "+minThreshold);
//		int trimValue = minThreshold;
		IProfile histogramProfile = new FloatProfile(d);
		IProfile trimmedHisto = histogramProfile.getSubregion(minThreshold, 255);
		
		// smooth the arrays,  get the deltas, and double smooth them
		IProfile trimDS = trimmedHisto.smooth(3).calculateDeltas(3).smooth(3).smooth(3);
		
		/* find minima and maxima above or below zero, with a total 
		 * displacement more than 0.1 of the range of values in the delta
		 * profile
		 */		
		BooleanProfile minimaD = trimDS.getLocalMinima(3, 0, 0.1);

		/* Set the threshold for this nucleus to the drop-off
		* This is the highest local minimum detected in the 
		* delta profile (if no minima were detected, we use the
		* original signal threshold). 
		*/ 
		int maxIndex = minThreshold;
		for(int i =0; i<minimaD.size(); i++){
			if(minimaD.get(i)==true){
				maxIndex = i+minThreshold;
			}
		}
		/*
		 * Add a bit more to the new threshold. This is because the minimum
		 * of the delta profile is in middle of the background drop off;
		 * we actually want to ignore the remainder of this background and just
		 * keep the signal. Arbitrary at present. TODO: Find the best point. 
		 */
		maxIndex+=10;
		
		updateThreshold(maxIndex);
		return detectForwardThresholdSignal(sourceFile, stack, n);
	}
}
