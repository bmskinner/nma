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
package com.bmskinner.nma.io;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.Imageable;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.io.Io.Importer;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.image.ImageConverter;
import com.bmskinner.nma.visualisation.image.ImageFilterer;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ChannelSplitter;
import ij.process.ImageProcessor;
import loci.formats.ChannelSeparator;
import loci.formats.FormatException;
import loci.plugins.util.ImageProcessorReader;
import loci.plugins.util.LociPrefs;

/**
 * This class takes any given input image, and will convert it to the ImageStack
 * needed for the analyses. The DNA/DAPI will always be set at index 0, with
 * other signals appended .
 * 
 * @since 1.11.0
 *
 */
public class ImageImporter implements Importer {

	private static final Logger LOGGER = Logger.getLogger(ImageImporter.class.getName());

	private static final String SOURCE_IMAGE_IS_NOT_AVAILABLE = "Source image is not available";

	private static final int[] IMAGE_TYPES_PROCESSED = { ImagePlus.GRAY8, ImagePlus.COLOR_RGB,
			ImagePlus.GRAY16 };

	// The file types that the program will try to open
	protected static final String[] IMPORTABLE_FILE_TYPES = { ".tif", ".tiff", ".jpg", ".nd2" };

	// The prefix to use when exporting images
	public static final String IMAGE_PREFIX = "export.";

	// Images with these prefixes are ignored by the image importer
	protected static final String[] PREFIXES_TO_IGNORE = { IMAGE_PREFIX, "composite", "plot",
			"._" };

	// RGB colour channels
	public static final int RGB_RED = 0;
	public static final int RGB_GREEN = 1;
	public static final int RGB_BLUE = 2;

	// imported images - stack positions
	public static final int COUNTERSTAIN = 1; // ImageStack slices are
												// numbered from 1; first
												// slice is blue
	public static final int FIRST_SIGNAL_CHANNEL = 2; // ImageStack slices are
														// numbered from 1; first
														// slice is blue

	private static final int EIGHT_BIT = 8;

	private final File f;

	/**
	 * Construct from a file. Checks that the given File object is valid, and throws
	 * an IllegalArgumentException if not.
	 * 
	 * @param f the file to import
	 */
	public ImageImporter(final File f) {
		if (!Importer.isSuitableImportFile(f))
			throw new IllegalArgumentException(
					f.getAbsolutePath() + ": " + Importer.whyIsUnsuitableImportFile(f));
		this.f = f;
	}

	/**
	 * Get the image for the given cellular component, and crop it to the bounds of
	 * the given cell. If the image cannot be imported, a white colour processor is
	 * returned of sufficient dimensions to contain the cell.
	 * 
	 * @param cell
	 * @param c
	 * @return
	 */
	public static ImageProcessor importCroppedImageTo24bitGreyscale(@NonNull ICell cell,
			@NonNull CellularComponent c) {
		ImageProcessor ip = importFullImageTo24bitGreyscale(c);
		return ImageFilterer.crop(ip, cell);
	}

	/**
	 * Get the image for the given cellular component. If the image cannot be
	 * imported, a white colour processor is returned of sufficient dimensions to
	 * contain the component. The 8-bit image is converted to 24bit RGB to allow
	 * coloured annotations. The image is then cropped to the bounds of the
	 * component.
	 * 
	 * @param c the component to import
	 * @return an RGB greyscale image cropped to the component
	 */
	public static ImageProcessor importCroppedImageTo24bitGreyscale(@NonNull CellularComponent c) {
		ImageProcessor ip = importFullImageTo24bitGreyscale(c);
		return ImageFilterer.crop(ip, c);
	}

	/**
	 * Get the image for the given cellular component. If the image cannot be
	 * imported, a white colour processor is returned of sufficient dimensions to
	 * contain the component. The 8-bit image is converted to 24bit RGB to allow
	 * coloured annotations. Orient the image according to the component's rules,
	 * then the image is then cropped to the bounds of the oriented component.
	 * 
	 * @param c the component to import
	 * @return an RGB greyscale image cropped to the component
	 * @throws MissingLandmarkException
	 * @throws ComponentCreationException
	 */
	public static ImageProcessor importCroppedOrientedImageTo24bitGreyscale(
			@NonNull Nucleus c) throws MissingLandmarkException, ComponentCreationException {
		ImageProcessor ip = importCroppedImageTo24bitGreyscale(c);
		ip.flipVertical(); // Y axis needs inverting
		return ImageFilterer.orientImage(ip, c);
	}

