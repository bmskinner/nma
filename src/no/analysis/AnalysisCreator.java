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
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.*;
import no.components.Profile;
import no.nuclei.*;
import no.nuclei.sperm.*;
import no.collections.*;
import no.nuclei.INuclearFunctions;


public class AnalysisCreator {

  /**
  * These values are keys to the different types of nucleus we 
  * can analyse. They act like a primary key in a database
  */
  private static final int RODENT_SPERM_NUCLEUS = 0;
  private static final int PIG_SPERM_NUCLEUS = 1;
  private static final int ROUND_NUCLEUS = 2;

	 // /* VALUES FOR DECIDING IF AN OBJECT IS A NUCLEUS */
  private  int    nucleusThreshold = 36;
  private  int    signalThreshold  = 70;
  private  double minNucleusSize   = 500;
  private  double maxNucleusSize   = 10000;
  private  double minNucleusCirc   = 0.0;
  private  double maxNucleusCirc   = 1.0;

  private int angleProfileWindowSize = 15;

  private String refoldMode = "Fast";

  private int xoffset = 0;
  private int yoffset = 0;
  private int mappingCount = 0;

  private Date startTime; // the time the analysis began

  private  double minSignalSize = 5;
  private  double maxSignalFraction = 0.5;

  private File folder;
//  private File outputFolder;
  private String outputFolderName;
//  private File logAnalysis;
  private File nucleiToFind;

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

  /**
   * The class of Nucleus to use in the analysis
   */
  private Class<?> nucleusClass;

  /**
   * The class of NucleusCollection to use in the analysis
   */
  private Class<?> collectionClass;

  /**
   * Should a reanalysis be performed?
   */
  private boolean performReanalysis = false;

  /**
   * Should images for a reanalysis be aligned
   * beyond the offsets provided?
   */
  private boolean realignMode = true;

  private Map<File, LinkedHashMap<String, Integer>> collectionNucleusCounts = new HashMap<File, LinkedHashMap<String, Integer>>();

  // allow us to map an id to a class to construct
  private static Map<Integer, Class<?>>  collectionClassTypes;
  private static Map<Integer, Class<?>>  nucleusClassTypes;
  private static Map<String, Integer> nucleusTypes;

  // the raw input from nucleus detector
  private Map<File, NucleusCollection> folderCollection;

  private List<INuclearCollection> nuclearPopulations = new ArrayList<INuclearCollection>(0);
//  private List<INuclearCollection> failedPopulations  = new ArrayList<INuclearCollection>(0);
  

  /*
    -----------------------
    Populate the class map with available options
    -----------------------
  */
  static
  {
      nucleusTypes = new HashMap<String, Integer>();
      nucleusTypes.put("Rodent sperm" , RODENT_SPERM_NUCLEUS);
      nucleusTypes.put("Pig sperm"    , PIG_SPERM_NUCLEUS);
      nucleusTypes.put("Round nucleus", ROUND_NUCLEUS);

      collectionClassTypes = new HashMap<Integer, Class<?>>();
      collectionClassTypes.put(RODENT_SPERM_NUCLEUS, new RodentSpermNucleusCollection().getClass());
      collectionClassTypes.put(PIG_SPERM_NUCLEUS, new PigSpermNucleusCollection().getClass());
      collectionClassTypes.put(ROUND_NUCLEUS, new NucleusCollection().getClass());

      nucleusClassTypes = new HashMap<Integer, Class<?>>();
      nucleusClassTypes.put(RODENT_SPERM_NUCLEUS, new RodentSpermNucleus().getClass());
      nucleusClassTypes.put(PIG_SPERM_NUCLEUS, new PigSpermNucleus().getClass());
      nucleusClassTypes.put(ROUND_NUCLEUS, new Nucleus().getClass());
  }

  /*
    -----------------------
    Constructors
    -----------------------
  */
  public AnalysisCreator(){
    this.initialise();
  }

  public void initialise(){

    boolean ok = this.displayOptionsDialog();
    if(!ok) return; // should be true if we got all needed data

    DirectoryChooser localOpenDialog = new DirectoryChooser("Select directory of images...");
    String folderName = localOpenDialog.getDirectory();

    if(folderName==null) return; // user cancelled
    this.folder = new File(folderName);

    if(performReanalysis){
      OpenDialog fileDialog = new OpenDialog("Select a mapping file...");
      String fileName = fileDialog.getPath();
      if(fileName==null) return;
      nucleiToFind = new File(fileName);
    }

    IJ.log("Directory: "+folderName);

    this.startTime = Calendar.getInstance().getTime();
    this.outputFolderName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(this.startTime);

//    this.outputFolder = new File(this.folder.getAbsolutePath()+File.separator+outputFolderName);
    this.readyToRun = true;
  }

