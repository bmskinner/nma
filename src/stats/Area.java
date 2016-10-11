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

package stats;

import java.awt.Shape;

import ij.gui.Roi;
import ij.process.FloatPolygon;

@SuppressWarnings("serial")
public class Area extends DescriptiveStatistic {
	
	public Area(Roi r){
		
		if( ! r.isArea()){
			throw new IllegalArgumentException("Roi is not an area");
		}
		
		value = calculatePolygonRoi(r);
		
	}
	
//	public Area(Shape s){
//
//		
//		
//	}
	
	private float calculatePolygonRoi(Roi r){
		
		
		FloatPolygon f = r.getFloatPolygon();
		
		float sum = 0;
		for (int i = 0; i < f.npoints -1; i++)
		{
		    sum = sum + f.xpoints[i]*f.ypoints[i+1] - f.ypoints[i]*f.xpoints[i+1];
		}
		
		return Math.abs( sum/2);		
	}

}
