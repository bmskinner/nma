/*******************************************************************************
 * Copyright (C) 2020 Ben Skinner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.components;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.Imageable;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.io.SampleDatasetReader;

/**
 * Tests for the Imageable interface static and 
 * default methods
 * @author ben
 *
 */
public class ImageableTest {
	
	@Test
	public void testTranslateCoordinateToComponentImage() throws Exception {
		
		// Take an arbitrary nucleus from a test dataset
		// We only care that it has an offset component image relative to the source image
		IAnalysisDataset d = SampleDatasetReader.openDataset(TestResources.MOUSE_TEST_DATASET);
		Nucleus n = d.getCollection().getNuclei().toArray(new Nucleus[0])[0];
				
		// The buffer must be added from the expected value
		
		assertTrue("Source base converted to component image", 
				n.getBase().plus(Imageable.COMPONENT_BUFFER)
				.overlaps(Imageable.translateCoordinateToComponentImage(n.getOriginalBase(), n)));
	}
	
	@Test
	public void testTranslateCoordinateToSourceImage() throws Exception {
		
		// Take an arbitrary nucleus from a test dataset
		// We only care that it has an offset component image relative to the source image
		IAnalysisDataset d = SampleDatasetReader.openDataset(TestResources.MOUSE_TEST_DATASET);
		Nucleus n = d.getCollection().getNuclei().toArray(new Nucleus[0])[0];
		
		// This is the template image coordinate converted to source
		// The buffer must be removed from the expected value
		// We only need this to be int precision
		assertTrue("Template converted to base", n.getOriginalBase().minus(Imageable.COMPONENT_BUFFER)
				.overlaps(Imageable.translateCoordinateToSourceImage(n.getBase(), n)));
		
	}
//
//	@Test
//	public void testComponentIsOffsetInSeahorseImage() throws Exception {
//		
//		File testFolder = new File(TestResources.IMAGE_FOLDER+"issues/offsets/").getAbsoluteFile();
//    	
//		IAnalysisOptions op = OptionsFactory.makeAnalysisOptions();
//		op.setRuleSetCollection(RuleSetCollection.roundRuleSetCollection());
//		HashOptions nOp = OptionsFactory.makeNucleusDetectionOptions(testFolder)
//				.withValue(HashOptions.MAX_SIZE_PIXELS, 2000000)
//				.withValue(HashOptions.MIN_SIZE_PIXELS, 5000)
//				.withValue(HashOptions.THRESHOLD, 30)
//				.withValue(HashOptions.CHANNEL, 2)
//				.withValue(HashOptions.IS_USE_CANNY, false)
//				.withValue(HashOptions.IS_USE_KUWAHARA, false)
//				.build();
//		
//		op.setDetectionOptions(CellularComponent.NUCLEUS, nOp);
//		op.setAngleWindowProportion(0.03);
//
//    	File saveFile = new File(TestResources.IMAGE_FOLDER+"issues/offsets/offsets.nmd").getAbsoluteFile();
//    	IAnalysisDataset d = TestImageDatasetCreator.createTestDataset(testFolder, op, false);
//    	TestImageDatasetCreator.saveTestDataset(d, saveFile);
//    	
//    	Nucleus n = d.getCollection().getNuclei().toArray(new Nucleus[0])[0];
//    	
//    	// Check the nucleus has loaded
//    	assertEquals("Original position", IPoint.makeNew(121, 84), n.getOriginalBase());
//    	
//    	IPoint offsetBase = Imageable.translateCoordinateToComponentImage(n.getOriginalBase(), n);    	
//    	assertEquals("Offset position", n.getBase().plus(Imageable.COMPONENT_BUFFER), offsetBase);
//	}

}
