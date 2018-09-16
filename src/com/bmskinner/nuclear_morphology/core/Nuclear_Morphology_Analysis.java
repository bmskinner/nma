package com.bmskinner.nuclear_morphology.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;

import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogFileFormatter;
import com.bmskinner.nuclear_morphology.logging.LogFileHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.IJ;
import ij.Prefs;

/**
 * This is the main class that runs the program.
 * @author bms41
 * @since 1.13.7
 *
 */
public class Nuclear_Morphology_Analysis
	implements Loggable {
	
	private static Nuclear_Morphology_Analysis instance; // for launching without ImageJ
	private CommandParser parser;
	
	/*
	 * Keep a strong reference to the logger so they can be accessed
	 * by all other classes implementing the Loggable interface
	 */
	private static final Logger errorLogger = Logger.getLogger(Loggable.ERROR_LOGGER);
	private static final Logger programLogger = Logger.getLogger(Loggable.PROGRAM_LOGGER);
	private static final Logger consoleLogger = Logger.getLogger(Loggable.CONSOLE_LOGGER);
	
	private static final ThreadManager threadManager = ThreadManager.getInstance();		

	
	
	private Nuclear_Morphology_Analysis(String[] args){
		loadLogger();
	    this.parser = new CommandParser(args);
	}
	
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
			// Get the location of the jar file
			File dir =  Importer.getProgramDir();
			
			File errorFile = new File(dir, "error.log");

			LogFileHandler errorHandler = new LogFileHandler(errorFile, new LogFileFormatter());
			Logger.getLogger(ERROR_LOGGER).addHandler(errorHandler);
			Logger.getLogger(ERROR_LOGGER).setLevel(Loggable.TRACE);
			
			Logger.getLogger(CONSOLE_LOGGER).addHandler(new ConsoleHandler(new LogPanelFormatter()));
			Logger.getLogger(CONSOLE_LOGGER).setLevel(Level.FINE);
			
		} catch (SecurityException e) {
			logToImageJ("Error initialising", e);
			e.printStackTrace();
		} catch (IOException e) {
			logToImageJ("Error initialising", e);
			e.printStackTrace();
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