  public void run(){
    
    if(!readyToRun) return;

     if(!performReanalysis){
      this.runAnalysis();
    } else {
      this.runReAnalysis();
    }
    this.assignNucleusTypes();
    this.analysePopulations();
    this.exportAnalysisLog();
    IJ.log("----------------------------- ");
    IJ.log("All done!"                     );
    IJ.log("----------------------------- ");
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
    NucleusDetector detector = new NucleusDetector(this.folder, this.outputFolderName);

    setDetectionParameters(detector);
    detector.runDetector();

    this.folderCollection = detector.getNucleiCollections();
    IJ.log("Imported folder(s)");
    this.analysisRun = true;
  }

  /**
   * Returns a HashMap<File, NucleusCollection> object. This 
   * contains the nuclei found, keyed to the folder in which
   * they were found. It filters the nuclei based on whether they
   * are present in a list of previously captured images.
   *
   * @return      the nuclei in each folder analysed
   * @see         NucleusCollection
   */
  public void runReAnalysis(){
    NucleusRefinder detector = new NucleusRefinder(this.folder, this.outputFolderName, nucleiToFind);
    setDetectionParameters(detector);
    detector.setXOffset(this.xoffset);
    detector.setYOffset(this.yoffset);
    detector.setRealignMode(this.realignMode);
    detector.runDetector();
    this.folderCollection = detector.getNucleiCollections();
    this.mappingCount = detector.getMappingCount();
    IJ.log("Imported folder(s)");
    this.reAnalysisRun = true;
  }

  private void  setDetectionParameters(NucleusDetector detector){
    detector.setMinNucleusSize(this.getMinNucleusSize()); 
    detector.setMaxNucleusSize(this.getMaxNucleusSize());
    detector.setThreshold(this.getNucleusThreshold());
    detector.setMinNucleusCirc(this.getMinNucleusCirc());
    detector.setMaxNucleusCirc(this.getMaxNucleusCirc());

    detector.setAngleProfileWindowSize(this.getAngleProfileWindowSize());

    detector.setSignalThreshold(this.getSignalThreshold());
    detector.setMinSignalSize(this.getMinSignalSize());
    detector.setMaxSignalFraction(this.getMaxSignalFraction());
  }

  /*
    -----------------------
    Getters
    -----------------------
  */

  public int getNucleusThreshold(){
    return this.nucleusThreshold;
  }

  public int getSignalThreshold(){
    return this.signalThreshold;
  }

  public double getMinNucleusSize(){
    return this.minNucleusSize;
  }

  public double getMaxNucleusSize(){
    return this.maxNucleusSize;
  }

  public double getMinNucleusCirc(){
    return this.minNucleusCirc;
  }

  public double getMaxNucleusCirc(){
    return this.maxNucleusCirc;
  }

  public double getMinSignalSize(){
    return this.minSignalSize;
  }

  public double getMaxSignalFraction(){
    return this.maxSignalFraction;
  }

  public int getAngleProfileWindowSize(){
    return this.angleProfileWindowSize;
  }

  /*
    -----------------------
    Setters
    -----------------------
  */

  public void setNucleusThreshold(int i){
    this.nucleusThreshold = i;
  }

  public void setSignalThreshold(int i){
    this.signalThreshold = i;
  }

  public void setMinNucleusSize(double d){
    this.minNucleusSize = d;
  }

  public void setMaxNucleusSize(double d){
    this.maxNucleusSize = d;
  }

  public void setMinNucleusCirc(double d){
    this.minNucleusCirc = d;
  }

  public void setMaxNucleusCirc(double d){
    this.maxNucleusCirc = d;
  }

  public void setMinSignalSize(double d){
    this.minSignalSize = d;
  }

  public void setMaxSignalFraction(double d){
    this.maxSignalFraction = d;
  }

  public void setNucleusClass(Nucleus n){
    this.nucleusClass = n.getClass();
  }

  public void setNucleusCollectionClass(NucleusCollection n){
    this.collectionClass = n.getClass();
  }

