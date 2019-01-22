package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.MultipleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.Io.Exporter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Export the cells in a dataset as single images. Used for (e.g.) preparing
 * images for machine learning classification. Each cell image will be exported
 * with a bounding region as close to square as possible.  
 * @author bms41
 * @since 1.15.0
 *
 */
public class CellImageExportMethod extends MultipleDatasetAnalysisMethod implements Exporter, Loggable {
	
	private static final String IMAGE_FOLDER = "SingleNucleusImages_";

	public CellImageExportMethod(IAnalysisDataset dataset) {
		super(dataset);
	}
	
	public CellImageExportMethod(List<IAnalysisDataset> datasets) {
		super(datasets);
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
					stack("Unable to load image for nucleus "+n.getNameAndNumber(), e);
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

		ip.setRoi(x, y, totalWidth, totalWidth);
		return ip.crop();
	}


}
