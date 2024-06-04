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
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.stats.Stats;
import com.bmskinner.nma.utility.ArrayUtils;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

/**
 * Read a text file with outlines of objects, and convert to nuclei. Compatible
 * with outputs of e.g. YOLO segmentation models
 * 
 * @author bs19022
 *
 */
public class TextFileNucleusFinder extends CellFinder {

	private static final Logger LOGGER = Logger
			.getLogger(TextFileNucleusFinder.class.getName());

	record YoloLine(int object, float x, float y) {
	}

	private final NucleusBuilderFactory factory;

	protected TextFileNucleusFinder(@NonNull IAnalysisOptions op) {
		super(op);

		if (op.getRuleSetCollection() == null)
			throw new IllegalArgumentException("No ruleset provided");

		factory = ComponentBuilderFactory.createNucleusBuilderFactory(op.getRuleSetCollection(),
				options.getProfileWindowProportion(), 1d); // scale is hardcoded for now
	}

	@Override
	public Collection<ICell> findInFolder(@NonNull final File folder) throws ImageImportException {

		final List<ICell> list = new ArrayList<>();
		File[] arr = folder.listFiles();
		if (arr == null)
			return list;

		for (File f : arr) {

			// Check we are good to use this file
			if (Thread.interrupted()
					|| f.isDirectory()
					|| !f.getName().endsWith(Io.TEXT_FILE_EXTENSION)) // Only look at text files
				continue;

			try {
				list.addAll(findInFile(f));
				LOGGER.finer(() -> "Found images in %s".formatted(f.getName()));
			} catch (ImageImportException e) {
				LOGGER.log(Level.SEVERE, "Error searching image: %s".formatted(e.getMessage()), e);
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
		File imageFile = null;
		List<YoloLine> yoloLines = new ArrayList<>();
		try (FileInputStream fstream = new FileInputStream(textFile);
				BufferedReader br = new BufferedReader(
						new InputStreamReader(fstream, StandardCharsets.UTF_8));) {
			String strLine;
			int row = 0;
			while ((strLine = br.readLine()) != null) {
				if (row == 0) {
					row++;
					continue;
				}
				String[] arr = strLine.split(Io.TAB);
				imageFile = new File(arr[0]);
				yoloLines.add(new YoloLine(Integer.parseInt(arr[1]),
						Float.parseFloat(arr[2]), Float.parseFloat(arr[3])));
			}
		} catch (FileNotFoundException e) {
			LOGGER.severe("Cannot find input file: %s".formatted(textFile.getAbsolutePath()));
			throw new ImageImportException(e);
		} catch (IOException e) {
			LOGGER.severe("Cannot read input file: %s".formatted(textFile.getAbsolutePath()));
			throw new ImageImportException(e);
		}

		final File image = imageFile;
		List<Nucleus> nuclei = yoloLines.stream()
				.collect(Collectors.groupingBy(YoloLine::object,
						Collectors.toList()))
				.values().stream()
				.map(l -> {
					try {
						return makeNucleus(l, image);
					} catch (ComponentCreationException | IllegalArgumentException e) {
						LOGGER.fine("Could not make a nucleus: %s".formatted(e.getMessage()));
					}
					return null;
				})
				.toList();

		List<ICell> result = new ArrayList<>();
		for (Nucleus n : nuclei) {
			if (null == n)
				continue; // nulls may come from failed makeNucleus above
			ICell c = new DefaultCell(n);
			result.add(c);
		}
		fireProgressEvent();
		return result;
	}

	private Nucleus makeNucleus(List<YoloLine> coordinates, File file)
			throws ComponentCreationException {

		Float[] xPoints = coordinates.stream().map(YoloLine::x).toArray(Float[]::new);
		Float[] yPoints = coordinates.stream().map(YoloLine::y).toArray(Float[]::new);
		float[] x = ArrayUtils.toFloat(xPoints);
		float[] y = ArrayUtils.toFloat(yPoints);

		// Calculate centroid
		double xMin = Stats.min(x);
		double yMin = Stats.min(y);
		double xMax = Stats.max(x);
		double yMax = Stats.max(y);
		double xc = xMin + (xMax - xMin) / 2;
		double yc = yMin + (yMax - yMin) / 2;

		IPoint com = new FloatPoint(xc, yc);

		Roi r = new PolygonRoi(x, y, Roi.POLYGON);

		return factory.newBuilder()
				.fromRoi(r)
				.withFile(file)
				.withCoM(com)
				.build();
	}

	@Override
	public boolean isValid(@NonNull ICell entity) {
		// We don't filter nuclei already defined
		return true;
	}

}
