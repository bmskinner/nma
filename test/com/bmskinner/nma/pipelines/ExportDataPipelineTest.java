package com.bmskinner.nma.pipelines;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.TestImageDatasetCreator;
import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.core.CommandOptions;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.io.SampleDatasetReader;

/**
 * Test the export pipeline is working
 * 
 * @author Ben Skinner
 *
 */
public class ExportDataPipelineTest {

	private static final Logger LOGGER = Logger.getLogger(ExportDataPipelineTest.class.getName());

	private static final File MEASUREMENT_FILE = new File(
			TestResources.MOUSE_SIGNALS_DATASET.getAbsolutePath() + ".measurements" + Io.TAB_FILE_EXTENSION);

	private static final File OUTLINE_FILE = new File(
			TestResources.MOUSE_SIGNALS_DATASET.getAbsolutePath() + ".outlines" + Io.TAB_FILE_EXTENSION);

	private static final File PROFILE_FILE = new File(
			TestResources.MOUSE_SIGNALS_DATASET.getAbsolutePath() + ".profiles" + Io.TAB_FILE_EXTENSION);

	/**
	 * Ensure the test datasets we need have been created if these tests are run
	 * outside the full test pipeline.
	 * 
	 * @throws Exception
	 */
	@Before
	public void createTestDatasets() throws Exception {
		if (!TestResources.MOUSE_SIGNALS_DATASET.exists()) {
			final TestImageDatasetCreator tidc = new TestImageDatasetCreator();
			tidc.createMouseWithSignalsDataset();
		}
	}

	/**
	 * Clear out any existing exported files before each test
	 */
	@Before
	public void removeExistingExportFiles() {
		if (MEASUREMENT_FILE.exists()) {
			MEASUREMENT_FILE.delete();
		}

		// Profiles should also give a file with long format outlines
		if (PROFILE_FILE.exists()) {
			PROFILE_FILE.delete();
		}

		// Outlines should also give a file with long format outlines
		if (OUTLINE_FILE.exists()) {
			OUTLINE_FILE.delete();
		}
	}

	/**
	 * Read the first line of the given input file, and extract column names.
	 * 
	 * @param inputFile the file to read
	 * @param sep       the column separator
	 * @return
	 */

	static List<String> readColumnHeaders(File inputFile, String sep) {
		final List<String> result = new ArrayList<>();

		try (FileInputStream fstream = new FileInputStream(inputFile);
				BufferedReader br = new BufferedReader(new InputStreamReader(fstream, StandardCharsets.ISO_8859_1));) {

			// Read first line only
			final String strLine = br.readLine();
			Collections.addAll(result, strLine.split(Io.TAB));

		} catch (final IOException e) {
			LOGGER.log(Level.SEVERE, "Error reading column headers", e);
		}
		return result;
	}

	/**
	 * Read the first line of the given input file, and extract column names.
	 * Assumes the column separator is a tab.
	 * 
	 * @param inputFile the file to read
	 * @return
	 */
	static List<String> readColumnHeaders(File inputFile) {
		return readColumnHeaders(inputFile, Io.TAB);
	}

	/**
	 * Test if the given observed column headers match the expected column headers.
	 * We test presence, not order of columns
	 * 
	 * @param expectedHeaders the expected column names
	 * @param observedHeaders the actual column names
	 */
	static void testColumnHeadersMatch(List<String> expectedHeaders, List<String> observedHeaders) {
		for (final String expected : expectedHeaders) {
			assertTrue("Expecting %s in header".formatted(expected), observedHeaders.contains(expected));
		}

		for (final String actual : observedHeaders) {
			assertTrue("Not expecting %s in headers".formatted(actual), expectedHeaders.contains(actual));
		}
	}

	/**
	 * Create the base column names expected in a data file output
	 * 
	 * @return
	 */
	static List<String> makeBaseColumnNames() {
		final List<String> expectedHeaders = new ArrayList<>();

		// Define the base columns
		Collections.addAll(expectedHeaders, new String[] {
				"Dataset",
				"File",
				"CellID",
				"Component",
				"ComponentID",
				"Folder",
				"Image",
				"Centre_of_mass",
		});
		return expectedHeaders;
	}