	/**
	 * Get the image for the given cellular component. If the image cannot be
	 * imported, a white colour processor is returned of sufficient dimensions to
	 * contain the component. All RGB channels are imported. The image is then
	 * cropped to the bounds of the component.
	 * 
	 * @param c the component to import
	 * @return an RGB image cropped to the bounds of the component
	 */
	public static ImageProcessor importCroppedImageTo24bitRGB(@NonNull CellularComponent c) {
		ImageProcessor ip = importFullImageTo24bitRGB(c);
		return ImageFilterer.crop(ip, c);
	}

	/**
	 * Get the image for the given cellular component. If the image cannot be
	 * imported, a white colour processor is returned of sufficient dimensions to
	 * contain the component. The 8-bit image is converted to 24bit RGB to allow
	 * coloured annotations. No cropping is performed
	 * 
	 * @param c the component to import
	 * @return an RGB greyscale image containing the component
	 */
	public static ImageProcessor importFullImageTo24bitGreyscale(@NonNull CellularComponent c) {
		if (!c.getSourceFile().exists()) {
			return ImageFilterer.createWhiteColorProcessor(
					(int) c.getMaxX() + Imageable.COMPONENT_BUFFER,
					(int) c.getMaxY() + Imageable.COMPONENT_BUFFER);
		}
		try {
			ImageProcessor ip = new ImageImporter(c.getSourceFile()).importImage(c.getChannel());
			return new ImageConverter(ip).convertToRGBGreyscale().invert().toProcessor();
		} catch (ImageImportException e) {
			return ImageFilterer.createWhiteColorProcessor(
					(int) c.getMaxX() + Imageable.COMPONENT_BUFFER,
					(int) c.getMaxY() + Imageable.COMPONENT_BUFFER);
		}
	}

	/**
	 * Get the image for the given cellular component. If the image cannot be
	 * imported, a white colour processor is returned of sufficient dimensions to
	 * contain the component. All RGB channels are imported.
	 * 
	 * @param c the component to import
	 * @return an RGB image containing the component
	 */
	public static ImageProcessor importFullImageTo24bitRGB(@NonNull CellularComponent c) {
		if (!c.getSourceFile().exists()) {
			return ImageFilterer.createWhiteColorProcessor(
					(int) c.getMaxX() + Imageable.COMPONENT_BUFFER,
					(int) c.getMaxY() + Imageable.COMPONENT_BUFFER);
		}

		return importFileTo24bit(c.getSourceFile());
	}

	/**
	 * Get the image from which the component was detected. Opens the image and
	 * fetches the appropriate channel. This will return the 8-bit greyscale image
	 * used for object detection.
	 * 
	 * @return an 8-bit greyscale image
	 * @throws UnloadableImageException if the image can't be loaded
	 */
	public static ImageProcessor importFullImageTo8bit(@NonNull CellularComponent c)
			throws UnloadableImageException {
		if (!c.getSourceFile().exists())
			throw new UnloadableImageException(
					"Source image is not available: " + c.getSourceFile().getAbsolutePath());

		// Get the stack, make greyscale and invert
		int stack = ImageImporter.rgbToStack(c.getChannel());

		try {
			ImageStack imageStack = new ImageImporter(c.getSourceFile()).importToStack();
			return imageStack.getProcessor(stack);
		} catch (ImageImportException e) {
			LOGGER.log(Loggable.STACK,
					"Error importing source image " + c.getSourceFile().getAbsolutePath(), e);
			throw new UnloadableImageException(SOURCE_IMAGE_IS_NOT_AVAILABLE);
		}
	}

	/**
	 * Get the image from which the component was detected. Opens the image and
	 * fetches the appropriate channel. This will return the 8-bit greyscale image
	 * used for object detection. The image is then cropped to the component bounds.
	 * 
	 * @return an 8-bit greyscale image cropped to the component bounds
	 * @throws UnloadableImageException if the image can't be loaded
	 */
	public static ImageProcessor importCroppedImageTo8bit(@NonNull CellularComponent c)
			throws UnloadableImageException {
		ImageProcessor ip = importFullImageTo8bit(c);
		return ImageFilterer.crop(ip, c);
	}

