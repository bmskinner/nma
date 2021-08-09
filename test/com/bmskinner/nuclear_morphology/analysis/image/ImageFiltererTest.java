package com.bmskinner.nuclear_morphology.analysis.image;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.io.ImageImporter;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class ImageFiltererTest {

	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testNormaliseToCounterstainOnConsistentValues() throws Exception {
		
		// Create an image with constant pixel values
		ImageProcessor ip1 = new ByteProcessor(100, 100);
		ip1.set(50);
		
		// Create an image with constant pixel values
		ImageProcessor ip2 = new ByteProcessor(100, 100);
		ip2.set(100);
		
		ImageProcessor result = ImageFilterer.normaliseToCounterStain(ip1, ip2);
		for(int i=0; i<result.getPixelCount(); i++) {
			assertEquals(0.5, result.getf(i), 0.01);
		}
		
//		new ImagePlus("", result).show();
//		Thread.sleep(10000);
	}
	
	@Test
	public void testNormaliseToCounterstainOnVariedValues() throws Exception {
		
		// Create an image with two zones of constant pixel values
		// Top half is twice the brightness of the bottom half
		ImageProcessor ip1 = new ByteProcessor(100, 100);
		ip1.set(50);
		for(int i=0; i<5000; i++) {
			ip1.set(i, 100);
		}

		// Create an image with constant pixel values
		ImageProcessor ip2 = new ByteProcessor(100, 100);
		ip2.set(100);
		
		ImageProcessor result = ImageFilterer.normaliseToCounterStain(ip1, ip2);
		for(int i=0; i<5000; i++) {
			assertEquals(1, result.getf(i), 0.01);
		}
		for(int i=5000; i<result.getPixelCount(); i++) {
			assertEquals(0.5, result.getf(i), 0.01);
		}
	}
	
	/**
	 * If the counterstain has zero value pixels, the
	 * normalised value should also be zero
	 * @throws Exception
	 */
	@Test
	public void testNormaliseToCounterstainWithZeroInCounterstain() throws Exception {
		
		// Create an image with two zones of constant pixel values
		// Top half is twice the brightness of the bottom half
		ImageProcessor ip1 = new ByteProcessor(100, 100);
		ip1.set(50);

		// Create an image with constant pixel values
		ImageProcessor ip2 = new ByteProcessor(100, 100);
		ip2.set(0);
		
		ImageProcessor result = ImageFilterer.normaliseToCounterStain(ip1, ip2);
		for(int i=0; i<result.getPixelCount(); i++) {
			assertEquals(0, result.getf(i), 0.01);
		}
	}
	
	/**
	 * If the input image has zero value pixels, the
	 * normalised value should also be zero
	 * @throws Exception
	 */
	@Test
	public void testNormaliseToCounterstainWithZeroInInput() throws Exception {
		
		// Create an image with two zones of constant pixel values
		// Top half is twice the brightness of the bottom half
		ImageProcessor ip1 = new ByteProcessor(100, 100);
		ip1.set(0);

		// Create an image with constant pixel values
		ImageProcessor ip2 = new ByteProcessor(100, 100);
		ip2.set(50);
		
		ImageProcessor result = ImageFilterer.normaliseToCounterStain(ip1, ip2);
		for(int i=0; i<result.getPixelCount(); i++) {
			assertEquals(0, result.getf(i), 0.01);
		}
	}
	
	/**
	 * If the input image and counterstain has zero value pixels, the
	 * normalised value should be 1
	 * @throws Exception
	 */
	@Test
	public void testNormaliseToCounterstainWithZeroInInputAndCounterstain() throws Exception {
		
		// Create an image with two zones of constant pixel values
		// Top half is twice the brightness of the bottom half
		ImageProcessor ip1 = new ByteProcessor(100, 100);
		ip1.set(0);

		// Create an image with constant pixel values
		ImageProcessor ip2 = new ByteProcessor(100, 100);
		ip2.set(0);
		
		ImageProcessor result = ImageFilterer.normaliseToCounterStain(ip1, ip2);
		for(int i=0; i<result.getPixelCount(); i++) {
			assertEquals(1, result.getf(i), 0.01);
		}
	}
	
	@Test
	public void testNormaliseToCounterstainOnRealImage() throws Exception {
		
		// Import a real image and test range is zero
		ImageProcessor ip1 = new ImageImporter(new File(TestResources.GLCM_SAMPLE_IMAGE)).importImage(2);
		
		// Create an image with constant pixel values
		ImageProcessor ip2 = new ImageImporter(new File(TestResources.GLCM_SAMPLE_IMAGE)).importImage(2);
				
		ImageProcessor result = ImageFilterer.normaliseToCounterStain(ip1, ip2);
				
		for(int i=0; i<result.getPixelCount(); i++) {
			assertEquals(1, result.getf(i), 0.01);
		}
	}

}
