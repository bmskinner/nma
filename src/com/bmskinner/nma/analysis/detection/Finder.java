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
package com.bmskinner.nma.analysis.detection;

import java.io.File;
import java.util.Collection;
import java.util.EventObject;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.ProgressListener;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;

import ij.process.ImageProcessor;

/**
 * Interface for all detection piplines. Replacing the DetectionPipeline
 * classes.
 * 
 * @author bms41
 * @param <E> the type of element to find
 * @since 1.13.5
 *
 */
public interface Finder<E> {

	/**
	 * Find elements using the options given in the setup
	 * 
	 * @return the cells detected in the folder of images
	 * @throws Exception if detection fails
	 */
	Collection<E> find() throws Exception;

	/**
	 * Find cells using the options given in the setup in the given folder
	 * 
	 * @param folder
	 * @return
	 * @throws ImageImportException
	 * @throws ComponentCreationException
	 */
	Collection<E> findInFolder(@NonNull File folder) throws ImageImportException;

	/**
	 * Find cells using the options given in the setup in the given image
	 * 
	 * @param imageFile
	 * @return
	 * @throws ImageImportException
	 * @throws ComponentCreationException
	 */
	Collection<E> findInImage(@NonNull File imageFile) throws ImageImportException;

	/**
	 * Test if the given entity is valid for this finder based on the option used to
	 * create the finder.
	 * 
	 * @param entity
	 * @return
	 */
	boolean isValid(@NonNull E entity);

	/**
	 * Add a listener for progress through the detection.
	 * 
	 * @param l
	 */
	void addProgressListener(ProgressListener l);

	/**
	 * Remove the progress listener from the finder
	 * 
	 * @param l
	 */
	void removeProgressListener(ProgressListener l);

	/**
	 * Add the given event listener to the finder
	 * 
	 * @param l
	 */
	void addDetectionEventListener(DetectionEventListener l);

	/**
	 * Remove the event listener from the finder
	 * 
	 * @param l
	 */
	void removeDetectionEventListener(DetectionEventListener l);

	/**
	 * Remove all event listeners from the finder
	 */
	void removeAllDetectionEventListeners();

	/**
	 * Fire a detection event with the given image and message
	 * 
	 * @param ip
	 * @param message
	 */
	void fireDetectionEvent(ImageProcessor ip, String message);

	/**
	 * Add the given event listener to the finder
	 * 
	 * @param l
	 */
	void addDetectedObjectEventListener(DetectedObjectListener<E> l);

	/**
	 * Add the given event listener to the finder
	 * 
	 * @param l
	 */
	void removeDetectedObjectEventListener(DetectedObjectListener<E> l);

	/**
	 * Remove all event listeners from the finder
	 */
	void removeAllDetectedObjectEventListeners();

	/**
	 * Fire a detection event with the given objects and message
	 * 
	 * @param valid   objects that pass filtering
	 * @param invalid objects that did not pass filtering
	 * @param message any message
	 */
	void fireDetectedObjectEvent(Collection<E> valid, Collection<E> invalid, String message);

	/**
	 * Interface implemented by probers to be notified that a new image is available
	 * for display
	 * 
	 * @author ben
	 * @since 1.13.5
	 *
	 */
	interface DetectionEventListener {

		/**
		 * Respond to a detection event
		 * 
		 * @param e
		 */
		void detectionEventReceived(DetectionEvent e);
	}

	/**
	 * Fired when an image has been processed to detect components.
	 * 
	 * @author ben
	 * @since 1.13.5
	 *
	 */
	@SuppressWarnings("serial")
	class DetectionEvent extends EventObject {

		private final ImageProcessor ip;
		private final String message;

		public DetectionEvent(final Object source, final ImageProcessor ip, final String message) {
			super(source);
			this.ip = ip;
			this.message = message;
		}

		/**
		 * Get the image processor in this event
		 * 
		 * @return
		 */
		public ImageProcessor getProcessor() {
			return ip;
		}

		/**
		 * Get the message in this event
		 * 
		 * @return
		 */
		public String getMessage() {
			return message;
		}

	}

	interface DetectedObjectListener<E> {
		/**
		 * Respond to a detection event
		 * 
		 * @param e
		 */
		void detectedObjectEventReceived(DetectedObjectEvent<E> e);
	}

	/**
	 * Fired when an image has been processed to detect components.
	 * 
	 * @author ben
	 * @since 1.13.5
	 *
	 */
	@SuppressWarnings("serial")
	class DetectedObjectEvent<E> extends EventObject {

		private final transient Collection<E> valid;
		private final transient Collection<E> invalid;
		private final String message;

		public DetectedObjectEvent(final Object source, final Collection<E> valid,
				final Collection<E> invalid,
				final String message) {
			super(source);
			this.valid = valid;
			this.invalid = invalid;
			this.message = message;
		}

		/**
		 * Get the valid objects detected in this event
		 * 
		 * @return
		 */
		public Collection<E> getValidObjects() {
			return valid;
		}

		/**
		 * Get the invalid objects detected in this event
		 * 
		 * @return
		 */
		public Collection<E> getInvalidObjects() {
			return invalid;
		}

		/**
		 * Get the message in this event
		 * 
		 * @return
		 */
		public String getMessage() {
			return message;
		}

	}

}
