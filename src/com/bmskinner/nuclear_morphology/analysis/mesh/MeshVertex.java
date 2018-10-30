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

import java.util.Set;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

public interface MeshVertex {

    /**
     * Test if the vertex is at the periphery of the mesh
     * @return
     */
    boolean isPeripheral();

    String getName();

    /**
     * Get the position of the vertex in cartesian coordinates
     * @return
     */
    IPoint getPosition();

    /**
     * Add the given edge to the vertex
     * @param e
     */
    void addEdge(MeshEdge e);

    /**
     * Remove the given edge to the vertex
     * @param e
     */
    void removeEdge(MeshEdge e);

    /**
     * Get the edges attached to the vertex
     * @return
     */
    Set<MeshEdge> getEdges();

    /**
     * Test if there is an edge from this vertex to the given vertex
     * @param v
     * @return
     */
    boolean hasEdgeTo(MeshVertex v);

    /**
     * Get the edge linking this vertex to the given vertex, if present.
     * Otherwise returns null
     * 
     * @param v
     * @return
     */
    MeshEdge getEdgeTo(MeshVertex v);

    /**
     * Get the cartesian distance from this vertex to the given vertex
     * @param v
     * @return
     */
    double getLengthTo(MeshVertex v);

    /**
     * Test if this vertex and the given vertex have the same cartesian coordinates
     * @param v
     * @return
     */
    boolean overlaps(MeshVertex v);

    /**
     * Get the number of the vertex within its parent mesh
     * @return
     */
    int getNumber();
}
