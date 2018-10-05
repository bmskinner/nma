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

import java.awt.geom.Path2D;
import java.util.Set;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * A face is composed of three vertices linked by three edges.
 * @author bms41
 * @since 1.13.3
 *
 */
/**
 * @author bms41
 *
 */
/**
 * @author bms41
 *
 */
public interface MeshFace {

    /**
     * Get the value stored with this face
     * 
     * @return
     */
    double getValue();

    /**
     * Get the log2 ratio of the value stored in the face
     * 
     * @return
     */
    double getLog2Ratio();

    /**
     * Store a value in this face
     * 
     * @param value
     */
    void setValue(double value);

    Set<MeshEdge> getEdges();

    Set<MeshVertex> getVertices();

    boolean contains(MeshEdge e);

    boolean contains(MeshVertex v);

    /**
     * Get the area of the face
     * 
     * @return
     */
    double getArea();

    String getName();

    IPoint getMidpoint();

    /**
     * Test if the given point is within the face
     * 
     * @param p
     * @return
     */
    boolean contains(IPoint p);

    /**
     * Generate a closed path for the face
     * 
     * @return
     */
    Path2D toPath();

    /**
     * Given a point within the face, get the face coordinate
     * 
     * @param p
     * @return
     */
    MeshFaceCoordinate getFaceCoordinate(IPoint p) throws PixelOutOfBoundsException;

    /**
     * Count the number of vertices in the face that are peripheral
     * 
     * @return
     */
    int getPeripheralVertexCount();

    /**
     * Count the number of vertices in the face that are internal
     * 
     * @return
     */
    int getInternalVertexCount();

    /**
     * Get the peripheral vertex with the lower vertex number
     * 
     * @return
     */
    MeshVertex getLowerPeripheralVertex();

    /**
     * Get the peripheral vertex with the higher vertex number
     * 
     * @return
     */
    MeshVertex getHigherPeripheralVertex();

    /**
     * Get the internal vertex with the lower vertex number
     * 
     * @return
     */
    MeshVertex getLowerInternalVertex();

    /**
     * Get the internal vertex with the higher vertex number
     * 
     * @return
     */
    MeshVertex getHigherInternalVertex();

    /**
     * Check that v1 is the internal vertex, or the lower perpheral vertex. If
     * not, return new new edge with reversed orientation
     * 
     * @param e
     *            the edge to test
     * @return
     */
    MeshEdge correctEdgeOrientation(MeshEdge e);

}
