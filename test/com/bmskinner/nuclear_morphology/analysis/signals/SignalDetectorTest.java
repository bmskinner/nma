package com.bmskinner.nuclear_morphology.analysis.signals;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;

public class SignalDetectorTest {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);
	
	static {
		for(Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINER);
		h.setLevel(Level.FINER);
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
		
		HashOptions o = OptionsFactory.makeNuclearSignalOptions(TestResources.TESTING_MOUSE_SIGNALS_FOLDER);
		o.setDouble(HashOptions.SIGNAL_MAX_FRACTION, 0.5);
		
		SignalDetector sd = new SignalDetector(o);

		int signals = 0;
		for(File f : d.getCollection().getImageFiles()) {
			for(Nucleus n : d.getCollection().getNuclei(f)) {
				assertTrue(f.exists());				
				List<INuclearSignal> s = sd.detectSignal(f.getAbsoluteFile(), n);
				signals += s.size();
			}
		}
		LOGGER.fine("Found "+signals+" signals");
		assertTrue(signals>0);
	}

}
