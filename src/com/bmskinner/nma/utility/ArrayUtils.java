package com.bmskinner.nma.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility methods for arrays
 * 
 * @author Ben Skinner
 *
 */
public class ArrayUtils {

	private ArrayUtils() {
		// only uses static methods
	}
	
	/**
	 * Box the given doubles and add to a mutable list
	 * 
	 * @param arr
	 * @return
	 */
	public static List<Double> toMutableList(double[] arr) {
		final List<Double> result = new ArrayList<>();
		for (final double d : arr) {
			result.add(d);
		}
		return result;
	}

	public static double[] toArray(List<Double> l) {
		final double[] result = new double[l.size()];
		for (int i = 0; i < l.size(); i++) {
			result[i] = l.get(i);
		}
		return result;
	}

	/**
	 * Unbox the given array
	 * @param arr
	 * @return
	 */
	public static int[] unbox(Integer[] arr) {
		return Arrays.stream(arr).mapToInt(Integer::intValue).toArray();
	}
	
	/**
	 * Unbox the given array
	 * @param arr
	 * @return
	 */
	public static double[] unbox(Double[] arr) {
		return Arrays.stream(arr).mapToDouble(Double::doubleValue).toArray();
	}

	/**
	 * Convert a Float array to a float array
	 * 
	 * @param arr the array to be converted
	 * @return
	 */
	public static float[] toFloat(Float[] arr) {
		final float[] f = new float[arr.length];
		for (int i = 0; i < arr.length; i++) {
			f[i] = arr[i];

		}
		return f;
	}

	/**
	 * Convert a long array to a float array
	 * 
	 * @param arr the array to be converted
	 * @return
	 */
	public static float[] toFloat(long[] arr) {
		final float[] f = new float[arr.length];

		for (int i = 0; i < arr.length; i++) {
			f[i] = arr[i];

		}
		return f;
	}

	/**
	 * Convert a double array to a float array
	 * 
	 * @param arr the array to be converted
	 * @return
	 */
	public static float[] toFloat(double[] arr) {
		final float[] f = new float[arr.length];

		for (int i = 0; i < arr.length; i++) {
			f[i] = (float) arr[i];

		}
		return f;
	}

	/**
	 * Reverse the given array
	 * 
	 * @param arr
	 * @return
	 */
	public static int[] reverse(int[] arr) {
		for (int i = 0; i < arr.length / 2; i++) {
			final int t = arr[i];
			arr[i] = arr[arr.length - i - 1];
			arr[arr.length - i - 1] = t;
		}
		return arr;
	}

	/**
	 * Create a float array with the given value
	 * 
	 * @param length
	 * @param value
	 * @return
	 */
	public static float[] floatArray(int length, float value) {
		final float[] f = new float[length];
		for (int i = 0; i < f.length; i++) {
			f[i] = value;
		}
		return f;
	}

	/**
	 * Create a byte array with the given value
	 * 
	 * @param length
	 * @param value
	 * @return
	 */
	public static byte[] byteArray(int length, byte value) {
		final byte[] f = new byte[length];
		for (int i = 0; i < f.length; i++) {
			f[i] = value;
		}
		return f;
	}

}
