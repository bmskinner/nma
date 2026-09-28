package com.bmskinner.nma.pipelines;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.analysis.detection.TextFileNucleusFinder;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.logging.ConsoleFormatter;
import com.bmskinner.nma.logging.ConsoleHandler;

public class TextOutlineAnalysisPipelineTest extends AnalysisPipelineTest {

	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	static {
		for (final Handler h : LOGGER.getHandlers()) {
			LOGGER.removeHandler(h);
		}
		final Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	/**
	 * Run detection from the given text outline file using the given options.
	 * 
	 * @param textFile the file of outlines matching the format in
	 *                 {@link TextFileNucleusFinder}
	 * @param xmlFile  the analysis options xml file
	 * @throws Exception
	 */
	protected void testPipelineCreatesReadableExportFile(File textFile, File xmlFile)
			throws Exception {
		assertTrue("Input image folder %s should exist".formatted(textFile.getAbsolutePath()), textFile.exists());
		assertTrue("XML options file %s should exist".formatted(xmlFile.getAbsolutePath()), xmlFile.exists());

		final File outputFolder = TestResources.DATASET_FOLDER.getAbsoluteFile();

		new TextOutlineAnalysisPipeline(textFile, xmlFile, outputFolder).call();
		assertTrue("Analysis output folder %s should exist".formatted(outputFolder.getAbsolutePath()),
				outputFolder.exists());

		final File expectedFile = new File(outputFolder, textFile.getName() + Io.NMD_FILE_EXTENSION);
		assertTrue("Output file %s should exist".formatted(expectedFile.getAbsolutePath()),
				expectedFile.exists());
		assertTrue(validateDataset(expectedFile));
	}

	@Test
	public void testCreateMouseDataset() throws Exception {
		final File textFile = new File(TestResources.TEXT_OUTLINES_FOLDER, "Mouse_text_outlines.txt");
		final File xmlFile = new File(TestResources.TEXT_OUTLINES_FOLDER, "Mouse.options.xml");
		testPipelineCreatesReadableExportFile(textFile, xmlFile);
	}

	@Test
	public void testCreateMaizeDataset() throws Exception {
		final File textFile = new File(TestResources.TEXT_OUTLINES_FOLDER, "Maize_text_outlines.txt");
		final File xmlFile = new File(TestResources.TEXT_OUTLINES_FOLDER, "Maize.options.xml");
		testPipelineCreatesReadableExportFile(textFile, xmlFile);
	}
}
