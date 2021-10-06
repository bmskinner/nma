package com.bmskinner.nuclear_morphology.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class StringUtilsTest {
	
	@Test
	public void testHexBytesEncodedAndDecoded(){
		
		byte[] b = { 1, 2, 3, 4 };
		
		String s = StringUtils.bytesToHex(b);
		
		byte[] t = StringUtils.hexToBytes(s);		
		assertTrue(Arrays.equals(b,  t));		
	}
	
	@Test
	public void testHexBytesEncodedAndDecodedWithLeadingZero(){
		
		byte[] b = { 0, 0, 1, 2, 3, 4 };
		
		String s = StringUtils.bytesToHex(b);
		
		byte[] t = StringUtils.hexToBytes(s);		
		assertTrue(Arrays.equals(b,  t));		
	}
	
	@Test
	public void testHexBytesEncodedAndDecodedWithAllZero(){
		
		byte[] b = { 0, 0, 0, 0, 0 ,0, 0 };
		
		String s = StringUtils.bytesToHex(b);
		
		byte[] t = StringUtils.hexToBytes(s);		
		assertTrue(Arrays.equals(b,  t));		
	}

}
