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
package com.bmskinner.nma.components.cells;

import java.io.File;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.generic.IPoint;

import ij.gui.Roi;

/**
 * The default implementation of ICytoplasm.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public class DefaultCytoplasm extends DefaultCellularComponent implements ICytoplasm {

	public DefaultCytoplasm(@NonNull Roi roi, @NonNull IPoint centreOfMass, File f, int channel,
			@NonNull UUID id) {
		super(roi, centreOfMass, f, channel, id);
	}

	/**
	 * Construct with an ROI, a source image and channel, and the original position
	 * in the source image
	 * 
	 * @param roi
	 * @param f
	 * @param channel
	 * @param position
	 * @param centreOfMass
	 */
	public DefaultCytoplasm(@NonNull Roi roi, @NonNull IPoint centreOfMass, File f, int channel) {
		super(roi, centreOfMass, f, channel);
	}

	protected DefaultCytoplasm(ICytoplasm n) {
		super(n);
	}

	@Override
	public ICytoplasm duplicate() {
		return new DefaultCytoplasm(this);
	}

	@Override
	public int compareTo(ICytoplasm o) {
		return 0;
	}

}
