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
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

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
import com.bmskinner.nuclear_morphology.analysis.nucleus.ProfileRefoldMethod.CurveRefoldingMode;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalDetectionMethod;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.IShellOptions;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod.ExportFormat;
import com.bmskinner.nuclear_morphology.io.xml.OptionsXMLReader;
import com.bmskinner.nuclear_morphology.io.xml.XMLReader.XMLReadingException;
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
	private File imageFolder;
	private File outputFolder = null;
	private List<IAnalysisDataset> datasets;
	private final List<IAnalysisMethod> methodsToRun = new ArrayList<>();
	
	
	/**
	 * Build a pipeline covering all the options within the given file
	 * @param imageFolder the image folder
	 * @param xmlFile the options for analysis
	 */
	public SavedOptionsAnalysisPipeline(@NonNull final File imageFolder, @NonNull final File xmlFile) {
		this(imageFolder, xmlFile, null);
	}
	
	/**
	 * Build a pipeline covering all the options within the given file
	 * @param imageFolder the image folder
	 * @param xmlFile the options for analysis
	 * @param outputFolder the folder to store the resulting nmd files
	 */
	public SavedOptionsAnalysisPipeline(@NonNull final File imageFolder, @NonNull final File xmlFile, @Nullable final File outputFolder) {
		this.xmlFile      = xmlFile;
		this.imageFolder  = imageFolder;
		this.outputFolder = outputFolder;
	}
	
	@Override
	public IAnalysisResult call() throws Exception {
		run();
		return new DefaultAnalysisResult(datasets);
	}
	
	@Override
	public void run(@NonNull final File imageFolder, @NonNull final File xmlFile) throws AnalysisPipelineException {
		this.xmlFile     = xmlFile;
		this.imageFolder = imageFolder;
		try {
			run();
		} catch(Exception e) {
			throw new AnalysisPipelineException(e);
		}
	}

	/**
	 * Build a pipeline covering all the options within the given file
	 * @param imageFolder the image folder
	 * @param xmlFile the options for analysis
	 * @throws Exception
	 */
	public void run() throws Exception {
		methodsToRun.clear();
		if(!imageFolder.exists())
	        throw new IllegalArgumentException("Detection folder does not exist");

		IAnalysisOptions options = readOptions();

		if(outputFolder==null)
			outputFolder = createOutputFolder(options);
		
    	if(options.hasDetectionOptions(CellularComponent.NUCLEUS)) {
    		createNucleusDetectionMethod(options);
    		createRefoldingMethod(options);
    		createSignalDetectionMethods(options);
    		createClusteringMethods();
    		
    		for(IAnalysisDataset dataset : datasets)
    			methodsToRun.add(new DatasetExportMethod(dataset, dataset.getSavePath(), ExportFormat.JAVA)); 
    	}
    	
    	run(methodsToRun);
	}
	
	/**
	 * Read the options XML file and create analysis options object
	 * @return an analysis options object with the options from file
	 * @throws XMLReadingException if the file cannot be read
	 */
	private IAnalysisOptions readOptions() throws XMLReadingException {
		OptionsXMLReader r = new OptionsXMLReader(xmlFile);
		IAnalysisOptions options = r.read();
		return options;
	}
	
	private void createNucleusDetectionMethod(@NonNull IAnalysisOptions options) throws Exception {
		options.getDetectionOptions(CellularComponent.NUCLEUS).get().setFolder(imageFolder);
		datasets =  new NucleusDetectionMethod(outputFolder, options).call().getDatasets();
		for(IAnalysisDataset dataset : datasets) {
			methodsToRun.add(new DatasetProfilingMethod(dataset));
			methodsToRun.add(new DatasetSegmentationMethod(dataset, MorphologyAnalysisMode.NEW));	
		}

	}
	
	/**
	 * Refold any datasets and child datasets that do not have a consensus nucleus 
	 * @param options
	 * @throws Exception
	 */
	private void createRefoldingMethod(@NonNull IAnalysisOptions options) throws Exception {
		for(IAnalysisDataset dataset : datasets) {
			// Refold
			NucleusType t = options.getNucleusType();
			switch(t){
				case ROUND:
				case NEUTROPHIL: {
					if(!dataset.getCollection().hasConsensus())
						methodsToRun.add(new ProfileRefoldMethod(dataset, CurveRefoldingMode.FAST));
					for(IAnalysisDataset d : dataset.getAllChildDatasets())
						if(!d.getCollection().hasConsensus())
							methodsToRun.add(new ProfileRefoldMethod(d, CurveRefoldingMode.FAST));
					break;
				}
				default: {
					if(!dataset.getCollection().hasConsensus())
						methodsToRun.add(new ConsensusAveragingMethod(dataset));
					for(IAnalysisDataset d : dataset.getAllChildDatasets())
						if(!d.getCollection().hasConsensus())
							methodsToRun.add(new ConsensusAveragingMethod(d));
				}
			}
		}
	}
	
	/**
	 * Create the methods to detect signals
	 * @param options
	 * @throws Exception
	 */
	private void createSignalDetectionMethods(@NonNull IAnalysisOptions options) throws Exception {
		
		OptionsXMLReader r = new OptionsXMLReader(xmlFile);
		Map<UUID, String> signalNames = r.readSignalGroupNames();
		
		for(IAnalysisDataset dataset : datasets) {
			// Add signals
			boolean checkShell = true;
			IShellOptions shellOptions = null;
			
			IAnalysisOptions datasetOptions = dataset.getAnalysisOptions().get();
			
			for(UUID signalGroupId : options.getNuclearSignalGroups()) {
				
				INuclearSignalOptions signalOptions = datasetOptions.getNuclearSignalOptions(signalGroupId);
				signalOptions.setFolder(imageFolder);
				ISignalGroup signalGroup = new SignalGroup(signalNames.get(signalGroupId));
				signalGroup.setGroupColour(ColourSelecter.getSignalColour(signalOptions.getChannel()));
				
				LOGGER.info("Set signal group "+signalGroup.getGroupName()+" to "+signalOptions.getFolder());
				
				dataset.getCollection().addSignalGroup(signalGroupId, signalGroup);
				methodsToRun.add(new SignalDetectionMethod(dataset, signalOptions, signalGroupId));
				if(checkShell) {
					if(signalOptions.hasShellOptions())
						shellOptions = signalOptions.getShellOptions();
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
	
	private void createClusteringMethods() throws Exception {
		OptionsXMLReader r = new OptionsXMLReader(xmlFile);
		List<IClusteringOptions> clusterOptions = r.readClusteringOptions();
		 for(IClusteringOptions cluster : clusterOptions) {
			 for(IAnalysisDataset dataset : datasets) {
				 methodsToRun.add(new NucleusClusteringMethod(dataset, cluster));
			 }
		 }
	}
		
	private File createOutputFolder(@NonNull IAnalysisOptions options) {
		Instant inst = Instant.ofEpochMilli(options.getAnalysisTime());
		LocalDateTime anTime = LocalDateTime.ofInstant(inst, ZoneOffset.systemDefault());
		String outputFolderName = anTime.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
		return new File(imageFolder, outputFolderName);
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
