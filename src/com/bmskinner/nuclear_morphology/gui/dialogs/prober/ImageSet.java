package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.util.Set;

/**
 * Describes the images that are to be displayed in an image prober
 * @author ben
 * @since 1.13.4
 *
 */
public interface ImageSet {
	
	
	/**
	 * The set of prober images for standard nucleus detection
	 */
	static final ImageSet NUCLEUS_IMAGE_SET = new DefaultImageSet()
			.add(DetectionImageType.KUWAHARA)
			.add(DetectionImageType.FLATTENED)
			.add(DetectionImageType.EDGE_DETECTION)
			.add(DetectionImageType.MORPHOLOGY_CLOSED)
			.add(DetectionImageType.DETECTED_OBJECTS)
			.add(DetectionImageType.ANNOTAED_OBJECTS);
	
	/**
	 * The set of images for neutrophil detection
	 */
	static final ImageSet NEUTROPHIL_IMAGE_SET = new DefaultImageSet()
			.add(DetectionImageType.CYTO_FLATTENED)
			.add(DetectionImageType.CYTOPLASM)
			.add(DetectionImageType.NUCLEUS_FLATTENED)
			.add(DetectionImageType.NUCLEUS)
			.add(DetectionImageType.DETECTED_OBJECTS)
			.add(DetectionImageType.ANNOTAED_OBJECTS);
	
	/**
	 * The set of images for FISH signal detection
	 */
	static final ImageSet SIGNAL_IMAGE_SET = new DefaultImageSet()
			.add(DetectionImageType.DETECTED_OBJECTS)
			.add(DetectionImageType.ANNOTAED_OBJECTS);
	
	/**
	 * The set of images for FISH remapping
	 */
	static final ImageSet FISH_REMAPPING_IMAGE_SET = new DefaultImageSet()
			.add(DetectionImageType.ORIGINAL)
			.add(DetectionImageType.FISH_IMAGE);

	
	/**
	 * Add the given image type to the set at the next index
	 * @param t the image type to add
	 * @return the set
	 */
	ImageSet add(ImageType t);

	/**
	 * Get the number of items in the set
	 * @return
	 */
	int size();
	
	/**
	 * Get the image type at the given index
	 * @param i
	 * @return
	 */
	ImageType getType(int i);

	/**
	 * Get the position of the given image type
	 * @param s
	 * @return
	 */
	int getPosition(ImageType s);
	
	Set<ImageType> values();
}
