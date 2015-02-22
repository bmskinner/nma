/* 
  -----------------------
  NUCLEUS COLLECTION CLASS
  -----------------------
  This class contains the nuclei that pass detection criteria
  Provides aggregate stats
  It enables offsets to be calculated based on the median normalised curves
*/

package no.nuclei;
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


public class NucleusCollection {

	private File folder; // the source of the nuclei
  private File debugFile;
  private String collectionType; // for annotating image names

  public static final int FAILURE_THRESHOLD = 4;
  public static final int FAILURE_FERET     = 8;
  public static final int FAILURE_ARRAY     = 16;
  public static final int FAILURE_AREA      = 32;
  public static final int FAILURE_PERIM     = 64;
  public static final int FAILURE_OTHER     = 128;
  public static final int FAILURE_SIGNALS   = 256;

	private ArrayList<Nucleus> nucleiCollection = new ArrayList<Nucleus>(0); // store all the nuclei analysed

	public NucleusCollection(File folder, String type){
		this.folder = folder;
    this.debugFile = new File(folder.getAbsolutePath()+File.separator+"logDebug.txt");
    this.collectionType = type;
	}

  /*
    -----------------------
    Define adders for all
    types of nucleus eligable
    -----------------------
  */

	public void addNucleus(Nucleus r){
		this.nucleiCollection.add(r);
	}

  public void addNucleus(SpermNucleus r){
    this.nucleiCollection.add(r);
  }

  public void addNucleus(RodentSpermNucleus r){
    this.nucleiCollection.add(r);
  }

  public void addNucleus(PigSpermNucleus r){
    this.nucleiCollection.add(r);
  }

  /*
    -----------------------
    Getters for aggregate stats
    -----------------------
  */

  public File getFolder(){
    return this.folder;
  }

  public File getDebugFile(){
    return this.debugFile;
  }

  public String getType(){
    return this.collectionType;
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
      d[i] = nucleiCollection.get(i).getLength();
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

  public ArrayList<Nucleus> getNuclei(){
    return this.nucleiCollection;
  }

  public Nucleus getNucleus(int i){
    return this.nucleiCollection.get(i);
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

  /*
    Turn a Double[] into a double[]
  */
  private double[] getDoubleFromDouble(Double[] d){
    double[] results = new double[d.length];
    for(int i=0;i<d.length;i++){
      results[i] = d[i];
    }
    return results;
  }

  /*
    Export the signal parameters of the nucleus to the designated log file
  */
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

        String log = signalCount == 0 ? redLogFile : greenLogFile; // the red signals are first to be analysed
        
        if(signalGroup.size()>0){
          for(int j=0; j<signalGroup.size();j++){
             NuclearSignal s = signalGroup.get(j);
             IJ.append(nucleusNumber+"\t"+
                       s.getArea()+"\t"+
                       s.getAngle()+"\t"+
                       s.getFeret()+"\t"+
                       s.getDistanceFromCoM()+"\t"+
                       s.getFractionalDistanceFromCoM()+"\t"+
                       s.getPerimeter()+"\t"+
                       path, log);
          } // end for
        } // end if
        signalCount++;
      } // end for
    } // end for
  }

  public void exportDistancesBetweenSingleSignals(){

    File logFile = new File(this.folder.getAbsolutePath()+File.separator+"logDistances."+collectionType+".txt");

    if(logFile.exists()){
      logFile.delete();
    }
    IJ.append("DISTANCE_BETWEEN_SIGNALS\tRED_DISTANCE_TO_COM\tGREEN_DISTANCE_TO_COM\tNUCLEAR_FERET\tRED_FRACTION_OF_FERET\tGREEN_FRACTION_OF_FERET\tDISTANCE_BETWEEN_SIGNALS_FRACTION_OF_FERET\tNORMALISED_DISTANCE\tPATH", logFile.getAbsolutePath());

    for(int i=0; i<this.getNucleusCount();i++){

      Nucleus n = this.nucleiCollection.get(i);
      if(n.getRedSignalCount()==1 && n.getGreenSignalCount()==1){

        NuclearSignal r = n.getRedSignals().get(0);
        NuclearSignal g = n.getGreenSignals().get(0);

        XYPoint rCoM = r.getCentreOfMass();
        XYPoint gCoM = g.getCentreOfMass();

        double distanceBetween = rCoM.getLengthTo(gCoM);    
        double rDistanceToCoM = rCoM.getLengthTo(n.getCentreOfMass());
        double gDistanceToCoM = gCoM.getLengthTo(n.getCentreOfMass());
        double nFeret = n.getFeret();

        double rFractionOfFeret = rDistanceToCoM / nFeret;
        double gFractionOfFeret = gDistanceToCoM / nFeret;
        double distanceFractionOfFeret = distanceBetween / nFeret;
        double normalisedPosition = distanceFractionOfFeret / rFractionOfFeret / gFractionOfFeret;

        IJ.append(  distanceBetween+"\t"+
                    rDistanceToCoM+"\t"+
                    gDistanceToCoM+"\t"+
                    nFeret+"\t"+
                    rFractionOfFeret+"\t"+
                    gFractionOfFeret+"\t"+
                    distanceFractionOfFeret+"\t"+
                    normalisedPosition+"\t"+
                    n.getPath(), logFile.getAbsolutePath());
      }
    }
  }

  public void exportAnnotatedNuclei(){
    for(int i=0; i<this.getNucleusCount();i++){
      Nucleus n = this.getNucleus(i);
      n.exportAnnotatedImage();
    }
  }

  /*
    Calculate the <lowerPercent> quartile from a Double[] array
  */
  protected static double quartile(double[] values, double lowerPercent) {

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

  protected static double quartile(Double[] values, double lowerPercent) {

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

  protected static int wrapIndex(int i, int length){
    if(i<0)
      i = length + i; // if i = -1, in a 200 length array,  will return 200-1 = 199
    if(Math.floor(i / length)>0)
      i = i - ( ((int)Math.floor(i / length) )*length);

    if(i<0 || i>length){
      IJ.log("Warning: array out of bounds: "+i);
    }
    
    return i;
  }

  protected double getMin(double[] d){
    double min = getMax(d);
    for(int i=0;i<d.length;i++){
      if( d[i]<min)
        min = d[i];
    }
    return min;
  }

  protected double getMax(double[] d){
    double max = 0;
    for(int i=0;i<d.length;i++){
      if( d[i]>max)
        max = d[i];
    }
    return max;
  }
}