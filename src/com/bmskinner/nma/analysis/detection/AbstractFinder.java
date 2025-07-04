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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.ProgressEvent;
import com.bmskinner.nma.analysis.ProgressListener;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.MissingOptionException;

import ij.process.ImageProcessor;

/**
 * An abstract implementation of the {@link Finder} interface.
 * 
 * @author bms41
 * @param <E> the element to find
 * @since 1.13.5
 *
 */
public abstract class AbstractFinder<E> implements Finder<E> {

	protected IAnalysisOptions options;
	protected List<DetectionEventListener> detectionlisteners = new ArrayList<>();
	protected List<DetectedObjectListener<E>> objectlisteners = new ArrayList<>();
	protected List<ProgressListener> progressListeners = new ArrayList<>();

	/**
	 * The minimum size of an object to detect for {@link Profileable} objects
	 */
	public static final int MIN_PROFILABLE_OBJECT_SIZE = 50;

	/**
	 * Construct with an analysis options
	 * 
	 * @param op the analysis options
	 */
	protected AbstractFinder(@NonNull final IAnalysisOptions op) {
		options = op;
	}

	/*
	 * METHODS IMPLEMENTING THE FINDER INTERFACE
	 * 
	 */

	@Override
	public Collection<E> find() throws Exception {

		Optional<HashOptions> op = options.getDetectionOptions(CellularComponent.CYTOPLASM);
		if (!op.isPresent())
			throw new MissingOptionException("No cytoplasm options");

		return findInFolder(new File(op.get().getString(HashOptions.DETECTION_FOLDER)));
	}

	/*
	 * EVENT HANDLING
	 * 
	 */
	@Override
	public void addDetectionEventListener(DetectionEventListener l) {
		detectionlisteners.add(l);
	}

	@Override
	public void removeDetectionEventListener(DetectionEventListener l) {
		detectionlisteners.remove(l);
	}

	@Override
	public void fireDetectionEvent(ImageProcessor ip, String message) {
		for (DetectionEventListener l : detectionlisteners) {
			l.detectionEventReceived(new DetectionEvent(this, ip, message));
		}
	}

	@Override
	public void removeAllDetectionEventListeners() {
		detectionlisteners.clear();
	}

	/**
	 * Are there registered listenters for detection events? These allow display of
	 * images from within the pipeline.
	 * 
	 * @return
	 */
	protected boolean hasDetectionListeners() {
		return !detectionlisteners.isEmpty();
	}

	// Detected objects handling

	@Override
	public void addDetectedObjectEventListener(DetectedObjectListener<E> l) {
		objectlisteners.add(l);
	}

	@Override
	public void removeDetectedObjectEventListener(DetectedObjectListener<E> l) {
		objectlisteners.remove(l);
	}

	@Override
	public void removeAllDetectedObjectEventListeners() {
		objectlisteners.clear();
	}

	@Override
	public void fireDetectedObjectEvent(Collection<E> valid, Collection<E> invalid,
			String message) {
		for (DetectedObjectListener<E> l : objectlisteners) {
			l.detectedObjectEventReceived(new DetectedObjectEvent<>(this, valid, invalid, message));
		}
	}

	/*
	 * PROGRESS HANDLING
	 * 
	 */

	@Override
	public synchronized void addProgressListener(ProgressListener l) {
		progressListeners.add(l);
	}

	@Override
	public synchronized void removeProgressListener(ProgressListener l) {
		progressListeners.remove(l);
	}

	protected boolean hasProgressListeners() {
		return !progressListeners.isEmpty();
	}

	/**
	 * Signal that a stage in an analysis has completed.
	 */
	protected synchronized void fireProgressEvent() {

		ProgressEvent event = new ProgressEvent(this);
		Iterator<ProgressListener> iterator = progressListeners.iterator();
		while (iterator.hasNext())
			iterator.next().progressEventReceived(event);
	}

}
