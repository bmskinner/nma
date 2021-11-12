package com.bmskinner.nuclear_morphology.gui.painters;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;

public class WarpedCellPainter implements ImagePainter {

	private static final Logger LOGGER = Logger.getLogger(WarpedCellPainter.class.getName());

	private IAnalysisDataset dataset;
	private ICell cell;
	private double originalWidth;

	/**
	 * Create with cell to paint and ratio for final image
	 * @param cell
	 * @param ratio
	 */
	public WarpedCellPainter(IAnalysisDataset dataset, ICell cell) {
		this.dataset = dataset;
		this.cell = cell;
		this.originalWidth = cell.getPrimaryNucleus().getPosition()[Imageable.WIDTH] + (Imageable.COMPONENT_BUFFER*2);
	}

	@Override
	public BufferedImage paint(BufferedImage input) {

		// TODO Paint the consensus segment pattern onto the image, since
		// it has been warped to the consensus shape
		return input;
	}

	@Override
	public BufferedImage paintMagnified(BufferedImage smallInput, BufferedImage largeInput, int cx, int cy,
			int smallRadius, int bigRadius) {
		// TODO Auto-generated method stub
		return smallInput;
	}

}
