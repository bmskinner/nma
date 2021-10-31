/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.api;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.classification.NucleusClusteringMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.NucleusDetectionMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.ProfileRefoldMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalDetectionMethod;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod.ExportFormat;
import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.io.xml.XMLReader;
import com.bmskinner.nuclear_morphology.reports.ShellReportMethod;

/**
 * A class to replicate a saved xml options file
 * @author ben
 * @since 1.14.0
 *
 */
public class SavedOptionsAnalysisPipeline extends AbstractAnalysisMethod implements AnalysisPipeline {
	
	private static final Logger LOGGER = Logger.getLogger(SavedOptionsAnalysisPipeline.class.getName());
	
	private static final String DATE_FORMAT = "YYYY-MM-dd_HH-mm-ss";
	
	private File xmlFile;
	private File rootFolder;
	private final List<File> imageFolders = new ArrayList<>();
	private File outputFolder = null;
	private final List<IAnalysisDataset> allDatasets = new ArrayList<>();
	private final List<IAnalysisMethod> methodsToRun = new ArrayList<>();
	
	
	/**
	 * Build a pipeline covering all the options within the given file
	 * @param rootFolder the root folder
	 * @param xmlFile the options for analysis
	 */
	public SavedOptionsAnalysisPipeline(@NonNull final File rootFolder, 
			@NonNull final File xmlFile) 
					throws AnalysisPipelineException {
		this(rootFolder, xmlFile, null);
	}

	/**
	 * Build a pipeline covering all the options within the given file
	 * @param rootFolder the root folder
	 * @param xmlFile the options for analysis
	 * @param outputFolder the folder to store the resulting nmd files
	 */
	public SavedOptionsAnalysisPipeline(@NonNull final File rootFolder,
			@NonNull final File xmlFile, 
			@Nullable final File outputFolder)
	throws AnalysisPipelineException {
		this.xmlFile      = xmlFile;
		this.rootFolder  = rootFolder;
		this.outputFolder = outputFolder;
		
		// Check if we can analyse the root folder directly, or
		// whether we need to find all subfolders with 
		// images
		if(Importer.containsImportableImageFiles(rootFolder)) {
			imageFolders.add(rootFolder);
		} else {
			addImageSubfolders(rootFolder);
		}

		if(imageFolders.isEmpty())			
			throw new AnalysisPipelineException("Image folder "
					+rootFolder.getAbsolutePath()+
					" contains no importable images");
		
	}
	
	/**
	 * Test if the subfolders of the given folder contains images
	 * and if so add the subfolders to be analysed
	 * @param folder the folder to test
	 */
	private void addImageSubfolders(File folder) {
		for(File f : folder.listFiles()) {
			if(f.isDirectory()) {
				if(Importer.containsImportableImageFiles(f))
					imageFolders.add(f);
				addImageSubfolders(f);
			}
		}
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();
		return new DefaultAnalysisResult(allDatasets);
	}
	
	@Override
	public void run(@NonNull final File rootFolder, 
			@NonNull final File xmlFile) 
					throws Exception {
		this.xmlFile     = xmlFile;
		this.rootFolder = rootFolder;
		run();
	}

	/**
	 * Build a pipeline covering all the options within the given file
	 * @param rootFolder the image folder
	 * @param xmlFile the options for analysis
	 * @throws Exception
	 */
	public void run() throws Exception {
		methodsToRun.clear();
		if(!rootFolder.exists())
	        throw new IllegalArgumentException("Detection folder does not exist");

		IAnalysisOptions options = XMLReader.readAnalysisOptions(xmlFile);

		if(outputFolder==null)
			outputFolder = createOutputFolder(options);

		// Analyse each folder 
		for(File imageFolder : imageFolders) {
			if(options.hasDetectionOptions(CellularComponent.NUCLEUS)) {
				List<IAnalysisDataset> datasets = createNucleusDetectionMethod(options, imageFolder);
				createRefoldingMethod(datasets);
				createSignalDetectionMethods(datasets, options, imageFolder);
				createClusteringMethods(datasets, options);
				
				for(IAnalysisDataset dataset : datasets)
					methodsToRun.add(new DatasetExportMethod(dataset, dataset.getSavePath(), ExportFormat.XML)); 
				
				allDatasets.addAll(datasets);
			}
		}
    	run(methodsToRun);
	}
	
