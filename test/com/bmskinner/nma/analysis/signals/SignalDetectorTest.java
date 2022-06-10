package com.bmskinner.nma.analysis.signals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import com.bmskinner.nma.analysis.detection.Detector;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.io.SampleDatasetReader;
import com.bmskinner.nma.logging.ConsoleFormatter;
import com.bmskinner.nma.logging.ConsoleHandler;
import com.bmskinner.nma.logging.Loggable;

import ij.gui.Roi;
import ij.process.ImageProcessor;

/**
 * Test that the signal detector is able to find signals in test images
 * 
 * @author ben
 * @since 2.0.0
 *
 */
public class SignalDetectorTest {

	private static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);

	static {
		for (Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

	/**
	 * Given a folder of images known to contain detectable signals, test that the
	 * signal detector can find them
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSignalFound() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openTestMouseSignalsDataset();

		HashOptions o = OptionsFactory.makeNuclearSignalOptions()
				.withValue(HashOptions.SIGNAL_MAX_FRACTION, 0.5)
				.build();

		File testFile = d.getCollection().getImageFiles().stream()
				.filter(f -> f.getName().equals("P104.tiff"))
				.findFirst().orElseThrow(Exception::new);

		// Check we have nucleus detection working ok
		assertEquals("File should have 2 nuclei", 2, d.getCollection().getNuclei(testFile).size());

		int signals = 0;
		for (Nucleus n : d.getCollection().getNuclei(testFile)) {
			assertTrue(testFile.exists());
			ImageProcessor ip = new ImageImporter(testFile)
					.importImage(o.getInt(HashOptions.CHANNEL));

			ip.threshold(o.getInt(HashOptions.THRESHOLD));

			Map<Roi, IPoint> rois = new Detector().getValidRois(ip, o, n);
			signals += rois.size();
		}

		assertEquals("File should have signals", 3, signals);
	}

}
