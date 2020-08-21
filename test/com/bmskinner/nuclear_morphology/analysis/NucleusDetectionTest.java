package com.bmskinner.nuclear_morphology.analysis;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
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
	
protected static final Logger LOGGER = Logger.getLogger(NucleusDetectionTest.class.getName());
	
	@Before
	public void setUp() throws Exception{
//		for(Handler h : LOGGER.getHandlers())
//			LOGGER.removeHandler(h);
//		Handler h = new ConsoleHandler(new ConsoleFormatter());
//		LOGGER.setLevel(Level.FINER);
//		h.setLevel(Level.FINER);
//		LOGGER.addHandler(h);
		
	}
	
	@Test
	public void testAllNuclearParametersCalculated() throws Exception {
		File saveFile = new File(TestResources.MOUSE_TEST_DATASET);
		IAnalysisDataset test = SampleDatasetReader.openDataset(saveFile);
		
		for(PlottableStatistic stat : PlottableStatistic.getNucleusStats()) {
			double value = test.getCollection().getMedian(stat, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
			assertFalse("Error calculating "+stat, Statistical.ERROR_CALCULATING_STAT==value);
			assertFalse("Did not calculate "+stat, Statistical.STAT_NOT_CALCULATED==value);
		}
	}

}
