package com.bmskinner.nma.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.utility.AngleTools;

public class RotatableTest {

	@Test
	public void testAlignVertical() {

		Random rng = new Random(1234);
		for (int it = 0; it < 500; it++) {

			IPoint t = new FloatPoint(rng.nextDouble() * 10, rng.nextDouble() * 10);
			IPoint b = new FloatPoint(rng.nextDouble() * 10, rng.nextDouble() * 10);
			IPoint c = new FloatPoint(rng.nextDouble() * 10, rng.nextDouble() * 10);

			double angle = Rotatable.getAngleToRotateVertical(t, b);

			t = AngleTools.rotateAboutPoint(t, c, angle);
			b = AngleTools.rotateAboutPoint(b, c, angle);
			assertTrue(ComponentTester.areVertical(t, b));

		}
	}

	/**
	 * Test that a test nucleus is able to be consistently rotated vertically using
	 * the TV and BV points
	 * 
	 * @throws Exception
	 */
	@Test
	public void testComponentAlignsVertically() throws Exception {

		RuleSetCollection rsc = RuleSetCollection.mouseSpermRuleSetCollection();

		// Create a single nucleus dataset
		IAnalysisDataset t = new TestDatasetBuilder(1234).cellCount(1).ofType(rsc).withMaxSizeVariation(0)
				.randomOffsetProfiles(true).segmented().build();

		// Profile length of the test nucleus
		int length = t.getCollection().stream().findFirst().get().getPrimaryNucleus().getBorderLength();

		// The initial bottom vertical point
		int bIndex = 1;

		// Create a top vertical point for alignment
		// We will test all possible positions in the
		// test dataset profile
		for (int tIndex = 0; tIndex < length; tIndex++) {
			if (tIndex == bIndex)
				continue;
			if (Math.abs(tIndex - bIndex) < 5)
				continue;

			// Make a new single nucleus dataset
			IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(1).ofType(rsc).withMaxSizeVariation(0)
					.randomOffsetProfiles(true).segmented().build();

			// Get the cell from the test dataset
			for (ICell c : d.getCollection()) {

				// Set the TV and BV to the current indices
				Nucleus n = c.getPrimaryNucleus();
				n.setOrientationMark(OrientationMark.TOP, tIndex);
				n.setOrientationMark(OrientationMark.BOTTOM, bIndex);
				n.orient();

				IPoint tv = n.getBorderPoint(OrientationMark.TOP);
				IPoint bv = n.getBorderPoint(OrientationMark.BOTTOM);

				List<OrientationMark> oms = n.getOrientationMarks();
				assertTrue(oms.contains(OrientationMark.TOP));
				assertTrue(oms.contains(OrientationMark.BOTTOM));

				// Test if the TV and BV points are vertical after rotation
				boolean areVertical = ComponentTester.areVertical(tv, bv);
				assertTrue(areVertical);
			}

		}
	}

	/**
	 * Test if a nucleus can be aligned horizontally using points on its border.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAlignPointsOnHorizontal() throws Exception {
		Nucleus n = TestComponentFactory.roundNucleus(50, 50, 100, 100, 0, 0,
				RuleSetCollection.roundRuleSetCollection());

		int length = n.getBorderLength();

		int i1 = length / 2;
		int i2 = i1 + 10;

		IPoint p1 = n.getBorderPoint(i1);
		IPoint p2 = n.getBorderPoint(i2);

		n.alignPointsOnHorizontal(p1, p2);

		IPoint r1 = n.getBorderPoint(i1);
		IPoint r2 = n.getBorderPoint(i2);

		// After alignment, X values should be identical
		assertTrue("R1 should have lower X value", r1.isLeftOf(r2));
		assertEquals("Y values should be equal", r1.getY(), r2.getY(), 0);
	}

	/**
	 * Test if a nucleus can be aligned vertically using points on its border.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAlignPointsOnVertical() throws Exception {
		Nucleus n = TestComponentFactory.roundNucleus(50, 50, 100, 100, 0, 0,
				RuleSetCollection.roundRuleSetCollection());

		int length = n.getBorderLength();

		int i1 = length / 2;
		int i2 = i1 + 10;

		IPoint p1 = n.getBorderPoint(i1);
		IPoint p2 = n.getBorderPoint(i2);

		n.alignPointsOnVertical(p1, p2);

		IPoint r1 = n.getBorderPoint(i1);
		IPoint r2 = n.getBorderPoint(i2);

		// After alignment, X values should be identical
		assertTrue("R1 should have higher Y value", r1.isAbove(r2));
		assertEquals("X values should be equal", r1.getX(), r2.getX(), 0);
	}

}