	/**
	 * Checks that the given file is suitable for analysis. Is the file an image.
	 * Also check if it is in the 'banned list'. These are prefixes that are
	 * attached to exported images at later stages of analysis. This prevents
	 * exported images from previous runs being analysed.
	 *
	 * @param file the File to check
	 * @return a true or false of whether the file passed checks
	 */
	public static boolean fileIsImportable(File file) {
		if (file == null)
			return false;
		if (!file.isFile())
			return false;
		String fileName = file.getName();

		for (String prefix : PREFIXES_TO_IGNORE)
			if (fileName.startsWith(prefix))
				return false;

		for (String fileType : IMPORTABLE_FILE_TYPES)
			if (fileName.endsWith(fileType))
				return true;

		return false;
	}

	/**
	 * Given an RGB channel, get the ImageStack stack for internal use
	 * 
	 * @param channel the channel
	 * @return the stack
	 */
	public static int rgbToStack(int channel) {
		if (channel < 0)
			throw new IllegalArgumentException(CHANNEL_BELOW_ZERO_ERROR);

		switch (channel) {
		case RGB_RED:
			return FIRST_SIGNAL_CHANNEL;
		case RGB_GREEN:
			return FIRST_SIGNAL_CHANNEL + 1;
		default:
			return COUNTERSTAIN;
		}
	}

	/**
	 * Given a channel integer, return the name of the channel. Handles red (0),
	 * green (1) and blue(2). Other ints will return a null string.
	 * 
	 * @param channel
	 * @return
	 */
	public static String channelIntToName(int channel) {
		if (channel < 0)
			throw new IllegalArgumentException(CHANNEL_BELOW_ZERO_ERROR);

		switch (channel) {
		case RGB_RED:
			return "Red";
		case RGB_GREEN:
			return "Green";
		case RGB_BLUE:
			return "Blue";
		default:
			return "Octarine";
		}
	}

	private ImageStack importND2ToStack() throws ImageImportException {

		ImageProcessorReader r = new ImageProcessorReader(
				new ChannelSeparator(LociPrefs.makeImageReader()));

		try {
			r.setId(f.getAbsolutePath());

			int num = r.getImageCount();
			int width = r.getSizeX();
			int height = r.getSizeY();
			ImageStack stack = new ImageStack(width, height);
			byte[][][] lookupTable = new byte[r.getSizeC()][][];
			for (int i = 0; i < num; i++) {
				ImageProcessor ip = r.openProcessors(i)[0];
				stack.addSlice("" + (i + 1), ip);
				int channel = r.getZCTCoords(i)[1];
				lookupTable[channel] = r.get8BitLookupTable();
			}

			return stack;

		} catch (FormatException | IOException e) {
			LOGGER.log(Level.SEVERE, "Error opening .nd2 file", e);
		}
		return null;
	}

	/**
	 * Import and convert the image in the given file to an ImageStack
	 * 
	 * @return the ImageStack
	 */
	public ImageStack importToStack() throws ImageImportException {

		// Need to use BioFormats for nd2
		if (f.getName().endsWith(".nd2"))
			return importND2ToStack();

		// otherwise ImageJ can do it alone
		ImagePlus image = new ImagePlus(f.getAbsolutePath());

		ImageStack stack = convertToStack(image);
		image.close();
		return stack;
	}

	/**
	 * Import and convert the image in the given file to a ColorProcessor
	 * 
	 * @return the processor
	 */
	public static ImageProcessor importFileTo24bit(@NonNull File f) {
		return new ImagePlus(f.getAbsolutePath()).getProcessor();
	}

	/**
	 * Import the image in the defined file as a stack, and return an image
	 * converter
	 * 
	 * @return
	 * @throws ImageImportException
	 */
	public ImageConverter toConverter() throws ImageImportException {
		ImageProcessor ip = importFileTo24bit(f);
		return new ImageConverter(ip);
	}

