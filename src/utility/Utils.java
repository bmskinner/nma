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
  UTILITY FUNCTIONS
  -----------------------
  This class contains basic functions for 
  nuclear organisation analysis used across
  other classes
*/

 package utility;

 import ij.IJ;
import ij.process.FloatPolygon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import components.CellularComponent;
import components.nuclear.NucleusBorderPoint;
import components.nuclei.Nucleus;

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
	
	public static int[] getintFromInteger(Integer[] i){
		int[] result = new int[i.length];
		for (int j = 0; j < i.length; j++){
			result[j] = i[j];
		}
		return result;
	}
	
	/**
	 * Create a String array from an Integer array for exports
	 * @param d the Integer array
	 * @return a String array
	 */
	public static String[] getStringFromInteger(Integer[] d){
		 String[] s = new String[d.length];
		 for (int i = 0; i < s.length; i++){
			 s[i] = String.valueOf(d[i]);
		 }
		 return s;
	 }
	
	/**
	 * Create a float array from an Integer list
	 * @param d the Integer list
	 * @return a float array
	 */
	public static float[] getFloatArrayFromIntegerList(List<Integer> d){
		 float[] f = new float[d.size()];
		 for (int i = 0; i < f.length; i++){
			 f[i] = d.get(i);
		 }
		 return f;
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
	  * Wrap arrays for doubles. If an index falls of the end, it is returned to the start and vice versa
	 * @param i the index
	 * @param length the array length
	 * @return the index within the array
	 */
	public static double wrapIndex(double i, int length){
		 if(i<0)
			 i = length + i; // if i = -1, in a 200 length array,  will return 200-1 = 199
		 
		 if(Math.floor(i / length)>0) // if i is greater than array length 
			 i = i - (  (      (int)Math.floor(i / length) )    * length  );

//		 if(i<0 || i>length){
//			 IJ.log("Warning: array out of bounds wrapping index: "+i);
//		 }

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
	 

	 /**
	  * Turn the border points in a nucleus into a polygon. The positions in the polygon
	 * are offset (that is, the min x and y values are zero). To get the absolute positions
	 * of the points in the source image, use createOriginalPolygon(Nucleus n)
	  * @param n the nucleus
	  * @return
	  */
	 public static FloatPolygon createPolygon(Nucleus n){
//		 float[] xpoints = new float[n.getLength()];
//		 float[] ypoints = new float[n.getLength()];
//
//		 for(int i=0;i<n.getLength();i++){
//			 NucleusBorderPoint p = n.getBorderPoint(i);
//			 xpoints[i] = (float) p.getX();
//			 ypoints[i] = (float) p.getY();
//		 }
//
//		 return new FloatPolygon(xpoints, ypoints, n.getLength());
		 return createPolygon(n.getBorderList());
	 }

	 /**
	 * Turn a list of border points into a polygon. 
	 * @param list the list of border points
	 * @return
	 */
	public static FloatPolygon createPolygon(List<NucleusBorderPoint> list){
		 float[] xpoints = new float[list.size()+1];
		 float[] ypoints = new float[list.size()+1];

		 for(int i=0;i<list.size();i++){
			 NucleusBorderPoint p = list.get(i);
			 xpoints[i] = (float) p.getX();
			 ypoints[i] = (float) p.getY();
		 }
		 
		 // Ensure the polygon is closed
		 xpoints[list.size()] = (float) list.get(0).getX();
		 ypoints[list.size()] = (float) list.get(0).getY();

		 return new FloatPolygon(xpoints, ypoints);
	 }
	
	 /**
		 * Turn a list of border points into a polygon. Offset the points to the original
		 * position in a source image. Uses the Nucleus.originalPosition format
		 * @param list the list of border points
		 * @param originalPosition an array giving the original positions
		 * @see Nucleus.getPosition
		 * @return
		 */
		public static FloatPolygon createOriginalPolygon(List<NucleusBorderPoint> list, double[] originalPosition){
			 float[] xpoints = new float[list.size()+1];
			 float[] ypoints = new float[list.size()+1];

			 for(int i=0;i<list.size();i++){
				 NucleusBorderPoint p = list.get(i);
				 xpoints[i] = (float) p.getX() + (float) originalPosition[CellularComponent.X_BASE];
				 ypoints[i] = (float) p.getY() + (float) originalPosition[CellularComponent.Y_BASE];
			 }
			 
			// Ensure the polygon is closed
			 xpoints[list.size()] = (float) list.get(0).getX();
			 ypoints[list.size()] = (float) list.get(0).getY();

			 return new FloatPolygon(xpoints, ypoints);
		 }
	
	/**
	 * Turn the border points in a nucleus into a polygon, offset
	 * to the original positions of the ROI in the source image.
	 * @param n the nucleus
	 * @return a polygon
	 */
	public static FloatPolygon createOriginalPolygon(Nucleus n){
//		 float[] xpoints = new float[n.getLength()];
//		 float[] ypoints = new float[n.getLength()]; 
//
//		 for(int i=0;i<n.getLength();i++){
//			 NucleusBorderPoint p = n.getBorderPoint(i);
//			 xpoints[i] = (float) p.getX() + (float) n.getPosition()[Nucleus.X_BASE];
//			 ypoints[i] = (float) p.getY() + (float) n.getPosition()[Nucleus.Y_BASE];
//		 }
//
//		 return new FloatPolygon(xpoints, ypoints, n.getLength());
		return createOriginalPolygon(n.getOriginalBorderList(), n.getPosition());

	 }
	
	
	/**
	 * Convert the length in pixels into a length in microns.
	 * Assumes that the scale is in metres
	 * @param pixels the number of pixels
	 * @param scale the size of a pixel in microns
	 * @return
	 */
	public static double micronLength(double pixels, double scale){
		double microns = pixels * scale;
		return microns;
	}
	
	public static double micronArea(double pixels, double scale){
		double microns = pixels * scale;
		return microns;
	}
	
	/**
	  * Get the unique values from a double array
	  * @param d the double array
	  * @return a double array with unique values only
	  */
	 public static double[] getUnique(double[] d){
//		 double[] results = new double[d.length];
		 List<Double> results = new ArrayList<Double>();
		 Arrays.sort(d);
		 for(int i=1;i<d.length;i++){
			 if(d[i]!=d[i-1]){
				 results.add(d[i]);
			 }
		 }
		 return Utils.getdoubleFromDouble(  results.toArray(new Double[0] ));
	 }
	
 }