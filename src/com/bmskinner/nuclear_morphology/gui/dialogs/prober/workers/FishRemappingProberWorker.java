package com.bmskinner.nuclear_morphology.gui.dialogs.prober.workers;

import java.io.File;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.DetectionImageType;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageProberTableCell;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageSet;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

import ij.ImageStack;
import ij.process.ImageProcessor;

public class FishRemappingProberWorker extends ImageProberWorker {
	
	private static final String FISH_FOLDER_IS_FILE_ERROR = "FISH directory is not a folder";
	
	private final File dir;
	
	public FishRemappingProberWorker(final File f, 
			final IDetectionOptions options, 
			final ImageSet type, 
			final TableModel model, 
			final File postFishDir) {
		
		super(f, options, type, model);
		
		if( ! postFishDir.isDirectory()){
			throw new IllegalArgumentException(FISH_FOLDER_IS_FILE_ERROR);
		}
		
		this.dir = postFishDir;
	}

	@Override
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
		ImageProcessor openProcessor = new ImageConverter(stack)
				.convertToGreyscale()
				.invert()
				.toProcessor();

		
		ImageProberTableCell iconCell = makeIconCell(openProcessor, true, DetectionImageType.ORIGINAL);
		publish(iconCell);
				

		File fishImageFile = new File(dir, imageName);
		
		if( ! fishImageFile.exists()){
			warn("File does not exist: "+fishImageFile.getAbsolutePath());
			ImageProcessor ep = ImageConverter.createBlankImage(openProcessor.getWidth(), openProcessor.getHeight())
					.toProcessor();
			
			ImageAnnotator an = new ImageAnnotator(ep)
					.annotateString(ep.getWidth()/2, ep.getHeight()/2, "File not found");
			ImageProberTableCell iconCell1 = makeIconCell(an.toProcessor(), true, DetectionImageType.FISH_IMAGE);
			publish(iconCell1);
			return;
		}
			
		ImageStack fishStack;
		try {
			fishStack = new ImageImporter(fishImageFile).importImage();
		} catch (ImageImportException e) {
			error("Error importing FISH image file "+fishImageFile.getAbsolutePath(), e);
			return;
		}

		ImageProcessor fp = new ImageConverter(fishStack).convertToRGB().toProcessor();

		ImageProberTableCell iconCell2 = makeIconCell(fp, true, DetectionImageType.FISH_IMAGE);
		publish(iconCell2);


}
}
