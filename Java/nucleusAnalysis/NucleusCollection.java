/* 
  -----------------------
  NUCLEUS COLLECTION CLASS
  -----------------------
  This class contains the nuclei that pass detection criteria
  Provides aggregate stats
  It enables offsets to be calculated based on the median normalised curves
*/

package nucleusAnalysis;

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

public class NucleusCollection {

	private String folder; // the source of the nuclei
  private String collectionType; // for annotating image names

	private ArrayList<Nucleus> nucleiCollection = new ArrayList<Nucleus>(0); // store all the nuclei analysed

  // private double maxDifferenceFromMedian = 1.5; // used to filter the nuclei, and remove those too small, large or irregular to be real
  // private double maxWibblinessFromMedian = 1.2; // filter for the irregular borders more stringently

	public NucleusCollection(String folder, String type){
		this.folder = folder;
    this.collectionType = type;
	}

	public void addNucleus(Nucleus r){
		this.nucleiCollection.add(r);
	}

  public String getFolder(){
    return this.folder;
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
    The filters needed to separate out the objects from nuclei
    Filter on: nuclear area, perimeter and array length to find
    conjoined nuclei and blobs too small to be nuclei
    Use path length to remove poorly thresholded nuclei
  */
  // public void filterNuclei(){

  //   double medianArea = this.getMedianNuclearArea();
  //   double medianPerimeter = this.getMedianNuclearPerimeter();
  //   double medianPathLength = this.getMedianPathLength();
  //   double medianArrayLength = this.getMedianArrayLength();
  //   double medianFeretLength = this.getMedianFeretLength();
  //   double medianDifferenceToMedianCurve = quartile(this.getDifferencesToMedian(),50);
    
  //   int beforeSize = nucleiCollection.size();

  //   double maxPathLength = medianPathLength * maxWibblinessFromMedian;
  //   double minArea = medianArea / maxDifferenceFromMedian;
  //   double maxArea = medianArea * maxDifferenceFromMedian;
  //   double maxPerim = medianPerimeter *maxDifferenceFromMedian;
  //   double minPerim = medianPerimeter / maxDifferenceFromMedian;
  //   double minFeret = medianFeretLength / maxDifferenceFromMedian;

  //   double maxCurveDifference = medianDifferenceToMedianCurve * 2;


  //   int area = 0;
  //   int perim = 0;
  //   int pathlength = 0;
  //   int arraylength = 0;
  //   int curveShape = 0;
  //   int feretlength = 0;

  //   int totalIterations = nucleiCollection.size();

  //   IJ.append("Prefiltered:", debugFile);
  //   IJ.append("    Area: "+(int)medianArea, debugFile);
  //   IJ.append("    Perimeter: "+(int)medianPerimeter, debugFile);
  //   IJ.append("    Path length: "+(int)medianPathLength, debugFile);
  //   IJ.append("    Array length: "+(int)medianArrayLength, debugFile);
  //   IJ.append("    Feret length: "+(int)medianFeretLength, debugFile);
  //   IJ.append("    Curve: "+(int)medianDifferenceToMedianCurve, debugFile);

  //   for(int i=0;i<nucleiCollection.size();i++){
  //     Nucleus n = nucleiCollection.get(i);
  //     boolean dropNucleus = false;

  //     if(n.getArea() > maxArea || n.getArea() < minArea ){
  //       n.failureCode = n.failureCode | FAILURE_AREA;
  //       area++;
  //     }
  //     if(n.getPerimeter() > maxPerim || n.getPerimeter() < minPerim ){
  //       n.failureCode = n.failureCode | FAILURE_PERIM;
  //       perim++;
  //     }
  //     if(n.getPathLength() > maxPathLength){ // only filter for values too big here - wibbliness detector
  //       n.failureCode = n.failureCode | FAILURE_THRESHOLD;
  //       pathlength++;
  //     }
  //     if(n.getLength() > medianArrayLength * maxDifferenceFromMedian || n.getLength() < medianArrayLength / maxDifferenceFromMedian ){
  //       n.failureCode = n.failureCode | FAILURE_ARRAY;
  //        arraylength++;
  //     }

  //     if(n.getFeret() < minFeret){
  //       n.failureCode = n.failureCode | FAILURE_FERET;
  //       feretlength++;
  //     }

      
  //     if(n.failureCode > 0){
  //       failedNuclei.addNucleus(n);
  //       this.nucleiCollection.remove(n);
  //       i--; // the array index automatically shifts to account for the removed nucleus. Compensate to avoid skipping nuclei
  //     }
  //   }

  //   medianArea = this.getMedianNuclearArea();
  //   medianPerimeter = this.getMedianNuclearPerimeter();
  //   medianPathLength = this.getMedianPathLength();
  //   medianArrayLength = this.getMedianArrayLength();
  //   medianFeretLength = this.getMedianFeretLength();
  //   medianDifferenceToMedianCurve = quartile(this.getDifferencesToMedian(),50);

  //   int afterSize = nucleiCollection.size();
  //   int removed = beforeSize - afterSize;

  //   IJ.append("Postfiltered:", debugFile);
  //   IJ.append("    Area: "+(int)medianArea, debugFile);
  //   IJ.append("    Perimeter: "+(int)medianPerimeter, debugFile);
  //   IJ.append("    Path length: "+(int)medianPathLength, debugFile);
  //   IJ.append("    Array length: "+(int)medianArrayLength, debugFile);
  //   IJ.append("    Feret length: "+(int)medianFeretLength, debugFile);
  //   IJ.append("    Curve: "+(int)medianDifferenceToMedianCurve, debugFile);
  //   IJ.log("Removed due to size or length issues: "+removed+" nuclei");
  //   IJ.append("  Due to area outside bounds "+(int)minArea+"-"+(int)maxArea+": "+area+" nuclei", debugFile);
  //   IJ.append("  Due to perimeter outside bounds "+(int)minPerim+"-"+(int)maxPerim+": "+perim+" nuclei", debugFile);
  //   IJ.append("  Due to wibbliness >"+(int)maxPathLength+" : "+(int)pathlength+" nuclei", debugFile);
  //   IJ.append("  Due to array length: "+arraylength+" nuclei", debugFile);
  //   IJ.append("  Due to feret length: "+feretlength+" nuclei", debugFile);
  //   IJ.append("  Due to curve shape: "+curveShape+" nuclei", debugFile);
  //   IJ.log("Remaining: "+this.nucleiCollection.size()+" nuclei");
  // }


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
}