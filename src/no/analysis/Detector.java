/*
  -----------------------
  DETECTOR
  -----------------------
  Contains the variables for detecting
  nuclei and signals
*/  
package no.analysis;
import java.util.*;

import no.utility.StatsMap;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;


public class Detector{

  /* VALUES FOR DECIDING IF AN OBJECT IS A NUCLEUS */
  private double minSize;
  private double maxSize;
  private double minCirc;
  private double maxCirc;

  private int  threshold;
  private int channel;

  private Roi[] roiArray;

//  private Map<Roi, StatsMap> roiMap = new HashMap<Roi, StatsMap>(0);

  public Detector(){

  }

  public void setMinSize(double d){
  	this.minSize = d;
  }

  public void setMaxSize(double d){
  	this.maxSize = d;
  }

  public void setMinCirc(double d){
  	this.minCirc = d;
  }

  public void setMaxCirc(double d){
  	this.maxCirc = d;
  }

  public void setThreshold(int i){
  	this.threshold = i;
  }

  public void setChannel(int i){
  	this.channel = i;
  }
  
  public void run(ImageStack image){
	  if(image==null){
		  throw new IllegalArgumentException("No image to analyse");
	  }
	  if( Double.isNaN(this.minSize) || 
			  Double.isNaN(this.maxSize) ||
			  Double.isNaN(this.minCirc) ||
			  Double.isNaN(this.maxCirc)){
		  throw new IllegalArgumentException("Detection parameters not set");
	  }
	  
	  if(this.minSize >= this.maxSize){
		  throw new IllegalArgumentException("Minimum size >= maximum size");
	  }
	  if(this.minCirc >= this.maxCirc){
		  throw new IllegalArgumentException("Minimum circularity >= maximum circularity");
	  }

	  if(this.channel==0 || this.channel > image.getSize()){
		  throw new IllegalArgumentException("Not a valid channel for this image");
	  }
	  findInImage(image);
  }

  public List<Roi> getRoiList(){
  	List<Roi> result = new ArrayList<Roi>(0);
  	for(Roi r : this.roiArray){
  		result.add(r);
  	}
  	return result;
  }
  
  private void findInImage(ImageStack image){

	  // Note - the channels in an ImageStack are numbered from 1
	  if(this.channel==0 || this.channel > image.getSize()){
		  throw new IllegalArgumentException("Not a valid channel for this image in Detector.findInImage():"+this.channel);
	  }
	  ImageProcessor searchProcessor = image.getProcessor(this.channel).duplicate();
	  
//	  searchProcessor.smooth();
	  searchProcessor.threshold(this.threshold);

	  this.runAnalyser(searchProcessor);
	  if(this.getRoiCount()==0){
		  searchProcessor.invert(); // Work PC needs the inversion; MANETHEREN does not. Don't yet know why.
		  this.runAnalyser(searchProcessor);
	  }
  }

  private int getRoiCount() {
	  return this.roiArray.length;
  }

  private void runAnalyser(ImageProcessor processor){
	  ImagePlus image = new ImagePlus(null, processor);
	  RoiManager manager = new RoiManager(true);
	  // run the particle analyser
	  ResultsTable rt = new ResultsTable();
	  ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES | ParticleAnalyzer.INCLUDE_HOLES, 
			  ParticleAnalyzer.FERET ,
			  rt, this.minSize, this.maxSize, this.minCirc, this.maxCirc);
	  try {
		  ParticleAnalyzer.setRoiManager(manager);
		  boolean success = pa.analyze(image);
		  if(!success){
			  IJ.log("  Unable to perform particle analysis");
		  }
	  } catch(Exception e){
		  IJ.log("  Error in particle analyser: "+e);
	  } finally {
		  image.close();
	  }

	  this.roiArray = manager.getSelectedRoisAsArray();
  }
  
  /**
   * Get the stats for the region covered by the given roi. Uses the channel
   * previously set.
   * @param roi the region to measure
   * @return
   */
  public StatsMap measure(Roi roi, ImageStack image ){
	  if(image==null || roi==null){
		  throw new IllegalArgumentException("Image or roi is null");
	  }
	  if(image.getProcessor(this.channel)==null){
		  throw new IllegalArgumentException("Not a valid channel for this image");
	  }
	  if(this.channel==0 || this.channel>image.getSize()){
		  throw new IllegalArgumentException("Channel out of range for this image");
	  }
	  ImageProcessor searchProcessor = image.getProcessor(this.channel).duplicate();
	  ImagePlus imp = new ImagePlus(null, searchProcessor);
	  imp.setRoi(roi);
	  ResultsTable rt = new ResultsTable();
	  Analyzer analyser = new Analyzer(imp, Measurements.CENTER_OF_MASS | 
			  								Measurements.AREA | 
			  								Measurements.PERIMETER | 
			  								Measurements.FERET, 
			  							rt);
	  analyser.measure();
	  StatsMap values = new StatsMap();
	  values.add("Area", rt.getValue("Area",0)); 
	  values.add("Feret", rt.getValue("Feret",0)); 
	  values.add("Perim", rt.getValue("Perim.",0)); 
	  values.add("XM", rt.getValue("XM",0)); 
	  values.add("YM", rt.getValue("YM",0)); 
	  return values;
  }

}