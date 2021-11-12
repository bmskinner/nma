package com.bmskinner.nuclear_morphology.gui.painters;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.components.cells.ICell;

public class WarpedCellPainter implements ImagePainter {
	
private static final Logger LOGGER = Logger.getLogger(WarpedCellPainter.class.getName());
	
	private ICell cell;
	private double originalWidth;
	
	/**
	 * Create with cell to paint and ratio for final image
	 * @param cell
	 * @param ratio
	 */
	public WarpedCellPainter(ICell cell, double originalWidth) {
		this.cell = cell;
		this.originalWidth = originalWidth;
	}

	@Override
	public BufferedImage paint(BufferedImage input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BufferedImage paintMagnified(BufferedImage smallInput, BufferedImage largeInput, int cx, int cy,
			int smallRadius, int bigRadius) {
		// TODO Auto-generated method stub
		return null;
	}

}
