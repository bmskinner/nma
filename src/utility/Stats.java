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

 import java.util.*;

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
 }