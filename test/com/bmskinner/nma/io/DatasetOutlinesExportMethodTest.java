package com.bmskinner.nma.io;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
import com.bmskinner.nma.io.ImageImporter.ImageImportException;

/**
 * Tests for the outline exporter
 * 
 * @author Ben Skinner
 *
 */
public class DatasetOutlinesExportMethodTest {
	
	private static final Logger LOGGER = Logger.getLogger(DatasetOutlinesExportMethodTest.class.getName());

	private static final float EPSILON = 0.001f;
	
	/**
	 * The fields in a text file that define a coordinate
	 * 
	 *  @param object the object number
	 *  @param imageName the path to the image file 
	 *  @param x the x coordinate of the point
	 *  @param y the y coordinate of the point
	 */
	record CoordinateLine(String[] colNames, String ... args) {
		
		public int indexOf(String colName) {
			
			for(int i=0; i<colNames.length; i++) {
				if(colNames[i].equals(colName))
					return i;
			}
			return -1;
		}
		
		public String col(int index) {
			return args[index];
		}
		
		public String col0() {
			return args[0];
		}
		
		public String col1() {
			return args[1];
		}
	}
	
	private record ParsedOutline(List<CoordinateLine> coords) {

	}

	private List<ParsedOutline> readOutlineFile(File importFile) throws Exception {

		List<CoordinateLine> coordinates = new ArrayList<>();
		String[] colNames = new String[0];
		
		try (FileInputStream fstream = new FileInputStream(importFile);
				BufferedReader br = new BufferedReader(
						new InputStreamReader(fstream, StandardCharsets.UTF_8));) {
			String strLine;
			int row = 0;
			while ((strLine = br.readLine()) != null) {
				if (row == 0) { // header
					colNames = strLine.split(Io.TAB);
					row++;
					continue;
				}
				String[] arr = strLine.split(Io.TAB);
				
				coordinates.add(new CoordinateLine(colNames, arr));
			}
		} catch (FileNotFoundException e) {
			LOGGER.severe("Cannot find input file: %s".formatted(importFile.getAbsolutePath()));
			throw new ImageImportException(e);
		} catch (IOException e) {
			LOGGER.severe("Cannot read input file: %s".formatted(importFile.getAbsolutePath()));
			throw new ImageImportException(e);
		}
		
		List<ParsedOutline> objects  = coordinates.stream()
				.collect(Collectors.groupingBy(CoordinateLine::col1,
						Collectors.toList()))
				.values().stream()
				.map(ParsedOutline::new)
				.collect(Collectors.toList());

		return objects;

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
		List<ParsedOutline> ps = readOutlineFile(outFile);

		CoordinateLine cellOneLineOne = ps.get(0).coords().get(0);
		UUID cellOne = UUID.fromString(cellOneLineOne.col(cellOneLineOne.indexOf("CellID")));

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus();

		float x = Float.valueOf(cellOneLineOne.col(cellOneLineOne.indexOf("RawX")));
		float y = Float.valueOf(cellOneLineOne.col(cellOneLineOne.indexOf("RawY")));
		
		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(0).getX(), x, EPSILON);
		assertEquals(n.getBorderPoint(0).getY(), y, EPSILON);
		assertEquals(n.getBorderLength(), ps.get(0).coords().size());
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
		List<ParsedOutline> ps = readOutlineFile(outFile);

		CoordinateLine cellOneLineOne = ps.get(0).coords().get(0);
		UUID cellOne = UUID.fromString(cellOneLineOne.col(cellOneLineOne.indexOf("CellID")));

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus();

