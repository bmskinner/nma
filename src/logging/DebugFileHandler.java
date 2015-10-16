package logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;

public class DebugFileHandler extends FileHandler {
	
	private static boolean APPEND = true; 
	private static int LIMIT = 10000000; 
	private static int COUNT = 1; 
	
	public DebugFileHandler(File logFile) throws SecurityException, IOException{
		super(logFile.getAbsolutePath(), LIMIT, COUNT, APPEND);
	}

}
