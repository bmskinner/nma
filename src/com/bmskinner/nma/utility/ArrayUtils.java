package com.bmskinner.nma.utility;

/**
 * Utility methods for arrays
 * 
 * @author bs19022
 *
 */
public class ArrayUtils {

	private ArrayUtils() {
		// only uses static methods
	}

	/**
	 * Convert a long array to a float array
	 * 
	 * @param arr the array to be converted
	 * @return
	 */
	public static float[] toFloat(long[] arr) {
		float[] f = new float[arr.length];

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
		float[] f = new float[arr.length];

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
			int t = arr[i];
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
		float[] f = new float[length];
		for (int i = 0; i < f.length; i++)
			f[i] = value;
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
		byte[] f = new byte[length];
		for (int i = 0; i < f.length; i++)
			f[i] = value;
		return f;
	}

}
