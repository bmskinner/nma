package com.bmskinner.nma;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nma.analysis.AnalysisTestSuite;
import com.bmskinner.nma.components.ComponentTestSuite;
import com.bmskinner.nma.io.IoTestSuite;
import com.bmskinner.nma.logging.ConsoleFormatter;
import com.bmskinner.nma.logging.ConsoleHandler;

import com.bmskinner.nma.pipelines.ApiTestSuite;
import com.bmskinner.nma.utility.UtilityTestSuite;

/**
 * This suite runs the test file creators, then runs the tests that depend on
 * them
 * 
 * @author Ben Skinner
 * @since 2.0.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
		TestImageDatasetCreator.class, // make test datasets for subsequent tests to read
		AnalysisTestSuite.class,
		ApiTestSuite.class,
		ComponentTestSuite.class,
		IoTestSuite.class,
		UtilityTestSuite.class
})
public class RunAllTests {

	static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	static {
		for (Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

}
