package com.bmskinner.nuclear_morphology.analysis.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.logging.Logger;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.image.MultiScaleStructuralSimilarityIndex.MSSIMScore;

import ij.IJ;
import ij.process.ImageProcessor;

/**
 * Tests for MS-SSIM*
 * @author bms41
 * @since 1.16.0
 *
 */
public class MultiScaleStructuralSimilarityIndexTest {
	
	private static final Logger LOGGER = Logger.getLogger(MultiScaleStructuralSimilarityIndexTest.class.getName());

	@Test
	public void testMSSIMcalculates() {
		String imagePath = TestResources.WARPING_FOLDER.getAbsolutePath();
		ImageProcessor wtX = IJ.openImage(imagePath+"/WT-X.tiff").getProcessor();
		ImageProcessor shX = IJ.openImage(imagePath+"/sh-X.tiff").getProcessor();
		
		MultiScaleStructuralSimilarityIndex ms = new MultiScaleStructuralSimilarityIndex();
		MSSIMScore result = ms.calculateMSSIM(wtX, shX);
		
		double expected = 0.8484014369982309; // previously measured value
		assertEquals(expected, result.msSsimIndex, 0);
	}
	
	@Test
	public void testMSSIMValueOrderMatchesKnown() {
		String imagePath = TestResources.WARPING_FOLDER.getAbsolutePath();
		ImageProcessor wtX = IJ.openImage(imagePath+"/WT-X.tiff").getProcessor();
		ImageProcessor shX = IJ.openImage(imagePath+"/sh-X.tiff").getProcessor();
		ImageProcessor shY = IJ.openImage(imagePath+"/sh-Y.tiff").getProcessor();
		
		MultiScaleStructuralSimilarityIndex ms = new MultiScaleStructuralSimilarityIndex();
		MSSIMScore wtx_shx_result = ms.calculateMSSIM(wtX, shX);
		MSSIMScore shx_shy_result = ms.calculateMSSIM(shX, shY);
		
		assertTrue(wtx_shx_result.msSsimIndex<shx_shy_result.msSsimIndex);
	}

}
