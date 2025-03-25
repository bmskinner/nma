package com.bmskinner.nma.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.jdom2.Document;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.logging.ConsoleFormatter;
import com.bmskinner.nma.logging.ConsoleHandler;


import ij.Prefs;

/**
 * Test the remapping works
 * 
 * @author Ben Skinner
 *
 */
public class XMLLandmarkRemappingMethodTest {

	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	@Before
	public void setUp() {
		Prefs.setThreads(2); // Attempt to avoid issue 162
		for (Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

	@Test
	public void test() throws Exception {
		File sourceFile = new File("test/samples/datasets/Mouse_with_clusters_source.nmd");
		File targetFile = new File("test/samples/datasets/Mouse_with_clusters_target.nmd");
		File outputFile = new File("test/samples/datasets/Mouse_with_clusters_updated.nmd");

		Document source = XMLReader.readDocument(sourceFile);
		Document target = XMLReader.readDocument(targetFile);

		new XMLLandmarkRemappingMethod(source, target, outputFile).call();

		IAnalysisDataset d = SampleDatasetReader.openDataset(outputFile);

		Optional<Nucleus> n = d.getCollection()
				.getNucleus(UUID.fromString("217743d1-bacf-4b40-8381-7038bf2e9ec0"));

		assertTrue(n.isPresent());

		int rp = n.get().getBorderIndex(OrientationMark.REFERENCE);

		assertEquals(190, rp);

	}

}
