package com.bmskinner.nma.analysis.image;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.visualisation.image.ImageFilterer;

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
		for (int i = 0; i < result.getPixelCount(); i++) {
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
		for (int i = 0; i < 5000; i++) {
			ip1.set(i, 100);
		}

		// Create an image with constant pixel values
		ImageProcessor ip2 = new ByteProcessor(100, 100);
		ip2.set(100);

		ImageProcessor result = ImageFilterer.normaliseToCounterStain(ip1, ip2);
		for (int i = 0; i < 5000; i++) {
			assertEquals(1, result.getf(i), 0.01);
		}
		for (int i = 5000; i < result.getPixelCount(); i++) {
			assertEquals(0.5, result.getf(i), 0.01);
		}
	}

	/**
	 * If the counterstain has zero value pixels, the normalised value will be 0
	 * 
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
		for (int i = 0; i < result.getPixelCount(); i++) {
			assertEquals(0, result.getf(i), 0.01);
		}
	}

	/**
	 * If the input image has zero value pixels, the normalised value should be 0
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNormaliseToCounterstainWithZeroInInput() throws Exception {

		ImageProcessor ip1 = new ByteProcessor(100, 100);
		ip1.set(0);

		ImageProcessor ip2 = new ByteProcessor(100, 100);
		ip2.set(50);

		ImageProcessor result = ImageFilterer.normaliseToCounterStain(ip1, ip2);
		for (int i = 0; i < result.getPixelCount(); i++) {
			assertEquals(0f, result.getf(i), 0.01);
		}
	}

	/**
	 * If the input image and counterstain has zero value pixels, the normalised
	 * value should be 0
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNormaliseToCounterstainWithZeroInInputAndCounterstain() throws Exception {

		ImageProcessor ip1 = new ByteProcessor(100, 100);
		ip1.set(0);

		ImageProcessor ip2 = new ByteProcessor(100, 100);
		ip2.set(0);

		ImageProcessor result = ImageFilterer.normaliseToCounterStain(ip1, ip2);
		for (int i = 0; i < result.getPixelCount(); i++) {
			assertEquals(0, result.getf(i), 0.01);
		}
	}

	/**
	 * Test that running normalisation on an image against itself gives a normalised
	 * value of 1 except in regions where pixel value is 0, which should return zero
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNormaliseToCounterstainOnRealImage() throws Exception {

		ImageProcessor ip1 = new ImageImporter(TestResources.GLCM_SAMPLE_IMAGE).importImage(2);
		ImageProcessor ip2 = new ImageImporter(TestResources.GLCM_SAMPLE_IMAGE).importImage(2);

		ImageProcessor result = ImageFilterer.normaliseToCounterStain(ip1, ip2);

		for (int i = 0; i < result.getPixelCount(); i++) {
			assertEquals(0.5f, result.getf(i), 0.5f);
		}
	}

	/**
	 * This gradient image has blue counterstain decreasing left to right, and red
	 * signal decreasing left to right but slower. The normalised result should show
	 * signal intensity increasing to the right
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNormaliseToCounterstainOnGradientImage() throws Exception {
		// Import a real image and test range is zero
		ImageProcessor ip1 = new ImageImporter(TestResources.WARPING_NORMALISATION_IMAGE)
				.importImage(0);
		ImageProcessor ip2 = new ImageImporter(TestResources.WARPING_NORMALISATION_IMAGE)
				.importImage(2);

		ImageProcessor result = ImageFilterer.normaliseToCounterStain(ip1, ip2);
//		new ImagePlus("", result).show();
//		Thread.sleep(10000);

//		result = ImageFilterer.rescaleImageIntensity(result);

	}

}
