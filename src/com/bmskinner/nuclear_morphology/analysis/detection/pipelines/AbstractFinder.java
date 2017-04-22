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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import com.bmskinner.nuclear_morphology.analysis.ProgressEvent;
import com.bmskinner.nuclear_morphology.analysis.ProgressListener;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

/**
 * An abstract implementation of the {@link Finder} interface.
 * @author bms41
 * @param <E> the element to find
 * @since 1.13.5
 *
 */
public abstract class AbstractFinder<E> implements Finder<E>, Loggable {

	volatile protected IAnalysisOptions options;
	volatile protected List<DetectionEventListener> detectionlisteners = new ArrayList<>();
	volatile protected List<ProgressListener>        progressListeners = new ArrayList<>();
	
	/**
	 * The minimum size of an object to detect for {@link Profileable} objects
	 */
	public static final int MIN_PROFILABLE_OBJECT_SIZE = 50;
	
	/**
	 * Construct with an analysis options
	 * @param op the analysis options
	 */
	public AbstractFinder(IAnalysisOptions op){
		options = op;
	}
	
	/*
	 * METHODS IMPLEMENTING THE FINDER INTERFACE
	 * 
	 */
	
	@Override
	public E find() throws Exception{
		
		E list = findInFolder(options.getDetectionOptions(IAnalysisOptions.CYTOPLASM).getFolder());
		return list;
		
	}	
	
	/*
	 * EVENT HANDLING
	 * 
	 */
	@Override
	public void addDetectionEventListener(DetectionEventListener l){
		detectionlisteners.add(l);
	}
	
	@Override
	public void removeDetectionEventListener(DetectionEventListener l){
		detectionlisteners.remove(l);
	}
	
	@Override
	public void fireDetectionEvent(ImageProcessor ip, String message){
		for(DetectionEventListener l : detectionlisteners){
			l.detectionEventReceived(new DetectionEvent(this, ip, message));
		}

	}
	
	@Override
	public void removeAllDetectionEventListeners(){
		detectionlisteners.clear();
	}
	
	/**
	 * Are there registered listenters for detection events? These
	 * allow display of images from within the pipeline.
	 * @return
	 */
	protected boolean hasDetectionListeners(){
		return !detectionlisteners.isEmpty();
	}
	
	/*
	 * PROGRESS HANDLING
	 * 
	 */
	
	public synchronized void addProgressListener( ProgressListener l ) {
		progressListeners.add( l );
	}

	public synchronized void removeProgressListener( ProgressListener l ) {
		progressListeners.remove( l );
	}
	
	protected boolean hasProgressListeners(){
		return !progressListeners.isEmpty();
	}

	/**
	 * Signal that a stage in an analysis has completed.
	 */
	protected synchronized void fireProgressEvent() {

		ProgressEvent event = new ProgressEvent( this);
		Iterator<ProgressListener> iterator = progressListeners.iterator();
		while( iterator.hasNext() ) {
			
			iterator.next().progressEventReceived( event );
		}
	}

}
