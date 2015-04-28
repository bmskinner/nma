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
import no.export.CompositeExporter;
import no.export.NucleusAnnotator;
import no.export.PopulationExporter;
import no.export.StatsExporter;
import no.gui.AnalysisSetup;
import no.gui.MainWindow;
import no.gui.PopulationSplitWindow;
import no.nuclei.INuclearFunctions;
import no.utility.Logger;
import no.utility.MappingFileParser;


public class AnalysisCreator {
	
	private MainWindow mw; // use to log and update gui
	
	private static final String spacerString = "------------";
	
	private Logger logger;
//	debugFile;
	
	private AnalysisSetup analysisOptions;
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
	private Map<File, NucleusCollection> folderCollection;

	private List<INuclearCollection> nuclearPopulations = new ArrayList<INuclearCollection>(0);
	
	private List<INuclearCollection> finalPopulations = new ArrayList<INuclearCollection>(0);

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

	  analysisOptions = new AnalysisSetup();
	  if(analysisOptions.run()){
		  
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
    
    boolean ok = true;
    // do as many post mappings as needed
    while(ok){
    	ok = this.postAnalysis();
    }
    
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
  public List<INuclearCollection> getPopulations(){
	  return this.finalPopulations;
  }

  /**
   * Following the main analysis, allow a mapping file to be applied
   * to investigate a seubset of nuclei.
   * @return true if another analysis is to be performed
   */
  private boolean postAnalysis(){
	  PopulationSplitWindow splitter = new PopulationSplitWindow(this.nuclearPopulations, this.mw);
	  if(splitter.getResult()){

		  try{
			  File f = splitter.addMappingFile();
			  
			  if(f==null) return false;
			  
			  if(!f.exists()) return false;
			  
			  INuclearCollection subjectCollection = splitter.getCollection();
			  if(subjectCollection==null) return false;

			  // import and parse the mapping file
			  List<String> pathList = MappingFileParser.parse(f);

			  // create a new collection to hold the nuclei
			  Constructor<?> collectionConstructor = analysisOptions.getCollectionClass().getConstructor(new Class<?>[]{File.class, String.class, String.class});
			  INuclearCollection remapCollection = (INuclearCollection) collectionConstructor.newInstance(subjectCollection.getFolder(), subjectCollection.getOutputFolderName(), f.getName());

			  // add nuclei to the new population based on the mapping info
			  for(INuclearFunctions n : subjectCollection.getNuclei()){
				  if(pathList.contains(n.getPath()+"\t"+n.getNucleusNumber())){
//					  IJ.log("    Adding nucleus: "+n.getImageName()+"-"+n.getNucleusNumber());
					  remapCollection.addNucleus(n);
				  }
			  }


			  // reanalyse / generate medians and consensus
			  // create median of the same length as the main population median
			  // ensure that no remapping of points or segmentation occurs.
			  // this allows the segments from the main population to be drawn directly
			  MorphologyAnalysis.reapplyProfiles(remapCollection, subjectCollection);
			  // draw the main population segment pattern on the new median profile
			  // make a consensus nucleus from the new median
			  CurveRefolder.run(remapCollection, analysisOptions.getNucleusClass(), analysisOptions.getRefoldMode());

		  } catch(InstantiationException e){
			  logger.log("Cannot create collection: "+e.getMessage(), Logger.ERROR);
//			  IJ.log("Cannot create collection: "+e.getMessage());
		  } catch(IllegalAccessException e){
			  logger.log("Cannot access constructor: "+e.getMessage(), Logger.ERROR);
//			  IJ.log("Cannot access constructor: "+e.getMessage());
		  } catch(InvocationTargetException e){
			  logger.log("Cannot invoke constructor: "+e.getMessage(), Logger.ERROR);
//			  IJ.log("Cannot invoke constructor: "+e.getMessage());
		  } catch (NoSuchMethodException e) {
			  logger.log("No such method: "+e.getMessage(), Logger.ERROR);
			  for(StackTraceElement el : e.getStackTrace()){
				  logger.log(el.toString(), Logger.ERROR);
			  }
		  } catch (SecurityException e) {
			  logger.log("Security error: "+e.getMessage(), Logger.ERROR);
			  for(StackTraceElement el : e.getStackTrace()){
				  logger.log(el.toString(), Logger.ERROR);
			  }
		  }
		  return true;
	  }	else {
		  return false;
	  }
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
   * @see         NucleusCollection
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
		  Constructor<?> nucleusConstructor = analysisOptions.getNucleusClass().getConstructor(new Class<?>[]{Nucleus.class});
		  logger.log("Prepared constructors",Logger.DEBUG);
		  
		  for (File key : keys) {
			  NucleusCollection collection = folderCollection.get(key);

			  try{
				  INuclearCollection spermNuclei = (INuclearCollection) collectionConstructor.newInstance(key, collection.getOutputFolderName(), "analysable", logger.getLogfile());
				  logger.log("Created collection instance",Logger.DEBUG);

				  mw.log(key.getAbsolutePath()+"   Nuclei: "+collection.getNucleusCount());

				  logger.log("Detecting nucleus border points");
				  for(int i=0;i<collection.getNucleusCount();i++){
					  INuclearFunctions p = collection.getNucleus(i);

					  INuclearFunctions subNucleus  = (INuclearFunctions) nucleusConstructor.newInstance(p);
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

	  for(INuclearCollection r : this.nuclearPopulations){

		  File folder = r.getFolder();
		  //		  mw.log(spacerString);
		  mw.log("Analysing: "+folder.getName());
		  logger.log("Analysing: "+folder.getName());
		  //		  mw.log(spacerString);

		  LinkedHashMap<String, Integer> nucleusCounts = new LinkedHashMap<String, Integer>();

		  try{

			  nucleusCounts.put("input", r.getNucleusCount());
			  Constructor<?> collectionConstructor = analysisOptions.getCollectionClass().getConstructor(new Class[]{File.class, String.class, String.class, File.class});
			  INuclearCollection failedNuclei = (INuclearCollection) collectionConstructor.newInstance(folder, r.getOutputFolderName(), "failed", logger.getLogfile());

			  r.refilterNuclei(failedNuclei); // put fails into failedNuclei, remove from r
			  if(failedNuclei.getNucleusCount()>0){
				  mw.log("Exporting failed nuclei");
				  logger.log("Exporting failed nuclei");

				  CompositeExporter.run(failedNuclei);
				  nucleusCounts.put("failed", failedNuclei.getNucleusCount());
				  logger.log("Failed nuclei exported");
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
		  mw.log("Population: "+r.getType()+":\n\t"+r.getNucleusCount()+" nuclei");
		  logger.log("Population: "+r.getType()+":\n\t"+r.getNucleusCount()+" nuclei");
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
		  SignalAnalysis.run(r);

		  // Perform shell analysis with 5 shells by default
		  ShellAnalysis.run(r, 5);

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
			  CurveRefolder.run(r, analysisOptions.getNucleusClass(), analysisOptions.getRefoldMode());
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
		  ArrayList<INuclearCollection> signalPopulations = dividePopulationBySignals(r);
		  
		  for(INuclearCollection p : signalPopulations){

			  nucleusCounts.put(p.getType(), p.getNucleusCount());

			  mw.log(spacerString);
			  mw.log("Sub-population: "+p.getType()+":\n\t"+p.getNucleusCount()+" nuclei");
			  mw.log(spacerString);

			  //        IJ.log("    ----------------------------- ");
			  //        IJ.log("    Analysing sub-population: "+p.getType()+" : "+p.getNucleusCount()+" nuclei");
			  //        IJ.log("    ----------------------------- ");

			  //        MorphologyAnalysis.run(p);
			  // use the same segmentation from the initial analysis
			  MorphologyAnalysis.reapplyProfiles(p, r);

			  SignalAnalysis.run(p);
			  ShellAnalysis.run(p, 5);
			  StatsExporter.run(p);

			  NucleusAnnotator.run(p);
			  CompositeExporter.run(p);

			  if(analysisOptions.refoldNucleus()){
				  CurveRefolder.run(p, analysisOptions.getNucleusClass(), analysisOptions.getRefoldMode());
			  }

			  // export the population to a save file for later
			  PopulationExporter.savePopulation(p);
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
  public ArrayList<INuclearCollection> dividePopulationBySignals(INuclearCollection r){

	  ArrayList<INuclearCollection> signalPopulations = new ArrayList<INuclearCollection>(0);

	  try{

		  Constructor<?> collectionConstructor = analysisOptions.getCollectionClass().getConstructor(new Class<?>[]{File.class, String.class, String.class, File.class});

		  List<Integer> channels = r.getSignalChannels();
		  for(int channel : channels){
			  List<INuclearFunctions> list = r.getNucleiWithSignals(channel);
			  if(!list.isEmpty()){
				  INuclearCollection listCollection = (INuclearCollection) collectionConstructor.newInstance(r.getFolder(), r.getOutputFolderName(), "Signals_in_channel_"+channel);
				  for(INuclearFunctions n : list){
					  listCollection.addNucleus( n );
				  }
				  signalPopulations.add(listCollection);

				  List<INuclearFunctions> notList = r.getNucleiWithSignals(-channel);
				  if(!notList.isEmpty()){
					  INuclearCollection notListCollection = (INuclearCollection) collectionConstructor.newInstance(r.getFolder(), r.getOutputFolderName(), "No_signals_in_channel_"+channel);
					  for(INuclearFunctions n : notList){
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

    for(INuclearCollection r : this.nuclearPopulations){
      
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

