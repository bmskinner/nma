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

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;

/**
 * Key for mapping templates to images
 * @author ben
 * @since 1.14.0
 *
 */
public class WarpedSignalKey implements Serializable {

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

