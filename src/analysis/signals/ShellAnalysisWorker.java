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
import io.ImageImporter;
import io.ImageImporter.ImageImportException;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import stats.Sum;
import utility.ArrayConverter;
import utility.ArrayConverter.ArrayConversionException;
import utility.Constants;
import analysis.AnalysisWorker;
import analysis.IAnalysisDataset;
import analysis.signals.ShellCounter.CountType;
import components.ICellCollection;
import components.active.generic.DefaultShellResult;
import components.active.generic.UnavailableSignalGroupException;
import components.nuclear.INuclearSignal;
import components.nuclear.IShellResult;
import components.nuclear.ISignalGroup;
import components.nuclear.ShellResult;
import components.nuclear.SignalGroup;
import components.nuclei.Nucleus;

public class ShellAnalysisWorker extends AnalysisWorker {
	
	private final int shells;
	
	private int totalPixels = 0;
		
	private static Map<UUID, ShellCounter> counters = new HashMap<UUID, ShellCounter>(0);
		
	public ShellAnalysisWorker(IAnalysisDataset dataset, int shells){
		super(dataset);
		this.shells = shells;
		this.setProgressTotal(dataset.getCollection().size());
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
			
			int progress = 0;
			for(Nucleus n : collection.getNuclei()){
				
				analyseNucleus(n);

				publish(progress++);

			}
			firePropertyChange("Cooldown", getProgress(), Constants.Progress.COOLDOWN.code());

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
		
		ShellDetector shellAnalyser;
		try {
			shellAnalyser = new ShellDetector(n, shells);
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

				ShellCounter counter = counters.get(signalGroup);

				for(INuclearSignal s : signals){


					try {

						double[] signalInSignals = shellAnalyser.findProportionPerShell(s);
						int[]    countsInSignals = shellAnalyser.findPixelCountPerShell(s);

						ImageStack signalStack = new ImageImporter(s.getSourceFile()).importImage();

						int[]    countsInNucleus = shellAnalyser.findPixelIntensityPerShell(signalStack, s.getChannel());
						double[] signalInNucleus = shellAnalyser.findProportionPerShell(signalStack, s.getChannel());



						int[] dapiIntensities = shellAnalyser.findPixelIntensityPerShell(nucleusStack, n.getChannel());

						double[] normalisedSignals = shellAnalyser.normalise(signalInSignals, dapiIntensities);
						double[] normalisedNucleus = shellAnalyser.normalise(signalInNucleus, dapiIntensities);

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
								
				IShellResult result = new DefaultShellResult(shells)
				.setRawMeans(       CountType.SIGNAL,  channelCounter.getRawMeans(CountType.SIGNAL))
	        	.setRawMeans(       CountType.NUCLEUS, channelCounter.getRawMeans(CountType.NUCLEUS))
	        	.setNormalisedMeans(CountType.SIGNAL,  channelCounter.getNormalisedMeans(CountType.SIGNAL))
	        	.setNormalisedMeans(CountType.NUCLEUS, channelCounter.getNormalisedMeans(CountType.NUCLEUS))
	        	.setRawStandardErrors( CountType.SIGNAL,  channelCounter.getRawStandardErrors(CountType.SIGNAL))
	        	.setRawStandardErrors( CountType.NUCLEUS, channelCounter.getRawStandardErrors(CountType.NUCLEUS))
	        	.setNormalisedStandardErrors( CountType.SIGNAL,  channelCounter.getNormalisedStandardErrors(CountType.SIGNAL))
	        	.setNormalisedStandardErrors( CountType.NUCLEUS, channelCounter.getNormalisedStandardErrors(CountType.NUCLEUS))
	        	.setPixelCounts(    CountType.SIGNAL,  channelCounter.getPixelCounts(CountType.SIGNAL))
	        	.setPixelCounts(    CountType.NUCLEUS, channelCounter.getPixelCounts(CountType.NUCLEUS))
	        	.setRawChiResult(      CountType.SIGNAL,  channelCounter.getRawChiSquare(CountType.SIGNAL), channelCounter.getRawPValue(CountType.SIGNAL))
	        	.setRawChiResult(      CountType.NUCLEUS, channelCounter.getRawChiSquare(CountType.NUCLEUS), channelCounter.getRawPValue(CountType.NUCLEUS))
				.setNormalisedChiResult(CountType.SIGNAL, channelCounter.getNormalisedChiSquare(CountType.SIGNAL), channelCounter.getNormalisedPValue(CountType.SIGNAL))
	        	.setNormalisedChiResult(CountType.NUCLEUS,channelCounter.getNormalisedChiSquare(CountType.NUCLEUS), channelCounter.getNormalisedPValue(CountType.NUCLEUS));
					
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
			
			int[] counts = sr.getCounts();

			try{

				List<Double> list       = new ArrayConverter(c).toDoubleList();
				List<Double> errList    = new ArrayConverter(err).toDoubleList();
				List<Integer> countList = new ArrayConverter(counts).toIntegerList();
				
				IShellResult randomResult = new DefaultShellResult(shells)
					.setPixelCounts(CountType.SIGNAL, countList)
					.setPixelCounts(CountType.NUCLEUS, countList)
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
