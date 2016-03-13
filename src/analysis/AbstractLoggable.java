package analysis;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AbstractLoggable {
	
	protected static final Logger programLogger =  Logger.getLogger("ProgramLogger"); // log to the program LogPanel
	protected static Logger fileLogger = null;
	
    /**
     * Log the given message to the program log window and to the dataset
     * debug file
     * @param level the log level
     * @param message the message to log
     */
    protected static void log(Level level, String message){
    	if(fileLogger!=null){
    		fileLogger.log(level, message);
    	}
		programLogger.log(level, message);
    }
    
    /**
     * Log an error to the program log window and to the dataset
     * debug file. Logs with Level.SEVERE
     * @param message the error messsage
     * @param t the exception
     */
    protected static void logError(String message, Throwable t){
    	if(fileLogger!=null){
    		fileLogger.log(Level.SEVERE, message, t);
    	}
		programLogger.log(Level.SEVERE, message, t);
    }

}
