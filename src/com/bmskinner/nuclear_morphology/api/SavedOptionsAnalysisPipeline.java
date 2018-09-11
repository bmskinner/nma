package com.bmskinner.nuclear_morphology.api;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNull;

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
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.IShellOptions;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;
import com.bmskinner.nuclear_morphology.io.OptionsXMLReader;

/**
 * A class to replicate a saved xml options file
 * @author ben
 * @since 1.14.0
 *
 */
public class SavedOptionsAnalysisPipeline extends AbstractAnalysisMethod implements AnalysisPipeline {
	
	private File xmlFile;
	private File imageFolder;
	private List<IAnalysisDataset> datasets;
	private final List<IAnalysisMethod> methodsToRun = new ArrayList<>();
	
	
	/**
	 * Build a pipeline covering all the options within the given file
	 * @param imageFolder the image folder
	 * @param xmlFile the options for analysis
	 * @throws Exception
	 */
	public SavedOptionsAnalysisPipeline(@NonNull final File imageFolder, @NonNull final File xmlFile) {
		this.xmlFile     = xmlFile;
		this.imageFolder = imageFolder;
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

    	String outFolder = createOutputFolderName(options);
    	if(options.hasDetectionOptions(IAnalysisOptions.NUCLEUS)) {
    		createNucleusDetectionMethod(options, outFolder);
    		createRefoldingMethod(options);
    		createSignalDetectionMethods(options);
    		createClusteringMethods();
    		for(IAnalysisDataset dataset : datasets)
    			methodsToRun.add(new DatasetExportMethod(dataset, dataset.getSavePath())); 
    	}
    	
    	run(methodsToRun);
	}
	
	private IAnalysisOptions readOptions() {
		OptionsXMLReader r = new OptionsXMLReader(xmlFile);
		IAnalysisOptions options = r.readAnalysisOptions();
		return options;
	}
	
	private void createNucleusDetectionMethod(@NonNull IAnalysisOptions options, @NonNull String outFolder) throws Exception {
		options.getDetectionOptions(CellularComponent.NUCLEUS).get().setFolder(imageFolder);
		datasets =  new NucleusDetectionMethod(outFolder, options).call().getDatasets();
		for(IAnalysisDataset dataset : datasets)
			methodsToRun.add(new DatasetProfilingMethod(dataset));
		for(IAnalysisDataset dataset : datasets)
			methodsToRun.add(new DatasetSegmentationMethod(dataset, MorphologyAnalysisMode.NEW));
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
	
	private void createSignalDetectionMethods(@NonNull IAnalysisOptions options) throws Exception {
		for(IAnalysisDataset dataset : datasets) {
			// Add signals
			boolean checkShell = true;
			IShellOptions shellOptions = null;
			for(UUID signalGroupId : options.getNuclearSignalGroups()) {
				INuclearSignalOptions nop = options.getNuclearSignalOptions(signalGroupId);
				nop.setFolder(dataset.getCollection().getFolder());
				ISignalGroup group = new SignalGroup("Channel "+nop.getChannel());
				group.setGroupColour(ColourSelecter.getSignalColour(nop.getChannel()));
				dataset.getCollection().addSignalGroup(signalGroupId, group);
				methodsToRun.add(new SignalDetectionMethod(dataset, nop, signalGroupId));
				if(checkShell) {
					if(nop.hasShellOptions())
						shellOptions = nop.getShellOptions();
					checkShell = false;
				}
			}
			if(shellOptions!=null) {
				// filter the dataset for cells that can have a shell analysis applied - if all pass, does nothing
				final int shellCount = shellOptions.getShellNumber();
				Predicate<ICell> p = (c)->{
					return c.getNuclei().stream().allMatch(n->{
						return (n.getStatistic(PlottableStatistic.AREA) > shellCount*ShellAnalysisMethod.MINIMUM_AREA_PER_SHELL
								&& n.getStatistic(PlottableStatistic.CIRCULARITY)>ShellAnalysisMethod.MINIMUM_CIRCULARITY);
					});
				};
				//			 methodsToRun.add(new CellCollectionFilteringMethod(dataset, p, "Suitable_for_shell_analysis"));
				methodsToRun.add(new ShellAnalysisMethod(dataset, shellOptions));
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
		
	private String createOutputFolderName(@NonNull IAnalysisOptions options) {
		Instant inst = Instant.ofEpochMilli(options.getAnalysisTime());
		LocalDateTime anTime = LocalDateTime.ofInstant(inst, ZoneOffset.systemDefault());
		String outputFolderName = anTime.format(DateTimeFormatter.ofPattern("YYYY-MM-dd_HH-mm-ss"));
		return outputFolderName;
	}
		
	private void run(@NonNull List<IAnalysisMethod> methods) throws Exception {
		 fireUpdateProgressTotalLength(methods.size());
		 for(IAnalysisMethod method : methods) {
			 System.out.println("Running "+method.getClass().getSimpleName());
			 method.call();
			 fireProgressEvent();
		 }
	}
}
