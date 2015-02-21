/*
-------------------------------------------------
SPERM CARTOGRAPHY IMAGEJ PLUGIN
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
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.ProgressBar;
import ij.gui.TextRoi;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.OpenDialog;
import ij.io.RandomAccessStream;
import ij.measure.ResultsTable;
import ij.measure.SplineFitter;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Color;
import java.awt.geom.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.*;
import nucleusAnalysis.*;

public class Sperm_Analysis
  extends ImagePlus
  implements PlugIn
{

  private static final String[] fileTypes = {".tif", ".tiff", ".jpg"};

  // colour channels
  private static final int RED_CHANNEL   = 0;
  private static final int GREEN_CHANNEL = 1;
  private static final int BLUE_CHANNEL  = 2;

  // Values for deciding whether an object is a signal
  private static final int SIGNAL_THRESHOLD = 70;
  private static final double MIN_SIGNAL_SIZE = 50; // how small can a signal be
  private static final double MAX_SIGNAL_SIZE = 2000; // how large can a signal be
  
  private static final double ANGLE_THRESHOLD = 40.0; // when calculating local minima, ignore angles above this

  /* VALUES FOR DECIDING IF AN OBJECT IS A NUCLEUS */
  private static final int NUCLEUS_THRESHOLD = 36;
  private static final double MIN_NUCLEAR_SIZE = 500;
  private static final double MAX_NUCLEAR_SIZE = 10000;
  private static final double MIN_NUCLEAR_CIRC = 0.3;
  private static final double MAX_NUCLEAR_CIRC = 0.8;
  private static final double PROFILE_INCREMENT = 0.5;

  private static final int MAX_INTERIOR_ANGLE_TO_CALL_TIP = 110;

  // failure codes - not in use, keep to add back to logFailed in refilter
  private static final int FAILURE_TIP       = 1;
  private static final int FAILURE_TAIL      = 2;
  private static final int FAILURE_THRESHOLD = 4;
  private static final int FAILURE_FERET     = 8;
  private static final int FAILURE_ARRAY     = 16;
  private static final int FAILURE_AREA      = 32;
  private static final int FAILURE_PERIM     = 64;
  private static final int FAILURE_OTHER     = 128;
  private static final int FAILURE_SIGNALS   = 256;

  // Chart drawing parameters
  private static final int CHART_WINDOW_HEIGHT     = 300;
  private static final int CHART_WINDOW_WIDTH      = 400;
  private static final int CHART_TAIL_BOX_Y_MIN    = 325;
  private static final int CHART_TAIL_BOX_Y_MID    = 340;
  private static final int CHART_TAIL_BOX_Y_MAX    = 355;
  private static final int CHART_SIGNAL_Y_LINE_MIN = 275;
  private static final int CHART_SIGNAL_Y_LINE_MAX = 315;

  private int totalNuclei = 0;
  private int nucleiFailedOnTip = 0;
  private int nucleiFailedOnTail = 0;
  private int nucleiFailedOther = 0; // generic reasons for failure

  private String logFile;
  private String failedFile;
  private String medianFile;
  private String statsFile;
  private String debugFile;

  private NucleusCollection completeCollection;
  private NucleusCollection failedNuclei;
  private NucleusCollection redNuclei;
  private NucleusCollection greenNuclei;
  private NucleusCollection notRedNuclei;
  private NucleusCollection notGreenNuclei;
  private ArrayList<NucleusCollection> nuclearPopulations = new ArrayList<NucleusCollection>(0);

    
  public void run(String paramString)  {

    DirectoryChooser localOpenDialog = new DirectoryChooser("Select directory of images...");
    String folderName = localOpenDialog.getDirectory();

    prepareLogFiles(folderName);

    IJ.showStatus("Opening directory: " + folderName);
    IJ.log("Directory: "+folderName);

    File folder = new File(folderName);
    File[] listOfFiles = folder.listFiles();
 
    completeCollection = new NucleusCollection(folderName, "complete"); // all nuclei except failed
    failedNuclei       = new NucleusCollection(folderName, "failed"); // failed nuclei; any reason
    redNuclei          = new NucleusCollection(folderName, "red"); // nuclei with one red signal
    greenNuclei        = new NucleusCollection(folderName, "green"); // nuclei with one green signal
    notRedNuclei       = new NucleusCollection(folderName, "not_red"); // nuclei without a red signal
    notGreenNuclei     = new NucleusCollection(folderName, "not_green"); // nuclei without a green signal

    for (File file : listOfFiles) {
      if (file.isFile()) {

        String fileName = file.getName();

        for( String fileType : fileTypes){
          if( fileName.endsWith(fileType) ){
            IJ.log("File:    "+fileName);

            if(!fileName.startsWith("composite") && !fileName.startsWith("plot") ){ // skip files generated by the script itself in past iterations
	            try {

	              // open and process each image here
	              String path = folderName+fileName;
	              Opener localOpener = new Opener();
	              ImagePlus localImagePlus = localOpener.openImage(path);             
	              // handle the image
	              processImage(localImagePlus, path);
	              localImagePlus.close();

	            } catch (Exception e) { 
	                IJ.log("Error:"+e);
	            }
	          }
          }
        }
      }
    }

    IJ.log("Within folder:");
    IJ.log("Total nuclei  : "+this.totalNuclei);
    IJ.log("Failed on tip : "+this.nucleiFailedOnTip);
    IJ.log("Failed (other): "+this.nucleiFailedOther);
    int analysed = completeCollection.getNucleusCount();
    IJ.log("Before filtering: "+analysed);


    IJ.log("Adding red nuclei");
    nuclearPopulations.add(redNuclei);
    nuclearPopulations.add(notRedNuclei);
    // IJ.log("Adding green nuclei");
    // nuclearPopulations.add(greenNuclei);
    // nuclearPopulations.add(notGreenNuclei);
    IJ.log("Adding complete collection");
    nuclearPopulations.add(completeCollection);

    for(int i = 0;i<nuclearPopulations.size();i++){

      NucleusCollection currentPopulation = nuclearPopulations.get(i);
      if(currentPopulation.getNucleusCount()==0){
        continue;
      }

      if(currentPopulation.collectionType.equals("not_red") && redNuclei.getNucleusCount()==0){
        continue;
      }

      if(currentPopulation.collectionType.equals("not_green") && greenNuclei.getNucleusCount()==0){
        continue;
      }

      IJ.log("Analysing population: "+currentPopulation.collectionType);
      IJ.log("  Total nuclei: "+currentPopulation.getNucleusCount());
      IJ.log("  Red signals: "+currentPopulation.getRedSignalCount());
      IJ.log("  Green signals: "+currentPopulation.getGreenSignalCount());

      currentPopulation.refilterNuclei(); // remove double nuclei, blobs, nuclei too wibbly
    
      currentPopulation.createProfileAggregate();
      currentPopulation.drawProfilePlots();

      currentPopulation.calculateNormalisedMedianLine();
      currentPopulation.findTailIndexInMedianCurve();
      currentPopulation.calculateOffsets();

      currentPopulation.refilterNuclei(); // remove any nuclei that are odd shapes
      
      currentPopulation.drawRawPositionsFromTailChart();
      currentPopulation.createNormalisedTailPositions();
      currentPopulation.drawNormalisedPositionsFromTailChart();
      currentPopulation.createTailCentredProfileAggregate();
      currentPopulation.calculateTailCentredNormalisedMedianLine();
      currentPopulation.measureNuclearOrganisation();
      currentPopulation.exportNuclearStats("logStats");
      currentPopulation.annotateImagesOfNuclei();
      currentPopulation.rotateAndAssembleNucleiForExport("composite");
      
      // curve refolding
      Nucleus refoldCandidate = currentPopulation.getNucleusMostSimilarToMedian();
      double[] targetProfile = currentPopulation.getMedianTargetCurve(refoldCandidate);

      CurveRefolder refolder = new CurveRefolder(targetProfile, refoldCandidate);
      refolder.refoldCurve();

      // orient refolded nucleus to put tail at the bottom
      refolder.putTailAtBottom();

      // draw signals on the refolded nucleus
      refolder.addSignalsToConsensus(currentPopulation);
      refolder.exportImage(currentPopulation);

    }

    failedNuclei.exportNuclearStats("logStats");
    failedNuclei.annotateImagesOfNuclei();
    failedNuclei.rotateAndAssembleNucleiForExport("composite");    
  }

  public int wrapIndex(int i, int length){
    if(i<0)
      i = length + i; // if i = -1, in a 200 length array,  will return 200-1 = 199
    if(Math.floor(i / length)>0)
      i = i - ( ((int)Math.floor(i / length) )*length); // if i is 250 in a 200 length array, will return 250-(200*1) = 50
    // if i is 201 in a 200 length array, will return 201 - floor(201/200)=1 * 200 = 1
    // if 200 in 200 length array: 200/200 = 1; 200-200 = 0

    if(i<0 || i>length){
    	IJ.log("Warning: array out of bounds: "+i);
    }
    
    return i;
  }

  /*
    If previous log files exist, delete them
    Add the header row to each file
  */
  public void prepareLogFiles(String folderName){

    this.logFile = folderName+"logProfiles.txt";
    File f = new File(logFile);
    if(f.exists()){
      f.delete();
    }
    IJ.append("# NORM_X\tANGLE\tRAW_X_FROM_TAIL", this.logFile);

    this.debugFile = folderName+"logDebug.txt";
    File h = new File(debugFile);
    if(h.exists()){
      h.delete();
    }
  }

  /*
    Calculate the <lowerPercent> quartile from a Double[] array
  */
  public static double quartile(double[] values, double lowerPercent) {

      if (values == null || values.length == 0) {
          throw new IllegalArgumentException("The data array either is null or does not contain any data.");
      }

      // Rank order the values
      double[] v = new double[values.length];
      System.arraycopy(values, 0, v, 0, values.length);
      Arrays.sort(v);

      int n = (int) Math.round(v.length * lowerPercent / 100);
      
      return (double)v[n];
  }
  /*
    Calculate the <lowerPercent> quartile from a double[] array
  */
  public static double quartile(Double[] values, double lowerPercent) {

      if (values == null || values.length == 0) {
          throw new IllegalArgumentException("The data array either is null or does not contain any data.");
      }

      // Rank order the values
      Double[] v = new Double[values.length];
      System.arraycopy(values, 0, v, 0, values.length);
      Arrays.sort(v);

      int n = (int) Math.round(v.length * lowerPercent / 100);
      
      return (double)v[n];
  }


  /*
    Input: ImagePlus image, String path to the image
    Detects nuclei within the image
    For each nuclcus, performs the full analysis
    Adds stats for each nucleus analysed to the global stats arrays
    Calculates the normalised profile plots and stores in <finalResults>
    Draws the profile on the global chart
  */
  public void processImage(ImagePlus image, String path){

    RoiManager nucleiInImage;

    nucleiInImage = findNucleiInImage(image);

    Roi[] roiArray = nucleiInImage.getSelectedRoisAsArray();
    int i = 0;

    for(Roi roi : roiArray){
      
      IJ.log("  Analysing nucleus "+i);
      try{
      	analyseNucleus(roi, image, i, path); // get the profile data back for the nucleus
      	this.totalNuclei++;
      } catch(Exception e){
      	IJ.log("  Error analysing nucleus: "+e);
      }
      i++;
    } 
  }

  /*
    Within a given image, look for nuclei using the particle analyser.
    Return an RoiManager containing the outlines of all potential nuclei
  */
  public RoiManager findNucleiInImage(ImagePlus image){

    RoiManager manager = new RoiManager(true);

    // split out blue channel
    ChannelSplitter cs = new ChannelSplitter();
    ImagePlus[] channels = cs.split(image);
    ImagePlus blue = channels[2];
    
    // threshold
    ImageProcessor ip = blue.getChannelProcessor();
    ip.smooth();
    ip.threshold(NUCLEUS_THRESHOLD);
    ip.invert();
    // blue.show();

    // run the particle analyser
    ResultsTable rt = new ResultsTable();
    ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES, 
                ParticleAnalyzer.CENTER_OF_MASS | ParticleAnalyzer.AREA ,
                 rt, MIN_NUCLEAR_SIZE, MAX_NUCLEAR_SIZE, MIN_NUCLEAR_CIRC, MAX_NUCLEAR_CIRC);
    try {
      pa.setRoiManager(manager);
      boolean success = pa.analyze(blue);
      if(success){
        String plural = manager.getCount() == 1 ? "nucleus" : "nuclei";
        IJ.log("  Found "+manager.getCount()+ " "+plural);
      } else {
        IJ.log("  Unable to perform particle analysis");
      }
    } catch(Exception e){
       IJ.log("  Error in particle analyser: "+e);
    } finally {
      blue.close();
    }
   
   return manager;
  }

  // /*
  //   Make a directory with the same name as the image being analysed
  // */
  // public String createImageDirectory(String path){
  //   File dir = new File(path);
    
  //   if (!dir.exists()) {
  //     try{
  //       dir.mkdir();
  //       IJ.log("    Dir created");
  //     } catch(Exception e) {
  //       IJ.log("Failed to create dir: "+e);
  //       IJ.log("Saving to: "+dir.toString());
  //     }
  //   }
  //   return dir.toString();
  // }

  public double getMin(double[] d){
    double min = getMax(d);
    for(int i=0;i<d.length;i++){
      if( d[i]<min)
        min = d[i];
    }
    return min;
  }

  public double getMax(double[] d){
    double max = 0;
    for(int i=0;i<d.length;i++){
      if( d[i]>max)
        max = d[i];
    }
    return max;
  }

  /*
    Carry out the full analysis of a given nucleus.
    Detect the nuclear centre of mass.
    Detect the sperm tip.
    Detect the sperm tail by multiple methods, and find a consensus point.
    Detect signals in the red and green channels, and calculate their positions relative to the CoM
    Draw regions of interest on a new image, and save this out to the relevant directory.
  */
  public void analyseNucleus(Roi nucleus, ImagePlus image, int nucleusNumber, String path){
    
    boolean nucleusPassedChecks = true; // any check can disable this
    int failureReason = 0;


    // make a copy of the nucleus only for saving out and processing
    image.setRoi(nucleus);
    image.copy();
    ImagePlus smallRegion = ImagePlus.getClipboard();

    nucleus.setLocation(0,0); // translate the roi to the new image coordinates
    smallRegion.setRoi(nucleus);


    // prepare an image processor to annotate the image
    ImageProcessor ip = smallRegion.getProcessor();


    // turn roi into Nucleus for manipulation

    File file = new File(path);
    Nucleus currentNucleus = new Nucleus(nucleus, file, smallRegion);
    currentNucleus.setNucleusNumber(nucleusNumber);


    // immediately save out a picture of the nucleus for later annotation
    // String saveFolder = createImageDirectory(currentNucleus.getPathWithoutExtension());
    // IJ.saveAsTiff(smallRegion, saveFolder+"\\"+currentNucleus.getNucleusNumber()+".tiff");


    // find tip - use the least angle method
    NucleusBorderPoint spermTip = currentNucleus.getAngleProfile().getPointWithMinimumAngle();
    if(spermTip.getInteriorAngle() > MAX_INTERIOR_ANGLE_TO_CALL_TIP){ // this is not a deep enough curve to declare the tip
        IJ.log("    Cannot reliably assign tip position");
        currentNucleus.failureCode  = currentNucleus.failureCode | FAILURE_TIP;
        this.nucleiFailedOnTip++;
        nucleusPassedChecks = false;
    }
    int tipIndex = currentNucleus.getAngleProfile().getIndexOfPoint(spermTip);
    currentNucleus.moveIndexToArrayStart(tipIndex);


    // currentNucleus.tipIndex = 0;
    currentNucleus.setSpermTip(spermTip);


    // decide if the profile is right or left handed; flip if needed
    if(!currentNucleus.isProfileOrientationOK()){
      IJ.log("    Reversing array");
      currentNucleus.reverseArray();
    }

    // now the array is in the correct orientation, calculate the distance profile
    currentNucleus.calculateDistanceProfile();
    

    // find local minima and maxima
    NucleusBorderPoint[] minima = currentNucleus.getAngleProfile().getLocalMinima();
    NucleusBorderPoint[] maxima = currentNucleus.getAngleProfile().getLocalMaxima();

    
    /*
      Find the tail point using multiple independent methods. 
      Find a consensus point

    	Method 1: Use the list of local minima to detect the tail corner
    						This is the corner furthest from the tip.
    						Can be confused as to which side of the sperm head is chosen
    */	
    NucleusBorderPoint spermTail2 = findTailPointFromMinima(spermTip, nucleusCoM, minima);
    currentNucleus.addTailEstimatePosition(spermTail2);

    /*
    	Method 2: Look at the 2nd derivative - rate of change of angles
    						Perform a 5win average smoothing of the deltas
    						Count the number of consecutive >1 degree blocks
    						Wide block far from tip = tail
    */	
    NucleusBorderPoint spermTail3 = currentNucleus.findTailFromDeltas(spermTip);
    currentNucleus.addTailEstimatePosition(spermTail3);

    /*    
      Method 3: Find the narrowest diameter around the nuclear CoM
                Draw a line orthogonal, and pick the intersecting border points
                The border furthest from the tip is the tail
    */  
    NucleusBorderPoint spermTail1 = currentNucleus.findTailByNarrowestWidthMethod();
    currentNucleus.addTailEstimatePosition(spermTail1);


    /*
      Given distinct methods for finding a tail,
      take a position between them on roi
    */
    int consensusTailIndex = currentNucleus.getPositionBetween(spermTail2, spermTail3);
    NucleusBorderPoint consensusTail = currentNucleus.getBorderPoint(consensusTailIndex);
    consensusTailIndex = currentNucleus.getPositionBetween(consensusTail, spermTail1);
    currentNucleus.tailIndex = consensusTailIndex;
    currentNucleus.setInitialConsensusTail(consensusTail);
    currentNucleus.setSpermTail(consensusTail);
    

    /*
    	Produce the normalised profile positions from the angle array.
    	Also recentre the profile on the tail position.
			No need to alter the array index; we want the same profile shape
			as in the tip-aligned plots.
			Take the consensusTailIndex and normalisedX, and offset the profile positions
			in the raw and normalised profiles appropriately
    */
    double pathLength = 0;
    double normalisedTailIndex = ((double)consensusTailIndex/(double)currentNucleus.smoothLength)*100;

    
    // if(spermTail2.getLengthTo(spermTail3) < nucleus.getFeretsDiameter() * 0.2){ // only proceed if the tail points are together  
    XYPoint prevPoint = new XYPoint(0,0);
     
    for (int i=0; i<currentNucleus.smoothLength;i++ ) {
        double normalisedX = ((double)i/(double)currentNucleus.smoothLength)*100; // normalise to 100 length
        // double normalisedXFromTail = normalisedX - normalisedTailIndex; // offset the normalised array based on the calculated tail position
        double rawXFromTail = (double)i - (double)consensusTailIndex; // offset the raw array based on the calculated tail position

        currentNucleus.normalisedXPositionsFromTip.add(normalisedX);
        currentNucleus.rawXPositionsFromTail.add(rawXFromTail);
        currentNucleus.rawXPositionsFromTip.add( (double)i);
        
        // IJ.append(normalisedX+"\t"+
        // 					currentNucleus.getBorderPoint(i).getInteriorAngle()+"\t"+
        // 					rawXFromTail, this.logFile);        

        // calculate the path length
        XYPoint thisPoint = new XYPoint(normalisedX,currentNucleus.getBorderPoint(i).getInteriorAngle());
        pathLength += thisPoint.getLengthTo(prevPoint);
        prevPoint = thisPoint;
    }

    // find the signals
    // within nuclear roi, analyze particles in colour channels
    RoiManager   redSignalManager = findSignalInNucleus(smallRegion, RED_CHANNEL);
    RoiManager greenSignalManager = findSignalInNucleus(smallRegion, GREEN_CHANNEL);

    Roi[] redSignals =     redSignalManager.getSelectedRoisAsArray();
    Roi[] greenSignals = greenSignalManager.getSelectedRoisAsArray();

    for(Roi roi : redSignals){

      ResultsTable redResults = findSignalMeasurements(smallRegion, roi, RED_CHANNEL);
      XYPoint signalCoM = new XYPoint(redResults.getValue("XM", 0),  redResults.getValue("YM", 0) );
      currentNucleus.addRedSignal( new NuclearSignal( roi, 
                                                redResults.getValue("Area",0), 
                                                redResults.getValue("Feret",0), 
                                                redResults.getValue("Perim.",0), 
                                                signalCoM));
    }

    // Add green signals to the nucleus
    for(Roi roi : greenSignals){

      ResultsTable greenResults = findSignalMeasurements(smallRegion, roi, GREEN_CHANNEL);
      XYPoint signalCoM = new XYPoint(greenResults.getValue("XM", 0),  greenResults.getValue("YM", 0) );
      currentNucleus.addGreenSignal( new NuclearSignal( roi, 
                                                  greenResults.getValue("Area",0), 
                                                  greenResults.getValue("Feret",0), 
                                                  greenResults.getValue("Perim.",0), 
                                                  signalCoM));
    }    

    // if everything checks out, add the measured parameters to the global pool
    // currently, the only reason to fail at this stage is if the tip cannot be found
    if(nucleusPassedChecks){

      currentNucleus.setPathLength(pathLength);
      this.completeCollection.addNucleus(currentNucleus);

      if(currentNucleus.getRedSignalCount()==1){
        this.redNuclei.addNucleus(currentNucleus);
        
      } else if(currentNucleus.getRedSignalCount()<1){
        this.notRedNuclei.addNucleus(currentNucleus);
      }

      if(currentNucleus.getGreenSignalCount()==1){
        this.greenNuclei.addNucleus(currentNucleus);
        
      } else if(currentNucleus.getGreenSignalCount()<1){
        this.notGreenNuclei.addNucleus(currentNucleus);
      }

    } else {
      this.failedNuclei.addNucleus(currentNucleus);
    }
  }

  /*
    Detect the tail based on a list of local minima in an NucleusBorderPoint array.
    The putative tail is the point furthest from the sum of the distances from the CoM and the tip
  */
  public NucleusBorderPoint findTailPointFromMinima(NucleusBorderPoint tip, XYPoint centre, NucleusBorderPoint[] array){
  
    // we cannot be sure that the greatest distance between two points will be the endpoints
    // because the hook may begin to curve back on itself. We supplement this basic distance with
    // the distances of each point from the centre of mass. The points with the combined greatest
    // distance are both far from each other and far from the centre, and are a more robust estimate
    // of the true ends of the signal
    double tipToCoMDistance = tip.getLengthTo(centre);

    double maxDistance = 0;
    NucleusBorderPoint tail = tip;

    for(NucleusBorderPoint a : array){
            
      double distanceAcrossCoM = tipToCoMDistance + centre.getLengthTo(a);
      double distanceBetweenEnds = tip.getLengthTo(a);
      
      double totalDistance = distanceAcrossCoM + distanceBetweenEnds;

      if(totalDistance > maxDistance){
        maxDistance = totalDistance;
        tail = a;
      }
    }
    return tail;
  }

  /*
    Detect a poiint in an NucleusBorderPoint array furthest from a given point.
  */
  public NucleusBorderPoint findPointFurthestFrom(NucleusBorderPoint p, NucleusBorderPoint[] list){

    double maxL = 0;
    NucleusBorderPoint result = p;
    for (NucleusBorderPoint a : list){
      double l = p.getLengthTo(a);
      if(l>maxL){
        maxL = l;
        result = a;
      }
    }
    return result;
  }


  public RoiManager findSignalInNucleus(ImagePlus image, int channel){

    RoiManager manager = new RoiManager(true);
    ChannelSplitter cs = new ChannelSplitter();
    ImagePlus[] channels = cs.split(image);
    ImagePlus imp = channels[channel];
    String colour = channel == 0 ? "red" : "green";

    
    // threshold
    ImageProcessor ip = imp.getChannelProcessor();
    ip.threshold(SIGNAL_THRESHOLD);
    ip.invert();

    // run the particle analyser
    ResultsTable rt = new ResultsTable();
    ParticleAnalyzer pa = new ParticleAnalyzer( ParticleAnalyzer.ADD_TO_MANAGER, 
    										                        ParticleAnalyzer.CENTER_OF_MASS | ParticleAnalyzer.AREA,
                                                 rt, 
                                                 MIN_SIGNAL_SIZE, 
                                                 MAX_SIGNAL_SIZE);
    try {
      pa.setRoiManager(manager);
      boolean success = pa.analyze(imp);
      if(success){
        String signalPlural = manager.getCount() == 1 ? "signal" : "signals"; // I am pedantic
        IJ.log("    Found "+manager.getCount()+ " "+signalPlural+" in "+colour+" channel");

      } else {
        IJ.log("    Unable to perform signal analysis");
      }
    } catch(Exception e){
       IJ.log("    Error: "+e);
    } finally {
      imp.close();
    }
    return manager;
  }

  /*
    Use the particle analyser to detect the nucleus in an image.
    Calculate parameters of interest and return a ResultsTable.
  */
  public ResultsTable findSignalMeasurements(ImagePlus imp, Roi roi, int channel){

    ChannelSplitter cs = new ChannelSplitter();
    ImagePlus[] channels = cs.split(imp);
    ImagePlus signalChannel = channels[channel];

    signalChannel.setRoi(roi);
    double feretDiameter = roi.getFeretsDiameter();

    ResultsTable rt = new ResultsTable();

    Analyzer an = new Analyzer(signalChannel, Analyzer.CENTER_OF_MASS | Analyzer.PERIMETER | Analyzer.AREA | Analyzer.FERET, rt);
    an.measure();
    return rt;
  }

  /*
    Use the particle analyser to detect the nucleus in an image.
    Calculate parameters of interest and return a ResultsTable.
  */
  public ResultsTable findNuclearMeasurements(ImagePlus imp, Roi roi){

    ChannelSplitter cs = new ChannelSplitter();
    ImagePlus[] channels = cs.split(imp);
    ImagePlus blueChannel = channels[2];

    blueChannel.setRoi(roi);
    double feretDiameter = roi.getFeretsDiameter();

    ResultsTable rt = new ResultsTable();

    Analyzer an = new Analyzer(blueChannel, Analyzer.CENTER_OF_MASS | Analyzer.PERIMETER | Analyzer.AREA | Analyzer.FERET, rt);
    an.measure();
    return rt;
  }

  /*
    Given three XYPoints, measure the angle a-b-c
      a   c
       \ /
        b
  */
  public double findAngleBetweenXYPoints(XYPoint a, XYPoint b, XYPoint c){

    float[] xpoints = { (float) a.getX(), (float) b.getX(), (float) c.getX()};
    float[] ypoints = { (float) a.getY(), (float) b.getY(), (float) c.getY()};
    PolygonRoi roi = new PolygonRoi(xpoints, ypoints, 3, Roi.ANGLE);
   return roi.getAngle();
  }

  private double getXComponentOfAngle(double length, double angle){
  	// cos(angle) = x / h
  	// x = cos(a)*h
  	double x = length * Math.cos(Math.toRadians(angle));
  	return x;
  }

  private double getYComponentOfAngle(double length, double angle){
  	double y = length * Math.sin(Math.toRadians(angle));
  	return y;
  }

  /*
    -----------------------
    NUCLEUS CLASS
    -----------------------
    Contains the variables for storing a nucleus,
    plus the functions for calculating aggregate stats
    within a nucleus
  */  
  // class Nucleus {
  
  //   private int nucleusNumber; // the number of the nucleus in the current image
  //   // private int windowSize = 23; // default size, can be overridden if needed
  //   // private int minimaCount; // the number of local minima detected in the array
  //   // private int maximaCount; // the number of local minima detected in the array
  //   private int length;  // the length of the array; shortcut to this.array.length
  //   private int smoothLength = 0; // the length of the smoothed array; shortcut to this.getBorderPointArray().length
  //   // private int minimaLookupDistance = 5; // the points ahead and behind to check when finding local minima and maxima
  //   // private int blockCount = 0; // the number of delta blocks detected
  //   // private int DELTA_WINDOW_MIN = 5; // the minimum number of points required in a delta block

  //   private int failureCode = 0; // stores a code to explain why the nucleus failed filters

  //   private int offsetForTail = 0;

  //   private int tailIndex; // the index in the smoothedArray that has been designated the tail
  //   private int tipIndex; // the index in the smoothedArray that has been designated the tip [should be 0]

  //   private double differenceToMedianCurve; // store the difference between curves as the sum of squares

  //   private double medianAngle; // the median angle from NucleusBorderPoint[] smoothedArray
  //   private double perimeter; // the nuclear perimeter
  //   private double pathLength; // the angle path length
  //   private double feret; // the maximum diameter
  //   private double area; // the nuclear area

  //   private NucleusBorderPoint[] array; // the points from the polygon made from the input roi. Not currently used.
  //   // private NucleusBorderPoint[] smoothedArray; // the interpolated points from the input polygon. Most calculations use this.
  //   private ArrayList<NucleusBorderPoint> intialSpermTails = new ArrayList<NucleusBorderPoint>(0); // holds the points considered to be sperm tails before filtering
  //   private AngleProfile angleProfile; // new class to replace smoothedArray

  //   private XYPoint centreOfMass;
  //   private NucleusBorderPoint spermTip;
  //   private NucleusBorderPoint spermTail;
  //   private NucleusBorderPoint intersectionPoint; // the point through the centre of mass directly opposite the sperm tail. Used for dividing hook/hump Rois
  //   private NucleusBorderPoint initialConsensusTail; // the point initially chosen as the tail. Used to draw tail position box plots
  //   private NucleusBorderPoint minFeretPoint1; // debugging tool used for identification of narrowest width across CoM. Stores the border point
  //   private NucleusBorderPoint minFeretPoint2;
    
  //   private String imagePath; // the path to the image being analysed

  //   private File sourceImage; // the image from which the nucleus came
  //   private File nucleusImage; // the image of just this nucleus, for annotation
  //   private File profileLog; // unused. Store output if needed

  //   // private boolean minimaCalculated = false; // has detectLocalMinima been run
  //   // private boolean maximaCalculated = false; // has detectLocalMaxima been run
  //   // private boolean anglesCalculated = false; // has makeAngleProfile been run
  //   // private boolean offsetCalculated = false; // has calculateOffsets been run
    
  //   private Roi roi; // the original ROI
  //   private Polygon polygon; // the ROI converted to a polygon; source of NucleusBorderPoint[] array

  //   private ArrayList<NuclearSignal> redSignals    = new ArrayList<NuclearSignal>(0); // an array to hold any signals detected
  //   private ArrayList<NuclearSignal> greenSignals  = new ArrayList<NuclearSignal>(0); // an array to hold any signals detected

  //   private FloatPolygon smoothedPolygon; // the interpolated polygon; source of NucleusBorderPoint[] smoothedArray // can probably be removed
  //   private FloatPolygon hookRoi;
  //   private FloatPolygon humpRoi;

  //   // these replaced measurementResults
  //   private ArrayList<Double> normalisedXPositionsFromTip  = new ArrayList<Double>(0); // holds the x values only after normalisation
  //   private ArrayList<Double> normalisedYPositionsFromTail = new ArrayList<Double>(0);
  //   private ArrayList<Double> normalisedXPositionsFromTail = new ArrayList<Double>(0);
  //   private ArrayList<Double> rawXPositionsFromTail        = new ArrayList<Double>(0);
  //   private ArrayList<Double> rawXPositionsFromTip         = new ArrayList<Double>(0);

  //   private double[] distanceProfile;
    
  //   public Nucleus (Roi roi, String path) { // construct from an roi

  //     // get the polygon from the roi
  //     this.roi = roi;
  //     this.sourceImage = new File(path);
  //     this.imagePath = sourceImage.getPath(); // eventually remove

  //     this.polygon = roi.getPolygon();
  //     this.array = new NucleusBorderPoint[this.polygon.npoints];
  //     this.length = this.array.length;
  //     for(int i=0; i<this.polygon.npoints; i++){
  //       array[i] = new NucleusBorderPoint(this.polygon.xpoints[i],this.polygon.ypoints[i]);
  //     }
     
  //    try{
  //       this.smoothedPolygon = roi.getInterpolatedPolygon(1,true); // interpolate and smooth the roi, 1 pixel spacing
  //       angleProfile = new AngleProfile(this.smoothedPolygon);
  //       this.smoothLength = angleProfile.size(); // shortcult for functions

  //     } catch(Exception e){
  //       IJ.log("Cannot create AngleProfile: "+e);
  //     } 
  //   }

  //   public Roi getRoi(){
  //   	return this.roi;
  //   }

  //   public NucleusBorderPoint[] getBorderPointArray(){
  //   	return this.angleProfile.getBorderPointArray();
  //   }

  //   public NucleusBorderPoint getBorderPoint(int i){
  //   	return angleProfile.getBorderPoint(i);
  //   }

  //   public AngleProfile getAngleProfile(){
  //     return this.angleProfile;
  //   }

  //   public double getMaxX(){
  //   	double d = 0;
  //     for(int i=0;i<smoothLength;i++){
  //     	if(angleProfile.getBorderPoint(i).getX()>d){
  //       	d = angleProfile.getBorderPoint(i).getX();
  //     	}
  //     }
  //     return d;
  //   }

  //   public double getMinX(){
  //   	double d = getMaxX();
  //     for(int i=0;i<smoothLength;i++){
  //     	if(angleProfile.getBorderPoint(i).getX()<d){
  //       	d = angleProfile.getBorderPoint(i).getX();
	 //      }
	 //    }
  //     return d;
  //   }

  //   public double getMaxY(){
  //   	double d = 0;
  //     for(int i=0;i<smoothLength;i++){
  //     	if(angleProfile.getBorderPoint(i).getY()>d){
  //       	d = angleProfile.getBorderPoint(i).getY();
  //     	}
  //     }
  //     return d;
  //   }

  //   public double getMinY(){
  //   	double d = getMaxY();
  //     for(int i=0;i<smoothLength;i++){
  //     	if(angleProfile.getBorderPoint(i).getY()<d){
  //       	d = angleProfile.getBorderPoint(i).getY();
	 //      }
	 //    }
  //     return d;
  //   }

  //   public double[] getNormalisedXPositionsFromTip(){
  //     double[] d = new double[normalisedXPositionsFromTip.size()];
  //     for(int i=0;i<normalisedXPositionsFromTip.size();i++){
  //       d[i] = normalisedXPositionsFromTip.get(i);
  //     }
  //     return d;
  //   }

  //   public double[] getNormalisedYPositionsFromTail(){
  //     double[] d = new double[normalisedYPositionsFromTail.size()];
  //     for(int i=0;i<normalisedYPositionsFromTail.size();i++){
  //       d[i] = normalisedYPositionsFromTail.get(i);
  //     }
  //     return d;
  //   }

  //   public double[] getNormalisedXPositionsFromTail(){
  //     double[] d = new double[normalisedXPositionsFromTail.size()];
  //     for(int i=0;i<normalisedXPositionsFromTail.size();i++){
  //       d[i] = normalisedXPositionsFromTail.get(i);
  //     }
  //     return d;
  //   }

  //   public double[] getRawXPositionsFromTail(){
  //     double[] d = new double[rawXPositionsFromTail.size()];
  //     for(int i=0;i<rawXPositionsFromTail.size();i++){
  //       d[i] = rawXPositionsFromTail.get(i);
  //     }
  //     return d;
  //   }

  //   public double[] getRawXPositionsFromTip(){
  //     double[] d = new double[rawXPositionsFromTip.size()];
  //     for(int i=0;i<rawXPositionsFromTip.size();i++){
  //       d[i] = rawXPositionsFromTip.get(i);
  //     }
  //     return d;
  //   }

  //   /* 
  //     Fetch the angles in the smoothed array; will be ordered from the tip
  //   */
  //   public double[] getAngles(){
  //     double[] d = new double[this.smoothLength];
  //     for(int i=0;i<this.smoothLength;i++){
  //       d[i] = angleProfile.getBorderPoint(i).getInteriorAngle();
  //     }
  //     return d;
  //   }

  //   public double getMaxRawXFromTail(){
  //     double d = 0;
  //     for(int i=0;i<rawXPositionsFromTail.size();i++){
  //       if(rawXPositionsFromTail.get(i) > d){
  //         d = rawXPositionsFromTail.get(i);
  //       }
  //     }
  //     return d;
  //   }

  //   public double getMinRawXFromTail(){
  //     double d = 0;
  //     for(int i=0;i<rawXPositionsFromTail.size();i++){
  //       if(rawXPositionsFromTail.get(i) < d){
  //         d = rawXPositionsFromTail.get(i);
  //       }
  //     }
  //     return d;
  //   }

  //   public double getMaxRawXFromTip(){
  //     double d = 0;
  //     for(int i=0;i<rawXPositionsFromTip.size();i++){
  //       if(rawXPositionsFromTip.get(i) > d){
  //         d = rawXPositionsFromTip.get(i);
  //       }
  //     }
  //     return d;
  //   }

  //   public double getMinRawXFromTip(){
  //     double d = 0;
  //     for(int i=0;i<rawXPositionsFromTip.size();i++){
  //       if(rawXPositionsFromTip.get(i) < d){
  //         d = rawXPositionsFromTip.get(i);
  //       }
  //     }
  //     return d;
  //   }

  //   public void addRedSignal(NuclearSignal n){
  //     this.redSignals.add(n);
  //   }

  //   public void addGreenSignal(NuclearSignal n){
  //     this.greenSignals.add(n);
  //   }

  //   public Polygon getPolygon(){
  //     return this.polygon;
  //   }

  //   // public void setWindowSize(int i){
  //   // 	this.windowSize = i;
  //   // }

  //   // public int getWindowSize(){
  //   // 	return this.windowSize;
  //   // }
  //   /* 
  //   Find the smoothed length of the array
  //   */
  //   public int getLength(){
  //   	return this.smoothLength;
  //   }

  //   public void setPath(String path){
  //     this.imagePath = path;
  //   }

  //   public void setNucleusNumber(int n){
  //     this.nucleusNumber = n;
  //   }

  //   public NucleusBorderPoint getPoint(int i){
  //     return this.array[i];
  //   }

  //   public NucleusBorderPoint getSmoothedPoint(int i){
  //     return this.angleProfile.getBorderPoint(i);
  //   }

  //   public String getPath(){
  //     return this.imagePath;
  //   }

  //   public String getDirectory(){
  //     File f = new File(this.imagePath);
  //     return f.getParent();
  //   }

  //   public String getPathWithoutExtension(){
      
  //     String extension = "";
  //     String trimmed = "";

  //     int i = this.imagePath.lastIndexOf('.');
  //     if (i > 0) {
  //         extension = this.imagePath.substring(i+1);
  //         trimmed = this.imagePath.substring(0,i);
  //     }
  //     return trimmed;
  //   }  

  //   public String getImageName(){
  //   	File f = new File(this.imagePath);
  //     return f.getName();
  //   }

  //   public int getNucleusNumber(){
  //     return this.nucleusNumber;
  //   }

  //   public String getPathAndNumber(){
  //     return this.imagePath+"\\"+this.nucleusNumber;
  //   }

  //   public XYPoint getCentreOfMass(){
  //     return this.centreOfMass;
  //   }

  //   public void setCentreOfMass(XYPoint p){
  //     this.centreOfMass = p;
  //   }

  //   public NucleusBorderPoint getSpermTip(){
  //     return this.spermTip;
  //   }

  //   public void setSpermTip(NucleusBorderPoint p){
  //     this.spermTip = p;
  //   }

  //   public void setInitialConsensusTail(NucleusBorderPoint p){
  //     this.initialConsensusTail = p;
  //   }

  //   public NucleusBorderPoint getInitialConsensusTail(){
  //     return this.initialConsensusTail;
  //   }


  //   public NucleusBorderPoint getSpermTail(){
  //     return this.spermTail;
  //   }

  //   public void setSpermTail(NucleusBorderPoint p){
  //     this.spermTail = p;
  //   }

  //   public double getPerimeter(){
  //     return this.perimeter;
  //   }

  //   public void setPerimeter(double d){
  //     this.perimeter = d;
  //   }

  //   public double getArea(){
  //     return this.area;
  //   }

  //   public void setArea(double d){
  //     this.area = d;
  //   }

  //   public double getFeret(){
  //     return this.feret;
  //   }

  //   public void setFeret(double d){
  //     this.feret = d;
  //   }

  //   public double getPathLength(){
  //     return this.pathLength;
  //   }

  //   public void setPathLength(double d){
  //     this.pathLength = d;
  //   }

  //   public int getTailIndex(){
  //     return this.tailIndex;
  //   }

  //   public void setTailIndex(int i){
  //     this.tailIndex = i;
  //   }

  //   public void setDistanceProfile( double[] d){
  //   	this.distanceProfile = d;
  //   }

  //   public double[] getDistanceProfile(){
  //   	return this.distanceProfile;
  //   }

  //   public ArrayList<NuclearSignal> getRedSignals(){
  //     return this.redSignals;
  //   }

  //   public ArrayList<NuclearSignal> getGreenSignals(){
  //     return this.greenSignals;
  //   }

  //   public int getRedSignalCount(){
  //     return redSignals.size();
  //   }

  //   public int getGreenSignalCount(){
  //     return greenSignals.size();
  //   }

  //   public void addTailEstimatePosition(NucleusBorderPoint p){
  //   	this.intialSpermTails.add(p);
  //   }

  //   public void reverseArray(){
  //   	this.angleProfile.reverseAngleProfile();
  //   }

  //   public void flipXAroundPoint(XYPoint p){
  //   	this.angleProfile.flipXAroundPoint(p);
  //   }

  //   public boolean isHookSide(XYPoint p){
  //     if(hookRoi.contains( (float)p.getX(), (float)p.getY() ) ){
  //       return true;
  //     } else { 
  //       return false;
  //     }
  //   }

  //   public boolean isHumpSide(XYPoint p){
  //     if(humpRoi.contains( (float)p.getX(), (float)p.getY() ) ){
  //       return true;
  //     } else { 
  //       return false;
  //     }
  //   }    

  //   /*
  //     For two NucleusBorderPoints in a Nucleus, find the point that lies halfway between them
  //     Used for obtaining a consensus between potential tail positions
  //   */
  //   public int getPositionBetween(NucleusBorderPoint pointA, NucleusBorderPoint pointB){

  //     int a = 0;
  //     int b = 0;
  //     // find the indices that correspond on the array
  //     NucleusBorderPoint[] points = this.angleProfile.getBorderPointArray();

  //     for(int i = 0; i<points.length; i++){
  //         if(points[i].overlaps(pointA)){
  //           a = i;
  //         }
  //         if(points[i].overlaps(pointB)){
  //           b = i;
  //         }
  //     }
  //     // get the midpoint
  //     int mid = (int)Math.floor( (a+b) /2);
  //     return mid;
  //   }

  //   public double[] getAngleProfileArray(){
  //   	return this.angleProfile.getAngleArray();
  //   }

  //   /*
	 //    Find the angle that the nucleus must be rotated to make the CoM-tail vertical.
  //     Uses the angle between [sperm tail x,0], sperm tail, and sperm CoM
	 //    Returns an angle
	 //  */
	 //  public double findRotationAngle(){
	 //    XYPoint end = new XYPoint(this.getSpermTail().getXAsInt(),this.getSpermTail().getYAsInt()-50);

  //     double angle = findAngleBetweenXYPoints(end, this.getSpermTail(), this.getCentreOfMass());

	 //    if(this.getCentreOfMass().getX() < this.getSpermTail().getX()){
	 //      return angle;
	 //    } else {
	 //      return 0-angle;
	 //    }
	 //  }

  //   // For a position in the roi, draw a line through the CoM and get the intersection point
  //   public NucleusBorderPoint findOppositeBorder(NucleusBorderPoint p){

  //     int minDeltaYIndex = 0;
  //     double minAngle = 180;
  //     NucleusBorderPoint[] points = this.angleProfile.getBorderPointArray();

  //     for(int i = 0; i<points.length;i++){

  //         double angle = findAngleBetweenXYPoints(p, this.getCentreOfMass(), points[i]);

  //         if(Math.abs(180 - angle) < minAngle){
  //           minDeltaYIndex = i;
  //           minAngle = 180 - angle;
  //         }
  //     }
  //     return points[minDeltaYIndex];
  //   }

  //   /*
  //     This is a method for finding a tail point independent of local minima:
  //       Find the narrowest diameter around the nuclear CoM
  //       Draw a line orthogonal, and pick the intersecting border points
  //       The border furthest from the tip is the tail
  //   */
  //   public NucleusBorderPoint findTailByNarrowestWidthMethod(){

  //   	IJ.log("Finding sperm tail from width...");
  //     // Find the narrowest point around the CoM
  //     // For a position in teh roi, draw a line through the CoM to the intersection point
  //     // Measure the length; if < min length..., store equation and border(s)

  //     double minDistance = this.getFeret();
  //     NucleusBorderPoint reference = this.getSpermTip();
  //     NucleusBorderPoint[] points = this.angleProfile.getBorderPointArray();

  //     // this.splitNucleusToHeadAndHump();

  //     for(int i=0;i<this.smoothLength;i++){

  //       NucleusBorderPoint p = this.getBorderPoint(i);
  //       NucleusBorderPoint opp = findOppositeBorder(p);
  //       double distance = p.getLengthTo(opp);

  //       if(distance<minDistance){
  //         minDistance = distance;
  //         reference = p;
  //       }
  //     }
  //     this.minFeretPoint1 = reference;
  //     this.minFeretPoint2 = findOppositeBorder(reference);
      
  //     // Using the point, draw a line from teh CoM to the border. Measure the angle to an intersection point
  //     // if close to 90, and the distance to the tip > CoM-tip, keep the point
  //     // return the best point
  //     double difference = 90;
  //     NucleusBorderPoint tail = new NucleusBorderPoint(0,0);
  //     for(int i=0;i<this.smoothLength;i++){

  //       NucleusBorderPoint p = points[i];
  //       double angle = findAngleBetweenXYPoints(reference, this.getCentreOfMass(), p);
  //       if(  Math.abs(90-angle)<difference && p.getLengthTo(this.getSpermTip()) > this.getCentreOfMass().getLengthTo( this.getSpermTip() ) ){
  //         difference = 90-angle;
  //         tail = p;
  //       }
  //     }
  //     return tail;
  //   }

  //   /*
  //     Change the smoothed array order to put the selected index at the beginning
  //     only works for smoothed array - indexes are different for normal array
  //     Input: int the index to move to the start
  //   */
  //   public void moveIndexToArrayStart(int i){
  //     this.angleProfile.moveIndexToArrayStart(i);  
  //   }

    
  //     To create the normalised tail-centred index, we want to take the 
  //     normalised tip-centred index, and move the tail index position to 
  //     the start. 
    
  //   public void createNormalisedYPositionsFromTail(){

  //     double[] tipCentredAngles = this.getAngles();
  //     double[] tipCentredXPositions = this.getNormalisedXPositionsFromTip();
  //     int tailIndex = this.getTailIndex();

  //     double[] tempArray = new double[tipCentredAngles.length];

  //     System.arraycopy(tipCentredAngles, tailIndex, tempArray, 0 , tipCentredAngles.length-tailIndex); // copy over the tailIndex to end values
  //     System.arraycopy(tipCentredAngles, 0, tempArray, tipCentredAngles.length-tailIndex, tailIndex); // copy over index 0 to tailIndex

  //     double[] tempXArray = new double[tipCentredAngles.length];
  //     System.arraycopy(tipCentredXPositions, tailIndex, tempXArray, 0 , tipCentredAngles.length-tailIndex); // copy over the tailIndex to end values
  //     System.arraycopy(tipCentredXPositions, 0, tempXArray, tipCentredAngles.length-tailIndex, tailIndex); // copy over index 0 to tailIndex


  //     for(int i=0; i<this.smoothLength;i++){
  //         this.normalisedYPositionsFromTail.add(tempArray[i]);
  //         this.normalisedXPositionsFromTail.add(tempXArray[i]);
  //     }
  //   }


  //   /*
  //     Checks if the smoothed array nuclear shape profile has the acrosome to the rear of the array
  //     If acrosome is at the beginning:
  //       returns true
  //     else returns false
  //   */
  //   public boolean isProfileOrientationOK(){

  //     // if(!this.anglesCalculated){
  //     //   this.makeAngleProfile();
  //     //   this.makeDeltaAngleProfile();
  //     // }
  //     // if(!this.minimaCalculated){
  //     //   this.detectLocalMinima();
  //     // }

  //     boolean ok = false;
  //     NucleusBorderPoint[] points = this.angleProfile.getBorderPointArray();

  //     double maxAngle = 0.0;
  //     int maxIndex = 0;
  //     for(int i=0; i<this.smoothLength;i++){

  //         double angle = points[i].getInteriorAngle();
  //         if(angle>maxAngle){
  //           maxAngle = angle;
  //           maxIndex = i;
  //         }

  //         // IJ.log(i+" \t "+angle+" \t "+minAngle+"  "+minIndex);
  //     }
  //     // IJ.log("    Maximum angle "+maxAngle+" at "+maxIndex);

  //     if(this.smoothLength - maxIndex < maxIndex){ // if the maxIndex is closer to the end than the beginning
  //       return false;
  //     } else{ 
  //       return true;
  //     }
  //   }

  //   /*
  //     Go through the deltas marked as consecutive blocks
  //     Find the midpoints of each block
  //     Return the point furthest from the tip
  //   */
  //   public NucleusBorderPoint findTailFromDeltas(NucleusBorderPoint tip){

  //   	IJ.log("Finding sperm tail from deltas...");
  //     // get the midpoint of each block
  //     ArrayList<NucleusBorderPoint> results = new ArrayList<NucleusBorderPoint>(0);
  //     int maxIndex = 0;
    
  //     // remember that block 0 is not assigned; start from 1
  //     try{

  //       this.angleProfile.updatePointsWithBlockCount();
  //       for(int i=1; i<this.angleProfile.getBlockCount();i++){

  //         // number of points in each block

  //         NucleusBorderPoint[] points = this.angleProfile.getBlockOfBorderPoints(i);
  //         for(NucleusBorderPoint p : points){
  //           if(p.isMidpoint()){ // will ignore any blocks without a midpoint established - <2 members
  //             results.add(p);
  //             // IJ.log("    Midpoint found for block "+i);
  //           }
  //         }
  //       }
  //       // IJ.log("    "+results.size()+" blocks");
  //     } catch(Exception e){
  //       IJ.log("    Error in finding midpoints: findTailFromDeltas(): "+e);
  //     }
      
  //     NucleusBorderPoint tail = new NucleusBorderPoint(0,0);
  //     try{
  //       // go through the midpoints, get the max distance from tip
  //       double maxLength = 0;
        
  //       for(Object o : results){
  //         NucleusBorderPoint p = (NucleusBorderPoint)o;
  //         if(p.getLengthTo(tip) > maxLength){
  //           maxLength = p.getLengthTo(tip);
  //           tail = p;
  //         }
  //       }
  //     } catch(Exception e){
  //       IJ.log("    Error in finding lengths: findTailFromDeltas(): "+e);
  //     }
  //      // IJ.log("    Midpoint decided at "+tail.toString());
  //     return tail;
  //   }

  //   /*
  //     For the interior angles in the smoothed angle array:
  //       Calculate the median angle in the array.
  //     Stores in medianAngle
  //   */    
  //   public void calculateMedianAngle() {

  //       double[] m = new double[this.smoothLength];
  //       for(int i = 0; i<this.smoothLength; i++){
  //         m[i] = this.getBorderPointArray()[i].getInteriorAngle();
  //       }
  //       Arrays.sort(m);

  //       int middle = m.length/2;
  //       if (m.length%2 == 1) {
  //           this.medianAngle = m[middle];
  //       } else {
  //           this.medianAngle = (m[middle-1] + m[middle]) / 2.0;
  //       }
  //   }

  //   /*
  //     Print key data to the image log file
  //     Overwrites any existing log
  //   */   
  //   public void printLogFile(String path){

  //     // String path = this.getPathWithoutExtension()+"\\"+this.getNucleusNumber()+".log";
  //     File f = new File(path);
  //     if(f.exists()){
  //       f.delete();
  //     }

  //     NucleusBorderPoint[] points = this.angleProfile.getBorderPointArray();
  //     String outLine = "SX\tSY\tFX\tFY\tIA\tMA\tI_NORM\tI_DELTA\tI_DELTA_S\tBLOCK_POSITION\tBLOCK_NUMBER\tL_MIN\tL_MAX\tIS_MIDPOINT\tIS_BLOCK\tPROFILE_X\tDISTANCE_PROFILE\n";

  //     // IJ.append("SX\tSY\tFX\tFY\tIA\tMA\tI_NORM\tI_DELTA\tI_DELTA_S\tBLOCK_POSITION\tBLOCK_NUMBER\tL_MIN\tL_MAX\tIS_MIDPOINT\tIS_BLOCK\tPROFILE_X\tDISTANCE_PROFILE", path);
      
  //     for(int i=0;i<this.smoothLength;i++){

  //       double normalisedIAngle = getBorderPointArray()[i].getInteriorAngle()-180;
  //       // double length = this.smoothLength;
  //       double normalisedX = ((double)i/(double)this.smoothLength)*100; // normalise to 100 length
        
  //       outLine = outLine + points[i].getXAsInt()+"\t"+
  //                           points[i].getYAsInt()+"\t"+
  //                           points[i].getX()+"\t"+
  //                           points[i].getY()+"\t"+
  //                           points[i].getInteriorAngle()+"\t"+
  //                           points[i].getMinAngle()+"\t"+
  //                           normalisedIAngle+"\t"+
  //                           points[i].getInteriorAngleDelta()+"\t"+
  //                           points[i].getInteriorAngleDeltaSmoothed()+"\t"+
  //                           points[i].getPositionWithinBlock()+"\t"+
  //                           points[i].getBlockNumber()+"\t"+
  //                           points[i].isLocalMin()+"\t"+
  //                           points[i].isLocalMax()+"\t"+
  //                           points[i].isMidpoint()+"\t"+
  //                           points[i].isBlock()+"\t"+
  //                           normalisedX+"\t"+
  //                           distanceProfile[i]+"\n";
  //     }
  //     IJ.append( outLine, path);
  //   }

  //   public double[] getInteriorAngles(){

  //     double[] ypoints = new double[this.smoothLength];

  //     for(int j=0;j<ypoints.length;j++){
  //         ypoints[j] = this.getBorderPoint(j).getInteriorAngle();
  //     }
  //     return ypoints;
  //   }

  //   /*
  //     For the given nucleus index:
  //     Go through the raw X positions centred on the tail, 
  //     and apply the calculated offset.
  //   */
  //   public double[] createOffsetRawProfile(){

  //     // if(!this.differencesCalculated){
  //     //   this.calculateOffsets();
  //     // }

  //     double offset = this.offsetForTail;

  //     double[] xRawCentredOnTail = this.getRawXPositionsFromTail();
  //     double[] offsetX = new double[xRawCentredOnTail.length];

  //     for(int j=0;j<xRawCentredOnTail.length;j++){
  //         offsetX[j] = xRawCentredOnTail[j]+offset;
  //     }
  //     return offsetX;
  //   }

  //   /*
		// 	In order to split the nuclear roi into hook and hump sides,
		// 	we need to get an intersection point of the line through the 
		// 	tail and centre of mass with the opposite border of the nucleus.
  //   */
  //   private int findIntersectionPointForNuclearSplit(){
  //   	// test if each point from the tail intersects the splitting line
  //     // determine the coordinates of the point intersected as int
  //     // for each xvalue of each point in array, get the line y value
  //     // at the point the yvalues are closest and not the tail point is the intersesction
  //   	double[] lineEquation = findLineEquation(this.getCentreOfMass(), this.getSpermTail());
  //     double minDeltaY = 100;
  //     int minDeltaYIndex = 0;

  //     for(int i = 0; i<smoothLength;i++){
  //     		double x = getBorderPointArray()[i].getX();
  //     		double y = getBorderPointArray()[i].getY();
  //     		double yOnLine = getYFromEquation(lineEquation, x);

  //     		double distanceToTail = getBorderPointArray()[i].getLengthTo(spermTail);

  //     		double deltaY = Math.abs(y - yOnLine);
  //     		if(deltaY < minDeltaY && distanceToTail > this.getFeret()/2){ // exclude points too close to the tail
  //     			minDeltaY = deltaY;
  //     			minDeltaYIndex = i;
  //     		}
  //     }
  //     return minDeltaYIndex;
  //   }

  //   public void splitNucleusToHeadAndHump(){

  //     int intersectionPointIndex = findIntersectionPointForNuclearSplit();
  //     NucleusBorderPoint intersectionPoint = getBorderPointArray()[intersectionPointIndex];
  //     this.intersectionPoint = intersectionPoint;

  //     // get an array of points from tip to tail
  //     ArrayList<NucleusBorderPoint> roi1 = new ArrayList<NucleusBorderPoint>(0);
  //     ArrayList<NucleusBorderPoint> roi2 = new ArrayList<NucleusBorderPoint>(0);
  //     boolean changeRoi = false;

  //     for(int i = 0; i<smoothLength;i++){

      	
  //     	int currentIndex = wrapIndex(tailIndex+i, smoothLength); // start at the tail, and go around the array
        
  //       NucleusBorderPoint p = getBorderPointArray()[currentIndex];

  //       if(currentIndex != intersectionPointIndex && !changeRoi){   // starting at the tip, assign points to roi1
  //       	roi1.add(p);
  //       }
  //       if(currentIndex==intersectionPointIndex && !changeRoi){ // until we hit the intersection point. Then, close the polygon of roi1 back to the tip. Switch to roi2
  //         roi1.add(p);
  //         roi1.add(spermTail);
  //         roi2.add(intersectionPoint);
  //         changeRoi = true;
  //       }
  //       if(currentIndex != intersectionPointIndex && currentIndex != tailIndex && changeRoi){   // continue with roi2, adjusting the index numbering as needed
  //         roi2.add(p);
  //       }

  //       if(currentIndex==tailIndex && changeRoi){ // after reaching the tail again, close the polygon back to the intersection point
  //       	roi2.add(intersectionPoint);
  //       }

  //     }

  //     float[] roi1X = new float[ roi1.size()];
  //     float[] roi2X = new float[ roi2.size()];
  //     float[] roi1Y = new float[ roi1.size()];
  //     float[] roi2Y = new float[ roi2.size()];

  //     for(int i=0;i<roi1.size();i++){
  //     	roi1X[i] = (float) roi1.get(i).getX();
  //     	roi1Y[i] = (float) roi1.get(i).getY();
  //     }

  //     for(int i=0;i<roi2.size();i++){
  //     	roi2X[i] = (float) roi2.get(i).getX();
  //     	roi2Y[i] = (float) roi2.get(i).getY();
  //     }

  //     for(int i=0;i<roi1.size();i++){
  //       if(roi1.get(i).overlaps(spermTip)){
  //         this.hookRoi = new FloatPolygon( roi1X, roi1Y);
  //         this.humpRoi = new FloatPolygon( roi2X, roi2Y);
  //         // IJ.log("Roi2 is hump");
  //         break;
  //       }
  //     }

  //     for(int i=0;i<roi2.size();i++){
  //       if(roi2.get(i).overlaps(spermTip)){
  //         this.hookRoi = new FloatPolygon( roi2X, roi2Y);
  //         this.humpRoi = new FloatPolygon( roi1X, roi1Y);
  //          // IJ.log("Roi1 is hump");
  //          break;
  //       }
  //     }
  //   }

  //   public void calculateSignalAnglesFromTail(){

  //     if(redSignals.size()>0){

  //       for(int i=0;i<redSignals.size();i++){
  //         NuclearSignal n = redSignals.get(i);
  //         double angle = findAngleBetweenXYPoints(this.getSpermTail(), this.getCentreOfMass(), n.getCentreOfMass());

  //         // hook or hump?
  //         if( this.isHookSide(n.getCentreOfMass()) ){ // hookRoi.contains((float) n.centreOfMass.getX() , (float) n.centreOfMass.getY())  
  //           angle = 360 - angle;
  //         }

  //         // set the final angle
  //         n.setAngle(angle);
  //       }
  //     }

  //   	if(greenSignals.size()>0){

  //       for(int i=0;i<greenSignals.size();i++){
  //         NuclearSignal n = greenSignals.get(i);
  //         double angle = findAngleBetweenXYPoints(this.getSpermTail(), this.getCentreOfMass(), n.getCentreOfMass());

  //         // hook or hump?
  //         if( this.isHookSide(n.getCentreOfMass()) ){
  //           angle = 360 - angle;
  //         }

  //         // set the final angle
  //         n.setAngle(angle);
  //       }
  //     }
  //   }

  //   public void calculateSignalDistances(){

  //     ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
  //     signals.add(redSignals);
  //     signals.add(greenSignals);

  //     for( ArrayList<NuclearSignal> signalGroup : signals ){

  //     	if(signalGroup.size()>0){
  //         for(int i=0;i<signalGroup.size();i++){
  //         	NuclearSignal n = signalGroup.get(i);

  //         	double distance = this.getCentreOfMass().getLengthTo(n.getCentreOfMass());
  //         	n.setDistance(distance);
  //         }
  //       }
  //     }
  //   }


  //   public double[] findLineEquation(XYPoint a, XYPoint b){

  //     // y=mx+c
  //     double deltaX = a.getX() - b.getX();
  //     double deltaY = a.getY() - b.getY();
        
  //     double m = deltaY / deltaX;
        
  //     // y - y1 = m(x - x1)
  //     double c = a.getY() -  ( m * a.getX() );
        
  //     // double testY = (m * position_2[0]) + c;
        
  //     // write("y = "+m+"x + "+c);
  //     // result=newArray(m, c);
  //     return new double[] { m, c };
  //   }

  //   public double getXFromEquation(double[] eq, double y){
  //     // x = (y-c)/m
  //     double x = (y - eq[1]) / eq[0];
  //     return x;
  //   }

  //   public double getYFromEquation(double[] eq, double x){
  //     // x = (y-c)/m
  //     double y = (eq[0] * x) + eq[1];
  //     return y;
  //   }

  //   /*
  //     Calculate the distance from the nuclear centre of
  //     mass as a fraction of the distance from the nuclear CoM, through the 
  //     signal CoM, to the nuclear border
  //   */
  //   public void calculateFractionalSignalDistances(){

  //     ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
  //     signals.add(redSignals);
  //     signals.add(greenSignals);

  //     for( ArrayList<NuclearSignal> signalGroup : signals ){
      
  //       if(signalGroup.size()>0){
  //         for(int i=0;i<signalGroup.size();i++){
  //           NuclearSignal n = signalGroup.get(i);

  //           // double distance = this.getCentreOfMass().getLengthTo(n.getCentreOfMass());

  //           // get the line equation
  //           double eq[] = findLineEquation(n.getCentreOfMass(), this.getCentreOfMass());

  //           // using the equation, get the y postion on the line for each X point around the roi
  //           double minDeltaY = 100;
  //           int minDeltaYIndex = 0;
  //           double minDistanceToSignal = 1000;

  //           for(int j = 0; j<smoothLength;j++){
  //               double x = getBorderPointArray()[j].getX();
  //               double y = getBorderPointArray()[j].getY();
  //               double yOnLine = getYFromEquation(eq, x);
  //               double distanceToSignal = getBorderPointArray()[j].getLengthTo(n.getCentreOfMass()); // fetch


  //               double deltaY = Math.abs(y - yOnLine);
  //               // find the point closest to the line; this could find either intersection
  //               // hence check it is as close as possible to the signal CoM also
  //               if(deltaY < minDeltaY && distanceToSignal < minDistanceToSignal){
  //                 minDeltaY = deltaY;
  //                 minDeltaYIndex = j;
  //                 minDistanceToSignal = distanceToSignal;
  //               }
  //           }
  //           NucleusBorderPoint borderPoint = getBorderPointArray()[minDeltaYIndex];
  //           double nucleusCoMToBorder = borderPoint.getLengthTo(this.getCentreOfMass());
  //           double signalCoMToNucleusCoM = this.getCentreOfMass().getLengthTo(n.getCentreOfMass());
  //           double fractionalDistance = signalCoMToNucleusCoM / nucleusCoMToBorder;
  //           n.setFractionalDistance(fractionalDistance);
  //         }
  //       }
  //     }
  //   }

  //   /*
  //     Go through the signals in the nucleus, and find the point on
  //     the nuclear ROI that is closest to the signal centre of mass.
  //   */
  //   public void calculateClosestBorderToSignal(){

  //     ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
  //     signals.add(redSignals);
  //     signals.add(greenSignals);

  //     for( ArrayList<NuclearSignal> signalGroup : signals ){
      
  //       if(signalGroup.size()>0){
  //         for(int i=0;i<signalGroup.size();i++){
  //           NuclearSignal n = signalGroup.get(i);

  //           int minIndex = 0;
  //           double minDistanceToSignal = 1000;

  //           for(int j = 0; j<smoothLength;j++){
  //               NucleusBorderPoint p = getBorderPointArray()[j];
  //               double distanceToSignal = p.getLengthTo(n.getCentreOfMass());

  //               // find the point closest to the CoM
  //               if(distanceToSignal < minDistanceToSignal){
  //                 minIndex = j;
  //                 minDistanceToSignal = distanceToSignal;
  //               }
  //           }
  //           // NucleusBorderPoint borderPoint = getBorderPointArray()[minIndex];
  //           n.setClosestBorderPoint(getBorderPointArray()[minIndex]);
  //         }
  //       }
  //     }
  //   }

  //   public void calculateDistanceProfile(){

  //   	IJ.log("Calculating distance profile...");
  //     double[] profile = new double[smoothLength];

  //     for(int i = 0; i<smoothLength;i++){

  //     		NucleusBorderPoint p = this.getBorderPoint(i);
  //     		NucleusBorderPoint opp = findOppositeBorder(p);

  //         profile[i] = p.getLengthTo(opp);
  //     }
  //     this.setDistanceProfile(profile);
  //   }
  // }

  /* 
    -----------------------
    NUCLEUS COLLECTION CLASS
    -----------------------
  	This class contains the nuclei that pass detection criteria
    Provides aggregate stats
  	It enables offsets to be calculated based on the median normalised curves
  */
  class NucleusCollection {

  	private String folder; // the source of the nuclei
    private String collectionType; // for annotating image names
    private String medianFile; // output medians
    private String tailNormalisedMedianFile; // output medians

  	private ArrayList<Nucleus> nucleiCollection = new ArrayList<Nucleus>(0); // store all the nuclei analysed
  
  	private double[] normalisedMedian; // this is an array of 200 angles
    private double[] normalisedTailCentredMedian; // this is an array of 200 angles

  	private boolean differencesCalculated = false;

    private Map<Double, Collection<Double>> normalisedProfiles = new HashMap<Double, Collection<Double>>();
    private Map<Double, Collection<Double>> normalisedTailCentredProfiles = new HashMap<Double, Collection<Double>>();

  	private int offsetCount = 20;
  	private int medianLineTailIndex;

    private Plot rawXFromTipPlot;
    private Plot normXFromTipPlot;
    private Plot rawXFromTailPlot;
    private Plot normXFromTailPlot;

    private PlotWindow rawXFromTipWindow;
    private PlotWindow normXFromTipWindow;
    private PlotWindow rawXFromTailWindow;
    private PlotWindow normXFromTailWindow;

    private double maxDifferenceFromMedian = 1.5; // used to filter the nuclei, and remove those too small, large or irregular to be real
    private double maxWibblinessFromMedian = 1.2; // filter for the irregular borders more stringently

  	public NucleusCollection(String folder, String type){
  		this.folder = folder;
      this.collectionType = type;
      this.medianFile = folder+"logTipMedians."+collectionType+".txt";
      this.tailNormalisedMedianFile = folder+"logTailMedians."+collectionType+".txt";

  	}

  	public void addNucleus(Nucleus r){
  		this.nucleiCollection.add(r);
  	}

    public double[] getPerimeters(){

      double[] d = new double[nucleiCollection.size()];

      for(int i=0;i<nucleiCollection.size();i++){
        d[i] = nucleiCollection.get(i).getPerimeter();
      }
      return d;
    }

    public double[] getAreas(){

      double[] d = new double[nucleiCollection.size()];

      for(int i=0;i<nucleiCollection.size();i++){
        d[i] = nucleiCollection.get(i).getArea();
      }
      return d;
    }

    public double[] getFerets(){

      double[] d = new double[nucleiCollection.size()];

      for(int i=0;i<nucleiCollection.size();i++){
        d[i] = nucleiCollection.get(i).getFeret();
      }
      return d;
    }

    public double[] getPathLengths(){

      double[] d = new double[nucleiCollection.size()];

      for(int i=0;i<nucleiCollection.size();i++){
        d[i] = nucleiCollection.get(i).getPathLength();
      }
      return d;
    }

    public double[] getArrayLengths(){

      double[] d = new double[nucleiCollection.size()];

      for(int i=0;i<nucleiCollection.size();i++){
        d[i] = nucleiCollection.get(i).smoothLength;
      }
      return d;
    }

    public int[] getTailIndexes(){
      int[] d = new int[nucleiCollection.size()];

      for(int i=0;i<nucleiCollection.size();i++){
        d[i] = nucleiCollection.get(i).getTailIndex();
      }
      return d;
    }

    public double[] getNormalisedTailIndexes(){
      double[] d = new double[nucleiCollection.size()];

      for(int i=0;i<nucleiCollection.size();i++){
        d[i] = ( (double) nucleiCollection.get(i).getTailIndex() / (double) nucleiCollection.get(i).smoothLength ) * 100;
      }
      return d;
    }

    public void createNormalisedTailPositions(){
      for(int i=0;i<nucleiCollection.size();i++){
        nucleiCollection.get(i).createNormalisedYPositionsFromTail();
      }
    }

    public double[] getDifferencesToMedian(){
      double[] d = new double[nucleiCollection.size()];

      for(int i=0;i<nucleiCollection.size();i++){
        d[i] = nucleiCollection.get(i).differenceToMedianCurve;
      }
      return d;
    }

    public Nucleus getNucleusMostSimilarToMedian(){
    	Nucleus n = nucleiCollection.get(0); // default to the first nucleus
    	double difference = 7000;
    	for(int i=0;i<nucleiCollection.size();i++){
        if(nucleiCollection.get(i).differenceToMedianCurve<difference){
        	difference = nucleiCollection.get(i).differenceToMedianCurve;
        	n = nucleiCollection.get(i);
        }
      }
      return n;
    }

    public String[] getNucleusPaths(){
      String[] s = new String[nucleiCollection.size()];

      for(int i=0;i<nucleiCollection.size();i++){
        s[i] = nucleiCollection.get(i).getPath()+"-"+nucleiCollection.get(i).getNucleusNumber();
      }
      return s;
    }

    public int getNucleusCount(){
      return this.nucleiCollection.size();
    }

    public int getRedSignalCount(){
      int count = 0;

      for(int i=0;i<nucleiCollection.size();i++){
        count += nucleiCollection.get(i).getRedSignalCount();
      }
      return count;
    }

    public int getGreenSignalCount(){
      int count = 0;

      for(int i=0;i<nucleiCollection.size();i++){
        count += nucleiCollection.get(i).getGreenSignalCount();
      }
      return count;
    }

    // allow for refiltering of nuclei based on nuclear parameters after looking at the rest of the data
    public double getMedianNuclearArea(){
      double[] areas = this.getAreas();
      double median = quartile(areas, 50);
      return median;
    }

    public double getMedianNuclearPerimeter(){
      double[] p = this.getPerimeters();
      double median = quartile(p, 50);
      return median;
    }

    public double getMedianPathLength(){
      double[] p = this.getPathLengths();
      double median = quartile(p, 50);
      return median;
    }

    public double getMedianArrayLength(){
      double[] p = this.getArrayLengths();
      double median = quartile(p, 50);
      return median;
    }

    public double getMedianFeretLength(){
      double[] p = this.getFerets();
      double median = quartile(p, 50);
      return median;
    }

    public double getMaxRawXFromTails(){
      double d = 0;
      for(int i=0;i<nucleiCollection.size();i++){
        if(nucleiCollection.get(i).getMaxRawXFromTail() > d){
          d = nucleiCollection.get(i).getMaxRawXFromTail();
        }
      }
      return d;
    }

    public double getMinRawXFromTails(){
      double d = 0;
      for(int i=0;i<nucleiCollection.size();i++){
        if(nucleiCollection.get(i).getMinRawXFromTail() < d){
          d = nucleiCollection.get(i).getMinRawXFromTail();
        }
      }
      return d;
    }

    public double getMaxRawXFromTips(){
      double d = 0;
      for(int i=0;i<nucleiCollection.size();i++){
        if(nucleiCollection.get(i).getMaxRawXFromTip() > d){
          d = nucleiCollection.get(i).getMaxRawXFromTip();
        }
      }
      return d;
    }

    public double getMinRawXFromTips(){
      double d = 0;
      for(int i=0;i<nucleiCollection.size();i++){
        if(nucleiCollection.get(i).getMaxRawXFromTip() < d){
          d = nucleiCollection.get(i).getMaxRawXFromTip();
        }
      }
      return d;
    }

    /*
      The filters needed to separate out the objects from nuclei
      Filter on: nuclear area, perimeter and array length to find
      conjoined nuclei and blobs too small to be nuclei
      Use path length to remove poorly thresholded nuclei
    */
    public void refilterNuclei(){

      double medianArea = this.getMedianNuclearArea();
      double medianPerimeter = this.getMedianNuclearPerimeter();
      double medianPathLength = this.getMedianPathLength();
      double medianArrayLength = this.getMedianArrayLength();
      double medianFeretLength = this.getMedianFeretLength();
      double medianDifferenceToMedianCurve = quartile(this.getDifferencesToMedian(),50);
      
      int beforeSize = nucleiCollection.size();

      double maxPathLength = medianPathLength * maxWibblinessFromMedian;
      double minArea = medianArea / maxDifferenceFromMedian;
      double maxArea = medianArea * maxDifferenceFromMedian;
      double maxPerim = medianPerimeter *maxDifferenceFromMedian;
      double minPerim = medianPerimeter / maxDifferenceFromMedian;
      double minFeret = medianFeretLength / maxDifferenceFromMedian;

      double maxCurveDifference = medianDifferenceToMedianCurve * 2;


      int area = 0;
      int perim = 0;
      int pathlength = 0;
      int arraylength = 0;
      int curveShape = 0;
      int feretlength = 0;

      int totalIterations = nucleiCollection.size();

      IJ.append("Prefiltered:", debugFile);
      IJ.append("    Area: "+(int)medianArea, debugFile);
      IJ.append("    Perimeter: "+(int)medianPerimeter, debugFile);
      IJ.append("    Path length: "+(int)medianPathLength, debugFile);
      IJ.append("    Array length: "+(int)medianArrayLength, debugFile);
      IJ.append("    Feret length: "+(int)medianFeretLength, debugFile);
      IJ.append("    Curve: "+(int)medianDifferenceToMedianCurve, debugFile);

      for(int i=0;i<nucleiCollection.size();i++){
        Nucleus n = nucleiCollection.get(i);
        boolean dropNucleus = false;

        if(n.getArea() > maxArea || n.getArea() < minArea ){
          n.failureCode = n.failureCode | FAILURE_AREA;
          area++;
        }
        if(n.getPerimeter() > maxPerim || n.getPerimeter() < minPerim ){
          n.failureCode = n.failureCode | FAILURE_PERIM;
          perim++;
        }
        if(n.getPathLength() > maxPathLength){ // only filter for values too big here - wibbliness detector
          n.failureCode = n.failureCode | FAILURE_THRESHOLD;
          pathlength++;
        }
        if(n.smoothLength > medianArrayLength * maxDifferenceFromMedian || n.smoothLength < medianArrayLength / maxDifferenceFromMedian ){
          n.failureCode = n.failureCode | FAILURE_ARRAY;
           arraylength++;
        }

        if(n.getFeret() < minFeret){
          n.failureCode = n.failureCode | FAILURE_FERET;
          feretlength++;
        }

        // if(n.differenceToMedianCurve > maxCurveDifference){
        //   dropNucleus = true;
        //   curveShape++;
        // }
        
        if(n.failureCode > 0){
          failedNuclei.addNucleus(n);
          this.nucleiCollection.remove(n);
          i--; // the array index automatically shifts to account for the removed nucleus. Compensate to avoid skipping nuclei
        }
      }

      medianArea = this.getMedianNuclearArea();
      medianPerimeter = this.getMedianNuclearPerimeter();
      medianPathLength = this.getMedianPathLength();
      medianArrayLength = this.getMedianArrayLength();
      medianFeretLength = this.getMedianFeretLength();
      medianDifferenceToMedianCurve = quartile(this.getDifferencesToMedian(),50);

      int afterSize = nucleiCollection.size();
      int removed = beforeSize - afterSize;

      IJ.append("Postfiltered:", debugFile);
      IJ.append("    Area: "+(int)medianArea, debugFile);
      IJ.append("    Perimeter: "+(int)medianPerimeter, debugFile);
      IJ.append("    Path length: "+(int)medianPathLength, debugFile);
      IJ.append("    Array length: "+(int)medianArrayLength, debugFile);
      IJ.append("    Feret length: "+(int)medianFeretLength, debugFile);
      IJ.append("    Curve: "+(int)medianDifferenceToMedianCurve, debugFile);
      IJ.log("Removed due to size or length issues: "+removed+" nuclei");
      IJ.append("  Due to area outside bounds "+(int)minArea+"-"+(int)maxArea+": "+area+" nuclei", debugFile);
      IJ.append("  Due to perimeter outside bounds "+(int)minPerim+"-"+(int)maxPerim+": "+perim+" nuclei", debugFile);
      IJ.append("  Due to wibbliness >"+(int)maxPathLength+" : "+(int)pathlength+" nuclei", debugFile);
      IJ.append("  Due to array length: "+arraylength+" nuclei", debugFile);
      IJ.append("  Due to feret length: "+feretlength+" nuclei", debugFile);
      IJ.append("  Due to curve shape: "+curveShape+" nuclei", debugFile);
      IJ.log("Remaining: "+this.nucleiCollection.size()+" nuclei");
    }

    /*
      We need to calculate the median angle profile. This requires binning the normalised profiles
      into bins of size PROFILE_INCREMENT to generate a table such as this:
            k   0.0   0.5   1.0   1.5   2.0 ... 99.5   <- normalised profile bins
      NUCLEUS1  180   185  170    130   120 ... 50     <- angle within those bins
      NUCLEUS2  180   185  170    130   120 ... 50

      The median of each bin can then be calculated. 
      Depending on the length of the profile arrays and the chosen increment, there may
      be >1 or <1 angle within each bin for any given nucleus. We rely on large numbers of 
      nuclei to average this problem away.

      The data are stored as a Map<Double, Collection<Double>>
    */
    public void createProfileAggregate(){

      for(int i=0;i<nucleiCollection.size();i++){

        ArrayList<Double> normalisedXValues = nucleiCollection.get(i).normalisedXPositionsFromTip;
        NucleusBorderPoint[] yValues = nucleiCollection.get(i).getBorderPointArray();

        for(double k=0.0;k<100;k+=PROFILE_INCREMENT){ // cover all the bin positions across the profile

          for(int j=0;j<normalisedXValues.size();j++){

            // double[] d = (double[])rt.get(j);
           
            if( normalisedXValues.get(j) > k && normalisedXValues.get(j) < k+PROFILE_INCREMENT){

              Collection<Double> values = normalisedProfiles.get(k);
              
              if (values==null) { // this this profile increment has not yet been encountered, create it
                  values = new ArrayList<Double>();
                  normalisedProfiles.put(k, values);
              }
              values.add(yValues[j].getInteriorAngle());
            }
          }
        }        
      }
    }

    public void createTailCentredProfileAggregate(){

      for(int i=0;i<nucleiCollection.size();i++){

        ArrayList<Double> normalisedXValues = nucleiCollection.get(i).normalisedXPositionsFromTip;
        double[] yValues = nucleiCollection.get(i).getNormalisedYPositionsFromTail();
        // NucleusBorderPoint[] yValues = nucleiCollection.get(i).getBorderPointArray();

        for(double k=0.0;k<100;k+=PROFILE_INCREMENT){ // cover all the bin positions across the profile

          for(int j=0;j<normalisedXValues.size();j++){
           
            if( normalisedXValues.get(j) > k && normalisedXValues.get(j) < k+PROFILE_INCREMENT){

              Collection<Double> values = normalisedTailCentredProfiles.get(k);
              
              if (values==null) { // this this profile increment has not yet been encountered, create it
                  values = new ArrayList<Double>();
                  normalisedTailCentredProfiles.put(k, values);
              }
              values.add(yValues[j]);
            }
          }
        }        
      }
    }



  	public void setNormalisedMedianLine(double[] d){
  		this.normalisedMedian = d;
  	}

    public void setTailCentredNormalisedMedianLine(double[] d){
      this.normalisedTailCentredMedian = d;
    }

    /*
      Write the median angles at each bin to the global log file
    */

    public ArrayList<Double[]> calculateMediansAndQuartilesOfProfile(Map<Double, Collection<Double>> profile, String logFile){

      File f = new File(logFile);
      if(f.exists()){
        f.delete();
      }

      IJ.append("# X_POSITION\tANGLE_MEDIAN\tQ25\tQ75\tQ10\tQ90\tNUMBER_OF_POINTS", logFile); 

      ArrayList<Double[]>  medianResults = new ArrayList<Double[]>(0);
      int arraySize = (int)Math.round(100/PROFILE_INCREMENT);
      Double[] xmedians = new Double[arraySize];
      Double[] ymedians = new Double[arraySize];
      Double[] lowQuartiles = new Double[arraySize];
      Double[] uppQuartiles = new Double[arraySize];
      Double[] tenQuartiles = new Double[arraySize];
      Double[] ninetyQuartiles = new Double[arraySize];

      int m = 0;
      for(double k=0.0;k<100;k+=PROFILE_INCREMENT){

        try{
            Collection<Double> values = profile.get(k);

            if(values.size()> 0){
              Double[] d = values.toArray(new Double[0]);
              int n = d.length;

              Arrays.sort(d);
              double median = quartile(d, 50.0);
              double q1     = quartile(d, 25.0);
              double q3     = quartile(d, 75.0);
              double q10    = quartile(d, 10.0);
              double q90    = quartile(d, 90.0);
             
              xmedians[m] = k;
              ymedians[m] = median;
              lowQuartiles[m] = q1;
              uppQuartiles[m] = q3;
              tenQuartiles[m] = q10;
              ninetyQuartiles[m] = q90;

              IJ.append(xmedians[m]+"\t"+
                        ymedians[m]+"\t"+
                        lowQuartiles[m]+"\t"+
                        uppQuartiles[m]+"\t"+
                        tenQuartiles[m]+"\t"+
                        ninetyQuartiles[m]+"\t"+
                        n, logFile);
            }
          } catch(Exception e){
               IJ.log("Cannot calculate median for "+k+": "+e);
               xmedians[m] = k;
               ymedians[m] = 0.0;
               lowQuartiles[m] = 0.0;
               uppQuartiles[m] = 0.0;
               tenQuartiles[m] = 0.0;
               ninetyQuartiles[m] = 0.0;
          } finally {
            m++;
        }
      }

      // repair medians with no points by interpolation
      for(int i=0;i<xmedians.length;i++){
        if(ymedians[i] == 0 && lowQuartiles[i] == 0 && uppQuartiles[i] == 0){

          int replacementIndex = 0;

          if(xmedians[i]<1)
            replacementIndex = i+1;
          if(xmedians[i]>99)
            replacementIndex = i-1;

          ymedians[i]        = ymedians[replacementIndex]    ;
          lowQuartiles[i]    = lowQuartiles[replacementIndex];
          uppQuartiles[i]    = uppQuartiles[replacementIndex];
          tenQuartiles[i]    = tenQuartiles[replacementIndex];
          ninetyQuartiles[i] = ninetyQuartiles[replacementIndex];

          IJ.log("Repaired medians at "+i+" with values from  "+replacementIndex);
        }
      }

      medianResults.add(xmedians);
      medianResults.add(ymedians);
      medianResults.add(lowQuartiles);
      medianResults.add(uppQuartiles);
      medianResults.add(tenQuartiles);
      medianResults.add(ninetyQuartiles);
      return medianResults;
    }

    private double[] getDoubleFromDouble(Double[] d){
      double[] results = new double[d.length];
      for(int i=0;i<d.length;i++){
        results[i] = d[i];
      }
      return results;
    }

    public void calculateNormalisedMedianLine(){
      // output the final results: calculate median positions

      ArrayList<Double[]> medians = calculateMediansAndQuartilesOfProfile(this.normalisedProfiles, this.medianFile );

      double[] xmedians        =  getDoubleFromDouble( medians.get(0) );
      double[] ymedians        =  getDoubleFromDouble( medians.get(1) );
      double[] lowQuartiles    =  getDoubleFromDouble( medians.get(2) );
      double[] uppQuartiles    =  getDoubleFromDouble( medians.get(3) );
      double[] tenQuartiles    =  getDoubleFromDouble( medians.get(4) );
      double[] ninetyQuartiles =  getDoubleFromDouble( medians.get(5) );

      setNormalisedMedianLine(ymedians);

      // add the median lines to the chart
      normXFromTipPlot.setColor(Color.BLACK);
      normXFromTipPlot.setLineWidth(3);
      normXFromTipPlot.addPoints(xmedians, ymedians, Plot.LINE);
      normXFromTipPlot.setColor(Color.DARK_GRAY);
      normXFromTipPlot.setLineWidth(2);
      normXFromTipPlot.addPoints(xmedians, lowQuartiles, Plot.LINE);
      normXFromTipPlot.addPoints(xmedians, uppQuartiles, Plot.LINE);

      // handle the normalised tail position mapping
      double[] xTails = this.getNormalisedTailIndexes();

      double[] yTails = new double[xTails.length];
      Arrays.fill(yTails, CHART_TAIL_BOX_Y_MID); // all dots at y=300
      normXFromTipPlot.setColor(Color.LIGHT_GRAY);
      normXFromTipPlot.addPoints(xTails, yTails, Plot.DOT);

      // median tail positions
      double tailQ50 = quartile(xTails, 50);
      double tailQ25 = quartile(xTails, 25);
      double tailQ75 = quartile(xTails, 75);

      normXFromTipPlot.setColor(Color.DARK_GRAY);
      normXFromTipPlot.setLineWidth(1);
      normXFromTipPlot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MAX, tailQ75, CHART_TAIL_BOX_Y_MAX);
      normXFromTipPlot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MIN, tailQ75, CHART_TAIL_BOX_Y_MIN);
      normXFromTipPlot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MIN, tailQ25, CHART_TAIL_BOX_Y_MAX);
      normXFromTipPlot.drawLine(tailQ75, CHART_TAIL_BOX_Y_MIN, tailQ75, CHART_TAIL_BOX_Y_MAX);
      normXFromTipPlot.drawLine(tailQ50, CHART_TAIL_BOX_Y_MIN, tailQ50, CHART_TAIL_BOX_Y_MAX);
      normXFromTipWindow.drawPlot(normXFromTipPlot);
    }

    public void calculateTailCentredNormalisedMedianLine(){

      ArrayList<Double[]> medians = calculateMediansAndQuartilesOfProfile(this.normalisedTailCentredProfiles, this.tailNormalisedMedianFile);

      double[] xmedians        =  getDoubleFromDouble( medians.get(0) );
      double[] ymedians        =  getDoubleFromDouble( medians.get(1) );
      double[] lowQuartiles    =  getDoubleFromDouble( medians.get(2) );
      double[] uppQuartiles    =  getDoubleFromDouble( medians.get(3) );
      double[] tenQuartiles    =  getDoubleFromDouble( medians.get(4) );
      double[] ninetyQuartiles =  getDoubleFromDouble( medians.get(5) );

      setTailCentredNormalisedMedianLine(ymedians);

      // add the median lines to the chart
      normXFromTailPlot.setColor(Color.BLACK);
      normXFromTailPlot.setLineWidth(3);
      normXFromTailPlot.addPoints(xmedians, ymedians, Plot.LINE);
      normXFromTailPlot.setColor(Color.DARK_GRAY);
      normXFromTailPlot.setLineWidth(2);
      normXFromTailPlot.addPoints(xmedians, lowQuartiles, Plot.LINE);
      normXFromTailPlot.addPoints(xmedians, uppQuartiles, Plot.LINE);

      normXFromTailWindow.drawPlot(normXFromTailPlot);
    }

    /*
      Calculate the offsets needed to corectly assign the tail positions
      compared to ideal median curves
    */
  	private void calculateOffsets(){

  		for(int i= 0; i<this.nucleiCollection.size();i++){ // for each roi
  			Nucleus r = this.nucleiCollection.get(i);
  			// int offset = calculateOffsetInNucleus(this.nucleiCollection.get(i));

				// double minSqDifference = 1000000000; // stupidly big until we see actual values
				// int minSqOffset = 0; // default to no change

	      int curveTailIndex = r.tailIndex;

	      // the curve needs to be matched to the median 
	      // hence the median array needs to be the same curve length
	      double[] medianInterpolatedArray = interpolateMedianToLength(r.smoothLength);

	      // alter the median tail index to the interpolated curve equivalent
	      int medianTailIndex = (int)Math.round(( (double)this.medianLineTailIndex / (double)normalisedMedian.length )* r.smoothLength);
				
	      if(medianInterpolatedArray.length != r.smoothLength){
	        IJ.log("    Error: interpolated median array is not the right length");
	      }

	      int offset = curveTailIndex - medianTailIndex;

	      // for comparisons between sperm, get the square difference between the offset curve and the median

				double totalDifference = 0;

				for(int j=0; j<r.smoothLength; j++){ // for each point round the array

		      // IJ.log("j="+j);
		      // find the next point in the array, given the tail point is our 0
		      int curveIndex = wrapIndex(curveTailIndex+j-offset, r.smoothLength);
		      // IJ.log("Curve index: "+curveIndex);

		      // get the angle at this point
		      double curveAngle = r.getBorderPointArray()[curveIndex].getInteriorAngle();

		      // get the next median index position, given the tail point is 0
		      int medianIndex = wrapIndex(medianTailIndex+j, medianInterpolatedArray.length); // DOUBLE CHECK THE LOGIC HERE - CAUSING NPE WHEN USING  normalisedMedian.length
		      // IJ.log("Median index: "+medianIndex);
		      double medianAngle = medianInterpolatedArray[medianIndex];
		      // IJ.log("j="+j+" Curve index: "+curveIndex+" Median index: "+medianIndex+" Median: "+medianAngle);
		      // double difference = 
		      totalDifference += Math.abs(curveAngle - medianAngle);
				}

				this.nucleiCollection.get(i).offsetForTail = offset;

        // r.offsetCalculated = true;
        r.tailIndex = r.tailIndex-offset; // update the tail position
        r.setSpermTail(r.getBorderPointArray()[r.tailIndex]); // ensure the spermTail is updated
        r.differenceToMedianCurve = totalDifference;
  		}

  		this.differencesCalculated = true;
  	}

    public double[] interpolateMedianToLength(int newLength){

      int oldLength = normalisedMedian.length;
      
      double[] newMedianCurve = new double[newLength];
      // where in the old curve index is the new curve index?
      for (int i=0; i<newLength; i++) {
        // we have a point in the new curve.
        // we want to know which points it lay between in the old curve
        double oldIndex = ( (double)i / (double)newLength)*oldLength; // get the frational index position needed
        double interpolatedMedian = interpolateNormalisedMedian(oldIndex, oldLength);
        newMedianCurve[i] = interpolatedMedian;
      }
      return newMedianCurve;
    }

    /*
      Create the plots that we will be using
      Get the x max and min as needed from aggregate stats
    */
    private void preparePlots(){

      this.rawXFromTipPlot = new Plot( "Raw tip-centred plot",
                                  "Position",
                                  "Angle", Plot.Y_GRID | Plot.X_GRID);
      rawXFromTipPlot.setLimits(0,this.getMaxRawXFromTips(),-50,360);
      rawXFromTipPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
      rawXFromTipPlot.setYTicks(true);
      rawXFromTipPlot.setColor(Color.BLACK);
      rawXFromTipPlot.drawLine(0, 180, this.getMaxRawXFromTips(), 180); 
      rawXFromTipPlot.setColor(Color.LIGHT_GRAY);


      normXFromTipPlot = new Plot("Normalised tip-centred plot",
                                  "Position",
                                  "Angle", Plot.Y_GRID | Plot.X_GRID);
      normXFromTipPlot.setLimits(0,100,-50,360);
      normXFromTipPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
      normXFromTipPlot.setYTicks(true);
      normXFromTipPlot.setColor(Color.BLACK);
      normXFromTipPlot.drawLine(0, 180, 100, 180); 
      normXFromTipPlot.setColor(Color.LIGHT_GRAY);


      this.rawXFromTailPlot = new Plot( "Raw tail-centred plot",
                                  "Position",
                                  "Angle", Plot.Y_GRID | Plot.X_GRID);
      rawXFromTailPlot.setLimits( this.getMinRawXFromTails(),
                                  this.getMaxRawXFromTails(),
                                  -50,360);
      rawXFromTailPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
      rawXFromTailPlot.setYTicks(true);
      rawXFromTailPlot.setColor(Color.BLACK);
      rawXFromTailPlot.drawLine(this.getMinRawXFromTails(), 180, this.getMaxRawXFromTails(), 180); 
      rawXFromTailPlot.setColor(Color.LIGHT_GRAY);

      this.normXFromTailPlot = new Plot("Normalised tail-centred plot", "Position", "Angle", Plot.Y_GRID | Plot.X_GRID);
      normXFromTailPlot.setLimits(0,100,-50,360);
      normXFromTailPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
      normXFromTailPlot.setYTicks(true);
      normXFromTailPlot.setColor(Color.BLACK);
      normXFromTailPlot.drawLine(0, 180, 100, 180); 
      normXFromTailPlot.setColor(Color.LIGHT_GRAY);
    }

    /*
      Create the charts of the profiles of the nuclei within this collecion.
      Currently drawing: 
        Tip-aligned raw X
        Tail-aligned raw X
        Tip-aligned normalised X
    */
    public void drawProfilePlots(){

      preparePlots();

      for(int i=0;i<nucleiCollection.size();i++){
        
        double[] rawXpoints         = nucleiCollection.get(i).getRawXPositionsFromTip();
        double[] yPoints            = nucleiCollection.get(i).getAngleProfileArray();
        double[] normalisedXFromTip = nucleiCollection.get(i).getNormalisedXPositionsFromTip();
        double[] rawXFromTail       = nucleiCollection.get(i).getRawXPositionsFromTail();

        this.rawXFromTipPlot.setColor(Color.LIGHT_GRAY);
        this.rawXFromTipPlot.addPoints(rawXpoints, yPoints, Plot.LINE);

        this.normXFromTipPlot.setColor(Color.LIGHT_GRAY);
        this.normXFromTipPlot.addPoints(normalisedXFromTip, yPoints, Plot.LINE);

        this.rawXFromTailPlot.setColor(Color.LIGHT_GRAY);
        this.rawXFromTailPlot.addPoints(rawXFromTail, yPoints, Plot.LINE);
        
      }

      // this.rawXFromTipPlot.draw();
      
      rawXFromTipWindow.noGridLines = true; 
      rawXFromTipWindow = rawXFromTipPlot.show();
      
      normXFromTipWindow.noGridLines = true; 
      normXFromTipWindow = normXFromTipPlot.show();
      
      rawXFromTailWindow.noGridLines = true; 
      rawXFromTailWindow = rawXFromTailPlot.show();
    }

  	/*
			Take an index position from a non-normalised profile
			Normalise it
			Find the corresponding angle in the median curve
			Interpolate as needed
  	*/
  	public double interpolateNormalisedMedian(double normIndex, int length){

  		// normalise the index
  		// double normIndex = ( (double)index / (double)length)*this.normalisedMedian.length;

  		// convert index to 1 window boundaries
  		int medianIndex1 = (int)Math.round(normIndex);
  		int medianIndex2 = medianIndex1 > normIndex
  												? medianIndex1 - 1
  												: medianIndex1 + 1;

  		int medianIndexLower = medianIndex1 < medianIndex2
  														? medianIndex1
  														: medianIndex2;

  		int medianIndexHigher = medianIndex2 < medianIndex1
  														 ? medianIndex2
  														 : medianIndex1;

  		// wrap the arrays
      medianIndexLower  = wrapIndex(medianIndexLower, length);
      medianIndexHigher = wrapIndex(medianIndexHigher, length);

  		// get the angle values in the median profile at the given indices
  		double medianAngleLower = this.normalisedMedian[medianIndexLower];
  		double medianAngleHigher = this.normalisedMedian[medianIndexHigher];

  		// interpolate on a stright line between the points
  		double medianAngleDifference = medianAngleHigher - medianAngleLower;
  		double positionToFind = medianIndexHigher - normIndex;
  		double interpolatedMedianAngle = (medianAngleDifference * positionToFind) + medianAngleLower;
  		return interpolatedMedianAngle;
  	}

  	public int findTailIndexInMedianCurve(){
			// can't use regular tail detector, because it's based on NucleusBorderPoints
			// get minima in curve, then find the lowest minima / minima furthest from both ends

  		ArrayList minima = detectLocalMinimaInMedian();

  		double minDiff = normalisedMedian.length;
  		double minAngle = 180;
  		int tailIndex = 0;

      if(minima.size()==0){
        IJ.log("  Error: no minima found in median line");
        tailIndex = 100; // set to roughly the middle of the array for the moment

      } else{

    		for(int i = 0; i<minima.size();i++){
    			Integer index = (Integer)minima.get(i);
          // IJ.log("  Minima at: "+index);
    			// int index = (int);
    			int toEnd = normalisedMedian.length - index;
    			int diff = Math.abs(index - toEnd);

    			double angle = normalisedMedian[index];
    			if(angle<minAngle && index > 40 && index < 120){ // get the lowest point that is not the tip
    				minAngle = angle;
    				tailIndex = index;
    			}
    		}
    		// IJ.log("Median tail index: "+tailIndex);
    		this.medianLineTailIndex = tailIndex;
    }
  		return tailIndex;
  	}

  	private ArrayList<Integer> detectLocalMinimaInMedian(){
      // go through angle array (with tip at start)
      // look at 1-2-3-4-5 points ahead and behind.
      // if all greater, local minimum
      int lookupDistance = 5;
      
      double[] prevAngles = new double[lookupDistance]; // slots for previous angles
      double[] nextAngles = new double[lookupDistance]; // slots for next angles

      // int count = 0;

      ArrayList<Integer> medianIndexMinima = new ArrayList<Integer>(0);

      for (int i=0; i<normalisedMedian.length; i++) { // for each position in sperm

        // go through each lookup position and get the appropriate angles
        for(int j=0;j<prevAngles.length;j++){

          int prev_i = i-(j+1); // the index j+1 before i
          int next_i = i+(j+1); // the index j+1 after i

          // handle beginning of array - wrap around
          if(prev_i < 0){
            prev_i = normalisedMedian.length + prev_i; // length of array - appropriate value
          }

          // handle end of array - wrap
          if(next_i >= normalisedMedian.length){
            next_i = next_i - normalisedMedian.length;
          }

          // fill the lookup array
          prevAngles[j] = this.normalisedMedian[prev_i];
          nextAngles[j] = this.normalisedMedian[next_i];
        }
        
        // with the lookup positions, see if minimum at i
        // return a 1 if all higher than last, 0 if not
        // prev_l = 0;
        int errors = 2; // allow two positions to be out of place; better handling of noisy data
        boolean ok = true;
        for(int l=0;l<prevAngles.length;l++){

          // for the first position in prevAngles, compare to the current index
          if(l==0){
            if(prevAngles[l] < this.normalisedMedian[i] || nextAngles[l] < this.normalisedMedian[i]){
              // ok = false;
              errors--;
            }
          } else { // for the remainder of the positions in prevAngles, compare to the prior prevAngle
            
            if(prevAngles[l] < prevAngles[l-1] || nextAngles[l] < nextAngles[l-1]){
              // ok = false;
              errors--;
            }
          }
          if(errors<0){
            ok = false;
          }
          // if( this.normalisedMedian[i] > -20){ // ignore any values close to 180 degrees
          //   ok = false;
          // }
        }

        if(ok){
          // count++;
          medianIndexMinima.add(i);
          // IJ.log("Minima at: "+i);
        }

      }
      
      return medianIndexMinima;
    }

    public void drawInterpolatedMedians(double[] d){

      logFile = folder+"logInterpolatedMedians.txt";

      IJ.append("INDEX\tANGLE", logFile);
      for(int i=0;i<d.length;i++){
        IJ.append(i+"\t"+d[i], logFile);
      }
      IJ.append("", logFile);

    }

    public void drawOffsets(double[] d){

      logFile = folder+"logOffsets.txt";
      IJ.append("OFFSET\tDIFFERENCE", logFile);

      for(int i=0;i<d.length;i++){
        IJ.append(i+"\t"+d[i], logFile);
      }
      IJ.append("", logFile);

    }

    public void measureNuclearOrganisation(){

      for(int i= 0; i<this.nucleiCollection.size();i++){ // for each roi

         this.nucleiCollection.get(i).splitNucleusToHeadAndHump();
         this.nucleiCollection.get(i).calculateSignalAnglesFromTail();
         this.nucleiCollection.get(i).calculateSignalDistances();
         this.nucleiCollection.get(i).calculateFractionalSignalDistances();
         this.nucleiCollection.get(i).calculateClosestBorderToSignal();
      }
      this.exportSignalStats();
      addSignalsToProfileChart();

      // find nearest border

      IJ.log("Red signals: "+ this.getRedSignalCount());
      IJ.log("Green signals: "+ this.getGreenSignalCount());
    }

    private void addSignalsToProfileChart(){
      // PlotWindow normXFromTipWindow; normXFromTipPlot
      // for each signal in each nucleus, find index of point. Draw dot at index at y=-30 (for now)
      // Add the signals to the tip centred profile plot

      normXFromTipPlot.setColor(Color.LIGHT_GRAY);
      normXFromTipPlot.setLineWidth(1);
      normXFromTipPlot.drawLine(0,CHART_SIGNAL_Y_LINE_MIN,100,CHART_SIGNAL_Y_LINE_MIN);
      normXFromTipPlot.drawLine(0,CHART_SIGNAL_Y_LINE_MAX,100,CHART_SIGNAL_Y_LINE_MAX);

      for(int i= 0; i<this.nucleiCollection.size();i++){ // for each roi

        Nucleus n = this.nucleiCollection.get(i);

        ArrayList<NuclearSignal> redSignals = n.getRedSignals();
        if(redSignals.size()>0){

          ArrayList<Double> redPoints = new ArrayList<Double>(0);
          ArrayList<Double> yPoints   = new ArrayList<Double>(0);

          for(int j=0; j<redSignals.size();j++){

            NucleusBorderPoint border = redSignals.get(j).getClosestBorderPoint();
            for(int k=0; k<n.smoothLength;k++){

              if(n.getBorderPointArray()[k].overlaps(border)){
                redPoints.add( n.normalisedXPositionsFromTip.get(k) );
                double yPosition = CHART_SIGNAL_Y_LINE_MIN + ( redSignals.get(j).getFractionalDistance() * ( CHART_SIGNAL_Y_LINE_MAX - CHART_SIGNAL_Y_LINE_MIN) ); // 
                yPoints.add(yPosition);
              }
            }
          }
          normXFromTipPlot.setColor(Color.RED);
          normXFromTipPlot.setLineWidth(2);
          normXFromTipPlot.addPoints(redPoints, yPoints, Plot.DOT);
        }
      }
      normXFromTipWindow.drawPlot(normXFromTipPlot);

      // Add the signals to the tail centred profile plot
      normXFromTailPlot.setColor(Color.LIGHT_GRAY);
      normXFromTailPlot.setLineWidth(1);
      normXFromTailPlot.drawLine(0,CHART_SIGNAL_Y_LINE_MIN,100,CHART_SIGNAL_Y_LINE_MIN);
      normXFromTailPlot.drawLine(0,CHART_SIGNAL_Y_LINE_MAX,100,CHART_SIGNAL_Y_LINE_MAX);

      for(int i= 0; i<this.nucleiCollection.size();i++){ // for each roi

        Nucleus n = this.nucleiCollection.get(i);


        ArrayList<NuclearSignal> redSignals = n.getRedSignals();
        if(redSignals.size()>0){

          ArrayList<Double> redPoints = new ArrayList<Double>(0);
          ArrayList<Double> yPoints   = new ArrayList<Double>(0);

          for(int j=0; j<redSignals.size();j++){

            NucleusBorderPoint border = redSignals.get(j).getClosestBorderPoint();
            for(int k=0; k<n.smoothLength;k++){

              if(n.getBorderPointArray()[k].overlaps(border)){
                // IJ.log("Found closest border: "+i+" : "+j);
                redPoints.add( n.normalisedXPositionsFromTail.get(k) );
                double yPosition = CHART_SIGNAL_Y_LINE_MIN + ( redSignals.get(j).getFractionalDistance() * ( CHART_SIGNAL_Y_LINE_MAX - CHART_SIGNAL_Y_LINE_MIN) ); // make between 220 and 260
                yPoints.add(yPosition);
              }
            }
          }
          normXFromTailPlot.setColor(Color.RED);
          normXFromTailPlot.setLineWidth(2);
          normXFromTailPlot.addPoints(redPoints, yPoints, Plot.DOT);
        }
      }
      normXFromTailWindow.drawPlot(normXFromTailPlot);

      ImagePlus tipPlot = normXFromTipPlot.getImagePlus();
      IJ.saveAsTiff(tipPlot, this.folder+"plotTipNorm."+collectionType+".tiff");
      ImagePlus tailPlot = normXFromTailPlot.getImagePlus();
      IJ.saveAsTiff(tailPlot, this.folder+"plotTailNorm."+collectionType+".tiff");
    }

    public void exportSignalStats(){

      String redLogFile = this.folder+"logRedSignals."+collectionType+".txt";
      File r = new File(redLogFile);
      if(r.exists()){
        r.delete();
      }

      String greenLogFile = this.folder+"logGreenSignals."+collectionType+".txt";
      File g = new File(greenLogFile);
      if(g.exists()){
        g.delete();
      }

      IJ.append("# NUCLEUS_NUMBER\tSIGNAL_AREA\tSIGNAL_ANGLE\tSIGNAL_FERET\tSIGNAL_DISTANCE\tFRACTIONAL_DISTANCE\tSIGNAL_PERIMETER\tSIGNAL_RADIUS\tPATH", redLogFile);
      IJ.append("# NUCLEUS_NUMBER\tSIGNAL_AREA\tSIGNAL_ANGLE\tSIGNAL_FERET\tSIGNAL_DISTANCE\tFRACTIONAL_DISTANCE\tSIGNAL_PERIMETER\tSIGNAL_RADIUS\tPATH", greenLogFile);
      
      for(int i= 0; i<this.nucleiCollection.size();i++){ // for each roi

        Nucleus n = this.nucleiCollection.get(i);

        int nucleusNumber = n.getNucleusNumber();
        String path = n.getPath();

        ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
        signals.add(n.getRedSignals());
        signals.add(n.getGreenSignals());

        int signalCount = 0;
        for( ArrayList<NuclearSignal> signalGroup : signals ){

          String log = signalCount == 0 ? redLogFile : greenLogFile;
          
          if(signalGroup.size()>0){
            for(int j=0; j<signalGroup.size();j++){
               NuclearSignal s = signalGroup.get(j);
               IJ.append(nucleusNumber+"\t"+
                         s.getArea()+"\t"+
                         s.getAngle()+"\t"+
                         s.getFeret()+"\t"+
                         s.getDistance()+"\t"+
                         s.getFractionalDistance()+"\t"+
                         s.getPerimeter()+"\t"+
                         path, log);
            } // end for
          } // end if
          signalCount++;
        } // end for
      } // end for
    }

    public void drawRawPositionsFromTailChart(){

      Plot offsetRawPlot = new Plot("Raw corrected tail-centred plot", "Position", "Angle", Plot.Y_GRID | Plot.X_GRID);
      PlotWindow offsetRawPlotWindow;

      offsetRawPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
      offsetRawPlot.setYTicks(true);
      
      double minX = 0;
      double maxX = 0;
      for(int i=0;i<this.nucleiCollection.size();i++){
        double[] xRawCentredOnTail = this.nucleiCollection.get(i).createOffsetRawProfile();
        if(getMin(xRawCentredOnTail)<minX){
          minX = getMin(xRawCentredOnTail);
        }
        if(getMax(xRawCentredOnTail)>maxX){
          maxX = getMax(xRawCentredOnTail);
        }
      }
      offsetRawPlot.setLimits( (int) minX-1, (int) maxX+1,-50,360);
      offsetRawPlot.setColor(Color.BLACK);
      offsetRawPlot.drawLine((int) minX-1, 180, (int) maxX+1, 180); 
      offsetRawPlot.setColor(Color.LIGHT_GRAY);
     
      for(int i=0;i<this.nucleiCollection.size();i++){
        double[] xRawCentredOnTail = this.nucleiCollection.get(i).createOffsetRawProfile();
        double[] ypoints = this.nucleiCollection.get(i).getInteriorAngles();

        offsetRawPlot.setColor(Color.LIGHT_GRAY);
        offsetRawPlot.addPoints(xRawCentredOnTail, ypoints, Plot.LINE);
      }
      
      offsetRawPlot.draw();
      offsetRawPlotWindow = offsetRawPlot.show();
      offsetRawPlotWindow.noGridLines = true; // I have no idea why this makes the grid lines appear on work PC, when they appear by default at home
      offsetRawPlotWindow.drawPlot(offsetRawPlot);  
    }

    public void drawNormalisedPositionsFromTailChart(){
     
      for(int i=0;i<this.nucleiCollection.size();i++){
        double[] xpoints = this.nucleiCollection.get(i).getNormalisedXPositionsFromTip();
        double[] ypoints = this.nucleiCollection.get(i).getNormalisedYPositionsFromTail();
        normXFromTailPlot.addPoints(xpoints, ypoints, Plot.LINE);
      }
      normXFromTailPlot.draw();
      normXFromTailWindow = normXFromTailPlot.show();
      normXFromTailWindow.drawPlot(normXFromTailPlot);  
    }

    public void exportNuclearStats(String filename){
    
      String statsFile = this.folder+filename+"."+collectionType+".txt";
      File f = new File(statsFile);
      if(f.exists()){
        f.delete();
      }

      String outLine = "# AREA\tPERIMETER\tFERET\tPATH_LENGTH\tNORM_TAIL_INDEX\tDIFFERENCE\tFAILURE_CODE\tPATH\n";

      // IJ.append("# AREA\tPERIMETER\tFERET\tPATH_LENGTH\tNORM_TAIL_INDEX\tDIFFERENCE\tFAILURE_CODE\tPATH", statsFile);

      IJ.log("Exporting stats for "+this.getNucleusCount()+" nuclei");
      double[] areas  = this.getAreas();
      double[] perims = this.getPerimeters();
      double[] ferets = this.getFerets();
      double[] pathLengths  = this.getPathLengths();
      int[] tails = this.getTailIndexes();
      double[] differences= this.getDifferencesToMedian();
      String[] paths = this.getNucleusPaths();


      for(int i=0; i<this.getNucleusCount();i++){
      	int j = i+1;
        IJ.log("  "+j+" of "+this.getNucleusCount());

        outLine = outLine + areas[i]+"\t"+
                            perims[i]+"\t"+
                            ferets[i]+"\t"+
                            pathLengths[i]+"\t"+
                            tails[i]+"\t"+
                            differences[i]+"\t"+
                            this.nucleiCollection.get(i).failureCode+"\t"+
                            paths[i]+"\n";

        // IJ.append(  areas[i]+"\t"+
        //             perims[i]+"\t"+
        //             ferets[i]+"\t"+
        //             pathLengths[i]+"\t"+
        //             tails[i]+"\t"+
        //             differences[i]+"\t"+
        //             this.nucleiCollection.get(i).failureCode+"\t"+
        //             paths[i], statsFile);

        // Include tip, CoM, tail
    		this.nucleiCollection.get(i).printLogFile(nucleiCollection.get(i).getPathWithoutExtension()+"\\"+nucleiCollection.get(i).getNucleusNumber()+".log");
      }
      IJ.append(  outLine, statsFile);
      IJ.log("Export complete");
    }

    public void rotateAndAssembleNucleiForExport(String filename){

      // foreach nucleus
      // createProcessor (500, 500)
      // sertBackgroundValue(0)
      // paste in old image at centre
      // insert(ImageProcessor ip, int xloc, int yloc)
      // rotate about CoM (new position)
      // display.
      IJ.log("Creating composite image...");
      

      int totalWidth = 0;
      int totalHeight = 0;

      int boxWidth = (int)(getMedianNuclearPerimeter()/1.4);
      int boxHeight = (int)(getMedianNuclearPerimeter()/1.2);

      int maxBoxWidth = boxWidth * 5;
      int maxBoxHeight = (boxHeight * (int)(Math.ceil(this.getNucleusCount()/5)) + boxHeight );

      ImagePlus finalImage = new ImagePlus("Final image", new BufferedImage(maxBoxWidth, maxBoxHeight, BufferedImage.TYPE_INT_RGB));
      ImageProcessor finalProcessor = finalImage.getProcessor();
      finalProcessor.setBackgroundValue(0);

      for(int i=0; i<this.getNucleusCount();i++){
        
        Nucleus n = this.nucleiCollection.get(i);
        String path = n.getPathWithoutExtension()+"\\"+n.getNucleusNumber()+".tiff";

        try {
          Opener localOpener = new Opener();
          ImagePlus image = localOpener.openImage(path);
          ImageProcessor ip = image.getProcessor();
          int width = ip.getWidth();
          int height = ip.getHeight();
          ip.setRoi(n.getRoi());


          ImageProcessor newProcessor = ip.createProcessor(boxWidth, boxHeight);

          newProcessor.setBackgroundValue(0);
          newProcessor.insert(ip, (int)boxWidth/4, (int)boxWidth/4); // put the original halfway in
          newProcessor.setInterpolationMethod(ImageProcessor.BICUBIC);
          newProcessor.rotate( n.findRotationAngle() );
          newProcessor.setBackgroundValue(0);

          if(totalWidth>maxBoxWidth-boxWidth){
          	totalWidth=0;
          	totalHeight+=(int)(boxHeight);
          }
          int newX = totalWidth;
          int newY = totalHeight;
          totalWidth+=(int)(boxWidth);
          
          finalProcessor.insert(newProcessor, newX, newY);
          TextRoi label = new TextRoi(newX, newY, n.getImageName()+"-"+n.getNucleusNumber());
          Overlay overlay = new Overlay(label);
          finalProcessor.drawOverlay(overlay);  
        } catch(Exception e){
          IJ.log("Error adding image to composite");
          IJ.append("Error adding image to composite: "+e, debugFile);
          IJ.append("  "+collectionType, debugFile);
          IJ.append("  "+path, debugFile);
        }     
      }
    	finalImage.show();
    	IJ.saveAsTiff(finalImage, folder+filename+"."+collectionType+".tiff");
    	IJ.log("Composite image created");
    }

    /*
      Draw the features of interest on the images of the nuclei created earlier
    */
    public void annotateImagesOfNuclei(){
    	IJ.log("Annotating images...");
    	for(int i=0; i<this.getNucleusCount();i++){
        int m = i+1;
    		IJ.log("  "+m+" of "+this.getNucleusCount());
    		Nucleus n = this.nucleiCollection.get(i);

        String path = n.getPathWithoutExtension()+"\\"+n.getNucleusNumber()+".tiff";
        String outPath = n.getPathWithoutExtension()+"\\"+n.getNucleusNumber()+"."+collectionType+".tiff";

        try{

      		// open the image we saved earlier
      		Opener localOpener = new Opener();
          ImagePlus image = localOpener.openImage(path);
          ImageProcessor ip = image.getProcessor();

          // draw the features of interest
          
          // draw the outline of the nucleus
  		    ip.setColor(Color.BLUE);
  		    ip.setLineWidth(1);
  		    ip.draw(n.getRoi());


  		    // draw the CoM
  		    ip.setColor(Color.MAGENTA);
  		    ip.setLineWidth(5);
  		    ip.drawDot(n.getCentreOfMass().getXAsInt(),  n.getCentreOfMass().getYAsInt());

  		    //draw the sperm tip 
  		    ip.setLineWidth(5);
  		    ip.setColor(Color.YELLOW);
  		    ip.drawDot(n.getSpermTip().getXAsInt(), n.getSpermTip().getYAsInt());

  		    // draw the points considered as sperm tails on a per-nucleus basis
  		    ip.setLineWidth(3);
  		    ip.setColor(Color.GRAY);
  		    for(int j=0; j<n.intialSpermTails.size();j++){
  		    	NucleusBorderPoint p = n.intialSpermTails.get(j);
  		    	ip.drawDot(p.getXAsInt(), p.getYAsInt());
  		    }

  		    // Draw the original consensus tail
  		    ip.setLineWidth(5);
  		    ip.setColor(Color.CYAN);
  		    ip.drawDot(n.getInitialConsensusTail().getXAsInt(), n.getInitialConsensusTail().getYAsInt());

  				// line from tail to intsersection point; should pass through CoM   
          if(n.intersectionPoint!=null){ // handle failed nuclei in which this analysis was not performed
    				ip.setLineWidth(1);
    				ip.setColor(Color.YELLOW);
    		    ip.drawLine(n.getSpermTail().getXAsInt(), n.getSpermTail().getYAsInt(), n.intersectionPoint.getXAsInt(), n.intersectionPoint.getYAsInt());
          }

          // The narrowest part of the sperm head
          ip.setLineWidth(1);
          ip.setColor(Color.MAGENTA);
          ip.drawLine(n.minFeretPoint1.getXAsInt(), n.minFeretPoint1.getYAsInt(), n.minFeretPoint2.getXAsInt(), n.minFeretPoint2.getYAsInt());
          ip.setLineWidth(3);
          ip.drawDot(n.minFeretPoint1.getXAsInt(), n.minFeretPoint1.getYAsInt());
          
  		    //   SIGNALS
  		    ip.setLineWidth(3);
  		    ip.setColor(Color.RED);
  		    ArrayList<NuclearSignal> redSignals = n.getRedSignals();
          if(redSignals.size()>0){
            for(int j=0; j<redSignals.size();j++){
              NuclearSignal s = redSignals.get(j);
              ip.setLineWidth(3);
              ip.drawDot(s.getCentreOfMass().getXAsInt(), s.getCentreOfMass().getYAsInt());
              ip.setLineWidth(1);
              ip.draw(s.getRoi());
            }

          }
          ip.setColor(Color.GREEN);
          ArrayList<NuclearSignal> greenSignals = n.getGreenSignals();
          if(redSignals.size()>0){
            for(int j=0; j<greenSignals.size();j++){
              NuclearSignal s = greenSignals.get(j);
              ip.setLineWidth(3);
              ip.drawDot(s.getCentreOfMass().getXAsInt(), s.getCentreOfMass().getYAsInt());
              ip.setLineWidth(1);
              ip.draw(s.getRoi());
            }
          }
  		    IJ.saveAsTiff(image, outPath);
  		    image.close();

        } catch(Exception e){
          IJ.log("Error annotating nucleus: "+e);
          IJ.append("Error annotating nucleus: "+e, debugFile);
          IJ.append("  "+collectionType, debugFile);
          IJ.append("  "+path, debugFile);
          IJ.append("  "+outPath, debugFile);
        }

    	}
    	 IJ.log("Annotation complete");
    }

    /*
			Interpolate the median profile to match the length of the most-median nucleus
			Store the angle profile as a double[] to feed into the curve refolder
    */
		public double[] getMedianTargetCurve(Nucleus n){
			double[] targetMedianCurve = interpolateMedianToLength(n.smoothLength);
			return targetMedianCurve;
		}	

  }

 /*
	-----------------------
    CURVE REFOLDER CLASS
    -----------------------
    Contains the code for taking a profile, and an ideal profile, and
		making the profile fit
 */
	class CurveRefolder {

		private double[] targetCurve;
		private double[] initialCurve;

		private Nucleus initialNucleus;
		private Nucleus targetNucleus;

		private Plot nucleusPlot;
		private PlotWindow nucleusPlotWindow;

		private Plot anglePlot;
		private PlotWindow anglePlotWindow;

		public CurveRefolder(double[] target, Nucleus n){
			this.targetCurve = target;
			this.initialNucleus = n;
			this.initialCurve = n.getAngleProfileArray();
		}

		/*
			The main function to be called externally;
			all other functions will hang off this
		*/
		public void refoldCurve(){

			this.moveCoMtoZero();
			this.preparePlots();

			double score = compareProfiles(targetCurve, initialCurve);
			
			IJ.log("Refolding curve: initial score: "+(int)score);

			double prevScore = score*2;
			int i=0;
			while(prevScore - score >0.0001 || i<100){
				prevScore = score;
				score = this.iterateOverNucleus();
				// IJ.log("Iteration "+i+": "+score);
				i++;
			}
			IJ.log("Refolded curve: final score: "+(int)score);
			// this.plotTargetNucleus();
			// return targetNucleus;
		}

		/*
			Translate the XY coordinates of each point so that
			the nuclear centre of mass is at 0,0.
			Then set the target nucleus as a copy.
		*/
		private void moveCoMtoZero(){

			XYPoint centreOfMass = initialNucleus.getCentreOfMass();
			double xOffset = centreOfMass.getX();
			double yOffset = centreOfMass.getY();

			initialNucleus.setCentreOfMass(new XYPoint(0,0));

			FloatPolygon offsetPolygon = new FloatPolygon();

			for(int i=0; i<initialNucleus.smoothLength; i++){
				NucleusBorderPoint p = initialNucleus.getBorderPointArray()[i];

				double x = p.getX() - xOffset;
				double y = p.getY() - yOffset;
				offsetPolygon.addPoint(x, y);

				initialNucleus.getBorderPointArray()[i].setX( x );
				initialNucleus.getBorderPointArray()[i].setY( y );
				
			}
			initialNucleus.smoothedPolygon = offsetPolygon;

			this.targetNucleus = initialNucleus;
		}

		/*
			Create the plots that will be needed to display the 
			intiial and target nuclear shapes, plus the angle profiles
		*/
		private void preparePlots(){

			double[] xPoints = new double[initialNucleus.smoothLength];
			double[] yPoints = new double[initialNucleus.smoothLength];
			double[] aPoints = new double[initialNucleus.smoothLength]; // angles
			double[] pPoints = new double[initialNucleus.smoothLength]; // positions along array

			for(int i=0; i<targetNucleus.smoothLength; i++){
				NucleusBorderPoint p = targetNucleus.getBorderPointArray()[i];
				xPoints[i] = p.getX();
				yPoints[i] = p.getY();
				aPoints[i] = targetCurve[i];
				pPoints[i] = i;
			}
			
			nucleusPlot = new Plot( "Nucleus shape",
                                  "",
                                  "");
    	
    	// get the limits  for the plot  	
			double minX = targetNucleus.getMinX();
	    double maxX = targetNucleus.getMaxX();
	    double minY = targetNucleus.getMinY();
	    double maxY = targetNucleus.getMaxY();

	    // ensure that the scales for each axis are the same
	    double min = Math.min(minX, minY);
	    double max = Math.max(maxX, maxY);

	    // ensure there is room for expansion of the target nucleus
	    min = Math.floor(min - Math.abs(min));
	    max = Math.ceil(max * 2);

	    nucleusPlot.setLimits(min, Math.abs(min), min, Math.abs(min));

	    nucleusPlot.setSize(400,400);
	    nucleusPlot.setYTicks(true);
      nucleusPlot.setColor(Color.LIGHT_GRAY);
	    nucleusPlot.drawLine(min, 0, Math.abs(min), 0);
	    nucleusPlot.drawLine(0, min, 0, Math.abs(min));

	    anglePlot = new Plot( "Angles",
                            "Position",
                            "Angle");

	    anglePlot.setLimits(0,targetCurve.length,-50,360);
	    anglePlot.setSize(300,300);
	    anglePlot.setYTicks(true);

	  	//   nucleusPlot.setColor(Color.LIGHT_GRAY);
			// nucleusPlot.addPoints(xPoints, yPoints, Plot.LINE);
			// anglePlot.setColor(Color.LIGHT_GRAY);
			// anglePlot.addPoints(pPoints, aPoints, Plot.LINE);
			nucleusPlotWindow = nucleusPlot.show();
			// anglePlotWindow = anglePlot.show();
		}

		/*
			Draw the current state of the target nucleus
		*/
		private void plotTargetNucleus(){

			double[] xPoints = new double[targetNucleus.smoothLength+1];
			double[] yPoints = new double[targetNucleus.smoothLength+1];
			// double[] aPoints = new double[targetNucleus.smoothLength+1]; // angles
			// double[] pPoints = new double[targetNucleus.smoothLength+1]; // positions along array

			for(int i=0; i<targetNucleus.smoothLength; i++){
				NucleusBorderPoint p = targetNucleus.getBorderPointArray()[i];
				xPoints[i] = p.getX();
				yPoints[i] = p.getY();
				// aPoints[i] = p.getInteriorAngle();
				// pPoints[i] = i;
			}

      // ensure nucleus outline joins up at tip
      NucleusBorderPoint p = targetNucleus.getBorderPointArray()[0];
      xPoints[targetNucleus.smoothLength] = p.getX();
      yPoints[targetNucleus.smoothLength] = p.getY();

			nucleusPlot.setColor(Color.DARK_GRAY);
			nucleusPlot.addPoints(xPoints, yPoints, Plot.LINE);
			// anglePlot.setColor(Color.DARK_GRAY);
			// anglePlot.addPoints(pPoints, aPoints, Plot.LINE);
			nucleusPlotWindow.drawPlot(nucleusPlot);
			// anglePlotWindow.drawPlot(anglePlot);
		}

		/*
			Go over the target nucleus, adjusting each point.
			Keep the change if it helps get closer to the target profile
		*/
		private double iterateOverNucleus(){

			double similarityScore = compareProfiles(targetCurve, targetNucleus.getAngleProfileArray());
			
			for(int i=0; i<targetNucleus.smoothLength; i++){

				NucleusBorderPoint p = targetNucleus.getBorderPointArray()[i];
	    		
    		double currentDistance = p.getLengthTo(new XYPoint(0,0));
    		double newDistance = currentDistance; // default no change

    		double oldX = p.getX();
    		double oldY = p.getY();

    		// make change dependent on score
    		double amountToChange = Math.random() * (similarityScore/1000); // when score is 1000, change by up to 1. When score is 300, change byup to 0.33

    		if(p.getInteriorAngle() > targetCurve[i]){
    					newDistance = currentDistance + amountToChange; 
    		}
    		if(p.getInteriorAngle() < targetCurve[i]){
    					newDistance = currentDistance - amountToChange; //  some change between 0 and 2
    		}

    		// find the angle the point makes to the x axis
    		double angle = findAngleBetweenXYPoints(p, new XYPoint(0,0), new XYPoint(10, 0)); // point, 10,0, p,0
    		if(oldY<0){
    			angle = 360-angle;
    		}
    		double newX = getXComponentOfAngle(newDistance, angle);
				double newY = getYComponentOfAngle(newDistance, angle);

				// IJ.log("Old: X:"+(int)oldX+" Y:"+(int)oldY+" Distance: "+(int)currentDistance+" Angle: "+(int)angle);
				// IJ.log("New: X:"+(int)newX+" Y:"+(int)newY+" Distance: "+(int)newDistance+" Angle: "+(int)angle);

				p.setX(newX); // the new x position
				p.setY(newY); // the new y position

				// ensure the interior angle calculation works with the current points
				targetNucleus.smoothedPolygon = createPolygon(); 

				// measure the new profile & compare
				targetNucleus.angleProfile.updateAngleCalculations();
				double[] newProfile = targetNucleus.getAngleProfileArray();
				double score = compareProfiles(targetCurve, newProfile);

				// IJ.log("Score: "+score);
				// do not apply change  if the distance from teh surrounding points changes too much
				double distanceToPrev = p.getLengthTo( targetNucleus.getBorderPointArray()[ wrapIndex(i-1, targetNucleus.smoothLength) ] );
				double distanceToNext = p.getLengthTo( targetNucleus.getBorderPointArray()[ wrapIndex(i+1, targetNucleus.smoothLength) ] );

				// reset if worse fit or distances are too high
				if(score > similarityScore  || distanceToNext > 1.2 || distanceToPrev > 1.2 ){
					p.setX(oldX);
					p.setY(oldY);
					targetNucleus.angleProfile.updateAngleCalculations();
					targetNucleus.smoothedPolygon = createPolygon();
					// IJ.log("Rejecting change");
				} else {
					similarityScore = score;
					// IJ.log("Keeping change");
				}
			}
			return similarityScore;
		}

		private FloatPolygon createPolygon(){
			FloatPolygon offsetPolygon = new FloatPolygon();

			for(int i=0; i<targetNucleus.smoothLength; i++){

				NucleusBorderPoint p = targetNucleus.getBorderPointArray()[i];
				double x = p.getX();
				double y = p.getY();
				offsetPolygon.addPoint(x, y);
	    }
	    return offsetPolygon;
		}

		/*
			Find the total difference between two angle profiles
		*/
		private double compareProfiles(double[] profile1, double[] profile2){

			double d = 0;
			for(int i=0; i<profile1.length; i++){
				d += Math.abs(profile1[i] - profile2[i]);
			}
			return d;
		}

		/*
      This is used only for the consensus image.
      The consenus nucleus needs to be oriented with
      the tail at the bottom. Assumes CoM is at 0,0
    */
    private void putTailAtBottom(){

    	// find the angle to rotate
    	double angleToRotate = 0;
    	double distanceFromZero = 180;

    	// get the angle from the tail to the vertical axis line
    	double tailAngle = findAngleBetweenXYPoints( targetNucleus.getSpermTail(), targetNucleus.getCentreOfMass(), new XYPoint(0,-10));
  		if(targetNucleus.getSpermTail().getX()<0){
  			tailAngle = 360-tailAngle; // correct for measuring the smallest angle
  		}

    	for(int i=0;i<360;i++){

    		// get a copy of the sperm tail
    		NucleusBorderPoint p = new NucleusBorderPoint( targetNucleus.getSpermTail().getX(), targetNucleus.getSpermTail().getY() );
    		
    		// get the distance from tail to CoM
    		double distance = p.getLengthTo(targetNucleus.getCentreOfMass());

    		// add the rotation amount
    		double newAngle = tailAngle + i;

    		double newX = getXComponentOfAngle(distance, newAngle);
				double newY = getYComponentOfAngle(distance, newAngle);

				if(Math.abs(newX) < distanceFromZero && newY < 0){
					angleToRotate = i;
					distanceFromZero = Math.abs(newX);
				}
    	}

    	// if(targetNucleus.getSpermTail().getX()<0){
    	// 	angleToRotate = 360-angleToRotate;
    	// }
    	IJ.log("Rotating by "+(int)angleToRotate);

    	for(int i=0;i<targetNucleus.smoothLength;i++){

    		NucleusBorderPoint p = targetNucleus.getBorderPointArray()[i];
    		double distance = p.getLengthTo(targetNucleus.getCentreOfMass());
    		double oldAngle = findAngleBetweenXYPoints( p, targetNucleus.getCentreOfMass(), new XYPoint(0,-10));
    		if(p.getX()<0){
    			oldAngle = 360-oldAngle;
    		}

    		double newAngle = oldAngle + angleToRotate;
    		double newX = getXComponentOfAngle(distance, newAngle);
				double newY = getYComponentOfAngle(distance, newAngle);

				p.setX(newX); // the new x position
				p.setY(newY); // the new y position
    	}

    	// also flip if tip X is >0
    	if(targetNucleus.getSpermTip().getX() > 0){
    		IJ.log("  Flipping");
    		targetNucleus.flipXAroundPoint(targetNucleus.getCentreOfMass());
    	}

    	plotTargetNucleus();
    }

    private void exportImage(NucleusCollection collection){
    	ImagePlus plot = nucleusPlot.getImagePlus();
      IJ.saveAsTiff(plot, targetNucleus.getDirectory()+"\\plotConsensus."+collection.collectionType+".tiff");

      targetNucleus.setPath(targetNucleus.getDirectory()+"\\logConsensusNucleus."+collection.collectionType+".txt");
      IJ.log("Exporting to: "+targetNucleus.getPath());
      targetNucleus.printLogFile(targetNucleus.getPath());
    }

    /*
      Using a list of signal locations, draw on
      the consensus plot.
    */
    public void addSignalsToConsensus(NucleusCollection collection){

    	for(int i= 0; i<collection.nucleiCollection.size();i++){ // for each roi

        Nucleus n = collection.nucleiCollection.get(i);

        ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
        signals.add(n.getRedSignals());
        signals.add(n.getGreenSignals());

        int signalCount = 0;
        for( ArrayList<NuclearSignal> signalGroup : signals ){

          if(signalGroup.size()>0){

            ArrayList<Double> xPoints = new ArrayList<Double>(0);
            ArrayList<Double> yPoints = new ArrayList<Double>(0);
    
            for(int j=0; j<signalGroup.size();j++){

            	double angle = signalGroup.get(j).getAngle();
            	double fractionalDistance = signalGroup.get(j).getFractionalDistance();

            	// determine the total distance to the border at this angle
            	double distanceToBorder = getDistanceFromAngle(angle);

            	// convert to fractional distance to signal
            	double signalDistance = distanceToBorder * fractionalDistance;
              
              // adjust X and Y because we are now counting angles from the vertical axis
            	double signalX = getXComponentOfAngle(signalDistance, angle-90);
            	double signalY = getYComponentOfAngle(signalDistance, angle-90);

              // add to array
              xPoints.add( signalX );
              yPoints.add( signalY );
             // IJ.log("Signal "+j+": Fdist: "+fractionalDistance+" Dist: "+signalDistance+" X: "+signalX+" Y: "+signalY);
              
            }
            if(signalCount==0)
              nucleusPlot.setColor(Color.RED);
            else
              nucleusPlot.setColor(Color.GREEN);

            nucleusPlot.setLineWidth(2);
            nucleusPlot.addPoints(xPoints, yPoints, Plot.DOT);
          }
          signalCount++;
        }
      }
      nucleusPlotWindow.drawPlot(nucleusPlot);

    }

    private double getDistanceFromAngle(double angle){

    	// go through the nucleus outline
    	// measure the angle to the tail and the distance to the CoM
    	// if closest to target angle, return distance
    	double bestAngle = 180;
    	double bestDiff = 180;
    	double bestDistance = 180;

    	for(int i=0;i<targetNucleus.smoothLength;i++){
    		NucleusBorderPoint p = targetNucleus.getBorderPointArray()[i];
    		double distance = p.getLengthTo(targetNucleus.getCentreOfMass());
    		double pAngle = findAngleBetweenXYPoints( p, targetNucleus.getCentreOfMass(), new XYPoint(0,-10));
    		if(p.getX()<0){
    			pAngle = 360-pAngle;
    		}

    		if(Math.abs(angle-pAngle) < bestDiff){
    			bestAngle = pAngle;
    			bestDiff = Math.abs(angle-pAngle);
    			bestDistance = distance;
    		}
    	}
    	// IJ.log("Target angle: "+angle+": Best angel: "+bestAngle+" Distance: "+bestDistance);
    	return bestDistance;
    }

	}

}
