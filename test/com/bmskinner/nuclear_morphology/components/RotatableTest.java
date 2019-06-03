package com.bmskinner.nuclear_morphology.components;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import javax.swing.JPanel;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.charting.OutlineTestChartFactory;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.utility.AngleTools;

public class RotatableTest extends ComponentTester {
	
	@Before
	public void setUp() throws Exception{
		super.setUp();
		logger.setLevel(Level.INFO);
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
			logger.fine("A: "+angle);
			logger.fine("T: "+t.toString());
			logger.fine("B: "+b.toString());
			assertTrue(areVertical(t, b));

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
		int bIndex = 1;
		
		for(int tIndex=0; tIndex<length; tIndex++) {			
			if(tIndex==bIndex)
				continue;
			if(Math.abs(tIndex-bIndex)<5)
				continue;

			IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(1)
					.ofType(NucleusType.ROUND)
					.withMaxSizeVariation(0)
					.randomOffsetProfiles(true)
					.segmented().build();

			for(ICell c : d.getCollection()) {
				List<JPanel> panels = new ArrayList<>();

				Nucleus n = c.getNucleus();
				logger.info("Testing "+tIndex+" and "+bIndex+" of "+length);
				n.setBorderTag(Tag.TOP_VERTICAL, tIndex);
				n.setBorderTag(Tag.BOTTOM_VERTICAL, bIndex);
				panels.add(OutlineTestChartFactory.generateOutlineChart(d, c));
				IPoint tv = n.getBorderPoint(Tag.TOP_VERTICAL);
				IPoint bv = n.getBorderPoint(Tag.BOTTOM_VERTICAL);

				logger.fine("TV: "+tv);
				logger.fine("BV: "+bv);

				n.alignVertically();
				tv = n.getBorderPoint(Tag.TOP_VERTICAL);
				bv = n.getBorderPoint(Tag.BOTTOM_VERTICAL);

				logger.fine("TV: "+tv);
				logger.fine("BV: "+bv);

				panels.add(OutlineTestChartFactory.generateOutlineChart(d, c));

				boolean areVertical = areVertical(tv, bv);
				//					if(!areVertical)
				//						ChartFactoryTest.showCharts(panels, "TV: "+tIndex+" BV "+bIndex);
				assertTrue(areVertical);
			}

		}
	}

}
