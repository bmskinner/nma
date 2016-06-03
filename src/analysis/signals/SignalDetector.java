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

import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import io.ImageImporter;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import logging.DebugFileHandler;
import utility.Constants;
import utility.StatsMap;
import utility.Utils;
import analysis.AnalysisDataset;
import analysis.AnalysisOptions.NuclearSignalOptions;
import analysis.detection.Detector;
import analysis.AnalysisWorker;
import components.Cell;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.XYPoint;
import components.nuclear.NuclearSignal;
import components.nuclear.SignalCollection;
import components.nuclei.AsymmetricNucleus;
import components.nuclei.Nucleus;


/**
 * Methods for finding a FISH signal in a nucleus.
 * TODO: For a paint, assume one or two signals per nucleus. 
 * If more are detected, lower the threshold until the signals merge
 */
public class SignalDetector extends AnalysisWorker {
	
	protected NuclearSignalOptions options = null;
	protected File folder;
	protected int channel;
	protected int signalGroup;
	protected String channelName;
		
	/**
	 * For use when running on an existing dataset
	 * @param d the dataset to add signals to
	 * @param folder the folder of images
	 * @param channel the RGB channel to search
	 * @param options the analysis options
	 * @param group the signal group to add signals to
	 */
	public SignalDetector(AnalysisDataset d, File folder, int channel, NuclearSignalOptions options, int group, String channelName){
		super(d);
		this.options	 = options;
		
		this.folder		 = folder;
		this.channel	 = channel;
		this.signalGroup = group;
		this.channelName = channelName;
		
		this.setProgressTotal(d.getCollection().getNucleusCount());
	}
	
	@Override
	protected Boolean doInBackground() throws Exception {
		boolean result = true;
		log(Level.FINE, "Beginning signal detection in channel "+channel);

		try{
			int progress = 0;
			
			int originalMinThreshold = options.getSignalThreshold();
			
			SignalFinder finder = new SignalFinder(options, channel);
			
			for(Cell c : getDataset().getCollection().getCells()){
				
				// reset the  min threshold for each cell
				options.setThreshold(originalMinThreshold);

				Nucleus n = c.getNucleus();
				log(Level.FINER, "Looking for signals associated with nucleus "+n.getSourceFileName()+"-"+n.getNucleusNumber());
				
				// get the image in the folder with the same name as the
				// nucleus source image
				File imageFile = new File(folder + File.separator + n.getSourceFileName());
				log(Level.FINER, "Source file: "+imageFile.getAbsolutePath());

				try{
					
					ImageStack stack = ImageImporter.getInstance().importImage(imageFile);

					List<NuclearSignal> signals = finder.detectSignal(imageFile, stack, n);
					
					log(Level.FINER, "Creating signal collection");
					
					SignalCollection signalCollection = n.getSignalCollection();
					signalCollection.addSignalGroup(signals, signalGroup, imageFile, channel);
					signalCollection.setSignalGroupName(signalGroup, channelName);
					n.calculateSignalDistancesFromCoM();
					n.calculateFractionalSignalDistancesFromCoM();

					log(Level.FINE, "Calculating signal angles");
					if(AsymmetricNucleus.class.isAssignableFrom(n.getClass())){
						log(Level.FINER, "Nucleus type is asymmetric: "+n.getClass().getSimpleName());
						
						if(n.hasBorderTag(BorderTag.ORIENTATION_POINT)){
							log(Level.FINEST, "Calculating angle from orientation point");
							n.calculateSignalAnglesFromPoint(n.getBorderPoint(BorderTag.ORIENTATION_POINT));
						} else {
							log(Level.FINEST, "No orientation point in nucleus");
						}
					} else {
						log(Level.FINER, "Nucleus type is round: "+n.getClass().getSimpleName());
					}
					
					
				} catch(Exception e){
					logError("Error detecting signal", e);
				}
				
				publish(progress++);
			}		
			
		} catch (Exception e){
			logError("Error in signal detection", e);
			return false;
		}

		return result;
	}
	
}
