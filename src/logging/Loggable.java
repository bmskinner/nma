package logging;

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
	
	public static final String ROOT_LOGGER = "";
	public static final String PROGRAM_LOGGER = "ProgramLogger";

    /**
     * Log the given message to the program log window
     * @param level the log level
     * @param message the message to log
     */
	default void log(Level level, String message){
//		Logger l = Logger.getLogger( this.getClass().getName());
//		l.setUseParentHandlers(true);
//		l.log(level, message); // will be passed upwards to root logger
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
     * Log an error to the program log window with Level.SEVERE
     * @param message the error messsage
     * @param t the exception
     */
	default void error(String message, Throwable t){
		Logger.getLogger(PROGRAM_LOGGER).log(Level.SEVERE, message, t);
	}
	
	/**
     * Log an error to the program log window with Level.FINE
     * @param message the error messsage
     * @param t the exception
     */
	default void fine(String message){
		Logger.getLogger(PROGRAM_LOGGER).log(Level.FINE, message);
	}
	
	/**
     * Log an error to the program log window with Level.FINER
     * @param message the error messsage
     * @param t the exception
     */
	default void finer(String message){
		Logger.getLogger(PROGRAM_LOGGER).log(Level.FINER, message);
	}
	
	/**
     * Log an error to the program log window with Level.FINEST
     * @param message the error messsage
     * @param t the exception
     */
	default void finest(String message){
		Logger.getLogger(PROGRAM_LOGGER).log(Level.FINEST, message);
	}
	
	/**
     * Log an error to the program log window with Level.WARNING
     * @param message the error messsage
     * @param t the exception
     */
	default void warn(String message){
		Logger.getLogger(PROGRAM_LOGGER).log(Level.WARNING, message);
	}
	
	/**
     * Log an error to the program log window with Level.INFO
     * @param message the error messsage
     * @param t the exception
     */
	default void log(String message){
		Logger.getLogger(PROGRAM_LOGGER).log(Level.INFO, message);
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
