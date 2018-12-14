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

import java.util.HashSet;
import java.util.Set;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * A nucleus imaplementation of the MeshVertex
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultMeshVertex implements MeshVertex {

    /** The number in the mesh - use to compare vertices between nuclei  */
    final private String  name;
    
    /**  The position of the vertex */
    final private IPoint position;
    
    /** True if the vertex lies on the periphery of the object */
    final private boolean isPeripheral;

    /** Strong reference to the edges attached to the vertex */
    final private Set<MeshEdge> edges = new HashSet<>();

    /**
     * Create a vertex as a given position
     * 
     * @param p the position of the vertex
     * @param name the name of the vertex
     * @param peripheral true if the vertex is on the periphery of the mesh, false
     *            otherwise
     */
    public DefaultMeshVertex(IPoint p, String name, boolean peripheral) {
        this.name = name;
        this.position = p;
        this.isPeripheral = peripheral;
    }

    /**
     * Construct a duplicate of a vertex.
     * 
     * @param v the vertex to duplicate
     */
    public DefaultMeshVertex(MeshVertex v) {
        this.name = v.getName();
        this.position = IPoint.makeNew(v.getPosition());
        this.isPeripheral = v.isPeripheral();
    }


    @Override
    public boolean isPeripheral() {
        return isPeripheral;
    }
    
    @Override
    public boolean isInternal() {
        return !isPeripheral();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IPoint getPosition() {
        return position;
    }


    @Override
    public void addEdge(MeshEdge e) {
        edges.add(e);
    }

    @Override
    public void removeEdge(MeshEdge e) {
        edges.remove(e);
    }


    @Override
    public Set<MeshEdge> getEdges() {
        return edges;
    }

    @Override
    public boolean hasEdgeTo(MeshVertex v) {

        for (MeshEdge e : edges) {
            if (e.getV1().equals(v) || e.getV2().equals(v)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public MeshEdge getEdgeTo(MeshVertex v) {

        for (MeshEdge e : edges) {
            if (e.getV1().equals(v) || e.getV2().equals(v)) {
                return e;
            }
        }
        return null;
    }

    @Override
    public double getLengthTo(MeshVertex v) {
        return position.getLengthTo(v.getPosition());
    }

    @Override
    public boolean overlaps(MeshVertex v) {
        return position.overlapsPerfectly(v.getPosition());
    }

    @Override
    public int getNumber() {
        String chars = this.name.substring(1); // , replacement)split("");
        return Integer.valueOf(chars);
    }

    public String toString() {
        return this.name + ": " + position.toString() + " : " + isPeripheral;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (isPeripheral ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultMeshVertex other = (DefaultMeshVertex) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (isPeripheral != other.isPeripheral)
            return false;

        // Don't worry about position - the name will allow equality
        // testing between meshes
        // if (position == null) {
        // if (other.position != null)
        // return false;
        // } else if (!position.equals(other.position))
        // return false;
        return true;
    }

}
