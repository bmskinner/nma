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

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.DetectionImageType;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageProberTableCell;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

import ij.ImageStack;
import ij.process.ImageProcessor;

/**
 * Test class for replacing the FISH remapper with a Finder.
 * Does not return useful cells
 * @author bms41
 * @since 1.13.5
 *
 */
public class FishRemappingFinder extends VoidFinder {
	
	private static final String FISH_FOLDER_IS_FILE_ERROR = "FISH directory is not a folder";
	private final File dir;
	
	public FishRemappingFinder(IAnalysisOptions op, final File postFishDir) {
		super(op);
		if( ! postFishDir.isDirectory()){
			throw new IllegalArgumentException(FISH_FOLDER_IS_FILE_ERROR);
		}
		
		this.dir = postFishDir;
	}

	@Override
	public Void findInImage(File imageFile) throws ImageImportException, ComponentCreationException {
		
		ImageStack stack;
		try {
			stack = new ImageImporter(imageFile).importToStack();
		} catch (ImageImportException e) {
			error("Error importing file "+imageFile.getAbsolutePath(), e);
			return null ;
		}

		// Import the image as a stack
		String imageName = imageFile.getName();

		finest("Converting image");
		ImageProcessor openProcessor = new ImageConverter(stack)
				.convertToGreyscale()
				.invert()
				.toProcessor();
		
		fireDetectionEvent(openProcessor.duplicate(), "Original image");			

		File fishImageFile = new File(dir, imageName);
		
		if( ! fishImageFile.exists()){
			warn("File does not exist: "+fishImageFile.getAbsolutePath());
			ImageProcessor ep = ImageConverter.createBlankImage(openProcessor.getWidth(), openProcessor.getHeight());
			
			ImageAnnotator an = new ImageAnnotator(ep)
					.annotateString(ep.getWidth()/2, ep.getHeight()/2, "File not found");
			
			fireDetectionEvent(an.toProcessor().duplicate(), "FISH image");			
			return null;
		}
			
		ImageStack fishStack;
		try {
			fishStack = new ImageImporter(fishImageFile).importToStack();
		} catch (ImageImportException e) {
			error("Error importing FISH image file "+fishImageFile.getAbsolutePath(), e);
			return null;
		}

		ImageProcessor fp = new ImageConverter(fishStack).convertToRGB().toProcessor();
		fireDetectionEvent(fp.duplicate(), "FISH image");	
		
		fireProgressEvent();
		return null;
		
	}

}
