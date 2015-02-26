/* 
  -----------------------
  UTILITY FUNCTIONS
  -----------------------
  This class contains basic functions for 
  nuclear organisation analysis used across
  other classes
*/

 package no.utility;

 import ij.IJ;
 import java.util.*;
 import no.components.*;

 public class NuclearOrganisationUtility {

 	/*
    Calculate the <lowerPercent> quartile from a Double[] array
  */
  public static double quartile(double[] values, double lowerPercent) {

      if (values == null || values.length == 0) {
          throw new IllegalArgumentException("The data array either is null or does not contain any data.");
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

  /*
    Turn a Double[] into a double[]
  */
  public static double[] getdoubleFromDouble(Double[] d){
    double[] results = new double[d.length];
    for(int i=0;i<d.length;i++){
      results[i] = d[i];
    }
    return results;
  }

  public static Double[] getDoubleFromdouble(double[] d){
    Double[] results = new Double[d.length];
    for(int i=0;i<d.length;i++){
      results[i] = d[i];
    }
    return results;
  }

  /*
    Turn a double[] into a String[]
  */
  public static String[] getStringFromDouble(double[] d){
    String[] s = new String[d.length];
    for (int i = 0; i < s.length; i++){
        s[i] = String.valueOf(d[i]);
    }
    return s;
  }

  /*
    Turn an int[] into a String[]
  */
  public static String[] getStringFromInt(int[] d){
    String[] s = new String[d.length];
    for (int i = 0; i < s.length; i++){
        s[i] = String.valueOf(d[i]);
    }
    return s;
  }

  public static int wrapIndex(int i, int length){
    if(i<0)
      i = length + i; // if i = -1, in a 200 length array,  will return 200-1 = 199
    if(Math.floor(i / length)>0)
      i = i - ( ((int)Math.floor(i / length) )*length);

    if(i<0 || i>length){
      IJ.log("Warning: array out of bounds: "+i);
    }
    
    return i;
  }

  public static double getMin(double[] d){
    double min = getMax(d);
    for(int i=0;i<d.length;i++){
      if( d[i]<min)
        min = d[i];
    }
    return min;
  }

  public static double getMax(double[] d){
    double max = 0;
    for(int i=0;i<d.length;i++){
      if( d[i]>max)
        max = d[i];
    }
    return max;
  }

  public static double[] findLineEquation(XYPoint a, XYPoint b){

    // y=mx+c
    double deltaX = a.getX() - b.getX();
    double deltaY = a.getY() - b.getY();
      
    double m = deltaY / deltaX;
      
    // y - y1 = m(x - x1)
    double c = a.getY() -  ( m * a.getX() );
      
    // double testY = (m * position_2[0]) + c;
      
    // write("y = "+m+"x + "+c);
    // result=newArray(m, c);
    return new double[] { m, c };
  }

  public static double getXFromEquation(double[] eq, double y){
    // x = (y-c)/m
    double x = (y - eq[1]) / eq[0];
    return x;
  }

  public static double getYFromEquation(double[] eq, double x){
    // x = (y-c)/m
    double y = (eq[0] * x) + eq[1];
    return y;
  }

  public static double getXComponentOfAngle(double length, double angle){
    // cos(angle) = x / h
    // x = cos(a)*h
    double x = length * Math.cos(Math.toRadians(angle));
    return x;
  }

  public static double getYComponentOfAngle(double length, double angle){
    double y = length * Math.sin(Math.toRadians(angle));
    return y;
  }
 }