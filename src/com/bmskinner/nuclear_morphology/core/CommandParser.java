package com.bmskinner.nuclear_morphology.core;

import java.io.File;

import javax.swing.UIManager;

import com.bmskinner.nuclear_morphology.api.BasicAnalysisPipeline;
import com.bmskinner.nuclear_morphology.gui.DefaultInputSupplier;
import com.bmskinner.nuclear_morphology.gui.main.DockableMainWindow;
import com.bmskinner.nuclear_morphology.gui.main.MainWindow;
import com.bmskinner.nuclear_morphology.io.PropertiesReader;
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
	    for(String s : arr){
	    	log("Argument: "+s);
	    	if(s.startsWith("-folder=")) {
	    		headless=true;
	    		String path = s.replace("-folder=", "");
	    		folder = new File(path); 
	    	}
	        
	    }
	    // load the config file
	    new PropertiesReader();
	    
	    if(headless){
	    	log("Running on folder: "+folder.getAbsolutePath());
	    	try {
				new BasicAnalysisPipeline(folder);
			} catch (Exception e) {
				error("Error in pipeline", e);
			}
	    } else {
	        runWithGUI();
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
                	System.err.println("Error setting UI look and feel");
                    e.printStackTrace();
                }

                if(GlobalOptions.getInstance().isUseDockableInterface()) {
                	DockableMainWindow mw = new DockableMainWindow(true, new EventHandler(new DefaultInputSupplier()));
                    mw.setVisible(true);
                } else {
                	MainWindow mw = new MainWindow(true, new EventHandler(new DefaultInputSupplier()));
                    mw.setVisible(true);
                }
                
                
        };
        java.awt.EventQueue.invokeLater( r );
        } catch(Exception e){
            System.err.println("Error loading main window");
            e.printStackTrace();
        } 
        
            
        
    }
}
