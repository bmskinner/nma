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
package com.bmskinner.nuclear_morphology.utility;

import ij.measure.CurveFitter;

// carry out cubic interpolations on curves
public class Interpolator {
	
//	private String formula;
	private double[] parameters;
		
	public Interpolator(double[] xvalues, double[] yvalues) {
		
		if(xvalues==null || yvalues == null){
			throw new IllegalArgumentException("Arrays are null or empty");
		}
		if(xvalues.length!=yvalues.length){
			throw new IllegalArgumentException("Array lengths do not match");
		}
		CurveFitter fitter = new CurveFitter(xvalues, yvalues);
		fitter.doFit(CurveFitter.POLY3);
//		this.formula = fitter.getFormula();
		this.parameters = fitter.getParams();
	}

	public double find(double xToFind){
		
		double y = 0;
		
		for(int i=0;i<parameters.length;i++){
			double param = parameters[i];
			y+= Math.pow(xToFind, i) * param;
		}
		return y;
	}

}