	private List<IAnalysisDataset> createNucleusDetectionMethod(@NonNull IAnalysisOptions options, 
			File imageFolder) throws Exception {
		options.getDetectionOptions(CellularComponent.NUCLEUS)
		.orElseThrow(MissingOptionException::new)
		.setFile(HashOptions.DETECTION_FOLDER, imageFolder);
		List<IAnalysisDataset> datasets = new NucleusDetectionMethod(outputFolder, options).call().getDatasets();
		for(IAnalysisDataset dataset : datasets) {
			methodsToRun.add(new DatasetProfilingMethod(dataset));
			methodsToRun.add(new DatasetSegmentationMethod(dataset, MorphologyAnalysisMode.NEW));	
		}
		return datasets;

	}
	
	/**
	 * Refold any datasets and child datasets that do not have a consensus nucleus 
	 * @param options
	 * @throws Exception
	 */
	private void createRefoldingMethod(List<IAnalysisDataset> datasets)  {
		for(IAnalysisDataset dataset : datasets) {
			if(dataset.getCollection().getRuleSetCollection().equals(RuleSetCollection.roundRuleSetCollection())) {
				createProfileRefoldMethod(datasets);
			} else {
				createAveragingMethod(datasets);
			}
		}
	}
	
	private void createProfileRefoldMethod(List<IAnalysisDataset> datasets) {
		for(IAnalysisDataset dataset : datasets) {
			if(!dataset.getCollection().hasConsensus())
				methodsToRun.add(new ProfileRefoldMethod(dataset));
			for(IAnalysisDataset d : dataset.getAllChildDatasets())
				if(!d.getCollection().hasConsensus())
					methodsToRun.add(new ProfileRefoldMethod(d));
		}
	}
	
	private void createAveragingMethod(List<IAnalysisDataset> datasets) {
		for(IAnalysisDataset dataset : datasets) {
			if(!dataset.getCollection().hasConsensus())
				methodsToRun.add(new ConsensusAveragingMethod(dataset));
			for(IAnalysisDataset d : dataset.getAllChildDatasets())
				if(!d.getCollection().hasConsensus())
					methodsToRun.add(new ConsensusAveragingMethod(d));
		}
	}
	
	/**
	 * Create the methods to detect signals
	 * @param options
	 * @throws MissingOptionException 
	 * @throws Exception
	 */
	private void createSignalDetectionMethods(List<IAnalysisDataset> datasets, 
			@NonNull IAnalysisOptions options, File imageFolder) throws MissingOptionException {

		for(IAnalysisDataset dataset : datasets) {
			// Add signals
			boolean checkShell = true;
			HashOptions shellOptions = null;
			
			IAnalysisOptions datasetOptions = dataset.getAnalysisOptions().get();
			
			for(UUID signalGroupId : options.getNuclearSignalGroups()) {
				
				HashOptions signalOptions = datasetOptions.getNuclearSignalOptions(signalGroupId).orElseThrow(MissingOptionException::new);
				
				signalOptions.setFile(HashOptions.DETECTION_FOLDER, imageFolder);
				methodsToRun.add(new SignalDetectionMethod(dataset, signalOptions));
				if(checkShell) {
					if(signalOptions.hasBoolean(HashOptions.SHELL_COUNT_INT))
						shellOptions = signalOptions;
					checkShell = false;
				}
			}
			
			// Handle shell analysis setup
			
			if(shellOptions!=null) {
				methodsToRun.add(new ShellAnalysisMethod(dataset, shellOptions));
				methodsToRun.add(new ShellReportMethod(dataset));
			}
		} 
	}
	
	private void createClusteringMethods(List<IAnalysisDataset> datasets, @NonNull IAnalysisOptions options) throws Exception {

		// Get all the sub-options starting with the cluster options key
		List<HashOptions> clusterOptions = options.getSecondaryOptionKeys().stream()
				.filter(s->s.startsWith(HashOptions.CLUSTER_SUB_OPTIONS_KEY))
				.map(s->options.getSecondaryOptions(s).orElseThrow())
				.collect(Collectors.toList());

		for(HashOptions cluster : clusterOptions) {
			for(IAnalysisDataset dataset : datasets) {
				methodsToRun.add(new NucleusClusteringMethod(dataset, cluster));
			}
		}
	}

	private File createOutputFolder(@NonNull IAnalysisOptions options) {
		Instant inst = Instant.ofEpochMilli(options.getAnalysisTime());
		LocalDateTime anTime = LocalDateTime.ofInstant(inst, ZoneOffset.systemDefault());
		String outputFolderName = anTime.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
		return new File(rootFolder, outputFolderName);
	}
		
	private void run(@NonNull List<IAnalysisMethod> methods) throws Exception {
		 for(IAnalysisMethod method : methods) {
			 method.addProgressListener(this);
			 method.call();
			 method.removeProgressListener(this);
			 fireProgressEvent();
		 }
	}
}
