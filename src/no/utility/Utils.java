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

 /**
 * @author bms41
 *
 */
public class Utils {
	 
	 /**
	  *  Create a double array from a Double array
	  * @param d the Double array
	  * @return a double array
	  */
	 public static double[] getdoubleFromDouble(Double[] d){
		 double[] results = new double[d.length];
		 for(int i=0;i<d.length;i++){
			 results[i] = d[i];
		 }
		 return results;
	 }

	 /**
	 * Create a Double array from a double array
	 * @param d the double array
	 * @return the Double array
	 */
	public static Double[] getDoubleFromdouble(double[] d){
		 Double[] results = new Double[d.length];
		 for(int i=0;i<d.length;i++){
			 results[i] = d[i];
		 }
		 return results;
	 }

	
	 /**
	  * Create a String array from a double array fro exports
	 * @param d the array
	 * @return the String array
	 */
	public static String[] getStringFromDouble(double[] d){
		 String[] s = new String[d.length];
		 for (int i = 0; i < s.length; i++){
			 s[i] = String.valueOf(d[i]);
		 }
		 return s;
	 }

	/**
	 * Create a String array from an int array for exports
	 * @param d the int array
	 * @return a String array
	 */
	public static String[] getStringFromInt(int[] d){
		 String[] s = new String[d.length];
		 for (int i = 0; i < s.length; i++){
			 s[i] = String.valueOf(d[i]);
		 }
		 return s;
	 }

	/**
	 * Create a double array from an int array
	 * @param d the int array
	 * @return a double array
	 */
	 public static double[] getdoubleFromInt(int[] d){
		 double[] s = new double[d.length];
		 for (int i = 0; i < s.length; i++){
			 s[i] = (double) d[i];
		 }
		 return s;
	 }

	 
	 
	 /**
	  * Wrap arrays. If an index falls of the end, it is returned to the start and vice versa
	 * @param i the index
	 * @param length the array length
	 * @return the index within the array
	 */
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

	
	 /**
	  * Find the length on the x-axis of a line at a given angle
	 * @param length the line length
	 * @param angle the angle from 0 relative to positive x axis
	 * @return the x distance
	 */
	public static double getXComponentOfAngle(double length, double angle){
		 // cos(angle) = x / h
		 // x = cos(a)*h
		 double x = length * Math.cos(Math.toRadians(angle));
		 return x;
	 }

	/**
	  * Find the length on the y-axis of a line at a given angle
	 * @param length the line length
	 * @param angle the angle from 0 relative to positive x axis
	 * @return the y distance
	 */
	 public static double getYComponentOfAngle(double length, double angle){
		 double y = length * Math.sin(Math.toRadians(angle));
		 return y;
	 }
 }