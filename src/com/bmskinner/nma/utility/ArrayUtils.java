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
