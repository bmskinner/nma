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

import java.awt.EventQueue;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;

import com.bmskinner.nuclear_morphology.api.BasicAnalysisPipeline;
import com.bmskinner.nuclear_morphology.api.SavedOptionsAnalysisPipeline;
import com.bmskinner.nuclear_morphology.gui.DefaultInputSupplier;
import com.bmskinner.nuclear_morphology.gui.main.DockableMainWindow;
import com.bmskinner.nuclear_morphology.io.ConfigFileReader;

import ij.IJ;
import ij.Prefs;

/**
 * Handle arguments passed to the program via the command line.
 * @author bms41
 * @since 1.13.7
 *
 */
public class CommandLineParser {

	private static final Logger LOGGER = Logger.getLogger(CommandLineParser.class.getName());

	/**
	 * Construct with an array of parameters for the program 
	 * to interpret
	 * @param arr
	 */
	public CommandLineParser(String[] arr){
		execute(arr);	
	}


	/**
	 * Execute the commands provided
	 * @param arr
	 */
	private void execute(String[] arr){

		boolean headless = false;
		File folder = null; 
		File options = null;
		for(String s : arr){
			System.out.println("Argument: "+s);

			if(s.startsWith("-h")) {
				System.out.println("Arguments:");
				System.out.println("\t-folder=<image_folder>");
				System.out.println("\t-options=<xml_options>");
				System.exit(0);
			}

			if(s.startsWith("-folder=")) {
				headless=true;
				String path = s.replace("-folder=", "").replace("\"", "");
				folder = new File(path); 
			}

			if(s.startsWith("-options=")) {
				headless=true;
				String path = s.replace("-options=", "").replace("\"", "");
				options = new File(path); 
			}

		}
		// load the config file
		new ConfigFileReader();
		Prefs.setThreads(GlobalOptions.getInstance().getInt(GlobalOptions.NUM_IMAGEJ_THREADS_KEY));

		if(headless){
			LOGGER.fine("Running headless");
			runHeadless(folder, options);
		} else {
			LOGGER.fine("Launching GUI");
			runWithGUI();
		}

	}

	/**
	 * Run in headless mode, specifying a folder of images, and 
	 * a file of options
	 * @param folder the folder of images
	 * @param options
	 */
	private void runHeadless(final File folder, final File options) {
		if(folder!=null) {
			LOGGER.info("Running on folder: "+folder.getAbsolutePath());

			if(!folder.isDirectory()) {
				LOGGER.warning("A directory is required in the '-folder' argument");
				return;
			}
			try {
				if(options!=null) {
					LOGGER.info("Running with saved options: "+options.getAbsolutePath());
					new SavedOptionsAnalysisPipeline(folder, options).call();
				} else {
					LOGGER.info("No analysis options provided, using defaults");
					new BasicAnalysisPipeline(folder);
				}

			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error in pipeline", e);
			}
		}
	}

	/**
	 * Load the program user interface
	 */
	private void runWithGUI(){
		try {
			Runnable r = () -> {

				IJ.setBackgroundColor(0, 0, 0);  // default background is black
				try {
					String lAndF = UIManager.getLookAndFeel().getName();
					LOGGER.config("Setting look and feel to "+lAndF);
					UIManager.setLookAndFeel(lAndF);
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Unable to set look and feel", e);
				}

				boolean useStandalone = true;

				InputSupplier is = new DefaultInputSupplier();
				EventHandler eh = new EventHandler(is);

				DockableMainWindow mw = new DockableMainWindow(useStandalone,eh);
				mw.setVisible(true);
			};
			
			EventQueue.invokeLater( r );
		} catch(Exception e){
			LOGGER.log(Level.SEVERE, "Error loading main window", e);
		} 
	}
}
