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