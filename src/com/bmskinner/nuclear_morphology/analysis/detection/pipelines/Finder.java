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

package com.bmskinner.nuclear_morphology.analysis.detection.pipelines;

import java.io.File;
import java.util.EventObject;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

import ij.process.ImageProcessor;

/**
 * Interface for all detection piplines. Replacing the DetectionPipeline classes.
 * @author bms41
 * @since 1.13.5
 *
 */
public interface Finder {

	/**
	 * Find cells using the options given in the setup
	 * @return the cells detected in the folder of images
	 * @throws Exception if detection fails
	 */
	public List<ICell> find() throws Exception;
	
	/**
	 * Find cells using the options given in the setup in the given folder
	 * @param folder
	 * @return
	 * @throws ImageImportException
	 * @throws ComponentCreationException
	 */
	public List<ICell> findInFolder(File folder) throws ImageImportException, ComponentCreationException;
	
	
	/**
	 * Find cells using the options given in the setup in the given image
	 * @param imageFile
	 * @return
	 * @throws ImageImportException
	 * @throws ComponentCreationException
	 */
	public List<ICell> findInImage(File imageFile) throws ImageImportException, ComponentCreationException;
	

	/**
	 * Add the given event listener to the finder
	 * @param l
	 */
	void addDetectionEventListener(DetectionEventListener l);
	
	/**
	 * Remove the event listener from the finder
	 * @param l
	 */
	void removeDetectionEventListener(DetectionEventListener l);
	
	/**
	 * Remove all event listeners from the finder
	 */
	void removeAllDetectionEventListeners();
	
	/**
	 * Fire a detection event with the given image and message
	 * @param ip
	 * @param message
	 */
	void fireDetectionEvent(ImageProcessor ip, String message);
	
	/**
	 * Interface implemented by probers to be notified that a new image is available
	 * for display
	 * @author ben
	 * @since 1.13.5
	 *
	 */
	interface DetectionEventListener {
		
		/**
		 * Respond to a detection event
		 * @param e
		 */
		void detectionEventReceived(DetectionEvent e);
	}
	
	/**
	 * Fired when an image has been processed to detect components.
	 * @author ben
	 * @since 1.13.5
	 *
	 */
	class DetectionEvent extends EventObject {
		
		final private ImageProcessor ip;
		final private String message;
		public DetectionEvent(final Object source, final ImageProcessor ip, final String message){
			super(source);
			this.ip = ip;
			this.message = message;
		}
		
		/**
		 * Get the image processor in this event
		 * @return
		 */
		public ImageProcessor getProcessor(){
			return ip;
		}
		
		/**
		 * Get the message in this event
		 * @return
		 */
		public String getMessage(){
			return message;
		}
		
	}
	
}
