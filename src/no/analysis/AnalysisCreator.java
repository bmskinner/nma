/*
-------------------------------------------------
ANALYSIS CREATOR
-------------------------------------------------
Copyright (C) Ben Skinner 2015

This class allows easy setup of the parameters that
can be varied in the nucleus and signal detection

 */
package no.analysis;

import ij.IJ;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import cell.Cell;
import utility.Constants;
import utility.Logger;
import no.collections.CellCollection;
import no.components.AnalysisOptions;
import no.export.CompositeExporter;
import no.export.NucleusAnnotator;
import no.export.PopulationExporter;
import no.export.StatsExporter;
import no.gui.AnalysisSetupWindow;
import no.gui.MainWindow;
import no.nuclei.Nucleus;
import no.nuclei.RoundNucleus;


public class AnalysisCreator extends SwingWorker<Boolean, Integer> implements PropertyChangeListener {

//	private MainWindow mw; // use to log and update gui

	private static final String spacerString = "------------";

	private Logger logger;

	private AnalysisOptions analysisOptions; // store the options
	private int mappingCount = 0;

	private Date startTime; // the time the analysis began

	private String outputFolderName;
	
//	private int totalImages;

	/**
	 * Will be set true if a primary analysis was run
	 */
	private boolean analysisRun = false;

	/**
	 * Will be set true if a reanalysis was run
	 */
	private boolean reAnalysisRun = false;

	/**
	 * Will be set true if all parameters have been set,
	 * and an analysis can be run
	 */
	private boolean readyToRun = false;

	private Map<File, LinkedHashMap<String, Integer>> collectionNucleusCounts = new HashMap<File, LinkedHashMap<String, Integer>>();

	// the raw input from nucleus detector
//	private Map<File, CellCollection> folderCollection;

	private List<CellCollection> nuclearPopulations = new ArrayList<CellCollection>(0);
//	private List<AnalysisDataset> nuclearDatasets = new ArrayList<AnalysisDataset>(0);

	private List<CellCollection> finalPopulations = new ArrayList<CellCollection>(0);
	private List<AnalysisDataset> finalDatasets = new ArrayList<AnalysisDataset>(0);

	/*
    -----------------------
    Constructors
    -----------------------
	 */
	public AnalysisCreator(List<CellCollection> collections){
//		this.mw = mw;
		this.nuclearPopulations = collections;
//		this.initialise();
	}

//	public void initialise(){
//
//		AnalysisSetupWindow analysisSetup = new AnalysisSetupWindow();
//		if( analysisSetup.getOptions()!=null){
//
//			this.analysisOptions = analysisSetup.getOptions();
//
//			mw.log("Directory: "+analysisOptions.getFolder().getName());
//
//			this.startTime = Calendar.getInstance().getTime();
//			this.outputFolderName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(this.startTime);
//
//			// craete the analysis folder early. Did not before in case folder had no images
//			File analysisFolder = new File(analysisOptions.getFolder().getAbsolutePath()+File.separator+outputFolderName);
//			if(!analysisFolder.exists()){
//				analysisFolder.mkdir();
//			}
//			this.logger = new Logger( new File(analysisOptions.getFolder().getAbsolutePath()+File.separator+outputFolderName+File.separator+"log.debug.txt"), "AnalysisCreator");
//
//			logger.log("Analysis began: "+analysisFolder.getAbsolutePath());
//			logger.log("Directory: "+analysisOptions.getFolder().getName());
//			this.readyToRun = true;
//		}
//	}
	
	@Override
	protected void process( List<Integer> integers ) {
		//update the number of entries added
		int total = this.nuclearPopulations.size();
		int amount = integers.get( integers.size() - 1 );
		int percent = (int) ( (double) amount / (double) total * 100);
		setProgress(percent); // the integer representation of the percent
	}
	
	@Override
	protected Boolean doInBackground() throws Exception {

		boolean result = false;
		try{
//			if(!analysisOptions.isReanalysis()){
//				this.runAnalysis();
//			} else {
//				this.runReAnalysis();
//			}
//			this.assignNucleusTypes();
			this.analysePopulations();
			//		this.exportAnalysisLog();

//			mw.log(spacerString);
//			mw.log("All done!" );
//			mw.log(spacerString);
//			logger.log("All done!");
			result = true;
		} catch (Exception e){
			result = false;
		}
		return result;
	}
	
