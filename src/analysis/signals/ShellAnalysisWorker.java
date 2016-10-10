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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import logging.DebugFileHandler;
import utility.Constants;
import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import components.CellCollection;
import components.nuclear.NuclearSignal;
import components.nuclear.ShellResult;
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
				
				ShellDetector shellAnalyser = new ShellDetector(n, shells);

				shellAnalyser.createShells();


				for(UUID signalGroup : n.getSignalCollection().getSignalGroupIDs()){
					if(collection.getSignalManager().hasSignals(signalGroup)){
						List<NuclearSignal> signals = n.getSignalCollection().getSignals(signalGroup); 
						
						File imageFile = n.getSignalCollection().getSourceFile(signalGroup);
						ImageStack signalStack = new ImageImporter(imageFile).importImage();
						


						ShellCounter counter = counters.get(signalGroup);

						for(NuclearSignal s : signals){
							try {

								
								double[] signalPerShell = shellAnalyser.findShell(s, signalStack);
								counter.addValues(signalPerShell);
							} catch (Exception e) {
								logError( "Error in signal in shell analysis", e);
							}
						} // end for signals
					} // end if signals
				}
				publish(progress);
				progress++;
			}
			firePropertyChange("Cooldown", getProgress(), Constants.Progress.COOLDOWN.code());

			// get stats and export
			for(UUID group : counters.keySet()){
				if(collection.getSignalManager().hasSignals(group)){
					ShellCounter channelCounter = counters.get(group);

					getDataset().getCollection()
						.getSignalGroup(group)
						.setShellResult(new ShellResult(channelCounter.getMeans(), channelCounter.getStandardErrors()));

				}
			}
			
			log("Shell analysis complete");
		} catch (Exception e) {
			logError( "Error in shell analysis", e);	
			return false;
		}
		return true;
	}
		
	/**
	 * Perform shell analysis on the given collection
	 * @param collection the collection of nuclei to analyse
	 * @param shells the number of shells per nucleus
	 */
	public boolean run(AnalysisDataset dataset, int shells){
		
		CellCollection collection = dataset.getCollection();
		
//		logger = new Logger(collection.getDebugFile(), "ShellAnalysis");
		
		if(collection.getSignalManager().getSignalCount()==0){
			fine("No signals in population");
			return true; // only bother if there are signals
		}
		
		fine("Performing shell analysis with "+shells+" shells...");
		
		try {
			counters = new HashMap<UUID, ShellCounter>(0);

			for(UUID group : collection.getSignalManager().getSignalGroupIDs()){
				counters.put(group, new ShellCounter(shells));
			}

			// make the shells and measure the values
			
			for(Nucleus n : collection.getNuclei()){

				ShellDetector shellAnalyser = new ShellDetector(n);
				shellAnalyser.createShells();
//				shellAnalyser.exportImage();

				for(UUID group : n.getSignalCollection().getSignalGroupIDs()){
					if(collection.getSignalManager().hasSignals(group)){
						List<NuclearSignal> signalGroup = n.getSignalCollection().getSignals(group); 
						

						File imageFile = n.getSignalCollection().getSourceFile(group);
						ImageStack signalStack = new ImageImporter(imageFile).importImage();

						ShellCounter counter = counters.get(group);

						for(NuclearSignal s : signalGroup){
							try {
								double[] signalPerShell = shellAnalyser.findShell(s, signalStack);
								counter.addValues(signalPerShell);
							} catch (Exception e) {
								fileLogger.log(Level.SEVERE, "Error in signal in shell analysis", e);			
							}
						} // end for signals
					} // end if signals
				}
			}

			// get stats and export
			for(UUID group : counters.keySet()){
				if(collection.getSignalManager().hasSignals(group)){
					ShellCounter channelCounter = counters.get(group);
					dataset.getCollection()
					.getSignalGroup(group)
					.setShellResult(new ShellResult(channelCounter.getMeans(), channelCounter.getStandardErrors()));
				}
			}
			
			fine("Shell analysis complete");
		} catch (Exception e) {
			error("Error in shell analysis", e);
			return false;
		}
		return true;
	}
}
