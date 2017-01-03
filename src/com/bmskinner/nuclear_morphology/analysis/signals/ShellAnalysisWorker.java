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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.bmskinner.nuclear_morphology.analysis.AnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellCounter.CountType;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.DefaultShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.stats.Sum;
import com.bmskinner.nuclear_morphology.utility.ArrayConverter;
import com.bmskinner.nuclear_morphology.utility.ArrayConverter.ArrayConversionException;

@Deprecated
public class ShellAnalysisWorker extends AnalysisWorker {
	
	private final int shells;
	
	private int totalPixels = 0;
		
	private static Map<UUID, ShellCounter> counters = new HashMap<UUID, ShellCounter>(0);
		
	public ShellAnalysisWorker(IAnalysisDataset dataset, int shells){
		super(dataset);
		this.shells = shells;
		this.setProgressTotal(dataset.getCollection().size()-1);
	}
		
	@Override
	protected Boolean doInBackground() {
		
		ICellCollection collection = this.getDataset().getCollection();
		
		if( ! collection.getSignalManager().hasSignals()){
			fine("No signals in population");
			return true; // only bother if there are signals
		}
		
		log("Performing shell analysis with "+shells+" shells...");
		
		try {
			counters = new HashMap<UUID, ShellCounter>(0);

			for(UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){
				
				if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
					continue;
				}
				counters.put(signalGroup, new ShellCounter(shells));
			}

			// make the shells and measure the values
						
			final AtomicInteger counter = new AtomicInteger(0);
			
//			collection.getNuclei().parallelStream().forEach( n ->{
//				analyseNucleus(n);
//				int i = counter.incrementAndGet();
//				publish(i);
//			});
			
			for(Nucleus n : collection.getNuclei()){
				
				analyseNucleus(n);
				int i = counter.incrementAndGet();
				publish(i);
//
			}
			
			firePropertyChange("Cooldown", getProgress(), IAnalysisWorker.INDETERMINATE);

			// get stats 
			createResults();
					
			
			log("Shell analysis complete");
		} catch (Exception e) {
			warn("Error in shell analysis");
			stack( "Error in shell analysis", e);	
			return false;
		}
		return true;
	}
	
	private void analyseNucleus(Nucleus n){
		
		ICellCollection collection = this.getDataset().getCollection();
		
		ShellDetector shellDetector;
		try {
			shellDetector = new ShellDetector(n, shells);
		} catch (ShellAnalysisException e1) {
			warn("Unable to make shells for "+n.getNameAndNumber());
			stack("Error in shell detector", e1);
			return;
		}
		
		try {
		
		ImageStack nucleusStack = new ImageImporter(n.getSourceFile()).importImage();

		for(UUID signalGroup : n.getSignalCollection().getSignalGroupIDs()){

			if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
				continue;
			}

			if(collection.getSignalManager().hasSignals(signalGroup)){
				List<INuclearSignal> signals = n.getSignalCollection().getSignals(signalGroup); 
				if(signals.isEmpty()){
					fine("No signals in signal group "+signalGroup+"in nucleus");
					continue;
				}
				
				File sourceFile = n.getSignalCollection().getSourceFile(signalGroup);
				
				if(sourceFile==null){
					warn("Cannot find signal image for "+n.getNameAndNumber());
					continue;
				}
				ImageStack signalStack = new ImageImporter(sourceFile).importImage();
				
				ShellCounter counter = counters.get(signalGroup);

				for(INuclearSignal s : signals){


					try {

						double[] signalInSignals = shellDetector.findProportionPerShell(s);
						int[]    countsInSignals = shellDetector.findPixelCountPerShell(s);

						int[]    countsInNucleus = shellDetector.findPixelIntensityPerShell(signalStack, s.getChannel());
						double[] signalInNucleus = shellDetector.findProportionPerShell(signalStack, s.getChannel());

						int[] dapiIntensities = shellDetector.findPixelIntensityPerShell(nucleusStack, n.getChannel());

						double[] normalisedSignals = shellDetector.normalise(signalInSignals, dapiIntensities);
						double[] normalisedNucleus = shellDetector.normalise(signalInNucleus, dapiIntensities);

						counter.addSignalValues(signalInSignals, normalisedSignals, countsInSignals);
						counter.addNucleusValues(signalInNucleus, normalisedNucleus, countsInNucleus);


						totalPixels += new Sum(counter.getPixelCounts(CountType.SIGNAL)).intValue();
						
						

					} catch (ShellAnalysisException e) {
						warn("Error in shell analysis");
						stack( "Error in signal in shell analysis", e);
					}
				} // end for signals
			} // end if signals
		}
		} catch (ImageImportException e) {

			warn("Cannot import image source file");
			stack("Error importing file", e);
		}

	}
	
	private void createResults(){
		// get stats and export
		ICellCollection collection = this.getDataset().getCollection();
		
		boolean addRandom = false;
				
		for(UUID group : counters.keySet()){
			if(collection.getSignalManager().hasSignals(group)){
				
				addRandom = true;
				ShellCounter channelCounter = counters.get(group);
								
				DefaultShellResult result = new DefaultShellResult(shells);
				
				for(CountType type : CountType.values()){
					result
					.setRawMeans(          type,  channelCounter.getRawMeans(type))
		        	.setNormalisedMeans(   type,  channelCounter.getNormalisedMeans(type))
		        	.setRawStandardErrors( type,  channelCounter.getRawStandardErrors(type))
		        	.setNormalisedStandardErrors( type,  channelCounter.getNormalisedStandardErrors(type))
		        	.setRawChiResult(      type,  channelCounter.getRawChiSquare(type), channelCounter.getRawPValue(type))
					.setNormalisedChiResult(type, channelCounter.getNormalisedChiSquare(type), channelCounter.getNormalisedPValue(type));
				}
				
					
				try {
					getDataset().getCollection()
						.getSignalGroup(group)
						.setShellResult(result);
				} catch (UnavailableSignalGroupException e) {
					stack("Signal group is not present", e);
					warn("Cannot save shell result");
				}

			}
		}
		
		if(addRandom){
			addRandomSignal();
		}
	}
	
	private void addRandomSignal(){
		
		ICellCollection collection = this.getDataset().getCollection();
		
		// Create a random sample distibution
		if(collection.hasConsensusNucleus()){
			
			ISignalGroup random = new SignalGroup();
			random.setGroupName("Random distribution");
			random.setFolder( new File(""));
			
			getDataset().getCollection()
			.addSignalGroup(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID, random);
			
			// Calculate random positions of pixels 
			fine("Creating random sample of "+totalPixels+" pixels");
			
			ShellRandomDistributionCreator sr = new ShellRandomDistributionCreator(collection.getConsensusNucleus(), 
					shells,
					totalPixels);

			double[] c = sr.getProportions();
			
			double[] err = new double[c.length];
			for(int i=0; i<c.length; i++){
				err[i] = 0;
			}
			
			try{

				List<Double> list       = new ArrayConverter(c).toDoubleList();
				List<Double> errList    = new ArrayConverter(err).toDoubleList();
				
				IShellResult randomResult = new DefaultShellResult(shells)
					.setRawMeans(CountType.SIGNAL, list)
					.setRawMeans(CountType.NUCLEUS, list)
					.setNormalisedMeans(CountType.SIGNAL, list)
					.setNormalisedMeans(CountType.NUCLEUS, list)
					.setRawStandardErrors(CountType.SIGNAL, errList)
					.setRawStandardErrors(CountType.NUCLEUS, errList)
					.setNormalisedStandardErrors(CountType.SIGNAL, errList)
					.setNormalisedStandardErrors(CountType.NUCLEUS, errList);

				getDataset().getCollection()
					.getSignalGroup(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)
					.setShellResult(randomResult);



			} catch(ArrayConversionException e){
				stack("Conversion error", e);
			} catch (UnavailableSignalGroupException e) {
				stack("Signal group is not present", e);
				warn("Cannot add random shell result");
			}
		} else {
			warn("Cannot create simulated dataset, no consensus");
		}
		
		
	}

}
