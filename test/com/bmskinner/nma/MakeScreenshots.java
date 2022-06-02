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
import com.bmskinner.nma.logging.Loggable;

/**
 * This suite runs the test file creators, then takes screenshots for inclusion
 * in the user guide
 * 
 * @author ben
 * @since 2.0.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
		TestImageDatasetCreator.class,
		Screenshotter.class
})
public class MakeScreenshots {

	static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);

	static {
		for (Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

}
