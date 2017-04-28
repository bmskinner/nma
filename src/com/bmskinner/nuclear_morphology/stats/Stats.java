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
/* 
  -----------------------
  STATS FUNCTIONS
  -----------------------
  This class contains basic functions for 
  nuclear organisation analysis used across
  other classes
 */

package com.bmskinner.nuclear_morphology.stats;

import java.util.stream.DoubleStream;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;

import com.bmskinner.nuclear_morphology.logging.Loggable;

public class Stats implements Loggable {

	public static final double LOG2 = Math.log(2);
	public static final String NULL_OR_EMPTY_ARRAY_ERROR = "The data array either is null or does not contain any data.";

	static double max(double[] array){
		return DoubleStream.of(array).max().orElse(0);
	}

	static double min(double[] array){
		return DoubleStream.of(array).min().orElse(0);
	}

	/**
	 * Calculate the standard error of an array of values
	 * @param m
	 * @return
	 */
	public static double stderr(double[] m){
		if (m == null || m.length == 0) {
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
		}

		if(m.length<2){
			return 0;
		}

		return stdev(m)/Math.sqrt(m.length);
	}

	/**
	 * Calculate the standard deviation of an array of values
	 * @param m
	 * @return
	 */
	public static double stdev(double[] m){
		if (m == null || m.length == 0) {
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
		}

		if(m.length<2){
			return 0;
		}
		return Math.sqrt(variance(m));
	}

	/**
	 * Calculate the variance of an array of values
	 * @param m
	 * @return
	 */
	public static double variance(double[] m){
		if (m == null || m.length == 0) {
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
		}

		if(m.length<2){
			return 0;
		}
		double mean = DoubleStream.of(m).average().orElse(0);
		double temp = 0;
		for(double d : m)
			temp += Math.pow(mean-d, 2);
		return temp/m.length;
	}

	public static double calculateLog2Ratio(double d){
		return Math.log(d)/LOG2;
	}

	/**
	 * Calculate the Spearman's rank  correlation coefficient between
	 * two arrays of paired values
	 * @param x
	 * @param y
	 * @return
	 */
	public static double getSpearmansCorrelation(double[] x, double[] y){

		if (x == null || x.length == 0) {
			throw new IllegalArgumentException("x:"+NULL_OR_EMPTY_ARRAY_ERROR);
		}

		if (y == null || y.length == 0) {
			throw new IllegalArgumentException("y:"+NULL_OR_EMPTY_ARRAY_ERROR);
		}

		if(x.length != y.length){
			throw new IllegalArgumentException("Input arrays have different lengths"); 
		}

		if(x.length==1){
			return 0;
		}
		SpearmansCorrelation sp = new SpearmansCorrelation();
		return sp.correlation(x, y);

	}

	public static double getSpearmanPValue(double r, double n){
		double t = getTStatistic(r, n);
		TDistribution tDist = new TDistribution(n - 1);
		return tDist.probability(t);
	} 

	/**
	 * Get the t-statistic for a Spearman's rho at a given sample size
	 * @param r
	 * @param n
	 * @return
	 */
	public static double getTStatistic(double r, double n){
		// t =  r * sqrt( n-2 / 1-r^2   )

		double t = r * Math.sqrt(  ( (n-2) / (1- (r*r)) )  );

		return t;

	}

	/**
	 * Run a Wilcoxon test on the given arrays. 
	 * @param values0
	 * @param values1
	 * @param isGetPValue
	 * @return
	 */
	public static double runWilcoxonTest(double[] values0, double[] values1, boolean isGetPValue){

		double result = 0;
		MannWhitneyUTest test = new MannWhitneyUTest(); // default, NaN's are left in place and ties get the average of applicable ranks


		if(isGetPValue){ // above diagonal, p-value
			result = test.mannWhitneyUTest(values0, values1); // correct for the number of datasets tested

		} else { // below diagonal, U statistic
			result = test.mannWhitneyU(values0, values1);
		}
		return result;
	}


}
