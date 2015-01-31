import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Plot;
import ij.gui.PlotWindow;
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
  private static final int SIGNAL_THRESHOLD = 70;
  
  private static final double ANGLE_THRESHOLD = 40.0; // when calculating local minima, ignore angles above this

  /* VALUES FOR DECIDING IF AN OBJECT IS A NUCLEUS */
  private static final int NUCLEUS_THRESHOLD = 40;
  private static final double MIN_NUCLEAR_SIZE = 500;
  private static final double MAX_NUCLEAR_SIZE = 7000;
  private static final double MIN_NUCLEAR_CIRC = 0.3;
  private static final double MAX_NUCLEAR_CIRC = 0.8;
  private static final double PROFILE_INCREMENT = 0.5;

  private static final double MAXIMUM_PATH_LENGTH = 1000; // reject nuclei with an angle path length greater than this; wibbly

  private int totalNuclei = 0;
  private int nucleiFailedOnTip = 0;
  private int nucleiFailedOnTail = 0;
  private int nucleiFailedOther = 0; // generic reasons for failure

  private Plot linePlot;
  private Plot rawProfilePlot;

  private PlotWindow plotWindow;
  private PlotWindow rawPlotWindow;

  private String logFile;
  private String failedFile;
  private String medianFile;
  private String statsFile;

  private Map<Double, Collection<Double>> finalResults = new HashMap<Double, Collection<Double>>();

  private ArrayList perimeterArray = new ArrayList(0);
  private ArrayList areaArray = new ArrayList(0);
  private ArrayList feretArray = new ArrayList(0);
  private ArrayList nucleusArray = new ArrayList(0); // hold the name and paths for reference
  private ArrayList pathLengthArray = new ArrayList(0); // hold the length from point to point of the angle profile.
  private ArrayList<Double> tailIndexArray = new ArrayList<Double>(0); // hold the length from point to point of the angle profile.

  private static final int RAW_PROFILE_CHART_WIDTH = 400;

  private static final int FAILURE_TIP = 1;
  private static final int FAILURE_TAIL = 2;
  private static final int FAILURE_THRESHOLD = 4;
  private static final int FAILURE_OTHER = 8;
  
  public void run(String paramString)  {

    DirectoryChooser localOpenDialog = new DirectoryChooser("Select directory of images...");
    String folderName = localOpenDialog.getDirectory();

    prepareLogFiles(folderName);

    IJ.showStatus("Opening directory: " + folderName);
    IJ.log("Directory: "+folderName);

    File folder = new File(folderName);
    File[] listOfFiles = folder.listFiles();


    this.linePlot = new Plot("Profiles in "+folderName,
            "Position",
            "Angle");
    linePlot.setLimits(0,100,-50,360);
    linePlot.setSize(800,600);
    linePlot.setYTicks(true);
    linePlot.setColor(Color.  LIGHT_GRAY);
    plotWindow = linePlot.show();

    this.rawProfilePlot = new Plot("Non-normalised profiles in "+folderName,
            "Position",
            "Angle");
    rawProfilePlot.setLimits(0,this.RAW_PROFILE_CHART_WIDTH,-50,360);
    rawProfilePlot.setSize(800,600);
    rawProfilePlot.setYTicks(true);
    rawProfilePlot.setColor(Color.  LIGHT_GRAY);
    rawPlotWindow = rawProfilePlot.show();

    for (File file : listOfFiles) {
      if (file.isFile()) {

        String fileName = file.getName();

        for( String fileType : fileTypes){
          if( fileName.endsWith(fileType) ){
            IJ.showStatus("Opening file: " + fileName);
            IJ.log("File:    "+fileName);
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

    exportMedians();
    exportNuclearStats();
    linePlot.draw();
    rawProfilePlot.draw();

    ImagePlus finalPlot = linePlot.getImagePlus();
    IJ.saveAsTiff(finalPlot, folderName+"plotNorm.tiff");
    ImagePlus finalRawPlot = rawProfilePlot.getImagePlus();
    IJ.saveAsTiff(finalRawPlot, folderName+"plotRaw.tiff");
    IJ.log("Completed folder");
    IJ.log("Total nuclei  : "+this.totalNuclei);
    IJ.log("Failed on tip : "+this.nucleiFailedOnTip);
    IJ.log("Failed on tail: "+this.nucleiFailedOnTail);
    IJ.log("Failed (other): "+this.nucleiFailedOther);
    int analysed = this.totalNuclei - this.nucleiFailedOnTail - this.nucleiFailedOnTip - this.nucleiFailedOther;
    IJ.log("Analysed      : "+analysed);

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
    IJ.append("# NORM_X\tANGLE", this.logFile);

    this.failedFile = folderName+"logFailed.txt";
    File g = new File(failedFile);
    if(g.exists()){
      g.delete();
    }

    IJ.append("# CAUSE_OF_FAILURE\tPERIMETER\tAREA\tFERET\tPATH_LENGTH\tNORM_TAIL_INDEX\tPATH", this.failedFile);

    this.medianFile = folderName+"logMedians.txt";
    File h = new File(medianFile);
    if(h.exists()){
      h.delete();
    }

    IJ.append("# X_POSITION\tANGLE_MEDIAN\tQ25\tQ7\tQ10\tQ90\tNUMBER_OF_POINTS", this.medianFile);

    this.statsFile = folderName+"logStats.txt";
    File i = new File(statsFile);
    if(i.exists()){
      i.delete();
    }

    IJ.append("# AREA\tPERIMETER\tFERET\tPATH_LENGTH\tNORM_TAIL_INDEX\tPATH", this.statsFile);
  }

  /*
    Write the median angles at each bin to the global log file
  */
  public void exportMedians(){
  	// output the final results: calculate median positions
    // IJ.append("", this.medianFile);

    int arraySize = (int)Math.round(100/PROFILE_INCREMENT);
    double[] xmedians = new double[arraySize];
    double[] ymedians = new double[arraySize];
    double[] lowQuartiles = new double[arraySize];
    double[] uppQuartiles = new double[arraySize];
    double[] tenQuartiles = new double[arraySize];
    double[] ninetyQuartiles = new double[arraySize];

    int m = 0;
    for(double k=0.0;k<100;k+=PROFILE_INCREMENT){

      try{
          Collection<Double> values = finalResults.get(k);

          if(values.size()> 0){
            Double[] d = values.toArray(new Double[0]);
            int n = d.length;

            // Arrays.sort(d);
            double median = quartile(d, 50.0);
            double q1 = quartile(d, 25.0);
            double q3 = quartile(d, 75.0);
            double q10 = quartile(d, 10.0);
            double q90 = quartile(d, 90.0);
           
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
                      n, this.medianFile);
          }
        } catch(Exception e){
             IJ.log("Cannot calculate median for "+k);
             xmedians[m] = k;
             ymedians[m] = 0;
             lowQuartiles[m] = 0;
             uppQuartiles[m] = 0;
             tenQuartiles[m] = 0;
             ninetyQuartiles[m] = 0;
        } finally {
        	m++;
    	}
    }

    linePlot.setColor(Color.BLACK);
    linePlot.setLineWidth(3);
    linePlot.addPoints(xmedians, ymedians, Plot.LINE);
    linePlot.setColor(Color.DARK_GRAY);
    linePlot.setLineWidth(2);
    linePlot.addPoints(xmedians, lowQuartiles, Plot.LINE);
    linePlot.addPoints(xmedians, uppQuartiles, Plot.LINE);
    // linePlot.addPoints(xmedians, tenQuartiles, Plot.LINE);
    // linePlot.addPoints(xmedians, ninetyQuartiles, Plot.LINE);


    // handle the tail position mapping
    double[] xTails = new double[tailIndexArray.size()];
    for(int i=0; i<tailIndexArray.size(); i++){
      xTails[i] = (double)tailIndexArray.get(i);
    }

    double[] yTails = new double[tailIndexArray.size()];
    Arrays.fill(yTails, 300);
    linePlot.setColor(Color.LIGHT_GRAY);
    linePlot.addPoints(xTails, yTails, Plot.DOT);

    // median tail positions
    Double[] tails = tailIndexArray.toArray(new Double[0]);
    double tailQ50 = quartile(tails, 50);
    double tailQ25 = quartile(tails, 25);
    double tailQ75 = quartile(tails, 75);

    linePlot.setColor(Color.DARK_GRAY);
    linePlot.setLineWidth(1);
    linePlot.drawLine(tailQ25, 320, tailQ75, 320);
    linePlot.drawLine(tailQ25, 280, tailQ75, 280);
    linePlot.drawLine(tailQ25, 280, tailQ25, 320);
    linePlot.drawLine(tailQ75, 280, tailQ75, 320);
    linePlot.drawLine(tailQ50, 280, tailQ50, 320);

  }

  /*
    Write the nuclear area, perimeter, feret and path to the global log file
  */
  public void exportNuclearStats(){
  	
  	for(int i=0; i<areaArray.size();i++){
  		IJ.append(areaArray.get(i)+"\t"+
                  perimeterArray.get(i)+"\t"+
                  feretArray.get(i)+"\t"+
                  pathLengthArray.get(i)+"\t"+
                  tailIndexArray.get(i)+"\t"+
                  nucleusArray.get(i), this.statsFile);
  	}

  }

  /*
    Calculate the <lowerPercent> quartile from a Double[] array
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

    	ArrayList rt = new ArrayList(0);
      // get the profile (and other) data back for the nucleus
      IJ.log("  Analysing nucleus "+i);
      try{
      	rt = analyseNucleus(roi, image, i, path);
      	this.totalNuclei++;
      } catch(Exception e){
      	IJ.log("  Error analysing nucleus: "+e);
      }

      // carry out the group processing - eg find median lines
      try{

        if(rt.size()>0){
          // add values to pool

          for(double k=0.0;k<100;k+=PROFILE_INCREMENT){ // cover all the bin positions across the profile

            for(int j=0;j<rt.size();j++){

                double[] d = (double[])rt.get(j);
               
                if( d[0] > k && d[0] < k+PROFILE_INCREMENT){

                    Collection<Double> values = finalResults.get(k);
                    if (values==null) {
                        values = new ArrayList<Double>();
                        finalResults.put(k, values);
                    }
                    values.add(d[1]);

                }
            }
          }

          double[] xpoints = new double[rt.size()];
          double[] ypoints = new double[rt.size()];
          double[] xprofile = new double[rt.size()];
          // double pathLength = 0;
          // XYPoint prevPoint = new XYPoint(0,0);
          for(int j=0;j<rt.size();j++){
              double[] d = (double[])rt.get(j);
              xpoints[j] = d[0];
              ypoints[j] = d[1];
              xprofile[j] = j;
          }

          linePlot.setColor(Color.LIGHT_GRAY);
          linePlot.addPoints(xpoints, ypoints, Plot.LINE);
          linePlot.draw();
          plotWindow.drawPlot(linePlot);
          rawProfilePlot.setColor(Color.LIGHT_GRAY);
          rawProfilePlot.addPoints(xprofile, ypoints, Plot.LINE);
          rawProfilePlot.draw();
          rawPlotWindow.drawPlot(rawProfilePlot);
        }

        i++;
      } catch(NullPointerException e){
         IJ.log("  Error processing nucleus data: "+e);
      }
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
        IJ.log("  Found "+manager.getCount()+ " nuclei");
        // rt.show("Title");
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

  /*
    Carry out the full analysis of a given nucleus.
    Detect the nuclear centre of mass.
    Detect the sperm tip.
    Detect the sperm tail by multiple methods, and find a consensus point.
    Detect signals in the red and green channels, and calculate their positions relative to the CoM
    Draw regions of interest on a new image, and save this out to the relevant directory.
  */
  public ArrayList analyseNucleus(Roi nucleus, ImagePlus image, int nucleusNumber, String path){
    
    // results table
    ArrayList rt = new ArrayList(0);
    boolean nucleusPassedChecks = true; // any check can disable this
    // String failureReason = "";
    int failureReason = 0;


    // make a copy of the nucleus only for saving out and processing
    image.setRoi(nucleus);
    image.copy();
    ImagePlus smallRegion = ImagePlus.getClipboard();
    nucleus.setLocation(0,0); // translate the roi to the new image coordinates
    smallRegion.setRoi(nucleus);


    // prepare an image processor to annotate the image
    ImageProcessor ip = smallRegion.getProcessor();


    // turn roi into RoiArray for manipulation
    RoiArray roiArray = new RoiArray(nucleus);
    roiArray.setPath(path);
    roiArray.setNucleusNumber(nucleusNumber);


    // measure CoM, area, perimeter and feret in blue
    ResultsTable blueResults = findNuclearMeasurements(smallRegion, nucleus);
    XYPoint nucleusCoM = new XYPoint(blueResults.getValue("XM", 0),  blueResults.getValue("YM", 0) );


    // draw the roi
    ip.setColor(Color.BLUE);
    ip.setLineWidth(1);
    ip.draw(nucleus);


    // draw the CoM
    ip.setColor(Color.MAGENTA);
    ip.setLineWidth(5);
    ip.drawDot(nucleusCoM.getXAsInt(), nucleusCoM.getYAsInt());


    // find tip - use the least angle method
    XYPoint spermTip = roiArray.findMinimumAngle();
    if(spermTip.getInteriorAngle() > 110){ // this is not a deep enough curve to declare the tip
        IJ.log("    Cannot reliably assign tip position");
        this.nucleiFailedOnTip++;
        nucleusPassedChecks = false;
        // failureReason += "Tip ";
        failureReason = failureReason | this.FAILURE_TIP;
    }
    roiArray.moveIndexToArrayStart(spermTip.getIndex());


    // decide if the profile is right or left handed; flip if needed
    if(!roiArray.isProfileOrientationOK()){
      IJ.log("    Reversing array");
      roiArray.reverseArray();
    }

    
    //draw the sperm tip 
    ip.setLineWidth(5);
    ip.setColor(Color.YELLOW);
    ip.drawDot(spermTip.getXAsInt(), spermTip.getYAsInt());
    

    // find local minima and maxima
    XYPoint[] minima = roiArray.getLocalMinima();
    XYPoint[] maxima = roiArray.getLocalMaxima();


    // draw  local minima
    ip.setColor(Color.GREEN);
    ip.setLineWidth(3);
    for (XYPoint p : minima){
         ip.drawDot(p.getXAsInt(), p.getYAsInt());
    }
       

    // draw  local maxima
    ip.setColor(Color.RED);
    for (XYPoint p : maxima){
         ip.drawDot(p.getXAsInt(), p.getYAsInt());
    }


    /*
    	Find the tail point using multiple independent methods. 
    	Find a consensus point
    
    	Method 1: Find the narrowest diameter around the nuclear CoM
    						Draw a line orthogonal, and pick the intersecting border points
    						The border furthest from the tip is the tail
    */	
    XYPoint spermTail1 = findPointFurthestFrom(spermTip, minima); // TO BE REPLACED WITH DESCRIPTION ABOVE

    /*
    	Method 2: Use the list of local minima to detect the tail corner
    						This is the corner furthest from the tip.
    						Can be confused as to which side of the sperm head is chosen
    */	
    XYPoint spermTail2 = findTailPointFromMinima(spermTip, nucleusCoM, minima);

    /*
    	Method 3: Look at the 2nd derivative - rate of change of angles
    						Perform a 5win average smoothing of the deltas
    						Count the number of consecutive >1 degree blocks
    						Wide block far from tip = tail
    */	
    XYPoint spermTail3 = roiArray.findTailFromDeltas(spermTip);

    /*
    	Given distinct methods for finding a tail,
    	take a position between them on roi
    */
    int consensusTailIndex = getPositionBetween(spermTail2, spermTail3, roiArray);
    XYPoint consensusTail = roiArray.smoothedArray[consensusTailIndex];
    // XYPoint consensusTail = getPositionBetween(spermTail2, spermTail3, roiArray);

    double pathLength = 0;
    double normalisedTailIndex = ((double)consensusTailIndex/(double)roiArray.smoothLength)*100;

    if(spermTail2.getLengthTo(spermTail3) < nucleus.getFeretsDiameter() * 0.3){

       XYPoint prevPoint = new XYPoint(0,0);
       
       for (int i=0; i<roiArray.smoothLength;i++ ) {
          double profileX = ((double)i/(double)roiArray.smoothLength)*100; // normalise to 100 length
          double[] d = new double[] { profileX, roiArray.smoothedArray[i].getInteriorAngle() };
          IJ.append(profileX+"\t"+roiArray.smoothedArray[i].getInteriorAngle(), this.logFile);

          XYPoint thisPoint = new XYPoint(d[0],d[1]);
          pathLength += thisPoint.getLengthTo(prevPoint);
          prevPoint = thisPoint;
          rt.add(d);
        }
        IJ.append("", this.logFile);

    } else {
      IJ.log("    Cannot assign tail position");
      this.nucleiFailedOnTail++;
      // failureReason += "Tail ";
      failureReason = failureReason | this.FAILURE_TAIL;
      nucleusPassedChecks = false;
    }
   

   	// EXPERIMENT WITH SPLINE FITTING
   	// roiArray.updateSplineArray();
   	// XYPoint[] splines = new XYPoint[roiArray.smoothLength];
   	// SplineFitter spf = new SplineFitter(roiArray.getXasArray(), roiArray.getYasArray(), roiArray.getLength()+1, true); // true  = closed curve
   	// for(int i=0; i<roiArray.smoothLength;i++) {
   	// 	double profileX = ((double)i/(double)this.smoothLength)*100;
   	// 	double splineY = spf.evalSpline(roiArray.smoothedArray[i].getX());
   	// 	XYPoint p = new XYPoint(roiArray.smoothedArray[i].getX(), splineY);
   	// 	// IJ.log("    Spline: "+splineY);
   	// 	splines[i] = p;
   	// }
   	// roiArray.setSplineArray(splines);
    

    // Include tip, CoM, tail
    roiArray.printLogFile();


    // rotate the ROI to put the tail at the top/bottom
    // save/export the rotated coordinates for schematic plot

    // double rotationAngleD = findRotationAngle(consensusTail, nucleusCoM);
    // double rotationAngleR = Math.toRadians(rotationAngleD); 
    // // IJ.log("    Rotate by "+rotationAngleD);
    // final AffineTransform at = AffineTransform.getRotateInstance(rotationAngleR,
    //                                                               nucleusCoM.getX(), 
    //                                                               nucleusCoM.getY() 
    // );
    // PathIterator p = roiArray.getPolygon().getPathIterator(at);
    // // go through path iterator and export points
    // String file = roiArray.getPathWithoutExtension()+"\\"+roiArray.getNucleusNumber()+".schematic";
    // File f = new File(file);
    // if(f.exists()){
    //   f.delete();
    // }

    // FloatPolygon rotatedPolygon = new FloatPolygon();
    // IJ.append("ROT_X\tROT_Y", file);
    // while(!p.isDone()){
    //   double[] coords = new double[2];
    //   int i = p.currentSegment(coords);
    //   if(i==p.SEG_LINETO){
    //     IJ.append(coords[0]+"\t"+coords[1], file);
    //     rotatedPolygon.addPoint(coords[0], coords[1]);
    //   }
    //   p.next();
    // }

    // LOOKS LIKE WE NEED TO MAKE A NEW ROIARRAY BASED ON THE ROTATED POLYGON
    // create a float polygon from the rotated coordinates. Use this to make an roiarray.
    // redo the interpolation and smoothing.
    // get the tip
    // PolygonRoi rotatedRoi = new PolygonRoi(rotatedPolygon, Roi.POLYGON);
    // RoiArray rotatedRoiArray = new RoiArray(rotatedRoi);
    // rotatedRoiArray.setPath(path);
    // rotatedRoiArray.setNucleusNumber(nucleusNumber);
    

    // determine hook from hump
    // the CoM should not have changed, as this was the rotation point
    // the sperm tip will always be the hook side; which way does it point?
    // XYPoint rotatedSpermTip = rotatedRoiArray.findMinimumAngle();
    // rotatedRoiArray.moveIndexToArrayStart(rotatedSpermTip.getIndex());
    // if(rotatedSpermTip.getX() < nucleusCoM.getX()){
    //   // rotatedRoiArray.flipXAroundPoint(nucleusCoM);
    // }
    // rotatedRoiArray.printLogFile();


    // get the acrosomal curve

    // find the signal positions
    // within nuclear roi, analyze particles in colour channels
    // RoiManager   redSignalsInImage = findSignalInNucleus(smallRegion, 0);
    // RoiManager greenSignalsInImage = findSignalInNucleus(smallRegion, 1);
    // get signal roi


    // find lectin stains

    // draw the points considered as sperm tails
    ip.setLineWidth(5);
    ip.setColor(Color.CYAN);
    ip.drawDot(consensusTail.getXAsInt(), consensusTail.getYAsInt());

    ip.setLineWidth(3);
    ip.setColor(Color.GRAY);
    ip.drawDot(spermTail2.getXAsInt(), spermTail2.getYAsInt());
    ip.setColor(Color.ORANGE);
    ip.drawDot(spermTail3.getXAsInt(), spermTail3.getYAsInt());

    String saveFolder = createImageDirectory(roiArray.getPathWithoutExtension());
    IJ.saveAsTiff(smallRegion, saveFolder+"\\"+roiArray.getNucleusNumber()+".tiff");


    // rotate the image to provide a consistent view
    // ip.setInterpolationMethod(ImageProcessor.BILINEAR);
    // ip.setBackgroundValue(0);
    // double diagonal = Math.sqrt( (ip.getWidth()*ip.getWidth() + ip.getHeight()*ip.getHeight() ) );
    // Double obj = new Double(diagonal);
    // int diag = obj.intValue();
    // ip.setRoi(0,0,diag,diag);
    // ip.rotate(rotationAngle);
    // ImagePlus finalImage = new ImagePlus("Image", ip);
    // IJ.saveAsTiff(finalImage, saveFolder+"\\"+roiArray.getNucleusNumber()+".final.tiff");

    if(pathLength > MAXIMUM_PATH_LENGTH){ // skip nuclei with poor thresholding
      IJ.log("    Nucleus failed on thresholding");
      this.nucleiFailedOther++;
      // failureReason += "Threshold ";
      failureReason = failureReason | this.FAILURE_THRESHOLD;
      nucleusPassedChecks = false;
    }


    // if everything checks out, add the measured parameters to the global pool
    if(nucleusPassedChecks){
      this.perimeterArray.add(blueResults.getValue("Perim.",0) );
      this.areaArray.add(blueResults.getValue("Area",0) );
      this.feretArray.add(blueResults.getValue("Feret",0) );
      this.nucleusArray.add(path+"-"+nucleusNumber);
      this.pathLengthArray.add(pathLength);
      this.tailIndexArray.add(normalisedTailIndex);
      return rt;
    } else {
      IJ.append(  failureReason+"\t"+
                  blueResults.getValue("Perim.",0)+"\t"+
                  blueResults.getValue("Area",0)+"\t"+
                  blueResults.getValue("Feret",0)+"\t"+
                  pathLength+"\t"+
                  normalisedTailIndex+"\t"+
                  path+"-"+nucleusNumber, this.failedFile);
      return new ArrayList(0);
    }

  }
  /*
    For two XYPoints in an RoiArray, find the point that lies halfway between them
    Used for obtaining a consensus between potential tail positions
  */
  public int getPositionBetween(XYPoint pointA, XYPoint pointB, RoiArray array){

    int a = 0;
    int b = 0;
    // find the indices that correspond on the array
    for(int i = 0; i<array.smoothLength; i++){
        if(array.smoothedArray[i].overlaps(pointA)){
          a = i;
        }
        if(array.smoothedArray[i].overlaps(pointB)){
          b = i;
        }
    }
    // IJ.log(    "Found a="+a+", b="+b);
    // get the midpoint
    int mid = (int)Math.floor( (a+b) /2);
    // IJ.log(    "Consensus tail at "+mid+": "+array.smoothedArray[mid].toString());
    // return array.smoothedArray[mid];
    return mid;
  }

  /*
    Find the angle that the nucleus must be rotated to make the CoM-tail vertical
    Returns an angle
  */
  public double findRotationAngle(XYPoint tail, XYPoint centre){
    XYPoint end = new XYPoint(tail.getXAsInt(),0);

    float[] xpoints = { (float) end.getX(), (float) tail.getX(), (float) centre.getX()};
    float[] ypoints = { (float) end.getY(), (float) tail.getY(), (float) centre.getY()};
    PolygonRoi roi = new PolygonRoi(xpoints, ypoints, 3, Roi.ANGLE);

   // measure the angle of the line
   double angle = roi.getAngle();

     if(centre.getX() < tail.getX()){
      return angle;
     } else {
      return 0-angle;
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

    double minSize = 400;
    double maxSize = 3000;
    RoiManager manager = new RoiManager(true);
    ChannelSplitter cs = new ChannelSplitter();
    ImagePlus[] channels = cs.split(image);
    ImagePlus imp = channels[channel];
    
    // threshold
    ImageProcessor ip = imp.getChannelProcessor();
    ip.threshold(SIGNAL_THRESHOLD);
    ip.invert();

    // run the particle analyser
    ResultsTable rt = new ResultsTable();
    ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER, 
    										   ParticleAnalyzer.CENTER_OF_MASS | ParticleAnalyzer.AREA,
                                               rt, 
                                               minSize, 
                                               maxSize);
    try {
      pa.setRoiManager(manager);
      boolean success = pa.analyze(imp);
      if(success){
        IJ.log("    Found "+manager.getCount()+ " signals in channel "+channel);
        // rt.show("Title");
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

    // public boolean isMidpoint(){
    //   return this.isMidpoint;
    // }

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


  class RoiArray {
  
    private int nucleusNumber; // the number of the nucleus in the current image
    private int windowSize = 23; // default size, can be overridden if needed
    private int minimaCount; // the number of local minima detected in the array
    private int maximaCount; // the number of local minima detected in the array
    private int length;  // the length of the array; shortcut to this.array.length
    private int smoothLength = 0; // the length of the smoothed array; shortcut to this.smoothedArray.length
    private int minimaLookupDistance = 5; // the points ahead and behind to check when finding local minima and maxima
    private int blockCount = 0; // the number of delta blocks detected
    private int DELTA_WINDOW_MIN = 5; // the minimum number of points required in a delta block

    private double medianAngle; // the median angle from XYPoint[] smoothedArray

    private XYPoint[] array; // the points from the polygon made from the input roi
    private XYPoint[] smoothedArray; // the interpolated points from the input polygon. Most calculations use this.
    private XYPoint[] splineArray; // spline values. Currently not used.
    
    private String imagePath; // the path to the image being analysed

    private boolean minimaCalculated = false; // has detectLocalMinima been run
    private boolean maximaCalculated = false; // has detectLocalMaxima been run
    private boolean anglesCalculated = false; // has makeAngleArray been run
    
    private Roi roi; // the original ROI
    private Polygon polygon; // the ROI converted to a polygon; source of XYPoint[] array

    private FloatPolygon smoothedPolygon; // the interpolated polygon; source of XYPoint[] smoothedArray
    
    public RoiArray (Roi roi) { // construct from an roi

      // get the polygon from the roi
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
  			float profileX = ((float)i/(float)this.smoothLength)*100; // normalise to 100 length
  			newArray[i] = profileX;
  		}
  		newArray[this.smoothLength] = newArray[0];
  		return newArray;
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

      // make a segmented line
      float[] xpoints = { (float) pointBefore.getX(), (float) point.getX(), (float) pointAfter.getX()};
      float[] ypoints = { (float) pointBefore.getY(), (float) point.getY(), (float) pointAfter.getY()};
      PolygonRoi roi = new PolygonRoi(xpoints, ypoints, 3, Roi.ANGLE);


      // measure the angle of the line
      double angle = roi.getAngle();

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
      ArrayList results = new ArrayList<XYPoint>(0);
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
        double profileX = ((double)i/(double)this.smoothLength)*100; // normalise to 100 length
        // IJ.log("i: "+i+" length: "+this.smoothLength+" profile: "+profileX);

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
                  profileX,
                  path);
      }
    }
  }
}
