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

import java.util.Arrays;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.DefaultConsensusNucleus;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;
import com.bmskinner.nuclear_morphology.utility.StringUtils;

/**
 * Key for mapping templates to images
 * 
 * @author ben
 * @since 1.14.0
 *
 */
public record DefaultWarpedSignal(@NonNull Nucleus target, @NonNull UUID templateId, boolean isCellsWithSignals,
		int threshold, boolean isBinarised, boolean isNormalised, byte[] image, int imageWidth)
		implements XmlSerializable, IWarpedSignal {

	public static DefaultWarpedSignal of(@NonNull Element e) throws ComponentCreationException {

		Nucleus c = null;
		for (Element el : e.getChild("TargetShape").getChildren()) {
			c = new DefaultConsensusNucleus(el);
		}

		if (c == null)
			throw new IllegalArgumentException("Target shape is missing from warped signal key");

		byte[] b = StringUtils.hexToBytes(e.getChildText("Bytes"));

		return new DefaultWarpedSignal(c, UUID.fromString(e.getChildText("TemplateId")),
				Boolean.valueOf(e.getChildText("IsCellWithSignalsOnly")), Integer.valueOf(e.getChildText("Threshold")),
				Boolean.valueOf(e.getChildText("IsBinarised")), Boolean.valueOf(e.getChildText("IsNormalised")), b,
				Integer.parseInt(e.getChildText("Width")));
	}

	@Override
	public IWarpedSignal duplicate() {
		return new DefaultWarpedSignal(this.target.duplicate(), this.templateId, this.isCellsWithSignals,
				this.threshold, this.isBinarised, this.isNormalised, this.image, this.imageWidth);
	}

	@Override
	public Element toXmlElement() {
		Element e = new Element("WarpedSignalKey");
		e.addContent(new Element("TargetShape").setContent(target.toXmlElement()));
		e.addContent(new Element("TargetShapeId").setText(target.getID().toString()));
		e.addContent(new Element("IsCellWithSignalsOnly").setText(String.valueOf(isCellsWithSignals)));
		e.addContent(new Element("Threshold").setText(String.valueOf(threshold)));
		e.addContent(new Element("TemplateId").setText(templateId.toString()));
		e.addContent(new Element("IsBinarised").setText(String.valueOf(isBinarised)));
		e.addContent(new Element("IsNormalised").setText(String.valueOf(isNormalised)));
		e.addContent(new Element("Bytes").setText(StringUtils.bytesToHex(image)));
		e.addContent(new Element("Width").setText(String.valueOf(imageWidth)));

		return e;
	}

	@Override
	public String toString() {
		return "Hash: " + hashCode() + " " + templateId + " " + target.getID() + " " + isCellsWithSignals + " "
				+ threshold + " " + isBinarised + " " + isNormalised;
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
