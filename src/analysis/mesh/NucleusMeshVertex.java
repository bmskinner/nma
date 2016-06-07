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
package analysis.mesh;

import java.util.HashSet;
import java.util.Set;

import components.generic.XYPoint;

public class NucleusMeshVertex {
	
	private String name; // the number in the mesh - use to compare vertexes between nuclei
	private XYPoint position; // the posiiton of the vertex
	private boolean peripheral; // is the vertex on the border of the object
	
	Set<NucleusMeshEdge> edges = new HashSet<NucleusMeshEdge>(); // store the edges attached to the vertex
	
	public NucleusMeshVertex(XYPoint p, String name, boolean peripheral){
		this.name = name;
		this.position = p;
		this.peripheral = peripheral;
	}
	
	/**
	 * Duplicate the vertex.
	 * @param v
	 */
	public NucleusMeshVertex(NucleusMeshVertex v){
		this.name = v.name;
		this.position = new XYPoint(v.position);
		this.peripheral = v.peripheral;
	}
	
	public boolean isPeripheral() {
		return peripheral;
	}

	public String getName() {
		return name;
	}

	public XYPoint getPosition() {
		return position;
	}
	
	public void addEdge(NucleusMeshEdge e){
		edges.add(e);
	}
	
	public void removeEdge(NucleusMeshEdge e){
		edges.remove(e);
	}
	
	public Set<NucleusMeshEdge> getEdges(){
		return edges;
	}
	
	public boolean hasEdgeTo(NucleusMeshVertex v){
		
		for(NucleusMeshEdge e : edges){
			if( e.getV1().equals(v)  || e.getV2().equals(v)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Get the edge linking this vertex to the given vertex, if present.
	 * Otherwise returns null
	 * @param v
	 * @return
	 */
	public NucleusMeshEdge getEdgeTo(NucleusMeshVertex v){
		
		for(NucleusMeshEdge e : edges){
			if( e.getV1().equals(v)  || e.getV2().equals(v)){
				return e;
			}
		}
		return null;
	}
	
	
	public double getLengthTo(NucleusMeshVertex v){
		return position.getLengthTo(v.getPosition());
	}
	
	public boolean overlaps(NucleusMeshVertex v){
		return position.overlapsPerfectly(v.position);
	}
	
	public int getNumber(){
		String[] chars = this.name.split("");
		return Integer.valueOf(chars[1]);
	}
		
	public String toString(){
		return this.name+": "+position.toString();
	}

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
