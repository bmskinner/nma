/* 
  -----------------------
  ASYMMETRIC NUCLEUS COLLECTION CLASS
  -----------------------
  An asymetric nucleus has a head and a tail; sperm nuclei
  are an example of this. This class enables offsets to be 
  calculated based on median normalised curves from head and
  tail points, plus rotation of nuclei for display in composite
  images
*/

package no.collections;

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
import no.nuclei.*;
import no.nuclei.sperm.*;
import no.components.*;
import no.utility.*;

public class AsymmetricNucleusCollection 
	extends no.collections.NucleusCollection
{

	// Chart drawing parameters
  public static final int CHART_WINDOW_HEIGHT     = 400;
  public static final int CHART_WINDOW_WIDTH      = 500;
  public static final int CHART_TAIL_BOX_Y_MIN    = 325;
  public static final int CHART_TAIL_BOX_Y_MID    = 340;
  public static final int CHART_TAIL_BOX_Y_MAX    = 355;
  public static final int CHART_SIGNAL_Y_LINE_MIN = 275;
  public static final int CHART_SIGNAL_Y_LINE_MAX = 315;

  // failure  codes
  public static final int FAILURE_HEAD = 128;
  public static final int FAILURE_TAIL = 256;

	private String logMedianFromHeadFile = "logMediansFromHead"; // output medians
  private String logMedianFromTailFile = "logMediansFromTail"; // output medians

	private double[] normalisedMedianProfileFromHead; // this is an array of 200 angles
  private double[] normalisedMedianProfileFromTail; // this is an array of 200 angles

	private boolean differencesCalculated = false;

  private Map<Double, Collection<Double>> normalisedProfilesFromHead = new HashMap<Double, Collection<Double>>();
  private Map<Double, Collection<Double>> normalisedProfilesFromTail = new HashMap<Double, Collection<Double>>();

  // store indexes of the head and tail in the median profile
	private int medianProfileTailIndex;
  private int medianProfileHeadIndex;

  // plots to draw the profiles
  private Plot  rawProfileFromHeadPlot;
  private Plot normProfileFromHeadPlot;
  private Plot  rawProfileFromTailPlot;
  private Plot normProfileFromTailPlot;

  // plot windows to display the profiles
  private PlotWindow  rawProfileFromHeadPlotWindow;
  private PlotWindow normProfileFromHeadPlotWindow;
  private PlotWindow  rawProfileFromTailPlotWindow;
  private PlotWindow normProfileFromTailPlotWindow;

  public AsymmetricNucleusCollection(File folder, String type){
  		super(folder, type);
  }

  public void measureProfilePositions(){

    this.createProfileAggregateFromTail();
    this.createProfileAggregateFromHead();
    this.drawProfilePlots();
    this.drawNormalisedMedianLineFromTail();
    this.drawNormalisedMedianLineFromHead();
    this.calculateDifferencesToMedianProfiles();
    this.exportProfilePlots();
  }

  @Override
  public void annotateAndExportNuclei(){
    this.exportNuclearStats("logStats");
    this.exportClusteringProfiles("logClusters");
    this.annotateImagesOfNuclei();
    this.exportAnnotatedNuclei();
    this.exportCompositeImage("composite");
  }


  /*
    -----------------------
    General getters
    -----------------------
  */
  public String getLogMedianFromHeadFile(){
    return this.logMedianFromTailFile;
  }

  public String getLogMedianFromTailFile(){
    return this.logMedianFromTailFile;
  }

  public Plot getRawProfilesFromHeadPlot(){
    return this.rawProfileFromHeadPlot;
  }

  public Plot getRawProfilesFromTailPlot(){
    return this.rawProfileFromTailPlot;
  }

  public Plot getNormProfilesFromHeadPlot(){
    return this.normProfileFromHeadPlot;
  }

  public Plot getNormProfilesFromTailPlot(){
    return this.normProfileFromTailPlot;
  }

  public PlotWindow getRawProfilesFromHeadPlotWindow(){
    return this.rawProfileFromHeadPlotWindow;
  }

  public PlotWindow getRawProfilesFromTailPlotWindow(){
    return this.rawProfileFromTailPlotWindow;
  }

  public PlotWindow getNormProfilesFromHeadPlotWindow(){
    return this.normProfileFromHeadPlotWindow;
  }

  public PlotWindow getNormProfilesFromTailPlotWindow(){
    return this.normProfileFromTailPlotWindow;
  }

  public int getMedianProfileTailIndex(){
    return this.medianProfileTailIndex;
  }

  public int getMedianProfileHeadIndex(){
    return this.medianProfileHeadIndex;
  }

  public boolean isDifferencesCalculated(){
    return this.differencesCalculated;
  }

  public void setDifferencesCalculated(boolean b){
    this.differencesCalculated = b;
  }

  public double[] getMedianProfileFromHead(){
    return this.normalisedMedianProfileFromHead;
  }

  public double[] getMedianProfileFromTail(){
    return this.normalisedMedianProfileFromTail;
  }

  private Map<Double, Collection<Double>> getNormalisedProfilesFromTail(){
    return this.normalisedProfilesFromTail;
  }

  private Map<Double, Collection<Double>> getNormalisedProfilesFromHead(){
    return this.normalisedProfilesFromHead;
  }

  /*
    -----------------------
    Get values relating to
    nucleus profiles
    -----------------------
  */

  public int[] getTailIndexes(){
    int[] d = new int[this.getNucleusCount()];

    for(int i=0;i<this.getNucleusCount();i++){
      AsymmetricNucleus n = (AsymmetricNucleus) this.getNucleus(i);
      d[i] = n.getTailIndex();
    }
    return d;
  }

  public double[] getNormalisedTailIndexesFromHead(){
    double[] d = new double[this.getNucleusCount()];

    for(int i=0;i<this.getNucleusCount();i++){

      AsymmetricNucleus n = (AsymmetricNucleus) this.getNucleus(i);
      int offset = NuclearOrganisationUtility.wrapIndex( n.getTailIndex() - n.getHeadIndex(), n.getLength() );
      d[i] = ( (double) offset / (double) n.getLength() ) * 100;
    }
    return d;
  }

  public double[] getNormalisedHeadIndexesFromTail(){
    double[] d = new double[this.getNucleusCount()];

    for(int i=0;i<this.getNucleusCount();i++){

      AsymmetricNucleus n = (AsymmetricNucleus) this.getNucleus(i);
      int offset = NuclearOrganisationUtility.wrapIndex( n.getHeadIndex() - n.getTailIndex(), n.getLength() );
      d[i] = ( (double) offset / (double) n.getLength() ) * 100;
    }
    return d;
  }

  public AsymmetricNucleus getNucleusMostSimilarToMedian(){
  	AsymmetricNucleus n = (AsymmetricNucleus) this.getNuclei().get(0); // default to the first nucleus

  	double difference = 7000;
  	for(int i=0;i<this.getNucleusCount();i++){
      AsymmetricNucleus p = (AsymmetricNucleus)this.getNucleus(i);
      if(p.getDifferenceToMedianProfileFromTail()<difference){
      	difference = p.getDifferenceToMedianProfileFromTail();
      	n = p;
      }
    }
    return n;
  }

  public double[] getDifferencesToMedianFromTail(){
    double[] d = new double[this.getNucleusCount()];

    for(int i=0;i<this.getNucleusCount();i++){

      AsymmetricNucleus n = (AsymmetricNucleus) this.getNucleus(i);
      d[i] = n.getDifferenceToMedianProfileFromTail();
    }
    return d;
  }

  public double[] getDifferencesToMedianFromHead(){
    double[] d = new double[this.getNucleusCount()];

    for(int i=0;i<this.getNucleusCount();i++){

      AsymmetricNucleus n = (AsymmetricNucleus) this.getNucleus(i);
      d[i] = n.getDifferenceToMedianProfileFromHead();
    }
    return d;
  }

  /*
		Interpolate the median profile to match the length of the most-median nucleus
		Store the angle profile as a double[] to feed into the curve refolder
  */
	public double[] getMedianTargetCurve(Nucleus n){
		double[] targetMedianCurve = interpolateMedianToLength(n.getLength(), this.getMedianProfileFromTail());
		return targetMedianCurve;
	}	

  public double[] getHeadToTailDistances(){
    double[] d = new double[this.getNucleusCount()];
    for(int i=0;i<this.getNucleusCount();i++){
      AsymmetricNucleus n = (AsymmetricNucleus)this.getNucleus(i);
      d[i] = n.getHead().getLengthTo(n.getTail());
    }
    return d;
  }

  /*
    -----------------------
    Setters
    -----------------------
  */

  public void setNormalisedMedianProfileFromHead(double[] d){
		this.normalisedMedianProfileFromHead = d;
	}

  public void setNormalisedMedianProfileFromTail(double[] d){
    this.normalisedMedianProfileFromTail = d;
  }

  public void setMedianProfileTailIndex(int i){
    this.medianProfileTailIndex = i;
  }

  public void setMedianProfileHeadIndex(int i){
    this.medianProfileHeadIndex = i;
  }

  /*
    -----------------------
    Filter nuclei on
    assymetric specific features
    -----------------------
  */



  /*
    -----------------------
    Create and manipulate
    aggregate profiles. These
    can be centred on head or tail
    -----------------------
  */

  public void createProfileAggregateFromHead(){

  	for(int i=0;i<this.getNucleusCount();i++){

      AsymmetricNucleus n = (AsymmetricNucleus)this.getNucleus(i);

      double[] xvalues = n.getNormalisedProfilePositions();
      double[] yvalues = n.getAngleProfile().getInteriorAngles(n.getHeadIndex());

	  	updateProfileAggregate(xvalues, yvalues, this.getNormalisedProfilesFromHead()); 
	  }
  }

  public void createProfileAggregateFromTail(){

  	for(int i=0;i<this.getNucleusCount();i++){

      AsymmetricNucleus n = (AsymmetricNucleus)this.getNucleus(i);

	  	double[] xvalues = n.getNormalisedProfilePositions();
      // get the angles starting from the tail index
      double[] yvalues = n.getAngleProfile().getInteriorAngles(n.getTailIndex());

	  	// double[] yvalues = n.getNormalisedYPositionsFromTail();
	  	updateProfileAggregate(xvalues, yvalues, this.getNormalisedProfilesFromTail()); 
	  }
  }

  // /*
  //   For each nucleus in the collection see if there is a differences to the given median
  // */
  public void calculateDifferencesToMedianProfiles(){

    for(int i= 0; i<this.getNucleusCount();i++){ // for each nucleus
      AsymmetricNucleus n = (AsymmetricNucleus)this.getNucleus(i);
      
      double differenceFromHead = n.calculateDifferenceToMedianProfile(this.getMedianProfileFromHead());
      n.setDifferenceToMedianProfileFromHead(differenceFromHead);

      double differenceFromTail = n.calculateDifferenceToMedianProfile(this.getMedianProfileFromTail());
      n.setDifferenceToMedianProfileFromTail(differenceFromTail);
    }
  }
	 
  @Override 
  public void measureNuclearOrganisation(){

    if(this.getRedSignalCount()>0 || this.getGreenSignalCount()>0){

      for(int i= 0; i<this.getNucleusCount();i++){
        AsymmetricNucleus n = (AsymmetricNucleus)this.getNucleus(i);
         n.calculateSignalAnglesFromPoint(n.getTail());
      }
      this.exportSignalStats();
      this.addSignalsToProfileChartFromHead();
      this.addSignalsToProfileChartFromTail();

      this.exportProfilePlots();
    }
  }


  /*
    -----------------------
    Annotate images
    -----------------------
  */
  
  public void annotateImagesOfNuclei(){
    IJ.log("    Annotating images ("+this.getType()+")...");
    for(int i=0; i<this.getNucleusCount();i++){
      AsymmetricNucleus n = (AsymmetricNucleus)this.getNucleus(i);
      n.annotateFeatures();
    }
     IJ.log("    Annotation complete");
  }


	/*
    -----------------------
    Export data
    -----------------------
  */

  public void exportInterpolatedMedians(double[] medianProfile){

    String logFile = makeGlobalLogFile("logOffsets");

    IJ.append("INDEX\tANGLE", logFile);
    for(int i=0;i<medianProfile.length;i++){
      IJ.append(i+"\t"+medianProfile[i], logFile);
    }
    IJ.append("", logFile);
  }

  public void exportOffsets(double[] d){

  	String logFile = makeGlobalLogFile("logOffsets");

    IJ.append("OFFSET\tDIFFERENCE", logFile);

    for(int i=0;i<d.length;i++){
      IJ.append(i+"\t"+d[i], logFile);
    }
    IJ.append("", logFile);
  }

  @Override
  public void exportNuclearStats(String filename){

    Map<String, List<String>> stats = super.calculateNuclearStats();

    String[] index  = NuclearOrganisationUtility.getStringFromInt(this.getTailIndexes());
    String[] diff   = NuclearOrganisationUtility.getStringFromDouble(this.getDifferencesToMedianFromTail());
    String[] points = NuclearOrganisationUtility.getStringFromDouble(this.getMedianDistanceBetweenPoints());

    stats.put("NORM_TAIL_INDEX",                Arrays.asList(index ));
    stats.put("DIFFERENCE_TO_MEDIAN_PROFILE",   Arrays.asList(diff  ));
    stats.put("MEDIAN_DISTANCE_BETWEEN_POINTS", Arrays.asList(points));

    exportStats(stats, filename);
  }

  public void exportCompositeImage(String filename){

    // foreach nucleus
    // createProcessor (500, 500)
    // sertBackgroundValue(0)
    // paste in old image at centre
    // insert(ImageProcessor ip, int xloc, int yloc)
    // rotate about CoM (new position)
    // display.
    if(this.getNucleusCount()==0){
      return;
    }
    IJ.log("    Creating composite image ("+this.getType()+")...");
    

    int totalWidth = 0;
    int totalHeight = 0;

    int boxWidth  = (int)(this.getMedianNuclearPerimeter()/1.4);
    int boxHeight = (int)(this.getMedianNuclearPerimeter()/1.2);

    int maxBoxWidth = boxWidth * 5;
    int maxBoxHeight = (boxHeight * (int)(Math.ceil(this.getNucleusCount()/5)) + boxHeight );

    ImagePlus finalImage = new ImagePlus("Final image", new BufferedImage(maxBoxWidth, maxBoxHeight, BufferedImage.TYPE_INT_RGB));
    ImageProcessor finalProcessor = finalImage.getProcessor();
    finalProcessor.setBackgroundValue(0);

    for(int i=0; i<this.getNucleusCount();i++){
      
      AsymmetricNucleus n = (AsymmetricNucleus)this.getNucleus(i);
      String path = n.getAnnotatedImagePath();

      try {
        Opener localOpener = new Opener();
        ImagePlus image = localOpener.openImage(path);
        ImageProcessor ip = image.getProcessor();
        int width  = ip.getWidth();
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
        IJ.append("Error adding image to composite: "+e, this.getDebugFile().getAbsolutePath());
        IJ.append("  "+getType(), this.getDebugFile().getAbsolutePath());
        IJ.append("  "+path, this.getDebugFile().getAbsolutePath());
      }     
    }
    // finalImage.show();
    IJ.saveAsTiff(finalImage, this.getFolder()+File.separator+filename+"."+getType()+".tiff");
    IJ.log("    Composite image created");
  }

  public void exportClusteringProfiles(String filename){

    String statsFile = makeGlobalLogFile(filename);

    StringBuilder outLine = new StringBuilder();
    outLine.append("PATH\tAREA\tPERIMETER\tFERET\tPATH_LENGTH\tDIFFERENCE\tFAILURE_CODE\tHEAD_TO_TAIL\t");

    IJ.log("    Exporting clustering profiles ("+this.getType()+")...");
    double[] areas        = this.getAreas();
    double[] perims       = this.getPerimeters();
    double[] ferets       = this.getFerets();
    double[] pathLengths  = this.getPathLengths();
    double[] differences  = this.getDifferencesToMedianFromTail();
    double[] headToTail   = this.getHeadToTailDistances();
    String[] paths        = this.getNucleusPaths();

    double maxPerim = NuclearOrganisationUtility.getMax(perims); // add column headers
    for(int i=0;i<maxPerim;i++){
      outLine.append(i+"\t");
    }
    outLine.append("\n");


    // export the profiles for each nucleus
    for(int i=0; i<this.getNucleusCount();i++){

      outLine.append(paths[i]      +"\t"+
                    areas[i]      +"\t"+
                    perims[i]     +"\t"+
                    ferets[i]     +"\t"+
                    pathLengths[i]+"\t"+
                    differences[i]+"\t"+
                    headToTail[i] +"\t");

      AsymmetricNucleus n = (AsymmetricNucleus)this.getNucleus(i);
      double[] profile = n.getAngleProfile().getInteriorAngles(n.getTailIndex());
      for(int j=0;j<profile.length;j++){
        outLine.append(profile[j]+"\t");
      }
      outLine.append("\n");
    }
    IJ.append(  outLine.toString(), statsFile);
    IJ.log("    Cluster export complete");
  }

  /*
    -----------------------
    Draw plots
    -----------------------
  */

  /*
    Create the plots that we will be using
    Get the x max and min as needed from aggregate stats
  */
  public void preparePlots(){

    this.rawProfileFromHeadPlot = new Plot( "Raw head-indexed plot",
                                "Position",
                                "Angle", Plot.Y_GRID | Plot.X_GRID);
    rawProfileFromHeadPlot.setLimits(0,this.getMaxProfileLength(),-50,360);
    rawProfileFromHeadPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
    rawProfileFromHeadPlot.setYTicks(true);
    rawProfileFromHeadPlot.setColor(Color.BLACK);
    rawProfileFromHeadPlot.drawLine(0, 180, this.getMaxProfileLength(), 180); 
    rawProfileFromHeadPlot.setColor(Color.LIGHT_GRAY);


    normProfileFromHeadPlot = new Plot("Normalised head-indexed plot",
                                "Position",
                                "Angle", Plot.Y_GRID | Plot.X_GRID);
    normProfileFromHeadPlot.setLimits(0,100,-50,360);
    normProfileFromHeadPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
    normProfileFromHeadPlot.setYTicks(true);
    normProfileFromHeadPlot.setColor(Color.BLACK);
    normProfileFromHeadPlot.drawLine(0, 180, 100, 180); 
    normProfileFromHeadPlot.setColor(Color.LIGHT_GRAY);


    this.rawProfileFromTailPlot = new Plot( "Raw tail-indexed plot",
                                "Position",
                                "Angle", Plot.Y_GRID | Plot.X_GRID);
    rawProfileFromTailPlot.setLimits( 0, this.getMaxProfileLength(),
                                -50,360);
    rawProfileFromTailPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
    rawProfileFromTailPlot.setYTicks(true);
    rawProfileFromTailPlot.setColor(Color.BLACK);
    rawProfileFromTailPlot.drawLine(0, 180, this.getMaxProfileLength(), 180); 
    rawProfileFromTailPlot.setColor(Color.LIGHT_GRAY);

    this.normProfileFromTailPlot = new Plot("Normalised tail-indexed plot", "Position", "Angle", Plot.Y_GRID | Plot.X_GRID);
    normProfileFromTailPlot.setLimits(0,100,-50,360);
    normProfileFromTailPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
    normProfileFromTailPlot.setYTicks(true);
    normProfileFromTailPlot.setColor(Color.BLACK);
    normProfileFromTailPlot.drawLine(0, 180, 100, 180); 
    normProfileFromTailPlot.setColor(Color.LIGHT_GRAY);
  }

  /*
    Create the charts of the profiles of the nuclei within this collecion.
    Currently drawing: 
      Head-aligned raw X
      Tail-aligned raw X
      Head-aligned normalised X
      Tail-aligned normalised X

  */
  public void drawProfilePlots(){

    preparePlots();

    for(int i=0;i<this.getNucleusCount();i++){

      AsymmetricNucleus n = (AsymmetricNucleus) this.getNucleus(i);
      
      double[] xPointsRaw  = n.getRawProfilePositions();
      double[] xPointsNorm = n.getNormalisedProfilePositions();

      double[] anglesFromHead = n.getAngleProfile().getInteriorAngles(n.getHeadIndex());
      double[] anglesFromTail = n.getAngleProfile().getInteriorAngles(n.getTailIndex());

      this.rawProfileFromHeadPlot.setColor(Color.LIGHT_GRAY);
      this.rawProfileFromHeadPlot.addPoints(xPointsRaw, anglesFromHead, Plot.LINE);

      this.normProfileFromHeadPlot.setColor(Color.LIGHT_GRAY);
      this.normProfileFromHeadPlot.addPoints(xPointsNorm, anglesFromHead, Plot.LINE);

      this.rawProfileFromTailPlot.setColor(Color.LIGHT_GRAY);
      this.rawProfileFromTailPlot.addPoints(xPointsRaw, anglesFromTail, Plot.LINE);

      this.normProfileFromTailPlot.setColor(Color.LIGHT_GRAY);
      this.normProfileFromTailPlot.addPoints(xPointsNorm, anglesFromTail, Plot.LINE);
    }   
  }


  /*
		Draw the median line on the normalised profile
		chart, aligned to the sperm tip
  */
  public void drawNormalisedMedianLineFromHead(){
    // output the final results: calculate median positions

    ArrayList<Double[]> medians = calculateMediansAndQuartilesOfProfile( this.getNormalisedProfilesFromHead() );
    this.exportMediansAndQuartilesOfProfile(medians, this.getLogMedianFromHeadFile());

    double[] xmedians        =  NuclearOrganisationUtility.getDoubleFromDouble( medians.get(0) );
    double[] ymedians        =  NuclearOrganisationUtility.getDoubleFromDouble( medians.get(1) );
    double[] lowQuartiles    =  NuclearOrganisationUtility.getDoubleFromDouble( medians.get(2) );
    double[] uppQuartiles    =  NuclearOrganisationUtility.getDoubleFromDouble( medians.get(3) );
    double[] tenQuartiles    =  NuclearOrganisationUtility.getDoubleFromDouble( medians.get(4) );
    double[] ninetyQuartiles =  NuclearOrganisationUtility.getDoubleFromDouble( medians.get(5) );

    setNormalisedMedianProfileFromHead(ymedians);


    // get the tail positions with the head offset applied
    double[] xTails = this.getNormalisedTailIndexesFromHead();
    double[] yTails = new double[xTails.length];
    Arrays.fill(yTails, CHART_TAIL_BOX_Y_MID); // all dots at y=300
    normProfileFromHeadPlot.setColor(Color.LIGHT_GRAY);
    normProfileFromHeadPlot.addPoints(xTails, yTails, Plot.DOT);

    // median tail positions
    double tailQ50 = NuclearOrganisationUtility.quartile(xTails, 50);
    double tailQ25 = NuclearOrganisationUtility.quartile(xTails, 25);
    double tailQ75 = NuclearOrganisationUtility.quartile(xTails, 75);

    // add the median lines to the chart
    normProfileFromHeadPlot.setColor(Color.BLACK);
    normProfileFromHeadPlot.setLineWidth(3);
    normProfileFromHeadPlot.addPoints(xmedians, ymedians, Plot.LINE);
    normProfileFromHeadPlot.setColor(Color.DARK_GRAY);
    normProfileFromHeadPlot.setLineWidth(2);
    normProfileFromHeadPlot.addPoints(xmedians, lowQuartiles, Plot.LINE);
    normProfileFromHeadPlot.addPoints(xmedians, uppQuartiles, Plot.LINE);

    normProfileFromHeadPlot.setColor(Color.DARK_GRAY);
    normProfileFromHeadPlot.setLineWidth(1);
    normProfileFromHeadPlot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MAX, tailQ75, CHART_TAIL_BOX_Y_MAX);
    normProfileFromHeadPlot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MIN, tailQ75, CHART_TAIL_BOX_Y_MIN);
    normProfileFromHeadPlot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MIN, tailQ25, CHART_TAIL_BOX_Y_MAX);
    normProfileFromHeadPlot.drawLine(tailQ75, CHART_TAIL_BOX_Y_MIN, tailQ75, CHART_TAIL_BOX_Y_MAX);
    normProfileFromHeadPlot.drawLine(tailQ50, CHART_TAIL_BOX_Y_MIN, tailQ50, CHART_TAIL_BOX_Y_MAX);
  }

  /*
		Draw the median line on the normalised profile
		chart, aligned to the sperm tail
  */
  public void drawNormalisedMedianLineFromTail(){

    ArrayList<Double[]> medians = calculateMediansAndQuartilesOfProfile( this.getNormalisedProfilesFromTail() );
    this.exportMediansAndQuartilesOfProfile(medians, this.getLogMedianFromTailFile());

    double[] xmedians        =  NuclearOrganisationUtility.getDoubleFromDouble( medians.get(0) );
    double[] ymedians        =  NuclearOrganisationUtility.getDoubleFromDouble( medians.get(1) );
    double[] lowQuartiles    =  NuclearOrganisationUtility.getDoubleFromDouble( medians.get(2) );
    double[] uppQuartiles    =  NuclearOrganisationUtility.getDoubleFromDouble( medians.get(3) );
    double[] tenQuartiles    =  NuclearOrganisationUtility.getDoubleFromDouble( medians.get(4) );
    double[] ninetyQuartiles =  NuclearOrganisationUtility.getDoubleFromDouble( medians.get(5) );

    setNormalisedMedianProfileFromTail(ymedians);

    // get the tail positions with the head offset applied
    double[] xTails = this.getNormalisedHeadIndexesFromTail();
    double[] yTails = new double[xTails.length];
    Arrays.fill(yTails, CHART_TAIL_BOX_Y_MID); // all dots at y=300
    normProfileFromTailPlot.setColor(Color.LIGHT_GRAY);
    normProfileFromTailPlot.addPoints(xTails, yTails, Plot.DOT);

    // median tail positions
    double tailQ50 = NuclearOrganisationUtility.quartile(xTails, 50);
    double tailQ25 = NuclearOrganisationUtility.quartile(xTails, 25);
    double tailQ75 = NuclearOrganisationUtility.quartile(xTails, 75);

    // add the median lines to the chart
    normProfileFromTailPlot.setColor(Color.BLACK);
    normProfileFromTailPlot.setLineWidth(3);
    normProfileFromTailPlot.addPoints(xmedians, ymedians, Plot.LINE);
    normProfileFromTailPlot.setColor(Color.DARK_GRAY);
    normProfileFromTailPlot.setLineWidth(2);
    normProfileFromTailPlot.addPoints(xmedians, lowQuartiles, Plot.LINE);
    normProfileFromTailPlot.addPoints(xmedians, uppQuartiles, Plot.LINE);

    normProfileFromTailPlot.setColor(Color.DARK_GRAY);
    normProfileFromTailPlot.setLineWidth(1);
    normProfileFromTailPlot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MAX, tailQ75, CHART_TAIL_BOX_Y_MAX);
    normProfileFromTailPlot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MIN, tailQ75, CHART_TAIL_BOX_Y_MIN);
    normProfileFromTailPlot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MIN, tailQ25, CHART_TAIL_BOX_Y_MAX);
    normProfileFromTailPlot.drawLine(tailQ75, CHART_TAIL_BOX_Y_MIN, tailQ75, CHART_TAIL_BOX_Y_MAX);
    normProfileFromTailPlot.drawLine(tailQ50, CHART_TAIL_BOX_Y_MIN, tailQ50, CHART_TAIL_BOX_Y_MAX);
  }

  public void addSignalsToProfileChartFromHead(){
    // PlotWindow normProfileFromTipWindow; normProfileFromHeadPlot
    // for each signal in each nucleus, find index of point. Draw dot
    // Add the signals to the tip centred profile plot

    normProfileFromHeadPlot.setColor(Color.LIGHT_GRAY);
    normProfileFromHeadPlot.setLineWidth(1);
    normProfileFromHeadPlot.drawLine(0,CHART_SIGNAL_Y_LINE_MIN,100,CHART_SIGNAL_Y_LINE_MIN);
    normProfileFromHeadPlot.drawLine(0,CHART_SIGNAL_Y_LINE_MAX,100,CHART_SIGNAL_Y_LINE_MAX);

    for(int i= 0; i<this.getNucleusCount();i++){

      AsymmetricNucleus n = (AsymmetricNucleus)this.getNucleus(i);

      ArrayList<NuclearSignal> redSignals = n.getRedSignals();
      if(redSignals.size()>0){

        ArrayList<Double> redPoints = new ArrayList<Double>(0);
        ArrayList<Double> yPoints   = new ArrayList<Double>(0);

        for(int j=0; j<redSignals.size();j++){

          NucleusBorderPoint border = redSignals.get(j).getClosestBorderPoint();
          for(int k=0; k<n.getLength();k++){

            if(n.getBorderPoint(k).overlaps(border)){
              redPoints.add( n.getNormalisedProfilePositions()[k] );
              double yPosition = CHART_SIGNAL_Y_LINE_MIN + ( redSignals.get(j).getFractionalDistanceFromCoM() * ( CHART_SIGNAL_Y_LINE_MAX - CHART_SIGNAL_Y_LINE_MIN) ); // 
              yPoints.add(yPosition);
            }
          }
        }
        normProfileFromHeadPlot.setColor(Color.RED);
        normProfileFromHeadPlot.setLineWidth(2);
        normProfileFromHeadPlot.addPoints(redPoints, yPoints, Plot.DOT);
      }
    }
  }

  public void addSignalsToProfileChartFromTail(){
  	// Add the signals to the tail centred profile plot
    normProfileFromTailPlot.setColor(Color.LIGHT_GRAY);
    normProfileFromTailPlot.setLineWidth(1);
    normProfileFromTailPlot.drawLine(0,CHART_SIGNAL_Y_LINE_MIN,100,CHART_SIGNAL_Y_LINE_MIN);
    normProfileFromTailPlot.drawLine(0,CHART_SIGNAL_Y_LINE_MAX,100,CHART_SIGNAL_Y_LINE_MAX);

    for(int i= 0; i<this.getNucleusCount();i++){ // for each roi

      AsymmetricNucleus n = (AsymmetricNucleus)this.getNucleus(i);

      ArrayList<NuclearSignal> redSignals = n.getRedSignals();
      if(redSignals.size()>0){

        ArrayList<Double> redPoints = new ArrayList<Double>(0);
        ArrayList<Double> yPoints   = new ArrayList<Double>(0);

        for(int j=0; j<redSignals.size();j++){

          NucleusBorderPoint border = redSignals.get(j).getClosestBorderPoint();
          for(int k=0; k<n.getLength();k++){

            if(n.getBorderPoint(k).overlaps(border)){
              // IJ.log("Found closest border: "+i+" : "+j);
              redPoints.add( n.getNormalisedProfilePositions()[k] );
              double yPosition = CHART_SIGNAL_Y_LINE_MIN + ( redSignals.get(j).getFractionalDistanceFromCoM() * ( CHART_SIGNAL_Y_LINE_MAX - CHART_SIGNAL_Y_LINE_MIN) ); // make between 220 and 260
              yPoints.add(yPosition);
            }
          }
        }
        normProfileFromTailPlot.setColor(Color.RED);
        normProfileFromTailPlot.setLineWidth(2);
        normProfileFromTailPlot.addPoints(redPoints, yPoints, Plot.DOT);
      }
    }
  }

  public void exportProfilePlots(){
    this.exportProfilePlot(rawProfileFromHeadPlot, "plotHeadRaw");
    this.exportProfilePlot(rawProfileFromTailPlot, "plotTailRaw");
    this.exportProfilePlot(normProfileFromHeadPlot, "plotHeadNorm");
    this.exportProfilePlot(normProfileFromTailPlot, "plotTailNorm");
  }

  public void updatePlotWindows(){
    this.rawProfileFromHeadPlotWindow.drawPlot(rawProfileFromHeadPlot);
    this.rawProfileFromTailPlotWindow.drawPlot(rawProfileFromTailPlot);
    this.normProfileFromTailPlotWindow.drawPlot(normProfileFromTailPlot);
    this.normProfileFromHeadPlotWindow.drawPlot(normProfileFromHeadPlot);
  }

  public void showPlotWindows(){
    this.rawProfileFromHeadPlotWindow.draw();
    this.rawProfileFromTailPlotWindow.draw();
    this.normProfileFromTailPlotWindow.draw();
    this.normProfileFromHeadPlotWindow.draw();
  }

  /*
    -----------------------
    Utility functions
    -----------------------
  */
}