		float x = Float.valueOf(cellOneLineOne.col(cellOneLineOne.indexOf("RawX")));
		float y = Float.valueOf(cellOneLineOne.col(cellOneLineOne.indexOf("RawY")));

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(OrientationMark.REFERENCE).getX(), x, EPSILON);
		assertEquals(n.getBorderPoint(OrientationMark.REFERENCE).getY(), y, EPSILON);
		assertEquals(n.getBorderLength(), ps.get(0).coords().size());
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
		List<ParsedOutline> ps = readOutlineFile(outFile);

		CoordinateLine cellOneLineOne = ps.get(0).coords().get(0);
		UUID cellOne = UUID.fromString(cellOneLineOne.col(cellOneLineOne.indexOf("CellID")));

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus();

		float x = Float.valueOf(cellOneLineOne.col(cellOneLineOne.indexOf("RawX")));
		float y = Float.valueOf(cellOneLineOne.col(cellOneLineOne.indexOf("RawY")));

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(0).getX(), x, EPSILON);
		assertEquals(n.getBorderPoint(0).getY(), y, EPSILON);
		assertEquals(100, ps.get(0).coords().size());

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
		List<ParsedOutline> ps = readOutlineFile(outFile);

		CoordinateLine cellOneLineOne = ps.get(0).coords().get(0);
		UUID cellOne = UUID.fromString(cellOneLineOne.col(cellOneLineOne.indexOf("CellID")));

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus();

		float x = Float.valueOf(cellOneLineOne.col(cellOneLineOne.indexOf("RawX")));
		float y = Float.valueOf(cellOneLineOne.col(cellOneLineOne.indexOf("RawY")));

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(OrientationMark.REFERENCE).getX(), x, EPSILON);
		assertEquals(n.getBorderPoint(OrientationMark.REFERENCE).getY(), y, EPSILON);
		assertEquals(100, ps.get(0).coords().size());
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
		List<ParsedOutline> ps = readOutlineFile(outFile);

		CoordinateLine cellOneLineOne = ps.get(0).coords().get(0);
		UUID cellOne = UUID.fromString(cellOneLineOne.col(cellOneLineOne.indexOf("CellID")));

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus();

		float x = Float.valueOf(cellOneLineOne.col(cellOneLineOne.indexOf("RawX")));
		float y = Float.valueOf(cellOneLineOne.col(cellOneLineOne.indexOf("RawY")));

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(0).getX(), x, EPSILON);
		assertEquals(n.getBorderPoint(0).getY(), y, EPSILON);
		assertEquals(n.getBorderLength(), ps.get(0).coords().size());

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
		List<ParsedOutline> ps = readOutlineFile(outFile);

		CoordinateLine cellOneLineOne = ps.get(0).coords().get(0);
		UUID cellOne = UUID.fromString(cellOneLineOne.col(cellOneLineOne.indexOf("CellID")));

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus().getOrientedNucleus();

		float x = Float.valueOf(cellOneLineOne.col(cellOneLineOne.indexOf("OrientedX")));
		float y = Float.valueOf(cellOneLineOne.col(cellOneLineOne.indexOf("OrientedY")));

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(OrientationMark.REFERENCE).getX(), x, EPSILON);
		assertEquals(n.getBorderPoint(OrientationMark.REFERENCE).getY(), y, EPSILON);
		assertEquals(n.getBorderLength(), ps.get(0).coords().size());
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
		List<ParsedOutline> ps = readOutlineFile(outFile);

		CoordinateLine cellOneLineOne = ps.get(0).coords().get(0);
		UUID cellOne = UUID.fromString(cellOneLineOne.col(cellOneLineOne.indexOf("CellID")));

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus().getOrientedNucleus();

		float x = Float.valueOf(cellOneLineOne.col(cellOneLineOne.indexOf("OrientedX")));
		float y = Float.valueOf(cellOneLineOne.col(cellOneLineOne.indexOf("OrientedY")));

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(0).getX(), x, EPSILON);
		assertEquals(n.getBorderPoint(0).getY(), y, EPSILON);
		assertEquals(100, ps.get(0).coords().size());

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
		List<ParsedOutline> ps = readOutlineFile(outFile);

		CoordinateLine cellOneLineOne = ps.get(0).coords().get(0);
		UUID cellOne = UUID.fromString(cellOneLineOne.col(cellOneLineOne.indexOf("CellID")));

		Nucleus n = d.getCollection().getCell(cellOne).getPrimaryNucleus().getOrientedNucleus();

		float x = Float.valueOf(cellOneLineOne.col(cellOneLineOne.indexOf("OrientedX")));
		float y = Float.valueOf(cellOneLineOne.col(cellOneLineOne.indexOf("OrientedY")));

		// Correct landmark selected and correct number of points exported
		assertEquals(n.getBorderPoint(OrientationMark.REFERENCE).getX(), x, EPSILON);
		assertEquals(n.getBorderPoint(OrientationMark.REFERENCE).getY(), y, EPSILON);
		assertEquals(100, ps.get(0).coords().size());
	}
}
