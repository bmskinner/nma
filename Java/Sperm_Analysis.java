/*
-------------------------------------------------
SPERM CARTOGRAPHY IMAGEJ PLUGIN
-------------------------------------------------
Copyright (C) Ben Skinner 2015

This plugin allows for automated detection of FISH
signals in a mouse sperm nucleus, and measurement of
the signal position relative to the nuclear centre of
mass and sperm tip. Works with both red and green channels.
It also generates a profile of the nuclear shape, allowing
morphology comparisons

  ---------------
  FEATURES TO ADD
  ---------------
    Median curve refolding & consensus image
      Find nucleus closest to the median curve as template as alternative to refolding
    Get measure of consistency in tail predictions
    Add distance across CoM profile for comparisons
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

public class Sperm_Analysis
  extends ImagePlus
  implements PlugIn
{

  private static final String[] fileTypes = {".tif", ".tiff", ".jpg"};

  // Values for deciding whether an object is a signal
  private static final int SIGNAL_THRESHOLD = 70;
  private static final double MIN_SIGNAL_SIZE = 50; // how small can a signal be
  private static final double MAX_SIGNAL_SIZE = 2000; // how large can a signal be
  
  private static final double ANGLE_THRESHOLD = 40.0; // when calculating local minima, ignore angles above this

  /* VALUES FOR DECIDING IF AN OBJECT IS A NUCLEUS */
  private static final int NUCLEUS_THRESHOLD = 36;
  private static final double MIN_NUCLEAR_SIZE = 500;
  private static final double MAX_NUCLEAR_SIZE = 7000;
  private static final double MIN_NUCLEAR_CIRC = 0.3;
  private static final double MAX_NUCLEAR_CIRC = 0.8;
  private static final double PROFILE_INCREMENT = 0.5;

  // failure codes - not in use, keep to add back to logFailed in refilter
  private static final int FAILURE_TIP       = 1;
  private static final int FAILURE_TAIL      = 2;
  private static final int FAILURE_THRESHOLD = 4;
  private static final int FAILURE_FERET     = 8;
  private static final int FAILURE_ARRAY     = 16;
  private static final int FAILURE_AREA      = 32;
  private static final int FAILURE_PERIM     = 64;
  private static final int FAILURE_OTHER     = 128;

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
    
  public void run(String paramString)  {

    DirectoryChooser localOpenDialog = new DirectoryChooser("Select directory of images...");
    String folderName = localOpenDialog.getDirectory();

    prepareLogFiles(folderName);

    IJ.showStatus("Opening directory: " + folderName);
    IJ.log("Directory: "+folderName);

    File folder = new File(folderName);
    File[] listOfFiles = folder.listFiles();
 
    completeCollection = new NucleusCollection(folderName);
    failedNuclei       = new NucleusCollection(folderName);

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

    completeCollection.refilterNuclei(); // remove double nuclei, blobs, nuclei too wibbly
  
    completeCollection.createProfileAggregate();
    completeCollection.drawProfilePlots();

    completeCollection.calculateNormalisedMedianLine();
    completeCollection.findTailIndexInMedianCurve();
    completeCollection.calculateOffsets();

    completeCollection.refilterNuclei(); // remove any nuclei that are odd shapes
    failedNuclei.exportNuclearStats("logFailed.txt");
    failedNuclei.annotateImagesOfNuclei();
    failedNuclei.rotateAndAssembleNucleiForExport("compositeFailed.tiff");

    completeCollection.drawRawPositionsFromTailChart();
    completeCollection.createNormalisedTailPositions();
    completeCollection.drawNormalisedPositionsFromTailChart();
    completeCollection.createTailCentredProfileAggregate();
    completeCollection.calculateTailCentredNormalisedMedianLine();
    completeCollection.measureNuclearOrganisation();
    completeCollection.exportNuclearStats("logStats.txt");
    completeCollection.annotateImagesOfNuclei();
    completeCollection.rotateAndAssembleNucleiForExport("composite.tiff");
    
  }

  public int wrapIndex(int i, int length){
    if(i<0)
      i = length + i; // if i = -1, in a 200 length array,  will return 200-1 = 199
    if(Math.floor(i / length)>0)
      i = i - ( ((int)Math.floor(i / length) )*length);

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

    // this.failedFile = folderName+"logFailed.txt";
    // File g = new File(failedFile);
    // if(g.exists()){
    //   g.delete();
    // }

    // IJ.append("# CAUSE_OF_FAILURE\tPERIMETER\tAREA\tFERET\tPATH_LENGTH\tNORM_TAIL_INDEX\tRAW_TAIL_INDEX\tPATH", this.failedFile);

    this.debugFile = folderName+"logDebug.txt";
    File h = new File(logFile);
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

  /*
    Make a directory with the same name as the image being analysed
  */
  public String createImageDirectory(String path){
    File dir = new File(path);
    
    if (!dir.exists()) {
      try{
        dir.mkdir();
        IJ.log("    Dir created");
      } catch(Exception e) {
        IJ.log("Failed to create dir: "+e);
        IJ.log("Saving to: "+dir.toString());
      }
    }
    return dir.toString();
  }

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
    Nucleus currentNucleus = new Nucleus(nucleus);
    currentNucleus.setPath(path);
    currentNucleus.setNucleusNumber(nucleusNumber);

    // immediately save out a picture of the nucleus for later annotation
    String saveFolder = createImageDirectory(currentNucleus.getPathWithoutExtension());
    IJ.saveAsTiff(smallRegion, saveFolder+"\\"+currentNucleus.getNucleusNumber()+".tiff");


    // measure CoM, area, perimeter and feret in blue
    ResultsTable blueResults = findNuclearMeasurements(smallRegion, nucleus);
    XYPoint nucleusCoM = new XYPoint(blueResults.getValue("XM", 0),  blueResults.getValue("YM", 0) );
    currentNucleus.setCentreOfMass(nucleusCoM);
    currentNucleus.setPerimeter(blueResults.getValue("Perim.",0));
    currentNucleus.setArea(blueResults.getValue("Area",0));
    currentNucleus.setFeret(blueResults.getValue("Feret",0));
    

    // find tip - use the least angle method
    XYPoint spermTip = currentNucleus.findMinimumAngle();
    if(spermTip.getInteriorAngle() > 110){ // this is not a deep enough curve to declare the tip
        IJ.log("    Cannot reliably assign tip position");
        currentNucleus.failureCode  = currentNucleus.failureCode | FAILURE_TIP;
        this.nucleiFailedOnTip++;
        nucleusPassedChecks = false;
    }
    currentNucleus.moveIndexToArrayStart(spermTip.getIndex());
    currentNucleus.tipIndex = 0;
    currentNucleus.setSpermTip(spermTip);


    // decide if the profile is right or left handed; flip if needed
    if(!currentNucleus.isProfileOrientationOK()){
      IJ.log("    Reversing array");
      currentNucleus.reverseArray();
    }
    

    // find local minima and maxima
    XYPoint[] minima = currentNucleus.getLocalMinima();
    XYPoint[] maxima = currentNucleus.getLocalMaxima();

    
    /*
      Find the tail point using multiple independent methods. 
      Find a consensus point

    	Method 1: Use the list of local minima to detect the tail corner
    						This is the corner furthest from the tip.
    						Can be confused as to which side of the sperm head is chosen
    */	
    XYPoint spermTail2 = findTailPointFromMinima(spermTip, nucleusCoM, minima);
    currentNucleus.addTailEstimatePosition(spermTail2);

    /*
    	Method 2: Look at the 2nd derivative - rate of change of angles
    						Perform a 5win average smoothing of the deltas
    						Count the number of consecutive >1 degree blocks
    						Wide block far from tip = tail
    */	
    XYPoint spermTail3 = currentNucleus.findTailFromDeltas(spermTip);
    currentNucleus.addTailEstimatePosition(spermTail3);

    /*    
      Method 3: Find the narrowest diameter around the nuclear CoM
                Draw a line orthogonal, and pick the intersecting border points
                The border furthest from the tip is the tail
    */  
    XYPoint spermTail1 = currentNucleus.findTailByNarrowestWidthMethod();
    currentNucleus.addTailEstimatePosition(spermTail1);


    /*
      Given distinct methods for finding a tail,
      take a position between them on roi
    */
    int consensusTailIndex = currentNucleus.getPositionBetween(spermTail2, spermTail3);
    XYPoint consensusTail = currentNucleus.smoothedArray[consensusTailIndex];
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
        
        IJ.append(normalisedX+"\t"+
        					currentNucleus.smoothedArray[i].getInteriorAngle()+"\t"+
        					rawXFromTail, this.logFile);        

        // calculate the path length
        XYPoint thisPoint = new XYPoint(normalisedX,currentNucleus.smoothedArray[i].getInteriorAngle());
        pathLength += thisPoint.getLengthTo(prevPoint);
        prevPoint = thisPoint;
    }

    IJ.append("", this.logFile);

    // if(spermTail2.getLengthTo(spermTail3) < nucleus.getFeretsDiameter() * 0.2){ // warn if the tail points are together  
    //   // IJ.log("    Difficulty assigning tail position");
    // }  

    // find the signals
    // within nuclear roi, analyze particles in colour channels
    RoiManager   redSignalManager = findSignalInNucleus(smallRegion, 0);
    RoiManager greenSignalManager = findSignalInNucleus(smallRegion, 1);

    Roi[] redSignals =     redSignalManager.getSelectedRoisAsArray();
    Roi[] greenSignals = greenSignalManager.getSelectedRoisAsArray();

    for(Roi roi : redSignals){

      ResultsTable redResults = findSignalMeasurements(smallRegion, roi, 1);
      XYPoint signalCoM = new XYPoint(redResults.getValue("XM", 0),  redResults.getValue("YM", 0) );
      currentNucleus.addRedSignal( new NuclearSignal( roi, 
                                                redResults.getValue("Area",0), 
                                                redResults.getValue("Feret",0), 
                                                redResults.getValue("Perim.",0), 
                                                signalCoM));
    }

    // Add green signals to the nucleus
    for(Roi roi : greenSignals){

      ResultsTable greenResults = findSignalMeasurements(smallRegion, roi, 1);
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

    } else {
      this.failedNuclei.addNucleus(currentNucleus);
    }
  }

  /*
    Detect the tail based on a list of local minima in an XYPoint array.
    The putative tail is the point furthest from the sum of the distances from the CoM and the tip
  */
  public XYPoint findTailPointFromMinima(XYPoint tip, XYPoint centre, XYPoint[] array){
  
    // we cannot be sure that the greatest distance between two points will be the endpoints
    // because the hook may begin to curve back on itself. We supplement this basic distance with
    // the distances of each point from the centre of mass. The points with the combined greatest
    // distance are both far from each other and far from the centre, and are a more robust estimate
    // of the true ends of the signal
    
    double tipToCoMDistance = tip.getLengthTo(centre);

    double maxDistance = 0;
    XYPoint tail = tip;

    for(XYPoint a : array){
            
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
    Detect a poiint in an XYPoint array furthest from a given point.
  */
  public XYPoint findPointFurthestFrom(XYPoint p, XYPoint[] list){

    double maxL = 0;
    XYPoint result = p;
    for (XYPoint a : list){
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
    -----------------------
    XY POINT CLASS
    -----------------------
    This class contains the X and Y coordinates of a point as doubles,
    plus any angles calculated for that point. 
    Also contains methods for determining distance and overlap with other points
  */
  class XYPoint {
    private double x;
    private double y;
    private double minAngle;
    private double interiorAngle; // depends on whether the min angle is inside or outside the shape
    private double interiorAngleDelta; // this will hold the difference between a previous interiorAngle and a next interiorAngle
    private double interiorAngleDeltaSmoothed; // holds delta from a 5-window average centred on this point

    private int index; // keep the original index position in case we need to change
    private int numberOfConsecutiveBlocks; // holds the number of interiorAngleDeltaSmootheds > 1 degree after this point
    private int blockNumber; // identifies the group of consecutive blocks this point is part of
    private int blockSize; // the total number of points within the block
    private int positionWithinBlock; // stores the place within the block starting at 0

    private boolean localMin; // is this angle a local minimum based on the minAngle
    private boolean localMax; // is this angle a local maximum based on the interior angle
    private boolean isMidpoint; // is this point the midpoint of a block
  
    public XYPoint (double x, double y){
      this.x = x;
      this.y = y;
    }

    public double getX(){
      return this.x;
    }
    public double getY(){
      return this.y;
    }

    public int getXAsInt(){
      Double obj = new Double(this.x);
      int i = obj.intValue();
      return i;
    }

    public int getYAsInt(){
      Double obj = new Double(this.y);
      int i = obj.intValue();
      return i;
    }

    public void setX(double x){
      this.x = x;
    }

    public void setY(double y){
      this.y = y;
    }

    public int getIndex(){
      return this.index;
    }

    public double getMinAngle(){
      return this.minAngle;
    }

    public void setIndex(int i){
      this.index = i;
    }

    public void setMinAngle(double d){
      this.minAngle = d;
    }

    public double getInteriorAngle(){
      return this.interiorAngle;
    }

    public void setInteriorAngle(double d){
      this.interiorAngle = d;
    }

     public double getInteriorAngleDelta(){
      return this.interiorAngleDelta;
    }

    public void setInteriorAngleDelta(double d){
      this.interiorAngleDelta = d;
    }

    public double getInteriorAngleDeltaSmoothed(){
      return this.interiorAngleDeltaSmoothed;
    }

    public void setInteriorAngleDeltaSmoothed(double d){
      this.interiorAngleDeltaSmoothed = d;
    }

    public int getConsecutiveBlocks(){
      return this.numberOfConsecutiveBlocks;
    }

    public void setConsecutiveBlocks(int i){
      this.numberOfConsecutiveBlocks = i;
    }

    public int getBlockNumber(){
      return this.blockNumber;
    }

    public void setBlockNumber(int i){
      this.blockNumber = i;
    }

    public int getBlockSize(){
      return this.blockSize;
    }

    public void setBlockSize(int i){
      this.blockSize = i;
    }

    public int getPositionWithinBlock(){
      return this.positionWithinBlock;
    }

    public void setPositionWithinBlock(int i){
      this.positionWithinBlock = i;
    }

    public void setLocalMin(boolean b){
      this.localMin = b;
    }

    public void setLocalMax(boolean b){
      this.localMax = b;
    }

    public boolean isLocalMin(){
      return this.localMin;
    }

    public boolean isLocalMax(){
      return this.localMax;
    }

    public boolean isBlock(){
      if(this.blockNumber>0){
        return true;
      } else {
        return false;
      }
    }

    public void setMidpoint(boolean b){
      this.isMidpoint = b;
    }

    public boolean isMidpoint(){
     
      int midpoint  = (int)Math.floor(this.getBlockSize()/2);
      if(this.getPositionWithinBlock() == midpoint){
        this.setMidpoint(true);
      } else {
        this.setMidpoint(false);
      }
      return this.isMidpoint;
    }

    public double getLengthTo(XYPoint a){

      // a2 = b2 + c2
      double dx = Math.abs(this.getX() - a.getX());
      double dy = Math.abs(this.getY() - a.getY());
      double dx2 = dx * dx;
      double dy2 = dy * dy;
      double length = Math.sqrt(dx2+dy2);
      return length;
    }

    public boolean overlaps(XYPoint a){
      if( this.getXAsInt() == a.getXAsInt() && this.getYAsInt() == a.getYAsInt()){
        return true;
      } else {
        return false;
      }

    }

    public String toString(){
      return this.getXAsInt()+","+this.getYAsInt();
    }
  }

  /*
    -----------------------
    NUCLEUS CLASS
    -----------------------
    Contains the variables for storing a nucleus,
    plus the functions for calculating aggregate stats
    within a nucleus
  */  
  class Nucleus {
  
    private int nucleusNumber; // the number of the nucleus in the current image
    private int windowSize = 23; // default size, can be overridden if needed
    private int minimaCount; // the number of local minima detected in the array
    private int maximaCount; // the number of local minima detected in the array
    private int length;  // the length of the array; shortcut to this.array.length
    private int smoothLength = 0; // the length of the smoothed array; shortcut to this.smoothedArray.length
    private int minimaLookupDistance = 5; // the points ahead and behind to check when finding local minima and maxima
    private int blockCount = 0; // the number of delta blocks detected
    private int DELTA_WINDOW_MIN = 5; // the minimum number of points required in a delta block

    private int failureCode = 0; // stores a code to explain why the nucleus failed filters

    private int offsetForTail = 0;

    private int tailIndex; // the index in the smoothedArray that has been designated the tail
    private int tipIndex; // the index in the smoothedArray that has been designated the tip [should be 0]

    private double differenceToMedianCurve; // store the difference between curves as the sum of squares

    private double medianAngle; // the median angle from XYPoint[] smoothedArray
    private double perimeter; // the nuclear perimeter
    private double pathLength; // the angle path length
    private double feret; // the maximum diameter
    private double area; // the nuclear area

    private XYPoint[] array; // the points from the polygon made from the input roi
    private XYPoint[] smoothedArray; // the interpolated points from the input polygon. Most calculations use this.
    private XYPoint[] splineArray; // spline values. Currently not used.
    private ArrayList<XYPoint> intialSpermTails = new ArrayList<XYPoint>(0); // holds the points considered to be sperm tails before filtering

    private XYPoint centreOfMass;
    private XYPoint spermTip;
    private XYPoint spermTail;
    private XYPoint intersectionPoint;
    private XYPoint initialConsensusTail;
    private XYPoint minFeretPoint1;
    private XYPoint minFeretPoint2;
    
    private String imagePath; // the path to the image being analysed

    private boolean minimaCalculated = false; // has detectLocalMinima been run
    private boolean maximaCalculated = false; // has detectLocalMaxima been run
    private boolean anglesCalculated = false; // has makeAngleArray been run
    private boolean offsetCalculated = false; // has makeAngleArray been run
    
    private Roi roi; // the original ROI
    private Polygon polygon; // the ROI converted to a polygon; source of XYPoint[] array

    private ArrayList<NuclearSignal> redSignals = new ArrayList<NuclearSignal>(0); // an array to hold any signals detected
    private ArrayList<NuclearSignal> greenSignals  = new ArrayList<NuclearSignal>(0); // an array to hold any signals detected

    private FloatPolygon smoothedPolygon; // the interpolated polygon; source of XYPoint[] smoothedArray // can probably be removed
    private FloatPolygon hookRoi;
    private FloatPolygon humpRoi;

    // private ArrayList<Double[]> measurementResults = new ArrayList<Double[]>(0);

    // these will replace measurementResults eventually
    private ArrayList<Double> normalisedXPositionsFromTip  = new ArrayList<Double>(0); // holds the x values only after normalisation
    private ArrayList<Double> normalisedYPositionsFromTail = new ArrayList<Double>(0);
    private ArrayList<Double> normalisedXPositionsFromTail = new ArrayList<Double>(0);
    private ArrayList<Double> rawXPositionsFromTail        = new ArrayList<Double>(0);
    private ArrayList<Double> rawXPositionsFromTip         = new ArrayList<Double>(0);
    
    public Nucleus (Roi roi) { // construct from an roi

      // get the polygon from the roi
      this.roi = roi;
      this.polygon = roi.getPolygon();
      this.array = new XYPoint[this.polygon.npoints];
      this.length = this.array.length;
      for(int i=0; i<this.polygon.npoints; i++){
        array[i] = new XYPoint(this.polygon.xpoints[i],this.polygon.ypoints[i]);
      }
     
     try{
        this.smoothedPolygon = roi.getInterpolatedPolygon(1,true); // interpolate and smooth the roi, 1 pixel spacing

        this.smoothedArray = new XYPoint[this.smoothedPolygon.npoints];
        this.smoothLength = this.smoothedArray.length; // shortcult for functions

        for(int i=0; i<this.smoothedPolygon.npoints; i++){
          smoothedArray[i] = new XYPoint(this.smoothedPolygon.xpoints[i],this.smoothedPolygon.ypoints[i]);
        }
      } catch(Exception e){
        IJ.log("Cannot create ROI array: "+e);
      } 
    }

    public Roi getRoi(){
    	return this.roi;
    }

    public double[] getNormalisedXPositionsFromTip(){
      double[] d = new double[normalisedXPositionsFromTip.size()];
      for(int i=0;i<normalisedXPositionsFromTip.size();i++){
        d[i] = normalisedXPositionsFromTip.get(i);
      }
      return d;
    }

    public double[] getNormalisedYPositionsFromTail(){
      double[] d = new double[normalisedYPositionsFromTail.size()];
      for(int i=0;i<normalisedYPositionsFromTail.size();i++){
        d[i] = normalisedYPositionsFromTail.get(i);
      }
      return d;
    }

    public double[] getNormalisedXPositionsFromTail(){
      double[] d = new double[normalisedXPositionsFromTail.size()];
      for(int i=0;i<normalisedXPositionsFromTail.size();i++){
        d[i] = normalisedXPositionsFromTail.get(i);
      }
      return d;
    }

    public double[] getRawXPositionsFromTail(){
      double[] d = new double[rawXPositionsFromTail.size()];
      for(int i=0;i<rawXPositionsFromTail.size();i++){
        d[i] = rawXPositionsFromTail.get(i);
      }
      return d;
    }

    public double[] getRawXPositionsFromTip(){
      double[] d = new double[rawXPositionsFromTip.size()];
      for(int i=0;i<rawXPositionsFromTip.size();i++){
        d[i] = rawXPositionsFromTip.get(i);
      }
      return d;
    }

    /* 
      Fetch the angles in the smoothed array; will be ordered from the tip
    */
    public double[] getAngles(){
      double[] d = new double[this.smoothLength];
      for(int i=0;i<this.smoothLength;i++){
        d[i] = this.smoothedArray[i].getInteriorAngle();
      }
      return d;
    }

    public double getMaxRawXFromTail(){
      double d = 0;
      for(int i=0;i<rawXPositionsFromTail.size();i++){
        if(rawXPositionsFromTail.get(i) > d){
          d = rawXPositionsFromTail.get(i);
        }
      }
      return d;
    }

    public double getMinRawXFromTail(){
      double d = 0;
      for(int i=0;i<rawXPositionsFromTail.size();i++){
        if(rawXPositionsFromTail.get(i) < d){
          d = rawXPositionsFromTail.get(i);
        }
      }
      return d;
    }

    public double getMaxRawXFromTip(){
      double d = 0;
      for(int i=0;i<rawXPositionsFromTip.size();i++){
        if(rawXPositionsFromTip.get(i) > d){
          d = rawXPositionsFromTip.get(i);
        }
      }
      return d;
    }

    public double getMinRawXFromTip(){
      double d = 0;
      for(int i=0;i<rawXPositionsFromTip.size();i++){
        if(rawXPositionsFromTip.get(i) < d){
          d = rawXPositionsFromTip.get(i);
        }
      }
      return d;
    }

    public void addRedSignal(NuclearSignal n){
      this.redSignals.add(n);
    }

    public void addGreenSignal(NuclearSignal n){
      this.greenSignals.add(n);
    }

    public Polygon getPolygon(){
      return this.polygon;
    }

    public void setWindowSize(int i){
    	this.windowSize = i;
    }

    public int getWindowSize(){
    	return this.windowSize;
    }
    /* 
    Find the smoothed length of the array
    */
    public int getLength(){
    	return this.smoothLength;
    }

    public void setPath(String path){
      this.imagePath = path;
    }

    public void setNucleusNumber(int n){
      this.nucleusNumber = n;
    }

    public XYPoint getPoint(int i){
      return this.array[i];
    }

    public XYPoint getSmoothedPoint(int i){
      return this.smoothedArray[i];
    }

    public String getPath(){
      return this.imagePath;
    }

    public String getDirectory(){
      File f = new File(this.imagePath);
      return f.getParent();
    }

    public String getPathWithoutExtension(){
      
      String extension = "";
      String trimmed = "";

      int i = this.imagePath.lastIndexOf('.');
      if (i > 0) {
          extension = this.imagePath.substring(i+1);
          trimmed = this.imagePath.substring(0,i);
      }
      return trimmed;
    }  

    public String getImageName(){
    	File f = new File(this.imagePath);
      return f.getName();
    }

    public void setBlockCount(int i){
    	this.blockCount = i;
    }  

    public int getBlockCount(){
    	return this.blockCount;
    }

    public int getNucleusNumber(){
      return this.nucleusNumber;
    }

    public String getPathAndNumber(){
      return this.imagePath+"\\"+this.nucleusNumber;
    }

    public XYPoint getCentreOfMass(){
      return this.centreOfMass;
    }

    public void setCentreOfMass(XYPoint p){
      this.centreOfMass = p;
    }

    public XYPoint getSpermTip(){
      return this.spermTip;
    }

    public void setSpermTip(XYPoint p){
      this.spermTip = p;
    }

    public void setInitialConsensusTail(XYPoint p){
      this.initialConsensusTail = p;
    }

    public XYPoint getInitialConsensusTail(){
      return this.initialConsensusTail;
    }


    public XYPoint getSpermTail(){
      return this.spermTail;
    }

    public void setSpermTail(XYPoint p){
      this.spermTail = p;
    }

    public double getPerimeter(){
      return this.perimeter;
    }

    public void setPerimeter(double d){
      this.perimeter = d;
    }

    public double getArea(){
      return this.area;
    }

    public void setArea(double d){
      this.area = d;
    }

    public double getFeret(){
      return this.feret;
    }

    public void setFeret(double d){
      this.feret = d;
    }

    public double getPathLength(){
      return this.pathLength;
    }

    public void setPathLength(double d){
      this.pathLength = d;
    }

    public int getTailIndex(){
      return this.tailIndex;
    }

    public void setTailIndex(int i){
      this.tailIndex = i;
    }

    public ArrayList<NuclearSignal> getRedSignals(){
      return this.redSignals;
    }

    public ArrayList<NuclearSignal> getGreenSignals(){
      return this.greenSignals;
    }

    public void addTailEstimatePosition(XYPoint p){
    	this.intialSpermTails.add(p);
    }

    public void reverseArray(){

      XYPoint tmp;

      for (int i = 0; i < this.smoothLength / 2; i++) {
          tmp = this.smoothedArray[i];
          this.smoothedArray[i] = this.smoothedArray[this.smoothedArray.length - 1 - i];
          this.smoothedArray[this.smoothedArray.length - 1 - i] = tmp;
      }

    }

    public void flipXAroundPoint(XYPoint p){

      double xCentre = p.getX();

      for(int i = 0; i<this.smoothLength;i++){

        double dx = xCentre - this.smoothedArray[i].getX();
        double xNew = xCentre + dx;
        this.smoothedArray[i].setX(xNew);
      }

    }

    /*
    	Get the X values from the smoothed array as a float array
    	To be used for spline fitting experiment, hence needs to have the first element duplicated
    */
    public float[] getXasArray(){

    	float[] newArray = new float[this.smoothLength+1]; // allow the first and last element to be duplicated
    	for(int i=0;i<this.smoothLength;i++){
    		newArray[i] = (float)this.smoothedArray[i].getX();
    	}

    	newArray[this.smoothLength] = newArray[0];

    	return newArray;
    }

    /*
    	Get the Y values from the smoothed array as a float array
    	To be used for spline fitting experiment, hence has the first element duplicated
    */
    public float[] getYasArray(){

    	float[] newArray = new float[this.smoothLength+1]; // allow the first and last element to be duplicated
    	for(int i=0;i<this.smoothLength;i++){
    		newArray[i] = (float)this.smoothedArray[i].getY();
    	}

    	newArray[this.smoothLength] = newArray[0];

    	return newArray;
    }

    public boolean isHookSide(XYPoint p){
      if(hookRoi.contains( (float)p.getX(), (float)p.getY() ) ){
        return true;
      } else { 
        return false;
      }
    }

    public boolean isHumpSide(XYPoint p){
      if(humpRoi.contains( (float)p.getX(), (float)p.getY() ) ){
        return true;
      } else { 
        return false;
      }
    }    

    /*
      For two XYPoints in a Nucleus, find the point that lies halfway between them
      Used for obtaining a consensus between potential tail positions
    */
    public int getPositionBetween(XYPoint pointA, XYPoint pointB){

      int a = 0;
      int b = 0;
      // find the indices that correspond on the array
      for(int i = 0; i<this.smoothLength; i++){
          if(this.smoothedArray[i].overlaps(pointA)){
            a = i;
          }
          if(this.smoothedArray[i].overlaps(pointB)){
            b = i;
          }
      }
      // get the midpoint
      int mid = (int)Math.floor( (a+b) /2);
      return mid;
    }
    /*
	   The spline array
    */
    public void setSplineArray(XYPoint[] p){
    	this.splineArray = p;
    }

    /*
    Spline fitting
    CURRENTLY UNUSED
    */
    public XYPoint[] getSplineArray(){
    	return this.splineArray;
    }

    /*
    Spline fitting
    CURRENTLY UNUSED
    */
    public void updateSplineArray(){

    	XYPoint[] splines = new XYPoint[this.smoothLength];
    	float[] profileArray = this.getProfileArray();
    	float[] angleArray = this.getAnglesAsArray();

	   	SplineFitter spf = new SplineFitter(profileArray, angleArray, this.getLength()+1, false); // true  = closed curve
	   	
	   	for(int i=0; i<this.smoothLength;i++) {
	   		double splineY = spf.evalSpline(profileArray[i]);
	   		XYPoint p = new XYPoint(profileArray[i], splineY);
	   		// IJ.log("    Spline: "+splineY);
	   		splines[i] = p;
	   	}
	   	this.setSplineArray(splines);
	  }

  	public float[] getProfileArray(){

  		float[] newArray = new float[this.smoothLength+1];
  		for(int i=0; i<this.smoothLength;i++) {
  			float normalisedX = ((float)i/(float)this.smoothLength)*100; // normalise to 100 length
  			newArray[i] = normalisedX;
  		}
  		newArray[this.smoothLength] = newArray[0];
  		return newArray;
  	}

    public double[] getProfileAngles(){

      double[] d = new double[this.smoothLength]; // allow the first and last element to be duplicated
      for(int i=0;i<this.smoothLength;i++){
        d[i] = this.smoothedArray[i].getInteriorAngle();
      }
      return d;
    }

	  public float[] getAnglesAsArray(){

    	float[] newArray = new float[this.smoothLength+1]; // allow the first and last element to be duplicated
    	for(int i=0;i<this.smoothLength;i++){
    		newArray[i] = (float)this.smoothedArray[i].getInteriorAngle();
    	}

    	newArray[this.smoothLength] = newArray[0];

    	return newArray;
    }

    /*
	    Find the angle that the nucleus must be rotated to make the CoM-tail vertical.
      Uses the angle between [sperm tail x,0], sperm tail, and sperm CoM
	    Returns an angle
	  */
	  public double findRotationAngle(){
	    XYPoint end = new XYPoint(this.getSpermTail().getXAsInt(),this.getSpermTail().getYAsInt()-50);

      double angle = findAngleBetweenXYPoints(end, this.getSpermTail(), this.getCentreOfMass());

	    if(this.getCentreOfMass().getX() < this.getSpermTail().getX()){
	      return angle;
	    } else {
	      return 0-angle;
	    }
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

    // For a position in the roi, draw a line through the CoM and get the intersection point
    public XYPoint findOppositeBorder(XYPoint p){

      int minDeltaYIndex = 0;
      double minAngle = 180;

      for(int i = 0; i<smoothLength;i++){

          double angle = findAngleBetweenXYPoints(p, this.getCentreOfMass(), smoothedArray[i]);

          if(Math.abs(180 - angle) < minAngle){
            minDeltaYIndex = i;
            minAngle = 180 - angle;
          }
      }
      return smoothedArray[minDeltaYIndex];
    }

    /*
      This is a method for finding a tail point independent of local minima:
        Find the narrowest diameter around the nuclear CoM
        Draw a line orthogonal, and pick the intersecting border points
        The border furthest from the tip is the tail
    */
    public XYPoint findTailByNarrowestWidthMethod(){

      // Find the narrowest point around the CoM
      // For a position in teh roi, draw a line through the CoM to the intersection point
      // Measure the length; if < min length..., store equation and border(s)

      double minDistance = this.getFeret();
      XYPoint reference = this.getSpermTip();

      // this.splitNucleusToHeadAndHump();

      for(int i=0;i<this.smoothLength;i++){

        XYPoint p = this.smoothedArray[i];
        XYPoint opp = findOppositeBorder(p);
        double distance = p.getLengthTo(opp);

        if(distance<minDistance){
          minDistance = distance;
          reference = p;
        }
      }
      this.minFeretPoint1 = reference;
      this.minFeretPoint2 = findOppositeBorder(reference);
      
      // Using the point, draw a line from teh CoM to the border. Measure the angle to an intersection point
      // if close to 90, and the distance to the tip > CoM-tip, keep the point
      // return the best point
      double difference = 90;
      XYPoint tail = new XYPoint(0,0);;
      for(int i=0;i<this.smoothLength;i++){

        XYPoint p = this.smoothedArray[i];
        double angle = findAngleBetweenXYPoints(reference, this.getCentreOfMass(), p);
        if(  Math.abs(90-angle)<difference && p.getLengthTo(this.getSpermTip()) > this.getCentreOfMass().getLengthTo( this.getSpermTip() ) ){
          difference = 90-angle;
          tail = p;
        }
      }
      return tail;
    }

    /*
      Change the smoothed array order to put the selected index at the beginning
      only works for smoothed array - indexes are different for normal array
      Input: int the index to move to the start
    */
    public void moveIndexToArrayStart(int i){

      // copy the array to refer to
      XYPoint[] tempSmooth = new XYPoint[this.smoothLength];
      System.arraycopy(this.smoothedArray, 0, tempSmooth, 0 , this.smoothLength);
     
      System.arraycopy(tempSmooth, i, this.smoothedArray, 0 , this.smoothLength-i); // copy over the i to end values
      System.arraycopy(tempSmooth, 0, this.smoothedArray, this.smoothLength-i, i); // copy over index 0 to i
     
      if(tempSmooth.length != this.smoothedArray.length){
        IJ.log("    Unequal array size");
      }     
    }

    /*
      To create the normalised tail-centred index, we want to take the 
      normalised tip-centred index, and move the tail index position to 
      the start. 
    */
    public void createNormalisedYPositionsFromTail(){

      double[] tipCentredAngles = this.getAngles();
      double[] tipCentredXPositions = this.getNormalisedXPositionsFromTip();
      int tailIndex = this.getTailIndex();

      double[] tempArray = new double[tipCentredAngles.length];

      System.arraycopy(tipCentredAngles, tailIndex, tempArray, 0 , tipCentredAngles.length-tailIndex); // copy over the tailIndex to end values
      System.arraycopy(tipCentredAngles, 0, tempArray, tipCentredAngles.length-tailIndex, tailIndex); // copy over index 0 to tailIndex

      double[] tempXArray = new double[tipCentredAngles.length];
      System.arraycopy(tipCentredXPositions, tailIndex, tempXArray, 0 , tipCentredAngles.length-tailIndex); // copy over the tailIndex to end values
      System.arraycopy(tipCentredXPositions, 0, tempXArray, tipCentredAngles.length-tailIndex, tailIndex); // copy over index 0 to tailIndex


      for(int i=0; i<this.smoothLength;i++){
          this.normalisedYPositionsFromTail.add(tempArray[i]);
          this.normalisedXPositionsFromTail.add(tempXArray[i]);
      }
    }

    /* 
    For a given delta block number in the smoothed XYPoint array:
    Get all the points in the array that have the same block number
    Input: int block number
    Return: XYPoint[] all the points in the block
    */
    public XYPoint[] fetchPointsWithBlockNumber(int b){

      int count = countPointsWithBlockNumber(b);
      XYPoint[] array = new XYPoint[count];
      
      int j=0;
      for(int i=0; i<this.smoothLength;i++){

        if(this.smoothedArray[i].getBlockNumber() == b){
          array[j] = this.smoothedArray[i];
          j++;
        }

      }
      return array;
    }

    /* 
    For a given delta block number in the smoothed XYPoint array:
    Count the number of points in the array that have the same block number
    Input: int block number
    Return: int total number of points in block
    */
    public int countPointsWithBlockNumber(int b){

      // find how many points within this block
      int count = 0;
      for(int i=0; i<this.smoothLength;i++){

        if(this.smoothedArray[i].getBlockNumber() == b){
          count++;
        }
      }
      return count;
    }   

    /* 
    For each point in the smoothed XYPoint array:
      Find how many points lie within the angle delta block
      Add this number to the blockSize variable of the point
    */
    public void updatePointsWithBlockCount(){

      for(int i=0; i<this.smoothLength;i++){

        int p = countPointsWithBlockNumber(this.smoothedArray[i].getBlockNumber());
        this.smoothedArray[i].setBlockSize(p);

      }
    }   

    /*
      For a given index in the smoothed angle array: 
        Draw a line between this point, and the points <window> ahead and <window> behind.
        Measure the angle between these points and store as minAngle
        Determine if the angle lies inside or outside the shape. Adjust the angle to always report the interior angle.
        Store interior angle as interiorAngle.
      Automatically wraps the array.
      Input: int index in the array, int window the points ahead and behind to look
    */
    public void findAngleBetweenPoints(int index, int window){

      // wrap the array
      int indexBefore = index < window
                      ? this.smoothLength - (window-index)
                      : index - window;

      int indexAfter = index + window > this.smoothLength-1
                     ? Math.abs(this.smoothLength - (index+window))
                     : index + window;

      XYPoint pointBefore = this.getSmoothedPoint(indexBefore);
      XYPoint pointAfter = this.getSmoothedPoint(indexAfter);
      XYPoint point = this.getSmoothedPoint(index);


      double angle = findAngleBetweenXYPoints(pointBefore, point, pointAfter);

      // find the halfway point between the first and last points.
      // is this within the roi?
      // if yes, keep min angle as interior angle
      // if no, 360-min is interior
      double midX = (pointBefore.getX()+pointAfter.getX())/2;
      double midY = (pointBefore.getY()+pointAfter.getY())/2;

      this.smoothedArray[index].setMinAngle(angle);
      if(this.smoothedPolygon.contains( (float) midX, (float) midY)){
        this.smoothedArray[index].setInteriorAngle(angle);
      } else {
        this.smoothedArray[index].setInteriorAngle(360-angle);
      }
    }

    // Make an angle array for the current coordinates in the XYPoint array
    // Will need to be rerun on each index order change
    public void makeAngleArray(){
    	// go through points
    	// find angle
    	// assign to angle array

        for(int i=0; i<this.smoothLength;i++){

          // use a window size of 25 for now
          findAngleBetweenPoints(i, this.getWindowSize());
          this.smoothedArray[i].setIndex(i);
        }
        this.calculateMedianAngle();

        // calculate the angle deltas and store
        double angleDelta = 0;
        for(int i=0; i<this.smoothLength;i++){

        	// handle array wrapping
	      if(i==0){
	      	angleDelta = this.smoothedArray[i+1].getInteriorAngle() - this.smoothedArray[this.smoothLength-1].getInteriorAngle();
	      } else if(i==this.smoothLength-1){
	      	angleDelta = this.smoothedArray[0].getInteriorAngle() - this.smoothedArray[i-1].getInteriorAngle();
	      } else{
	        angleDelta = this.smoothedArray[i+1].getInteriorAngle() - this.smoothedArray[i-1].getInteriorAngle();
	      }
	      this.smoothedArray[i].setInteriorAngleDelta(angleDelta);
        }

        // perform 5-window smoothing of the deltas
        double smoothedDelta = 0;
        for(int i=0; i<this.smoothLength;i++){

        	// handle array wrapping - TODO: replace with arbitrary length smoothing
	      if(i==0){
	      	smoothedDelta = ( this.smoothedArray[this.smoothLength-2].getInteriorAngleDelta() +
	      					  this.smoothedArray[this.smoothLength-1].getInteriorAngleDelta() +
	      					  this.smoothedArray[i].getInteriorAngleDelta() +
	      					  this.smoothedArray[i+1].getInteriorAngleDelta() +
	      					  this.smoothedArray[i+2].getInteriorAngleDelta() ) / 5;
	      } else if(i==1){

	      	smoothedDelta = ( this.smoothedArray[this.smoothLength-1].getInteriorAngleDelta() +
	      					  this.smoothedArray[i-1].getInteriorAngleDelta() +
	      					  this.smoothedArray[i].getInteriorAngleDelta() +
	      					  this.smoothedArray[i+1].getInteriorAngleDelta() +
	      					  this.smoothedArray[i+2].getInteriorAngleDelta() ) / 5;

	      } else if(i==this.smoothLength-2){

	      	smoothedDelta = ( this.smoothedArray[i-2].getInteriorAngleDelta() +
	      					  this.smoothedArray[i-1].getInteriorAngleDelta() +
	      					  this.smoothedArray[i].getInteriorAngleDelta() +
	      					  this.smoothedArray[i+1].getInteriorAngleDelta() +
	      					  this.smoothedArray[0].getInteriorAngleDelta() ) / 5;

	      } else if(i==this.smoothLength-1){

	      	smoothedDelta = ( this.smoothedArray[i-2].getInteriorAngleDelta() +
	      					  this.smoothedArray[i-1].getInteriorAngleDelta() +
	      					  this.smoothedArray[i].getInteriorAngleDelta() +
	      					  this.smoothedArray[0].getInteriorAngleDelta() +
	      					  this.smoothedArray[1].getInteriorAngleDelta() ) / 5;

	      }else{
	        smoothedDelta  = ( this.smoothedArray[i-2].getInteriorAngleDelta() +
	      					  this.smoothedArray[i-1].getInteriorAngleDelta() +
	      					  this.smoothedArray[i].getInteriorAngleDelta() +
	      					  this.smoothedArray[i+1].getInteriorAngleDelta() +
	      					  this.smoothedArray[i+2].getInteriorAngleDelta() ) / 5;
	      }
	      this.smoothedArray[i].setInteriorAngleDeltaSmoothed(smoothedDelta);
        }

        this.countConsecutiveDeltas();
        this.anglesCalculated = true;

        // IJ.log("    Measured angles with window size "+this.windowSize);
        // IJ.log("    Median angle "+this.medianAngle);
    }

    public void countConsecutiveDeltas(){
    	
    	int blockNumber = 0;
    	for(int i=0;i<this.smoothLength;i++){ // iterate over every point in the array

    		int count = 0;
    		if(this.smoothedArray[i].getInteriorAngleDeltaSmoothed() < 1){ // if the current XYPoint has an angle < 1, move on
    			this.smoothedArray[i].setConsecutiveBlocks(0);
    			this.smoothedArray[i].setBlockNumber(0);
          this.smoothedArray[i].setPositionWithinBlock(0);
    			continue;
    		}

        int positionInBlock = i==0
                            ? 0
                            : this.smoothedArray[i-1].getPositionWithinBlock() + 1; // unless first element of array, use prev value++
    		
        for(int j=1;j<this.smoothLength-i;j++){ // next point on until up to end of array
    			if(this.smoothedArray[i+j].getInteriorAngleDeltaSmoothed() >= 1){
    				count++;

    			} else {
    				break; // stop counting on first point below 1 degree delta
    			}
    		}
    		
    		this.smoothedArray[i].setConsecutiveBlocks(count);
        this.smoothedArray[i].setPositionWithinBlock(positionInBlock);
    		if(i>0){
	    		if(this.smoothedArray[i-1].getBlockNumber()==0){
	    			blockNumber++;
	    		}
	    	}
    		this.smoothedArray[i].setBlockNumber(blockNumber);
    		
    	}

    	this.setBlockCount(blockNumber);
    }

    public XYPoint findMinimumAngle(){

      if(!this.anglesCalculated){
        this.makeAngleArray();
      }
      if(!this.minimaCalculated){
        this.detectLocalMinima();
      }
      double minAngle = 180.0;
      int minIndex = 0;
      for(int i=0; i<this.smoothLength;i++){

          // use a window size of 25 for now
          double angle = this.smoothedArray[i].getMinAngle();
          if(angle<minAngle){
            minAngle = angle;
            minIndex = i;
          }

          // IJ.log(i+" \t "+angle+" \t "+minAngle+"  "+minIndex);
      }
      return this.smoothedArray[minIndex];
    }

    /*
      Checks if the smoothed array nuclear shape profile has the acrosome to the rear of the array
      If acrosome is at the beginning:
        returns true
      else returns false
    */
    public boolean isProfileOrientationOK(){

      if(!this.anglesCalculated){
        this.makeAngleArray();
      }
      if(!this.minimaCalculated){
        this.detectLocalMinima();
      }

      boolean ok = false;

      double maxAngle = 0.0;
      int maxIndex = 0;
      for(int i=0; i<this.smoothLength;i++){

          double angle = this.smoothedArray[i].getInteriorAngle();
          if(angle>maxAngle){
            maxAngle = angle;
            maxIndex = i;
          }

          // IJ.log(i+" \t "+angle+" \t "+minAngle+"  "+minIndex);
      }
      // IJ.log("    Maximum angle "+maxAngle+" at "+maxIndex);

      if(this.smoothLength - maxIndex < maxIndex){ // if the maxIndex is closer to the end than the beginning
        return false;
      } else{ 
        return true;
      }
    }

    /*
      Retrieves an XYPoint array of the points designated as local minima.
      If the local minimum detection has not yet been run, calculates local minima
    */
    public XYPoint[] getLocalMinima(){

      if(!this.minimaCalculated){
        this.detectLocalMinima();
      }

      XYPoint[] newArray = new XYPoint[this.minimaCount];
      int j = 0;

      try{
        for (int i=0; i<this.smoothLength; i++) {
          if(this.smoothedArray[i].isLocalMin()){
            newArray[j] = this.smoothedArray[i];
            j++;
          }
        }
      } catch(Exception e){
        IJ.log("    Error in minima detection: "+e);
      }

      // IJ.log("    Detected "+j+" local minima with lookup size "+this.minimaLookupDistance);
      return newArray;
    }

    /*
      Retrieves an XYPoint array of the points designated as local maxima.
      If the local maximum detection has not yet been run, calculates local maxima
    */
    public XYPoint[] getLocalMaxima(){
 
      if(!this.maximaCalculated){
        this.detectLocalMaxima();
      }

      XYPoint[] newArray = new XYPoint[this.maximaCount];
      int j = 0;
      try{  
        for (int i=0; i<this.smoothLength; i++) {
          if(this.smoothedArray[i].isLocalMax()){
            newArray[j] = this.smoothedArray[i];
            j++;
          }
        }
      } catch(Exception e){
        IJ.log("    Error in maxima detection: "+e);
      }

      // IJ.log("    Detected "+j+" local maxima with lookup size "+this.minimaLookupDistance);
      return newArray;
    }

    /*
      For each point in the smoothed angle array, test for a local minimum.
      The angles of the points <minimaLookupDistance> ahead and behind are checked.
      Each should be greater than the angle before.
      One exception is allowed, to account for noisy data.
    */
    private void detectLocalMinima(){
      // go through angle array (with tip at start)
      // look at 1-2-3-4-5 points ahead and behind.
      // if all greater, local minimum
      
      double[] prevAngles = new double[this.minimaLookupDistance]; // slots for previous angles
      double[] nextAngles = new double[this.minimaLookupDistance]; // slots for next angles

      int count = 0;

      for (int i=0; i<this.smoothLength; i++) { // for each position in sperm

        // go through each lookup position and get the appropriate angles
        for(int j=0;j<prevAngles.length;j++){

          int prev_i = i-(j+1); // the index j+1 before i
          int next_i = i+(j+1); // the index j+1 after i

          // handle beginning of array - wrap around
          if(prev_i < 0){
            prev_i = this.smoothLength + prev_i; // length of array - appropriate value
          }

          // handle end of array - wrap
          if(next_i >= this.smoothLength){
            next_i = next_i - this.smoothLength;
          }

          // fill the lookup array
          prevAngles[j] = this.smoothedArray[prev_i].getInteriorAngle();
          nextAngles[j] = this.smoothedArray[next_i].getInteriorAngle();
        }
        
        // with the lookup positions, see if minimum at i
        // return a 1 if all higher than last, 0 if not
        // prev_l = 0;
        boolean ok = true;
        for(int l=0;l<prevAngles.length;l++){

          // for the first position in prevAngles, compare to the current index
          if(l==0){
            if(prevAngles[l] < this.smoothedArray[i].getInteriorAngle() || nextAngles[l] < this.smoothedArray[i].getInteriorAngle()){
              ok = false;
            }
          } else { // for the remainder of the positions in prevAngles, compare to the prior prevAngle
            
            if(prevAngles[l] < prevAngles[l-1] || nextAngles[l] < nextAngles[l-1]){
              ok = false;
            }
          }

          if( this.smoothedArray[i].getInteriorAngle()-180 > -20){ // ignore any values close to 180 degrees
            ok = false;
          }
        }

        if(ok){
          count++;
        }

        // put oks into array to put into multiarray
        smoothedArray[i].setLocalMin(ok);
      }
      this.minimaCalculated = true;
      this.minimaCount =  count;
    }

    /*
      For each point in the smoothed angle array, test for a local maximum.
      The angles of the points <minimaLookupDistance> ahead and behind are checked.
        *Note that this uses the same variable as detectLocalMinima()*
      Each should be lower than the angle before.
      One exception is allowed, to account for noisy data.
    */
    private void detectLocalMaxima(){
      // go through interior angle array (with tip at start)
      // look at 1-2-3-4-5 points ahead and behind.
      // if all lower, local maximum
      
      double[] prevAngles = new double[this.minimaLookupDistance]; // slots for previous angles
      double[] nextAngles = new double[this.minimaLookupDistance]; // slots for next angles

      int count = 0;

      for (int i=0; i<this.smoothLength; i++) { // for each position in sperm

        // go through each lookup position and get the appropriate angles
        for(int j=0;j<prevAngles.length;j++){

          int prev_i = i-(j+1);
          int next_i = i+(j+1);

          // handle beginning and end of array - wrap around
          if(prev_i < 0){
            prev_i = this.smoothLength + prev_i;
          }
          if(next_i >= this.smoothLength){
            next_i = next_i - this.smoothLength;
          }

          // fill the lookup array
          prevAngles[j] = this.smoothedArray[prev_i].getInteriorAngle();
          nextAngles[j] = this.smoothedArray[next_i].getInteriorAngle();
        }
        
        // with the lookup positions, see if maximum at i
        // return true if all lower than last, false if not
        // prev_l = 0;
        boolean ok = true;
        boolean ignoreOne = true; // allow a single value to be out of place (account for noise in pixel data)
        for(int l=0;l<prevAngles.length;l++){

          // not ok if the outer entries are not higher than inner entries
          if(l==0){
            if( prevAngles[l] > this.smoothedArray[i].getInteriorAngle() ||
                   nextAngles[l] > this.smoothedArray[i].getInteriorAngle() ){

              if( !ignoreOne){
                ok = false;
              }
              
              ignoreOne = false;
            }
          } else {
            
            if(  prevAngles[l] > prevAngles[l-1] || nextAngles[l] > nextAngles[l-1] )  {
              
              if( !ignoreOne){
                ok = false;
              }
              
              ignoreOne = false;
            }
          }

          // we want the angle of a maximum to be higher than the median angle of the array set
          // if( this.smoothedArray[i].getInteriorAngle()-180 < -10){
          if( this.smoothedArray[i].getInteriorAngle() < this.medianAngle){
            ok = false;
          }
        }

        if(ok){
          count++;
        }

        // put oks into array to put into multiarray
        smoothedArray[i].setLocalMax(ok);
      }
      this.maximaCalculated = true;
      this.maximaCount =  count;
    }

    /*
      Go through the deltas marked as consecutive blocks
      Find the midpoints of each block
      Return the point furthest from the tip
    */
    public XYPoint findTailFromDeltas(XYPoint tip){


      // get the midpoint of each block
      ArrayList<XYPoint> results = new ArrayList<XYPoint>(0);
      int maxIndex = 0;
    
      // remember that block 0 is not assigned; start from 1
      try{

        this.updatePointsWithBlockCount();
        for(int i=1; i<this.getBlockCount();i++){

          // number of points in each block

          XYPoint[] points = this.fetchPointsWithBlockNumber(i);
          for(XYPoint p : points){
            if(p.isMidpoint()){ // will ignore any blocks without a midpoint established - <2 members
              results.add(p);
              // IJ.log("    Midpoint found for block "+i);
            }
          }
        }
        // IJ.log("    "+results.size()+" blocks");
      } catch(Exception e){
        IJ.log("    Error in finding midpoints: findTailFromDeltas(): "+e);
      }
      
      XYPoint tail = new XYPoint(0,0);
      try{
        // go through the midpoints, get the max distance from tip
        double maxLength = 0;
        
        for(Object o : results){
          XYPoint p = (XYPoint)o;
          if(p.getLengthTo(tip) > maxLength){
            maxLength = p.getLengthTo(tip);
            tail = p;
          }
        }
      } catch(Exception e){
        IJ.log("    Error in finding lengths: findTailFromDeltas(): "+e);
      }
       // IJ.log("    Midpoint decided at "+tail.toString());
      return tail;
    }

    /*
      For the interior angles in the smoothed angle array:
        Calculate the median angle in the array.
      Stores in medianAngle
    */    
    public void calculateMedianAngle() {

        double[] m = new double[this.smoothLength];
        for(int i = 0; i<this.smoothLength; i++){
          m[i] = this.smoothedArray[i].getInteriorAngle();
        }
        Arrays.sort(m);

        int middle = m.length/2;
        if (m.length%2 == 1) {
            this.medianAngle = m[middle];
        } else {
            this.medianAngle = (m[middle-1] + m[middle]) / 2.0;
        }
    }

    /*
      Print key data to the image log file
      Overwrites any existing log
    */   
    public void printLogFile(){

      String path = this.getPathWithoutExtension()+"\\"+this.getNucleusNumber()+".log";
      File f = new File(path);
      if(f.exists()){
        f.delete();
      }

      IJ.append("SX\tSY\tFX\tFY\tIA\tMA\tI_NORM\tI_DELTA\tI_DELTA_S\tBLOCK_POSITION\tBLOCK_NUMBER\tL_MIN\tL_MAX\tIS_MIDPOINT\tIS_BLOCK\tPROFILE_X", path);
      
      for(int i=0;i<this.smoothLength;i++){

        double normalisedIAngle = smoothedArray[i].getInteriorAngle()-180;
        // double length = this.smoothLength;
        double normalisedX = ((double)i/(double)this.smoothLength)*100; // normalise to 100 length
        // IJ.log("i: "+i+" length: "+this.smoothLength+" profile: "+normalisedX);

        IJ.append(smoothedArray[i].getXAsInt()+"\t"+
                  smoothedArray[i].getYAsInt()+"\t"+
                  smoothedArray[i].getX()+"\t"+
                  smoothedArray[i].getY()+"\t"+
                  smoothedArray[i].getInteriorAngle()+"\t"+
                  smoothedArray[i].getMinAngle()+"\t"+
                  normalisedIAngle+"\t"+
                  smoothedArray[i].getInteriorAngleDelta()+"\t"+
                  smoothedArray[i].getInteriorAngleDeltaSmoothed()+"\t"+
                  smoothedArray[i].getPositionWithinBlock()+"\t"+
                  smoothedArray[i].getBlockNumber()+"\t"+
                  smoothedArray[i].isLocalMin()+"\t"+
                  smoothedArray[i].isLocalMax()+"\t"+
                  smoothedArray[i].isMidpoint()+"\t"+
                  smoothedArray[i].isBlock()+"\t"+
                  normalisedX,
                  path);
      }
    }

    public double[] getInteriorAngles(){

      double[] ypoints = new double[this.smoothLength];

      for(int j=0;j<ypoints.length;j++){
          ypoints[j] = this.smoothedArray[j].getInteriorAngle();
      }
      return ypoints;
    }

    /*
      For the given nucleus index:
      Go through the raw X positions centred on the tail, 
      and apply the calculated offset.
    */
    public double[] createOffsetRawProfile(){

      // if(!this.differencesCalculated){
      //   this.calculateOffsets();
      // }

      double offset = this.offsetForTail;

      double[] xRawCentredOnTail = this.getRawXPositionsFromTail();
      double[] offsetX = new double[xRawCentredOnTail.length];

      for(int j=0;j<xRawCentredOnTail.length;j++){
          offsetX[j] = xRawCentredOnTail[j]+offset;
      }
      return offsetX;
    }

    /*
			In order to split the nuclear roi into hook and hump sides,
			we need to get an intersection point of the line through the 
			tail and centre of mass with the opposite border of the nucleus.
    */
    private int findIntersectionPointForNuclearSplit(){
    	// test if each point from the tail intersects the splitting line
      // determine the coordinates of the point intersected as int
      // for each xvalue of each point in array, get the line y value
      // at the point the yvalues are closest and not the tail point is the intersesction
    	double[] lineEquation = findLineEquation(this.getCentreOfMass(), this.getSpermTail());
      double minDeltaY = 100;
      int minDeltaYIndex = 0;

      for(int i = 0; i<smoothLength;i++){
      		double x = smoothedArray[i].getX();
      		double y = smoothedArray[i].getY();
      		double yOnLine = getYFromEquation(lineEquation, x);

      		double distanceToTail = smoothedArray[i].getLengthTo(spermTail);

      		double deltaY = Math.abs(y - yOnLine);
      		if(deltaY < minDeltaY && distanceToTail > this.getFeret()/2){ // exclude points too close to the tail
      			minDeltaY = deltaY;
      			minDeltaYIndex = i;
      		}
      }
      return minDeltaYIndex;
    }

    public void splitNucleusToHeadAndHump(){

      int intersectionPointIndex = findIntersectionPointForNuclearSplit();
      XYPoint intersectionPoint = smoothedArray[intersectionPointIndex];
      this.intersectionPoint = intersectionPoint;

      // get an array of points from tip to tail
      ArrayList<XYPoint> roi1 = new ArrayList<XYPoint>(0);
      ArrayList<XYPoint> roi2 = new ArrayList<XYPoint>(0);
      boolean changeRoi = false;

      for(int i = 0; i<smoothLength;i++){

      	
      	int currentIndex = wrapIndex(tailIndex+i, smoothLength); // start at the tail, and go around the array
        
        XYPoint p = smoothedArray[currentIndex];

        if(currentIndex != intersectionPointIndex && !changeRoi){   // starting at the tip, assign points to roi1
        	roi1.add(p);
        }
        if(currentIndex==intersectionPointIndex && !changeRoi){ // until we hit the intersection point. Then, close the polygon of roi1 back to the tip. Switch to roi2
          roi1.add(p);
          roi1.add(spermTail);
          roi2.add(intersectionPoint);
          changeRoi = true;
        }
        if(currentIndex != intersectionPointIndex && currentIndex != tailIndex && changeRoi){   // continue with roi2, adjusting the index numbering as needed
          roi2.add(p);
        }

        if(currentIndex==tailIndex && changeRoi){ // after reaching the tail again, close the polygon back to the intersection point
        	roi2.add(intersectionPoint);
        }

      }

      float[] roi1X = new float[ roi1.size()];
      float[] roi2X = new float[ roi2.size()];
      float[] roi1Y = new float[ roi1.size()];
      float[] roi2Y = new float[ roi2.size()];

      for(int i=0;i<roi1.size();i++){
      	roi1X[i] = (float) roi1.get(i).getX();
      	roi1Y[i] = (float) roi1.get(i).getY();
      }

      for(int i=0;i<roi2.size();i++){
      	roi2X[i] = (float) roi2.get(i).getX();
      	roi2Y[i] = (float) roi2.get(i).getY();
      }

      for(int i=0;i<roi1.size();i++){
        if(roi1.get(i).overlaps(spermTip)){
          this.hookRoi = new FloatPolygon( roi1X, roi1Y);
          this.humpRoi = new FloatPolygon( roi2X, roi2Y);
          // IJ.log("Roi2 is hump");
          break;
        }
      }

      for(int i=0;i<roi2.size();i++){
        if(roi2.get(i).overlaps(spermTip)){
          this.hookRoi = new FloatPolygon( roi2X, roi2Y);
          this.humpRoi = new FloatPolygon( roi1X, roi1Y);
           // IJ.log("Roi1 is hump");
           break;
        }
      }
    }

    private double getPolygonArea(float[] x, float[] y, int points){ 
        
        double area = 0;         // Accumulates area in the loop
        int j = points-1;  // The last vertex is the 'previous' one to the first

        for (int i=0; i<points; i++){ 
          area = area +  (x[j]+x[i]) * (y[j]-y[i]); 
          j = i;  //j is previous vertex to i
        }
        return area/2;
    }

    public void calculateSignalAnglesFromTail(){

      if(redSignals.size()>0){

        for(int i=0;i<redSignals.size();i++){
          NuclearSignal n = redSignals.get(i);
          double angle = findAngleBetweenXYPoints(this.getSpermTail(), this.getCentreOfMass(), n.getCentreOfMass());

          // hook or hump?
          if( this.isHookSide(n.getCentreOfMass()) ){ // hookRoi.contains((float) n.centreOfMass.getX() , (float) n.centreOfMass.getY())  
            angle = 360 - angle;
          }

          // set the final angle
          n.setAngle(angle);
        }
      }

    	if(greenSignals.size()>0){

        for(int i=0;i<greenSignals.size();i++){
          NuclearSignal n = greenSignals.get(i);
          double angle = findAngleBetweenXYPoints(this.getSpermTail(), this.getCentreOfMass(), n.getCentreOfMass());

          // hook or hump?
          if( this.isHookSide(n.getCentreOfMass()) ){
            angle = 360 - angle;
          }

          // set the final angle
          n.setAngle(angle);
        }
      }
    }

    public void calculateSignalDistances(){

      ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
      signals.add(redSignals);
      signals.add(greenSignals);

      for( ArrayList<NuclearSignal> signalGroup : signals ){

      	if(signalGroup.size()>0){
          for(int i=0;i<signalGroup.size();i++){
          	NuclearSignal n = signalGroup.get(i);

          	double distance = this.getCentreOfMass().getLengthTo(n.getCentreOfMass());
          	n.setDistance(distance);
          }
        }
      }

      // if(greenSignals.size()>0){
      //   for(int i=0;i<greenSignals.size();i++){
      //   	NuclearSignal n = greenSignals.get(i);
      //   	double distance = this.getCentreOfMass().getLengthTo(n.getCentreOfMass());
      //   	n.setDistance(distance);
      //   }
      // }
    }


    public double[] findLineEquation(XYPoint a, XYPoint b){

      // y=mx+c
      double deltaX = a.getX() - b.getX();
      double deltaY = a.getY() - b.getY();
        
      double m = deltaY / deltaX;
        
      // y - y1 = m(x - x1)
      double c = a.getY() -  ( m * a.getX() );
        
      // double testY = (m * position_2[0]) + c;
        
      // write("y = "+m+"x + "+c);
      // result=newArray(m, c);
      return new double[] { m, c };
    }

    public double getXFromEquation(double[] eq, double y){
      // x = (y-c)/m
      double x = (y - eq[1]) / eq[0];
      return x;
    }

    public double getYFromEquation(double[] eq, double x){
      // x = (y-c)/m
      double y = (eq[0] * x) + eq[1];
      return y;
    }

    /*
      Calculate the distance from the nuclear centre of
      mass as a fraction of the distance from the nuclear CoM, through the 
      signal CoM, to the nuclear border
    */
    public void calculateFractionalSignalDistances(){

      ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
      signals.add(redSignals);
      signals.add(greenSignals);

      for( ArrayList<NuclearSignal> signalGroup : signals ){
      
        if(signalGroup.size()>0){
          for(int i=0;i<signalGroup.size();i++){
            NuclearSignal n = signalGroup.get(i);

            // double distance = this.getCentreOfMass().getLengthTo(n.getCentreOfMass());

            // get the line equation
            double eq[] = findLineEquation(n.getCentreOfMass(), this.getCentreOfMass());

            // using the equation, get the y postion on the line for each X point around the roi
            double minDeltaY = 100;
            int minDeltaYIndex = 0;
            double minDistanceToSignal = 1000;

            for(int j = 0; j<smoothLength;j++){
                double x = smoothedArray[j].getX();
                double y = smoothedArray[j].getY();
                double yOnLine = getYFromEquation(eq, x);
                double distanceToSignal = smoothedArray[j].getLengthTo(n.getCentreOfMass()); // fetch


                double deltaY = Math.abs(y - yOnLine);
                // find the point closest to the line; this could find either intersection
                // hence check it is as close as possible to the signal CoM also
                if(deltaY < minDeltaY && distanceToSignal < minDistanceToSignal){
                  minDeltaY = deltaY;
                  minDeltaYIndex = j;
                  minDistanceToSignal = distanceToSignal;
                }
            }
            XYPoint borderPoint = smoothedArray[minDeltaYIndex];
            double nucleusCoMToBorder = borderPoint.getLengthTo(this.getCentreOfMass());
            double signalCoMToNucleusCoM = this.getCentreOfMass().getLengthTo(n.getCentreOfMass());
            double fractionalDistance = signalCoMToNucleusCoM / nucleusCoMToBorder;
            n.setFractionalDistance(fractionalDistance);
          }
        }
      }
    }

    /*
      Go through the signals in the nucleus, and find the point on
      the nuclear ROI that is closest to the signal centre of mass.
    */
    public void calculateClosestBorderToSignal(){

      ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
      signals.add(redSignals);
      signals.add(greenSignals);

      for( ArrayList<NuclearSignal> signalGroup : signals ){
      
        if(signalGroup.size()>0){
          for(int i=0;i<signalGroup.size();i++){
            NuclearSignal n = signalGroup.get(i);

            int minIndex = 0;
            double minDistanceToSignal = 1000;

            for(int j = 0; j<smoothLength;j++){
                XYPoint p = smoothedArray[j];
                double distanceToSignal = p.getLengthTo(n.getCentreOfMass());

                // find the point closest to the CoM
                if(distanceToSignal < minDistanceToSignal){
                  minIndex = j;
                  minDistanceToSignal = distanceToSignal;
                }
            }
            // XYPoint borderPoint = smoothedArray[minIndex];
            n.setClosestBorderPoint(smoothedArray[minIndex]);
          }
        }
      }
    }
  }

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

  	public NucleusCollection(String folder){
  		this.folder = folder;
      this.medianFile = folder+"logTipMedians.txt";
      this.tailNormalisedMedianFile = folder+"logTailMedians.txt";
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
        count += nucleiCollection.get(i).getRedSignals().size();
      }
      return count;
    }

    public int getGreenSignalCount(){
      int count = 0;

      for(int i=0;i<nucleiCollection.size();i++){
        count += nucleiCollection.get(i).getGreenSignals().size();
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

      IJ.log("Prefiltered:");
      IJ.log("    Area: "+(int)medianArea);
      IJ.log("    Perimeter: "+(int)medianPerimeter);
      IJ.log("    Path length: "+(int)medianPathLength);
      IJ.log("    Array length: "+(int)medianArrayLength);
      IJ.log("    Feret length: "+(int)medianFeretLength);
      IJ.log("    Curve: "+(int)medianDifferenceToMedianCurve);

      for(int i=0;i<nucleiCollection.size();i++){
        Nucleus n = nucleiCollection.get(i);
        boolean dropNucleus = false;
        // IJ.log("Filtering: "+i);

        // IJ.log("Nucleus "+n.getPath()+"-"+n.getNucleusNumber()+" Path length "+n.getPathLength());

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

      IJ.log("Postfiltered:");
      IJ.log("    Area: "+(int)medianArea);
      IJ.log("    Perimeter: "+(int)medianPerimeter);
      IJ.log("    Path length: "+(int)medianPathLength);
      IJ.log("    Array length: "+(int)medianArrayLength);
      IJ.log("    Feret length: "+(int)medianFeretLength);
      IJ.log("    Curve: "+(int)medianDifferenceToMedianCurve);
      IJ.log("Removed due to size or length issues: "+removed+" nuclei");
      IJ.log("  Due to area outside bounds "+(int)minArea+"-"+(int)maxArea+": "+area+" nuclei");
      IJ.log("  Due to perimeter outside bounds "+(int)minPerim+"-"+(int)maxPerim+": "+perim+" nuclei");
      IJ.log("  Due to wibbliness >"+(int)maxPathLength+" : "+(int)pathlength+" nuclei");
      IJ.log("  Due to array length: "+arraylength+" nuclei");
      IJ.log("  Due to feret length: "+feretlength+" nuclei");
      IJ.log("  Due to curve shape: "+curveShape+" nuclei");
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
        XYPoint[] yValues = nucleiCollection.get(i).smoothedArray;

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
        // XYPoint[] yValues = nucleiCollection.get(i).smoothedArray;

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
		      double curveAngle = r.smoothedArray[curveIndex].getInteriorAngle();

		      // get the next median index position, given the tail point is 0
		      int medianIndex = wrapIndex(medianTailIndex+j, medianInterpolatedArray.length); // DOUBLE CHECK THE LOGIC HERE - CAUSING NPE WHEN USING  normalisedMedian.length
		      // IJ.log("Median index: "+medianIndex);
		      double medianAngle = medianInterpolatedArray[medianIndex];
		      // IJ.log("j="+j+" Curve index: "+curveIndex+" Median index: "+medianIndex+" Median: "+medianAngle);
		      // double difference = 
		      totalDifference += Math.abs(curveAngle - medianAngle);
				}

				this.nucleiCollection.get(i).offsetForTail = offset;

        r.offsetCalculated = true;
        r.tailIndex = r.tailIndex-offset; // update the tail position
        r.setSpermTail(r.smoothedArray[r.tailIndex]); // ensure the spermTail is updated
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
        double[] yPoints            = nucleiCollection.get(i).getProfileAngles();
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


  		// if(medianIndexLower<0)
  		// 	medianIndexLower = (this.normalisedMedian.length-1) + index;
  		// if(medianIndexLower>this.normalisedMedian.length-1)
  		// 	medianIndexLower = medianIndexLower - (this.normalisedMedian.length-1);

  		// if(medianIndexHigher<0)
  		// 	medianIndexHigher = (this.normalisedMedian.length-1) + index;
  		// if(medianIndexHigher>this.normalisedMedian.length-1)
  		// 	medianIndexHigher = medianIndexHigher - (this.normalisedMedian.length-1);

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
			// can't use regular tail detector, because it's based on XYPoints
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

    			// if(diff < minDiff){
    			// 	minDiff = diff;
    			// 	tailIndex = index;
    			// }

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

          ArrayList redPoints = new ArrayList(0);
          ArrayList yPoints = new ArrayList(0);

          for(int j=0; j<redSignals.size();j++){

            XYPoint border = redSignals.get(j).getClosestBorderPoint();
            for(int k=0; k<n.smoothLength;k++){

              if(n.smoothedArray[k].overlaps(border)){
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

          ArrayList redPoints = new ArrayList(0);
          ArrayList yPoints = new ArrayList(0);

          for(int j=0; j<redSignals.size();j++){

            XYPoint border = redSignals.get(j).getClosestBorderPoint();
            for(int k=0; k<n.smoothLength;k++){

              if(n.smoothedArray[k].overlaps(border)){
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
      IJ.saveAsTiff(tipPlot, this.folder+"plotTipNorm.tiff");
      ImagePlus tailPlot = normXFromTailPlot.getImagePlus();
      IJ.saveAsTiff(tailPlot, this.folder+"plotTailNorm.tiff");
    }

    public void exportSignalStats(){

      String redLogFile = this.folder+"logRedSignals.txt";
      File r = new File(redLogFile);
      if(r.exists()){
        r.delete();
      }

      String greenLogFile = this.folder+"logGreenSignals.txt";
      File g = new File(greenLogFile);
      if(g.exists()){
        g.delete();
      }

      IJ.append("# NUCLEUS_NUMBER\tSIGNAL_AREA\tSIGNAL_ANGLE\tSIGNAL_FERET\tSIGNAL_DISTANCE\tFRACTIONAL_DISTANCE\tSIGNAL_PERIMETER\tPATH", redLogFile);
      IJ.append("# NUCLEUS_NUMBER\tSIGNAL_AREA\tSIGNAL_ANGLE\tSIGNAL_FERET\tSIGNAL_DISTANCE\tFRACTIONAL_DISTANCE\tSIGNAL_PERIMETER\tPATH", greenLogFile);
      for(int i= 0; i<this.nucleiCollection.size();i++){ // for each roi

        int nucleusNumber = this.nucleiCollection.get(i).getNucleusNumber();
        String path = this.nucleiCollection.get(i).getPath();

        ArrayList<NuclearSignal> redSignals = this.nucleiCollection.get(i).getRedSignals();
        if(redSignals.size()>0){
          for(int j=0; j<redSignals.size();j++){
             NuclearSignal n = redSignals.get(j);
             IJ.append(nucleusNumber+"\t"+
                       n.getArea()+"\t"+
                       n.getAngle()+"\t"+
                       n.getFeret()+"\t"+
                       n.getDistance()+"\t"+
                       n.getFractionalDistance()+"\t"+
                       n.getPerimeter()+"\t"+
                       path, redLogFile);
          }
        }

        ArrayList<NuclearSignal> greenSignals = this.nucleiCollection.get(i).getGreenSignals();
        if(greenSignals.size()>0){
          for(int j=0; j<greenSignals.size();j++){
             NuclearSignal n = greenSignals.get(j);
             IJ.append(nucleusNumber+"\t"+
                       n.getArea()+"\t"+
                       n.getAngle()+"\t"+
                       n.getFeret()+"\t"+
                       n.getDistance()+"\t"+
                       n.getFractionalDistance()+"\t"+
                       n.getPerimeter()+"\t"+
                       path, greenLogFile);
          }
        }
      }
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
    
      String statsFile = this.folder+filename;
      File f = new File(statsFile);
      if(f.exists()){
        f.delete();
      }
      IJ.append("# AREA\tPERIMETER\tFERET\tPATH_LENGTH\tNORM_TAIL_INDEX\tSQUARE_DIFFERENCE\tFAILURE_CODE\tPATH", statsFile);

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
        // progressBar.show(i, this.getNucleusCount());
        IJ.append(  areas[i]+"\t"+
                    perims[i]+"\t"+
                    ferets[i]+"\t"+
                    pathLengths[i]+"\t"+
                    tails[i]+"\t"+
                    differences[i]+"\t"+
                    this.nucleiCollection.get(i).failureCode+"\t"+
                    paths[i], statsFile);

        // Include tip, CoM, tail
    		this.nucleiCollection.get(i).printLogFile();
      }
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
      }
    	finalImage.show();
    	IJ.saveAsTiff(finalImage, folder+filename);
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

    		// open the image we saved earlier
    		String path = n.getPathWithoutExtension()+"\\"+n.getNucleusNumber()+".tiff";
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
		    	XYPoint p = n.intialSpermTails.get(j);
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
		    IJ.saveAsTiff(image, path);
		    image.close();

    	}
    	 IJ.log("Annotation complete");
    }
  }
  /*
    -----------------------
    NUCLEUS SIGNAL CLASS
    -----------------------
    Contains the variables for storing a signal within the nucleus
  */  
  class NuclearSignal {

    private double area;
    private double perimeter;
    private double feret;
    private double angleFromTail;
    private double distanceFromCentreOfMass; // the absolute measured distance from the signal CoM to the nuclear CoM
    private double fractionalDistanceFromCoM; // the distance to the centre of mass as a fraction of the distance from the CoM to the closest border

    private XYPoint centreOfMass;
    private XYPoint closestNuclearBorderPoint;

    private Roi roi;

    public NuclearSignal(Roi roi, double area, double feret, double perimeter, XYPoint centreOfMass){
      this.roi = roi;
      this.area = area;
      this.perimeter = perimeter;
      this.feret = feret;
      this.centreOfMass = centreOfMass;
    }

    public Roi getRoi(){
      return this.roi;
    }

    public double getArea(){
      return this.area;
    }

    public double getPerimeter(){
      return this.perimeter;
    }

    public double getFeret(){
      return this.feret;
    }

    public double getAngle(){
      return this.angleFromTail;
    }

    public double getDistance(){
      return this.distanceFromCentreOfMass;
    }

    public double getFractionalDistance(){
      return this.fractionalDistanceFromCoM;
    }

    public XYPoint getCentreOfMass(){
      return this.centreOfMass;
    }

    public XYPoint getClosestBorderPoint(){
      return this.closestNuclearBorderPoint;
    }

    public void setArea(double d){
      this.area = d;
    }

    public void setPerimeter(double d){
      this.perimeter = d;
    }

    public void setFeret(double d){
      this.feret = d;
    }

    public void setAngle(double d){
      this.angleFromTail = d;
    }

    public void setDistance(double d){
      this.distanceFromCentreOfMass = d;
    }

    public void setFractionalDistance(double d){
      this.fractionalDistanceFromCoM = d;
    }

    public void setCentreOfMass(XYPoint p){
      this.centreOfMass = p;
    }

    public void setClosestBorderPoint(XYPoint p){
      this.closestNuclearBorderPoint = p;
    }
  }

}
