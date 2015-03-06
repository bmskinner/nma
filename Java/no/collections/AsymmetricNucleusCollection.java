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
import no.analysis.Analysable;
import no.nuclei.*;
import no.nuclei.sperm.*;
import no.components.*;
import no.components.Profile;
import no.utility.*;

public class AsymmetricNucleusCollection 
	extends no.collections.NucleusCollection
  implements no.analysis.Analysable
{

  // failure  codes
  public static final int FAILURE_HEAD = 128;
  public static final int FAILURE_TAIL = 256;

	// private String logMedianFromHeadFile = "logMediansFromHead"; // output medians
 //  private String logMedianFromTailFile = "logMediansFromTail"; // output medians

	// private double[] normalisedMedianProfileFromHead; // this is an array of 200 angles
 //  private double[] normalisedMedianProfileFromTail; // this is an array of 200 angles

	private boolean differencesCalculated = false;

  // private Map<Double, Collection<Double>> normalisedProfilesFromHead = new HashMap<Double, Collection<Double>>();
  // private Map<Double, Collection<Double>> normalisedProfilesFromTail = new HashMap<Double, Collection<Double>>();

  public AsymmetricNucleusCollection(File folder, String outputFolder, String type){
  		super(folder, outputFolder, type);
  }

  
  public AsymmetricNucleusCollection(){

  }

  // public void measureProfilePositions(){

  //   this.createProfileAggregates();
  //   this.drawProfilePlots();

  //   this.drawNormalisedMedianLines();
  //   this.calculateDifferencesToMedianProfiles();
  //   this.exportProfilePlots();
  // }

  @Override
  public void exportStatsFiles(){
    super.exportStatsFiles();
    this.exportClusteringProfiles("logClusters");

    Profile normalisedMedian = this.getMedianProfile("tail");
    Profile interpolatedMedian = normalisedMedian.interpolate((int)this.getMedianNuclearPerimeter());
    this.exportMediansOfProfile(interpolatedMedian, "logMediansPerimeterLength");
  }

  @Override
  public void annotateAndExportNuclei(){
    this.annotateImagesOfNuclei();
    super.annotateAndExportNuclei();
  }


  /*
    -----------------------
    General getters
    -----------------------
  */

  public boolean isDifferencesCalculated(){
    return this.differencesCalculated;
  }

  public void setDifferencesCalculated(boolean b){
    this.differencesCalculated = b;
  }


  /*
    -----------------------
    Filter nuclei on
    assymetric specific features
    -----------------------
  */

    // public void refilterNuclei(Analysable failedCollection){
    //   super.refilterNuclei(failedCollection);
    // }


  /*
    -----------------------
    Create and manipulate
    aggregate profiles. These
    can be centred on head or tail
    -----------------------
  */
	 
  @Override 
  public void measureNuclearOrganisation(){

    if(this.getRedSignalCount()>0 || this.getGreenSignalCount()>0){

      for(int i= 0; i<this.getNucleusCount();i++){
        INuclearFunctions n = (INuclearFunctions)this.getNucleus(i);
        n.calculateSignalAnglesFromPoint(n.getBorderTag("tail"));
      }
      this.exportSignalStats();
      this.addSignalsToProfileCharts();
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
      INuclearFunctions n = (INuclearFunctions)this.getNucleus(i);
      n.annotateFeatures();
    }
     IJ.log("    Annotation complete");
  }


	/*
    -----------------------
    Export data
    -----------------------
  */

  public void exportInterpolatedMedians(Profile medianProfile){

    String logFile = makeGlobalLogFile("logOffsets");

    IJ.append("INDEX\tANGLE", logFile);
    for(int i=0;i<medianProfile.size();i++){
      IJ.append(i+"\t"+medianProfile.get(i), logFile);
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

    String[] index  = NuclearOrganisationUtility.getStringFromInt(this.getPointIndexes("tail"));
    // String[] diff   = NuclearOrganisationUtility.getStringFromDouble(this.getDifferencesToMedianFromTail());
    String[] points = NuclearOrganisationUtility.getStringFromDouble(this.getMedianDistanceBetweenPoints());

    stats.put("NORM_TAIL_INDEX",                Arrays.asList(index ));
    // stats.put("DIFFERENCE_TO_MEDIAN_PROFILE",   Arrays.asList(diff  ));
    stats.put("MEDIAN_DISTANCE_BETWEEN_POINTS", Arrays.asList(points));

    exportStats(stats, filename);
  }

  @Override
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
    IJ.log("    Creating composite image...");
    

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
      
      INuclearFunctions n = (INuclearFunctions)this.getNucleus(i);
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
    IJ.saveAsTiff(finalImage, this.getFolder()+File.separator+this.getOutputFolder()+File.separator+filename+"."+getType()+".tiff");
    IJ.log("    Composite image created");
  }

  public void exportClusteringProfiles(String filename){

    String statsFile = makeGlobalLogFile(filename);

    StringBuilder outLine = new StringBuilder();
    outLine.append("PATH\tAREA\tPERIMETER\tFERET\tPATH_LENGTH\tDIFFERENCE\tFAILURE_CODE\tHEAD_TO_TAIL\t");

    IJ.log("    Exporting clustering profiles...");
    double[] areas        = this.getAreas();
    double[] perims       = this.getPerimeters();
    double[] ferets       = this.getFerets();
    double[] pathLengths  = this.getPathLengths();
    double[] differences  = this.getDifferencesToMedianFromPoint("tail");
    double[] headToTail   = this.getPointToPointDistances("head", "tail");
    String[] paths        = this.getNucleusPaths();

    double maxPerim = NuclearOrganisationUtility.getMax(perims); // add column headers
    for(int i=0;i<maxPerim;i++){
      outLine.append(i+"\t");
    }
    outLine.append("\r\n");


    // export the profiles for each nucleus
    for(int i=0; i<this.getNucleusCount();i++){

      outLine.append(paths[i]     +"\t"+
                    areas[i]      +"\t"+
                    perims[i]     +"\t"+
                    ferets[i]     +"\t"+
                    pathLengths[i]+"\t"+
                    differences[i]+"\t"+
                    headToTail[i] +"\t");

      INuclearFunctions n = (INuclearFunctions)this.getNucleus(i);
      Profile profile = n.getAngleProfile("tail");
      for(int j=0;j<profile.size();j++){
        outLine.append(profile.get(j)+"\t");
      }
      outLine.append("\r\n");
    }
    IJ.append(  outLine.toString(), statsFile);
    IJ.log("    Cluster export complete");
  }
}