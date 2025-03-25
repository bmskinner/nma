package com.bmskinner.nma.io;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsBuilder;
import com.bmskinner.nma.components.rules.OrientationMark;

/**
 * Tests for the outline exporter
 * 
 * @author Ben Skinner
 *
 */
public class DatasetOutlinesExporterTest {

	private record ParsedOutline(List<UUID> cellIds, List<List<IPoint>> rawBorders,
			List<List<IPoint>> orientedBorders) {

	}

	private ParsedOutline readOutlineFile(File importFile) throws Exception {

		try (FileInputStream fstream = new FileInputStream(importFile);
				BufferedReader br = new BufferedReader(
						new InputStreamReader(fstream, StandardCharsets.ISO_8859_1));) {

			String strLine;

			List<UUID> uuids = new ArrayList<>();
			List<List<IPoint>> rawBorders = new ArrayList<>();
			List<List<IPoint>> orientedBorders = new ArrayList<>();

			int cellIdIndex = 0;
			int rawIndex = 0;
			int orientedIndex = 0;

			while ((strLine = br.readLine()) != null) {
				String[] arr = strLine.split("\t");
				if (arr[0].equals("Dataset")) { // find which columns contain the data we need
					for (int i = 1; i < arr.length; i++) {
						if (arr[i].equals("CellID"))
							cellIdIndex = i;
						if (arr[i].equals("RawCoordinates"))
							rawIndex = i;
						if (arr[i].equals("OrientedCoordinates"))
							orientedIndex = i;
					}
					continue;
				}

				if (strLine.isEmpty())
					continue;

				UUID cellid = UUID.fromString(arr[cellIdIndex]);
				uuids.add(cellid);
				String[] coordArr = arr[rawIndex].split(",");
				String[] orientedArr = arr[orientedIndex].split(",");

				rawBorders.add(toList(coordArr));
				orientedBorders.add(toList(orientedArr));
			}
			return new ParsedOutline(uuids, rawBorders, orientedBorders);
		}

	}

	/**
	 * Given an array of pipe delimited xy coordinates, convert to a list of points
	 * 
	 * @param coordArr
	 * @return
	 */
	private List<IPoint> toList(String[] coordArr) {
		return Arrays.stream(coordArr).map(s -> {
			String[] xy = s.split("\\|");
			return (IPoint) new FloatPoint(Float.valueOf(xy[0]),
					Float.valueOf(xy[1]));
		}).toList();
	}

