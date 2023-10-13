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
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;
import com.bmskinner.nma.visualisation.image.ImageAnnotator;
import com.bmskinner.nma.visualisation.image.ImageConverter;

import ij.process.ImageProcessor;

/**
 * Test class for replacing the FISH remapper with a Finder. Does not return
 * useful cells
 * 
 * @author bms41
 * @since 1.13.5
 *
 */
public class FishRemappingFinder extends VoidFinder {

	private static final Logger LOGGER = Logger.getLogger(FishRemappingFinder.class.getName());

	private static final String FISH_FOLDER_IS_FILE_ERROR = "FISH directory is not a folder";
	private final File dir;

	public FishRemappingFinder(IAnalysisOptions op, final File postFishDir) {
		super(op);
		if (!postFishDir.isDirectory()) {
			throw new IllegalArgumentException(FISH_FOLDER_IS_FILE_ERROR);
		}

		this.dir = postFishDir;
	}

	@Override
	public Collection<Void> findInImage(@NonNull File imageFile) throws ImageImportException {

		String imageName = imageFile.getName();

		ImageProcessor openProcessor = new ImageConverter(
				ImageImporter.importFileTo24bit(imageFile))
						.convertToRGBGreyscale()
						.invert()
						.convertToColorProcessor()
						.toProcessor();

		fireDetectionEvent(openProcessor.duplicate(), "Original image");

		File fishImageFile = new File(dir, imageName);

		if (!fishImageFile.exists()) {
			LOGGER.warning("File does not exist: " + fishImageFile.getAbsolutePath());
			ImageProcessor ep = ImageConverter.createBlankImage(openProcessor.getWidth(),
					openProcessor.getHeight());

			ImageAnnotator an = new ImageAnnotator(ep).annotateString(ep.getWidth() / 2,
					ep.getHeight() / 2,
					"File not found");

			fireDetectionEvent(an.toProcessor().duplicate(), "FISH image");
			return null;
		}

		ImageProcessor fp = ImageImporter.importFileTo24bit(fishImageFile);
		fireDetectionEvent(fp.duplicate(), "FISH image");

		fireProgressEvent();
		return null;

	}

	@Override
	public boolean isValid(@NonNull Void entity) {
		// TODO Auto-generated method stub
		return false;
	}

}
