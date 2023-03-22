package com.bmskinner.nma.pipelines;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DatasetMergeMethod;
import com.bmskinner.nma.analysis.classification.ClusterFileAssignmentMethod;
import com.bmskinner.nma.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nma.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nma.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nma.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.CommandOptions;
import com.bmskinner.nma.gui.dialogs.DatasetArithmeticSetupDialog.BooleanOperation;
import com.bmskinner.nma.io.DatasetExportMethod;
import com.bmskinner.nma.io.DatasetImportMethod;
import com.bmskinner.nma.io.XMLImportMethod;

public class AnalyseDataPipeline {
	private static final Logger LOGGER = Logger
			.getLogger(AnalyseDataPipeline.class.getName());

	private CommandOptions opt;

	private IAnalysisDataset root;

	/**
	 * Build a pipeline covering all the options within the given file
	 * 
	 * @param xmlFile the options for analysis
	 * @throws Exception
	 */

	public AnalyseDataPipeline(@NonNull final CommandOptions opt)
			throws Exception {

		this.opt = opt;

		if (opt.file != null) {
			root = readDataset();
			LOGGER.info("Read dataset from file");
		}

		if (opt.clusterFile != null)
			addClusterFromFile();

		if (opt.mergeSources != null)
			mergeDatasets();

		LOGGER.info("Analyses complete");
	}

	private IAnalysisDataset readDataset() throws Exception {
		XMLImportMethod m = new XMLImportMethod(opt.file);
		m.call();

		return new DatasetImportMethod(m.getXMLDocument()).call()
				.getFirstDataset();
	}

	private void addClusterFromFile() throws Exception {
		LOGGER.info("Importing clusters from: " + opt.clusterFile.getAbsolutePath());
		new ClusterFileAssignmentMethod(root, opt.clusterFile).call();
		new DatasetExportMethod(root, root.getSavePath()).call();
	}

	private void mergeDatasets() throws Exception {
		
		List<IAnalysisDataset> sources = new ArrayList<>();

		for (File f : opt.mergeSources) {
			XMLImportMethod m = new XMLImportMethod(f);
			m.call();
			IAnalysisDataset d = new DatasetImportMethod(m.getXMLDocument()).call()
			.getFirstDataset();
			sources.add(d);
			LOGGER.info(() -> "Read %s with %d cells".formatted(d.getName(), d.getCollection().size()));
		}

		LOGGER.info("Merging into " + opt.output.getAbsolutePath());
		IAnalysisDataset merged = new DatasetMergeMethod(sources, BooleanOperation.OR, opt.output).call()
				.getFirstDataset();
		
		new DatasetProfilingMethod(merged)
				.then(new DatasetSegmentationMethod(merged,
				MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH))
				.then(new ConsensusAveragingMethod(merged))
				.then(new DatasetExportMethod(merged, merged.getSavePath()))
				.call();
	}
}
