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
package com.bmskinner.nuclear_morphology.analysis.signals;

import ij.ImageStack;

import java.io.File;
import java.util.List;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.AnalysisWorker;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.NuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.AsymmetricNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IMutableNuclearSignalOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;


/**
 * Methods for finding a FISH signal in a nucleus.
 * TODO: For a paint, assume one or two signals per nucleus. 
 * If more are detected, lower the threshold until the signals merge
 */
@Deprecated
public class SignalDetectionWorker extends AnalysisWorker {
	
	protected IMutableNuclearSignalOptions options = null;
	protected File folder;
	protected int channel;
	protected UUID signalGroup;
	protected String channelName;
		
	/**
	 * For use when running on an existing dataset
	 * @param d the dataset to add signals to
	 * @param folder the folder of images
	 * @param channel the RGB channel to search
	 * @param options the analysis options
	 * @param group the signal group to add signals to
	 */

	public SignalDetectionWorker(IAnalysisDataset d, File folder, int channel, IMutableNuclearSignalOptions options, UUID group, String channelName){
		super(d);
		
		this.options	 = options;
		this.folder		 = folder;
		this.channel	 = channel;
		this.signalGroup = group;
		this.channelName = channelName;
		
		this.setProgressTotal(d.getCollection().size());
	}
	
	@Override
	protected Boolean doInBackground() throws Exception {
		boolean result = true;
		fine("Beginning signal detection in channel "+channel);

		try{
			int progress = 0;
			
			int originalMinThreshold = options.getThreshold();
			
			SignalDetector finder = new SignalDetector(options, channel);
			
			for(ICell c : getDataset().getCollection().getCells()){
				
				// reset the  min threshold for each cell
				options.setThreshold(originalMinThreshold);

				Nucleus n = c.getNucleus();
				finer("Looking for signals associated with nucleus "+n.getSourceFileName()+"-"+n.getNucleusNumber());
				
				// get the image in the folder with the same name as the
				// nucleus source image
				File imageFile = new File(folder + File.separator + n.getSourceFileName());
				finer("Source file: "+imageFile.getAbsolutePath());
				
				try {
					
					ImageStack stack = new ImageImporter(imageFile).importToStack();

					List<INuclearSignal> signals = finder.detectSignal(imageFile, stack, n);
					
					finer("Creating signal collection");
					
					ISignalCollection signalCollection = n.getSignalCollection();
					signalCollection.addSignalGroup(signals, signalGroup, imageFile, channel);
					
					SignalAnalyser s = new SignalAnalyser();
					s.calculateSignalDistancesFromCoM(n);
					s.calculateFractionalSignalDistancesFromCoM(n);

					fine("Calculating signal angles");
					
					// If the nucleus is asymmetric, calculate angles
					if( ! this.getDataset().getCollection().getNucleusType().equals(NucleusType.ROUND)){
						
						finer("Nucleus type is asymmetric: "+n.getClass().getSimpleName());
						
						if(n.hasBorderTag(Tag.ORIENTATION_POINT)){
							finest("Calculating angle from orientation point");
							n.calculateSignalAnglesFromPoint(n.getBorderPoint(Tag.ORIENTATION_POINT));
						} else {
							finest("No orientation point in nucleus");
						}
						
					} else {
						finer("Nucleus type is round: "+n.getClass().getSimpleName());
					}		
					
				} catch(ImageImportException e){
					warn("Cannot open "+imageFile.getAbsolutePath());
					stack("Cannot load image", e);
				}
									
				publish(progress++);
			}		
			
		} catch (Exception e){
			warn("Error in signal detection");
			stack("Error in signal detection", e);
			return false;
		}

		return result;
	}
	
}
