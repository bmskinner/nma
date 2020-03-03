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
import java.nio.file.Files;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;

import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogFileFormatter;
import com.bmskinner.nuclear_morphology.logging.LogFileHandler;

import ij.IJ;
import ij.Prefs;

/**
 * This is the main class that runs the program.
 * @author bms41
 * @since 1.13.7
 *
 */
public class Nuclear_Morphology_Analysis {
	
	private static Nuclear_Morphology_Analysis instance; // for launching without ImageJ
//	private CommandLineParser parser; // parse command line arguments and launch the UI
	
	/*
	 * Keep a strong reference to the logger so they can be accessed
	 * by all other classes implementing the Loggable interface
	 */
	private static final Logger LOGGER;
//	private static final Logger LOGGER = LoggerFactory.getLogger(Nuclear_Morphology_Analysis.class);
	
	
	static {
		String path = Nuclear_Morphology_Analysis.class.getClassLoader()
                .getResource("logging.properties")
                .getFile();
//		System.out.println(path);
//		System.out.println("Logging properties file exists: "+Files.exists(new File(path).toPath()));
		System.setProperty("java.util.logging.config.file", path);
		LOGGER = Logger.getLogger(Nuclear_Morphology_Analysis.class.getName());
	}
	
	/**
	 * Private constructor used when launching as a standalone program
	 * @param args
	 */
	private Nuclear_Morphology_Analysis(String[] args){
		loadLogger();
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
	
	private void loadLogger(){
		
		try {
			
			// Load the log file
			LOGGER.config("Loaded logger properties from: "+System.getProperty("java.util.logging.config.file"));
			LOGGER.config("Log file location: "+System.getProperty("user.dir"));
//			LOGGER.info("Loaded logging properties file");
//			LOGGER.config("Debug message");
//			LOGGER.warning("Warning message");
			
			// Remove existing handlers
//			for(Handler h : LOGGER.getHandlers())
//				LOGGER.removeHandler(h);
			
			// Overall level for the logger to respond to 
//			LOGGER.setLevel(Level.FINER);

			// Output to the console
//			Handler consoleHander = new ConsoleHandler(new ConsoleFormatter());
//			LOGGER.addHandler(consoleHander);
//			consoleHander.setLevel(Level.FINE);

			/* Get the location of the jar file
			 * and create a log file in the same
			 * directory if not present
			 */
//			File dir =  Io.getProgramDir();
//			LOGGER.config("Program dir: "+dir.getAbsolutePath());
//			File errorFile = new File(dir, "error.log");
//			LOGGER.config("Log file: "+errorFile.getAbsolutePath());
//			if(errorFile.createNewFile()) {
//				LOGGER.fine("Created new log file");
//			}

			// Log stack traces and config to the log file for debugging
//			LogFileHandler fileHandler = new LogFileHandler(errorFile, new LogFileFormatter());			
//			LOGGER.addHandler(fileHandler);
//			fileHandler.setLevel(Level.CONFIG);			
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
		
		loadLogger();
		
		/*
		 * Add a splash screen for long load times
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
