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

 package stats;

 import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import logging.Loggable;
import utility.Utils;

 public class Stats implements Loggable {
	 
	 public static final double LOG2 = Math.log(2);
	 public static final String NULL_OR_EMPTY_ARRAY_ERROR = "The data array either is null or does not contain any data.";

 /*
    Calculate the <lowerPercent> quartile from a Double[] array
  */
  public static double quartile(double[] values, double lowerPercent) {

	  return quartile(Utils.getDoubleFromdouble(values), lowerPercent).doubleValue();

  }
  
  /**
   * Calculate the given quartile for an array of values
   * @param values
   * @param lowerPercent
   * @return
   */
  public static Number quartile(Number[] values, double lowerPercent) {
	  if (values == null || values.length == 0) {
		  throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
	  }

	  if(values.length==1){
		  return values[0];
	  }

	  // Rank order the values
	  Number[] v = new Number[values.length];
	  System.arraycopy(values, 0, v, 0, values.length);
	  Arrays.sort(v);

	  int n = (int) Math.round(v.length * lowerPercent / 100);

	  return v[n];
  }

  /**
   * Calculate the mean of an array of values
   * @param m
   * @return
   */
  public static double mean(double[] m) {
	  return mean(Utils.getDoubleFromdouble(m)).doubleValue();
  }
  
  /**
   * Calculate the mean of an array of values
   * @param m
   * @return
   */
  public static Number mean(Number[] m) {
    if (m == null || m.length == 0) {
        throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
    }
    
    if(m.length==1){
    	return m[0];
    }
    
    double sum = 0;
    for(Number d : m){
    	sum += d.doubleValue();
    }
    return sum / m.length;
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

	  double mean = mean(m);
	  double temp = 0;
	  for(double d : m)
		  temp += Math.pow(mean-d, 2);
	  return temp/m.length;
  }

  /**
   * Calculate the confidence interval about the data mean for
   * the given confidence level
   * @param data the array of data points
   * @param level the confidence level (0-1)
   * @return the lower [0] and upper [1] CI about the mean.
   */
  public static double[] calculateMeanConfidenceInterval(double[] data, double level){
	  

	  // Calculate 95% confidence interval
	  double ci = calculateConfidenceIntervalSize(data, level);
	  double mean = Stats.mean(data);

	  double lower = mean - ci;
	  double upper = mean + ci;
	  double[] result = { lower, upper };
	  return result;
  }

  /**
   * Calculate the confidence interval about the mean using a T-distribution
   * @param stats
   * @param level
   * @return
   */
  public static double calculateConfidenceIntervalSize(double[] data, double level) {
	  
	  if(data.length<2){
		  return 0;
	  }
	  try {
		  
		  SummaryStatistics stats = new SummaryStatistics();
		  for (double val : data) {
			  stats.addValue(val);
		  }
		  
		  // Create T Distribution with N-1 degrees of freedom
		  TDistribution tDist = new TDistribution(stats.getN() - 1);
		  // Calculate critical value
		  double critVal = tDist.inverseCumulativeProbability(1.0 - (1 - level) / 2);
		  // Calculate confidence interval
		  return critVal * stats.getStandardDeviation() / Math.sqrt(stats.getN());
	  } catch (MathIllegalArgumentException e) {
		  return Double.NaN;
	  }
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
   * Get the minimum Number in the list
   * @param values
   * @return
   */
  public static Number min(List<Number> values){
	  Number result = Double.MAX_VALUE; 
	  for(Number n : values){
		  if(n.doubleValue()<result.doubleValue()){
			  result=n;
		  }
	  }
	  return result;
  }
  
  /**
   * Get the maximum Number in the list
   * @param values
   * @return
   */
  public static Number max(List<Number> values){

	  Number result = Double.MIN_VALUE; 
	  for(Number n : values){
		  if(n.doubleValue()>result.doubleValue()){
			  result=n;
		  }
	  }
	  return result;
  }
  
  public static Number quartile(List<Number> values, double lowerPercent) {
	  
	  Number[] array = values.toArray(new Number[0]);
	  Number result = quartile(array, lowerPercent);
	  return result;
  }
  
  public static Number mean(List<Number> values) {
	  
	  Number[] array = values.toArray(new Number[0]);
	  Number result = mean(array);
	  return result;
  }
  
  public static Number sum(List<Number> values){
	  double result = 0; 
	  for(Number n : values){
		  result += n.doubleValue();
	  }
	  return result;
  }

  
 }
  