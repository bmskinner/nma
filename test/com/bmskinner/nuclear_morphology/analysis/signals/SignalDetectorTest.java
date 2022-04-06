package com.bmskinner.nuclear_morphology.analysis.signals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Test that the signal detector is able to find signals
 * in test images
 * @author ben
 * @since 2.0.0
 *
 */
public class SignalDetectorTest {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);
	
	static {
		for(Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

	/**
	 * Given a folder of images known to contain detectable
	 * signals, test that the signal detector can find them
	 * @throws Exception
	 */
	@Test
	public void testSignalFound() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openTestMouseSignalsDataset();
		
		HashOptions o = OptionsFactory.makeNuclearSignalOptions(TestResources.MOUSE_SIGNALS_INPUT_FOLDER)
				.withValue(HashOptions.SIGNAL_MAX_FRACTION, 0.5)
				.build();
		
		SignalDetector sd = new SignalDetector(o);
		
		File testFile = d.getCollection().getImageFiles().stream()
				.filter(f->f.getName().equals("P104.tiff"))
				.findFirst().orElseThrow(Exception::new);

		// Check we have nucleus detection working ok
		assertEquals("File should have 2 nuclei", 2, d.getCollection().getNuclei(testFile).size());

		
		int signals = 0;
		for(Nucleus n : d.getCollection().getNuclei(testFile)) {
			assertTrue(testFile.exists());				
			List<INuclearSignal> s = sd.detectSignal(testFile.getAbsoluteFile(), n);
			signals += s.size();
		}

		assertEquals("File should have signals", 3, signals);
	}

}
