/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.core;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.io.Io;

/**
 * This is the main class that runs the program.
 * @author bms41
 * @since 1.13.7
 *
 */
public class NuclearMorphologyAnalysis {
	
	private static NuclearMorphologyAnalysis instance;
	
	/** Initialise the logger for the project namespace */
	private static final Logger LOGGER = Logger.getLogger("com.bmskinner.nuclear_morphology");
	
	static {
		
		// Create a log folder in the user home dir if needed
		File logFolder = Io.getConfigDir();
    	
    	if(!logFolder.exists()) {
    		logFolder.mkdirs();
    	}
		
    	try {
    		LogManager.getLogManager()
    		.readConfiguration(NuclearMorphologyAnalysis.class.getClassLoader()
    				.getResourceAsStream("logging.properties"));
    	} catch (SecurityException | IOException e) {
    		LOGGER.log(Level.SEVERE, "Unable to make log manager", e);
    	}
	}
	
	/**
	 * Private constructor used when launching as a standalone program
	 * @param args
	 */
	private NuclearMorphologyAnalysis(String[] args){
		logConfiguration();
	    new CommandLineParser(args);
	}
	
	public static void main(String[] args){
		instance = new NuclearMorphologyAnalysis(args);
	}
	
	/**
	 * Get this program instance
	 * @return
	 */
	public static NuclearMorphologyAnalysis getInstance(){
		return instance;
	}
		
	private void logConfiguration(){
		
		try {
				LOGGER.config("Log file location: " + Io.getConfigDir().getAbsolutePath());
				GlobalOptions.getInstance().setString(GlobalOptions.LOG_DIRECTORY_KEY, 
						Io.getConfigDir().getAbsolutePath());
			
			LOGGER.config("OS: "+System.getProperty("os.name")+", version "+System.getProperty("os.version")+", "+System.getProperty("os.arch"));
			LOGGER.config("JVM: "+System.getProperty("java.vendor")+", version "+System.getProperty("java.version"));
			LOGGER.config("NMA version: "+Version.currentVersion());
			
			// First invokation of the thread manager will log available resources 
			ThreadManager.getInstance();
		} catch (SecurityException e ) {
			LOGGER.log(Level.SEVERE, "Error initialising logger", e);
		}
	}
}