	/**
	 * Create the column names expected for the measurements of a dataset
	 * 
	 * @param dataset
	 * @return
	 */
	static List<String> makeMeasurementColumnNames(IAnalysisDataset dataset) {
		final List<String> expectedHeaders = new ArrayList<>();
		final List<Measurement> measurements = dataset
				.getAnalysisOptions().get()
				.getRuleSetCollection()
				.getMeasurableValues().stream()
				.toList();

		for (final Measurement s : measurements) {

			String label = s.label(MeasurementScale.PIXELS).replace(" ", "_").replace("(", "_")
					.replace(")", "")
					.replace("__", "_");
			expectedHeaders.add(label);

			if (!s.isDimensionless() && !s.isAngle()) { // only give micron
				// measurements when
				// length or area
				label = s.label(MeasurementScale.MICRONS).replace(" ", "_").replace("(", "_")
						.replace(")", "")
						.replace("__", "_");

				expectedHeaders.add(label);
			}
		}
		return expectedHeaders;
	}

	/**
	 * Create the column names expected for the measurements of a dataset
	 * 
	 * @param dataset
	 * @return
	 */
	static List<String> makeProfileColumnNames(IAnalysisDataset dataset) {
		final List<String> expectedHeaders = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			expectedHeaders.add("Angle_profile_" + i);
			expectedHeaders.add("Diameter_profile_" + i);
			expectedHeaders.add("Radius_profile_" + i);
		}

		final int segs = dataset.getCollection().getProfileManager().getSegmentCount();
		for (int i = 0; i < segs; i++) {
			expectedHeaders.add("Length_seg_%s_pixels".formatted(i));
			expectedHeaders.add("Length_seg_%s_microns".formatted(i));
			expectedHeaders.add("Seg_%s_start".formatted(i));
			expectedHeaders.add("Seg_%s_end".formatted(i));
		}

