package analysis;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import logging.DebugFileFormatter;
import logging.DebugFileHandler;
import analysis.nucleus.NucleusDetector;
import utility.Constants;

/**
 * This class provides progress and done signalling control for SwingWorker analyses.
 * It provides a consistent process() and done() methods, allowing doInBackground() to 
 * be overridden as needed for each worker
 * @author bms41
 *
 */
public abstract class AnalysisWorker extends SwingWorker<Boolean, Integer>{

	
	private int progressTotal; // the maximum value for the progress bar
	
	protected static Logger programLogger; // log to the program LogPanel
	protected static Logger fileLogger; // log to the active dataset log file
	protected static final Level FILE_DEBUG_LEVEL = Level.ALL;
	protected int progressCount = 0;

	private File logFile = null;
    
    private final AnalysisDataset activeDataset;
    
    public AnalysisWorker(final AnalysisDataset dataset, final Logger programLogger){
    	this.activeDataset = dataset;
    	AnalysisWorker.programLogger = programLogger;
    	if(dataset!=null){
    		AnalysisWorker.fileLogger = Logger.getLogger(this.getClass().getName());
    		fileLogger.setLevel(FILE_DEBUG_LEVEL);
    		fileLogger.addHandler(dataset.getLogHandler());
    	} else {
    		fileLogger = programLogger;
    	}

    	programLogger.log(Level.FINEST, "Created worker");
    	
    }
    
    /**
     * Constructor to use when a dataset has not yet been created. It takes a null dataset,
     * a program logger and a file for logging
     * @param dataset
     * @param programLogger
     * @param debugFile
     */
    public AnalysisWorker(final AnalysisDataset dataset, final Logger programLogger, final File debugFile){
    	this(dataset, programLogger);
    	this.logFile = debugFile;
    	programLogger.log(Level.FINEST, "Creating log file handler");
		DebugFileHandler handler = null;
		try {
			
			AnalysisWorker.fileLogger = Logger.getLogger(this.getClass().getName());
			handler = new DebugFileHandler(debugFile);
			handler.setFormatter(new DebugFileFormatter());
			fileLogger.addHandler(handler);
			fileLogger.setLevel(FILE_DEBUG_LEVEL);

		} catch (SecurityException e1) {
			programLogger.log(Level.SEVERE, "Could not create the log file handler", e1);
		} catch (IOException e1) {
			programLogger.log(Level.SEVERE, "Could not create the log file handler", e1);
		}
		log(Level.FINEST, "Created worker");
    }
    
    /**
     * Log the given message to the program log window and to the dataset
     * debug file
     * @param level the log level
     * @param message the message to log
     */
    protected static void log(Level level, String message){
    	fileLogger.log(level, message);
		programLogger.log(level, message);
    }
    
    /**
     * Log an error to the program log window and to the dataset
     * debug file. Logs with Level.SEVERE
     * @param message the error messsage
     * @param t the exception
     */
    protected static void logError(String message, Throwable t){
    	fileLogger.log(Level.SEVERE, message, t);
		programLogger.log(Level.SEVERE, message, t);
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
    
    protected void fireCooldown(){
    	firePropertyChange("Cooldown", getProgress(), Constants.Progress.COOLDOWN.code());  
    }
    
    @Override
    public void done() {
    	
    	log(Level.FINEST, "Worker completed task");

        try {
            if(this.get()){
            	log(Level.FINEST, "Firing trigger for sucessful task");
                firePropertyChange("Finished", getProgress(), Constants.Progress.FINISHED.code());            

            } else {
            	log(Level.FINEST, "Firing trigger for failed task");
                firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
            }
        } catch (InterruptedException e) {
        	logError("Interruption error in worker", e);
        } catch (ExecutionException e) {
        	logError("Execution error in worker", e);

       } finally{
    	   
    	   log(Level.FINEST, "Closing log file handlers");

    	   if(activeDataset!=null){
    		   
    		   // if dataset is null, the fileLogger was set to be the programLogger
    		   // and we don't want to close that
    		   closeLogFileHandlers();
    		   
    	   } else {
    		   // No dataset was given, so no intrinsic log file
    		   // But if a separate log file was provided, we need to close it
    		   if(logFile!=null){
    			   closeLogFileHandlers();
    		   }
    	   }
       }

    } 
    
    private void closeLogFileHandlers(){
    	for(Handler h : fileLogger.getHandlers()){
			   h.close();
			   fileLogger.removeHandler(h);
		   }
    }
	

}
