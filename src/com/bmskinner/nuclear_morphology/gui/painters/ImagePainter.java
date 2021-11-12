package com.bmskinner.nuclear_morphology.gui.painters;

import java.awt.image.BufferedImage;

/**
 * Interface for painters
 * @author ben
 *
 */
public interface ImagePainter {

	/**
	 * Paint the given element onto an image 
	 * @param input the image to be painted on
	 * @param element the element to paint
	 * @param ratio the size ratio adjustment
	 * @return the painted image
	 */
	BufferedImage paint(BufferedImage input);

	/**
	 * Paint a section of a large image onto a smaller
	 * image to simulate magnification.
	 * @param smallInput the smaller input image
	 * @param largeInput the larger input image
	 * @param cx the x coordinate in the small image
	 * @param cy the y coordinate in the small image
	 * @param smallRadius the radius of the small box
	 * @param bigRadius the radius of the big box
	 * @return
	 */
	BufferedImage paintMagnified(BufferedImage smallInput, BufferedImage largeInput, 
			int cx, int cy, int smallRadius, int bigRadius);

}