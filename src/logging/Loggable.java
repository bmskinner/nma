package logging;

import java.util.logging.Level;

public interface Loggable {

	public void log(Level level, String message);
    
    /**
     * Log an error to the program log window and to the dataset
     * debug file. Logs with Level.SEVERE
     * @param message the error messsage
     * @param t the exception
     */
	public void logError(String message, Throwable t);
}
