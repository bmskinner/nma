package com.bmskinner.nma.utility;

import java.nio.charset.StandardCharsets;

/**
 * Utilities for string conversions and 
 * manipulation
 * @author ben
 * @since 2.0.0
 */
public class StringUtils {
	
	private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
	
	private StringUtils() {	}
	

	/**
	 * Create hexadecimal representation of the given 
	 * byte array
	 * @param bytes
	 * @return
	 */
	public static String bytesToHex(byte[] bytes) {
	    byte[] hexChars = new byte[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars, StandardCharsets.UTF_8);
	}
	
	
	/**
	 * Create a byte array from the hexadecimal representation
	 * @param s the hexadecimal string
	 * @return
	 */
	public static byte[] hexToBytes(String s) {
		byte[] bytes = new byte[s.length() / 2];
	       
        for (int i = 0; i < s.length(); i += 2) {
             // using left shift operator on every character
        	bytes[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return bytes;
	}
}
