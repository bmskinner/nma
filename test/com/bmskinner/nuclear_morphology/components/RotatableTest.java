package com.bmskinner.nuclear_morphology.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.charting.ChartFactoryTest;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.utility.AngleTools;

public class RotatableTest {
	
	private void assertAreVertical(@NonNull IPoint topPoint, @NonNull IPoint bottomPoint) {
		double err = bottomPoint.getX()-topPoint.getX();
		System.out.println("Error = "+err);
		assertEquals(bottomPoint.getX(), topPoint.getX(), 0.0001);
		
		assertTrue(topPoint.getY()>bottomPoint.getY());
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
			System.out.println("A: "+angle);
			System.out.println("T: "+t.toString());
			System.out.println("B: "+b.toString());
			assertAreVertical(t, b);

		}
	}
	
	@Test 
	public void testComponentAlignsVertically() throws Exception {
		IAnalysisDataset t = new TestDatasetBuilder(1234).cellCount(1)
				.ofType(NucleusType.ROUND)
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.segmented().build();
		
		int length = t.getCollection().stream().findFirst().get().getNucleus().getBorderLength();

		for(int tIndex=0; tIndex<length; tIndex++) {
			for(int bIndex=0; bIndex<length; bIndex++) {
				if(tIndex==bIndex)
					continue;
				if(Math.abs(tIndex-bIndex)<5)
					continue;

				IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(1)
						.ofType(NucleusType.ROUND)
						.withMaxSizeVariation(10)
						.randomOffsetProfiles(true)
						.segmented().build();

				for(Nucleus n : d.getCollection().getNuclei()) {
					System.out.println("Testing "+tIndex+" and "+bIndex);
					n.setBorderTag(Tag.TOP_VERTICAL, tIndex);
					n.setBorderTag(Tag.BOTTOM_VERTICAL, bIndex);

					IPoint tv = n.getBorderPoint(Tag.TOP_VERTICAL);
					IPoint bv = n.getBorderPoint(Tag.BOTTOM_VERTICAL);

					System.out.println("TV: "+tv);
					System.out.println("BV: "+bv);

					n.alignVertically();
					tv = n.getBorderPoint(Tag.TOP_VERTICAL);
					bv = n.getBorderPoint(Tag.BOTTOM_VERTICAL);

					System.out.println("TV: "+tv);
					System.out.println("BV: "+bv);
					assertAreVertical(tv, bv);
				}
			}

		}
	}

}
