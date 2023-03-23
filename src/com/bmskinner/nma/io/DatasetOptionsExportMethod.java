package com.bmskinner.nma.io;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.MultipleDatasetAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;

/**
 * Export analysis options
 * 
 * @author bs19022
 *
 */
public class DatasetOptionsExportMethod extends MultipleDatasetAnalysisMethod implements Io {
	private static final Logger LOGGER = Logger
			.getLogger(DatasetOptionsExportMethod.class.getName());

	/**
	 * Either a file or a folder depending on whether we export one or many datasets
	 */
	private File outputFile;

	/**
	 * Create with a dataset of cells to export
	 * 
	 * @param dataset
	 * @param outputFile the file for the options to be saved as
	 */
	public DatasetOptionsExportMethod(@NonNull IAnalysisDataset dataset, File outputFile) {
		super(dataset);
		this.outputFile = outputFile;
	}

	/**
	 * Create with datasets of cells to export
	 * 
	 * @param datasets
	 * @param outputFolder the folder for the options files to be saved to
	 */
	public DatasetOptionsExportMethod(@NonNull List<IAnalysisDataset> datasets, File outputFolder) {
		super(datasets);
		this.outputFile = outputFolder;
	}

	@Override
	public IAnalysisResult call() throws Exception {

		if (datasets.size() == 1) {
			exportOptions(datasets.get(0), outputFile);
		} else {
			// Each dataset options to a separate file
			for (IAnalysisDataset d : datasets) {
				File f = new File(outputFile, d.getName() + Io.XML_FILE_EXTENSION);
				exportOptions(d, f);
			}
		}

		return new DefaultAnalysisResult(datasets);
	}

	private void exportOptions(IAnalysisDataset d, File f) {

		Optional<IAnalysisOptions> opt = d.getAnalysisOptions();

		if (opt.isEmpty())
			return;

		IAnalysisOptions op = opt.get().duplicate();

		// Remove any folders from the export
		// The point of this is to make a reusable analysis,
		// not replicate existing datasets
		for (String s : op.getDetectionOptionTypes()) {
			op.getDetectionOptions(s).get().remove(HashOptions.DETECTION_FOLDER);
		}

		// Put clustering options into the main analysis options
		for (IClusterGroup g : d.getClusterGroups()) {
			op.setSecondaryOptions(HashOptions.CLUSTER_SUB_OPTIONS_KEY + "_" + g.getName(),
					g.getOptions().get());
		}

		// Also remove the analysis time so we don't use the same output folder
		op.clearAnalysisTime();

		try {
			XMLWriter.writeXML(d.getAnalysisOptions().get().toXmlElement(), f);
		} catch (IOException e) {
			LOGGER.warning("Unable to write options to file");
		}
		LOGGER.info(String.format("Exported %s options to %s", d.getName(),
				f.getAbsolutePath()));
	}
}