	/**
	 * Can we export non-normalised values correctly with no landmark indexing?
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNonNormalisedExportSucceedsWithNoLandmark() throws Exception {
		IAnalysisDataset d = SampleDatasetReader
				.openDataset(TestResources.MOUSE_TEST_DATASET);

		HashOptions o = new OptionsBuilder()
				.withValue(DefaultOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY, false)
				.build();

		File outFile = new File(d.getSavePath().getParent(), d.getName() + "_outlines.txt");
		new DatasetOutlinesExportMethod(outFile, d, o).call();

		// Read the file and check the first item is correct
		ParsedOutline ps = readOutlineFile(outFile);

		UUID cellOne = ps.cellIds().get(0);

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus();

		List<IPoint> border = ps.rawBorders().get(0);

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(0), border.get(0));
		assertEquals(n.getBorderLength(), border.size());
	}

	/**
	 * Can we export non-normalised values correctly from the reference point?
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNonNormalisedExportSucceedsWithLandmark() throws Exception {
		IAnalysisDataset d = SampleDatasetReader
				.openDataset(TestResources.MOUSE_TEST_DATASET);

		HashOptions o = new OptionsBuilder()
				.withValue(DefaultOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY, false)
				.withValue(DefaultOptions.EXPORT_OUTLINE_STARTING_LANDMARK_KEY,
						OrientationMark.REFERENCE.name())
				.build();

		File outFile = new File(d.getSavePath().getParent(), d.getName() + "_outlines.txt");
		new DatasetOutlinesExportMethod(outFile, d, o).call();

		// Read the file and check the first item is correct
		ParsedOutline ps = readOutlineFile(outFile);

		UUID cellOne = ps.cellIds().get(0);

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus();

		List<IPoint> border = ps.rawBorders().get(0);

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(OrientationMark.REFERENCE), border.get(0));
		assertEquals(n.getBorderLength(), border.size());
	}

	/**
	 * Can we export normalised values correctly with no landmark indexing?
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNormalisedExportSucceedsWithNoLandmark() throws Exception {
		IAnalysisDataset d = SampleDatasetReader
				.openDataset(TestResources.MOUSE_TEST_DATASET);

		HashOptions o = new OptionsBuilder()
				.withValue(DefaultOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY, true)
				.withValue(DefaultOptions.EXPORT_OUTLINE_N_SAMPLES_KEY, 100)
				.build();

		File outFile = new File(d.getSavePath().getParent(), d.getName() + "_outlines.txt");
		new DatasetOutlinesExportMethod(outFile, d, o).call();

		// Read the file and check the first item is correct
		ParsedOutline ps = readOutlineFile(outFile);

		UUID cellOne = ps.cellIds().get(0);

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus();

		List<IPoint> border = ps.rawBorders().get(0);

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(0), border.get(0));
		assertEquals(100, border.size());
	}

	/**
	 * Can we export normalised values correctly from the reference point?
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNormalisedExportSucceedsWithLandmark() throws Exception {
		IAnalysisDataset d = SampleDatasetReader
				.openDataset(TestResources.MOUSE_TEST_DATASET);

		HashOptions o = new OptionsBuilder()
				.withValue(DefaultOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY, true)
				.withValue(DefaultOptions.EXPORT_OUTLINE_N_SAMPLES_KEY, 100)
				.withValue(DefaultOptions.EXPORT_OUTLINE_STARTING_LANDMARK_KEY,
						OrientationMark.REFERENCE.name())
				.build();

		File outFile = new File(d.getSavePath().getParent(), d.getName() + "_outlines.txt");
		new DatasetOutlinesExportMethod(outFile, d, o).call();

		// Read the file and check the first item is correct
		ParsedOutline ps = readOutlineFile(outFile);

		UUID cellOne = ps.cellIds().get(0);

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus();

		List<IPoint> border = ps.rawBorders().get(0);

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(OrientationMark.REFERENCE), border.get(0));
		assertEquals(100, border.size());
	}

	/**
	 * Can we export non-normalised oriented values correctly with no landmark
	 * indexing?
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNonNormalisedOrientedExportSucceedsWithNoLandmark() throws Exception {
		IAnalysisDataset d = SampleDatasetReader
				.openDataset(TestResources.MOUSE_TEST_DATASET);

		HashOptions o = new OptionsBuilder()
				.withValue(DefaultOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY, false)
				.build();

		File outFile = new File(d.getSavePath().getParent(), d.getName() + "_outlines.txt");
		new DatasetOutlinesExportMethod(outFile, d, o).call();

		// Read the file and check the first item is correct
		ParsedOutline ps = readOutlineFile(outFile);

		UUID cellOne = ps.cellIds().get(0);

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus().getOrientedNucleus();
		n.moveCentreOfMass(IPoint.atOrigin());

		List<IPoint> border = ps.orientedBorders().get(0);

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(0), border.get(0));
		assertEquals(n.getBorderLength(), border.size());
	}

	/**
	 * Can we export non-normalised oriented values correctly from the reference
	 * point?
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNonNormalisedOrientedExportSucceedsWithLandmark() throws Exception {
		IAnalysisDataset d = SampleDatasetReader
				.openDataset(TestResources.MOUSE_TEST_DATASET);

		HashOptions o = new OptionsBuilder()
				.withValue(DefaultOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY, false)
				.withValue(DefaultOptions.EXPORT_OUTLINE_STARTING_LANDMARK_KEY,
						OrientationMark.REFERENCE.name())
				.build();

		File outFile = new File(d.getSavePath().getParent(), d.getName() + "_outlines.txt");
		new DatasetOutlinesExportMethod(outFile, d, o).call();

		// Read the file and check the first item is correct
		ParsedOutline ps = readOutlineFile(outFile);

		UUID cellOne = ps.cellIds().get(0);

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus().getOrientedNucleus();
		n.moveCentreOfMass(IPoint.atOrigin());

		List<IPoint> border = ps.orientedBorders().get(0);

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(OrientationMark.REFERENCE), border.get(0));
		assertEquals(n.getBorderLength(), border.size());
	}

	/**
	 * Can we export normalised oriented values correctly with no landmark indexing?
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNormalisedOrientedExportSucceedsWithNoLandmark() throws Exception {
		IAnalysisDataset d = SampleDatasetReader
				.openDataset(TestResources.MOUSE_TEST_DATASET);

		HashOptions o = new OptionsBuilder()
				.withValue(DefaultOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY, true)
				.withValue(DefaultOptions.EXPORT_OUTLINE_N_SAMPLES_KEY, 100)
				.build();

		File outFile = new File(d.getSavePath().getParent(), d.getName() + "_outlines.txt");
		new DatasetOutlinesExportMethod(outFile, d, o).call();

		// Read the file and check the first item is correct
		ParsedOutline ps = readOutlineFile(outFile);

		UUID cellOne = ps.cellIds().get(0);

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus().getOrientedNucleus();
		n.moveCentreOfMass(IPoint.atOrigin());

		List<IPoint> border = ps.orientedBorders().get(0);

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(0), border.get(0));
		assertEquals(100, border.size());
	}

	/**
	 * Can we export normalised oriented values correctly from the reference point?
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNormalisedOrientedExportSucceedsWithLandmark() throws Exception {
		IAnalysisDataset d = SampleDatasetReader
				.openDataset(TestResources.MOUSE_TEST_DATASET);

		HashOptions o = new OptionsBuilder()
				.withValue(DefaultOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY, true)
				.withValue(DefaultOptions.EXPORT_OUTLINE_N_SAMPLES_KEY, 100)
				.withValue(DefaultOptions.EXPORT_OUTLINE_STARTING_LANDMARK_KEY,
						OrientationMark.REFERENCE.name())
				.build();

		File outFile = new File(d.getSavePath().getParent(), d.getName() + "_outlines.txt");
		new DatasetOutlinesExportMethod(outFile, d, o).call();

		// Read the file and check the first item is correct
		ParsedOutline ps = readOutlineFile(outFile);

		UUID cellOne = ps.cellIds().get(0);

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus().getOrientedNucleus();
		n.moveCentreOfMass(IPoint.atOrigin());

		List<IPoint> border = ps.orientedBorders().get(0);

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(OrientationMark.REFERENCE), border.get(0));
		assertEquals(100, border.size());
	}
}
