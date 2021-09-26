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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;

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

    int DEFAULT_VERTEX_SPACING = 10;

    E getComponent();

    String getComponentName();

    /**
     * Test if this mesh contains a vertex with the same position
     * 
     * @param v
     *            the vertex to test
     * @return
     */
    boolean contains(@NonNull MeshVertex v);

    /**
     * Test if this mesh contains a face with the same vertex positions
     * 
     * @param test
     * @return
     */
    boolean contains(@NonNull MeshFace test);

    /**
     * Test if this mesh contains an edge with the same vertex positions
     * 
     * @param e
     *            the edge to test
     * @return
     */
    boolean contains(@NonNull MeshEdge e);

    /**
     * Test if the mesh contains the given point within one of its faces
     * 
     * @param p
     * @return
     */
    boolean contains(@NonNull IPoint p);

    /**
     * Get the face containing the given cartesian point, or null if there is no face with
     * the point
     * 
     * @param p the point
     * @return the face containing the point or null
     */
    MeshFace getFace(@NonNull IPoint p);

    /**
     * Get the index spacing between vertices
     * 
     * @return
     */
    int getVertexSpacing();

    /**
     * Get the proportions of each peripheral vertex along each segment
     * @return
     */
    Map<Integer, List<Double>> getVertexProportions();

    /**
     * The total number of vertices, internal and peripheral
     * 
     * @return
     */
    int getVertexCount();

    /**
     * The total number of internal vertices
     * 
     * @return
     */
    int getInternalVertexCount();

    /**
     * The total number of peripheral vertices
     * 
     * @return
     */
    int getPeripheralVertexCount();

    /**
     * The total number of edges
     * 
     * @return
     */
    int getEdgeCount();

    /**
     * The total number of faces
     * 
     * @return
     */
    int getFaceCount();

    /**
     * Get the peripheral vertices
     * 
     * @return
     */
    List<MeshVertex> getPeripheralVertices();

    /**
     * Get the internal vertices
     * 
     * @return
     */
    List<MeshVertex> getInternalVertices();

    /**
     * Get the edges
     * 
     * @return
     */
    Set<MeshEdge> getEdges();

    /**
     * Get the faces
     * 
     * @return
     */
    Set<MeshFace> getFaces();
    
    /**
     * Get the faces whose periperal vertices are within the given segment
     * @param seg the segment
     * @return
     */
    Set<MeshFace> getFaces(IProfileSegment seg);

    /**
     * Test if the given mesh can be compared to this mesh. That is,
     * does the mesh have the same number of vertices and segmentation
     * pattern?
     * 
     * @param mesh the mesh to test
     * @return true if the mesh can be compared to this mesh, false otherwise
     */
    boolean isComparableTo(@NonNull Mesh<E> mesh);

    /**
     * Find the edge and face ratios of this mesh versus the given mesh. Meshes
     * must have the same number of vertices, edges and faces.
     * 
     * @param mesh
     * @return
     */
    Mesh<E> comparison(@NonNull Mesh<E> mesh);
    
    /**
     * Find the edge and face ratios of this mesh versus the mesh fit to the target object. 
     * 
     * @param target the object to create a mesh for and compare this mesh to
     * @return
     * @throws MeshCreationException if the mesh cannot be created
     */
    Mesh<E> comparison(@NonNull E target) throws MeshCreationException;

    /**
     * Reposition the vertices such that the internal skeleton vertices form a
     * vertical line, equally spaced.
     * 
     * @return
     */
//    Mesh<E> straighten();

    /**
     * Get the face within this mesh described by the given face
     * 
     * @param f the face to find
     * @return the face in this mesh equivalent to the input face
     */
    MeshFace getFace(@NonNull MeshFace f);

    /**
     * Get the edge within this mesh described by the given edge
     * 
     * @param e the edge to find
     * @return the edge in this mesh equivalent to the input edge
     */
    MeshEdge getEdge(@NonNull MeshEdge e);
    
    
    double getMaxEdgeRatio();

    /**
     * Get a closed path comprising the peripheral points of the mesh
     * 
     * @return
     */
    Path2D toPath();

}
