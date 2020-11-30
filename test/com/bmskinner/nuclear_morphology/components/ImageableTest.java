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
package com.bmskinner.nuclear_morphology.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestImageDatasetCreator;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

/**
 * Tests for the Imageable interface static and 
 * default methods
 * @author ben
 *
 */
public class ImageableTest extends ComponentTester {

	@Before
	public void setUp() throws Exception {
		super.setUp();
	}
	
	@Test
	public void testTranslateCoordinateToComponentImage() throws Exception {
		
		// Take an arbitrary nucleus from a test dataset
		// We only care that it has an offset component image relative to the source image
		IAnalysisDataset d = SampleDatasetReader.openDataset(new File(TestResources.MOUSE_TEST_DATASET));
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
		IAnalysisDataset d = SampleDatasetReader.openDataset(new File(TestResources.MOUSE_TEST_DATASET));
		Nucleus n = d.getCollection().getNuclei().toArray(new Nucleus[0])[0];
		
		// This is the template image coordinate converted to source
		// The buffer must be removed from the expected value
		// We only need this to be int precision
		assertTrue("Template converted to base", n.getOriginalBase().minus(Imageable.COMPONENT_BUFFER)
				.overlaps(Imageable.translateCoordinateToSourceImage(n.getBase(), n)));
		
	}

	@Test
	public void testComponentIsOffsetInSeahorseImage() throws Exception {
		
		File testFolder = new File(TestResources.IMAGE_FOLDER+"issues/offsets/").getAbsoluteFile();
    	
    	IAnalysisOptions op = OptionsFactory.makeAnalysisOptions();
    	op.setNucleusType(NucleusType.ROUND);
    	IDetectionOptions nOp = OptionsFactory.makeNucleusDetectionOptions(testFolder);
    	nOp.setMaxSize(2000000);
    	nOp.setMinSize(5000);
    	nOp.setThreshold(30);
    	nOp.setChannel(2);
    	nOp.getCannyOptions().setUseCanny(false);
    	nOp.getCannyOptions().setUseKuwahara(false);
    	op.setDetectionOptions(CellularComponent.NUCLEUS, nOp);
    	op.setAngleWindowProportion(0.03);

    	File saveFile = new File(TestResources.IMAGE_FOLDER+"issues/offsets/offsets.nmd").getAbsoluteFile();
    	IAnalysisDataset d = TestImageDatasetCreator.createTestDataset(testFolder.getAbsolutePath(), op, false);
    	TestImageDatasetCreator.saveTestDataset(d, saveFile);
    	
    	Nucleus n = d.getCollection().getNuclei().toArray(new Nucleus[0])[0];
    	
    	// Check the nucleus has loaded
    	assertEquals("Original position", IPoint.makeNew(121, 84), n.getOriginalBase());
    	
    	IPoint offsetBase = Imageable.translateCoordinateToComponentImage(n.getOriginalBase(), n);    	
    	assertEquals("Offset position", n.getBase().plus(Imageable.COMPONENT_BUFFER), offsetBase);
	}

}
