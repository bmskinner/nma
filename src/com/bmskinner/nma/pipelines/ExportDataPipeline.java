package com.bmskinner.nma.pipelines;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.core.CommandOptions;
import com.bmskinner.nma.io.CellFileExporter;
import com.bmskinner.nma.io.CellImageExportMethod;
import com.bmskinner.nma.io.DatasetImportMethod;
import com.bmskinner.nma.io.DatasetMeasurementsExportMethod;
import com.bmskinner.nma.io.DatasetOptionsExportMethod;
import com.bmskinner.nma.io.DatasetOutlinesExportMethod;
import com.bmskinner.nma.io.DatasetProfileExporter;
import com.bmskinner.nma.io.DatasetShellsExporter;
import com.bmskinner.nma.io.DatasetSignalsExporter;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.io.SVGWriter;
import com.bmskinner.nma.io.XMLImportMethod;
import com.bmskinner.nma.io.XMLWriter;

public class ExportDataPipeline {

	private static final Logger LOGGER = Logger
			.getLogger(ExportDataPipeline.class.getName());

	private final CommandOptions opt;

	private IAnalysisDataset root;
	private List<IAnalysisDataset> datasets = new ArrayList<>();

	/**
	 * Build a pipeline covering all the options within the given file
	 * 
	 * @param xmlFile the options for analysis
	 * @throws Exception
	 */

	public ExportDataPipeline(@NonNull final CommandOptions opt)
			throws Exception {

		this.opt = opt;

		datasets = readDataset();

		LOGGER.info("Read dataset from file");

		if (opt.isMeasurements || opt.isAll) {
			exportMeasurements();
		}

		if (opt.isProfiles || opt.isAll) {
			exportProfiles();
		}

		if (opt.isOutlines || opt.isAll) {
			exportOutlines();
		}

		if (opt.isSignals || opt.isAll) {
			exportSignals();
		}

		if (opt.isShells || opt.isAll) {
			exportShells();
		}

		if (opt.isAnalysisOptions || opt.isAll) {
			exportAnalysisOptions();
		}

		if (opt.isRulesets || opt.isAll) {
			exportRulesets();
		}

		if (opt.isConsensus || opt.isAll) {
			exportConsensus();
		}

		if (opt.isCellLocations || opt.isAll) {
			exportCellLocations();
		}

		// Don't include this in 'all' because it takes much longer
		if (opt.isSingleCellImages) {
			exportSingleCellImages();
		}

		LOGGER.info("Export complete");

	}

	private List<IAnalysisDataset> readDataset() throws Exception {
		final XMLImportMethod m = new XMLImportMethod(opt.file);
		m.call();
		root = new DatasetImportMethod(m.getXMLDocument()).call()
				.getFirstDataset();

		final List<IAnalysisDataset> datasets = new ArrayList<>();
		datasets.add(root);
		datasets.addAll(root.getAllChildDatasets());

		return datasets;
	}

	private void exportMeasurements() throws Exception {
		final File statsFile = new File(root.getSavePath().getParentFile(),
				"%s.measurements%s".formatted(root.getSavePath().getName(), Io.TAB_FILE_EXTENSION));
		LOGGER.info("Exporting data to: " + statsFile.getAbsolutePath());

		final HashOptions exportOptions = new DefaultOptions();
		exportOptions.setBoolean(HashOptions.EXPORT_MEASUREMENTS_KEY, true);
		exportOptions.setBoolean(HashOptions.EXPORT_PROFILES_KEY, true);
		exportOptions.setInt(HashOptions.EXPORT_PROFILE_INTERPOLATION_LENGTH, 100);
		exportOptions.setBoolean(HashOptions.EXPORT_OUTLINES_KEY, true);
		exportOptions.setInt(HashOptions.EXPORT_OUTLINE_N_SAMPLES_KEY, 100);
		exportOptions.setBoolean(HashOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY, true);
		exportOptions.setString(HashOptions.EXPORT_OUTLINE_STARTING_LANDMARK_KEY, OrientationMark.REFERENCE.name());


		new DatasetMeasurementsExportMethod(statsFile, datasets, exportOptions)
				.call();
	}

	/**
	 * Export the raw profiles for the cells in the dataset
	 * 
	 * @throws Exception
	 */
	private void exportProfiles() throws Exception {
		final File statsFile = new File(root.getSavePath().getParentFile(),
				"%s.profiles%s".formatted(root.getSavePath().getName(), Io.TAB_FILE_EXTENSION));
		LOGGER.info("Exporting profiles to: %s".formatted(statsFile.getAbsolutePath()));

		final HashOptions exportOptions = new DefaultOptions();
		exportOptions.setBoolean(HashOptions.EXPORT_PROFILES_KEY, true);


		new DatasetProfileExporter(statsFile, datasets, new DefaultOptions()).call();
	}

