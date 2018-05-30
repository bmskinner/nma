package com.bmskinner.nuclear_morphology.main;

import javax.swing.UIManager;

import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.io.PropertiesReader;

import ij.IJ;

/**
 * Handle arguments passed to the program via the command line.
 * @author bms41
 * @since 1.13.7
 *
 */
public class CommandParser {
    	
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
	    for(String s : arr){
//	        System.out.println(s);
	        
	        if(s.equals("-headless")){
	            headless = true;
	        }
	    }
	    
	    
	    if(headless){
	        launch();
	    } else {
	        runStandalone();
	    }
	    		
	}
	
	public void launch(){
	    System.out.println("Launching headless...");
	    System.out.println("No further functionality enabled");
	}
	
	
	/**
     * Load the program as standalone
     */
    private void runStandalone(){
        try {
            // load the config file properties
            new PropertiesReader();
            loadMainWindow(true);
        } catch(Exception e){
            System.err.println("Error loading main window");
            e.printStackTrace();
        } 
        
            
        
    }
    
    private void loadMainWindow(boolean standalone){
        
        Runnable r = () -> {

                IJ.setBackgroundColor(0, 0, 0);  // default background is black
                try {
                    UIManager.setLookAndFeel(
                            UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                	System.err.println("Error setting UI look and feel");
                    e.printStackTrace();
                }

                MainWindow mw = new MainWindow(standalone, new EventHandler());
                mw.setVisible(true);
        };
        java.awt.EventQueue.invokeLater( r );
    }

}
