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
package com.bmskinner.nuclear_morphology.components.signals;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * Interface for signals that have been derived from warping signal from one or
 * more nuclei onto a consensus template.
 * 
 * @author bms41
 * @since 1.14.0
 *
 */
public interface IWarpedSignal extends XmlSerializable {

	/**
	 * Create a byte array from the given byte processor.
	 * 
	 * @param ip
	 * @return
	 */
	static byte[][] toByteArrayArray(ByteProcessor ip) {
		byte[][] arr = new byte[ip.getWidth()][ip.getHeight()];

		for (int w = 0; w < ip.getWidth(); w++) {
			for (int h = 0; h < ip.getHeight(); h++) {
				arr[w][h] = (byte) ip.get(w, h);
			}
		}
		return arr;
	}

	/**
	 * Create a byte array from the given image processor
	 * 
	 * @param ip the image to be converted
	 * @return the byte array or an empty array if the image processor cannot be
	 *         converted
	 */
	static byte[] toArray(ImageProcessor ip) {
		if (ip instanceof ByteProcessor)
			return (byte[]) ip.getPixels();
		if (ip instanceof ShortProcessor)
			return shortToByteArray((short[]) ip.getPixels());
		return new byte[0];
	}

	/**
	 * Convert a short array to a byte array for serialising
	 * 
	 * @param shortArray the array of shorts
	 * @return the byte array representation
	 */
	static byte[] shortToByteArray(short[] shortArray) {
		ByteBuffer buf = ByteBuffer.allocate(Short.SIZE / Byte.SIZE * shortArray.length);
		buf.asShortBuffer().put(shortArray);
		return buf.array();
	}

	/**
	 * Convert a byte array to a short array for deserialising
	 * 
	 * @param bytes the byte array encoding a short array
	 * @return the short array
	 */
	static short[] byteToshortArray(byte[] bytes) {
		ShortBuffer buf = ByteBuffer.wrap(bytes).asShortBuffer();
		short[] shortArray = new short[buf.limit()];
		buf.get(shortArray);
		return shortArray;
	}

	/**
	 * Create an 8-bit byte processor from the given array
	 * 
	 * @param arr
	 * @return
	 */
	static ImageProcessor toImageProcessor(byte[] arr, int width) {
		ByteProcessor image = new ByteProcessor(width, arr.length / width);
		image.setPixels(arr);
		return image;
	}

	/**
	 * Create a 16-bit short processor from the given array
	 * 
	 * @param arr
	 * @return
	 */
	static ImageProcessor toImageProcessor(short[] arr, int width) {
		ShortProcessor image = new ShortProcessor(width, arr.length / width);
		image.setPixels(arr);
		return image;
	}

	/**
	 * Create a copy of this signal
	 * 
	 * @return
	 */
	IWarpedSignal duplicate();

	Nucleus target();

	UUID sourceDatasetId();

	String targetName();

	String sourceSignalGroupName();

	String sourceDatasetName();

	boolean isCellsWithSignals();

	boolean isNormalised();

	boolean isBinarised();

	int threshold();

	int imageWidth();

	byte[] image();

	ImageProcessor toImage();

	Color colour();

	void setColour(Color c);

	int displayThreshold();

	void setDisplayThreshold(int i);

	boolean isPseudoColour();

	void setPseudoColour(boolean b);

	// /**
//	 * Defines the signal group which was warped
//	 * 
//	 * @return
//	 */
//	@NonNull
//	UUID getSignalGroupId();
//
//	/**
//	 * Get the target shapes onto which signals have been warped
//	 * 
//	 * @return
//	 */
//	@NonNull
//	Set<DefaultWarpedSignal> getWarpedSignalKeys();
//
//	/**
//	 * Add a warped signal image for the given template
//	 * 
//	 * @param target                the target object signals were warped on to
//	 * @param templateId            the id of the dataset the signals came from
//	 * @param name                  the name of the template object
//	 * @param threshold             the threshold level for the images before
//	 *                              warping
//	 * @param isCellWithSignalsOnly whether the image covers all cells in the source
//	 *                              dataset
//	 * @param isBinarised           if the images were binarised before warping
//	 * @param image                 the warped image
//	 */
//	void addWarpedImage(@NonNull Nucleus target, @NonNull UUID templateId, @NonNull String name,
//			boolean isCellWithSignalsOnly, int threshold, boolean isBinarised, boolean isNormalised,
//			@NonNull ImageProcessor image);
//
//	/**
//	 * Remove the given warped image
//	 * 
//	 * @param key the key to remove
//	 */
//	void removeWarpedImage(@NonNull DefaultWarpedSignal key);
//
//	/**
//	 * Get the warped signal image corresponding to the signals warped onto the
//	 * given target shape
//	 * 
//	 * @param template
//	 * @param isCellWithSignalsOnly whether the image covers all cells in the source
//	 *                              dataset, or just those with defined signals
//	 * @return
//	 */
//	Optional<ImageProcessor> getWarpedImage(@NonNull Nucleus template, @NonNull UUID templateId,
//			boolean isCellWithSignalsOnly, int threshold, boolean isBinarised, boolean isNormalised);
//
//	/**
//	 * Get the warped signal image corresponding to the signals warped onto the
//	 * given target shape
//	 * 
//	 * @param key the signal key
//	 * @return
//	 */
//	Optional<ImageProcessor> getWarpedImage(@NonNull DefaultWarpedSignal key);
//
//	/**
//	 * Get the name of the target shape
//	 * 
//	 * @param template
//	 * @return
//	 */
//	String getTargetName(@NonNull DefaultWarpedSignal key);

}