  public void setAngleProfileWindowSize(int i){
    this.angleProfileWindowSize = i;
  }

  /*
    Use reflection to assign the correct class to the nuclei and populations
  */
  public void assignNucleusTypes(){
    
    Set<File> keys = this.folderCollection.keySet();

    try{
      Constructor<?> collectionConstructor = this.collectionClass.getConstructor(new Class<?>[]{File.class, String.class, String.class});
      Constructor<?> nucleusConstructor = this.nucleusClass.getConstructor(new Class<?>[]{Nucleus.class});
    
      for (File key : keys) {
        NucleusCollection collection = folderCollection.get(key);

        try{
          INuclearCollection spermNuclei = (INuclearCollection) collectionConstructor.newInstance(key, collection.getOutputFolder(), "analysable");
          
          // RodentSpermNucleusCollection spermNuclei = new RodentSpermNucleusCollection(key, "complete");
          IJ.log(key.getAbsolutePath()+"   Nuclei: "+collection.getNucleusCount());

          for(int i=0;i<collection.getNucleusCount();i++){
            INuclearFunctions p = collection.getNucleus(i);

            INuclearFunctions subNucleus  = (INuclearFunctions) nucleusConstructor.newInstance(p);
            subNucleus.findPointsAroundBorder();

            // RodentSpermNucleus p = new RodentSpermNucleus(n);
            spermNuclei.addNucleus(subNucleus);
          }
          this.nuclearPopulations.add(spermNuclei);
          IJ.log("  Population converted to "+nucleusClass.getSimpleName()+" in "+spermNuclei.getClass().getSimpleName());
        } catch(InstantiationException e){
          IJ.log("Cannot create collection: "+e.getMessage());
        } catch(IllegalAccessException e){
          IJ.log("Cannot access constructor: "+e.getMessage());
        } catch(InvocationTargetException e){
          IJ.log("Cannot invoke constructor: "+e.getMessage());
        }
      }
    } catch(NoSuchMethodException e){
      IJ.log("Cannot find constructor: "+e.getMessage());
    }

  }

