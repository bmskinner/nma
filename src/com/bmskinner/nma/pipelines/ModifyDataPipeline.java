package com.bmskinner.nma.pipelines;

import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.classification.ClusterFileAssignmentMethod;
import com.bmskinner.nma.analysis.classification.NucleusClusteringMethod;
import com.bmskinner.nma.analysis.classification.PrincipalComponentAnalysis;
import com.bmskinner.nma.analysis.classification.TsneMethod;
import com.bmskinner.nma.analysis.classification.UMAPMethod;
import com.bmskinner.nma.analysis.signals.SignalDetectionMethod;
import com.bmskinner.nma.analysis.signals.shells.ShellAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.core.CommandOptions;
import com.bmskinner.nma.io.DatasetExportMethod;
import com.bmskinner.nma.io.DatasetImportMethod;
import com.bmskinner.nma.io.XMLImportMethod;
import com.bmskinner.nma.io.XMLReader;

public class ModifyDataPipeline {
	private static final Logger LOGGER = Logger
			.getLogger(ModifyDataPipeline.class.getName());

	private CommandOptions opt;

	private IAnalysisDataset root;

	/**
	 * Build a pipeline covering all the options within the given file
	 * 
	 * @param xmlFile the options for analysis
	 * @throws Exception
	 */

	public ModifyDataPipeline(@NonNull final CommandOptions opt)
			throws Exception {

		this.opt = opt;

		if (opt.file != null) {
			root = readDataset();
			LOGGER.info("Read dataset from file");
		}

		if (opt.clusterFile != null)
			addClusterFromFile();

		if (opt.options != null)
			addOptionsAnalyses();

		// Save the modified dataset
		new DatasetExportMethod(root, root.getSavePath()).call();

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

	private void addOptionsAnalyses() throws Exception {

		IAnalysisOptions options = XMLReader.readAnalysisOptions(opt.options);

		runSignalDetectionMethods(options);

		runClusteringMethods(options);
	}

	/**
	 * Create the methods to detect signals
	 * 
	 * @param options
	 * @throws Exception
	 */
	private void runSignalDetectionMethods(@NonNull IAnalysisOptions options) throws Exception {

		// Add all signal groups
		for (UUID signalGroupId : options.getNuclearSignalGroups()) {

			HashOptions signalOptions = options.getNuclearSignalOptions(signalGroupId)
					.orElseThrow(MissingOptionException::new);

			// Detect signals and run shell analysis if needed
			new SignalDetectionMethod(root, signalOptions,
					root.getAnalysisOptions().get().getNucleusDetectionFolder().get()).call();

			if (signalOptions.hasBoolean(HashOptions.SHELL_COUNT_INT)) {
				new ShellAnalysisMethod(root, signalOptions).call();
			}

			// Add the new options to the dataset
			root.getAnalysisOptions().get().setNuclearSignalDetectionFolder(signalGroupId,
					root.getAnalysisOptions().get().getNucleusDetectionFolder().get());

			root.getAnalysisOptions().get().setNuclearSignalDetectionOptions(signalOptions);
		}
	}

	/**
	 * Create methods needed for dimensionality reduction for clusters
	 * 
	 * @param datasets
	 * @param options
	 * @throws Exception
	 */
	private void runClusteringMethods(@NonNull IAnalysisOptions options)
			throws Exception {

		// Get all the sub-options starting with the cluster options key
		for (String s : options.getSecondaryOptionKeys()) {

			HashOptions ops = options.getSecondaryOptions(s).orElseThrow();
			root.getAnalysisOptions().get().setSecondaryOptions(s, ops);

			if (ops.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY)) {
				new PrincipalComponentAnalysis(root, ops).call();
			}

			if (ops.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY)) {
				new TsneMethod(root, ops).call();
			}

			if (ops.getBoolean(HashOptions.CLUSTER_USE_UMAP_KEY)) {
				new UMAPMethod(root, ops).call();
			}

			new NucleusClusteringMethod(root, ops).call();

		}
	}

}
