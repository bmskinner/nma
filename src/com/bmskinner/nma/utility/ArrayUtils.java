package com.bmskinner.nma.utility;

public class ArrayUtils {
	
	/**
	 * Reverse the given array
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

	public static float[] floatArray(int length, float value) {
		float[] f = new float[length];
		for (int i = 0; i < f.length; i++)
			f[i] = value;
		return f;
	}

	public static byte[] byteArray(int length, byte value) {
		byte[] f = new byte[length];
		for (int i = 0; i < f.length; i++)
			f[i] = value;
		return f;
	}

}
