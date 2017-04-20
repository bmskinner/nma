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
import java.util.List;

import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

/**
 * An abstract implementation of the {@link Finder} interface.
 * @author bms41
 * @since 1.13.5
 *
 */
public abstract class AbstractFinder implements Finder, Loggable {

	final protected IAnalysisOptions options;
	final protected List<Object> listeners = new ArrayList<>();
	
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
	public List<ICell> find() throws Exception{
		
		List<ICell> list = findInFolder(options.getDetectionOptions(IAnalysisOptions.CYTOPLASM).getFolder());
		return list;
		
	}
	
	@Override
	public List<ICell> findInFolder(File folder) throws ImageImportException, ComponentCreationException{
		List<ICell> list = new ArrayList<>();
		
		for(File f : folder.listFiles()){
			if(! f.isDirectory()){
				list.addAll(findInImage(f));
			}
		}
		
		return list;
	}
	
	
	/*
	 * EVENT HANDLING
	 * 
	 */
	@Override
	public void addDetectionEventListener(DetectionEventListener l){
		listeners.add(l);
	}
	
	@Override
	public void removeDetectionEventListener(DetectionEventListener l){
		listeners.remove(l);
	}
	
	@Override
	public void fireDetectionEvent(ImageProcessor ip, String message){
		for(Object l : listeners){
			((DetectionEventListener)l).detectionEventReceived(new DetectionEvent(this, ip, message));
		}

	}
	
	@Override
	public void removeAllDetectionEventListeners(){
		listeners.clear();
	}

}
