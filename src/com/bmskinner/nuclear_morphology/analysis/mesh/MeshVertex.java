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

import java.util.Set;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

public interface MeshVertex {

	boolean isPeripheral();

	String getName();

	IPoint getPosition();

	void setPosition(IPoint p);

	void addEdge(MeshEdge e);

	void removeEdge(MeshEdge e);

	Set<MeshEdge> getEdges();

	boolean hasEdgeTo(MeshVertex v);

	/**
	 * Get the edge linking this vertex to the given vertex, if present.
	 * Otherwise returns null
	 * @param v
	 * @return
	 */
	MeshEdge getEdgeTo(MeshVertex v);

	double getLengthTo(MeshVertex v);

	boolean overlaps(MeshVertex v);

	int getNumber();

	int hashCode();

}