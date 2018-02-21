package com.bmskinner.nuclear_morphology.gui;

import java.text.DecimalFormat;

import org.junit.Test;

public class FormatTest {
	@Test
	public void NumberFormatForExponentials(){
		
		DecimalFormat df=new DecimalFormat();
		df.applyPattern("0.#E0"); // this only works for the first (1.0)
		

		double zero = 1;
		double one = 0.1;
		double five = 0.00001;
		double ten = 0.0000000001;
		double thirty = 0.000000000000000000000000000001;		

		System.out.println(df.format(zero));
		System.out.println(df.format(one));
		System.out.println(df.format(five));
		System.out.println(df.format(ten));
		System.out.println(df.format(thirty));
				
	}
}
