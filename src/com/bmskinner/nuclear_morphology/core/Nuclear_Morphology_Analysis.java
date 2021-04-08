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
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;

import com.bmskinner.nuclear_morphology.components.generic.Version;

import ij.IJ;
import ij.Prefs;

/**
 * This is the main class that runs the program.
 * @author bms41
 * @since 1.13.7
 *
 */
public class Nuclear_Morphology_Analysis {
	
	private static final String JAVA_UTIL_LOGGING_CONFIG_FILE = "java.util.logging.config.file";

	private static Nuclear_Morphology_Analysis instance; // for launching without ImageJ
	
	/** Initialise the logger for the project namespace */
	private static final Logger LOGGER = Logger.getLogger("com.bmskinner.nuclear_morphology");

	/** The folder to write and store logs */
    private static final String LOG_FOLDER_NAME = "logs";
    
    private static final File LOG_FOLDER;
	
	
	static {
		
		// Create a log folder in the user home dir if needed
		LOG_FOLDER = new File(System.getProperty("user.home"), LOG_FOLDER_NAME);
    	
    	if(!LOG_FOLDER.exists()) {
    		LOG_FOLDER.mkdirs();
    	}
		
		try {
			// If a logging properties file is specified in the launch parameters,
			// use it. Otherwise, default to the file within the jar
			String logConfigFile = System.getProperty(JAVA_UTIL_LOGGING_CONFIG_FILE);
			if(logConfigFile==null) {
				LogManager.getLogManager()
				.readConfiguration(Nuclear_Morphology_Analysis.class.getClassLoader()
						.getResourceAsStream("logging.properties"));
			}
		} catch (SecurityException | IOException e) {
			LOGGER.log(Level.SEVERE, "Unable to make log manager", e);
		}
	}
	
	/**
	 * Private constructor used when launching as a standalone program
	 * @param args
	 */
	private Nuclear_Morphology_Analysis(String[] args){
		logConfiguration();
	    new CommandLineParser(args);
	}
	
	
	/**
	 * Public constructor used when launching within ImageJ.
	 * Invokes the private constructor with no arguments.
	 */
	public Nuclear_Morphology_Analysis(){
	    String[] arr = new String[1];
        arr[0] = "";
        main(arr);
	}
	
	public static void main(String[] args){
		instance = new Nuclear_Morphology_Analysis(args);
	}
	
	public static Nuclear_Morphology_Analysis getInstance(){
		return instance;
	}
	
	private boolean checkJavaVersion(){
		
		if( ! IJ.isJava18() ){
			LOGGER.warning("Unable to open Nuclear Morphology Analysis");
			LOGGER.warning("Java version 8 is required");
			LOGGER.warning("Update ImageJ to a version bundled with Java 8");
			LOGGER.warning("This is the version called 'bundled with Java 1.8.0_xx' here:");
			LOGGER.warning("http://rsb.info.nih.gov/ij/download.html");
			return false;
		}
		return true;
	}
	
	private void logConfiguration(){
		
		try {
			String logFile = System.getProperty(JAVA_UTIL_LOGGING_CONFIG_FILE);
			if(logFile!=null) {
				LOGGER.config("Logger properties file specified: "
						+System.getProperty(JAVA_UTIL_LOGGING_CONFIG_FILE));
			} else {
				LOGGER.config("No external logging properties specified; using default logging config");
				LOGGER.config("Log file location: " + LOG_FOLDER.getAbsolutePath());
				GlobalOptions.getInstance().setString(GlobalOptions.LOG_DIRECTORY_KEY, 
													LOG_FOLDER.getAbsolutePath());
			}
			
			LOGGER.config("OS: "+System.getProperty("os.name")+", version "+System.getProperty("os.version")+", "+System.getProperty("os.arch"));
			LOGGER.config("JVM: "+System.getProperty("java.vendor")+", version "+System.getProperty("java.version"));
			LOGGER.config("Version: "+Version.currentVersion());
			// First invokation of the thread manager will log available resources 
			ThreadManager.getInstance();
		} catch (SecurityException e ) {
			LOGGER.log(Level.SEVERE, "Error initialising logger", e);
		}
	}
	
	/* 
     * The first method run when the plugin starts within ImageJ.
	 */
	public void run(String paramString){
		
		if(!checkJavaVersion())
			return;
		
		logConfiguration();
		
		/*
		 * Add a splash screen for long load times.
		 * Applies to ImageJ launch only
		 */
		final JWindow splash = createSplash();
		Prefs.blackBackground = true;		
		splash.dispose();
	}
			
	/**
	 * Get the URL for the splash screen gif
	 * @return
	 */
	private URL getSplashURL(){
		
		String path = "icons/splash.gif";
		ClassLoader cl = this.getClass().getClassLoader();
		URL urlToGif = cl.getResource(path);
		if(urlToGif!=null){
			return urlToGif;
		}
		path = "splash.gif";
		urlToGif = cl.getResource(path);
		return urlToGif;
	}
	
	/**
	 * Make the splash window from a JWindow, and make
	 * it visible. We're not using SplashScreen because the
	 * JVM has started with IJ, and so getSplashScreen() will
	 * return null.
	 * @return
	 */
	private JWindow createSplash(){
		JWindow window = new JWindow();
		window.getContentPane().add(
		    new JLabel("", new ImageIcon(getSplashURL()), SwingConstants.CENTER));
		window.setBounds(500, 150, 300, 200);
		window.setVisible(true);
		return window;
	}
}
