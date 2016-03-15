package logging;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This interface provides default methods for logging to 
 * the program log panel
 * @author bms41
 *
 */
public interface Loggable {

    /**
     * Log the given message to the program log window
     * @param level the log level
     * @param message the message to log
     */
	default void log(Level level, String message){
		Logger.getLogger("ProgramLogger").log(level, message);
	}
    
    /**
     * Log an error to the program log window and to the dataset
     * debug file. Logs with Level.SEVERE
     * @param message the error messsage
     * @param t the exception
     */
	default void logError(String message, Throwable t){
		Logger.getLogger("ProgramLogger").log(Level.SEVERE, message, t);
	}
	
	/**
     * Log an error to the program log window and to the dataset
     * debug file. Logs with Level.SEVERE
     * @param message the error messsage
     * @param t the exception
     */
	default void log(Level level, String message, Throwable t){
		Logger.getLogger("ProgramLogger").log(level, message, t);
	}
}
