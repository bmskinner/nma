package com.bmskinner.nma.visualisation.image;

import java.awt.image.BufferedImage;

/**
 * Interface for painters
 * 
 * @author ben
 *
 */
public interface ImagePainter {

	/**
	 * Paint the object in this painter with any decorations and return an image of
	 * the desired dimensions
	 * 
	 * @param isOriented is the component oriented
	 * @param w          the max width of the output image
	 * @param h          the max height of the output image
	 * @return
	 */
	BufferedImage paintDecorated(int w, int h);

	/**
	 * Return an image of the desired dimensions containing just the base object
	 * image without decorations.
	 * 
	 * @param isOriented is the component oriented
	 * @param w          the max width of the output image
	 * @param h          the max height of the output image
	 * @return
	 */
	BufferedImage paintRaw(int w, int h);

	/**
	 * Paint a section of a large image onto a smaller image to simulate
	 * magnification.
	 * 
	 * @param smallInput  the smaller input image
	 * @param largeInput  the larger input image
	 * @param cx          the x coordinate in the small image
	 * @param cy          the y coordinate in the small image
	 * @param smallRadius the radius of the small box
	 * @param bigRadius   the radius of the big box
	 * @return
	 */
	BufferedImage paintMagnified(BufferedImage smallInput, BufferedImage largeInput, int cx, int cy, int smallRadius,
			int bigRadius);

}