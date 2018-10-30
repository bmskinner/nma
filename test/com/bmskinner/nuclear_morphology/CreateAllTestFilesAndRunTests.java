package com.bmskinner.nuclear_morphology;

import java.util.logging.Logger;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nuclear_morphology.analysis.AnalysisTestSuite;
import com.bmskinner.nuclear_morphology.api.ApiTestSuite;
import com.bmskinner.nuclear_morphology.components.ComponentTestSuite;
import com.bmskinner.nuclear_morphology.io.IoTestSuite;
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
	
	static final Logger logger = Logger.getLogger(Loggable.CONSOLE_LOGGER);

}