  public void analysePopulations(){
    IJ.log("Beginning analysis");

    for(INuclearCollection r : this.nuclearPopulations){

      if(r.getDebugFile().exists()){
        r.getDebugFile().delete();
      }

      File folder = r.getFolder();
      IJ.log("  ----------------------------- ");
      IJ.log("  Analysing: "+folder.getName());
      IJ.log("  ----------------------------- ");

      LinkedHashMap<String, Integer> nucleusCounts = new LinkedHashMap<String, Integer>();

      try{

        
        nucleusCounts.put("input", r.getNucleusCount());
        Constructor<?> collectionConstructor = this.collectionClass.getConstructor(new Class[]{File.class, String.class, String.class});
        INuclearCollection failedNuclei = (INuclearCollection) collectionConstructor.newInstance(folder, r.getOutputFolder(), "failed");

        r.refilterNuclei(failedNuclei); // put fails into failedNuclei, remove from r
        if(failedNuclei.getNucleusCount()>0){
          IJ.log("    ----------------------------- ");
          IJ.log("    Exporting failed nuclei"       );
          IJ.log("    ----------------------------- ");
          // failedNuclei.exportStatsFiles(); // NPE on clustering profile export
          failedNuclei.annotateAndExportNuclei();
          nucleusCounts.put("failed", failedNuclei.getNucleusCount());
        }

      } catch(InstantiationException e){
        IJ.log("Cannot create collection: "+e.getMessage());
      } catch(IllegalAccessException e){
        IJ.log("Cannot access constructor: "+e.getMessage());
      } catch(InvocationTargetException e){
        IJ.log("Cannot invoke constructor: "+e.getMessage());
      } catch(NoSuchMethodException e){
        IJ.log("Cannot find constructor: "+e.getMessage());
      }

      IJ.log("    ----------------------------- ");
      IJ.log("    Analysing population: "+r.getType()+" : "+r.getNucleusCount()+" nuclei");
      IJ.log("    ----------------------------- ");

//      r.measureProfilePositions();
      
      // align profiles
      PopulationProfiler.run(r);
      
      // segment profiles
      SegmentationAnalysis.run(r, "tail");
      
      r.exportProfiles();
      r.measureNuclearOrganisation();
      
      // Perform shell analysis
      ShellAnalysis.run(r, 5);
      
      r.exportStatsFiles();
      r.annotateAndExportNuclei();

      IJ.log("    ----------------------------- ");
      IJ.log("    Refolding nucleus"             );
      IJ.log("    ----------------------------- ");

      CurveRefolder.run(r, nucleusClass, refoldMode);

    
      ArrayList<INuclearCollection> signalPopulations = dividePopulationBySignals(r);
      
      for(INuclearCollection p : signalPopulations){
       
        nucleusCounts.put(p.getType(), p.getNucleusCount());

        IJ.log("    ----------------------------- ");
        IJ.log("    Analysing population: "+p.getType()+" : "+p.getNucleusCount()+" nuclei");
        IJ.log("    ----------------------------- ");
        
        PopulationProfiler.run(p);
//        p.measureProfilePositions();
        p.exportProfiles();
        p.measureNuclearOrganisation();
        ShellAnalysis.run(p, 5);
        p.exportStatsFiles();
        p.annotateAndExportNuclei();
        CurveRefolder.run(p, nucleusClass, refoldMode);
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

      Constructor<?> collectionConstructor = this.collectionClass.getConstructor(new Class<?>[]{File.class, String.class, String.class});
      
      List<INuclearFunctions> redList = r.getNucleiWithSignals(Nucleus.RED_CHANNEL);
      if(redList.size()>0){
        // INuclearCollection redNuclei = new INuclearCollection(r.getFolder(), "red");
        INuclearCollection redNuclei = (INuclearCollection) collectionConstructor.newInstance(r.getFolder(), r.getOutputFolder(), "red");
        for(INuclearFunctions n : redList){
          redNuclei.addNucleus( (INuclearFunctions)n );
        }
        signalPopulations.add(redNuclei);
        List<INuclearFunctions> notRedList = r.getNucleiWithSignals(Nucleus.NOT_RED_CHANNEL);
        if(notRedList.size()>0){
          INuclearCollection notRedNuclei = (INuclearCollection) collectionConstructor.newInstance(r.getFolder(), r.getOutputFolder(), "not_red");
          // INuclearCollection notRedNuclei = new INuclearCollection(r.getFolder(), "not_red");
          for(INuclearFunctions n : notRedList){
            notRedNuclei.addNucleus( (INuclearFunctions)n );
          }
          signalPopulations.add(notRedNuclei);
        }
      }

      List<INuclearFunctions> greenList = r.getNucleiWithSignals(Nucleus.GREEN_CHANNEL);
      if(greenList.size()>0){
        INuclearCollection greenNuclei = (INuclearCollection) collectionConstructor.newInstance(r.getFolder(), r.getOutputFolder(), "green");
        // INuclearCollection greenNuclei = new INuclearCollection(r.getFolder(), "green");
        for(INuclearFunctions n : greenList){
          greenNuclei.addNucleus( (INuclearFunctions)n );
        }
        signalPopulations.add(greenNuclei);
        List<INuclearFunctions> notGreenList = r.getNucleiWithSignals(Nucleus.NOT_GREEN_CHANNEL);
        if(notGreenList.size()>0){
          INuclearCollection notGreenNuclei = (INuclearCollection) collectionConstructor.newInstance(r.getFolder(), r.getOutputFolder(), "not_green");
          // INuclearCollection notGreenNuclei = new INuclearCollection(r.getFolder(), "not_green");
          for(INuclearFunctions n : notGreenList){
            notGreenNuclei.addNucleus( (INuclearFunctions)n );
          }
          signalPopulations.add(notGreenNuclei);
        }
      }

    } catch(InstantiationException e){
      IJ.log("Cannot create collection: "+e.getMessage());
    } catch(IllegalAccessException e){
      IJ.log("Cannot access constructor: "+e.getMessage());
    } catch(InvocationTargetException e){
      IJ.log("Cannot invoke constructor: "+e.getMessage());
    } catch(NoSuchMethodException e){
      IJ.log("Cannot find constructor: "+e.getMessage());
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
        outLine.append("Mapping file      : "+this.nucleiToFind.getAbsolutePath()+"\r\n");
        outLine.append("Initial X offset  : "+this.xoffset+"\r\n");
        outLine.append("Initial Y offset  : "+this.yoffset+"\r\n");
        outLine.append("Aligning images   : "+this.realignMode+"\r\n");
        outLine.append("Mapping count     : "+this.mappingCount+" nuclei\r\n");
      }
      
      outLine.append("-------------------------\r\n");
      outLine.append("Parameters:\r\n");
      outLine.append("-------------------------\r\n");
      outLine.append("\tNucleus thresholding: "+this.getNucleusThreshold()+"\r\n");
      outLine.append("\tNucleus minimum size: "+this.getMinNucleusSize()+"\r\n");
      outLine.append("\tNucleus maximum size: "+this.getMaxNucleusSize()+"\r\n");
      outLine.append("\tNucleus minimum circ: "+this.getMinNucleusCirc()+"\r\n");
      outLine.append("\tNucleus maximum circ: "+this.getMaxNucleusCirc()+"\r\n");
      outLine.append("\tSignal thresholding : "+this.getSignalThreshold()+"\r\n");
      outLine.append("\tSignal minimum size : "+this.getMinSignalSize()+"\r\n");
      outLine.append("\tSignal max. fraction: "+this.getMaxSignalFraction()+"\r\n");
      outLine.append("\tAngle profile window: "+this.getAngleProfileWindowSize()+"\r\n");
      outLine.append("\tNucleus class       : "+this.nucleusClass.getSimpleName()+"\r\n");
      outLine.append("\tCollection class    : "+this.collectionClass.getSimpleName()+"\r\n");
      outLine.append("\tRefolding mode      : "+this.refoldMode+"\r\n");
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
      String outPath = r.getFolder().getAbsolutePath()+File.separator+this.outputFolderName+File.separator+"logAnalysis.txt";
      IJ.append( outLine.toString(), outPath);
    }
  }

