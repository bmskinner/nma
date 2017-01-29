/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.analysis.mesh;

import java.util.HashSet;
import java.util.Set;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * A nucleus imaplementation of the MeshVertex
 * @author ben
 *
 */
public class NucleusMeshVertex implements MeshVertex {
	
	final private String name; // the number in the mesh - use to compare vertexes between nuclei
	final private IPoint position; // the posiiton of the vertex
	final private boolean peripheral; // is the vertex on the border of the object
	
	Set<MeshEdge> edges = new HashSet<MeshEdge>(); // store the edges attached to the vertex
	
	/**
	 * Create a vertex as a given position
	 * @param p the position of the vertex
	 * @param name the name of the vertex
	 * @param peripheral true if the vertex is on the periphery of the mesh, false otherwise
	 */
	public NucleusMeshVertex(IPoint p, String name, boolean peripheral){
		this.name = name;
		this.position = p;
		this.peripheral = peripheral;
	}
	
	/**
	 * Construct a duplicate of a vertex.
	 * @param v the vertex to duplicate
	 */
	public NucleusMeshVertex(MeshVertex v){
		this.name       = v.getName();
		this.position   = IPoint.makeNew(v.getPosition());
		this.peripheral = v.isPeripheral();
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex#isPeripheral()
	 */
	@Override
	public boolean isPeripheral() {
		return peripheral;
	}

	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex#getPosition()
	 */
	@Override
	public IPoint getPosition() {
		return position;
	}
	
//	/* (non-Javadoc)
//	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex#setPosition(com.bmskinner.nuclear_morphology.components.generic.IPoint)
//	 */
//	@Override
//	public void setPosition(IPoint p){
//		this.position = p;
//	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex#addEdge(com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMeshEdge)
	 */
	@Override
	public void addEdge(MeshEdge e){
		edges.add(e);
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex#removeEdge(com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge)
	 */
	@Override
	public void removeEdge(MeshEdge e){
		edges.remove(e);
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex#getEdges()
	 */
	@Override
	public Set<MeshEdge> getEdges(){
		return edges;
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex#hasEdgeTo(com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex)
	 */
	@Override
	public boolean hasEdgeTo(MeshVertex v){
		
		for(MeshEdge e : edges){
			if( e.getV1().equals(v)  || e.getV2().equals(v)) {
				return true;
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex#getEdgeTo(com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex)
	 */
	@Override
	public MeshEdge getEdgeTo(MeshVertex v){
		
		for(MeshEdge e : edges){
			if( e.getV1().equals(v)  || e.getV2().equals(v)){
				return e;
			}
		}
		return null;
	}
	
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex#getLengthTo(com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex)
	 */
	@Override
	public double getLengthTo(MeshVertex v){
		return position.getLengthTo(v.getPosition());
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex#overlaps(com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMeshVertex)
	 */
	@Override
	public boolean overlaps(MeshVertex v){
		return position.overlapsPerfectly(v.getPosition());
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex#getNumber()
	 */
	@Override
	public int getNumber(){
		String chars = this.name.substring(1); //, replacement)split("");
		return Integer.valueOf(chars);
	}
		
	public String toString(){
		return this.name+": "+position.toString()+" : "+peripheral;
	}

	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (peripheral ? 1231 : 1237);
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
		NucleusMeshVertex other = (NucleusMeshVertex) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (peripheral != other.peripheral)
			return false;
		
		// Don't worry about position - the name will allow equality
		// testing between meshes
//		if (position == null) {
//			if (other.position != null)
//				return false;
//		} else if (!position.equals(other.position))
//			return false;
		return true;
	}
	
	
	
}
