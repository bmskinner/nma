package com.bmskinner.nuclear_morphology.analysis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

public class ComponentMeasurerTest {

	@Test
	public void testHookLengthCalculatedInSquareCell() throws Exception {
		IAnalysisDataset dataset = new TestDatasetBuilder(123).cellCount(1)
				.baseHeight(40).baseWidth(40).profiled().build();
				
		Nucleus n = dataset.getCollection().getCells().get(0).getPrimaryNucleus();
		
		int midpoint = n.getBorderLength()/2;
		
		n.setLandmark(Landmark.TOP_VERTICAL, 0);
		n.setLandmark(Landmark.BOTTOM_VERTICAL, midpoint);
		
		assertFalse(Statistical.STAT_NOT_CALCULATED==ComponentMeasurer.calculate(Measurement.HOOK_LENGTH, n));
		assertFalse(Statistical.ERROR_CALCULATING_STAT==ComponentMeasurer.calculate(Measurement.HOOK_LENGTH, n));
	}
	
	@Test
	public void testBodyWidthCalculatedInSquareCell() throws Exception {
		IAnalysisDataset dataset = new TestDatasetBuilder(123).cellCount(1)
				.baseHeight(40).baseWidth(40).profiled().build();
				
		Nucleus n = dataset.getCollection().getCells().get(0).getPrimaryNucleus();
		
		int midpoint = n.getBorderLength()/2;
		
		n.setLandmark(Landmark.TOP_VERTICAL, 0);
		n.setLandmark(Landmark.BOTTOM_VERTICAL, midpoint);
		
		assertFalse(Statistical.STAT_NOT_CALCULATED==ComponentMeasurer.calculate(Measurement.BODY_WIDTH, n));
		assertFalse(Statistical.ERROR_CALCULATING_STAT==ComponentMeasurer.calculate(Measurement.BODY_WIDTH, n));
	}
	
	@Test
	public void testHookLengthCalculatedInMouseSperm() throws Exception {
		IAnalysisDataset dataset = SampleDatasetReader.openTestRodentDataset();
				
		Nucleus n = dataset.getCollection().getCells().get(0).getPrimaryNucleus();
				
		assertTrue(n.hasLandmark(Landmark.TOP_VERTICAL));
		assertTrue(n.hasLandmark(Landmark.BOTTOM_VERTICAL));
		
		assertFalse(Statistical.STAT_NOT_CALCULATED==ComponentMeasurer.calculate(Measurement.HOOK_LENGTH, n));
		assertFalse(Statistical.ERROR_CALCULATING_STAT==ComponentMeasurer.calculate(Measurement.HOOK_LENGTH, n));
	}
	
	@Test
	public void testBodyWidthCalculatedInMouseSperm() throws Exception {
		IAnalysisDataset dataset = SampleDatasetReader.openTestRodentDataset();
				
		Nucleus n = dataset.getCollection().getCells().get(0).getPrimaryNucleus();
				
		assertTrue(n.hasLandmark(Landmark.TOP_VERTICAL));
		assertTrue(n.hasLandmark(Landmark.BOTTOM_VERTICAL));
		
		assertFalse(Statistical.STAT_NOT_CALCULATED==ComponentMeasurer.calculate(Measurement.BODY_WIDTH, n));
		assertFalse(Statistical.ERROR_CALCULATING_STAT==ComponentMeasurer.calculate(Measurement.BODY_WIDTH, n));
	}

}
