/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.analysis.mesh;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

public interface MeshEdge {

    MeshVertex getV1();

    MeshVertex getV2();

    MeshEdge reverse();

    void setValue(double d);

    /**
     * Get the value stored within this edge.
     * 
     * @return the value
     */
    double getValue();

    double getLog2Ratio();

    double getLength();

    IPoint getMidpoint();

    boolean isLongerThan(MeshEdge e);

    /**
     * Test if the edges share both endpoints
     * 
     * @param e
     * @return
     */
    boolean overlaps(MeshEdge e);

    boolean crosses(MeshEdge e);

    /**
     * Check if any of the endpoints of the edges are shared
     * 
     * @param e
     * @return
     */
    boolean sharesEndpoint(MeshEdge e);

    boolean containsVertex(MeshVertex v);

    boolean equals(MeshEdge e);

    /**
     * Get the point a given fraction of the way along the edge (starting at v1)
     * 
     * @param d
     * @return
     */
    IPoint getProportionalPosition(double d);

    /**
     * If the point lies on the edge, get the proportional distance along the
     * edge from v1. Otherwise return 0
     * 
     * @param p
     * @return
     */
    double getPositionProportion(IPoint p);

    int hashCode();

    boolean equals(Object obj);

    String getName();

}
