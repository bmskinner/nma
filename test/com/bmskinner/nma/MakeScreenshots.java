package com.bmskinner.nma;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nma.doc.Screenshotter;
import com.bmskinner.nma.logging.ConsoleFormatter;
import com.bmskinner.nma.logging.ConsoleHandler;


/**
 * This suite runs the test file creators, then takes screenshots for inclusion
 * in the user guide
 * 
 * @author Ben Skinner
 * @since 2.0.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
		TestResources.class, // ensure directory structure is created
		TestImageDatasetCreator.class, // ensure sample datasets are present
		Screenshotter.class // take the screenshots
})
public class MakeScreenshots {

	static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	static {
		for (final Handler h : LOGGER.getHandlers()) {
			LOGGER.removeHandler(h);
		}
		final Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

}
