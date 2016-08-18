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

package analysis.signals;

import gui.ImageType;
import gui.dialogs.FishRemappingDialog.FishMappingImageType;
import ij.ImageStack;
import ij.process.ImageProcessor;
import io.ImageExporter;
import io.ImageImporter;

import java.io.File;
import java.util.logging.Level;

import javax.swing.table.TableModel;

import analysis.detection.IconCell;
import analysis.detection.ImageProberWorker;

public class FishRemappingWorker extends ImageProberWorker {
	
	private File postFISHImageDirectory;

	public FishRemappingWorker(File f, 
			ImageType type, TableModel model, 
			File fishDirectory) {
		super(f, null, type, model);

		
		if( ! fishDirectory.isDirectory()){
			throw new IllegalArgumentException("Fish directory is not a folder");
		}
		this.postFISHImageDirectory = fishDirectory;
		
	}
	
	protected void analyseImages() throws Exception {

			ImageStack stack = ImageImporter.getInstance().importImage(file);

			// Import the image as a stack
			String imageName = file.getName();

			log(Level.FINEST, "Converting image");
			ImageProcessor openProcessor = ImageExporter.getInstance().makeGreyRGBImage(stack).getProcessor();
			openProcessor.invert();
			
			IconCell iconCell = makeIconCell(openProcessor, FishMappingImageType.ORIGINAL_IMAGE);
			publish(iconCell);
			

			File fishImageFile = new File(postFISHImageDirectory+File.separator+imageName);
			ImageStack fishStack = ImageImporter.getInstance().importImage(fishImageFile);

			ImageProcessor fishProcessor = ImageExporter.getInstance().convertToRGB(fishStack).getProcessor();

			

//			// Get the cells matching the imageFile
//			for(Cell c : dataset.getCollection().getCells(file)){
//				drawNucleus(c, openProcessor);
//			}

			IconCell iconCell2 = makeIconCell(fishProcessor, FishMappingImageType.FISH_IMAGE);
			publish(iconCell2);

	}
	


}
