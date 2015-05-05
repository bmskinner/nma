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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.*;

import no.nuclei.*;
import no.collections.*;
import no.components.AnalysisOptions;
import no.export.CompositeExporter;
import no.export.NucleusAnnotator;
import no.export.PopulationExporter;
import no.export.StatsExporter;
import no.gui.AnalysisSetup;
import no.gui.MainWindow;
import no.nuclei.Nucleus;
import no.utility.Logger;


public class AnalysisCreator {
	
	private MainWindow mw; // use to log and update gui
	
	private static final String spacerString = "------------";
	
	private Logger logger;
//	debugFile;
	
	private AnalysisSetup analysisSetup; // make the gui
	private AnalysisOptions analysisOptions; // store the options
	private int mappingCount = 0;

	private Date startTime; // the time the analysis began

	private String outputFolderName;

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
	private Map<File, RoundNucleusCollection> folderCollection;

	private List<NucleusCollection> nuclearPopulations = new ArrayList<NucleusCollection>(0);
	
	private List<NucleusCollection> finalPopulations = new ArrayList<NucleusCollection>(0);

	/*
    -----------------------
    Constructors
    -----------------------
  */
	public AnalysisCreator(MainWindow mw){
		this.mw = mw;
		this.initialise();
	}
	  
  public void initialise(){

	  analysisSetup = new AnalysisSetup();
	  if(analysisSetup.run()){
		  
		  this.analysisOptions = analysisSetup.getOptions();
		  
		  mw.log("Directory: "+analysisOptions.getFolder().getName());

		  this.startTime = Calendar.getInstance().getTime();
		  this.outputFolderName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(this.startTime);
		  
		  // craete the analysis folder early. Did not before in case folder had no images
		  File analysisFolder = new File(analysisOptions.getFolder().getAbsolutePath()+File.separator+outputFolderName);
		  if(!analysisFolder.exists()){
			  analysisFolder.mkdir();
		  }
		  this.logger = new Logger( new File(analysisOptions.getFolder().getAbsolutePath()+File.separator+outputFolderName+File.separator+"log.debug.txt"), "AnalysisCreator");
//		  mw.log("Debug at: "+logger.getLogfile().getAbsolutePath());
		  logger.log("Analysis began: "+analysisFolder.getAbsolutePath());
		  logger.log("Directory: "+analysisOptions.getFolder().getName());
		  this.readyToRun = true;
	  }
  }

  public void run(){
    
    if(!readyToRun) return;

     if(!analysisOptions.isReanalysis()){
      this.runAnalysis();
    } else {
      this.runReAnalysis();
    }
    this.assignNucleusTypes();
    this.analysePopulations();
    this.exportAnalysisLog();
    
//    boolean ok = true;
//    // do as many post mappings as needed
//    while(ok){
//    	ok = this.postAnalysis();
//    }
    
    mw.log(spacerString);
    mw.log("All done!" );
    mw.log(spacerString);
    logger.log("All done!");
  }