	/**
	 * Import the image in the given file, and return an image processor for the
	 * channel requested. Inverts the greyscale image.
	 * 
	 * @param channel
	 * @return
	 */
	public ImageProcessor importImageAndInvert(int channel) throws ImageImportException {
		ImageProcessor ip = importImage(channel);
		ip.invert();
		return ip;
	}

	/**
	 * Import the image in the given file, and return an image processor for the
	 * channel requested.
	 * 
	 * @param channel
	 * @return
	 */
	public ImageProcessor importImage(int channel) throws ImageImportException {
		ImageStack s = importToStack();
		int stack = rgbToStack(channel);
		if (stack > s.getSize())
			throw new ImageImportException(f.getAbsolutePath() + " has only " + s.getSize()
					+ " slices; trying to fetch slice " + stack);
		return s.getProcessor(stack);
	}

	/**
	 * Test if the given image can be read by this program
	 * 
	 * @param image
	 * @return
	 */
	private boolean isImportable(@NonNull ImagePlus image) {
		for (int i : IMAGE_TYPES_PROCESSED)
			if (i == image.getType())
				return true;
		return false;
	}

	/**
	 * Create an ImageStack from the input image
	 * 
	 * @param image the image to be converted to a stack
	 * @return the stack with counterstain in slice 1, and other channels following
	 */
	private ImageStack convertToStack(@NonNull ImagePlus image) throws ImageImportException {
		if (!isImportable(image))
			throw new ImageImportException("Cannot handle image type: " + image.getType());

		switch (image.getType()) {
		case ImagePlus.GRAY8:
			return convert8bitToStack(image);
		case ImagePlus.COLOR_RGB:
			return convert24bitToStack(image);
		case ImagePlus.GRAY16:
			return convert16bitTo8bit(image);

		default: { // Should never occur given the test in isImportable(), but shows intent
			throw new ImageImportException("Unsupported image type: " + image.getType());
		}
		}
	}

	/**
	 * @param image the image to convert
	 * @return a stack with the input image as position 0
	 */
	private ImageStack convert8bitToStack(@NonNull final ImagePlus image) {
		ImageStack result = ImageStack.create(image.getWidth(), image.getHeight(), 0, EIGHT_BIT);
		result.addSlice("counterstain", image.getProcessor());
		result.deleteSlice(1); // remove the blank first slice
		return result;
	}

	/**
	 * Given an RGB image, convert it to a stack, with the blue channel first
	 * 
	 * @param image the image to convert to a stack
	 * @return the stack
	 */
	private ImageStack convert24bitToStack(@NonNull final ImagePlus image) {

		int imageDepth = 0; // number of images in the stack to begin
		int bitDepth = 8; // default 8 bit images

		// Create a new empty stack. There will be a blank image in the
		// stack at index 1. NB stacks do not use zero indexing.
		ImageStack result = ImageStack.create(image.getWidth(), image.getHeight(), imageDepth,
				bitDepth);

		// split out colour channel
		ImagePlus[] channels = ChannelSplitter.split(image);

		// Put each channel into the correct stack position
		result.addSlice("counterstain", channels[RGB_BLUE].getProcessor());
		result.addSlice(channels[RGB_RED].getProcessor());
		result.addSlice(channels[RGB_GREEN].getProcessor());
		result.deleteSlice(1); // remove the blank first slice
		return result;
	}

	/**
	 * Convert a 16 bit greyscale image. For now, this just down converts to an 8
	 * bit image.
	 * 
	 * @param image the 16 bit image to convert
	 * @return the stack
	 */
	private ImageStack convert16bitTo8bit(ImagePlus image) {
		// this is the ij.process.ImageConverter, not my
		// analysis.image.ImageConverter
		ij.process.ImageConverter converter = new ij.process.ImageConverter(image);
		converter.convertToGray8();
		return convert8bitToStack(image);
	}

	/**
	 * Thrown when a conversion fails or a file is not convertible
	 * 
	 * @author ben
	 *
	 */
	public class ImageImportException extends Exception {
		private static final long serialVersionUID = 1L;

		public ImageImportException() {
			super();
		}

		public ImageImportException(String message) {
			super(message);
		}

		public ImageImportException(String message, Throwable cause) {
			super(message, cause);
		}

		public ImageImportException(Throwable cause) {
			super(cause);
		}
	}
}
