/* 
  -----------------------
  STATS FUNCTIONS
  -----------------------
  This class contains basic functions for 
  nuclear organisation analysis used across
  other classes
*/

 package no.utility;

 import ij.IJ;
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

    if (values == null || values.length == 0) {
        throw new IllegalArgumentException("The data array either is null or does not contain any data.");
    }

    // Rank order the values
    Double[] v = new Double[values.length];
    System.arraycopy(values, 0, v, 0, values.length);
    Arrays.sort(v);

    int n = (int) Math.round(v.length * lowerPercent / 100);
    
    return (double)v[n];
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