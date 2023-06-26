package com.bmskinner.nma.io;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.rules.OrientationMark;

/**
 * Tests for the outline exporter
 * 
 * @author bs19022
 *
 */
public class DatasetOutlinesExporterTest {

	private record ParsedOutline(List<UUID> cellIds, List<List<IPoint>> borders) {

	}

	private ParsedOutline readOutlineFile(File importFile) throws Exception {

		try (FileInputStream fstream = new FileInputStream(importFile);
				BufferedReader br = new BufferedReader(
						new InputStreamReader(fstream, StandardCharsets.ISO_8859_1));) {

			String strLine;

			List<UUID> uuids = new ArrayList<>();
			List<List<IPoint>> borders = new ArrayList<>();

			while ((strLine = br.readLine()) != null) {
				String[] arr = strLine.split("\t");
				if (arr[0].equals("Dataset")) // skip first header line
					continue;

				if (strLine.isEmpty())
					continue;

				UUID cellid = UUID.fromString(arr[1]);
				uuids.add(cellid);
				String[] coordArr = arr[5].split(",");

				List<IPoint> points = new ArrayList<>();
				for (String s : coordArr) {
					String[] xy = s.split("\\|");
					points.add(new FloatPoint(Float.valueOf(xy[0]), Float.valueOf(xy[1])));
				}
				borders.add(points);
			}
			return new ParsedOutline(uuids, borders);
		}

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
		new DatasetOutlinesExporter(outFile, d, o).call();

		// Read the file and check the first item is correct
		ParsedOutline ps = readOutlineFile(outFile);

		UUID cellOne = ps.cellIds().get(0);

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus();

		List<IPoint> border = ps.borders().get(0);

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

		Landmark rp = d.getCollection().getProfileCollection()
				.getLandmark(OrientationMark.REFERENCE);

		HashOptions o = new OptionsBuilder()
				.withValue(DefaultOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY, false)
				.withValue(DefaultOptions.EXPORT_OUTLINE_STARTING_LANDMARK_KEY, rp.getName())
				.build();

		File outFile = new File(d.getSavePath().getParent(), d.getName() + "_outlines.txt");
		new DatasetOutlinesExporter(outFile, d, o).call();

		// Read the file and check the first item is correct
		ParsedOutline ps = readOutlineFile(outFile);

		UUID cellOne = ps.cellIds().get(0);

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus();

		List<IPoint> border = ps.borders().get(0);

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(rp), border.get(0));
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

		Landmark rp = d.getCollection().getProfileCollection()
				.getLandmark(OrientationMark.REFERENCE);

		HashOptions o = new OptionsBuilder()
				.withValue(DefaultOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY, true)
				.withValue(DefaultOptions.EXPORT_OUTLINE_N_SAMPLES_KEY, 100)
				.build();

		File outFile = new File(d.getSavePath().getParent(), d.getName() + "_outlines.txt");
		new DatasetOutlinesExporter(outFile, d, o).call();

		// Read the file and check the first item is correct
		ParsedOutline ps = readOutlineFile(outFile);

		UUID cellOne = ps.cellIds().get(0);

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus();

		List<IPoint> border = ps.borders().get(0);

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

		Landmark rp = d.getCollection().getProfileCollection()
				.getLandmark(OrientationMark.REFERENCE);

		HashOptions o = new OptionsBuilder()
				.withValue(DefaultOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY, true)
				.withValue(DefaultOptions.EXPORT_OUTLINE_N_SAMPLES_KEY, 100)
				.withValue(DefaultOptions.EXPORT_OUTLINE_STARTING_LANDMARK_KEY, rp.getName())
				.build();

		File outFile = new File(d.getSavePath().getParent(), d.getName() + "_outlines.txt");
		new DatasetOutlinesExporter(outFile, d, o).call();

		// Read the file and check the first item is correct
		ParsedOutline ps = readOutlineFile(outFile);

		UUID cellOne = ps.cellIds().get(0);

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus();

		List<IPoint> border = ps.borders().get(0);

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(rp), border.get(0));
		assertEquals(100, border.size());
	}
}
