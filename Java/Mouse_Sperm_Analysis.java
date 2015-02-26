/*
-------------------------------------------------
MOUSE SPERM CARTOGRAPHY IMAGEJ PLUGIN
-------------------------------------------------
Copyright (C) Ben Skinner 2015

This plugin allows for automated detection of FISH
signals in a mouse sperm nucleus, and measurement of
the signal position relative to the nuclear centre of
mass (CoM) and sperm tip. Works with both red and green channels.
It also generates a profile of the nuclear shape, allowing
morphology comparisons

  ---------------
  PLOT AND IMAGE FILES
  ---------------

  plotConsensus.tiff: The consensus nucleus with measured signal centres of mass displayed
  plotTailNorm.tiff: The normalised profiles centred on the tail, with median, IQR and signal positions.
  plotTipNorm.tiff: The normalised profiles centred on the tip, with median, IQR, signal positions and initial estimated tail positions.

  composite.tiff: All nuclei passing filters aggregated and rotated to put the tail at the bottom. Yellow line is tail-CoM-intersection.
                  This line divides the hook and hump ROIs (regions of interest).
                  Grey dots are initial tail estimates by 3 methods. Cyan dot is consensus tail position based on initial estimates.
                  Yellow dot is sperm tip. Pink line is the narrowest width through the nuclear CoM. Pink dot is the nuclear CoM.
                  Red and green dots are measured red and green signal CoMs. Red and green lines outline the signal ROIs.
                  The text annotation above and left of the nucleus corresponds to the image and log files in the directory.

  compositeFailed.tiff: As above, for nuclei that failed to pass filters.


  ---------------
  LOG FILES
  ---------------
  
  logProfiles: The normalised position in the array, interiorAngle and raw X position from the tail. No header row. Designed for R cut.

  logStats: The following fields for each nucleus passing filters:
      AREA            - nuclear area
      PERIMETER       - nuclear perimeter
      FERET           - longest distance across the nucleus
      PATH_LENGTH     - measure of wibbliness. Affected by thresholding.
      NORM_TAIL_INDEX - the position in the profile array normalised to 100
      DIFFERENCE      - the difference between the profile for this nucleus and the median profile of the collection of nuclei
      FAILURE_CODE    - will be 0 for all nuclei in this file
      PATH            - the path to the source image

  logFailed: The same fields for each nucleus failing filters. Failure codes are a sum of the following:
      FAILURE_TIP       = 1
      FAILURE_TAIL      = 2
      FAILURE_THRESHOLD = 4
      FAILURE_FERET     = 8
      FAILURE_ARRAY     = 16
      FAILURE_AREA      = 32
      FAILURE_PERIM     = 64
      FAILURE_OTHER     = 128

  logGreenSignals:
  logRedSignals:
    NUCLEUS_NUMBER      - the nucleus in the image. 
    SIGNAL_AREA         - area of the signal 
    SIGNAL_ANGLE        - angle of the signal CoM to nuclear CoM to the tail
    SIGNAL_FERET        - longest diameter of the signal 
    SIGNAL_DISTANCE     - distance in pixels of the signal from the nuclear CoM
    FRACTIONAL_DISTANCE - signal distance as a fraction of the distance to the nuclear border at the given angle. 0 = at CoM, 1 = at border
    SIGNAL_PERIMETER    - perimeter of the signal 
    SIGNAL_RADIUS       - radius of a circle with the same area as the signal.
    PATH                - the path to the source image

  logTailMedians: The medians centred on the tail
  logTipMedians: The medians centred on the tip
    X_POSITION       - normalised position along the profile. 0-100. Series of bins created from the normalised nuclei
    ANGLE_MEDIAN     - median angle in this bin
    Q25              - lowwer quartile
    Q75              - upper quartile
    Q10              - 10%ile
    Q90              - 90%ile
    NUMBER_OF_POINTS - the number of angles within the bin, from which the median angle was calculated             

  logConsensusNucleus: As per individual nuclei logs, but created for the consensus nucleus. Only SX, SY, FX, FY, IA are relevant.
    For each point in the nuclear boundary:
    SX - int x position
    SY - int y position
    FX - double x position
    FY - double y position
    IA - interior angle

    Remaining fields are for debugging only
    SX  SY  FX  FY  IA  MA  I_NORM  I_DELTA I_DELTA_S BLOCK_POSITION  BLOCK_NUMBER  L_MIN L_MAX IS_MIDPOINT IS_BLOCK  PROFILE_X DISTANCE_PROFILE

  ---------------
  FEATURES TO ADD
  ---------------
    Fix bug in signal drawing on tail profile
    Fix NPE bug in exporting refolded nucleus profile and images
    Signal size thresholds adapted
    Adaptive thresholding
    Measure DAPI propotions in each x-degree segment around CoM for normalisation.
      Relevant measurement code:  getResult("IntDen", 0);
    Alter filters to be more permissive of extreme Yqdel
    Clustering of profiles before median tail fitting and exclusion?
    Add smoothing to consensus nucleus outline
    Rescale consensus image plot to rotated nucleus dimensions
    Add signal areas to consensus image
    Get measure of consistency in tail predictions
    Better profile orientation detector based on area above 180
    Confirm area of consenus nucleus matches median area, to allow overlay of different genotypes
*/
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import java.io.File;
import java.io.IOException;
import java.util.*;
import no.nuclei.*;
import no.nuclei.sperm.*;
import no.analysis.*;
import no.collections.*;

