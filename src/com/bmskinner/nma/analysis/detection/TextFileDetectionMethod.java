package com.bmskinner.nma.analysis.detection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
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
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;
import com.bmskinner.nma.io.Io;

/**
 * Create nuclei from a text file describing their outlines. For example, as
 * generated from a YOLO segmentation model.
 * 
 * @author Ben Skinner
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
		

		LOGGER.info("Running nucleus detector");

		// Detect the nuclei in the folders selected
		File coordinateFile = templateOptions.getNucleusDetectionOptions().get()
				.getFile(HashOptions.COORDINATE_LOCATION_FILE_KEY);

		if (!coordinateFile.exists()) {
			throw new AnalysisMethodException("Coordinate file '%s' does not exist".formatted(coordinateFile.getAbsolutePath()));
		}
		
		processCoordinateFile(coordinateFile);

		LOGGER.fine("Finished detecting nuclei in '%s'".formatted(coordinateFile.getAbsolutePath()));

		if (Thread.interrupted())
			return;

		List<IAnalysisDataset> analysedDatasets = analysePopulations();

		if (analysedDatasets.isEmpty())
			throw new AnalysisMethodException("No datasets returned");

		datasets.addAll(analysedDatasets);
		LOGGER.fine("Nucleus detection complete for"
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
	 * Go through the input file. Check if the file is suitable for analysis, and
	 * if so, call the analyser.
	 *
	 * @param coordinateFile the file of coordinates to be analysed
	 */
	protected void processCoordinateFile(@NonNull final File coordinateFile) {
		if(!coordinateFile.isFile())
			return;

		// Make an empty cell collection
		ICellCollection fc = new DefaultCellCollection(templateOptions.getRuleSetCollection(),
				coordinateFile.getName(), UUID.randomUUID());

//		// Make a cell finder
		final Finder<ICell> finder = new TextFileNucleusFinder(templateOptions);
		finder.addProgressListener(this);

		// Detect cells and add to collection
		try {
			final Collection<ICell> cells = finder.findInFile(coordinateFile);

			if (!cells.isEmpty() && !outputFolder.exists())
				outputFolder.mkdir();
			fc.addAll(cells);

		} catch (ImageImportException e) {
			LOGGER.log(Level.SEVERE, "Error searching directory: %s".formatted(e.getMessage()), e);
		}

		// Add the new collection to the group
		collectionGroup.put(coordinateFile, fc);

	}
}
