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

import java.io.Serializable;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultConsensusNucleus;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Key for mapping templates to images
 * @author ben
 * @since 1.14.0
 *
 */
public class WarpedSignalKey implements Serializable, XmlSerializable {
	
	private static final Logger LOGGER = Logger.getLogger(WarpedSignalKey.class.getName());

	private static final long serialVersionUID = 1L;
	private final CellularComponent targetShape;
	private final UUID targetShapeId;
	private final boolean isCellWithSignalsOnly;
	private final int threshold;
	private final UUID templateId;
	private final boolean isBinarised;
	private final boolean isNormalised;
	
	public WarpedSignalKey(@NonNull CellularComponent target, 
			@NonNull UUID templateId, 
			final boolean cellsWithSignals, 
			final int threshold,
			final boolean isBinarised,
			final boolean isNormalised) {
		targetShape = target;
		targetShapeId = target.getID();
		isCellWithSignalsOnly = cellsWithSignals;
		this.threshold = threshold;
		this.templateId = templateId;
		this.isBinarised = isBinarised;
		this.isNormalised = isNormalised;
	}
	
	public WarpedSignalKey(@NonNull Element e) {
		
		CellularComponent c = null;
		try {
			c = new DefaultConsensusNucleus(e.getChild("TargetShape"));
		} catch (ComponentCreationException e1) {
			LOGGER.log(Loggable.STACK, "Unable to unmarshal warped key target shape", e);
		}
		
		targetShape = c;
		
		targetShapeId = UUID.fromString(e.getChildText("TargetShapeId"));
		templateId    = UUID.fromString(e.getChildText("TemplateId"));
		
		isCellWithSignalsOnly = Boolean.valueOf(e.getChildText("IsCellWithSignalsOnly"));
		isNormalised = Boolean.valueOf(e.getChildText("IsNormalised"));
		isBinarised = Boolean.valueOf(e.getChildText("IsBinarised"));
		
		threshold = Integer.valueOf(e.getChildText("Threshold"));
	}
	
	@Override
	public Element toXmlElement() {
		Element e = new Element("WarpedSignalKey");
		e.addContent(new Element("TargetShape").setContent(targetShape.toXmlElement()));
		
		e.addContent("TargetShapeId").setText(targetShapeId.toString());
		e.addContent("IsCellWithSignalsOnly").setText(String.valueOf(isCellWithSignalsOnly));
		e.addContent("Threshold").setText(String.valueOf(threshold));
		e.addContent("TemplateId").setText(templateId.toString());
		e.addContent("IsBinarised").setText(String.valueOf(isBinarised));
		e.addContent("IsNormalised").setText(String.valueOf(isNormalised));
		
		return e;
	} 
	
	public UUID getTemplateId() {
		return templateId;
	}

	public CellularComponent getTargetShape() {
		return targetShape;
	}

	public boolean isCellWithSignalsOnly() {
		return isCellWithSignalsOnly;
	}
	
	public int getThreshold() {
		return threshold;
	}
	
	public boolean isBinarised() {
		return isBinarised;
	}
	
	public boolean isNormalised() {
		return isNormalised;
	}
	
	@Override
	public String toString() {
		return "Hash: "+hashCode()
				+" "+templateId
				+" "+targetShapeId
				+" "+isCellWithSignalsOnly
				+" "+threshold
				+" "+isBinarised
				+" "+isNormalised;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isCellWithSignalsOnly ? 1231 : 1237);
		result = prime * result + ((targetShapeId == null) ? 0 : targetShapeId.hashCode());
		result = prime * result + threshold;
		result = prime * result + (isBinarised ? 1231 : 1237);
		result = prime * result + (isNormalised ? 1231 : 1237);
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
		WarpedSignalKey other = (WarpedSignalKey) obj;
		if (isCellWithSignalsOnly != other.isCellWithSignalsOnly)
			return false;
		if (isBinarised != other.isBinarised)
			return false;
		if (isNormalised != other.isNormalised)
			return false;
		if (targetShapeId == null) {
			if (other.targetShapeId != null)
				return false;
		} else if (!targetShapeId.equals(other.targetShapeId))
			return false;
		return true;
	}

}

