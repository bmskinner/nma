package com.bmskinner.nma.analysis.detection;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.logging.ConsoleFormatter;
import com.bmskinner.nma.logging.ConsoleHandler;

import ij.Prefs;

public class TextFileNucleusFinderTest {
	private static final Logger LOGGER = Logger.getLogger(TextFileNucleusFinderTest.class.getName());

	@Before
	public void setUp() {
		Prefs.setThreads(2); // Attempt to avoid issue 162
		for (final Handler h : LOGGER.getHandlers()) {
			LOGGER.removeHandler(h);
		}
		final Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

	/**
	 * This test is based on a text conversion of our standard mouse test dataset
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMouseOutlinesAnalysed() throws Exception {
		final IAnalysisOptions op = OptionsFactory
				.makeDefaultRodentAnalysisOptions(TestResources.TEXT_OUTLINES_FOLDER);
		final HashOptions nucleus = op.getDetectionOptions(CellularComponent.NUCLEUS).get();
		nucleus.setDouble(HashOptions.MIN_CIRC, 0.15);
		nucleus.setDouble(HashOptions.MAX_CIRC, 1);

		nucleus.setInt(HashOptions.MIN_SIZE_PIXELS, 500);
		nucleus.setInt(HashOptions.MAX_SIZE_PIXELS, 10000);

		final TextFileNucleusFinder f = new TextFileNucleusFinder(op);

		final Collection<ICell> cells = f
				.findInFile(new File(TestResources.TEXT_OUTLINES_FOLDER, "Mouse_text_outlines.txt"));

		assertTrue("Cells should be detected", cells.size() > 0);
	}

	/**
	 * This test is based on a subset of maize stomata data
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRoundOutlinesAnalysed() throws Exception {
		final IAnalysisOptions op = OptionsFactory
				.makeDefaultRoundAnalysisOptions(TestResources.TEXT_OUTLINES_FOLDER);
		final HashOptions nucleus = op.getDetectionOptions(CellularComponent.NUCLEUS).get();
		nucleus.setDouble(HashOptions.MIN_CIRC, 0.15);
		nucleus.setDouble(HashOptions.MAX_CIRC, 1);

		nucleus.setInt(HashOptions.MIN_SIZE_PIXELS, 500);
		nucleus.setInt(HashOptions.MAX_SIZE_PIXELS, 10000);

		final TextFileNucleusFinder f = new TextFileNucleusFinder(op);

		final Collection<ICell> cells = f
				.findInFile(new File(TestResources.TEXT_OUTLINES_FOLDER, "Maize_text_outlines.txt"));

		assertTrue("Cells should be detected", cells.size() > 0);
	}

}