	@Override
	public void done() {

		try {
			if(this.get()){
				firePropertyChange("Finished", getProgress(), Constants.PROGRESS_FINISHED);			
				
			} else {
				firePropertyChange("Error", getProgress(), Constants.PROGRESS_ERROR);
			}
		} catch (InterruptedException e) {
			logger.log("Error in signal detection: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		} catch (ExecutionException e) {
			logger.log("Error in signal detection: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		}

	} 


//	public void run(){
//
//		if(!readyToRun) return;
//
//		if(!analysisOptions.isReanalysis()){
//			this.runAnalysis();
//		} else {
//			this.runReAnalysis();
//		}
//		this.assignNucleusTypes();
//		this.analysePopulations();
////		this.exportAnalysisLog();
//
//		mw.log(spacerString);
//		mw.log("All done!" );
//		mw.log(spacerString);
//		logger.log("All done!");
//	}

	/**
	 * Get the populations from this analysis. Allows re/sub analysis to take place
	 * outside the AnalysisCreator
	 * @return the collections of nuclei
	 */
//	public List<CellCollection> getPopulations(){
//		return this.finalPopulations;
//	}

	/**
	 * Get the datasets from this analysis. Allows re/sub analysis to take place
	 * outside the AnalysisCreator
	 * @return the datasets
	 */
	public List<AnalysisDataset> getDatasets(){
		return this.finalDatasets;
	}

	/*
    -----------------------
    Run the analysis
    -----------------------
	 */

	/**
	 * Returns a HashMap<File, NucleusCollection> object. This 
	 * contains the nuclei found, keyed to the folder in which
	 * they were found. 
	 *
	 * @return      the nuclei in each folder analysed
	 * @see         RoundNucleusCollection
	 */

//	public void runAnalysis(){
//		NucleusDetector detector = new NucleusDetector(this.outputFolderName, this.mw, logger.getLogfile(), analysisOptions);
//		detector.addPropertyChangeListener(this);
////		detector.runDetector();
//		detector.execute();
//
//		this.folderCollection = detector.getNucleiCollections();
//		logger.log("Imported folder(s)");
//		mw.log("Imported folder(s)");
//		this.analysisRun = true;
//	}

	/**
	 * Run an analysis of selected nuclei based on a mapping file
	 *
	 * @return      the nuclei in each folder analysed
	 */
//	public void runReAnalysis(){
//		NucleusRefinder detector = new NucleusRefinder(this.outputFolderName, analysisOptions.getMappingFile(), this.mw, logger.getLogfile(), analysisOptions);
//		//	  setDetectionParameters(detector);
//		detector.setXOffset(analysisOptions.getXOffset());
//		detector.setYOffset(analysisOptions.getYOffset());
//		detector.setRealignMode(analysisOptions.realignImages());
////		detector.runDetector();
//		this.folderCollection = detector.getNucleiCollections();
//		this.mappingCount = detector.getMappingCount();
//
//		logger.log("Imported folder(s)");
//		mw.log("Imported folder(s)");
//		this.reAnalysisRun = true;
//	}

	/*
    Use reflection to assign the correct class to the nuclei and populations
	 */
//	public void assignNucleusTypes(){
//
//		Set<File> keys = this.folderCollection.keySet();
//		logger.log("Assigning nucleus types");
//
//		for (File key : keys) {
//			CellCollection collection = folderCollection.get(key);
//			this.nuclearPopulations.add(collection);
//		}
//	}

	public void analysePopulations(){
//		mw.log("Beginning analysis");
		logger.log("Beginning population analysis");

		int progress = 0;
		for(CellCollection r : this.nuclearPopulations){

			AnalysisDataset dataset = new AnalysisDataset(r);
			dataset.setAnalysisOptions(analysisOptions);
			dataset.setRoot(true);

			File folder = r.getFolder();
			//		  mw.log(spacerString);
//			mw.log("Analysing: "+folder.getName());
			logger.log("Analysing: "+folder.getName());
			//		  mw.log(spacerString);

			LinkedHashMap<String, Integer> nucleusCounts = new LinkedHashMap<String, Integer>();

			try{

				nucleusCounts.put("input", r.getNucleusCount());
				CellCollection failedNuclei = new CellCollection(folder, r.getOutputFolderName(), "failed", logger.getLogfile(), analysisOptions.getNucleusClass());

//				boolean ok;
//				mw.logc("Filtering collection...");
				boolean ok = CollectionFilterer.run(r, failedNuclei); // put fails into failedNuclei, remove from r
				if(ok){
//					mw.log("OK");
				} else {
//					mw.log("Error");
				}

				if(failedNuclei.getNucleusCount()>0){
//					mw.logc("Exporting failed nuclei...");
					ok = CompositeExporter.run(failedNuclei);
					if(ok){
//						mw.log("OK");
					} else {
//						mw.log("Error");
					}
					nucleusCounts.put("failed", failedNuclei.getNucleusCount());
				}
				
			} catch(Exception e){
				logger.log("Cannot create collection: "+e.getMessage(), Logger.ERROR);
			}

			//		  mw.log(spacerString);
//			mw.log("Population: "+r.getType());
//			mw.log("Population: "+r.getNucleusCount()+" nuclei");
			logger.log("Population: "+r.getType()+" : "+r.getNucleusCount()+" nuclei");
			//		  mw.log(spacerString);

			// core analysis - align profiles and segment
//			mw.logc("Running morphology analysis...");
			boolean ok = MorphologyAnalysis.run(r);
			if(ok){
//				mw.log("OK");
			} else {
//				mw.log("Error");
			}

			// measure general nuclear organisation
//			mw.logc("Running signal analysis...");
//			logger.log("Preparing for signal analysis");
//			ok = SignalAnalysis.run(r);
//			if(ok){
//				mw.log("OK");
//			} else {
//				mw.log("Error");
//			}

			// Perform shell analysis with 5 shells by default
//			if(r.getNucleusClass() != RoundNucleus.class){
//				logger.log("Not a round nucleus; skipping shell analysis");
//			} else {
//				mw.logc("Running shell analysis...");
//				ok = ShellAnalysis.run(dataset, 5);
//				if(ok){
//					mw.log("OK");
//				} else {
//					mw.log("Error");
//				}
//			}

			// export the stats files
//			mw.logc("Exporting stats...");
			ok = StatsExporter.run(r);
			if(ok){
//				mw.log("OK");
			} else {
//				mw.log("Error");
			}

			// annotate the nuclei in the population
//			mw.logc("Annotating nuclei...");
			ok = NucleusAnnotator.run(r);
			if(ok){
//				mw.log("OK");
			} else {
//				mw.log("Error");
			}


			// make a composite image of all nuclei in the collection
//			mw.logc("Exporting composite...");
			ok = CompositeExporter.run(r);
			if(ok){
//				mw.log("OK");
			} else {
//				mw.log("Error");
			}

			// refold the median consensus nucleus
			if(analysisOptions.refoldNucleus()){
//				mw.logc("Refolding profile...");
				
				CurveRefolder refolder = new CurveRefolder(r, 
						analysisOptions.getNucleusClass(), 
						analysisOptions.getRefoldMode());
				
				refolder.execute();
				try {
					if(refolder.get()){
//						mw.log("OK");
					} else {
//						mw.log("Error");
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}

			finalPopulations.add(r);

//			ArrayList<CellCollection> signalPopulations = dividePopulationBySignals(r);
//
//			for(CellCollection p : signalPopulations){
//
//				AnalysisDataset subDataset = new AnalysisDataset(p, dataset.getSavePath());
//				subDataset.setAnalysisOptions(analysisOptions);
//
//				nucleusCounts.put(p.getType(), p.getNucleusCount());
//
//				mw.log("Sub-population: "+p.getType());
//				mw.log("Sub-population: "+p.getNucleusCount()+" nuclei");
//				logger.log("Sub-population: "+p.getType()+" : "+p.getNucleusCount()+" nuclei");
//
//				//        MorphologyAnalysis.run(p);
//				// use the same segmentation from the initial analysis
//				mw.logc("Reapplying morphology...");
//				ok = MorphologyAnalysis.reapplyProfiles(p, r);
//				if(ok){
//					mw.log("OK");
//				} else {
//					mw.log("Error");
//				}
//
//				// measure general nuclear organisation
//				mw.logc("Running signal analysis...");
//				ok = SignalAnalysis.run(p);
//				if(ok){
//					mw.log("OK");
//				} else {
//					mw.log("Error");
//				}
//
//				// Perform shell analysis with 5 shells by default
//				if(p.getNucleusClass() != RoundNucleus.class){
//					logger.log("Not a round nucleus; skipping");
//				} else {
//					mw.logc("Running shell analysis...");
//					ok = ShellAnalysis.run(subDataset, 5);
//					if(ok){
//						mw.log("OK");
//					} else {
//						mw.log("Error");
//					}
//				}
//
//				// export the stats files
//				mw.logc("Exporting stats...");
//				ok = StatsExporter.run(p);
//				if(ok){
//					mw.log("OK");
//				} else {
//					mw.log("Error");
//				}
//
//				// annotate the nuclei in the population
//				mw.logc("Annotating nuclei...");
//				ok = NucleusAnnotator.run(p);
//				if(ok){
//					mw.log("OK");
//				} else {
//					mw.log("Error");
//				}
//
//
//				// make a composite image of all nuclei in the collection
//				mw.logc("Exporting composite...");
//				ok = CompositeExporter.run(p);
//				if(ok){
//					mw.log("OK");
//				} else {
//					mw.log("Error");
//				}
//
//				if(analysisOptions.refoldNucleus()){
//					
//					CurveRefolder refolder = new CurveRefolder(p, 
//							analysisOptions.getNucleusClass(), 
//							analysisOptions.getRefoldMode());
//					
//					refolder.execute();
//					try {
//						if(refolder.get()){
//							mw.log("OK");
//						} else {
//							mw.log("Error");
//						}
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} catch (ExecutionException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					
////					CurveRefolder.run(p, analysisOptions.getNucleusClass(), analysisOptions.getRefoldMode());
//				}
//
//				finalPopulations.add(p);
//				dataset.addChildDataset(subDataset);
//			}
			finalDatasets.add(dataset);
			collectionNucleusCounts.put(folder, nucleusCounts);

			// export the population to a save file for later
//			mw.logc("Saving to file...");
			ok = PopulationExporter.saveAnalysisDataset(dataset);
			if(ok){
//				mw.log("OK");
			} else {
//				mw.log("Error");
			}
			
			progress++;
			publish(progress);
		}
	}

//	/*
//    Given a complete collection of nuclei, split it into up to 4 populations;
//      nuclei with red signals, with green signals, without red signals and without green signals
//    Only include the 'without' populations if there is a 'with' population.
//	 */
//	public ArrayList<CellCollection> dividePopulationBySignals(CellCollection r){
//
//		ArrayList<CellCollection> signalPopulations = new ArrayList<CellCollection>(0);
//		logger.log("Dividing population by signals...");
//		try{
//
//			List<Integer> signalGroups = r.getSignalGroups();
//			for(int signalGroup : signalGroups){
//				List<Cell> list = r.getCellsWithNuclearSignals(signalGroup, true);
//				if(!list.isEmpty()){
//					logger.log("Found nuclei with signals in group "+signalGroup);
//					CellCollection listCollection = new CellCollection(r.getFolder(), 
//							r.getOutputFolderName(), 
//							"Signals_in_group_"+signalGroup, 
//							r.getDebugFile(), 
//							r.getNucleusClass());
//					
//					for(Cell c : list){
//						listCollection.addCell( c );
//					}
//					signalPopulations.add(listCollection);
//
//					List<Cell> notList = r.getCellsWithNuclearSignals(signalGroup, false);
//					if(!notList.isEmpty()){
//						logger.log("Found nuclei without signals in group "+signalGroup);
//						CellCollection notListCollection = new CellCollection(r.getFolder(), 
//								r.getOutputFolderName(), 
//								"No_signals_in_group_"+signalGroup, 
//								r.getDebugFile(), 
//								r.getNucleusClass());
//						
//						for(Cell c : notList){
//							notListCollection.addCell( c );
//						}
//						signalPopulations.add(notListCollection);
//					}
//
//				}
//			}
//
//		} catch(Exception e){
//			logger.log("Cannot create collection: "+e.getMessage(), Logger.ERROR);
//		}
//
//		return signalPopulations;
//	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub
		
		int value = (Integer) evt.getNewValue(); // should be percent
		publish(value);
				
	}

//	public void exportAnalysisLog(){
//
//		for(CellCollection r : this.nuclearPopulations){
//
//			StringBuilder outLine = new StringBuilder();
//			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
//			String timeStamp = formatter.format(Calendar.getInstance().getTime());
//			outLine.append("-------------------------\r\n");
//			outLine.append("Nuclear morphology analysis log\r\n");
//			outLine.append("-------------------------\r\n");
//			outLine.append("Analysis began    : "+formatter.format(this.startTime)+"\r\n");
//			outLine.append("Analysis complete : "+timeStamp+"\r\n");
//			if(this.analysisRun)
//				outLine.append("Analysis type     : Primary analysis\r\n");
//			if(this.reAnalysisRun){
//				outLine.append("Analysis type     : Nucleus refinding analysis\r\n");
//				outLine.append("Mapping file      : "+analysisOptions.getMappingFile().getAbsolutePath()+"\r\n");
//				outLine.append("Initial X offset  : "+analysisOptions.getXOffset()+"\r\n");
//				outLine.append("Initial Y offset  : "+analysisOptions.getYOffset()+"\r\n");
//				outLine.append("Aligning images   : "+analysisOptions.realignImages()+"\r\n");
//				outLine.append("Mapping count     : "+this.mappingCount+" nuclei\r\n");
//			}
//
//			outLine.append("-------------------------\r\n");
//			outLine.append("Parameters:\r\n");
//			outLine.append("-------------------------\r\n");
//			outLine.append("\tNucleus thresholding: "+analysisOptions.getNucleusThreshold()+"\r\n");
//			outLine.append("\tNucleus minimum size: "+analysisOptions.getMinNucleusSize()+"\r\n");
//			outLine.append("\tNucleus maximum size: "+analysisOptions.getMaxNucleusSize()+"\r\n");
//			outLine.append("\tNucleus minimum circ: "+analysisOptions.getMinNucleusCirc()+"\r\n");
//			outLine.append("\tNucleus maximum circ: "+analysisOptions.getMaxNucleusCirc()+"\r\n");
//			outLine.append("\tSignal thresholding : "+analysisOptions.getNuclearSignalOptions("default").getSignalThreshold()+"\r\n");
//			outLine.append("\tSignal minimum size : "+analysisOptions.getNuclearSignalOptions("default").getMinSize()+"\r\n");
//			outLine.append("\tSignal max. fraction: "+analysisOptions.getNuclearSignalOptions("default").getMaxFraction()+"\r\n");
//			outLine.append("\tAngle profile window: "+analysisOptions.getAngleProfileWindowSize()+"\r\n");
//			outLine.append("\tNucleus class       : "+analysisOptions.getNucleusClass().getSimpleName()+"\r\n");
////			outLine.append("\tCollection class    : "+analysisOptions.getCollectionClass().getSimpleName()+"\r\n");
//			outLine.append("\tRefolding mode      : "+analysisOptions.getRefoldMode()+"\r\n");
//			outLine.append("-------------------------\r\n");
//			outLine.append("Populations:\r\n");
//			outLine.append("-------------------------\r\n");
//
//			outLine.append("\t"+r.getFolder().getAbsolutePath()+"\r\n");
//
//			Map<String, Integer> nucleusCounts = collectionNucleusCounts.get(r.getFolder());
//			Set<String> keys = nucleusCounts.keySet();
//			for(String s : keys){
//				double percent = ( (double) nucleusCounts.get(s) / (double)r.getNucleusCount() )* 100;
//				if(s.equals("input")){
//					outLine.append("\t\t"+s+" : "+nucleusCounts.get(s)+" nuclei\r\n");
//				} else {
//					if(s.equals("failed")){
//						outLine.append("\t\t"+s+" : "+nucleusCounts.get(s)+" nuclei\r\n");
//						outLine.append("\t\t"+r.getType()+" : "+r.getNucleusCount()+" nuclei\r\n");
//					} else {
//						outLine.append("\t\t"+s+" : "+nucleusCounts.get(s)+" nuclei ("+(int)percent+"% of analysable)\r\n");
//					} //else
//				} //else
//			} //for(String s : keys)
//			String outPath = r.getFolder().getAbsolutePath()+File.separator+this.outputFolderName+File.separator+"log.analysis.txt";
//			IJ.append( outLine.toString(), outPath);
//		}
//	}
}

