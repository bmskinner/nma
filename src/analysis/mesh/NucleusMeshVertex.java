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
	
	private int number; // the number in the mesh - use to compare vertexes between nuclei
	private XYPoint position; // the posiiton of the vertex
	private boolean peripheral; // is the vertex on the border of the object
	
	Set<NucleusMeshEdge> edges = new HashSet<NucleusMeshEdge>(); // store the edges attached to the vertex
	
	public NucleusMeshVertex(int i, XYPoint p, boolean peripheral){
//		this.number = i;
		this.position = p;
		this.peripheral = peripheral;
	}
	
	public boolean isPeripheral() {
		return peripheral;
	}

	public int getNumber() {
		return number;
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
			if( e.getV1().equals(v)  || e.getV2().equals(v)){
				return true;
			}
		}
		return false;
	}
	
	public double getLengthTo(NucleusMeshVertex v){
		return position.getLengthTo(v.getPosition());
	}
	
	public boolean overlaps(NucleusMeshVertex v){
		return position.overlapsPerfectly(v.position);
	}
	
	public String toString(){
		return position.toString();
	}
	
}
