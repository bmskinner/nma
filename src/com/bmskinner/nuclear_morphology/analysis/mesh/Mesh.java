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
import java.util.Set;

public interface Mesh extends Comparable<Mesh>{

	public static final int DEFAULT_VERTEX_SPACING = 10;

	String getNucleusName();

	boolean contains(NucleusMeshVertex v);

	/**
	 * Test if this mesh contains a face with the same vertex positions
	 * @param test
	 * @return
	 */
	boolean contains(NucleusMeshFace test);

	boolean contains(NucleusMeshEdge e);

	int getSegmentCount();

	int getVertexSpacing();

	/**
	 * The total number of vertices, internal and peripheral
	 * @return
	 */
	int getVertexCount();

	int getInternalVertexCount();

	int getPeripheralVertexCount();

	int getEdgeCount();

	int getFaceCount();

	List<NucleusMeshVertex> getPeripheralVertices();

	List<NucleusMeshVertex> getInternalVertices();

	Set<NucleusMeshEdge> getEdges();

	Set<NucleusMeshFace> getFaces();

	boolean isComparableTo(Mesh mesh);

	/**
	 * Find the edge and face ratios of this mesh versus the given mesh.
	 * Meshes must have the same number of vertices,  edges and faces. 
	 * @param mesh
	 * @return
	 */
	Mesh compareTo(NucleusMesh mesh);

	/**
	 * Reposition the vertices such that the internal
	 * skeleton vertices form a vertical line, equally
	 * spaced.
	 * @return
	 */
	Mesh straighten();

	/**
	 * Get a closed path comprising the peripheral points of the mesh 
	 * @return
	 */
	Path2D toPath();

}