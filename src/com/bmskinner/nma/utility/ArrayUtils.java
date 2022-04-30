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

}
