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
import java.util.Arrays;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.DefaultConsensusNucleus;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;
import com.bmskinner.nuclear_morphology.utility.StringUtils;

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
	private final UUID source;
	private final boolean isCellsWithSignals;
	private final int threshold;
	private final boolean isBinarised;
	private final boolean isNormalised;
	private final byte[] image;
	private final int imageWidth;
	private Color pseudoColour = null;
	private boolean isShowPseudoColour = true;
	private int displayThreshold = 255; // show all by default

	public DefaultWarpedSignal(@NonNull Nucleus target, String targetName, UUID source, boolean isCellsWithSignals,
			int threshold, boolean isBinarised, boolean isNormalised, byte[] image, int imageWidth, Color pseudoColour,
			int displayThreshold) {
		this.target = target;
		this.targetName = targetName;
		this.source = source;
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
		for (Element el : e.getChild("TargetShape").getChildren()) {
			c = new DefaultConsensusNucleus(el);
		}

		if (c == null)
			throw new IllegalArgumentException("Target shape is missing from warped signal key");

		this.target = c;
		targetName = e.getAttributeValue("targetName");
		source = UUID.fromString(e.getAttributeValue("source"));
		isCellsWithSignals = Boolean.valueOf(e.getAttributeValue("isSignalsOnly"));
		threshold = Integer.valueOf(e.getAttributeValue("threshold"));
		isNormalised = Boolean.valueOf(e.getAttributeValue("isNormalised"));
		isBinarised = Boolean.valueOf(e.getAttributeValue("isBinarised"));
		imageWidth = Integer.parseInt(e.getAttributeValue("width"));
		pseudoColour = Color.decode(e.getAttributeValue("colour"));
		displayThreshold = Integer.parseInt(e.getAttributeValue("displayThreshold"));
		image = StringUtils.hexToBytes(e.getChildText("Bytes"));
	}

	@Override
	public IWarpedSignal duplicate() {
		return new DefaultWarpedSignal(this.target.duplicate(), this.targetName, this.source, isCellsWithSignals,
				this.threshold, this.isBinarised, this.isNormalised, this.image, this.imageWidth, this.pseudoColour,
				this.displayThreshold);
	}

	@Override
	public Element toXmlElement() {
		Element e = new Element("WarpedSignal");
		e.setAttribute("targetName", targetName);
		e.setAttribute("source", source.toString());
		e.setAttribute("threshold", String.valueOf(threshold));
		e.setAttribute("isSignalsOnly", String.valueOf(isCellsWithSignals));
		e.setAttribute("isBinarised", String.valueOf(isBinarised));
		e.setAttribute("isNormalised", String.valueOf(isNormalised));
		e.setAttribute("width", String.valueOf(imageWidth));
		e.setAttribute("colour", String.valueOf(pseudoColour.getRGB()));
		e.setAttribute("displayThreshold", String.valueOf(displayThreshold));
		e.addContent(new Element("TargetShape").setContent(target.toXmlElement()));
		e.addContent(new Element("Bytes").setText(StringUtils.bytesToHex(image)));

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
	public UUID source() {
		return source;
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
		return "Hash: " + hashCode() + target.getID() + " " + isCellsWithSignals + " " + threshold + " " + isBinarised
				+ " " + isNormalised;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isCellsWithSignals ? 1231 : 1237);
		result = prime * result + ((target.getID() == null) ? 0 : target.getID().hashCode());
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
		if (target.getID() == null) {
			if (other.target.getID() != null)
				return false;
		} else if (!target.getID().equals(other.target.getID()))
			return false;
		if (!Arrays.equals(image, other.image))
			return false;
		if (imageWidth != other.imageWidth)
			return false;

		return true;
	}
}
