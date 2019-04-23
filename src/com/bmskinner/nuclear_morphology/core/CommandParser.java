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

import javax.swing.UIManager;

import com.bmskinner.nuclear_morphology.api.BasicAnalysisPipeline;
import com.bmskinner.nuclear_morphology.api.SavedOptionsAnalysisPipeline;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.gui.DefaultInputSupplier;
import com.bmskinner.nuclear_morphology.gui.main.DockableMainWindow;
import com.bmskinner.nuclear_morphology.gui.main.MainWindow;
import com.bmskinner.nuclear_morphology.io.ConfigFileReader;
import com.bmskinner.nuclear_morphology.io.UpdateChecker;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.IJ;

/**
 * Handle arguments passed to the program via the command line.
 * @author bms41
 * @since 1.13.7
 *
 */
public class CommandParser implements Loggable {
    	
	/**
	 * Construct with an array of parameters for the program 
	 * to interpret
	 * @param arr
	 */
	public CommandParser(String[] arr){
	    execute(arr);	
	}
	
	
	private void execute(String[] arr){
	    
	    boolean headless = false;
	    File folder = null; 
	    File options = null;
	    for(String s : arr){
	    	System.out.println("Argument: "+s);
	    	
	    	if(s.startsWith("-h")) {
	    		System.out.println("Arguments:");
	    		System.out.println("\t-folder <image_folder>");
	    		System.out.println("\t-options <xml_options>");
	    		System.exit(0);
	    	}
	    	
	    	if(s.startsWith("-folder=")) {
	    		headless=true;
	    		String path = s.replace("-folder=", "");
	    		folder = new File(path); 
	    	}
	    	
	    	if(s.startsWith("-options=")) {
	    		headless=true;
	    		String path = s.replace("-options=", "");
	    		options = new File(path); 
	    	}
	        
	    }
	    // load the config file
	    new ConfigFileReader();

	    if(headless){
	    	runHeadless(folder, options);
	    } else {
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
    		log("Running on folder: "+folder.getAbsolutePath());
    		
    		if(!folder.isDirectory()) {
    			warn("A directory is required in the '-folder' argument");
    			return;
    		}
    		try {
    			if(options!=null) {
    				log("Running with saved options: "+options.getAbsolutePath());
    				new SavedOptionsAnalysisPipeline(folder, options).call();
    			} else {
    				log("No analysis options provided, using defaults");
    				new BasicAnalysisPipeline(folder);
    			}

    		} catch (Exception e) {
    			error("Error in pipeline", e);
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
                    UIManager.setLookAndFeel(
                            UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                	error("Error setting UI look and feel", e);
                }
                
                boolean useStandalone = true;
                
                InputSupplier is = new DefaultInputSupplier();
                EventHandler eh = new EventHandler(is);

                boolean useDockable = GlobalOptions.getInstance().isUseDockableInterface();
                
                finest("Dockable: "+useDockable);
                
                if(useDockable) {
                	DockableMainWindow mw = new DockableMainWindow(useStandalone,eh);
                    mw.setVisible(true);
                } else {
                	MainWindow mw = new MainWindow(useStandalone, eh);
                    mw.setVisible(true);
                }
                
                // Check quietly for updates
                Runnable r1 = () -> {
    				Version v = UpdateChecker.fetchLatestVersion();
    				fine("Latest online version is "+v.toString());
    				if(v.isNewerThan(Version.currentVersion())) {
    					log("A new version - "+v+" - is available");
    					log("Get it at https://bitbucket.org/bmskinner/nuclear_morphology/downloads/");
    				}
    			};
    			ThreadManager.getInstance().submit(r1);
        };
        java.awt.EventQueue.invokeLater( r );
        } catch(Exception e){
            System.err.println("Error loading main window");
            e.printStackTrace();
        } 
        
            
        
    }
}
