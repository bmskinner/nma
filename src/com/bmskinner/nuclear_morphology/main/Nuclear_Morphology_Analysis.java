package com.bmskinner.nuclear_morphology.main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;

import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.logging.DebugFileFormatter;
import com.bmskinner.nuclear_morphology.logging.DebugFileHandler;
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
	 * Keep a strong reference to the loggers so they can be accessed
	 * by all other classes implementing the Loggable interface
	 */
//	private static final Logger programLogger = Logger.getLogger(Loggable.PROGRAM_LOGGER);	
	private static final Logger errorLogger = Logger.getLogger(Loggable.ERROR_LOGGER);
	
	private static final ThreadManager threadManager = ThreadManager.getInstance();		
	
	// Store which plugins have been found
//	private HashMap<String, Boolean>  requiredFiles = new HashMap<String, Boolean>();
	
//	private MainWindow mw;
	
	
//	// The plugins that are needed for the program to start
//	private static String[] fileNames = {
////			"commons-math3",
////			"jcommon",
////			"jdistlib",
////			"jebl",
////			"jfreechart",
////			"swingx-all",
////			"weka",
////			"AnalyzeSkeleton",
////			"MorphoLibJ",
////			"jfreesvg"
//	};
	
	
	private Nuclear_Morphology_Analysis(String[] args){
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
	
//	public CommandParser getParser(){
//		return parser;
//	}
	
	
	/**
	 * Reset all found files to false
	 */
//	private void clearFileList(){
//		for(String s : fileNames){
//			requiredFiles.put(s, false);
//		}
//	}
	
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
		// Add a log file for program errors
		
		try {
			// Get the location of the jar file
			File dir =  Importer.getProgramDir();
			
			File errorFile = new File(dir, "error.log");
			System.out.println(errorFile.getAbsolutePath());

			DebugFileHandler errorHandler = new DebugFileHandler(errorFile);
			errorHandler.setFormatter(new DebugFileFormatter());
			errorLogger.addHandler(errorHandler);
			errorLogger.setLevel(Loggable.TRACE);
			
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
		
		try {
			load();
		} catch(Exception e){
			e.printStackTrace();
		} finally {
			splash.dispose();
		}
	}
	
//	/**
//	 * Load the program as standalone
//	 */
//	private void runStandalone(){
//		loadLogger();
//		try {
//			
//			// load the config file properties
//			new PropertiesReader();
//			loadMainWindow(true);
//		} catch(Exception e){
//			error("Error loading main window", e);
//			System.err.println("Error loading main window");
//			e.printStackTrace();
//		} 
//		
//			
//		
//	}
//	
//	private void loadMainWindow(boolean standalone){
//	    
//	    Runnable r = () -> {
//
//                IJ.setBackgroundColor(0, 0, 0);  // default background is black
//                try {
//                    UIManager.setLookAndFeel(
//                            UIManager.getSystemLookAndFeelClassName());
//                } catch (Exception e) {
//
//                    logToImageJ("Error initialising", e);
//                }
//
//                mw = new MainWindow(standalone);
//                mw.setVisible(true);
//        };
//		java.awt.EventQueue.invokeLater( r );
//	}
	
	/*
	 * Check all dependencies are present, the
	 * Java version is correct and load the main window
	 */
	public void load()  {

		try {
			
//			if(checkPlugins()){ 
				// load the config file properties
//				new PropertiesReader();
				
				// Check the ImageJ background colour settings
				// This must be made consistent on all platforms
				Prefs.blackBackground = true;
//				ThresholdAdjuster.update();

				
//				loadMainWindow(false);

//			} else {
//
//				displayMissingPlugins();
//				IJ.log("Unable to launch the Nuclear Morphology Analysis plugin for ImageJ");
//				IJ.log("This is because a required plugin is missing");
//				IJ.log("The names of the missing plugins are listed above");
//				IJ.log("Visit the project wiki for links to download missing plugins:");
//				IJ.log("https://bitbucket.org/bmskinner/nuclear_morphology/wiki/Installation");
//
//			}


		} catch (Exception e) {
			logToImageJ("Error initialising", e);
		} 
		
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
		} else {
			path = "splash.gif";
			urlToGif = cl.getResource(path);
			return urlToGif;
		}
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
	
	/**
	 * Check the given directory for files
	 * @param dir
	 */
//	private void checkDir(File dir){
//				
//		if(dir==null){
//			return;
//		}
//		
//		if(allPluginsFound()){ // Don't waste time if they have all been found so far
//			return;
//		}
//		
//		if( ! dir.exists()){
//			return;
//		}
//		if( ! dir.isDirectory()){
//			return;
//		}
//		
////		List<String> toCheck = Arrays.stream(fileNames)
////			.filter( s -> requiredFiles.get(s)==false )
////			.collect(Collectors.toList());
//		
//		if(dir.listFiles()==null){
//			return;
//		}
//				
//
//		for(File file : dir.listFiles()){
//			
//			if(file.isDirectory()){
//				continue;
//			}
//				
//			for(String s : toCheck){
//				
//				if(file.getName().startsWith(s)){
//					requiredFiles.put(s, true);
//				}
//				
//			}
//		}
//	}
	
//	/**
//	 * Check if all the plugins needed have been found yet
//	 * @return
//	 */
//	private boolean allPluginsFound(){
//		long count = Arrays.stream(fileNames)
//				.filter( s -> requiredFiles.get(s)==false )
//				.count();
//		
//		return count == 0;
//			
//	}
	
//	private void displayMissingPlugins(){
//		// report missing jars
//		for(String s : requiredFiles.keySet()){
//			if(requiredFiles.get(s)==false){
//				IJ.log("Cannot find a required plugin: "+s);
//			}
//		}
//	}
	
	/**
	 * Look in the likely plugins folders for the required plugins.
	 * @return
	 */
//	private boolean checkPlugins(){
//		
//		clearFileList(); // set all files to false
//		
//		String pluginDirName = IJ.getDirectory("plugins");
//		
//		File pluginDir = new File(pluginDirName);
//		File oldJarDir = new File(pluginDirName, "jars");
//		File jarDir    = new File(pluginDirName, "Nuclear_Morphology_Analysis");
//		
//
//		// check the plugins directory directly
//		checkDir(pluginDir);
//		
//		
//		/*
//		 * Check the new jar dir for jars
//		 */
//		checkDir(jarDir);
//		
//		/*
//		 * Check the old folder for jars (optional storage in 1.12.0 and earlier)
//		 */
//		checkDir(oldJarDir);
//		
//				
//		return allPluginsFound();
//	}
}
