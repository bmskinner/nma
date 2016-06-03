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
import java.util.logging.Level;
import java.util.logging.Logger;

import analysis.AnalysisOptions.NuclearSignalOptions;
import analysis.detection.Detector;
import components.CellularComponent;
import components.generic.BooleanProfile;
import components.generic.Profile;
import components.generic.XYPoint;
import components.nuclear.NuclearSignal;
import components.nuclei.Nucleus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import logging.Loggable;
import stats.NucleusStatistic;
import utility.Constants;
import utility.StatsMap;
import utility.Utils;

public class SignalFinder implements Loggable {
	
	private NuclearSignalOptions options;
	private  int channel;
	private int minThreshold;
	
	/**
	 * Create a finder with the desired options
	 * @param options the size and circularity parameters
	 * @param programLogger the logger
	 * @param channel the RGB channel
	 * @param signalGroup the group
	 * @param channelName the name of the group
	 */
	public SignalFinder(NuclearSignalOptions options, int channel){
		this.options = options;
		this.channel = channel;
		this.minThreshold = options.getSignalThreshold();
	}
	
	/**
	 * Call the appropriate signal detection method based on the analysis options
	 * @param sourceFile the file the image came from
	 * @param stack the imagestack
	 * @param n the nucleus
	 * @throws Exception 
	 */
	public List<NuclearSignal> detectSignal(File sourceFile, ImageStack stack, Nucleus n) throws Exception{
		
		options.setThreshold(minThreshold); // reset to default;
		
		if(options==null || options.getMode()==NuclearSignalOptions.FORWARD){
			finest("Running forward detection");
			return detectForwardThresholdSignal(sourceFile, stack, n);
		}
		
		if(options.getMode()==NuclearSignalOptions.REVERSE){
			finest( "Running reverse detection");
			return detectReverseThresholdSignal(sourceFile, stack, n);
		}
		
		if(options.getMode()==NuclearSignalOptions.HISTOGRAM){
			finest( "Running adaptive detection");
			return detectHistogramThresholdSignal(sourceFile, stack, n);
		}
		finest( "No mode specified");
		return null;
	}
	
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
	private List<NuclearSignal> detectForwardThresholdSignal(File sourceFile, ImageStack stack, Nucleus n) throws Exception{
//		SignalCollection signalCollection = n.getSignalCollection();
		
		// choose the right stack number for the channel
		int stackNumber = Constants.rgbToStack(channel);
		
		// create a new detector
		Detector detector = new Detector();
		detector.setMaxSize(n.getStatistic(NucleusStatistic.AREA) * options.getMaxFraction());
		detector.setMinSize(options.getMinSize());
		detector.setMinCirc(options.getMinCirc());
		detector.setMaxCirc(options.getMaxCirc());
		detector.setThreshold(options.getSignalThreshold());

		try{
			ImageProcessor ip = stack.getProcessor(stackNumber);
			detector.run(ip);
		} catch(Exception e){
			error("Error in signal detection", e);
		}
		List<Roi> roiList = detector.getRoiList();

		List<NuclearSignal> signals = new ArrayList<NuclearSignal>(0);

		if(!roiList.isEmpty()){
			
			fine( roiList.size()+" signals in stack "+stackNumber);

			for( Roi r : roiList){
				ImageProcessor ip = stack.getProcessor(stackNumber);
				StatsMap values = detector.measure(r, ip);
				
				// Offset the centre of mass of the signal to match the nucleus offset
				NuclearSignal s = new NuclearSignal( r, 
						values.get("Area"), 
						values.get("Feret"), 
						values.get("Perim"), 
						new XYPoint(values.get("XM")-n.getPosition()[CellularComponent.X_BASE], 
									values.get("YM")-n.getPosition()[CellularComponent.Y_BASE])
						);

				// only keep the signal if it is within the nucleus
				if(n.containsPoint(s.getCentreOfMass())){
					s.setSourceFile(sourceFile);
					s.setChannel(channel);
					
					double xbase     = r.getXBase();
					double ybase     = r.getYBase();
					Rectangle bounds = r.getBounds();
					double[] originalPosition = {xbase, ybase, bounds.getWidth(), bounds.getHeight() };
					s.setPosition(originalPosition);
					signals.add(s);
					
				}
				
			}
		} else {
			fine( "No signal in stack "+stackNumber);
		}
		return signals;
	}
	
//	private List<NuclearSignal> findSignalsFromRois(List<Roi> roiList, Nucleus n){
//		List<NuclearSignal> signals = new ArrayList<NuclearSignal>(0);
//
//		if(!roiList.isEmpty()){
//			
//			programLogger.log(Level.FINE, roiList.size()+" signals in stack "+stackNumber);
//
//			for( Roi r : roiList){
//				
//				StatsMap values = detector.measure(r, stack);
//				
//				// Offset the centre of mass of the signal to match the nucleus offset
//				NuclearSignal s = new NuclearSignal( r, 
//						values.get("Area"), 
//						values.get("Feret"), 
//						values.get("Perim"), 
//						new XYPoint(values.get("XM")-n.getPosition()[CellularComponent.X_BASE], 
//									values.get("YM")-n.getPosition()[CellularComponent.Y_BASE])
//						);
//
//				// only keep the signal if it is within the nucleus
//				if(n.containsPoint(s.getCentreOfMass())){
//					s.setSourceFile(sourceFile);
//					s.setChannel(channel);
//					
//					double xbase     = r.getXBase();
//					double ybase     = r.getYBase();
//					Rectangle bounds = r.getBounds();
//					double[] originalPosition = {xbase, ybase, bounds.getWidth(), bounds.getHeight() };
//					s.setPosition(originalPosition);
//					signals.add(s);
//				}
//				
//			}
//			return signals;
//	}
	
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
	private List<NuclearSignal> detectReverseThresholdSignal(File sourceFile, ImageStack stack, Nucleus n) throws Exception{
		
//		SignalCollection signalCollection = n.getSignalCollection();
		log( "Beginning reverse detection for nucleus");
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
	private List<NuclearSignal> detectHistogramThresholdSignal(File sourceFile, ImageStack stack, Nucleus n) throws Exception{
		fine( "Beginning histogram detection for nucleus");

		// choose the right stack number for the channel
		int stackNumber = Constants.rgbToStack(channel);
		
		ImageProcessor ip = stack.getProcessor(stackNumber);
		double[] positions = n.getPosition();
		Rectangle boundingBox = new Rectangle( (int) positions[CellularComponent.X_BASE],
				(int) positions[CellularComponent.Y_BASE],
				(int) positions[CellularComponent.WIDTH],
				(int) positions[CellularComponent.HEIGHT]);
		
		ip.setRoi(boundingBox);
		ImageStatistics statistics = ImageStatistics.getStatistics(ip, Measurements.AREA, new Calibration());
		long[] histogram = statistics.getHistogram();
		
		double[] d = new double[histogram.length];

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
		Profile histogramProfile = new Profile(d);
		Profile trimmedHisto = histogramProfile.getSubregion(minThreshold, 255);
		
		// smooth the arrays,  get the deltas, and double smooth them
		Profile trimDS = trimmedHisto.smooth(3).calculateDeltas(3).smooth(3).smooth(3);
		
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
