package logging;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import ij.IJ;

/**
 * This interface provides default methods for logging to 
 * the program log panel
 * @author bms41
 *
 */
public interface Loggable {
	
	public static final String PROGRAM_LOGGER = "ProgramLogger";

    /**
     * Log the given message to the program log window
     * @param level the log level
     * @param message the message to log
     */
	default void log(Level level, String message){
//		Logger l = Logger.getLogger();

//
//		for(Handler h : Logger.getLogger(PROGRAM_LOGGER).getHandlers()){
//			l.addHandler(h);
//			l.log(level, message);
//			l.removeHandler(h);
//		}
//		Logger.getLogger(PROGRAM_LOGGER).log(level, message + ":"+  this.getClass().getName());
		Logger.getLogger(PROGRAM_LOGGER).log(level, message );
	}
    
    /**
     * Log an error to the program log window and to the dataset
     * debug file. Logs with Level.SEVERE
     * @param message the error messsage
     * @param t the exception
     */
	default void logError(String message, Throwable t){
		Logger.getLogger(PROGRAM_LOGGER).log(Level.SEVERE, message, t);
	}
	
	/**
     * Log an error to the program log window and to the dataset
     * debug file. Logs with Level.SEVERE
     * @param message the error messsage
     * @param t the exception
     */
	default void log(Level level, String message, Throwable t){
		Logger.getLogger(PROGRAM_LOGGER).log(level, message, t);
	}
	
	default void logToImageJ(String message, Throwable t){
		IJ.log(message);
		IJ.log(t.getMessage());
		for(StackTraceElement el : t.getStackTrace()){
			IJ.log(el.toString());
		}
	}
	
	default void logIJ(String message){
		IJ.log(message);
	}
}
