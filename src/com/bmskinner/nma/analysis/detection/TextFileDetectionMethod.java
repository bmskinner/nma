package com.bmskinner.nma.analysis.detection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.AbstractAnalysisMethod;
import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.DefaultAnalysisDataset;
import com.bmskinner.nma.components.datasets.DefaultCellCollection;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.logging.Loggable;

/**
 * Create nuclei from a text file describing their outlines. For example, as
 * generated from a YOLO segmentation model.
 * 
 * @author bs19022
 *
 */
public class TextFileDetectionMethod extends AbstractAnalysisMethod {

	private static final Logger LOGGER = Logger
			.getLogger(TextFileDetectionMethod.class.getName());

	private final File outputFolder;

	private final IAnalysisOptions templateOptions;

	/** Map a folder of images to the detected cell collection */
	private Map<File, ICellCollection> collectionGroup = new HashMap<>();

	private final List<IAnalysisDataset> datasets = new ArrayList<>();

	/**
	 * Construct a detector with the given options and output the results to the
	 * given output folder
	 * 
	 * @param outputFolder the folder to save the results into
	 * @param options      the options to detect with
	 * @throws AnalysisMethodException
	 */
	public TextFileDetectionMethod(@NonNull File outputFolder,
			@NonNull IAnalysisOptions options)
			throws AnalysisMethodException {
		// We need the parent of the output folder to exist so
		// the folder can be created
		if (outputFolder.getParentFile() == null || !outputFolder.getParentFile().exists())
			throw new AnalysisMethodException(
					"Output parent folder does not exist: " + outputFolder.getAbsolutePath());

		this.outputFolder = outputFolder;
		this.templateOptions = options;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();
		return new DefaultAnalysisResult(datasets);
	}

	/**
	 * Run the method. Use {@link call} to get the resulting datasets
	 * 
	 * @throws AnalysisMethodException
	 */
	public void run() throws AnalysisMethodException {
		int i = countTotalFilesToAnalyse();
		if (i == 0) {
			throw new AnalysisMethodException("No analysable images");
		}

		LOGGER.info("Running nucleus detector");

		// Detect the nuclei in the folders selected
		File filePath = templateOptions.getNucleusDetectionFolder().get();
		processFolder(filePath);

		LOGGER.fine(() -> "Detected nuclei in %s".formatted(filePath.getAbsolutePath()));

		if (Thread.interrupted())
			return;

		List<IAnalysisDataset> analysedDatasets = analysePopulations();

		if (analysedDatasets.isEmpty())
			throw new AnalysisMethodException("No datasets returned");

		datasets.addAll(analysedDatasets);
		LOGGER.fine(() -> "Nucleus detection complete for %d folders"
				.formatted(analysedDatasets.size()));
	}

	/**
	 * From all possible cell collections, choose those with cells and create root
	 * datasets
	 * 
	 * @return
	 */
	private List<IAnalysisDataset> analysePopulations() {

		LOGGER.finer("Creating cell collections");
		List<IAnalysisDataset> foundDatasets = new ArrayList<>();

		for (final Entry<File, ICellCollection> entry : collectionGroup.entrySet()) {
			// Only keep collections containing nuclei
			ICellCollection collection = entry.getValue();
			if (collection.isEmpty())
				continue;

			File folder = entry.getKey();
			IAnalysisDataset dataset = new DefaultAnalysisDataset(collection,
					new File(outputFolder, collection.getName() + Io.NMD_FILE_EXTENSION));

			// Ensure the actual folder of images is set in the analysis options, not a root
			// folder
			IAnalysisOptions datasetOptions = templateOptions.duplicate();
			datasetOptions.setDetectionFolder(CellularComponent.NUCLEUS, folder.getAbsoluteFile());
			dataset.setAnalysisOptions(datasetOptions);

			try {
				collection.clear(MeasurementScale.PIXELS);
				collection.clear(MeasurementScale.MICRONS);
				LOGGER.info(() -> "Found %d nuclei in %s".formatted(collection.size(),
						collection.getName()));
				foundDatasets.add(dataset);

			} catch (Exception e) {
				LOGGER.severe("Cannot create cell collection: %s".formatted(e.getMessage()));
			}
		}

		return foundDatasets;
	}

	/**
	 * Go through the input folder. Check if each file is suitable for analysis, and
	 * if so, call the analyser.
	 *
	 * @param folder the folder of images to be analysed
	 */
	protected void processFolder(@NonNull final File folder) {
		File[] arr = folder.listFiles();
		if (arr == null)
			return;
		if (Thread.interrupted())
			return;

		// Recurse over all folders in the supplied folder
		for (File f : arr) {
			if (f.isDirectory())
				processFolder(f);
		}

		if (!containsTextFiles(folder))
			return;

		// Make an empty cell collection
		ICellCollection fc = new DefaultCellCollection(templateOptions.getRuleSetCollection(),
				folder.getName(), UUID.randomUUID());

//		// Make a cell finder
		final Finder<ICell> finder = new TextFileNucleusFinder(templateOptions);
		finder.addProgressListener(this);

		// Detect cells and add to collection
		try {
			final Collection<ICell> cells = finder.findInFolder(folder);

			if (!cells.isEmpty() && !outputFolder.exists())
				outputFolder.mkdir();
			fc.addAll(cells);

		} catch (ImageImportException e) {
			LOGGER.log(Loggable.STACK, "Error searching folder: %s".formatted(e.getMessage()), e);
		}

		// Add the new collection to the group
		collectionGroup.put(folder, fc);

	}

	/**
	 * Test if the given folder has any files that can be analysed
	 * 
	 * @param folder the folder to test
	 * @return
	 */
	protected boolean containsTextFiles(@NonNull final File folder) {
		if (!folder.isDirectory())
			return false;
		File[] arr = folder.listFiles();
		if (arr == null)
			return false;
		for (File f : arr)
			if (f.getName().endsWith(Io.TEXT_FILE_EXTENSION))
				return true;
		return false;
	}

	/**
	 * Count the number of outline files that are available to be analysed across
	 * all folders
	 * 
	 * @return
	 */
	private int countTotalFilesToAnalyse() {

		if (templateOptions.getNucleusDetectionFolder().isEmpty()) {
			LOGGER.fine("No detection folder provided");
			return 0;
		}

		File folder = templateOptions.getNucleusDetectionFolder().get();
		int totalImages = countSuitableFiles(folder);
		fireUpdateProgressTotalLength(totalImages);
		LOGGER.info(() -> "Analysing %d images".formatted(totalImages));
		return totalImages;
	}

	/**
	 * Count the number of files in the given folder that are suitable for analysis.
	 * Rcursive over subfolders.
	 * 
	 * @param folder the folder to count
	 * @return the number of analysable text files in this folder or subfolders
	 */
	private static int countSuitableFiles(@NonNull final File folder) {
		final File[] listOfFiles = folder.listFiles();
		if (listOfFiles == null)
			return 0;
		int result = 0;

		for (File file : listOfFiles) {
			boolean ok = file.getName().endsWith(Io.TEXT_FILE_EXTENSION);
			if (ok) {
				result++;
			} else {
				if (file.isDirectory())// recurse over any sub folders
					result += countSuitableFiles(file);
			}
		}
		return result;
	}

}
