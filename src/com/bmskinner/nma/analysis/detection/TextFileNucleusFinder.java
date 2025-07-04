package com.bmskinner.nma.analysis.detection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.ComponentBuilderFactory;
import com.bmskinner.nma.components.ComponentBuilderFactory.NucleusBuilderFactory;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.DefaultCell;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.stats.Stats;
import com.bmskinner.nma.utility.ArrayUtils;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

/**
 * Read a text file containing the outlines of objects, and use to create nuclei.
 * <p>
 * The text file format is four tab-separated columns with a header line. Each
 * line contains an XY coordinate for one point in an object outline. The column
 * names are not important.<br>
 * <br>
 * 
 * The file contains the following columns:
 * <pre>
 * 0 - Path of the image file.
 * 1 - Object identifier. Any string that uniquely identifies 
 *     the object within this file.
 * 2 - x coordinate of the point in integer or float precision
 * 3 - y coordinate of the point in integer or float precision
 * </pre>
 * 
 * Example:<br>
 * <pre>
 * ImageFile	ObjectID	x	y
 * /path/to/image.jpg	0	1563.3	1020.6
 * /path/to/image.jpg	0	1561.275	1022.625
 * </pre>
 * 
 * @author Ben Skinner
 *
 */
public class TextFileNucleusFinder extends CellFinder {

	private static final Logger LOGGER = Logger.getLogger(TextFileNucleusFinder.class.getName());

	/** Options keys for columns to read */
	public static final String IMAGE_FILE_COL = "ImageFileColumn";
	public static final String NUCLEUS_ID_COL = "NucleusIDColumn";
	public static final String X_COORDINATE_COL = "XCoordColumn";
	public static final String Y_COORDINATE_COL = "YCoordColumn";

	/**
	 * Default column indexes for text files. These columns will be used unless
	 * otherwise specified
	 */
	private static final int DEFAULT_IMAGE_FILE_COL = 0;
	private static final int DEFAULT_NUCLEUS_ID_COL = 1;
	private static final int DEFAULT_X_COORDINATE_COL = 2;
	private static final int DEFAULT_Y_COORDINATE_COL = 3;

	/** Column indexes for text files */
	private int imageFileCol = DEFAULT_IMAGE_FILE_COL;
	private int nucleusCol = DEFAULT_NUCLEUS_ID_COL;
	private int xCol = DEFAULT_X_COORDINATE_COL;
	private int yCol = DEFAULT_Y_COORDINATE_COL;

	/**
	 * The fields in a text file that define a coordinate
	 * 
	 * @param object    the object number
	 * @param imageName the path to the image file
	 * @param x         the x coordinate of the point
	 * @param y         the y coordinate of the point
	 */
	record CoordinateLine(String object, String imageName, float x, float y) {
	}

	private final NucleusBuilderFactory factory;
	private final HashOptions nucleusOptions;

	protected TextFileNucleusFinder(@NonNull IAnalysisOptions op) {
		super(op);

		if (op.getRuleSetCollection() == null)
			throw new IllegalArgumentException("No ruleset provided");

		nucleusOptions = op.getNucleusDetectionOptions().get();

		// Check if the options override the default input columns
		if (nucleusOptions.hasInt(IMAGE_FILE_COL)) {
			imageFileCol = nucleusOptions.getInt(IMAGE_FILE_COL);
		}
		if (nucleusOptions.hasInt(NUCLEUS_ID_COL)) {
			nucleusCol = nucleusOptions.getInt(NUCLEUS_ID_COL);
		}
		if (nucleusOptions.hasInt(X_COORDINATE_COL)) {
			xCol = nucleusOptions.getInt(X_COORDINATE_COL);
		}
		if (nucleusOptions.hasInt(Y_COORDINATE_COL)) {
			yCol = nucleusOptions.getInt(Y_COORDINATE_COL);
		}

//		LOGGER.fine("Searching columns image %s, nucleus %s, x %s, y %s".formatted(imageFileCol, nucleusCol, xCol, yCol));

		factory = ComponentBuilderFactory.createNucleusBuilderFactory(op.getRuleSetCollection(),
				options.getProfileWindowProportion(), 1d); // scale is hardcoded for now
	}