public class Mouse_Sperm_Analysis
  extends ImagePlus
  implements PlugIn
{
   // /* VALUES FOR DECIDING IF AN OBJECT IS A NUCLEUS */
  private static final int NUCLEUS_THRESHOLD = 36;
  private static final double MIN_NUCLEAR_SIZE = 2000;
  private static final double MAX_NUCLEAR_SIZE = 10000;
  private static final double MIN_NUCLEAR_CIRC = 0.3;
  private static final double MAX_NUCLEAR_CIRC = 0.8;

  private static final double MIN_SIGNAL_SIZE = 50;

  private ArrayList<RodentSpermNucleusCollection> nuclearPopulations = new ArrayList<RodentSpermNucleusCollection>(0);
  private ArrayList<RodentSpermNucleusCollection> failedPopulations  = new ArrayList<RodentSpermNucleusCollection>(0);
  
  public void run(String paramString)  {

    DirectoryChooser localOpenDialog = new DirectoryChooser("Select directory of images...");
    String folderName = localOpenDialog.getDirectory();

    if(folderName==null){
      return;
    }
    IJ.log("Directory: "+folderName);
    File folder = new File(folderName);

    AnalysisCreator analysisCreator = new AnalysisCreator(folder);

    analysisCreator.setMinNucleusSize(  MIN_NUCLEAR_SIZE );
    analysisCreator.setMaxNucleusSize(  MAX_NUCLEAR_SIZE );
    analysisCreator.setMaxNucleusSize(  MAX_NUCLEAR_SIZE );
    analysisCreator.setNucleusThreshold(NUCLEUS_THRESHOLD);
    analysisCreator.setMinNucleusCirc(  MIN_NUCLEAR_CIRC );
    analysisCreator.setMaxNucleusCirc(  MAX_NUCLEAR_CIRC );
    analysisCreator.setMinSignalSize(   MIN_SIGNAL_SIZE  );

    HashMap<File, NucleusCollection> folderCollection = analysisCreator.runAnalysis();

    getPopulations(folderCollection);
    analysePopulations();

    IJ.log("----------------------------- ");
    IJ.log("All done!"                     );
    IJ.log("----------------------------- ");
  }  

  public void getPopulations(HashMap<File, NucleusCollection> folderCollection){
    Set<File> keys = folderCollection.keySet();

    for (File key : keys) {
      NucleusCollection collection = folderCollection.get(key);
      RodentSpermNucleusCollection spermNuclei = new RodentSpermNucleusCollection(key, "complete");
      IJ.log(key.getAbsolutePath()+"   Nuclei: "+collection.getNucleusCount());

      for(int i=0;i<collection.getNucleusCount();i++){
        Nucleus n = collection.getNucleus(i);
        RodentSpermNucleus p = new RodentSpermNucleus(n);
        spermNuclei.addNucleus(p);
      }
      this.nuclearPopulations.add(spermNuclei);
      IJ.log("  Population converted to Rodent Sperm Nuclei");
    }
  }

  public void analysePopulations(){
    IJ.log("Beginning analysis");

    for(RodentSpermNucleusCollection r : this.nuclearPopulations){

      if(r.getDebugFile().exists()){
        r.getDebugFile().delete();
      }

      File folder = r.getFolder();
      IJ.log("  ----------------------------- ");
      IJ.log("  Analysing: "+folder.getName());
      IJ.log("  ----------------------------- ");

      RodentSpermNucleusCollection failedNuclei = new RodentSpermNucleusCollection(folder, "failed");

      r.refilterNuclei(failedNuclei); // put fails into failedNuclei, remove from r

      IJ.log("    ----------------------------- ");
      IJ.log("    Analysing population: "+r.getType()+" : "+r.getNucleusCount()+" nuclei");
      IJ.log("    ----------------------------- ");
      // IJ.log("    Total nuclei: "+r.getNucleusCount());
      // IJ.log("    Red signals: "+r.getRedSignalCount());
      // IJ.log("    Green signals: "+r.getGreenSignalCount());


      r.measureProfilePositions();
      r.measureNuclearOrganisation();
      r.exportStatsFiles();
      r.annotateAndExportNuclei();

      // r.refilterNuclei(failedNuclei);
      IJ.log("    ----------------------------- ");
      IJ.log("    Exporting failed nuclei"       );
      IJ.log("    ----------------------------- ");
      failedNuclei.annotateAndExportNuclei();

      IJ.log("    ----------------------------- ");
      IJ.log("    Refolding nucleus"             );
      IJ.log("    ----------------------------- ");

      attemptRefoldingConsensusNucleus(r);

      ArrayList<RodentSpermNucleusCollection> signalPopulations = dividePopulationBySignals(r);
      
      for(RodentSpermNucleusCollection p : signalPopulations){

        IJ.log("    ----------------------------- ");
        IJ.log("    Analysing population: "+p.getType()+" : "+p.getNucleusCount()+" nuclei");
        IJ.log("    ----------------------------- ");
        p.measureProfilePositions();
        p.exportStatsFiles();
        p.annotateAndExportNuclei();
        attemptRefoldingConsensusNucleus(p);
      }
    }
  }

  public void attemptRefoldingConsensusNucleus(RodentSpermNucleusCollection collection){

    try{ 
      RodentSpermNucleus refoldCandidate = (RodentSpermNucleus)collection.getNucleusMostSimilarToMedian();
      if(refoldCandidate==null){
        throw new Exception();
      }
      double[] targetProfile = collection.getMedianTargetCurve(refoldCandidate);

      CurveRefolder refolder = new CurveRefolder(targetProfile, refoldCandidate);
      refolder.refoldCurve();

      // orient refolded nucleus to put tail at the bottom
      refolder.putPointAtBottom(refoldCandidate.getBorderPointOfInterest("tail"));

      // draw signals on the refolded nucleus
      refolder.addSignalsToConsensus(collection);
      refolder.exportImage(collection);

    } catch(Exception e){
      IJ.log("    Unable to refold nucleus: "+e.getMessage());
    }

  }

  /*
    Given a complete collection of nuclei, split it into up to 4 populations;
      nuclei with red signals, with green signals, without red signals and without green signals
    Only include the 'without' populations if there is a 'with' population.
  */
  public ArrayList<RodentSpermNucleusCollection> dividePopulationBySignals(RodentSpermNucleusCollection r){

    ArrayList<RodentSpermNucleusCollection> signalPopulations = new ArrayList<RodentSpermNucleusCollection>(0);

    ArrayList<Nucleus> redList = r.getNucleiWithSignals(Nucleus.RED_CHANNEL);
    if(redList.size()>0){
      RodentSpermNucleusCollection redNuclei = new RodentSpermNucleusCollection(r.getFolder(), "red");
      for(Nucleus n : redList){
        redNuclei.addNucleus( (RodentSpermNucleus)n );
      }
      signalPopulations.add(redNuclei);
      ArrayList<Nucleus> notRedList = r.getNucleiWithSignals(Nucleus.NOT_RED_CHANNEL);
      if(notRedList.size()>0){
        RodentSpermNucleusCollection notRedNuclei = new RodentSpermNucleusCollection(r.getFolder(), "not_red");
        for(Nucleus n : notRedList){
          notRedNuclei.addNucleus( (RodentSpermNucleus)n );
        }
        signalPopulations.add(notRedNuclei);
      }
    }

    ArrayList<Nucleus> greenList = r.getNucleiWithSignals(Nucleus.GREEN_CHANNEL);
    if(greenList.size()>0){
      RodentSpermNucleusCollection greenNuclei = new RodentSpermNucleusCollection(r.getFolder(), "green");
      for(Nucleus n : greenList){
        greenNuclei.addNucleus( (RodentSpermNucleus)n );
      }
      signalPopulations.add(greenNuclei);
      ArrayList<Nucleus> notGreenList = r.getNucleiWithSignals(Nucleus.NOT_GREEN_CHANNEL);
      if(notGreenList.size()>0){
        RodentSpermNucleusCollection notGreenNuclei = new RodentSpermNucleusCollection(r.getFolder(), "not_green");
        for(Nucleus n : notGreenList){
          notGreenNuclei.addNucleus( (RodentSpermNucleus)n );
        }
        signalPopulations.add(notGreenNuclei);
      }
    }
    return signalPopulations;
  }
}
