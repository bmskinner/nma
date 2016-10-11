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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import utility.ArrayConverter;
import utility.ArrayConverter.ArrayConversionException;
import utility.Constants;
import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import components.CellCollection;
import components.nuclear.NuclearSignal;
import components.nuclear.ShellResult;
import components.nuclear.SignalGroup;
import components.nuclei.Nucleus;

public class ShellAnalysisWorker extends AnalysisWorker {
	
	private final int shells;
	
	private static Map<UUID, ShellCounter> counters = new HashMap<UUID, ShellCounter>(0);
		
	
	public ShellAnalysisWorker(AnalysisDataset dataset, int shells){
		super(dataset);
		this.shells = shells;
		this.setProgressTotal(dataset.getCollection().getNucleusCount());
	}
		
	@Override
	protected Boolean doInBackground() {
		
		CellCollection collection = this.getDataset().getCollection();
		
		if( ! collection.getSignalManager().hasSignals()){
			fine("No signals in population");
			return true; // only bother if there are signals
		}
		
		log("Performing shell analysis with "+shells+" shells...");
		
		try {
			counters = new HashMap<UUID, ShellCounter>(0);

			for(UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){
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
			logError( "Error in shell analysis", e);	
			return false;
		}
		return true;
	}
	
	private void analyseNucleus(Nucleus n){
		
		CellCollection collection = this.getDataset().getCollection();
		
		ShellDetector shellAnalyser = new ShellDetector(n, shells);
		
		for(UUID signalGroup : n.getSignalCollection().getSignalGroupIDs()){
			if(collection.getSignalManager().hasSignals(signalGroup)){
				List<NuclearSignal> signals = n.getSignalCollection().getSignals(signalGroup); 

				ShellCounter counter = counters.get(signalGroup);

				for(NuclearSignal s : signals){
					try {

						
						double[] signalPerShell = shellAnalyser.findProportionPerShell(s);
						counter.addValues(signalPerShell);
					} catch (Exception e) {
						logError( "Error in signal in shell analysis", e);
					}
				} // end for signals
			} // end if signals
		}
		
	}
	
	private void createResults(){
		// get stats and export
		CellCollection collection = this.getDataset().getCollection();
		
		boolean addRandom = false;
		for(UUID group : counters.keySet()){
			if(collection.getSignalManager().hasSignals(group)){
				
				addRandom = true;
				ShellCounter channelCounter = counters.get(group);

				getDataset().getCollection()
				.getSignalGroup(group)
				.setShellResult(new ShellResult(channelCounter.getMeans(), channelCounter.getStandardErrors()));

			}
		}
		
		if(addRandom){
			addRandomSignal();
		}
	}
	
	private void addRandomSignal(){
		
		CellCollection collection = this.getDataset().getCollection();
		
		// Create a random sample distibution
		if(collection.hasConsensusNucleus()){
			
			SignalGroup random = new SignalGroup();
			random.setGroupName("Random distribution");
			random.setFolder( new File(""));
			
			getDataset().getCollection()
			.addSignalGroup(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID, random);
			
			ShellRandomDistributionCreator sr = new ShellRandomDistributionCreator(collection.getConsensusNucleus(), shells);

			double[] c = sr.getProportions();
			
			double[] err = new double[c.length];
			for(int i=0; i<c.length; i++){
				err[i] = 0;
			}

			try{

				List<Double> list = new ArrayConverter(c).toDoubleList();
				List<Double> errList = new ArrayConverter(err).toDoubleList();

				ShellResult randomResult = new ShellResult(list,  errList);

				getDataset().getCollection()
				.getSignalGroup(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)
				.setShellResult(randomResult);
			} catch(ArrayConversionException e){
				error("Conversion error", e);
			}
		}
		
		
	}

}
