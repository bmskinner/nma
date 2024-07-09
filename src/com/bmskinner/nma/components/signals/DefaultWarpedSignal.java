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
package com.bmskinner.nma.components.signals;

import java.awt.Color;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.DefaultNucleus;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.io.XmlSerializable;
import com.bmskinner.nma.utility.StringUtils;

import ij.process.ImageProcessor;

/**
 * Key for mapping templates to images
 * 
 * @author ben
 * @since 1.14.0
 *
 */
public class DefaultWarpedSignal implements XmlSerializable, IWarpedSignal {
	private final Nucleus target;
	private final String targetName;
	private final String sourceDatasetName;
	private final String sourceSignalGroupName;

	private final UUID sourceDatasetId;
	private final boolean isCellsWithSignals;
	private final int threshold;
	private final boolean isBinarised;
	private final boolean isNormalised;
	private final byte[] image;
	private final int imageWidth;
	private Color pseudoColour = null;
	private boolean isShowPseudoColour = true;
	private int displayThreshold = 255; // show all by default

	public DefaultWarpedSignal(@NonNull Nucleus target, String targetName, String sourceDatasetName,
			String sourceSignalGroupName, UUID source, boolean isCellsWithSignals, int threshold,
			boolean isBinarised,
			boolean isNormalised, byte[] image, int imageWidth, Color pseudoColour,
			int displayThreshold) {
		this.target = target;
		this.targetName = targetName;
		this.sourceDatasetName = sourceDatasetName;
		this.sourceSignalGroupName = sourceSignalGroupName;
		this.sourceDatasetId = source;
		this.isCellsWithSignals = isCellsWithSignals;
		this.isNormalised = isNormalised;
		this.isBinarised = isBinarised;
		this.threshold = threshold;
		this.image = image;
		this.imageWidth = imageWidth;
		this.pseudoColour = pseudoColour;
		this.displayThreshold = displayThreshold;
	}

	public DefaultWarpedSignal(@NonNull Element e) throws ComponentCreationException {

		Nucleus c = null;
		for (Element el : e.getChild(XMLNames.XML_WARPED_SIGNAL_TARGET_SHAPE).getChildren()) {
			c = new DefaultNucleus(el);
		}

		if (c == null)
			throw new IllegalArgumentException("Target shape is missing from warped signal key");

		this.target = c;
		targetName = e.getAttributeValue(XMLNames.XML_WARPED_SIGNAL_TARGET_NAME);
		sourceDatasetId = UUID
				.fromString(e.getAttributeValue(XMLNames.XML_WARPED_SIGNAL_SOURCE_DATASET_ID));
		sourceDatasetName = e.getAttributeValue(XMLNames.XML_WARPED_SIGNAL_SOURCE_DATASET);
		sourceSignalGroupName = e.getAttributeValue(XMLNames.XML_WARPED_SIGNAL_SOURCE_SIGNAL);

		isCellsWithSignals = Boolean
				.valueOf(e.getAttributeValue(XMLNames.XML_WARPED_SIGNAL_IS_SIGNALS_ONLY));
		threshold = Integer
				.valueOf(e.getAttributeValue(XMLNames.XML_WARPED_SIGNAL_DETECTION_THRESHOLD));
		isNormalised = Boolean
				.valueOf(e.getAttributeValue(XMLNames.XML_WARPED_SIGNAL_IS_NORMALISED));
		isBinarised = Boolean.valueOf(e.getAttributeValue(XMLNames.XML_WARPED_SIGNAL_IS_BINARISED));
		imageWidth = Integer.parseInt(e.getAttributeValue(XMLNames.XML_WARPED_SIGNAL_IMAGE_WIDTH));
		pseudoColour = Color.decode(e.getAttributeValue(XMLNames.XML_SIGNAL_COLOUR));
		displayThreshold = Integer
				.parseInt(e.getAttributeValue(XMLNames.XML_WARPED_SIGNAL_DISPLAY_THRESHOLD));

		// Prior to 2.2.0, warped images were stored as byte arrays. From 2.2.0, we use
		// base64
		if (e.getChild(XMLNames.XML_WARPED_SIGNAL_BASE64) != null) {
			image = Base64.getDecoder().decode(e.getChildText(XMLNames.XML_WARPED_SIGNAL_BASE64));
		} else {
			image = StringUtils.hexToBytes(e.getChildText(XMLNames.XML_WARPED_SIGNAL_BYTES));
		}

	}

	@Override
	public IWarpedSignal duplicate() {
		return new DefaultWarpedSignal(this.target.duplicate(), this.targetName,
				this.sourceDatasetName,
				this.sourceSignalGroupName, this.sourceDatasetId, isCellsWithSignals,
				this.threshold, this.isBinarised,
				this.isNormalised, this.image, this.imageWidth, this.pseudoColour,
				this.displayThreshold);
	}

