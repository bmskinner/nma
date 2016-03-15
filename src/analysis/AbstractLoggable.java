package analysis;

import java.util.logging.Level;
import java.util.logging.Logger;

import logging.Loggable;

public class AbstractLoggable implements Loggable {
	
//	protected transient static final Logger programLogger =  Logger.getLogger("ProgramLogger"); // log to the program LogPanel
//	protected transient static Logger fileLogger = null;
	
    /**
     * Log the given message to the program log window and to the dataset
     * debug file
     * @param level the log level
     * @param message the message to log
     */
    public void log(Level level, String message){
//    	if(fileLogger!=null){
//    		fileLogger.log(level, message);
//    	}
    	
    	Logger.getLogger("ProgramLogger").log(level, message);
    }
    
    /**
     * Log an error to the program log window and to the dataset
     * debug file. Logs with Level.SEVERE
     * @param message the error messsage
     * @param t the exception
     */
    public void logError(String message, Throwable t){
//    	if(fileLogger!=null){
//    		fileLogger.log(Level.SEVERE, message, t);
//    	}
    	Logger.getLogger("ProgramLogger").log(Level.SEVERE, message, t);
    }

}
