package analysis;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import logging.DebugFileFormatter;
import logging.DebugFileHandler;
import logging.Loggable;
import utility.Constants;

/**
 * This class provides progress and done signalling control for SwingWorker analyses.
 * It provides a consistent process() and done() methods, allowing doInBackground() to 
 * be overridden as needed for each worker
 * @author bms41
 *
 */
public abstract class AnalysisWorker extends SwingWorker<Boolean, Integer> implements Loggable {

	private int progressTotal; // the maximum value for the progress bar
	
	protected static Logger fileLogger; // log to the active dataset log file
	protected static final Level FILE_DEBUG_LEVEL = Level.ALL;
	protected int progressCount = 0;
	
	protected final ForkJoinPool mainPool = new ForkJoinPool();
    
    private final IAnalysisDataset activeDataset;
    
    /**
     * Default constructor using a dataset. If possible, a file logger will be added
     * for the dataset's debug file
     * @param dataset
     */
    public AnalysisWorker(final IAnalysisDataset dataset){
    	this.activeDataset = dataset;

    	if(dataset!=null){
    		AnalysisWorker.fileLogger = Logger.getLogger(this.getClass().getName());
    		fileLogger.setLevel(FILE_DEBUG_LEVEL);
    		try {
    			
    			Handler h = dataset.getLogHandler();
    			
    			if(h!=null){
    				fileLogger.addHandler(h);
    			}
    			
    		} catch(Exception e){
    			error("Error getting log file handler", e);
    			fileLogger = null;
    		}
    	} else {
    		fileLogger = null;
    	}

    	finest("Created worker");
    	
    }
    
    /**
     * Constructor to use when a dataset has not yet been created. It takes a null dataset,
     * a program logger and a file for logging
     * @param dataset
     * @param programLogger
     * @param debugFile
     */
    public AnalysisWorker(final IAnalysisDataset dataset, final File debugFile){
    	this(dataset);

    	finest("Creating log file handler");
		DebugFileHandler handler = null;
		try {
			
			AnalysisWorker.fileLogger = Logger.getLogger(this.getClass().getName());
			handler = new DebugFileHandler(debugFile);
			handler.setFormatter(new DebugFileFormatter());
			fileLogger.addHandler(handler);
			fileLogger.setLevel(FILE_DEBUG_LEVEL);

		} catch (SecurityException e1) {
			log(Level.SEVERE, "Could not create the log file handler", e1);
			fileLogger = null;

		} catch (IOException e1) {
			log(Level.SEVERE, "Could not create the log file handler", e1);
			fileLogger = null;
		}
		log(Level.FINEST, "Created worker");
    }
    
    /**
     * Log the given message to the program log window and to the dataset
     * debug file
     * @param level the log level
     * @param message the message to log
     */
    @Override
    public void log(Level level, String message){
    	if(fileLogger!=null){
    		fileLogger.log(level, message);
    	}
		Loggable.super.log(level, message);
    }
    
    
    
    @Override
    public void fine(String message){
    	log(Level.FINE, message);
    }
    
    @Override
    public void finer(String message){
    	log(Level.FINER, message);
    }
    
    @Override
    public void finest(String message){
    	log(Level.FINEST, message);
    }
    
    @Override
    public void warn(String message){
    	log(Level.WARNING, message);
    }
    
    @Override
    public void error(String message, Throwable t){
    	if(fileLogger!=null){
    		fileLogger.log(Level.SEVERE, message, t);
    	}
    	Loggable.super.error( message, t);
    }
    
    
    protected void setProgressTotal(int i){
    	this.progressTotal = i;
    }
    
    protected int getProgressTotal(){
    	return this.progressTotal;
    }
    
    protected IAnalysisDataset getDataset(){
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
    	
    	finest("Worker completed task");

    	finest("Closing log file handlers");
    	closeLogFileHandlers();
    	
    	finest("Shutting main thread pool");
    	mainPool.shutdown();

    	 try {
            if(this.get()){
            	finest("Firing trigger for sucessful task");
                firePropertyChange("Finished", getProgress(), Constants.Progress.FINISHED.code());            

            } else {
            	finest("Firing trigger for failed task");
                firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
            }
    	 } catch(StackOverflowError e){
    		 warn("Stack overflow detected");
    		 fine("Stack overflow in worker", e);
    		 firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
        } catch (InterruptedException e) {
        	error("Interruption error in worker", e);
        	firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
        } catch (ExecutionException e) {
        	if(e.getCause() instanceof java.lang.OutOfMemoryError){
        		warn("Error: Not enough memory!");
        	} else {
        		warn("Unable to complete task due to an internal error: "+e.getCause().getClass().getSimpleName());
        		warn("Try restarting the program");
        		fine("Execution error in worker", e);
        		
        	}
        	
        	firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
       }

    } 
    
    private void closeLogFileHandlers(){
    	
    	if(fileLogger!=null){
    		for(Handler h : fileLogger.getHandlers()){
    			h.close();
    			fileLogger.removeHandler(h);
    		}
    	}
    }
	

}
