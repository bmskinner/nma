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

package com.bmskinner.nuclear_morphology.analysis.signals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.AbstractFinder;
import com.bmskinner.nuclear_morphology.components.ComponentFactory;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalFactory;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

/**
 * Implementation of the Finder interface for detecting nuclear signals
 * @author ben
 * @since 1.13.5
 *
 */
public class SignalFinder extends AbstractFinder<List<INuclearSignal>> {
	
	final private ComponentFactory<INuclearSignal> factory = new SignalFactory();
	final private INuclearSignalOptions signalOptions;
	final private int channel;
	
	public SignalFinder(IAnalysisOptions op, INuclearSignalOptions signalOptions, int channel) {
		super(op);
		this.signalOptions = signalOptions;
		this.channel = channel;
	}

	@Override
	public List<INuclearSignal> findInFolder(File folder) throws ImageImportException, ComponentCreationException {
		List<INuclearSignal> list = new ArrayList<>();
		
		List<File> files = Arrays.asList(folder.listFiles());

		files.parallelStream().forEach( f -> {
			if( ! f.isDirectory()){
				
				if(ImageImporter.fileIsImportable(f)){
					try {
						list.addAll(findInImage(f));
					} catch (ImageImportException | ComponentCreationException e) {
						stack("Error searching image", e);
					}
				}
			}
		});

		return list;
	}

	@Override
	public List<INuclearSignal> findInImage(File imageFile) throws ImageImportException, ComponentCreationException {
		
		SignalDetector detector = new SignalDetector(signalOptions, channel);
		
//		return detector.detectSignal(sourceFile, stack, n);
		return null;

	}




	
	

}