  private String[] getNucleusTypeStrings(){
    return AnalysisCreator.nucleusTypes.keySet().toArray(new String[0]);
  }

  public boolean displayOptionsDialog(){
    GenericDialog gd = new GenericDialog("New analysis");
    gd.addNumericField("Nucleus threshold: ", nucleusThreshold, 0);
    gd.addNumericField("Signal threshold: ", signalThreshold, 0);
    gd.addNumericField("Min nuclear size: ", minNucleusSize, 0);
    gd.addNumericField("Max nuclear size: ", maxNucleusSize, 0);
    gd.addNumericField("Min nuclear circ: ", minNucleusCirc, 2);
    gd.addNumericField("Max nuclear circ: ", maxNucleusCirc, 2);
    gd.addNumericField("Min signal size: ", minSignalSize, 0);
    gd.addNumericField("Max signal fraction: ", maxSignalFraction, 2);
    gd.addNumericField("Profile window size: ", angleProfileWindowSize, 0);

    String[] items = this.getNucleusTypeStrings();
    gd.addChoice("Nucleus type", items, items[1]); // default to rodent for now

    Set<String> modeSet = CurveRefolder.MODES.keySet();
    String[] modeArray = modeSet.toArray(new String[modeSet.size()]);
    gd.addRadioButtonGroup("Consensus refolding mode:", modeArray, 1, 3, "Fast");

    gd.addCheckbox("Perform re-analysis", false);
    gd.addNumericField("X offset:      ", xoffset, 0);
    gd.addNumericField("Y offset:      ", yoffset, 0);
    gd.addCheckbox("Realign each image", true);
    gd.showDialog();
    if (gd.wasCanceled()) return false;

    nucleusThreshold = (int) gd.getNextNumber();
    signalThreshold = (int) gd.getNextNumber();
    minNucleusSize = gd.getNextNumber();
    maxNucleusSize = gd.getNextNumber();
    minNucleusCirc = gd.getNextNumber();
    maxNucleusCirc = gd.getNextNumber();
    minSignalSize = gd.getNextNumber();
    maxSignalFraction = gd.getNextNumber();
    angleProfileWindowSize = (int) gd.getNextNumber();
    performReanalysis = gd.getNextBoolean();
    xoffset = (int)gd.getNextNumber();
    yoffset = (int)gd.getNextNumber();

    String nucleusType = gd.getNextChoice();
    int nucleusCode = AnalysisCreator.nucleusTypes.get(nucleusType);
    this.collectionClass = AnalysisCreator.collectionClassTypes.get(nucleusCode);
    this.nucleusClass = AnalysisCreator.nucleusClassTypes.get(nucleusCode);
    this.refoldMode = gd.getNextRadioButton();
    this.realignMode = gd.getNextBoolean();
    return true;
  }
}

