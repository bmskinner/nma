package no.analysis;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import utility.Constants;
import utility.Logger;
import no.collections.CellCollection;
import no.components.NuclearSignal;
import no.components.ShellResult;
import no.nuclei.Nucleus;

public class ShellAnalysis extends SwingWorker<Boolean, Integer> {
	
	private static Logger logger;
	private final AnalysisDataset dataset;
	private final int shells;
	
	private static Map<Integer, ShellCounter> counters = new HashMap<Integer, ShellCounter>(0);
	
	
	public ShellAnalysis(AnalysisDataset dataset, int shells){
		this.dataset = dataset;
		this.shells = shells;
		
	}
	
	@Override
	protected void process(List<Integer> integers){
		// get last published value
		int amount = integers.get( integers.size() - 1 );
		
		// total number of nuclei
		int total = dataset.getCollection().getNucleusCount();
		
		// express as percent as int
		int progress = (int) (((double) amount / (double) total)*100);
		setProgress(progress);
	}
	
	@Override
	protected Boolean doInBackground() {
		
		CellCollection collection = dataset.getCollection();
		
		logger = new Logger(collection.getDebugFile(), "ShellAnalysis");
		
		if(collection.getSignalCount()==0){
			logger.log("No signals in population",Logger.DEBUG);
			return true; // only bother if there are signals
		}
		
		logger.log("Performing shell analysis with "+shells+" shells...");
		
		try {
			counters = new HashMap<Integer, ShellCounter>(0);

			for(int channel : collection.getSignalGroups()){
				counters.put(channel, new ShellCounter(shells));
			}

			// make the shells and measure the values
			
			int progress = 0;
			for(Nucleus n : collection.getNuclei()){

				ShellCreator shellAnalyser = new ShellCreator(n, logger.getLogfile());
				shellAnalyser.createShells();
				shellAnalyser.exportImage();

				for(int channel : n.getSignalGroups()){
					if(collection.hasSignals(channel)){
						List<NuclearSignal> signalGroup = n.getSignals(channel); 

						ShellCounter counter = counters.get(channel);

						for(NuclearSignal s : signalGroup){
							try {
								double[] signalPerShell = shellAnalyser.findShell(s, channel);
								counter.addValues(signalPerShell);
							} catch (Exception e) {
								logger.log("Error in signal in shell analysis: "+e.getMessage(), Logger.ERROR);
							}
						} // end for signals
					} // end if signals
				}
				publish(progress);
				progress++;
			}
			firePropertyChange("Cooldown", getProgress(), Constants.PROGRESS_COOLDOWN);

			// get stats and export
			for(int channel : counters.keySet()){
				if(collection.hasSignals(channel)){
					ShellCounter channelCounter = counters.get(channel);
					channelCounter.export(new File(collection.getLogFileName( "log.shells."+channel  )));
					dataset.addShellResult(channel, new ShellResult(channelCounter.getMeans(), channelCounter.getStandardErrors()));
				}
			}
			
			logger.log("Shell analysis complete");
		} catch (Exception e) {
			logger.log("Error in shell analysis: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
			return false;
		}
		return true;
	}
	
	@Override
	public void done() {
		try {
			if(this.get()){
				firePropertyChange("Finished", getProgress(), Constants.PROGRESS_FINISHED);
			} else {
				firePropertyChange("Error", getProgress(), Constants.PROGRESS_ERROR);
			}
		} catch (InterruptedException e) {
			logger.log("Error in shell analysis: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		} catch (ExecutionException e) {
			logger.log("Error in shell analysis: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		}
		
	}
	
	/**
	 * Perform shell analysis on the given collection
	 * @param collection the collection of nuclei to analyse
	 * @param shells the number of shells per nucleus
	 */
	public static boolean run(AnalysisDataset dataset, int shells){
		
		CellCollection collection = dataset.getCollection();
		
		logger = new Logger(collection.getDebugFile(), "ShellAnalysis");
		
		if(collection.getSignalCount()==0){
			logger.log("No signals in population",Logger.DEBUG);
			return true; // only bother if there are signals
		}
		
		logger.log("Performing shell analysis with "+shells+" shells...");
		
		try {
			counters = new HashMap<Integer, ShellCounter>(0);

			for(int channel : collection.getSignalGroups()){
				counters.put(channel, new ShellCounter(shells));
			}

			// make the shells and measure the values
			
			for(Nucleus n : collection.getNuclei()){

				ShellCreator shellAnalyser = new ShellCreator(n, logger.getLogfile());
				shellAnalyser.createShells();
				shellAnalyser.exportImage();

				for(int channel : n.getSignalGroups()){
					if(collection.hasSignals(channel)){
						List<NuclearSignal> signalGroup = n.getSignals(channel); 

						ShellCounter counter = counters.get(channel);

						for(NuclearSignal s : signalGroup){
							try {
								double[] signalPerShell = shellAnalyser.findShell(s, channel);
								counter.addValues(signalPerShell);
							} catch (Exception e) {
								logger.log("Error in signal in shell analysis: "+e.getMessage(), Logger.ERROR);
							}
						} // end for signals
					} // end if signals
				}
			}

			// get stats and export
			for(int channel : counters.keySet()){
				if(collection.hasSignals(channel)){
					ShellCounter channelCounter = counters.get(channel);
					channelCounter.export(new File(collection.getLogFileName( "log.shells."+channel  )));
					dataset.addShellResult(channel, new ShellResult(channelCounter.getMeans(), channelCounter.getStandardErrors()));
				}
			}
			
			logger.log("Shell analysis complete");
		} catch (Exception e) {
			logger.log("Error in shell analysis: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
			return false;
		}
		return true;
	}
}