  /**
   * Get the populations from this analysis. Allows re/sub analysis to take place
   * outside the AnalysisCreator
   * @return the collections of nuclei
   */
  public List<NucleusCollection> getPopulations(){
	  return this.finalPopulations;
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

  public void runAnalysis(){
    NucleusDetector detector = new NucleusDetector(analysisOptions.getFolder(), this.outputFolderName, this.mw, logger.getLogfile());

    setDetectionParameters(detector);
    detector.runDetector();

    this.folderCollection = detector.getNucleiCollections();
    logger.log("Imported folder(s)");
    mw.log("Imported folder(s)");
    this.analysisRun = true;
  }

  /**
   * Run an analysis of selected nuclei based on a mapping file
   *
   * @return      the nuclei in each folder analysed
   */
  public void runReAnalysis(){
	  NucleusRefinder detector = new NucleusRefinder(analysisOptions.getFolder(), this.outputFolderName, analysisOptions.getMappingFile(), this.mw, logger.getLogfile());
	  setDetectionParameters(detector);
	  detector.setXOffset(analysisOptions.getXOffset());
	  detector.setYOffset(analysisOptions.getYOffset());
	  detector.setRealignMode(analysisOptions.realignImages());
	  detector.runDetector();
	  this.folderCollection = detector.getNucleiCollections();
	  this.mappingCount = detector.getMappingCount();

	  logger.log("Imported folder(s)");
	  mw.log("Imported folder(s)");
	  this.reAnalysisRun = true;
  }

  private void  setDetectionParameters(NucleusDetector detector){
	  logger.log("Setting detection parameters...", Logger.DEBUG);
	  
	  logger.log("Min size: "+analysisOptions.getMinNucleusSize(), Logger.DEBUG);
	  logger.log("Max size: "+analysisOptions.getMaxNucleusSize(), Logger.DEBUG);
	  logger.log("Min circ: "+analysisOptions.getMinNucleusCirc(), Logger.DEBUG);
	  logger.log("Max circ: "+analysisOptions.getMaxNucleusCirc(), Logger.DEBUG);
	  logger.log("Nucleus threshold: "+analysisOptions.getNucleusThreshold(), Logger.DEBUG);

	  logger.log("Profile window: "+analysisOptions.getAngleProfileWindowSize(), Logger.DEBUG);
	  
	  logger.log("Signal threshold: "+analysisOptions.getSignalThreshold(), Logger.DEBUG);
	  logger.log("Signal min: "+analysisOptions.getMinSignalSize(), Logger.DEBUG);
	  logger.log("Signal max: "+analysisOptions.getMaxSignalFraction(), Logger.DEBUG);
	  
	  detector.setMinNucleusSize(analysisOptions.getMinNucleusSize()); 
	  detector.setMaxNucleusSize(analysisOptions.getMaxNucleusSize());
	  detector.setThreshold(analysisOptions.getNucleusThreshold());
	  detector.setMinNucleusCirc(analysisOptions.getMinNucleusCirc());
	  detector.setMaxNucleusCirc(analysisOptions.getMaxNucleusCirc());

	  detector.setAngleProfileWindowSize(analysisOptions.getAngleProfileWindowSize());

	  detector.setSignalThreshold(analysisOptions.getSignalThreshold());
	  detector.setMinSignalSize(analysisOptions.getMinSignalSize());
	  detector.setMaxSignalFraction(analysisOptions.getMaxSignalFraction());
	  logger.log("Detection parameters set", Logger.DEBUG);
  }

  /*
    Use reflection to assign the correct class to the nuclei and populations
  */
  public void assignNucleusTypes(){

	  Set<File> keys = this.folderCollection.keySet();
	  logger.log("Assigning nucleus types");
	  

	  try{
		  Constructor<?> collectionConstructor = analysisOptions.getCollectionClass().getConstructor(new Class<?>[]{File.class, String.class, String.class, File.class});
		  Constructor<?> nucleusConstructor = analysisOptions.getNucleusClass().getConstructor(new Class<?>[]{RoundNucleus.class});
		  logger.log("Prepared constructors",Logger.DEBUG);
		  
		  for (File key : keys) {
			  RoundNucleusCollection collection = folderCollection.get(key);

			  try{
				  NucleusCollection spermNuclei = (NucleusCollection) collectionConstructor.newInstance(key, collection.getOutputFolderName(), "analysable", logger.getLogfile());
				  logger.log("Created collection instance",Logger.DEBUG);

				  mw.log(key.getAbsolutePath()+"   Nuclei: "+collection.getNucleusCount());

				  logger.log("Detecting nucleus border points");
				  for(int i=0;i<collection.getNucleusCount();i++){
					  Nucleus p = collection.getNucleus(i);

					  Nucleus subNucleus  = (Nucleus) nucleusConstructor.newInstance(p);
					  subNucleus.findPointsAroundBorder();

					  spermNuclei.addNucleus(subNucleus);
				  }
				  this.nuclearPopulations.add(spermNuclei);
				  logger.log("Nucleus border points found");
				  logger.log("Population converted to "+analysisOptions.getNucleusClass().getSimpleName()+" in "+spermNuclei.getClass().getSimpleName(),Logger.DEBUG);
				  //          mw.log("  Population converted to "+analysisOptions.getNucleusClass().getSimpleName()+" in "+spermNuclei.getClass().getSimpleName());
				  //          IJ.log("  Population converted to "+analysisOptions.getNucleusClass().getSimpleName()+" in "+spermNuclei.getClass().getSimpleName());
			  } catch(InstantiationException e){
				  logger.log("Cannot create collection: "+e.getMessage(), Logger.ERROR);
			  } catch(IllegalAccessException e){
				  logger.log("Cannot access constructor: "+e.getMessage(), Logger.ERROR);
			  } catch(InvocationTargetException e){
				  logger.log("Cannot invoke constructor: "+e.getMessage(), Logger.ERROR);
			  }
		  }
	  } catch(NoSuchMethodException e){
		  IJ.log("Cannot find constructor: "+e.getMessage());
	  }

  }

  public void analysePopulations(){
	  mw.log("Beginning analysis");
	  logger.log("Beginning population analysis");

	  for(NucleusCollection r : this.nuclearPopulations){
		  
		  r.setAnalysisOptions(analysisOptions);

		  File folder = r.getFolder();
		  //		  mw.log(spacerString);
		  mw.log("Analysing: "+folder.getName());
		  logger.log("Analysing: "+folder.getName());
		  //		  mw.log(spacerString);

		  LinkedHashMap<String, Integer> nucleusCounts = new LinkedHashMap<String, Integer>();

		  try{

			  nucleusCounts.put("input", r.getNucleusCount());
			  Constructor<?> collectionConstructor = analysisOptions.getCollectionClass().getConstructor(new Class[]{File.class, String.class, String.class, File.class});
			  NucleusCollection failedNuclei = (NucleusCollection) collectionConstructor.newInstance(folder, r.getOutputFolderName(), "failed", logger.getLogfile());

			  mw.logc("Filtering collection...");
			  boolean ok = CollectionFilterer.run(r, failedNuclei); // put fails into failedNuclei, remove from r
			  if(ok){
				  mw.log("OK");
			  } else {
				  mw.log("Error");
			  }
			  
//			  r.refilterNuclei(failedNuclei); 
			  if(failedNuclei.getNucleusCount()>0){
				  mw.logc("Exporting failed nuclei...");
				  ok = CompositeExporter.run(failedNuclei);
				  if(ok){
					  mw.log("OK");
				  } else {
					  mw.log("Error");
				  }
				  nucleusCounts.put("failed", failedNuclei.getNucleusCount());
			  }

		  } catch(InstantiationException e){
			  logger.log("Cannot create collection: "+e.getMessage(), Logger.ERROR);
		  } catch(IllegalAccessException e){
			  logger.log("Cannot access constructor: "+e.getMessage(), Logger.ERROR);
		  } catch(InvocationTargetException e){
			  logger.log("Cannot invoke constructor: "+e.getMessage(), Logger.ERROR);
		  } catch(NoSuchMethodException e){
			  logger.log("Cannot find constructor: "+e.getMessage(), Logger.ERROR);
		  }

		  //		  mw.log(spacerString);
		  mw.log("Population: "+r.getType());
		  mw.log("Population: "+r.getNucleusCount()+" nuclei");
		  logger.log("Population: "+r.getType()+" : "+r.getNucleusCount()+" nuclei");
		  //		  mw.log(spacerString);

		  // core analysis - align profiles and segment
		  mw.logc("Running morphology analysis...");
		  boolean ok = MorphologyAnalysis.run(r);
		  if(ok){
			  mw.log("OK");
		  } else {
			  mw.log("Error");
		  }

		  // measure general nuclear organisation
		  mw.logc("Running signal analysis...");
		  ok = SignalAnalysis.run(r);
		  if(ok){
			  mw.log("OK");
		  } else {
			  mw.log("Error");
		  }

		  // Perform shell analysis with 5 shells by default
		  if(r.getClass() != RoundNucleusCollection.class){
			  logger.log("Not a round nucleus; skipping");
		  } else {
			  mw.logc("Running shell analysis...");
			  ok = ShellAnalysis.run(r, 5);
			  if(ok){
				  mw.log("OK");
			  } else {
				  mw.log("Error");
			  }
		  }

		  // export the stats files
		  mw.logc("Exporting stats...");
		  ok = StatsExporter.run(r);
		  if(ok){
			  mw.log("OK");
		  } else {
			  mw.log("Error");
		  }

		  // annotate the nuclei in the population
		  mw.logc("Annotating nuclei...");
		  ok = NucleusAnnotator.run(r);
		  if(ok){
			  mw.log("OK");
		  } else {
			  mw.log("Error");
		  }


		  // make a composite image of all nuclei in the collection
		  mw.logc("Exporting composite...");
		  ok = CompositeExporter.run(r);
		  if(ok){
			  mw.log("OK");
		  } else {
			  mw.log("Error");
		  }

		  // refold the median consensus nucleus
		  if(analysisOptions.refoldNucleus()){
			  mw.logc("Refolding profile...");
			  ok = CurveRefolder.run(r, analysisOptions.getNucleusClass(), analysisOptions.getRefoldMode());
			  if(ok){
				  mw.log("OK");
			  } else {
				  mw.log("Error");
			  }
		  }

		  // export the population to a save file for later
		  mw.logc("Saving to file...");
		  ok = PopulationExporter.savePopulation(r);
		  if(ok){
			  mw.log("OK");
		  } else {
			  mw.log("Error");
		  }

		  finalPopulations.add(r);
		  ArrayList<NucleusCollection> signalPopulations = dividePopulationBySignals(r);
		  
		  for(NucleusCollection p : signalPopulations){
			  
			  p.setAnalysisOptions(analysisOptions);

			  nucleusCounts.put(p.getType(), p.getNucleusCount());

			  mw.log("Sub-population: "+p.getType());
			  mw.log("Sub-population: "+p.getNucleusCount()+" nuclei");
			  logger.log("Sub-population: "+p.getType()+" : "+p.getNucleusCount()+" nuclei");

			  //        MorphologyAnalysis.run(p);
			  // use the same segmentation from the initial analysis
			  mw.logc("Reapplying morphology...");
			  ok = MorphologyAnalysis.reapplyProfiles(p, r);
			  if(ok){
				  mw.log("OK");
			  } else {
				  mw.log("Error");
			  }
			  
			  // measure general nuclear organisation
			  mw.logc("Running signal analysis...");
			  ok = SignalAnalysis.run(p);
			  if(ok){
				  mw.log("OK");
			  } else {
				  mw.log("Error");
			  }

			  // Perform shell analysis with 5 shells by default
			  if(p.getClass() != RoundNucleusCollection.class){
				  logger.log("Not a round nucleus; skipping");
			  } else {
				  mw.logc("Running shell analysis...");
				  ok = ShellAnalysis.run(p, 5);
				  if(ok){
					  mw.log("OK");
				  } else {
					  mw.log("Error");
				  }
			  }

			  // export the stats files
			  mw.logc("Exporting stats...");
			  ok = StatsExporter.run(p);
			  if(ok){
				  mw.log("OK");
			  } else {
				  mw.log("Error");
			  }

			  // annotate the nuclei in the population
			  mw.logc("Annotating nuclei...");
			  ok = NucleusAnnotator.run(p);
			  if(ok){
				  mw.log("OK");
			  } else {
				  mw.log("Error");
			  }


			  // make a composite image of all nuclei in the collection
			  mw.logc("Exporting composite...");
			  ok = CompositeExporter.run(p);
			  if(ok){
				  mw.log("OK");
			  } else {
				  mw.log("Error");
			  }
			  
//
//			  SignalAnalysis.run(p);
//			  ShellAnalysis.run(p, 5);
//			  StatsExporter.run(p);
//
//			  NucleusAnnotator.run(p);
//			  CompositeExporter.run(p);

			  if(analysisOptions.refoldNucleus()){
				  CurveRefolder.run(p, analysisOptions.getNucleusClass(), analysisOptions.getRefoldMode());
			  }

			  // export the population to a save file for later
			// export the population to a save file for later
			  mw.logc("Saving to file...");
			  ok = PopulationExporter.savePopulation(p);
			  if(ok){
				  mw.log("OK");
			  } else {
				  mw.log("Error");
			  }
//			  PopulationExporter.savePopulation(p);
			  finalPopulations.add(p);
		  }
		  collectionNucleusCounts.put(folder, nucleusCounts);
	  }
  }

  /*
    Given a complete collection of nuclei, split it into up to 4 populations;
      nuclei with red signals, with green signals, without red signals and without green signals
    Only include the 'without' populations if there is a 'with' population.
  */
  public ArrayList<NucleusCollection> dividePopulationBySignals(NucleusCollection r){

	  ArrayList<NucleusCollection> signalPopulations = new ArrayList<NucleusCollection>(0);
	  logger.log("Dividing population by signals...");
	  try{

		  Constructor<?> collectionConstructor = analysisOptions.getCollectionClass().getConstructor(new Class<?>[]{File.class, String.class, String.class, File.class});

		  List<Integer> channels = r.getSignalChannels();
		  for(int channel : channels){
			  List<Nucleus> list = r.getNucleiWithSignals(channel);
			  if(!list.isEmpty()){
				  NucleusCollection listCollection = (NucleusCollection) collectionConstructor.newInstance(r.getFolder(), r.getOutputFolderName(), "Signals_in_channel_"+channel, r.getDebugFile());
				  for(Nucleus n : list){
					  listCollection.addNucleus( n );
				  }
				  signalPopulations.add(listCollection);

				  List<Nucleus> notList = r.getNucleiWithSignals(-channel);
				  if(!notList.isEmpty()){
					  NucleusCollection notListCollection = (NucleusCollection) collectionConstructor.newInstance(r.getFolder(), r.getOutputFolderName(), "No_signals_in_channel_"+channel, r.getDebugFile());
					  for(Nucleus n : notList){
						  notListCollection.addNucleus( n );
					  }
					  signalPopulations.add(notListCollection);
				  }

			  }
		  }

	  } catch(InstantiationException e){
		  logger.log("Cannot create collection: "+e.getMessage(), Logger.ERROR);
	  } catch(IllegalAccessException e){
		  logger.log("Cannot access constructor: "+e.getMessage(), Logger.ERROR);
	  } catch(InvocationTargetException e){
		  logger.log("Cannot invoke constructor: "+e.getMessage(), Logger.ERROR);
	  } catch(NoSuchMethodException e){
		  logger.log("Cannot find constructor: "+e.getMessage(), Logger.ERROR);
	  }

    return signalPopulations;
  }

  public void exportAnalysisLog(){

    for(NucleusCollection r : this.nuclearPopulations){
      
      StringBuilder outLine = new StringBuilder();
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
      String timeStamp = formatter.format(Calendar.getInstance().getTime());
      outLine.append("-------------------------\r\n");
      outLine.append("Nuclear morphology analysis log\r\n");
      outLine.append("-------------------------\r\n");
      outLine.append("Analysis began    : "+formatter.format(this.startTime)+"\r\n");
      outLine.append("Analysis complete : "+timeStamp+"\r\n");
      if(this.analysisRun)
        outLine.append("Analysis type     : Primary analysis\r\n");
      if(this.reAnalysisRun){
        outLine.append("Analysis type     : Nucleus refinding analysis\r\n");
        outLine.append("Mapping file      : "+analysisOptions.getMappingFile().getAbsolutePath()+"\r\n");
        outLine.append("Initial X offset  : "+analysisOptions.getXOffset()+"\r\n");
        outLine.append("Initial Y offset  : "+analysisOptions.getYOffset()+"\r\n");
        outLine.append("Aligning images   : "+analysisOptions.realignImages()+"\r\n");
        outLine.append("Mapping count     : "+this.mappingCount+" nuclei\r\n");
      }
      
      outLine.append("-------------------------\r\n");
      outLine.append("Parameters:\r\n");
      outLine.append("-------------------------\r\n");
      outLine.append("\tNucleus thresholding: "+analysisOptions.getNucleusThreshold()+"\r\n");
      outLine.append("\tNucleus minimum size: "+analysisOptions.getMinNucleusSize()+"\r\n");
      outLine.append("\tNucleus maximum size: "+analysisOptions.getMaxNucleusSize()+"\r\n");
      outLine.append("\tNucleus minimum circ: "+analysisOptions.getMinNucleusCirc()+"\r\n");
      outLine.append("\tNucleus maximum circ: "+analysisOptions.getMaxNucleusCirc()+"\r\n");
      outLine.append("\tSignal thresholding : "+analysisOptions.getSignalThreshold()+"\r\n");
      outLine.append("\tSignal minimum size : "+analysisOptions.getMinSignalSize()+"\r\n");
      outLine.append("\tSignal max. fraction: "+analysisOptions.getMaxSignalFraction()+"\r\n");
      outLine.append("\tAngle profile window: "+analysisOptions.getAngleProfileWindowSize()+"\r\n");
      outLine.append("\tNucleus class       : "+analysisOptions.getNucleusClass().getSimpleName()+"\r\n");
      outLine.append("\tCollection class    : "+analysisOptions.getCollectionClass().getSimpleName()+"\r\n");
      outLine.append("\tRefolding mode      : "+analysisOptions.getRefoldMode()+"\r\n");
      outLine.append("-------------------------\r\n");
      outLine.append("Populations:\r\n");
      outLine.append("-------------------------\r\n");

      outLine.append("\t"+r.getFolder().getAbsolutePath()+"\r\n");

      Map<String, Integer> nucleusCounts = collectionNucleusCounts.get(r.getFolder());
      Set<String> keys = nucleusCounts.keySet();
      for(String s : keys){
        double percent = ( (double) nucleusCounts.get(s) / (double)r.getNucleusCount() )* 100;
        if(s.equals("input")){
          outLine.append("\t\t"+s+" : "+nucleusCounts.get(s)+" nuclei\r\n");
        } else {
            if(s.equals("failed")){
              outLine.append("\t\t"+s+" : "+nucleusCounts.get(s)+" nuclei\r\n");
              outLine.append("\t\t"+r.getType()+" : "+r.getNucleusCount()+" nuclei\r\n");
            } else {
              outLine.append("\t\t"+s+" : "+nucleusCounts.get(s)+" nuclei ("+(int)percent+"% of analysable)\r\n");
            } //else
        } //else
      } //for(String s : keys)
      String outPath = r.getFolder().getAbsolutePath()+File.separator+this.outputFolderName+File.separator+"log.analysis.txt";
      IJ.append( outLine.toString(), outPath);
    }
  }
}

