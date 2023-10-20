package com.bmskinner.nma;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bmskinner.nma.analysis.classification.NucleusClusteringMethod;
import com.bmskinner.nma.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nma.analysis.nucleus.NucleusDetectionMethod;
import com.bmskinner.nma.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nma.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nma.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nma.analysis.signals.SignalDetectionMethod;
import com.bmskinner.nma.analysis.signals.SignalWarpingMethod;
import com.bmskinner.nma.analysis.signals.shells.ShellAnalysisMethod;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.gui.tabs.signals.warping.SignalWarpingRunSettings;
import com.bmskinner.nma.io.DatasetExportMethod;
import com.bmskinner.nma.io.DatasetOptionsExportMethod;
import com.bmskinner.nma.io.SampleDatasetReader;
import com.bmskinner.nma.logging.ConsoleFormatter;
import com.bmskinner.nma.logging.ConsoleHandler;
import com.bmskinner.nma.logging.Loggable;

import ij.Prefs;

/**
 * Generate the test datasets for the current version, which will be used for
 * comparison testing with older versions by other classes. This class should be
 * run first in test suites.
 * 
 * @author ben
 * @since 1.14.0
 *
 */
public class TestImageDatasetCreator {

	private static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);

	public static final UUID RED_SIGNAL_ID = UUID
			.fromString("00000000-0000-0000-0000-100000000001");
	public static final UUID GREEN_SIGNAL_ID = UUID
			.fromString("00000000-0000-0000-0000-100000000002");

	public static final String RED_SIGNAL_NAME = "Test red";
	public static final String GREEN_SIGNAL_NAME = "Test green";

	/**
	 * Remove any previous copies of the unit test output folders
	 * 
	 * @throws IOException
	 */
	@BeforeClass
	public static void removeUnitTestFolders() throws IOException {
		deleteContents(TestResources.DATASET_FOLDER);

		deleteContents(TestResources.MOUSE_OUTPUT_FOLDER);
		deleteContents(TestResources.PIG_OUTPUT_FOLDER);
		deleteContents(TestResources.ROUND_OUTPUT_FOLDER);
		deleteContents(TestResources.MULTIPLE_BASE_OUTPUT_FOLDER);

		deleteContents(TestResources.MOUSE_SIGNALS_OUTPUT_FOLDER);
		deleteContents(TestResources.PIG_SIGNALS_OUTPUT_FOLDER);
		deleteContents(TestResources.ROUND_SIGNALS_OUTPUT_FOLDER);

		deleteContents(TestResources.MOUSE_CLUSTERS_OUTPUT_FOLDER);
		deleteContents(TestResources.PIG_CLUSTERS_OUTPUT_FOLDER);
		deleteContents(TestResources.ROUND_CLUSTERS_OUTPUT_FOLDER);

	}

	private static void deleteContents(File f) throws IOException {
		if (!f.exists())
			return;
		for (File file : f.listFiles()) {
			if (file.isDirectory())
				deleteContents(file);
			Files.delete(file.toPath());
		}
	}

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
	public void createMouseDataset() throws Exception {

		File testFolder = TestResources.MOUSE_INPUT_FOLDER.getAbsoluteFile();
		IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
		IAnalysisDataset d = createTestDataset(TestResources.MOUSE_OUTPUT_FOLDER, op, false);
		saveTestDataset(d, TestResources.MOUSE_TEST_DATASET);
		testUnmarshalling(d, TestResources.MOUSE_TEST_DATASET);
	}

	@Test
	public void createPigDataset() throws Exception {

		File testFolder = TestResources.PIG_INPUT_FOLDER.getAbsoluteFile();
		IAnalysisOptions op = OptionsFactory.makeDefaultPigAnalysisOptions(testFolder);
		IAnalysisDataset d = createTestDataset(TestResources.PIG_OUTPUT_FOLDER, op, false);
		saveTestDataset(d, TestResources.PIG_TEST_DATASET);
		testUnmarshalling(d, TestResources.PIG_TEST_DATASET);
	}

	@Test
	public void createRoundDataset() throws Exception {

		File testFolder = TestResources.ROUND_INPUT_FOLDER.getAbsoluteFile();
		IAnalysisOptions op = OptionsFactory.makeDefaultRoundAnalysisOptions(testFolder);
		IAnalysisDataset d = createTestDataset(TestResources.ROUND_OUTPUT_FOLDER, op, false);
		saveTestDataset(d, TestResources.ROUND_TEST_DATASET);
		testUnmarshalling(d, TestResources.ROUND_TEST_DATASET);
	}

	@Test
	public void createMultipleSource1Dataset() throws Exception {
		IAnalysisOptions op = OptionsFactory
				.makeDefaultRodentAnalysisOptions(TestResources.MULTIPLE_SOURCE_1_FOLDER);
		IAnalysisDataset d = createTestDataset(TestResources.MULTIPLE_SOURCE_1_FOLDER, op, false);
		saveTestDataset(d, TestResources.MULTIPLE1_TEST_DATASET);
		testUnmarshalling(d, TestResources.MULTIPLE1_TEST_DATASET);
	}

	@Test
	public void createMultipleSource2Dataset() throws Exception {
		IAnalysisOptions op = OptionsFactory
				.makeDefaultRodentAnalysisOptions(TestResources.MULTIPLE_SOURCE_2_FOLDER);
		IAnalysisDataset d = createTestDataset(TestResources.MULTIPLE_SOURCE_2_FOLDER, op, false);
		saveTestDataset(d, TestResources.MULTIPLE2_TEST_DATASET);
		testUnmarshalling(d, TestResources.MULTIPLE2_TEST_DATASET);
	}

	@Test
	public void createMouseWithClustersDataset() throws Exception {
		IAnalysisOptions op = OptionsFactory
				.makeDefaultRodentAnalysisOptions(TestResources.MOUSE_CLUSTERS_INPUT_FOLDER);
		IAnalysisDataset d = createTestDataset(TestResources.MOUSE_OUTPUT_FOLDER, op, true);
		saveTestDataset(d, TestResources.MOUSE_CLUSTERS_DATASET);
		testUnmarshalling(d, TestResources.MOUSE_CLUSTERS_DATASET);
	}

	@Test
	public void createPigWithClustersDataset() throws Exception {
		IAnalysisOptions op = OptionsFactory
				.makeDefaultPigAnalysisOptions(TestResources.PIG_CLUSTERS_INPUT_FOLDER);
		IAnalysisDataset d = createTestDataset(TestResources.PIG_OUTPUT_FOLDER, op, true);
		saveTestDataset(d, TestResources.PIG_CLUSTERS_DATASET);
		testUnmarshalling(d, TestResources.PIG_CLUSTERS_DATASET);
	}

	@Test
	public void createRoundWithClustersDataset() throws Exception {
		IAnalysisOptions op = OptionsFactory
				.makeDefaultRoundAnalysisOptions(TestResources.ROUND_CLUSTERS_INPUT_FOLDER);
		IAnalysisDataset d = createTestDataset(TestResources.ROUND_OUTPUT_FOLDER, op, true);
		saveTestDataset(d, TestResources.ROUND_CLUSTERS_DATASET);
		testUnmarshalling(d, TestResources.ROUND_CLUSTERS_DATASET);
	}

	@Test
	public void createMouseWithSignalsDataset() throws Exception {
		IAnalysisOptions op = OptionsFactory
				.makeDefaultRodentAnalysisOptions(TestResources.MOUSE_SIGNALS_INPUT_FOLDER);
		HashOptions nucleus = op.getNucleusDetectionOptions().get();
		nucleus.setDouble(HashOptions.MIN_CIRC, 0.15);
		nucleus.setDouble(HashOptions.MAX_CIRC, 0.85);

		nucleus.setInt(HashOptions.MIN_SIZE_PIXELS, 2000);
		nucleus.setInt(HashOptions.MAX_SIZE_PIXELS, 10000);

		IAnalysisDataset d = createTestSignalDataset(op, true, false);
		saveTestDataset(d, TestResources.MOUSE_SIGNALS_DATASET);

		// We know what should be detected for these images
		assertEquals("Nucleus count should match", 81, d.getCollection().size());
		assertEquals("Signal count should match", 32,
				d.getCollection().getSignalManager().getSignalCount(RED_SIGNAL_ID));

		testUnmarshalling(d, TestResources.MOUSE_SIGNALS_DATASET);
	}

	@Test
	public void createPigWithSignalsDataset() throws Exception {
		IAnalysisOptions op = OptionsFactory
				.makeDefaultPigAnalysisOptions(TestResources.PIG_SIGNALS_INPUT_FOLDER);
		HashOptions nucleus = op.getDetectionOptions(CellularComponent.NUCLEUS).get();
		nucleus.setInt(HashOptions.MIN_SIZE_PIXELS, 4000);
		nucleus.setInt(HashOptions.MAX_SIZE_PIXELS, 15000);

		IAnalysisDataset d = createTestSignalDataset(op, false, true);
		saveTestDataset(d, TestResources.PIG_SIGNALS_DATASET);
		testUnmarshalling(d, TestResources.PIG_SIGNALS_DATASET);
	}

	@Test
	public void createRoundWithSignalsDataset() throws Exception {
		IAnalysisOptions op = OptionsFactory
				.makeDefaultRoundAnalysisOptions(TestResources.ROUND_SIGNALS_INPUT_FOLDER);
		IAnalysisDataset d = createTestSignalDataset(op, true, true);
		saveTestDataset(d, TestResources.ROUND_SIGNALS_DATASET);
		testUnmarshalling(d, TestResources.ROUND_SIGNALS_DATASET);
	}

	/**
	 * Run a new analysis on the images using the given options.
	 * 
	 * @param op       the analysis options
	 * @param addRed   should red signals be detected with default parameters?
	 * @param addGreen should green signals be detected with default parameters?
	 * @throws Exception
	 */
	public static IAnalysisDataset createTestSignalDataset(IAnalysisOptions op, boolean addRed,
			boolean addGreen) throws Exception {

		File testFolder = op.getNucleusDetectionFolder().get();

		if (!testFolder.exists())
			throw new IllegalArgumentException("Detection folder does not exist");

		IAnalysisDataset d = new NucleusDetectionMethod(testFolder, op).call().getFirstDataset();

		new DatasetProfilingMethod(d)
				.then(new DatasetSegmentationMethod(d, MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH))
				.then(op.getRuleSetCollection().equals(RuleSetCollection.roundRuleSetCollection())
						? new ConsensusAveragingMethod(d) // TODO: replace once new refold method is
															// available
						: new ConsensusAveragingMethod(d))
				.call();

		if (addRed) {
			HashOptions redOptions = OptionsFactory.makeNuclearSignalOptions()
					.withValue(HashOptions.CHANNEL, 0)
					.withValue(HashOptions.THRESHOLD, 70)
					.withValue(HashOptions.MIN_SIZE_PIXELS, 5)
					.withValue(HashOptions.SIGNAL_MAX_FRACTION, 0.5)
					.withValue(HashOptions.SIGNAL_GROUP_NAME, RED_SIGNAL_NAME)
					.withValue(HashOptions.SIGNAL_GROUP_ID, RED_SIGNAL_ID.toString())
					.build();

			new SignalDetectionMethod(d, redOptions, testFolder).call();
			assertTrue("Dataset should have red signals",
					d.getCollection().hasSignalGroup(RED_SIGNAL_ID));
			assertTrue("Dataset should have red signals",
					d.getCollection().getSignalManager().getSignalCount(RED_SIGNAL_ID) > 0);

			SignalWarpingRunSettings sw = new SignalWarpingRunSettings(d, d, RED_SIGNAL_ID);
			sw.setInt(SignalWarpingRunSettings.MIN_THRESHOLD_KEY, 70);
			new SignalWarpingMethod(d, sw).call();

		}

		if (addGreen) {
			HashOptions greenOptions = OptionsFactory.makeNuclearSignalOptions()
					.withValue(HashOptions.CHANNEL, 1)
					.withValue(HashOptions.THRESHOLD, 70)
					.withValue(HashOptions.MIN_SIZE_PIXELS, 5)
					.withValue(HashOptions.SIGNAL_MAX_FRACTION, 0.5)
					.withValue(HashOptions.SIGNAL_GROUP_NAME, GREEN_SIGNAL_NAME)
					.withValue(HashOptions.SIGNAL_GROUP_ID, GREEN_SIGNAL_ID.toString())
					.build();

			new SignalDetectionMethod(d, greenOptions, testFolder).call();
			assertTrue("Dataset should have green signals",
					d.getCollection().hasSignalGroup(GREEN_SIGNAL_ID));
			assertTrue("Dataset should have green signals",
					d.getCollection().getSignalManager().getSignalCount(GREEN_SIGNAL_ID) > 0);

			SignalWarpingRunSettings sw = new SignalWarpingRunSettings(d, d, GREEN_SIGNAL_ID);
			sw.setInt(SignalWarpingRunSettings.MIN_THRESHOLD_KEY, 70);
			new SignalWarpingMethod(d, sw).call();
		}

		new ShellAnalysisMethod(d, OptionsFactory.makeShellAnalysisOptions().build()).call();
		return d;
	}

	/**
	 * Create and run an analysis
	 * 
	 * @param folder       the output folder for save files
	 * @param op           the analysis options
	 * @param makeClusters should clusters be created
	 * @return a dataset with the results of the analysis
	 * @throws Exception if anything goes wrong
	 */
	public static IAnalysisDataset createTestDataset(File outputFolder, IAnalysisOptions op,
			boolean makeClusters) throws Exception {

		File inputFolder = op.getNucleusDetectionFolder().get();
		if (!inputFolder.exists())
			throw new IllegalArgumentException(
					"Input folder does not exist: " + inputFolder.getAbsolutePath());

		IAnalysisDataset d = new NucleusDetectionMethod(outputFolder.getAbsoluteFile(), op).call()
				.getFirstDataset();

		HashOptions clusterOptions = OptionsFactory.makeDefaultClusteringOptions().build();

		new DatasetProfilingMethod(d)
				.then(new DatasetSegmentationMethod(d, MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH))
				.then(op.getRuleSetCollection().equals(RuleSetCollection.roundRuleSetCollection())
						? new ConsensusAveragingMethod(d) // TODO: temp replacement before the new
															// ProfileRefoldMethod is written
						: new ConsensusAveragingMethod(d))
				.thenIf(makeClusters, new NucleusClusteringMethod(d, clusterOptions))
				.call();

		if (makeClusters) {
			for (IAnalysisDataset child : d.getAllChildDatasets())
				new ConsensusAveragingMethod(child).call();
			assertFalse("Dataset should have clusters", d.getClusterGroups().isEmpty());
		}
		return d;
	}

	/**
	 * Save the given dataset to the desired output folder, and create a backup file
	 * in the appropriate {@link TestResources#DATASET_FOLDER}.
	 * 
	 * @param d        the dataset to save
	 * @param saveFile the full path to the nmd file
	 * @return the new dataset
	 * @throws Exception
	 */
	public static void saveTestDataset(IAnalysisDataset d, File saveFile) throws Exception {
		if (saveFile.exists())
			saveFile.delete();

		assertFalse("Expecting output file to be deleted: " + saveFile.getAbsolutePath(),
				saveFile.exists());
		new DatasetExportMethod(d, saveFile).call();

		assertTrue("Expecting file saved to " + saveFile.getAbsolutePath(), saveFile.exists());

		// Copy the saved file into backup file for comparison and conversion testing in
		// the next version.
		String bakName = saveFile.getAbsolutePath().replaceAll(".nmd$", ".bak");
		File bakFile = new File(bakName);
		if (!bakFile.getParentFile().exists())
			bakFile.getParentFile().mkdirs();
		if (bakFile.exists())
			bakFile.delete();
		assertFalse("Expecting backup file to be deleted: " + bakFile.getAbsolutePath(),
				bakFile.exists());
		Files.copy(saveFile.getAbsoluteFile().toPath(), bakFile.getAbsoluteFile().toPath(),
				StandardCopyOption.COPY_ATTRIBUTES);
		assertTrue("Expecting backup copied to " + bakFile.getAbsolutePath(), bakFile.exists());

		// Create an xml representation of the analysis options for pipeline testing
		String xmlName = saveFile.getAbsolutePath().replaceAll(".nmd$", ".options.xml");

		File xmlFile = new File(xmlName);
		if (xmlFile.exists())
			xmlFile.delete();

		assertFalse("Expecting xml file to be deleted: " + xmlFile.getAbsolutePath(),
				xmlFile.exists());

		new DatasetOptionsExportMethod(d, xmlFile).call();

		assertTrue("Expecting xml exported to " + xmlFile.getAbsolutePath(), xmlFile.exists());
	}

	/**
	 * Profiles are regenerated from raw values when datasets are read. Check that
	 * the recalculated values match what is expected. Opens a saved dataset, and
	 * compares it to a fresh analysis of the same files.
	 * 
	 * @throws Exception
	 */
	public static void testUnmarshalling(IAnalysisDataset d, File saveFile) throws Exception {
		IAnalysisDataset t = SampleDatasetReader.openDataset(saveFile);
		LOGGER.fine("Sample dataset opened: " + saveFile.getName());
		ComponentTester.testDuplicatesByField(d.getName(), d, t);
		assertEquals("Datasets should match", d, t);
		assertEquals("Hashcodes should match", d.hashCode(), t.hashCode());
	}
}
