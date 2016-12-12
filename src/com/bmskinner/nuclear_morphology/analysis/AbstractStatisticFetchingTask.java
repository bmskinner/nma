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
package com.bmskinner.nuclear_morphology.analysis;

import java.util.concurrent.RecursiveTask;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.PlottableStatistic;

@SuppressWarnings("serial")
public abstract class AbstractStatisticFetchingTask extends RecursiveTask<double[]> implements Loggable{
	
	protected final int low, high;
	protected final Nucleus[] nuclei;
	public static final int THRESHOLD = 200;
	protected final PlottableStatistic stat;
	protected final MeasurementScale scale;
	
	public AbstractStatisticFetchingTask(Nucleus[] nuclei, PlottableStatistic stat, MeasurementScale scale){
		this(nuclei, stat, scale, 0,nuclei.length );
	}
	
	protected AbstractStatisticFetchingTask(Nucleus[] nuclei, PlottableStatistic stat, MeasurementScale scale, int low, int high) {
		this.low    = low;
		this.high   = high;
		this.nuclei = nuclei;
		this.stat   = stat;
		this.scale  = scale;
	}
	
	public double[] concat(double[] a, double[] b) {
		   int aLen = a.length;
		   int bLen = b.length;
		   double[] c= new double[aLen+bLen];
		   System.arraycopy(a, 0, c, 0, aLen);
		   System.arraycopy(b, 0, c, aLen, bLen);
		   return c;
		}

}
