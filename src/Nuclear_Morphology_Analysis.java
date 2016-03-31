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

import gui.MainWindow;
import ij.IJ;
import ij.plugin.PlugIn;

import java.io.File;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import logging.Loggable;

public class Nuclear_Morphology_Analysis
implements PlugIn, Loggable

{
	
	String[] requiredFiles = {
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
	
	
	/* 
    The first method to be run when the plugin starts.
	 */
	public void run(String paramString)  {

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

				java.awt.EventQueue.invokeLater(new Runnable() {
					public void run() {

						IJ.setBackgroundColor(0, 0, 0);	 // default background is black

						try {
							UIManager.setLookAndFeel(
									UIManager.getSystemLookAndFeelClassName());
						} catch (Exception e) {

							logToImageJ("Error initialising", e);
						}

						MainWindow frame = new MainWindow();
						frame.setVisible(true);
					}
				});

			}
		} catch (Exception e) {
			logToImageJ("Error initialising", e);
		}
	}
	
	private boolean checkPlugins(){
		boolean result = true;
		String pluginDirName = IJ.getDirectory("plugins");
		File pluginDir = new File(pluginDirName);
		
		File oldJarDir = new File(pluginDirName+File.separator+"jars");
		File jarDir = new File(pluginDirName+File.separator+"Nuclear_Morphology_Analysis");
		
		boolean[] oklist = new boolean[requiredFiles.length];
		for(boolean ok : oklist){
			ok = false;
		}

		// check the plugins directory and the plugins/jars directory
		for(int i=0; i<requiredFiles.length; i++){
			String fileName = requiredFiles[i];
			for(File file : pluginDir.listFiles()){
				
				if(file.getName().startsWith(fileName)){
					oklist[i] = true;
				}
				
			}
			
			for(File file : jarDir.listFiles()){
				
				if(file.getName().startsWith(fileName)){
					oklist[i] = true;
				}
				
			}
			
			/*
			 * Check the old folder for jars
			 */
			for(File file : oldJarDir.listFiles()){
				
				if(file.getName().startsWith(fileName)){
					oklist[i] = true;
				}
				
			}
			
		}
		
		// report missing jars
		for(int i=0; i<requiredFiles.length; i++){
			if(oklist[i]==false){
				IJ.log("Cannot find a required plugin: "+requiredFiles[i]);
				result = false;
			}
		}
		
		if(result==false){
			IJ.log("Unable to launch the Nuclear Morphology Analysis plugin for ImageJ");
			IJ.log("This is because a required plugin is missing");
			IJ.log("The names of the missing plugins are listed above");
			IJ.log("Visit the project wiki for links to download missing plugins:");
			IJ.log("https://bitbucket.org/bmskinner/nuclear_morphology/wiki/Installation");
			
		}
		
		return result;
	}
}

