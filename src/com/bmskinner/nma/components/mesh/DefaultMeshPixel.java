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
package com.bmskinner.nma.components.mesh;

/**
 * A default implemnetation of the MeshPixel
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public class DefaultMeshPixel implements MeshPixel {

	final int value;
	final MeshFaceCoordinate coordinate;

	public DefaultMeshPixel(final MeshFaceCoordinate c, final int v) {

		if (v < 0) {
			throw new IllegalArgumentException("Pixel value is below zero");
		}
		value = v;
		coordinate = c;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public MeshFaceCoordinate getCoordinate() {
		return coordinate;
	}

	@Override
	public String toString() {
		return coordinate.toString() + " - " + value;
	}

}
