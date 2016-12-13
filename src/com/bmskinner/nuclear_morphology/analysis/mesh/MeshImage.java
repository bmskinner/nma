/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.analysis.mesh;

import ij.process.ImageProcessor;

/**
 * A mesh image converts the pixels within an image
 * to coordinates within each face of a Mesh. It provides
 * the mechanisms to convert a mesh back into an image
 * @author bms41
 * @since 1.13.3
 *
 */
public interface MeshImage<CellularComponent> {

	/**
	 * Draw the image in this object at the coordinates in the given mesh
	 * @param mesh the mesh to use to position pixels in cartesian space
	 * @return
	 * @throws UncomparableMeshImageException if the mesh does not match this MeshImage
	 */
	ImageProcessor createImage(Mesh<CellularComponent> mesh) throws UncomparableMeshImageException;

}