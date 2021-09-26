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
package com.bmskinner.nuclear_morphology.analysis.mesh;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;

import ij.process.ImageProcessor;

/**
 * A mesh image converts the pixels within an image to coordinates within each
 * face of a Mesh. It provides the mechanisms to convert a mesh back into an
 * image given a template Mesh
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface MeshImage<E extends CellularComponent> {

    /**
     * Draw the image in this object onto the coordinates in the given mesh
     * 
     * @param mesh the mesh to use to position pixels in cartesian space
     * @return an image processor with the image drawn according to the mesh
     * @throws UncomparableMeshImageException if the mesh does not match this MeshImage
     */
    ImageProcessor drawImage(@NonNull Mesh<E> mesh) throws UncomparableMeshImageException;

    /**
     * Get the pixels for the given face in the mesh
     * 
     * @param f the face
     * @return the pixels within the face
     */
    List<MeshPixel> getMeshPixels(@NonNull MeshFace f);
    
    
    /**
     * Calculate the fractional signal intensity within the given face
     * as a proportion of the total signal intensity within the image.
     * @param f the face to quantify.
     * @return
     */
    double quantifySignalProportion(@NonNull MeshFace f);

}
