package com.bmskinner.nuclear_morphology.io;

/**
 * Interface for all export classes. Defines file extensions.
 * @author ben
 *
 */
public interface Exporter {
	static final String TAB_FILE_EXTENSION = ".txt";
	
	static final String NEWLINE = System.getProperty("line.separator"); 
}