	/**
	 * Export the raw outlines for the cells in the dataset.
	 * 
	 * @throws Exception
	 */
	private void exportOutlines() throws Exception {
		final File statsFile = new File(root.getSavePath().getParentFile(),
				"%s.outlines%s".formatted(root.getSavePath().getName(), Io.TAB_FILE_EXTENSION));
		LOGGER.info("Exporting outlines to: %s".formatted(statsFile.getAbsolutePath()));
		final HashOptions exportOptions = new DefaultOptions();
		exportOptions.setBoolean(HashOptions.EXPORT_OUTLINES_KEY, true);
		exportOptions.setBoolean(HashOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY, false);

		new DatasetOutlinesExportMethod(statsFile, datasets, exportOptions).call();
	}

	private void exportSignals() throws Exception {
		final File statsFile = new File(root.getSavePath().getParentFile(),
				"%s.signals%s".formatted(root.getSavePath().getName(), Io.TAB_FILE_EXTENSION));
		LOGGER.info("Exporting signals to: %s".formatted(statsFile.getAbsolutePath()));

		new DatasetSignalsExporter(statsFile, datasets, new DefaultOptions()).call();
	}

	private void exportShells() throws Exception {
		final File statsFile = new File(root.getSavePath().getParentFile(),
				"%s.shells%s".formatted(root.getSavePath().getName(), Io.TAB_FILE_EXTENSION));
		LOGGER.info("Exporting shells to: %s".formatted(statsFile.getAbsolutePath()));

		new DatasetShellsExporter(statsFile, datasets, new DefaultOptions()).call();
	}

	private void exportSingleCellImages() throws Exception {

		LOGGER.info("Exporting single cell images");

		final HashOptions exportOptions = new DefaultOptions();
		exportOptions.setBoolean(CellImageExportMethod.MASK_BACKGROUND_KEY, false);
		exportOptions.setBoolean(CellImageExportMethod.MASK_FOREGROUND_KEY, false);
		exportOptions.setBoolean(CellImageExportMethod.SINGLE_CELL_IMAGE_IS_NORMALISE_WIDTH_KEY,
				false);
		exportOptions.setInt(CellImageExportMethod.SINGLE_CELL_IMAGE_WIDTH_KEY, 255);
		exportOptions.setBoolean(CellImageExportMethod.SINGLE_CELL_IMAGE_IS_RGB_KEY,
				false);
		exportOptions.setBoolean(CellImageExportMethod.SINGLE_CELL_IMAGE_IS_EXPORT_KEYPOINTS_KEY,
				true);
		new CellImageExportMethod(datasets, exportOptions).call();
	}

	private void exportAnalysisOptions() throws Exception {
		final File outFile = new File(root.getSavePath().getParentFile(),
				root.getSavePath().getName() + ".analysis-options" + Io.XML_FILE_EXTENSION);
		LOGGER.info("Exporting analysis options to: " + outFile.getAbsolutePath());

		new DatasetOptionsExportMethod(root, outFile).call();
	}

	private void exportRulesets() throws Exception {
		final File outFile = new File(root.getSavePath().getParentFile(),
				root.getSavePath().getName() + ".rulesets" + Io.XML_FILE_EXTENSION);
		LOGGER.info("Exporting rulesets to: " + outFile.getAbsolutePath());

		final RuleSetCollection rsc = root.getCollection().getRuleSetCollection();
		try {
			XMLWriter.writeXML(rsc.toXmlElement(), outFile);
		} catch (final IOException e) {
			LOGGER.warning("Unable to write rulesets to file");
		}
	}

	private void exportConsensus() throws Exception {
		final File outFile = new File(root.getSavePath().getParentFile(),
				root.getSavePath().getName() + ".consensus" + Io.SVG_FILE_EXTENSION);
		LOGGER.info("Exporting consensus nucleus to: " + outFile.getAbsolutePath());

		// Ensure all datasets have a consensus
		for (final IAnalysisDataset ds : datasets) {
			if (!ds.getCollection().hasConsensus()) {
				new ConsensusAveragingMethod(ds).call();
			}
		}

		new SVGWriter(outFile).exportConsensusOutlines(datasets, MeasurementScale.MICRONS);
	}

	private void exportCellLocations() throws Exception {
		new CellFileExporter(datasets).call();
	}
}
