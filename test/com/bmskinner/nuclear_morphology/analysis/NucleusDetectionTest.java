package com.bmskinner.nuclear_morphology.analysis;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestImageDatasetCreator;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;

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
	
	static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);
	
	static {
		for(Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}
		
	@Before
	public void setUp() throws Exception{
	}
	
	@Test
	public void testAllNuclearParametersCalculated() throws Exception {
		
		File testFolder = TestResources.TESTING_MOUSE_FOLDER.getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	IAnalysisDataset d = TestImageDatasetCreator.createTestDataset(TestResources.UNIT_TEST_FOLDER, op, false);
		
		for(Measurement stat : Measurement.getNucleusStats()) {
			double value = d.getCollection().getMedian(stat, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
			assertFalse("Error calculating "+stat, Statistical.ERROR_CALCULATING_STAT==value);
			assertFalse("Did not calculate "+stat, Statistical.STAT_NOT_CALCULATED==value);
		}
	}

}
