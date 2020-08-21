package com.bmskinner.nuclear_morphology;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nuclear_morphology.analysis.AnalysisTestSuite;
import com.bmskinner.nuclear_morphology.api.ApiTestSuite;
import com.bmskinner.nuclear_morphology.components.ComponentTestSuite;
import com.bmskinner.nuclear_morphology.io.IoTestSuite;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This suite runs the test file creators, then runs the
 * tests that depend on them
 * @author ben
 * @since 1.14.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ 
	TestImageDatasetCreator.class, // make test datasets for subsequent tests to read 
	AnalysisTestSuite.class,
	ApiTestSuite.class,
	ComponentTestSuite.class,
	IoTestSuite.class
	})
public class CreateAllTestFilesAndRunTests {
	
	static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);
	
	static {
		for(Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

}
