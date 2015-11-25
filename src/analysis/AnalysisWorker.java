package analysis;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import utility.Constants;

/**
 * This class provides progress and done signalling control for SwingWorker analyses
 * @author bms41
 *
 */
public abstract class AnalysisWorker extends SwingWorker<Boolean, Integer>{

	
	private int progressTotal; // the maximum value for the progress bar
	
	protected static Logger programLogger; // log to the program LogPanel
	protected static Logger fileLogger; // log to the active dataset log file
    
    private AnalysisDataset activeDataset;
    
    public AnalysisWorker(AnalysisDataset dataset, Logger programLogger){
    	this.activeDataset = dataset;
    	AnalysisWorker.programLogger = programLogger;
    	
    }
    
    protected void setProgressTotal(int i){
    	this.progressTotal = i;
    }
    
    protected int getProgressTotal(){
    	return this.progressTotal;
    }
    
    protected AnalysisDataset getDataset(){
    	return this.activeDataset;
    }
	
    @Override
    protected void process( List<Integer> integers ) {
        int amount = integers.get( integers.size() - 1 );
        int percent = (int) ( (double) amount / (double) progressTotal * 100);
        
        if(percent >= 0 && percent <=100){
        	setProgress(percent); // the integer representation of the percent
        }
    }
    
    @Override
    public void done() {
    	
    	programLogger.log(Level.FINEST, "Completed worker task; firing trigger");

        try {
            if(this.get()){
            	programLogger.log(Level.FINEST, "Firing trigger for sucessful task");
                firePropertyChange("Finished", getProgress(), Constants.Progress.FINISHED.code());            

            } else {
            	programLogger.log(Level.FINEST, "Firing trigger for error in task");
                firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
            }
        } catch (InterruptedException e) {
        	fileLogger.log(Level.SEVERE, "Interruption error in worker", e);
        } catch (ExecutionException e) {
        	fileLogger.log(Level.SEVERE, "Execution error in worker", e);

       }

    } 
	

}
