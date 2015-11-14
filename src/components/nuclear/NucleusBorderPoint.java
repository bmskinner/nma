/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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


package components.nuclear;

/**
 *  This class contains border points around the periphery of a nucleus.
 *	Mostly the same as an XYPoint now, after creation of Profiles. It does
 * allow linkage of points, but this is not yet used
 *
 */
public class NucleusBorderPoint
	extends components.generic.XYPoint {

	private static final long serialVersionUID = 1L;
	
	
	private NucleusBorderPoint prevPoint = null;
	private NucleusBorderPoint nextPoint = null;
	
	public NucleusBorderPoint( double x, double y){
		super(x, y);
	}

	public NucleusBorderPoint( NucleusBorderPoint p){
		super(p.getX(), p.getY());
	}
	
	public void setNextPoint(NucleusBorderPoint next){
		this.nextPoint = next;
	}
	
	public void setPrevPoint(NucleusBorderPoint prev){
		this.prevPoint = prev;
	}
	
	public NucleusBorderPoint nextPoint(){
		return  this.nextPoint;
	}
	
	public NucleusBorderPoint prevPoint(){
		return  this.prevPoint;
	}
	
	public boolean hasNextPoint(){
		if(this.nextPoint()!=null){
			return  true;
		} else {
			return  false;
		}
	}
	
	public boolean hasPrevPoint(){
		if(this.prevPoint()!=null){
			return  true;
		} else {
			return  false;
		}
	}


}