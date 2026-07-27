package com.bmskinner.nma.analysis.classification;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.TestImageDatasetCreator;
import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.io.SampleDatasetReader;
import com.bmskinner.nma.logging.ConsoleFormatter;
import com.bmskinner.nma.logging.ConsoleHandler;

public class NonUnimodalRegionClusteringMethodTest {
	private static final Logger LOGGER = Logger.getLogger("com.bmskinner.nma");

	@Before
	public void setUp() {
		LogManager.getLogManager().reset();
		LOGGER.setLevel(Level.FINE);
		final Handler h = new ConsoleHandler(new ConsoleFormatter());
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

	@Test
	public void testRegionsIdentifed() throws Exception {
		LOGGER.info("Reading dataset");

		final IAnalysisDataset d = SampleDatasetReader.openTestMouseDataset().copy();
		d.setSavePath(new File(TestResources.MOUSE_OUTPUT_FOLDER,
				TestResources.MOUSE + "_Hamming" + Io.NMD_FILE_EXTENSION));
		TestImageDatasetCreator.saveTestDataset(d, d.getSavePath());

		final IAnalysisDataset d1 = SampleDatasetReader.openDataset(d.getSavePath());


		new NonunimodalRegionClusteringMethod(d1).call();

		TestImageDatasetCreator.saveTestDataset(d1, d1.getSavePath());

		// Check XMl conversions worked
		final IAnalysisDataset d2 = SampleDatasetReader.openDataset(d.getSavePath());

		LOGGER.fine(d2.toString());

		fail("Not yet implemented");
	}

}