		return (expectedHeaders);
	}

	/**
	 * Create the column names expected for the measurements of a dataset
	 * 
	 * @param dataset
	 * @return
	 */
	static List<String> makeOutlineColumnNames() {
		final List<String> expectedHeaders = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			expectedHeaders.add("Outline_RawCoordinates_X_" + i);
			expectedHeaders.add("Outline_RawCoordinates_Y_" + i);
			expectedHeaders.add("Outline_OrientedCoordinates_X_" + i);
			expectedHeaders.add("Outline_OrientedCoordinates_Y_" + i);
		}

		return (expectedHeaders);
	}

	/**
	 * Create the column names expected for the long profile file
	 * 
	 * @param dataset
	 * @return
	 */
	static List<String> makeLongProfileColumnNames() {
		final List<String> expectedHeaders = new ArrayList<>();
		expectedHeaders.add("Dataset");
		expectedHeaders.add("CellID");
		expectedHeaders.add("Component");
		expectedHeaders.add("Folder");
		expectedHeaders.add("Image");
		expectedHeaders.add("Profile");
		return (expectedHeaders);
	}

	/**
	 * Create the column names expected for the long outline file
	 * 
	 * @param dataset
	 * @return
	 */
	static List<String> makeLongOutlineColumnNames() {
		final List<String> expectedHeaders = new ArrayList<>();
		expectedHeaders.add("Dataset");
		expectedHeaders.add("CellID");
		expectedHeaders.add("Component");
		expectedHeaders.add("ImageFile");
		expectedHeaders.add("RawX");
		expectedHeaders.add("RawY");
		expectedHeaders.add("OrientedX");
		expectedHeaders.add("OrientedY");
		return (expectedHeaders);
	}


	/**
	 * Test the --measurements export flag creates an export file with appropriate
	 * columns.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPipelineExportsMeasurementsData() throws Exception {

		if (MEASUREMENT_FILE.exists()) {
			MEASUREMENT_FILE.delete();
		}

		LOGGER.info("Saving measurements file from %s to %s".formatted(TestResources.MOUSE_SIGNALS_DATASET,
				MEASUREMENT_FILE.getAbsolutePath()));

		final IAnalysisDataset dataset  = SampleDatasetReader.openDataset(TestResources.MOUSE_SIGNALS_DATASET);

		final CommandOptions options = new CommandOptions();
		options.file = TestResources.MOUSE_SIGNALS_DATASET;
		options.isMeasurements = true;

		new ExportDataPipeline(options);
		assertTrue(MEASUREMENT_FILE.exists());
		// Read the real columns exported
		final List<String> colHeaders = readColumnHeaders(MEASUREMENT_FILE);

		// Define the columns expected
		final List<String> expectedHeaders = new ArrayList<>();
		expectedHeaders.addAll(makeBaseColumnNames());
		expectedHeaders.addAll(makeMeasurementColumnNames(dataset));
		testColumnHeadersMatch(expectedHeaders, colHeaders);
	}

	/**
	 * Test the --profiles export flags create an export file with appropriate
	 * columns.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPipelineExportProfileData() throws Exception {

		LOGGER.info("Saving measurements file from %s to %s".formatted(TestResources.MOUSE_SIGNALS_DATASET,
				MEASUREMENT_FILE.getAbsolutePath()));

		final IAnalysisDataset dataset = SampleDatasetReader.openDataset(TestResources.MOUSE_SIGNALS_DATASET);

		final CommandOptions options = new CommandOptions();
		options.file = TestResources.MOUSE_SIGNALS_DATASET;
		options.isProfiles = true;

		new ExportDataPipeline(options);
		assertTrue(MEASUREMENT_FILE.exists());
		// Read the real columns exported
		final List<String> colHeaders = readColumnHeaders(MEASUREMENT_FILE);

		// Define the columns expected
		final List<String> expectedHeaders = new ArrayList<>();
		expectedHeaders.addAll(makeBaseColumnNames());
		expectedHeaders.addAll(makeProfileColumnNames(dataset));
		testColumnHeadersMatch(expectedHeaders, colHeaders);

		// Define the columns expected
		assertTrue(PROFILE_FILE.exists());
		final List<String> longHeaders = readColumnHeaders(PROFILE_FILE);
		final List<String> expectedLongHeaders = new ArrayList<>();
		expectedHeaders.addAll(makeBaseColumnNames());
		expectedLongHeaders.addAll(makeLongProfileColumnNames());
		testColumnHeadersMatch(expectedLongHeaders, longHeaders);

	}

	/**
	 * Test the --outlines export flags create an export file with appropriate
	 * columns.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPipelineExportOutlinesData() throws Exception {

		LOGGER.info("Saving measurements file from %s to %s".formatted(TestResources.MOUSE_SIGNALS_DATASET,
				MEASUREMENT_FILE.getAbsolutePath()));

		final CommandOptions options = new CommandOptions();
		options.file = TestResources.MOUSE_SIGNALS_DATASET;
		options.isOutlines = true;

		new ExportDataPipeline(options);
		assertTrue(MEASUREMENT_FILE.exists());
		// Read the real columns exported
		final List<String> colHeaders = readColumnHeaders(MEASUREMENT_FILE);

		// Define the columns expected
		final List<String> expectedHeaders = new ArrayList<>();
		expectedHeaders.addAll(makeBaseColumnNames());
		expectedHeaders.addAll(makeOutlineColumnNames());
		testColumnHeadersMatch(expectedHeaders, colHeaders);

		// Define the columns expected
		assertTrue(OUTLINE_FILE.exists());
		final List<String> longOutlineHeaders = readColumnHeaders(OUTLINE_FILE);
		final List<String> expectedLongOutlineHeaders = new ArrayList<>();
		expectedLongOutlineHeaders.addAll(makeLongOutlineColumnNames());
		testColumnHeadersMatch(expectedLongOutlineHeaders, longOutlineHeaders);
	}

	/**
	 * Test the --measurements --profiles export flags create an export file with
	 * appropriate columns.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPipelineExportsMeasurementsAndProfileData() throws Exception {

		LOGGER.info("Saving measurements file from %s to %s".formatted(TestResources.MOUSE_SIGNALS_DATASET,
				MEASUREMENT_FILE.getAbsolutePath()));

		final IAnalysisDataset dataset = SampleDatasetReader.openDataset(TestResources.MOUSE_SIGNALS_DATASET);

		final CommandOptions options = new CommandOptions();
		options.file = TestResources.MOUSE_SIGNALS_DATASET;
		options.isMeasurements = true;
		options.isProfiles = true;

		new ExportDataPipeline(options);
		assertTrue(MEASUREMENT_FILE.exists());
		// Read the real columns exported
		final List<String> colHeaders = readColumnHeaders(MEASUREMENT_FILE);

		// Define the columns expected
		final List<String> expectedHeaders = new ArrayList<>();
		expectedHeaders.addAll(makeBaseColumnNames());
		expectedHeaders.addAll(makeMeasurementColumnNames(dataset));
		expectedHeaders.addAll(makeProfileColumnNames(dataset));
		testColumnHeadersMatch(expectedHeaders, colHeaders);

		assertTrue(PROFILE_FILE.exists());
		final List<String> longProfileHeaders = readColumnHeaders(PROFILE_FILE);
		final List<String> expectedLongProfileHeaders = new ArrayList<>();
		expectedLongProfileHeaders.addAll(makeLongProfileColumnNames());
		testColumnHeadersMatch(expectedLongProfileHeaders, longProfileHeaders);
	}

	/**
	 * Test the --measurements --profiles --outlines export flags create an export
	 * file with appropriate columns.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPipelineExportsMeasurementsAndProfileAndOutlinesData() throws Exception {


		LOGGER.info("Saving measurements file from %s to %s".formatted(TestResources.MOUSE_SIGNALS_DATASET,
				MEASUREMENT_FILE.getAbsolutePath()));

		final IAnalysisDataset dataset = SampleDatasetReader.openDataset(TestResources.MOUSE_SIGNALS_DATASET);

		final CommandOptions options = new CommandOptions();
		options.file = TestResources.MOUSE_SIGNALS_DATASET;
		options.isMeasurements = true;
		options.isProfiles = true;
		options.isOutlines = true;

		new ExportDataPipeline(options);
		assertTrue(MEASUREMENT_FILE.exists());
		// Read the real columns exported
		final List<String> colHeaders = readColumnHeaders(MEASUREMENT_FILE);

		// Define the columns expected
		final List<String> expectedHeaders = new ArrayList<>();
		expectedHeaders.addAll(makeBaseColumnNames());
		expectedHeaders.addAll(makeMeasurementColumnNames(dataset));
		expectedHeaders.addAll(makeProfileColumnNames(dataset));
		expectedHeaders.addAll(makeOutlineColumnNames());
		testColumnHeadersMatch(expectedHeaders, colHeaders);

		assertTrue(OUTLINE_FILE.exists());
		final List<String> longOutlineHeaders = readColumnHeaders(OUTLINE_FILE);
		final List<String> expectedLongOutlineHeaders = new ArrayList<>();
		expectedLongOutlineHeaders.addAll(makeLongOutlineColumnNames());
		testColumnHeadersMatch(expectedLongOutlineHeaders, longOutlineHeaders);

		assertTrue(PROFILE_FILE.exists());
		final List<String> longProfileHeaders = readColumnHeaders(PROFILE_FILE);
		final List<String> expectedLongProfileHeaders = new ArrayList<>();
		expectedLongProfileHeaders.addAll(makeLongProfileColumnNames());
		testColumnHeadersMatch(expectedLongProfileHeaders, longProfileHeaders);
	}

	/**
	 * Test the --measurements --profiles --outlines --normalised-only export flags
	 * create an export file with appropriate columns.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPipelineExportsMeasurementsAndProfileAndOutlinesDataNormalisedOnly() throws Exception {

		LOGGER.info("Saving measurements file from %s to %s".formatted(TestResources.MOUSE_SIGNALS_DATASET,
				MEASUREMENT_FILE.getAbsolutePath()));

		final IAnalysisDataset dataset = SampleDatasetReader.openDataset(TestResources.MOUSE_SIGNALS_DATASET);

		final CommandOptions options = new CommandOptions();
		options.file = TestResources.MOUSE_SIGNALS_DATASET;
		options.isMeasurements = true;
		options.isProfiles = true;
		options.isOutlines = true;
		options.isNormalisedOnly = true;

		new ExportDataPipeline(options);
		assertTrue(MEASUREMENT_FILE.exists());
		// Read the real columns exported
		final List<String> colHeaders = readColumnHeaders(MEASUREMENT_FILE);

		// Define the columns expected
		final List<String> expectedHeaders = new ArrayList<>();
		expectedHeaders.addAll(makeBaseColumnNames());
		expectedHeaders.addAll(makeMeasurementColumnNames(dataset));
		expectedHeaders.addAll(makeProfileColumnNames(dataset));
		expectedHeaders.addAll(makeOutlineColumnNames());
		testColumnHeadersMatch(expectedHeaders, colHeaders);

		assertFalse(OUTLINE_FILE.exists());
		assertFalse(PROFILE_FILE.exists());
	}
}
