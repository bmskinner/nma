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
	private double value;
	
	public NucleusMeshEdge(NucleusMeshVertex v1, NucleusMeshVertex v2, double ratio){
		this.v1 = v1;
		this.v2 = v2;
		this.value = ratio;
	}
	
	/**
	 * Duplicate the edge
	 * @param e
	 */
	public NucleusMeshEdge(NucleusMeshEdge e){
		this.v1 = new NucleusMeshVertex(e.v1);
		this.v2 = new NucleusMeshVertex(e.v2);

		this.value = e.value;
	}

	public NucleusMeshVertex getV1() {
		return v1;
	}

	public NucleusMeshVertex getV2() {
		return v2;
	}
	
	public void setValue(double d){
		this.value = d;
	}

	public double getRatio() {
		return value;
	}
	
	public double getLog2Ratio(){
		return Stats.calculateLog2Ratio(value);
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
	
	public boolean equals(NucleusMeshEdge e){
		if(this==e){
			return true;
		}
		
		return this.overlaps(e);
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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((v1 == null) ? 0 : v1.hashCode());
		result = prime * result + ((v2 == null) ? 0 : v2.hashCode());
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
		NucleusMeshEdge other = (NucleusMeshEdge) obj;
		if (v1 == null) {
			if (other.v1 != null)
				return false;
		} else if (!v1.equals(other.v1) || !v1.equals(other.v2))
			return false;
		if (v2 == null) {
			if (other.v2 != null)
				return false;
		} else if (!v2.equals(other.v2) || !v2.equals(other.v1))
			return false;
		return true;
	}

	public String getName(){
		return v1.getName()+" - "+v2.getName();
	}
					
	public String toString(){
		return v1.getName()+" - "+v2.getName()+" : "+getLength();
	}

}
