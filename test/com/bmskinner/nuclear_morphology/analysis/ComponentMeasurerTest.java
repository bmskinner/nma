package com.bmskinner.nuclear_morphology.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.components.ComponentMeasurer;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
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

		assertFalse(Statistical.ERROR_CALCULATING_STAT==ComponentMeasurer.calculate(Measurement.BODY_WIDTH, n));
	}
	
	@Test
	public void testHookLengthCalculatedInMouseSperm() throws Exception {
		IAnalysisDataset dataset = SampleDatasetReader.openTestRodentDataset();
				
		Nucleus n = dataset.getCollection().getCells().get(0).getPrimaryNucleus();
				
		assertTrue(n.hasLandmark(Landmark.TOP_VERTICAL));
		assertTrue(n.hasLandmark(Landmark.BOTTOM_VERTICAL));
		
		assertFalse(Statistical.ERROR_CALCULATING_STAT==ComponentMeasurer.calculate(Measurement.HOOK_LENGTH, n));
	}
	
	@Test
	public void testBodyWidthCalculatedInMouseSperm() throws Exception {
		IAnalysisDataset dataset = SampleDatasetReader.openTestRodentDataset();
				
		Nucleus n = dataset.getCollection().getCells().get(0).getPrimaryNucleus();
				
		assertTrue(n.hasLandmark(Landmark.TOP_VERTICAL));
		assertTrue(n.hasLandmark(Landmark.BOTTOM_VERTICAL));
		
		assertFalse(Statistical.ERROR_CALCULATING_STAT==ComponentMeasurer.calculate(Measurement.BODY_WIDTH, n));
	}
	
	@Test
	public void testMeasurementsAreConsistent() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(ComponentTester.RNG_SEED)
				.baseHeight(100)
				.baseWidth(150)
				.cellCount(500)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.build();
		// These are the measurements that don't depend on landmarks 
    	List<@NonNull Measurement> toTest = List.of(Measurement.AREA, Measurement.PERIMETER, 
    			Measurement.CIRCULARITY, Measurement.MIN_DIAMETER);
    	
    	// Store the results before profiling for comparison
    	Map<UUID, Map<Measurement, Double>> pre = new HashMap<>();
    	
    	// Check the border list has not changed either
    	Map<UUID, List<IPoint>> borders = new HashMap<>();

    	for(Nucleus n : d.getCollection().getNuclei()) {
    		Map<Measurement, Double> mes = new HashMap<>();
    		for(Measurement stat : toTest) {
        		if(Measurement.VARIABILITY.equals(stat))
    				continue; // we can't test this on a per-nucleus level
        		mes.put(stat, n.getMeasurement(stat));
        	}
    		// Store the current values
    		pre.put(n.getID(), mes);
    		
    		borders.put(n.getID(), n.getBorderList());

    		
        	// Check current values match the stored values before we profile
    		// Sanity check that nothing non-deterministic is happening
			assertEquals("Border list should not change", borders.get(n.getID()),
					n.getBorderList());

    		for(Measurement stat : toTest) {
    			if(Measurement.VARIABILITY.equals(stat))
    				continue; // we can't test this on a per-nucleus level
    			assertEquals(stat+" should not change", pre.get(n.getID()).get(stat), 
    					n.getMeasurement(stat), 0.0000001);
        	}
    	}
    	

		new DatasetProfilingMethod(d).call();
		

		// Check each of the saved results against their current value.
		// Nothing should have changed in the test measurements.
		for(UUID id : pre.keySet()) {
			Nucleus n = d.getCollection().getNucleus(id).orElseThrow(NullPointerException::new);
			
			assertEquals("Border list should not change", borders.get(n.getID()),
					n.getBorderList());
			
			// Check if any values have changed
			for(Measurement stat : toTest) {
				if(Measurement.VARIABILITY.equals(stat))
					continue; // we can't test this on a per-nucleus level

				assertEquals(stat+" should not change in "+n.getNameAndNumber(), pre.get(id).get(stat), 
						n.getMeasurement(stat), 0.0000001);

	    	}
			
			
		}
	}

}
