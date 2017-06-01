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

import java.awt.geom.Path2D;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * 
 * The mesh should allow comparisons of equivalent points between different
 * components.
 * 
 * The requirement is to: 1) consistently identify points around the periphery
 * of the components 2) Translate those points to another nucleus.
 * 
 * The points are identified based on proportion through segments. We can be
 * reasonably confident that segment boundaries are at equivalent biological
 * features. Each segment is divided into points, separated by about 10 pixels.
 * 
 * These points around the periphery of the component are used to build a
 * skeleton for the object. The skeleton travels from the reference point
 * through the centre of the nucleus.
 * 
 * Edges are constructed between the peripheral vertices and their corresponding
 * skeleton vertices, making a triangular mesh.
 * 
 * All vertices can be located in another components using segment proportions.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface Mesh<E extends CellularComponent> extends Comparable<Mesh<E>> {

    public static final int DEFAULT_VERTEX_SPACING = 10;

    E getComponent();

    String getComponentName();

    /**
     * Test if this mesh contains a vertex with the same position
     * 
     * @param v
     *            the vertex to test
     * @return
     */
    boolean contains(MeshVertex v);

    /**
     * Test if this mesh contains a face with the same vertex positions
     * 
     * @param test
     * @return
     */
    boolean contains(MeshFace test);

    /**
     * Test if this mesh contains an edge with the same vertex positions
     * 
     * @param e
     *            the edge to test
     * @return
     */
    boolean contains(MeshEdge e);

    /**
     * Test if the mesh contains the given point within one of its faces
     * 
     * @param p
     * @return
     */
    boolean contains(IPoint p);

    /**
     * Get the face containing the given point, or null if there is no face with
     * the point
     * 
     * @param p
     *            the point to test
     * @return the face with the point or null
     */
    MeshFace getFace(IPoint p);

    /**
     * Get the number of segments used to construct the mesh
     * 
     * @return
     */
    int getSegmentCount();

    /**
     * Get the index spacing between vertices
     * 
     * @return
     */
    int getVertexSpacing();

    Map<Integer, List<Double>> getVertexProportions();

    /**
     * The total number of vertices, internal and peripheral
     * 
     * @return
     */
    int getVertexCount();

    int getInternalVertexCount();

    int getPeripheralVertexCount();

    int getEdgeCount();

    int getFaceCount();

    List<MeshVertex> getPeripheralVertices();

    List<MeshVertex> getInternalVertices();

    Set<MeshEdge> getEdges();

    Set<MeshFace> getFaces();

    boolean isComparableTo(Mesh<E> mesh);

    /**
     * Find the edge and face ratios of this mesh versus the given mesh. Meshes
     * must have the same number of vertices, edges and faces.
     * 
     * @param mesh
     * @return
     */
    Mesh<E> comparison(Mesh<E> mesh);

    /**
     * Reposition the vertices such that the internal skeleton vertices form a
     * vertical line, equally spaced.
     * 
     * @return
     */
    Mesh<E> straighten();

    /**
     * Get the face within this mesh described by the given face
     * 
     * @param f
     *            the face to find
     * @return the face in this mesh equivalent to the input face
     */
    MeshFace getFace(MeshFace f);

    /**
     * Get the edge within this mesh described by the given edge
     * 
     * @param e
     *            the edge to find
     * @return the edge in this mesh equivalent to the input edge
     */
    MeshEdge getEdge(MeshEdge e);

    /**
     * Get a closed path comprising the peripheral points of the mesh
     * 
     * @return
     */
    Path2D toPath();

}