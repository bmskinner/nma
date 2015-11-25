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
/* 
  -----------------------
  STATS FUNCTIONS
  -----------------------
  This class contains basic functions for 
  nuclear organisation analysis used across
  other classes
*/

 package utility;

 import java.util.Arrays;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

 public class Stats {

 /*
    Calculate the <lowerPercent> quartile from a Double[] array
  */
  public static double quartile(double[] values, double lowerPercent) {

      if (values == null || values.length == 0) {
          throw new IllegalArgumentException("The data array either is null or does not contain any data.");
      }

      if(values.length==1){
        return values[0];
      }

      // Rank order the values
      double[] v = new double[values.length];
      System.arraycopy(values, 0, v, 0, values.length);
      Arrays.sort(v);

      int n = (int) Math.round(v.length * lowerPercent / 100);
      
      return (double)v[n];
  }

  public static double quartile(Double[] values, double lowerPercent) {
	return quartile(Utils.getdoubleFromDouble(values), lowerPercent);
  }

  public static double mean(double[] m) {
    if (m == null || m.length == 0) {
        throw new IllegalArgumentException("The data array either is null or does not contain any data.");
    }
    double sum = 0;
    for (int i = 0; i < m.length; i++) {
        sum += m[i];
    }
    return sum / m.length;
  }
  
  public static double stderr(double[] m){
	  if (m == null || m.length == 0) {
	        throw new IllegalArgumentException("The data array either is null or does not contain any data.");
	  }
	  return stdev(m)/Math.sqrt(m.length);
  }
  
  public static double stdev(double[] m){
	  if (m == null || m.length == 0) {
	        throw new IllegalArgumentException("The data array either is null or does not contain any data.");
	  }
	  return Math.sqrt(variance(m));
  }
  
  public static double variance(double[] m){
	  if (m == null || m.length == 0) {
	        throw new IllegalArgumentException("The data array either is null or does not contain any data.");
	  }
      double mean = mean(m);
      double temp = 0;
      for(double d : m)
          temp += Math.pow(mean-d, 2);
      return temp/m.length;
  }

  public static double min(double[] d){
    double min = max(d);
    for(int i=0;i<d.length;i++){
      if( d[i]<min)
        min = d[i];
    }
    return min;
  }

  public static double max(double[] d){
	  double max = 0;
	  for(int i=0;i<d.length;i++){
		  if( d[i]>max)
			  max = d[i];
	  }
	  return max;
  }

  public static int max(int[] d){
	  int max = 0;
	  for(int i=0;i<d.length;i++){
		  if( d[i]>max)
			  max = d[i];
	  }
	  return max;
  }

  /**
   * Calculate the confidence interval about the data mean for
   * the given confidence level
   * @param data the array of data points
   * @param level the confidence level (0-1)
   * @return the lower [0] and upper [1] CI about the mean.
   */
  public static double[] calculateMeanConfidenceInterval(double[] data, double level){
	  SummaryStatistics stats = new SummaryStatistics();
	  for (double val : data) {
		  stats.addValue(val);
	  }

	  // Calculate 95% confidence interval
	  double ci = calcMeanCI(stats, level);
	  System.out.println(String.format("Mean: %f", stats.getMean()));

	  double lower = stats.getMean() - ci;
	  double upper = stats.getMean() + ci;
	  double[] result = { lower, upper };
	  return result;
  }

  /**
   * Calculate the confidence interval about the mean using a T-distribution
 * @param stats
 * @param level
 * @return
 */
  private static double calcMeanCI(SummaryStatistics stats, double level) {
	  try {
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

 }
  