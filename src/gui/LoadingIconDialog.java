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

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

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
		
		String pathToGif   = path+"ajax-loader.gif";	
		String pathToBlank = path+"blank.gif";
		
		boolean ok = false;
		try{
			
			// Get current classloader
			ClassLoader cl = this.getClass().getClassLoader();
			URL urlToGif = cl.getResource(pathToGif);
			
			if(urlToGif!=null){
				loadingGif = new ImageIcon(urlToGif);

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
				blankGif = new ImageIcon(urlToBlank);

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
	}
}
