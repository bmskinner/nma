package com.bmskinner.nma.io;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.MultipleDatasetAnalysisMethod;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.logging.Loggable;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;

/**
 * Export the cells in a dataset as single images. Used for (e.g.) preparing
 * images for machine learning classification. Each cell image will be exported
 * with a bounding region as close to square as possible.
 * 
 * @author bms41
 * @since 1.15.0
 *
 */
public class CellImageExportMethod extends MultipleDatasetAnalysisMethod implements Io {

	private static final Logger LOGGER = Logger.getLogger(CellImageExportMethod.class.getName());

	private static final String IMAGE_FOLDER = "SingleNucleusImages_";

	private final HashOptions options;

	/**
	 * When exporting single cell images this option determines whether the output
	 * images are masked to only pixels within the nuclear border. If true, only
	 * pixels within the nucleus will be written to the output image. All other
	 * pixels in the image will be 0 (black). If false, all pixels from the original
	 * image within the export region will be written. The default value for this
	 * option is {@link CellImageExportMethod.MASK_BACKGROUND_DEFAULT}.
	 * 
	 */
	public static final String MASK_BACKGROUND_KEY = "Mask background";
	public static final boolean MASK_BACKGROUND_DEFAULT = false;

	/**
	 * When exporting single cell images this option determines whether the output
	 * images should be of fixed dimensions. If true, all images have the same width
	 * and height (specified by
	 * {@link CellImageExportMethod.SINGLE_CELL_IMAGE_WIDTH_KEY}. If false, each
	 * image will be a square with width and determined by the maximum of the
	 * nucleus bounding box dimensions. The default value for this option is
	 * {@link CellImageExportMethod.SINGLE_CELL_IMAGE_IS_NORMALISE_WIDTH_DEFAULT}.
	 * 
	 */
	public static final String SINGLE_CELL_IMAGE_IS_NORMALISE_WIDTH_KEY = "SINGLE_CELL_IMAGE_WIDTH";
	public static final boolean SINGLE_CELL_IMAGE_IS_NORMALISE_WIDTH_DEFAULT = false;

	/**
	 * The value to use if normalising single cell images to a consistent size.
	 * Width and height will be constrained to this value. Note that setting this
	 * value smaller than a cell may result in cropping the cell.
	 */
	public static final String SINGLE_CELL_IMAGE_WIDTH_KEY = "SINGLE_CELL_IMAGE_WIDTH";
	public static final int SINGLE_CELL_IMAGE_WIDTH_DEFAULT = 255;

	/**
	 * If true, all colour channels from the nucleus detection image are exported
	 * when single cell images are created. If false, only the channel used for
	 * nucleus detection is exported.
	 */
	public static final String SINGLE_CELL_IMAGE_IS_RGB_KEY = "SINGLE_CELL_IMAGE_IS_RGB";
	public static final boolean SINGLE_CELL_IMAGE_IS_RGB_DEFAULT = true;

	public CellImageExportMethod(@NonNull IAnalysisDataset dataset, @NonNull HashOptions options) {
		super(dataset);
		this.options = options;
	}

	public CellImageExportMethod(@NonNull List<IAnalysisDataset> datasets,
			@NonNull HashOptions options) {
		super(datasets);
		this.options = options;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		for (IAnalysisDataset d : datasets)
			exportImages(d);
		return new DefaultAnalysisResult(datasets);
	}

	private void exportImages(IAnalysisDataset d) {

		File outFolder = new File(
				d.getSavePath().getParent() + File.separator + IMAGE_FOLDER + d.getName());
		if (!outFolder.exists())
			outFolder.mkdirs();

		for (ICell c : d.getCollection()) {
			for (Nucleus n : c.getNuclei()) {
				try {
					ImageProcessor ip = cropToSquare(n);
					ImagePlus imp = new ImagePlus("", ip);
					String fileName = String.format("%s_%s.tiff", n.getNameAndNumber(), c.getId());
					IJ.saveAsTiff(imp, new File(outFolder, fileName).getAbsolutePath());
					fireProgressEvent();

				} catch (UnloadableImageException e) {
					LOGGER.log(Loggable.STACK,
							"Unable to load image for nucleus " + n.getNameAndNumber(), e);
				}
			}
		}

	}

	/**
	 * Crop the source image of the nucleus to a square containing just the nucleus.
	 * TODO: fixed size of 265, scaling based on pixel/micron TODO: mask regions not
	 * within the nucleus ROI (avoid including other nuclei fragments)
	 * 
	 * @param n the nucleus to be exported
	 * @return
	 * @throws UnloadableImageException
	 */
	private ImageProcessor cropToSquare(Nucleus n) throws UnloadableImageException {

		// Choose whether to use the RGB or greyscale image
		ImageProcessor ip = options.getBoolean(SINGLE_CELL_IMAGE_IS_RGB_KEY)
				? ImageImporter.importFileTo24bit(n.getSourceFile())
				: ImageImporter.importFullImageTo8bit(n);

		int padding = CellularComponent.COMPONENT_BUFFER;

		int x = 0;
		int y = 0;

		int totalWidth = 0;

		// Should we normalise to a constant export size, or let each cell be a square
		// of independent size?
		if (options.getBoolean(SINGLE_CELL_IMAGE_IS_NORMALISE_WIDTH_KEY)) {
			totalWidth = options.getInt(SINGLE_CELL_IMAGE_WIDTH_KEY);

			int w = (int) n.getWidth();
			int h = (int) n.getHeight();

			// Centre square on nucleus position
			int xDiff = (totalWidth - w) / 2;
			int yDiff = (totalWidth - h) / 2;

			x = n.getXBase() - xDiff;
			y = n.getYBase() - yDiff;

			x = x < 0 ? 0 : x;
			y = y < 0 ? 0 : y;
		} else {

			// Use independent size for each nucleus
			int w = (int) n.getWidth();
			int h = (int) n.getHeight();

			int square = Math.max(w, h);
			totalWidth = square + (padding * 2);

			int xDiff = (w - square) / 2;
			int yDiff = (h - square) / 2;

			x = n.getXBase() - padding + xDiff;
			y = n.getYBase() - padding + yDiff;

			x = x < 0 ? 0 : x;
			y = y < 0 ? 0 : y;
		}

		if (options.getBoolean(MASK_BACKGROUND_KEY)) {
			// mask out everything not inside the nucleus ROI
			// Only check the region we are cropping to
			Roi roi = n.toOriginalRoi();
			for (int xx = x; xx < x + totalWidth; xx++) {
				if (xx >= ip.getWidth())
					continue;
				for (int yy = y; yy < y + totalWidth; yy++) {
					if (yy >= ip.getHeight())
						continue;
					if (roi.contains(xx, yy))
						continue;
					ip.set(xx, yy, 0);
				}
			}
		}

		ip.setRoi(x, y, totalWidth, totalWidth);
		return ip.crop();
	}

}
