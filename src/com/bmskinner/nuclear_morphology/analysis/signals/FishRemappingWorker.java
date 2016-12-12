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

import ij.ImageStack;
import ij.process.ImageProcessor;

import java.io.File;

import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.detection.IconCell;
import com.bmskinner.nuclear_morphology.analysis.detection.ImageProberWorker;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.gui.ImageType;
import com.bmskinner.nuclear_morphology.gui.dialogs.FishRemappingDialog.FishMappingImageType;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

public class FishRemappingWorker extends ImageProberWorker {
	
	private File postFISHImageDirectory;

	public FishRemappingWorker(File f, 
			ImageType type, TableModel model, 
			File fishDirectory) {
		super(f, null, type, model);

		
		if( ! fishDirectory.isDirectory()){
			throw new IllegalArgumentException("FISH directory is not a folder");
		}
		this.postFISHImageDirectory = fishDirectory;
		
	}
	
	protected void analyseImages() {

			ImageStack stack;
			try {
				stack = new ImageImporter(file).importImage();
			} catch (ImageImportException e) {
				error("Error importing file "+file.getAbsolutePath(), e);
				return;
			}

			// Import the image as a stack
			String imageName = file.getName();

			finest("Converting image");
			ImageProcessor openProcessor = new ImageConverter(stack).convertToGreyscale().toProcessor();
			openProcessor.invert();
			
			IconCell iconCell = makeIconCell(openProcessor, FishMappingImageType.ORIGINAL_IMAGE);
			publish(iconCell);
			

			File fishImageFile = new File(postFISHImageDirectory+File.separator+imageName);
			
			if( ! fishImageFile.exists()){
				warn("File does not exist: "+fishImageFile.getAbsolutePath());
				
			} else {
			
				ImageStack fishStack;
				try {
					fishStack = new ImageImporter(fishImageFile).importImage();
				} catch (ImageImportException e) {
					error("Error importing FISH image file "+fishImageFile.getAbsolutePath(), e);
					return;
				}

				ImageProcessor fishProcessor = new ImageConverter(fishStack).convertToRGB().toProcessor();

				IconCell iconCell2 = makeIconCell(fishProcessor, FishMappingImageType.FISH_IMAGE);
				publish(iconCell2);

			}
	}
	


}
