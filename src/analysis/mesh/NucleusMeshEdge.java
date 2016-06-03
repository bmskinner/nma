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

import java.awt.geom.Line2D;

import stats.Stats;

import components.generic.Equation;
import components.generic.XYPoint;

public class NucleusMeshEdge {
	private NucleusMeshVertex v1;
	private NucleusMeshVertex v2;
	private double ratio;
	
	public NucleusMeshEdge(NucleusMeshVertex v1, NucleusMeshVertex v2, double ratio){
		this.v1 = v1;
		this.v2 = v2;
		this.ratio = ratio;
	}

	public NucleusMeshVertex getV1() {
		return v1;
	}

	public NucleusMeshVertex getV2() {
		return v2;
	}

	public double getRatio() {
		return ratio;
	}
	
	public double getLog2Ratio(){
		return Stats.calculateLog2Ratio(ratio);
	}
	
	public double getLength(){
		return v1.getLengthTo(v2);
	}
	
	public XYPoint getMidpoint(){
		Equation eq = new Equation(v1.getPosition(), v2.getPosition());
		if(v1.getPosition().getX()<v2.getPosition().getX()){
			return eq.getPointOnLine(v1.getPosition(), getLength()/2);
		} else {
			return eq.getPointOnLine(v1.getPosition(), -(getLength()/2));
		}
		
	}
	
	/**
	 * Test if both vertices lie on the periphery of the mesh, and are adjacent
	 * in the vertex list. TODO: end of list wrapping
	 * @return
	 */
	public boolean isPeripheral(){
		
		if(v1.isPeripheral() && v2.isPeripheral()){
			if(  Math.abs(v1.getNumber() - v2.getNumber())==1 ){
				return true;
			}
		}
		return false;
	}
	
	public boolean isLongerThan(NucleusMeshEdge e){
		return getLength() > e.getLength();
	}
	
	/**
	 * Test if the edges share both endpoints
	 * @param e
	 * @return
	 */
	public boolean overlaps(NucleusMeshEdge e){
		return this.containsVertex(e.v1) && this.containsVertex(e.v2);			
	}
	
	public boolean crosses(NucleusMeshEdge e){
		
		Line2D line1 = new Line2D.Double(v1.getPosition().asPoint(), v2.getPosition().asPoint());
		Line2D line2 = new Line2D.Double(e.v1.getPosition().asPoint(), e.v2.getPosition().asPoint());

		if(line1.intersectsLine(line2)){
			

			if(sharesEndpoint(e)){
				return false;
			} else {
				return true;
			}
		} 
		return false;
	}
	
	/**
	 * Check if any of the endpoints of the edges are shared
	 * @param e
	 * @return
	 */
	public boolean sharesEndpoint(NucleusMeshEdge e){
		return this.containsVertex(e.v1) || this.containsVertex(e.v2);			
	}
	
	public boolean containsVertex(NucleusMeshVertex v){
		return v1.overlaps(v)  || v2.overlaps(v);
	}
	
	/**
	 * Compare the length of this edge to the given edge, and return
	 * a new edge with the ratio
	 * @param e
	 * @return
	 */
	public NucleusMeshEdge compare(NucleusMeshEdge e){
		
		double thisDistance = getLength();
		double thatDistance = e.getLength();
		
		double ratio = thisDistance / thatDistance ;
		return new NucleusMeshEdge(v1, v2, ratio);
		
	}
					
	public String toString(){
		return v1.toString()+" : "+v2.toString()+" : "+getLength();
	}

}
