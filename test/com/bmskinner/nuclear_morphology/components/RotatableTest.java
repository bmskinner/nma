package com.bmskinner.nuclear_morphology.components;

import static org.junit.Assert.assertTrue;

import java.util.Random;
import java.util.logging.Level;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.utility.AngleTools;

public class RotatableTest extends ComponentTester {
	
	@Before
	public void setUp() throws Exception{
		super.setUp();
		LOGGER.setLevel(Level.INFO);
	}

	@Test
	public void testAlignVertical() {

		Random rng = new Random(1234);
		for(int it=0; it<500; it++) {

			IPoint t = IPoint.makeNew(rng.nextDouble()*10, rng.nextDouble()*10);
			IPoint b = IPoint.makeNew(rng.nextDouble()*10, rng.nextDouble()*10);
			IPoint c = IPoint.makeNew(rng.nextDouble()*10, rng.nextDouble()*10);

			double angle = Rotatable.getAngleToRotateVertical(t, b);

			t = AngleTools.rotateAboutPoint(t, c, angle);
			b = AngleTools.rotateAboutPoint(b, c, angle);
			assertTrue(areVertical(t, b));

		}
	}
	
	/**
	 * Test that a test nucleus is able to be consistently
	 * rotated vertically using the TV and BV points
	 * @throws Exception
	 */
	@Test 
	public void testComponentAlignsVertically() throws Exception {
		
		// Create a single nucleus dataset
		IAnalysisDataset t = new TestDatasetBuilder(1234).cellCount(1)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(0)
				.randomOffsetProfiles(true)
				.segmented().build();
		
		// Profile length of the test nucleus
		int length = t.getCollection().stream()
				.findFirst().get()
				.getPrimaryNucleus()
				.getBorderLength();
		
		// The initial bottom vertical point
		int bIndex = 1;
		
		// Create a top vertical point for alignment
		// We will test all possible positions in the 
		// test dataset profile
		for(int tIndex=0; tIndex<length; tIndex++) {			
			if(tIndex==bIndex)
				continue;
			if(Math.abs(tIndex-bIndex)<5)
				continue;

			// Make a new single nucleus dataset
			IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(1)
					.ofType(RuleSetCollection.roundRuleSetCollection())
					.withMaxSizeVariation(0)
					.randomOffsetProfiles(true)
					.segmented().build();

			// Get the cell from the test dataset
			for(ICell c : d.getCollection()) {
				
				// Set the TV and BV to the current indices
				Nucleus n = c.getPrimaryNucleus();
				n.setLandmark(Landmark.TOP_VERTICAL, tIndex);
				n.setLandmark(Landmark.BOTTOM_VERTICAL, bIndex);
				n.alignVertically();
				
				IPoint tv = n.getBorderPoint(Landmark.TOP_VERTICAL);
				IPoint bv = n.getBorderPoint(Landmark.BOTTOM_VERTICAL);

				// Test if the TV and BV points are vertical after rotation
				boolean areVertical = areVertical(tv, bv);
				assertTrue(areVertical);
			}

		}
	}

}
