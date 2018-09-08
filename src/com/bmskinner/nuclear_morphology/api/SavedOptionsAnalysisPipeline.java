package com.bmskinner.nuclear_morphology.api;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.classification.NucleusClusteringMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.NucleusDetectionMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalDetectionMethod;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.OptionsXMLReader;

/**
 * A class to replicate a saved xml options file
 * @author ben
 * @since 1.14.0
 *
 */
public class SavedOptionsAnalysisPipeline extends AbstractAnalysisMethod {
	
	private final File xmlFile;
	private final File imageFolder;
	private IAnalysisDataset dataset;
	
	
	/**
	 * Build a pipeline covering all the options within the given file
	 * @param imageFolder the image folder
	 * @param xmlFile the options for analysis
	 * @throws Exception
	 */
	public SavedOptionsAnalysisPipeline(@NonNull final File imageFolder, @NonNull final File xmlFile) {
		this.xmlFile = xmlFile;
		this.imageFolder = imageFolder;
	}
	
	@Override
	public IAnalysisResult call() throws Exception {
		run();
		return new DefaultAnalysisResult(dataset);
	}

	/**
	 * Build a pipeline covering all the options within the given file
	 * @param imageFolder the image folder
	 * @param xmlFile the options for analysis
	 * @throws Exception
	 */
	public void run() throws Exception {
		
		if(!imageFolder.exists())
	        throw new IllegalArgumentException("Detection folder does not exist");
		
		OptionsXMLReader r = new OptionsXMLReader(xmlFile);
		IAnalysisOptions options = r.readAnalysisOptions();
		
		Instant inst = Instant.ofEpochMilli(options.getAnalysisTime());
		LocalDateTime anTime = LocalDateTime.ofInstant(inst, ZoneOffset.systemDefault());
		String outputFolderName = anTime.format(DateTimeFormatter.ofPattern("YYYY-MM-dd_HH-mm-ss"));
    	File outFolder = new File(imageFolder, outputFolderName);
    	outFolder.mkdirs();
    	File saveFile = new File(outFolder, imageFolder.getName()+Io.SAVE_FILE_EXTENSION);

    	if(options.hasDetectionOptions(IAnalysisOptions.NUCLEUS)) {
    		options.getDetectionOptions(CellularComponent.NUCLEUS).get().setFolder(imageFolder);
    		dataset =  new NucleusDetectionMethod(outputFolderName, options).call().getFirstDataset();
    		 
    		 List<IAnalysisMethod> methodsToRun = new ArrayList<>();
    		 
    		 methodsToRun.add(new DatasetProfilingMethod(dataset));
    		 methodsToRun.add(new DatasetSegmentationMethod(dataset, MorphologyAnalysisMode.NEW));
    		 methodsToRun.add(new ConsensusAveragingMethod(dataset));
    		 
    		 // Add signals
    		 boolean checkShell = true;
    		 IAnalysisMethod shellMethod = null;
    		 for(UUID signalGroupId : options.getNuclearSignalGroups()) {
    			 INuclearSignalOptions nop = options.getNuclearSignalOptions(signalGroupId);
    			 nop.setFolder(imageFolder);
    			 ISignalGroup group = new SignalGroup("Channel "+nop.getChannel());
    			 group.setFolder(imageFolder);
    			 group.setGroupColour(ColourSelecter.getSignalColour(nop.getChannel()));
    			 dataset.getCollection().addSignalGroup(signalGroupId, group);
    			 methodsToRun.add(new SignalDetectionMethod(dataset, nop, signalGroupId));
    			if(checkShell) {
    				if(nop.hasShellOptions())
    					shellMethod = new ShellAnalysisMethod(dataset, nop.getShellOptions());
    				checkShell = false;
    			}
    		 }
    		 if(shellMethod!=null)
    			 methodsToRun.add(shellMethod);
    		 
    		 // Run clustering
    		 List<IClusteringOptions> clusterOptions = r.readClusteringOptions();
    		 for(IClusteringOptions cluster : clusterOptions) {
    			 methodsToRun.add(new NucleusClusteringMethod(dataset, cluster));
    		 }
    		 
    		 // Save
    		 methodsToRun.add(new DatasetExportMethod(dataset, saveFile));
    		 
    		 for(IAnalysisMethod method : methodsToRun) {
    			 System.out.println("Running "+method.getClass().getSimpleName());
    			 method.call();
    		 }
    	}
	}
}
