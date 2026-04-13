package com.bmskinner.nma.analysis.classification;

import static org.junit.Assert.fail;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
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
		final IAnalysisDataset d = SampleDatasetReader.openTestMouseDataset();

		final NonunimodalRegionClusteringMethod nrcm = new NonunimodalRegionClusteringMethod();
		nrcm.findNonUnimodalProfileRegions(d);
		fail("Not yet implemented");
	}

}
