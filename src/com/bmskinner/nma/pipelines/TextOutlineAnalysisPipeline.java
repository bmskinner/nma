package com.bmskinner.nma.pipelines;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.analysis.AbstractAnalysisMethod;
import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.classification.NucleusClusteringMethod;
import com.bmskinner.nma.analysis.classification.PrincipalComponentAnalysis;
import com.bmskinner.nma.analysis.classification.TsneMethod;
import com.bmskinner.nma.analysis.classification.UMAPMethod;
import com.bmskinner.nma.analysis.detection.TextFileDetectionMethod;
import com.bmskinner.nma.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nma.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nma.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nma.analysis.profiles.DefaultDatasetProfilingMethod;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.io.DatasetExportMethod;
import com.bmskinner.nma.io.XMLReader;

public class TextOutlineAnalysisPipeline extends AbstractAnalysisMethod
		implements AnalysisPipeline {
	private static final Logger LOGGER = Logger.getLogger(TextOutlineAnalysisPipeline.class.getName());

	private static final String DATE_FORMAT = "YYYY-MM-dd_HH-mm-ss";

	private File optionsXmlFile;
	private File textFile;
	private File outputFolder = null;
	private final List<IAnalysisDataset> allDatasets = new ArrayList<>();
	private final List<IAnalysisMethod> methodsToRun = new ArrayList<>();

	/**
	 * Build a pipeline covering all the options within the given file
	 * 
	 * @param rootFolder the root folder
	 * @param xmlFile    the options for analysis
	 */
	public TextOutlineAnalysisPipeline(@NonNull final File textFile,
			@NonNull final File optionsXmlFile)
			throws AnalysisPipelineException {
		this(textFile, optionsXmlFile, null);
	}

	/**
	 * Build a pipeline covering all the options within the given file
	 * 
	 * @param rootFolder   the root folder
	 * @param xmlFile      the options for analysis
	 * @param outputFolder the folder to store the resulting nmd files
	 */
	public TextOutlineAnalysisPipeline(@NonNull final File textFile,
			@NonNull final File optionsXmlFile,
			@Nullable final File outputFolder)
			throws AnalysisPipelineException {
		this.optionsXmlFile = optionsXmlFile;
		this.textFile = textFile;
		this.outputFolder = outputFolder;


		if (!textFile.canRead())
			throw new AnalysisPipelineException(
					"Cannot read file with text outlines '%s'".formatted(textFile.getAbsolutePath()));

		if (!optionsXmlFile.canRead())
			throw new AnalysisPipelineException(
					"Cannot read file with options file '%s'".formatted(optionsXmlFile.getAbsolutePath()));

	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();
		return new DefaultAnalysisResult(allDatasets);
	}

	@Override
	public void run(@NonNull final File textFile,
			@NonNull final File optionsXmlFile)
			throws Exception {
		this.optionsXmlFile = optionsXmlFile;
		this.textFile = textFile;
		run();
	}

	/**
	 * Build a pipeline covering all the options within the given file
	 * 
	 * @throws Exception
	 */
	public void run() throws Exception {
		methodsToRun.clear();
		if (!textFile.exists())
			throw new IllegalArgumentException("Text outlines file does not exist");

		final IAnalysisOptions options = XMLReader.readAnalysisOptions(optionsXmlFile);

		if (outputFolder == null) {
			outputFolder = createOutputFolder(options);
		}

		LOGGER.finer("Output to '%s'".formatted(outputFolder.getAbsolutePath()));

		// Analyse the text file
		if (options.hasDetectionOptions(CellularComponent.NUCLEUS)) {
			final List<IAnalysisDataset> datasets = createTextOutlineDetectionMethod(options, textFile);
			createRefoldingMethod(datasets);

			createDimensionalityReductionMethods(datasets, options);
			createClusteringMethods(datasets, options);

			for (final IAnalysisDataset dataset : datasets) {
				methodsToRun.add(new DatasetExportMethod(dataset, dataset.getSavePath()));
			}

			allDatasets.addAll(datasets);

		}
		run(methodsToRun);
	}

	/**
	 * Create a directory name in the same directory as the input text outlines file
	 * 
	 * @param options the analysis options
	 * @return
	 */
	private File createOutputFolder(@NonNull IAnalysisOptions options) {
		LOGGER.fine("Making output folder name");
		final Instant inst = Instant.ofEpochMilli(options.getAnalysisTime());
		final LocalDateTime anTime = LocalDateTime.ofInstant(inst, ZoneOffset.systemDefault());
		final String outputFolderName = anTime.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
		return new File(textFile.getParentFile(), outputFolderName);
	}

	private List<IAnalysisDataset> createTextOutlineDetectionMethod(@NonNull IAnalysisOptions options,
			File textFile) throws Exception {

		options.getNucleusDetectionOptions().get().setFile(HashOptions.COORDINATE_LOCATION_FILE_KEY, textFile);

		final List<IAnalysisDataset> datasets = new TextFileDetectionMethod(outputFolder, options).call()
				.getDatasets();
		for (final IAnalysisDataset dataset : datasets) {
			methodsToRun.add(new DefaultDatasetProfilingMethod(dataset));
			methodsToRun.add(new DatasetSegmentationMethod(dataset,
					MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH));
		}
		return datasets;

	}

	/**
	 * Refold any datasets and child datasets that do not have a consensus nucleus
	 * 
	 * @param options
	 * @throws Exception
	 */
	private void createRefoldingMethod(List<IAnalysisDataset> datasets) {
		for (final IAnalysisDataset dataset : datasets) {
			if (dataset.getCollection().getRuleSetCollection()
					.equals(RuleSetCollection.roundRuleSetCollection())) {
				createAveragingMethod(datasets);
			} else {
				createAveragingMethod(datasets);
			}
		}
	}

	private void createAveragingMethod(List<IAnalysisDataset> datasets) {
		for (final IAnalysisDataset dataset : datasets) {
			if (!dataset.getCollection().hasConsensus()) {
				methodsToRun.add(new ConsensusAveragingMethod(dataset));
			}
			for (final IAnalysisDataset d : dataset.getAllChildDatasets())
				if (!d.getCollection().hasConsensus()) {
					methodsToRun.add(new ConsensusAveragingMethod(d));
				}
		}
	}

	/**
	 * Create methods needed for dimensionality reduction for clusters
	 * 
	 * @param datasets
	 * @param options
	 */
	private void createDimensionalityReductionMethods(List<IAnalysisDataset> datasets,
			@NonNull IAnalysisOptions options) {
		// Get all the sub-options starting with the cluster options key
		final List<HashOptions> clusterOptions = options.getSecondaryOptionKeys().stream()
				.filter(s -> s.startsWith(HashOptions.CLUSTER_SUB_OPTIONS_KEY))
				.map(s -> options.getSecondaryOptions(s).orElseThrow())
				.collect(Collectors.toList());

		for (final HashOptions ops : clusterOptions) {

			if (ops.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY)) {
				for (final IAnalysisDataset dataset : datasets) {
					methodsToRun.add(new PrincipalComponentAnalysis(dataset, ops));
				}
			}

			if (ops.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY)) {
				for (final IAnalysisDataset dataset : datasets) {
					methodsToRun.add(new TsneMethod(dataset, ops));
				}
			}

			if (ops.getBoolean(HashOptions.CLUSTER_USE_UMAP_KEY)) {
				for (final IAnalysisDataset dataset : datasets) {
					methodsToRun.add(new UMAPMethod(dataset, ops));
				}
			}

		}
	}

	private void createClusteringMethods(List<IAnalysisDataset> datasets,
			@NonNull IAnalysisOptions options) throws Exception {

		// Get all the sub-options starting with the cluster options key
		final List<HashOptions> clusterOptions = options.getSecondaryOptionKeys().stream()
				.filter(s -> s.startsWith(HashOptions.CLUSTER_SUB_OPTIONS_KEY))
				.map(s -> options.getSecondaryOptions(s).orElseThrow())
				.collect(Collectors.toList());

		for (final HashOptions ops : clusterOptions) {
			LOGGER.fine("Adding clustering option");
			for (final IAnalysisDataset dataset : datasets) {
				methodsToRun.add(new NucleusClusteringMethod(dataset, ops));
			}
		}
	}


}
