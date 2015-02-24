/* 
  -----------------------
  RODENT SPERM NUCLEUS COLLECTION CLASS
  -----------------------
  Rodent sperm differ from other mammalian sperm in that
  they have a hook shape. Consequently, thre is a left and right
  plus a tip after the acrosomal curve that is more useful 
  to detect on than the head point (which is within the acrosome)
*/

package no.nuclei.sperm;

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

public class RodentSpermNucleusCollection 
	extends no.nuclei.AsymmetricNucleusCollection
{


  // failure  codes
  public static final int FAILURE_TIP = 512;

	private String logMedianFromTipFile  = "logMediansFromTip"; // output medians

	private double[] normalisedMedianProfileFromTip; // this is an array of 200 angles

  private Map<Double, Collection<Double>> normalisedProfilesFromTip  = new HashMap<Double, Collection<Double>>();

  private Plot  rawProfileFromTipPlot;
  private Plot normProfileFromTipPlot;

  private PlotWindow  rawProfileFromTipPlotWindow;
  private PlotWindow normProfileFromTipPlotWindow;

  public RodentSpermNucleusCollection(File folder, String type){
  		super(folder, type);
  }

  @Override
  public void measureProfilePositions(){

    this.createProfileAggregateFromTip();

    calculateNormalisedMedianLineFromTip();

    this.findTailIndexInMedianCurve();
    this.calculateOffsets();

    this.createProfileAggregateFromTail();
    this.createProfileAggregateFromHead();

    this.drawProfilePlots();
    this.drawNormalisedMedianLineFromTail();
    this.drawNormalisedMedianLineFromHead();
    this.drawNormalisedMedianLineFromTip();
    this.calculateDifferencesToMedianProfiles();
    this.exportProfilePlots();
  }

  /*
    -----------------------
    Get values relating to sperm
    profiles
    -----------------------
  */

  public double getMaxRawXFromTips(){
    double d = 0;
    for(int i=0;i<this.getNucleusCount();i++){
      RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);
      if(n.getMaxRawXFromTip() > d){
        d = n.getMaxRawXFromTip();
      }
    }
    return d;
  }

  public double getMinRawXFromTips(){
    double d = 0;
    for(int i=0;i<this.getNucleusCount();i++){
      RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);
      if(n.getMaxRawXFromTip() < d){
        d = n.getMaxRawXFromTip();
      }
    }
    return d;
  }

  public double[] getMedianProfileFromTip(){
    return this.normalisedMedianProfileFromTip;
  }

  public double[] getHeadToTipDistances(){
    double[] d = new double[this.getNucleusCount()];
    for(int i=0;i<this.getNucleusCount();i++){
      RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);
      d[i] = n.getSpermTip().getLengthTo(n.getHead());
    }
    return d;
  }

  public double[] getTailToTipDistances(){
    double[] d = new double[this.getNucleusCount()];
    for(int i=0;i<this.getNucleusCount();i++){
      RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);
      d[i] = n.getSpermTip().getLengthTo(n.getTail());
    }
    return d;
  }

  /*
    -----------------------
    Setters
    -----------------------
  */

  public void setNormalisedMedianProfileFromTip(double[] d){
		this.normalisedMedianProfileFromTip = d;
	}


  /*
    -----------------------
    Filters for nuclei
    -----------------------
  */


  /*
    -----------------------
    Create and manipulate
    aggregate profiles. These
    can be centred on tip  or tail
    -----------------------
  */

  public void createProfileAggregateFromTip(){

  	for(int i=0;i<this.getNucleusCount();i++){

      RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);

      double[] xvalues = n.getNormalisedProfilePositions();
      double[] yvalues = n.getAngleProfile().getInteriorAngles(n.getTipIndex());

	  	updateProfileAggregate(xvalues, yvalues, this.normalisedProfilesFromTip); 
	  }
  }

	public void findTailIndexInMedianCurve(){
		// can't use regular tail detector, because it's based on NucleusBorderPoints
		// get minima in curve, then find the lowest minima / minima furthest from both ends

		ArrayList<Integer> minima = this.detectLocalMinimaInMedian(this.getMedianProfileFromTip());

		double minDiff = this.getMedianProfileFromTip().length;
		double minAngle = 180;
		int tailIndex = 0;

    if(minima.size()==0){
      IJ.log("  Error: no minima found in median line");
      tailIndex = 100; // set to roughly the middle of the array for the moment

    } else{

  		for(int i = 0; i<minima.size();i++){
  			Integer index = (Integer)minima.get(i);

  			int toEnd = this.getMedianProfileFromTip().length - index;
  			int diff = Math.abs(index - toEnd);

  			double angle = this.getMedianProfileFromTip()[index];
  			if(angle<minAngle && index > 40 && index < 120){ // get the lowest point that is not near the tip
  				minAngle = angle;
  				tailIndex = index;
  			}
  		}
  	}
  	this.setMedianProfileTailIndex(tailIndex);
	}

  public void measureNuclearOrganisation(){

    for(int i= 0; i<this.getNucleusCount();i++){ // for each roi

      RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);

       n.splitNucleusToHeadAndHump();
       n.calculateSignalAnglesFromTail();
    }
    this.exportSignalStats();
    this.addSignalsToProfileChartFromTip();
    this.addSignalsToProfileChartFromTail();

    IJ.log("Red signals: "  + this.getRedSignalCount());
    IJ.log("Green signals: "+ this.getGreenSignalCount());
  }

  /*
    Calculate the offsets needed to corectly assign the tail positions
    compared to ideal median curves
  */
  public void calculateOffsets(){

    for(int i= 0; i<this.getNucleusCount();i++){ // for each roi
      RodentSpermNucleus n = (RodentSpermNucleus) this.getNucleus(i);

      // int originalTailIndex = n.getTailIndex();

      // the curve needs to be matched to the median 
      // hence the median array needs to be the same curve length
      double[] medianToCompare = this.getMedianProfileFromTip();

      double[] interpolatedMedian = NucleusCollection.interpolateMedianToLength(n.getLength(), medianToCompare);

      // find the median tail index position in the interplolated median profile
      int medianTailIndex = (int)Math.round(( (double)this.getMedianProfileTailIndex() / (double)medianToCompare.length )* n.getLength());
      
      int offset = n.getTailIndex() - medianTailIndex;

      n.setOffsetForTail(offset);

      int newTailIndex = wrapIndex(n.getTailIndex()-offset, n.getLength());

      n.setTailIndex(newTailIndex); // update the tail position
      n.setSpermTail(n.getBorderPoint(n.getTailIndex())); // ensure the spermTail is updated

      // also update the head position
      n.setHead( n.findOppositeBorder( n.getSpermTail() ));
    }
  }


	/*
    -----------------------
    Export data
    -----------------------
  */

  public void exportOffsets(double[] d){

  	String logFile = this.getFolder()+File.separator+"logOffsets.txt";
    File f = new File(logFile);
    if(f.exists()){
      f.delete();
    }

    IJ.append("OFFSET\tDIFFERENCE", logFile);

    for(int i=0;i<d.length;i++){
      IJ.append(i+"\t"+d[i], logFile);
    }
    IJ.append("", logFile);
  }

  /*
    Draw the features of interest on the images of the nuclei created earlier
  */
  public void annotateImagesOfNuclei(){
  	IJ.log("Annotating images ("+this.getType()+")...");
  	for(int i=0; i<this.getNucleusCount();i++){
  		RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);
  		n.annotateFeatures();
  	}
  	 IJ.log("Annotation complete");
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
  @Override
  public void preparePlots(){

    super.preparePlots();

    this.rawProfileFromTipPlot = new Plot( "Raw tip-centred plot",
                                "Position",
                                "Angle", Plot.Y_GRID | Plot.X_GRID);
    rawProfileFromTipPlot.setLimits(0,this.getMaxProfileLength(),-50,360);
    rawProfileFromTipPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
    rawProfileFromTipPlot.setYTicks(true);
    rawProfileFromTipPlot.setColor(Color.BLACK);
    rawProfileFromTipPlot.drawLine(0, 180, this.getMaxProfileLength(), 180); 
    rawProfileFromTipPlot.setColor(Color.LIGHT_GRAY);


    normProfileFromTipPlot = new Plot("Normalised tip-centred plot",
                                "Position",
                                "Angle", Plot.Y_GRID | Plot.X_GRID);
    normProfileFromTipPlot.setLimits(0,100,-50,360);
    normProfileFromTipPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
    normProfileFromTipPlot.setYTicks(true);
    normProfileFromTipPlot.setColor(Color.BLACK);
    normProfileFromTipPlot.drawLine(0, 180, 100, 180); 
    normProfileFromTipPlot.setColor(Color.LIGHT_GRAY);
  }

  /*
    Create the charts of the profiles of the nuclei within this collecion.
    Currently drawing: 
      Tip-aligned raw X
      Tip-aligned normalised X
  */
  @Override
  public void drawProfilePlots(){

    preparePlots();
    super.drawProfilePlots();

    for(int i=0;i<this.getNucleusCount();i++){

      RodentSpermNucleus n = (RodentSpermNucleus) this.getNucleus(i);
      
      double[] xPointsRaw  = n.getRawProfilePositions();
      double[] xPointsNorm = n.getNormalisedProfilePositions();

      double[] anglesFromTip = n.getAngleProfile().getInteriorAngles(n.getTipIndex());

      this.rawProfileFromTipPlot.setColor(Color.LIGHT_GRAY);
      this.rawProfileFromTipPlot.addPoints(xPointsRaw, anglesFromTip, Plot.LINE);

      this.normProfileFromTipPlot.setColor(Color.LIGHT_GRAY);
      this.normProfileFromTipPlot.addPoints(xPointsNorm, anglesFromTip, Plot.LINE);
      
    }

  }

  /*
    Calculate and export the normalised median profile from tip
  */

  public void calculateNormalisedMedianLineFromTip(){
    ArrayList<Double[]> medians = calculateMediansAndQuartilesOfProfile( this.normalisedProfilesFromTip );
    this.exportMediansAndQuartilesOfProfile(medians, this.logMedianFromTipFile);
    setNormalisedMedianProfileFromTip(getDoubleFromDouble( medians.get(1) ));
  }

  /*
		Draw the median line on the normalised profile
		chart, aligned to the sperm tip
  */
  public void drawNormalisedMedianLineFromTip(){
    // output the final results: calculate median positions

    ArrayList<Double[]> medians = calculateMediansAndQuartilesOfProfile( this.normalisedProfilesFromTip );

    double[] xmedians        =  getDoubleFromDouble( medians.get(0) );
    double[] ymedians        =  getDoubleFromDouble( medians.get(1) );
    double[] lowQuartiles    =  getDoubleFromDouble( medians.get(2) );
    double[] uppQuartiles    =  getDoubleFromDouble( medians.get(3) );
    double[] tenQuartiles    =  getDoubleFromDouble( medians.get(4) );
    double[] ninetyQuartiles =  getDoubleFromDouble( medians.get(5) );

    // add the median lines to the chart
    normProfileFromTipPlot.setColor(Color.BLACK);
    normProfileFromTipPlot.setLineWidth(3);
    normProfileFromTipPlot.addPoints(xmedians, ymedians, Plot.LINE);
    normProfileFromTipPlot.setColor(Color.DARK_GRAY);
    normProfileFromTipPlot.setLineWidth(2);
    normProfileFromTipPlot.addPoints(xmedians, lowQuartiles, Plot.LINE);
    normProfileFromTipPlot.addPoints(xmedians, uppQuartiles, Plot.LINE);

    // handle the normalised tail position mapping
    // double[] xTails = this.getNormalisedTailIndexes();

    // double[] yTails = new double[xTails.length];
    // Arrays.fill(yTails, CHART_TAIL_BOX_Y_MID); // all dots at y=300
    // normProfileFromTipPlot.setColor(Color.LIGHT_GRAY);
    // normProfileFromTipPlot.addPoints(xTails, yTails, Plot.DOT);

    // // median tail positions
    // double tailQ50 = quartile(xTails, 50);
    // double tailQ25 = quartile(xTails, 25);
    // double tailQ75 = quartile(xTails, 75);

    // normProfileFromTipPlot.setColor(Color.DARK_GRAY);
    // normProfileFromTipPlot.setLineWidth(1);
    // normProfileFromTipPlot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MAX, tailQ75, CHART_TAIL_BOX_Y_MAX);
    // normProfileFromTipPlot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MIN, tailQ75, CHART_TAIL_BOX_Y_MIN);
    // normProfileFromTipPlot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MIN, tailQ25, CHART_TAIL_BOX_Y_MAX);
    // normProfileFromTipPlot.drawLine(tailQ75, CHART_TAIL_BOX_Y_MIN, tailQ75, CHART_TAIL_BOX_Y_MAX);
    // normProfileFromTipPlot.drawLine(tailQ50, CHART_TAIL_BOX_Y_MIN, tailQ50, CHART_TAIL_BOX_Y_MAX);
  }

  private void addSignalsToProfileChartFromTip(){
    // PlotWindow normXFromTipWindow; normProfileFromTipPlot
    // for each signal in each nucleus, find index of point.
    // Add the signals to the tip centred profile plot

    //********************
    // NEED TO ADD GREEN
    //********************

    normProfileFromTipPlot.setColor(Color.LIGHT_GRAY);
    normProfileFromTipPlot.setLineWidth(1);
    normProfileFromTipPlot.drawLine(0,CHART_SIGNAL_Y_LINE_MIN,100,CHART_SIGNAL_Y_LINE_MIN);
    normProfileFromTipPlot.drawLine(0,CHART_SIGNAL_Y_LINE_MAX,100,CHART_SIGNAL_Y_LINE_MAX);

    for(int i= 0; i<this.getNucleusCount();i++){ // for each roi

      RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);

      ArrayList<NuclearSignal> redSignals = n.getRedSignals();
      if(redSignals.size()>0){

        ArrayList<Double> xPoints = new ArrayList<Double>(0);
        ArrayList<Double> yPoints = new ArrayList<Double>(0);

        for(int j=0; j<redSignals.size();j++){

          NucleusBorderPoint border = redSignals.get(j).getClosestBorderPoint();
          xPoints.add( (double)n.getAngleProfile().getIndexOfPoint(border));

          // scale the y position to the fractional distance from CoM
          double yPosition = CHART_SIGNAL_Y_LINE_MIN +
                            ( redSignals.get(j).getFractionalDistanceFromCoM() * 
                              ( CHART_SIGNAL_Y_LINE_MAX - CHART_SIGNAL_Y_LINE_MIN) 
                            );
          yPoints.add(yPosition);

        }
        normProfileFromTipPlot.setColor(Color.RED);
        normProfileFromTipPlot.setLineWidth(2);
        normProfileFromTipPlot.addPoints(xPoints, yPoints, Plot.DOT);
      }
    }
  }

  /*
    -----------------------
    Export data
    -----------------------
  */
  @Override
  public void exportProfilePlots(){
    super.exportProfilePlots();
    this.exportProfilePlot(rawProfileFromTipPlot, "plotTipRaw");
    this.exportProfilePlot(normProfileFromTipPlot, "plotTipNorm");
  }

  @Override
  public void exportClusteringProfiles(String filename){
    String statsFile = this.getFolder()+File.separator+filename+"."+getType()+".txt";
    File f = new File(statsFile);
    if(f.exists()){
      f.delete();
    }

    String outLine = "PATH\tAREA\tPERIMETER\tFERET\tPATH_LENGTH\tDIFFERENCE\tFAILURE_CODE\tHEAD_TO_TAIL\tTIP_TO_TAIL\tHEAD_TO_TIP\t";

    IJ.log("Exporting clustering profiles for "+this.getNucleusCount()+" nuclei ("+this.getType()+")...");
    double[] areas        = this.getAreas();
    double[] perims       = this.getPerimeters();
    double[] ferets       = this.getFerets();
    double[] pathLengths  = this.getPathLengths();
    double[] differences  = this.getDifferencesToMedianFromTail();
    double[] headToTail   = this.getHeadToTailDistances();
    double[] headToTip    = this.getHeadToTipDistances();
    double[] tipToTail    = this.getTailToTipDistances();
    String[] paths        = this.getNucleusPaths();

    double maxPerim = getMax(perims); // add column headers
    for(int i=0;i<maxPerim;i++){
      outLine += i+"\t";
    }
    outLine += "\n";

    // export the profiles for each nucleus
    for(int i=0; i<this.getNucleusCount();i++){

      outLine = outLine + paths[i]      +"\t"+
                          areas[i]      +"\t"+
                          perims[i]     +"\t"+
                          ferets[i]     +"\t"+
                          pathLengths[i]+"\t"+
                          differences[i]+"\t"+
                          headToTail[i] +"\t"+
                          tipToTail[i]  +"\t"+
                          headToTip[i]  +"\t";

      AsymmetricNucleus n = (AsymmetricNucleus)this.getNucleus(i);
      double[] profile = n.getAngleProfile().getInteriorAngles(n.getTailIndex());
      for(int j=0;j<profile.length;j++){
        outLine += profile[j]+"\t";
      }
      outLine += "\n";
    }
    IJ.append(  outLine, statsFile);
    IJ.log("Cluster export complete");
  }

  /*
    -----------------------
    Utility functions
    -----------------------
  */
}