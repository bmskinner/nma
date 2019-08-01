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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;

import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogFileFormatter;
import com.bmskinner.nuclear_morphology.logging.LogFileHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;

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
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
//	private static final ThreadManager threadManager = ThreadManager.getInstance();		

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
			IJ.log("Unable to open Nuclear Morphology Analysis");
			IJ.log("Java version 8 is required");
			IJ.log("Update ImageJ to a version bundled with Java 8");
			IJ.log("This is the version called 'bundled with Java 1.8.0_xx' here:");
			IJ.log("http://rsb.info.nih.gov/ij/download.html");
			return false;
		}
		return true;
	}
	
	private void loadLogger(){
		
		try {
			
			// Remove existing handlers
			for(Handler h : LOGGER.getHandlers())
				LOGGER.removeHandler(h);
			
			LOGGER.setLevel(Level.FINE);

			Handler consoleHander = new ConsoleHandler(new ConsoleFormatter());
			LOGGER.addHandler(consoleHander);
			consoleHander.setLevel(Level.FINER);

			/* Get the location of the jar file
			 * and create a log file in the same
			 * directory if not present
			 */
			File dir =  Importer.getProgramDir();
			LOGGER.config("Program dir: "+dir.getAbsolutePath());
			File errorFile = new File(dir, "error.log");
			LOGGER.config("Log file: "+errorFile.getAbsolutePath());
			if(errorFile.createNewFile()) {
				LOGGER.fine("Created new log file");
			}

			// Log stack traces to the log file for debugging
			LogFileHandler fileHandler = new LogFileHandler(errorFile, new LogFileFormatter());			
			LOGGER.addHandler(fileHandler);
			fileHandler.setLevel(Loggable.STACK);			
			ThreadManager.getInstance();
			
		} catch (SecurityException |IOException e ) {
			LOGGER.log(Level.SEVERE, "Error initialising logger: "+e.getMessage(), e);
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
