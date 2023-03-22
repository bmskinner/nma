package com.bmskinner.nma.pipelines;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.classification.ClusterFileAssignmentMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.CommandOptions;
import com.bmskinner.nma.io.DatasetExportMethod;
import com.bmskinner.nma.io.DatasetImportMethod;
import com.bmskinner.nma.io.XMLImportMethod;

public class AnalyseDataPipeline {
	private static final Logger LOGGER = Logger
			.getLogger(AnalyseDataPipeline.class.getName());

	private CommandOptions opt;

	private IAnalysisDataset root;
	private List<IAnalysisDataset> datasets = new ArrayList<>();

	/**
	 * Build a pipeline covering all the options within the given file
	 * 
	 * @param xmlFile the options for analysis
	 * @throws Exception
	 */

	public AnalyseDataPipeline(@NonNull final CommandOptions opt)
			throws Exception {

		this.opt = opt;
		datasets = readDataset();

		LOGGER.info("Read dataset from file");

		if (opt.clusterFile != null)
			addClusterFromFile();

		LOGGER.info("Analyses complete");
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

	private void addClusterFromFile() throws Exception {
		LOGGER.info("Importing clusters from: " + opt.clusterFile.getAbsolutePath());
		new ClusterFileAssignmentMethod(root, opt.clusterFile).call();
		new DatasetExportMethod(root, root.getSavePath()).call();
	}
}
