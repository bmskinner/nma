package com.bmskinner.nuclear_morphology.components.nuclear;

import java.io.Serializable;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.CellularComponent;

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
	
	public WarpedSignalKey(@NonNull CellularComponent target, final boolean cellsWithSignals) {
		targetShape = target;
		targetShapeId = target.getID();
		isCellWithSignalsOnly = cellsWithSignals;
	}

	public CellularComponent getTargetShape() {
		return targetShape;
	}

	public boolean isCellWithSignalsOnly() {
		return isCellWithSignalsOnly;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isCellWithSignalsOnly ? 1231 : 1237);
		result = prime * result + ((targetShapeId == null) ? 0 : targetShapeId.hashCode());
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
		if (targetShapeId == null) {
			if (other.targetShapeId != null)
				return false;
		} else if (!targetShapeId.equals(other.targetShapeId))
			return false;
		return true;
	}

}

