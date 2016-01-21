/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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
package gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.MediaTracker;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;

/**
 * This is an abstract class containing a JLabel with an animated gif for a loading
 * icon. This is used in the ImageProbers and in the cluster trees
 * @author ben
 *
 */
@SuppressWarnings("serial")
public abstract class LoadingIconDialog extends JDialog {
	
	private static final String LOADING_GIF_NAME = "ajax-loader.gif";
	private static final String BLANK_GIF_NAME   = "blank.gif";

	private JLabel loadingLabel = new JLabel("");
	private ImageIcon loadingGif = null; // the icon for the loading gif
	private ImageIcon blankGif = null; // the icon for the blank gif
	protected Logger programLogger;
	
	public LoadingIconDialog(Logger programLogger){
		this.programLogger = programLogger;
		
		// Load the gif (may be in a res folder depending on Eclipse version)
//		String pathToGif   = "res/ajax-loader.gif";	
//		String pathToBlank = "res/blank.gif";	
		String path = "res/";
		boolean ok = loadResources(path);
		if(!ok){
			path = "";
//			path = "ajax-loader.gif";	
			ok = loadResources(path);
		}
		if(!ok){
			programLogger.log(Level.WARNING, "Resource loading failed (gif): "+path);
		}
		
		this.loadingLabel.setIcon(blankGif);

	}
	
	protected Logger getProgramLogger(){
		return this.programLogger;
	}
	
	/**
	 * Get the JLabel with the loading icon
	 * @return
	 */
	protected JLabel getLoadingLabel(){
		return this.loadingLabel;
	}
	
	/**
	 * Get the gif used for loading
	 * @return
	 */
	protected ImageIcon getLoadingGif(){
		return this.loadingGif;
	}
	
	/**
	 * Set the text for the label with the loading icon
	 * @param s
	 */
	protected void setLoadingLabelText(String s){
		this.loadingLabel.setText(s);
	}
	
	/**
	 * Fetch the gif loading resources
	 * 
	 */
	private boolean loadResources(String path){
		
		String pathToGif   = path+LOADING_GIF_NAME;	
		String pathToBlank = path+BLANK_GIF_NAME;
		
		boolean ok = false;
		try{
			
			// Get current classloader
			ClassLoader cl = this.getClass().getClassLoader();
			URL urlToGif = cl.getResource(pathToGif);
			
			if(urlToGif!=null){
//				loadingGif = new ImageIcon(urlToGif);
				loadingGif = loadURL(urlToGif);

				if(loadingGif==null){
					programLogger.log(Level.WARNING, "Unable to load gif");
					ok = false;
				} else {
					ok = true;
				}

			} 
			
			// Get current classloader
			URL urlToBlank = cl.getResource(pathToBlank);

			if(urlToBlank!=null){
				blankGif = loadURL(urlToBlank);
//				blankGif = new ImageIcon(urlToBlank);

				if(blankGif==null){
					programLogger.log(Level.WARNING, "Unable to load blank gif");
					ok = false;
				} else {
					ok = true;
				}

			} 
			
			
		} catch (Exception e){
			programLogger.log(Level.SEVERE, "Cannot load gif resource", e);
		}
		return ok;
	}
	
	private ImageIcon loadURL(URL url){
    	ImageIcon icon = null;
    	if(url!=null){
    		programLogger.log(Level.FINER, "URL found: "+url.toString());
    		icon = new ImageIcon(url);
    		
    		String status = "";
    		switch(icon.getImageLoadStatus()){
    		
    			case 1: status = "Loading";
    				break;
    			case 2: status = "Aborted";
    				break;
    			case 4: status = "Errored";
					break;
    			case 8: status = "Complete";
					break;
    		
    		}
    		
    		programLogger.log(Level.FINER, "Load status: "+status);
    		
//    		if(icon.getImageLoadStatus()== MediaTracker.ERRORED){
//    			programLogger.log(Level.WARNING, "Could not load icon from "+url.getPath());
//    			programLogger.log(Level.WARNING, "Icon status "+icon.getImageLoadStatus());
//
//    			
//    			
//    			programLogger.log(Level.WARNING, "Trying to load icon directly...");
//    			try{
//    				Image img = ImageIO.read(url);
//    				icon = new ImageIcon(img);
//    			} catch(IOException e){
//    				programLogger.log(Level.SEVERE, "Error in loading image ", e);
//    				icon = null;
//    			}
//    		}

    	}
    	return icon;
    }
	
	/**
	 * Set the header label to loading
	 * the loading gif
	 */
	protected void setStatusLoading(){
		if(loadingGif!=null){
			ImageIcon hicon = (ImageIcon) loadingLabel.getIcon();
			if(hicon!=null){
				hicon.getImage().flush();
			}
			loadingLabel.setIcon(loadingGif);
			loadingLabel.repaint();
		}

		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		for(Component c : this.getComponents()){
			c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		}
	}

	/**
	 * Set the header label to the blank gif
	 */
	protected void setStatusLoaded(){
		ImageIcon hicon = (ImageIcon) loadingLabel.getIcon();
		if(hicon!=null){
			hicon.getImage().flush();
		}
		loadingLabel.setIcon(blankGif);
		loadingLabel.repaint();

		this.setCursor(Cursor.getDefaultCursor());
		for(Component c : this.getComponents()){
			c.setCursor(Cursor.getDefaultCursor());
		}
	}
}