	@Override
	public Collection<ICell> findInFolder(@NonNull final File folder) throws ImageImportException {

		final List<ICell> list = new ArrayList<>();
		final File[] arr = folder.listFiles();
		if (arr == null)
			return list;

		for (final File f : arr) {

			// Check we are good to use this file
			if (Thread.interrupted() || f.isDirectory() || !f.getName().endsWith(Io.TEXT_FILE_EXTENSION)) { // Only look
				// at text
																															// files
								continue;
			}

			try {
				list.addAll(findInFile(f));
				LOGGER.finer(() -> "Found text files in %s".formatted(f.getName()));
			} catch (final ImageImportException e) {
				LOGGER.log(Level.SEVERE, "Error searching file: %s".formatted(e.getMessage()), e);
			}
		}

		return list;
	}

	@Override
	public Collection<ICell> findInFile(@NonNull File textFile) throws ImageImportException {

//		Read the text file
//		Group by object
//		Convert object coords to Roi
//		Invoke NucleusBuilder
		final List<CoordinateLine> coordinates = new ArrayList<>();
		try (FileInputStream fstream = new FileInputStream(textFile);
				BufferedReader br = new BufferedReader(new InputStreamReader(fstream, StandardCharsets.UTF_8));) {
			String strLine;
			int row = 0;
			while ((strLine = br.readLine()) != null) {
				if (row == 0) { // skip header
					row++;
					continue;
				}
				final String[] arr = strLine.split(Io.TAB);
				coordinates.add(new CoordinateLine(arr[nucleusCol], arr[imageFileCol], Float.parseFloat(arr[xCol]),
						Float.parseFloat(arr[yCol])));
			}
		} catch (final FileNotFoundException e) {
			LOGGER.severe("Cannot find input file: %s".formatted(textFile.getAbsolutePath()));
			throw new ImageImportException(e);
		} catch (final IOException e) {
			LOGGER.severe("Cannot read input file: %s".formatted(textFile.getAbsolutePath()));
			throw new ImageImportException(e);
		} catch (final NumberFormatException e) {
			LOGGER.severe(
					"When reading x and y coordinates, unable to parse a string as a number. %s. Check input file columns are in the correct order."
							.formatted(e.getMessage()));
			throw new ImageImportException("Unable to read object coordinates from file", e);
		} catch (final Exception e) {
			LOGGER.severe("Error parsing coordinate file: %s".formatted(e.getMessage()));
			throw new ImageImportException("Unable to read object coordinates from file", e);
		}

		final List<Nucleus> nuclei = coordinates.stream()
				.collect(Collectors.groupingBy(CoordinateLine::object, Collectors.toList())).values().stream()
				.map(l -> {
					try {
						return makeNucleus(l);
					} catch (ComponentCreationException | IllegalArgumentException e) {
						LOGGER.fine("Could not make a nucleus: %s".formatted(e.getMessage()));
					}
					return null;
				}).toList();

		final List<ICell> result = new ArrayList<>();
		for (final Nucleus n : nuclei) {
			if (null == n)
			 {
				continue; // nulls may come from failed makeNucleus above
			}
			final ICell c = new DefaultCell(n);
			result.add(c);
		}
		fireProgressEvent();
		return result;
	}

	private Nucleus makeNucleus(List<CoordinateLine> coordinates) throws ComponentCreationException {

		final String image = coordinates.stream().map(c -> c.imageName).findFirst().get();
		final File imageFile = new File(image);
		final Float[] xPoints = coordinates.stream().map(CoordinateLine::x).toArray(Float[]::new);
		final Float[] yPoints = coordinates.stream().map(CoordinateLine::y).toArray(Float[]::new);
		final float[] x = ArrayUtils.toFloat(xPoints);
		final float[] y = ArrayUtils.toFloat(yPoints);

		// Calculate centroid
		final double xMin = Stats.min(x);
		final double yMin = Stats.min(y);
		final double xMax = Stats.max(x);
		final double yMax = Stats.max(y);
		final double xc = xMin + (xMax - xMin) / 2;
		final double yc = yMin + (yMax - yMin) / 2;

		final IPoint com = new FloatPoint(xc, yc);

		final Roi r = new PolygonRoi(x, y, Roi.POLYGON);

		return factory.newBuilder()
				.fromRoi(r)
				.withFile(imageFile)
				.withCoM(com)
				.withChannel(nucleusOptions.getInt(HashOptions.CHANNEL))
				.build();
	}

	@Override
	public boolean isValid(@NonNull ICell entity) {
		// We don't filter nuclei already defined
		return true;
	}

}
