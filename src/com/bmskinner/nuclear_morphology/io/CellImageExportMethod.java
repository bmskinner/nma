package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.MultipleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.io.Io.Exporter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;

/**
 * Export the cells in a dataset as single images. Used for (e.g.) preparing
 * images for machine learning classification. Each cell image will be exported
 * with a bounding region as close to square as possible.  
 * @author bms41
 * @since 1.15.0
 *
 */
public class CellImageExportMethod extends MultipleDatasetAnalysisMethod implements Exporter {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	private static final String IMAGE_FOLDER = "SingleNucleusImages_";
	
	private final HashOptions options;
	
	public static final String MASK_BACKGROUND = "Mask background";
	public static final boolean DEAULT_MASK_BACKGROUND = false;

	public CellImageExportMethod(IAnalysisDataset dataset, HashOptions options) {
		super(dataset);
		this.options = options;
	}
	
	public CellImageExportMethod(List<IAnalysisDataset> datasets, HashOptions options) {
		super(datasets);
		this.options = options;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		for(IAnalysisDataset d : datasets)
			exportImages(d);
		return new DefaultAnalysisResult(datasets);
	}

	private void exportImages(IAnalysisDataset d) {

		File outFolder = new File(d.getSavePath().getParent()+File.separator+IMAGE_FOLDER+d.getName());
		if(!outFolder.exists())
			outFolder.mkdirs();
		
		for(ICell c : d.getCollection()) {
			for(Nucleus n : c.getNuclei()) {
				try {
					ImageProcessor ip = cropToSquare(n);
					ImagePlus imp = new ImagePlus("", ip);
					String fileName = String.format("%s_%s.tiff", n.getNameAndNumber(), c.getId());
					IJ.saveAsTiff(imp, new File(outFolder, fileName).getAbsolutePath());
					fireProgressEvent();
					
				} catch (UnloadableImageException e) {
					LOGGER.log(Loggable.STACK, "Unable to load image for nucleus "+n.getNameAndNumber(), e);
				}
			}
		}

	}

	/**
	 * Crop the source image of the nucleus to a square containing just 
	 * the nucleus. 
	 * TODO: fixed size of 265, scaling based on pixel/micron
	 * TODO: mask regions not within the nucleus ROI (avoid including other nuclei fragments)
	 * @param n the nucleus to be exported
	 * @return
	 * @throws UnloadableImageException
	 */
	private ImageProcessor cropToSquare(Nucleus n) throws UnloadableImageException {
		int[] positions = n.getPosition();
		ImageProcessor ip = n.getGreyscaleImage();
		
		

		int padding = CellularComponent.COMPONENT_BUFFER;
		
		int w = positions[CellularComponent.WIDTH];
		int h = positions[CellularComponent.HEIGHT];
		
		int square     = Math.max(w,h);
		int totalWidth = square+(padding*2);
		
		int xDiff = (w-square)/2;
		int yDiff = (h-square)/2;

		int x = positions[CellularComponent.X_BASE] - padding + xDiff;
		int y = positions[CellularComponent.Y_BASE] - padding + yDiff;

		x = x < 0 ? 0 : x;
		y = y < 0 ? 0 : y;
		
		if(options.getBoolean(MASK_BACKGROUND)) {
			// mask out everything not inside the nucleus ROI
			// Only check the region we are cropping to
			Roi roi = n.toOriginalRoi();
			for(int xx=x; xx<x+totalWidth; xx++) {
				if(xx>=ip.getWidth())
					continue;
				for(int yy=y; yy<y+totalWidth; yy++) {
					if(yy>=ip.getHeight())
						continue;
					if(roi.contains(xx, yy))
						continue;
					ip.set(xx, yy, 0);
				}
			}
		}
		

		ip.setRoi(x, y, totalWidth, totalWidth);
		return ip.crop();
	}


}
