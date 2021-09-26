package com.bmskinner.nuclear_morphology.analysis;

import static org.junit.Assert.assertFalse;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

/**
 * Test that nuclei in an image are detected,
 * and all parameters are calculated. For mouse
 * sperm, this includes the hook length and body
 * width
 * @author bs19022
 * @since 1.17.1
 *
 */
public class NucleusDetectionTest {
		
	@Before
	public void setUp() throws Exception{
	}
	
	@Test
	public void testAllNuclearParametersCalculated() throws Exception {
		File saveFile = new File(TestResources.MOUSE_TEST_DATASET);
		IAnalysisDataset test = SampleDatasetReader.openDataset(saveFile);
		
		for(Measurement stat : Measurement.getNucleusStats()) {
			double value = test.getCollection().getMedian(stat, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
			assertFalse("Error calculating "+stat, Statistical.ERROR_CALCULATING_STAT==value);
			assertFalse("Did not calculate "+stat, Statistical.STAT_NOT_CALCULATED==value);
		}
	}

}
