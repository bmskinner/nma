package com.bmskinner.nuclear_morphology.analysis.signals;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

public class SignalDetectorTest {

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
		
		assertTrue(signals>0);
	}

}
