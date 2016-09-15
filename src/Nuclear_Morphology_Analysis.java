/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

import ij.IJ;
import ij.plugin.PlugIn;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import gui.MainWindow;
import logging.Loggable;

/**
 * This is designed to work as a plugin for ImageJ - this
 * means this launching class must be in the default package.
 * This also launches the program when run as standalone.
 * @author bms41
 *
 */
public class Nuclear_Morphology_Analysis
implements PlugIn, Loggable

{
	
	private static Nuclear_Morphology_Analysis instance; // for launching without ImageJ
	
	// Store which plugins have been found
	private HashMap<String, Boolean>  requiredFiles = new HashMap<String, Boolean>();
	
	
	// The plugins that are needed for the program to start
	private String[] fileNames = { "commons-math3",
			"commons-math3",
			"jcommon",
			"jdistlib",
			"jebl",
			"jfreechart",
			"swingx-all",
			"weka",
			"AnalyzeSkeleton",
			"Gray_Morphology"
	};
	
	public static void main(String[] args){
		instance = new Nuclear_Morphology_Analysis();
		instance.runStandalone();
	}
	
	/**
	 * Reset all found files to false
	 */
	private void clearFileList(){
		for(String s : fileNames){
			requiredFiles.put(s, false);
		}
	}
	
	/* 
     * The first method run when the plugin starts.
	 */
	public void run(String paramString){
		/*
		 * Add a splash screen for long load times
		 */
		final JWindow splash = createSplash();
		
		try {
			load();
		} catch(Exception e){
			
		} finally {
			splash.dispose();
		}
	}
	
	/**
	 * Load the program as standalone
	 */
	private void runStandalone(){
				
		try {
			loadMainWindow(true);
		} catch(Exception e){
			System.err.println("Error loading main window");
			e.printStackTrace();
		} 
		
			
		
	}
	
	private void loadMainWindow(boolean standalone){
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {

				IJ.setBackgroundColor(0, 0, 0);	 // default background is black

				try {
					UIManager.setLookAndFeel(
							UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {

					logToImageJ("Error initialising", e);
				}

				MainWindow frame = new MainWindow(standalone);
				frame.setVisible(true);
			}
		});
	}
	
	/*
	 * Check all dependencies are present, the
	 * Java version is correct and load the main window
	 */
	public void load()  {

		try {
			
			if( ! IJ.isJava18() ){
				IJ.log("Unable to open Nuclear Morphology Analysis");
				IJ.log("Java version 8 is required");
				IJ.log("Update ImageJ to a version bundled with Java 8");
				IJ.log("This is the version called 'bundled with Java 1.8.0_xx' here:");
				IJ.log("http://rsb.info.nih.gov/ij/download.html");
				return;
			}

			if(checkPlugins()){ 

				loadMainWindow(false);

			} else {

				displayMissingPlugins();
				IJ.log("Unable to launch the Nuclear Morphology Analysis plugin for ImageJ");
				IJ.log("This is because a required plugin is missing");
				IJ.log("The names of the missing plugins are listed above");
				IJ.log("Visit the project wiki for links to download missing plugins:");
				IJ.log("https://bitbucket.org/bmskinner/nuclear_morphology/wiki/Installation");

			}


		} catch (Exception e) {
			logToImageJ("Error initialising", e);
		} 
		
	}
	
	/**
	 * Get the URL for the splash screen gif
	 * @return
	 */
	private URL getSplashURL(){
		
		String path = "res/splash.gif";
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
	private void checkDir(File dir){
				
		if(dir==null){
			return;
		}
		
		if(allPluginsFound()){ // Don't waste time if they have all been found so far
			return;
		}
		
		if( ! dir.exists()){
			return;
		}
		if( ! dir.isDirectory()){
			return;
		}
		
		List<String> toCheck = Arrays.stream(fileNames)
			.filter( s -> requiredFiles.get(s)==false )
			.collect(Collectors.toList());
				

		for(File file : dir.listFiles()){
			
			if(file.isDirectory()){
				continue;
			}
				
			for(String s : toCheck){
				
				if(file.getName().startsWith(s)){
					requiredFiles.put(s, true);
				}
				
			}
		}
	}
	
	/**
	 * Check if all the plugins needed have been found yet
	 * @return
	 */
	private boolean allPluginsFound(){
		long count = Arrays.stream(fileNames)
				.filter( s -> requiredFiles.get(s)==false )
				.count();
		
		return count == 0;
			
	}
	
	private void displayMissingPlugins(){
		// report missing jars
		for(String s : requiredFiles.keySet()){
			if(requiredFiles.get(s)==false){
				IJ.log("Cannot find a required plugin: "+s);
			}
		}
	}
	
	/**
	 * Look in the likely plugins folders for the required plugins.
	 * @return
	 */
	private boolean checkPlugins(){
		
		clearFileList(); // set all files to false
		
		String pluginDirName = IJ.getDirectory("plugins");
		
		File pluginDir = new File(pluginDirName);
		File oldJarDir = new File(pluginDirName+File.separator+"jars");
		File jarDir    = new File(pluginDirName+File.separator+"Nuclear_Morphology_Analysis");
		

		// check the plugins directory directly
		checkDir(pluginDir);
		
		
		/*
		 * Check the new jar dir for jars
		 */
		checkDir(jarDir);
		
		/*
		 * Check the old folder for jars (optional storage in 1.12.0 and earlier)
		 */
		checkDir(oldJarDir);
		
				
		return allPluginsFound();
	}
}

