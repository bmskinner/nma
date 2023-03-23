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
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.core.CommandOptions;
import com.bmskinner.nma.io.CellFileExporter;
import com.bmskinner.nma.io.CellImageExportMethod;
import com.bmskinner.nma.io.DatasetImportMethod;
import com.bmskinner.nma.io.DatasetOptionsExportMethod;
import com.bmskinner.nma.io.DatasetOutlinesExporter;
import com.bmskinner.nma.io.DatasetProfileExporter;
import com.bmskinner.nma.io.DatasetShellsExporter;
import com.bmskinner.nma.io.DatasetSignalsExporter;
import com.bmskinner.nma.io.DatasetStatsExporter;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.io.SVGWriter;
import com.bmskinner.nma.io.XMLImportMethod;
import com.bmskinner.nma.io.XMLWriter;

public class ExportDataPipeline {

	private static final Logger LOGGER = Logger
			.getLogger(ExportDataPipeline.class.getName());

	private CommandOptions opt;

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

		if (opt.isMeasurements || opt.isAll)
			exportMeasurements();

		if (opt.isProfiles || opt.isAll)
			exportProfiles();

		if (opt.isOutlines || opt.isAll)
			exportOutlines();

		if (opt.isSignals || opt.isAll)
			exportSignals();

		if (opt.isShells || opt.isAll)
			exportShells();

		if (opt.isAnalysisOptions || opt.isAll)
			exportAnalysisOptions();

		if (opt.isRulesets || opt.isAll)
			exportRulesets();

		if (opt.isConsensus || opt.isAll)
			exportConsensus();

		if (opt.isCellLocations || opt.isAll)
			exportCellLocations();

		// Don't include this in 'all' because it takes much longer
		if (opt.isSingleCellImages)
			exportSingleCellImages();

		LOGGER.info("Export complete");

	}

	private List<IAnalysisDataset> readDataset() throws Exception {
		XMLImportMethod m = new XMLImportMethod(opt.file);
		m.call();
		root = new DatasetImportMethod(m.getXMLDocument()).call()
				.getFirstDataset();

		List<IAnalysisDataset> datasets = new ArrayList<>();
		datasets.add(root);
		datasets.addAll(root.getAllChildDatasets());

		return datasets;
	}

	private void exportMeasurements() throws Exception {
		File statsFile = new File(root.getSavePath().getParentFile(),
				root.getSavePath().getName() + ".measurements" + Io.TAB_FILE_EXTENSION);
		LOGGER.info("Exporting data to: " + statsFile.getAbsolutePath());

		HashOptions exportOptions = new DefaultOptions();
		exportOptions.setInt(Io.PROFILE_SAMPLES_KEY, 100);

		new DatasetStatsExporter(statsFile, datasets, exportOptions)
				.call();
	}

	private void exportProfiles() throws Exception {
		File statsFile = new File(root.getSavePath().getParentFile(),
				root.getSavePath().getName() + ".profiles" + Io.TAB_FILE_EXTENSION);
		LOGGER.info("Exporting profiles to: " + statsFile.getAbsolutePath());
		new DatasetProfileExporter(statsFile, datasets).call();
	}

	private void exportOutlines() throws Exception {
		File statsFile = new File(root.getSavePath().getParentFile(),
				root.getSavePath().getName() + ".outlines" + Io.TAB_FILE_EXTENSION);
		LOGGER.info("Exporting outlines to: " + statsFile.getAbsolutePath());

		new DatasetOutlinesExporter(statsFile, datasets).call();
	}

	private void exportSignals() throws Exception {
		File statsFile = new File(root.getSavePath().getParentFile(),
				root.getSavePath().getName() + ".signals" + Io.TAB_FILE_EXTENSION);
		LOGGER.info("Exporting signals to: " + statsFile.getAbsolutePath());

		new DatasetSignalsExporter(statsFile, datasets).call();
	}

	private void exportShells() throws Exception {
		File statsFile = new File(root.getSavePath().getParentFile(),
				root.getSavePath().getName() + ".shells" + Io.TAB_FILE_EXTENSION);
		LOGGER.info("Exporting shells to: " + statsFile.getAbsolutePath());

		new DatasetShellsExporter(statsFile, datasets).call();
	}

	private void exportSingleCellImages() throws Exception {

		LOGGER.info("Exporting single cell images");

		HashOptions exportOptions = new DefaultOptions();
		exportOptions.setBoolean(CellImageExportMethod.MASK_BACKGROUND_KEY, false);
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
		File outFile = new File(root.getSavePath().getParentFile(),
				root.getSavePath().getName() + ".analysis-options" + Io.XML_FILE_EXTENSION);
		LOGGER.info("Exporting analysis options to: " + outFile.getAbsolutePath());

		new DatasetOptionsExportMethod(root, outFile).call();
	}

	private void exportRulesets() throws Exception {
		File outFile = new File(root.getSavePath().getParentFile(),
				root.getSavePath().getName() + ".rulesets" + Io.XML_FILE_EXTENSION);
		LOGGER.info("Exporting rulesets to: " + outFile.getAbsolutePath());

		RuleSetCollection rsc = root.getCollection().getRuleSetCollection();
		try {
			XMLWriter.writeXML(rsc.toXmlElement(), outFile);
		} catch (IOException e) {
			LOGGER.warning("Unable to write rulesets to file");
		}
	}

	private void exportConsensus() throws Exception {
		File outFile = new File(root.getSavePath().getParentFile(),
				root.getSavePath().getName() + ".consensus" + Io.SVG_FILE_EXTENSION);
		LOGGER.info("Exporting consensus nucleus to: " + outFile.getAbsolutePath());

		// Ensure all datasets have a consensus
		for (IAnalysisDataset ds : datasets) {
			if (!ds.getCollection().hasConsensus())
				new ConsensusAveragingMethod(ds).call();
		}

		new SVGWriter(outFile).exportConsensusOutlines(datasets, MeasurementScale.MICRONS);
	}

	private void exportCellLocations() throws Exception {
		new CellFileExporter(datasets).call();
	}
}