	@Override
	public Element toXmlElement() {
		Element e = new Element(XMLNames.XML_WARPED_SIGNAL);
		e.setAttribute(XMLNames.XML_WARPED_SIGNAL_TARGET_NAME, targetName);
		e.setAttribute(XMLNames.XML_WARPED_SIGNAL_SOURCE_DATASET, sourceDatasetName);
		e.setAttribute(XMLNames.XML_WARPED_SIGNAL_SOURCE_SIGNAL, sourceSignalGroupName);
		e.setAttribute(XMLNames.XML_WARPED_SIGNAL_SOURCE_DATASET_ID, sourceDatasetId.toString());
		e.setAttribute(XMLNames.XML_WARPED_SIGNAL_DETECTION_THRESHOLD, String.valueOf(threshold));
		e.setAttribute(XMLNames.XML_WARPED_SIGNAL_IS_SIGNALS_ONLY,
				String.valueOf(isCellsWithSignals));
		e.setAttribute(XMLNames.XML_WARPED_SIGNAL_IS_BINARISED, String.valueOf(isBinarised));
		e.setAttribute(XMLNames.XML_WARPED_SIGNAL_IS_NORMALISED, String.valueOf(isNormalised));
		e.setAttribute(XMLNames.XML_WARPED_SIGNAL_IMAGE_WIDTH, String.valueOf(imageWidth));
		e.setAttribute(XMLNames.XML_SIGNAL_COLOUR, String.valueOf(pseudoColour.getRGB()));
		e.setAttribute(XMLNames.XML_WARPED_SIGNAL_DISPLAY_THRESHOLD,
				String.valueOf(displayThreshold));
		e.addContent(new Element(XMLNames.XML_WARPED_SIGNAL_TARGET_SHAPE)
				.setContent(target.toXmlElement()));

		e.addContent(new Element(XMLNames.XML_WARPED_SIGNAL_BASE64)
				.setText(Base64.getEncoder().encodeToString(image)));
		return e;
	}

	@Override
	public Nucleus target() {
		return target;
	}

	@Override
	public String targetName() {
		return targetName;
	}

	@Override
	public String sourceSignalGroupName() {
		return sourceSignalGroupName;
	}

	@Override
	public String sourceDatasetName() {
		return sourceDatasetName;
	}

	@Override
	public UUID sourceDatasetId() {
		return sourceDatasetId;
	}

	@Override
	public boolean isCellsWithSignals() {
		return isCellsWithSignals;
	}

	@Override
	public boolean isNormalised() {
		return isNormalised;
	}

	@Override
	public boolean isBinarised() {
		return isBinarised;
	}

	@Override
	public int threshold() {
		return threshold;
	}

	@Override
	public int imageWidth() {
		return imageWidth;
	}

	@Override
	public byte[] image() {
		return image;
	}

	@Override
	public Color colour() {
		return pseudoColour;
	}

	@Override
	public void setColour(Color c) {
		pseudoColour = c;
	}

	@Override
	public int displayThreshold() {
		return this.displayThreshold;
	}

	@Override
	public void setDisplayThreshold(int i) {
		displayThreshold = i;
	}

	@Override
	public boolean isPseudoColour() {
		return this.isShowPseudoColour;
	}

	@Override
	public void setPseudoColour(boolean b) {
		isShowPseudoColour = b;
	}

	@Override
	public ImageProcessor toImage() {
		return IWarpedSignal.toImageProcessor(IWarpedSignal.byteToshortArray(image), imageWidth);
	}

	@Override
	public String toString() {
		return "Hash: " + hashCode() + target.getId() + " " + isCellsWithSignals + " " + threshold
				+ " " + isBinarised
				+ " " + isNormalised;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isCellsWithSignals ? 1231 : 1237);
		result = prime * result + ((target.getId() == null) ? 0 : target.getId().hashCode());
		result = prime * result + threshold;
		result = prime * result + (isBinarised ? 1231 : 1237);
		result = prime * result + (isNormalised ? 1231 : 1237);
		result = prime * result + (Arrays.hashCode(image));
		result = prime * result + imageWidth;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultWarpedSignal other = (DefaultWarpedSignal) obj;
		if (isCellsWithSignals != other.isCellsWithSignals)
			return false;
		if (isBinarised != other.isBinarised)
			return false;
		if (isNormalised != other.isNormalised)
			return false;
		if (!target.getId().equals(other.target.getId()))
			return false;
		if (!Arrays.equals(image, other.image))
			return false;
		if (imageWidth != other.imageWidth)
			return false;

		return true;
	}
}
