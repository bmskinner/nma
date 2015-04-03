package no.analysis;

import ij.IJ;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.collections.INuclearCollection;
import no.components.NuclearSignal;
import no.nuclei.INuclearFunctions;

public class ShellAnalysis {
	
	private static Map<Integer, ShellCounter> counters = new HashMap<Integer, ShellCounter>(0);
		
	/**
	 * Perform shell analysis on the given collection
	 * @param collection the collection of nuclei to analyse
	 * @param shells the number of shells per nucleus
	 */
	public static void run(INuclearCollection collection, int shells){
		
		if(collection.getSignalCount()==0){
			return; // only bother if there are signals
		}
		
		IJ.log("    Performing shell analysis...");
		
		counters = new HashMap<Integer, ShellCounter>(0);

		for(int channel : collection.getSignalChannels()){
			counters.put(channel, new ShellCounter(shells));
		}

		// make the shells and measure the values
		for(INuclearFunctions n : collection.getNuclei()){

			ShellCreator shellAnalyser = new ShellCreator(n);
			shellAnalyser.createShells();
			shellAnalyser.exportImage();

			for(int channel : n.getSignalChannels()){
				List<NuclearSignal> signalGroup = n.getSignals(channel); 
				if(!signalGroup.isEmpty()){
					ShellCounter counter = counters.get(channel);

					for(NuclearSignal s : signalGroup){
						try {
							double[] signalPerShell = shellAnalyser.findShell(s, channel);
							counter.addValues(signalPerShell);
						} catch (Exception e) {
							IJ.log("    Error in shell analysis: "+e.getMessage());;
						}
					} // end for signals
				} // end if signals
			}
		}

		// get stats and export
		for(int channel : counters.keySet()){
			counters.get(channel).export(new File(collection.getLogFileName( "log.shells."+channel  )));
		}
		IJ.log("    Shell analysis complete");
	}
}
