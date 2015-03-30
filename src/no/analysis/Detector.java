/*
  -----------------------
  DETECTOR
  -----------------------
  Contains the variables for detecting
  nuclei and signals
*/  
package no.analysis;
import java.util.*;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.plugin.ChannelSplitter;
import ij.process.ImageProcessor;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;


public class Detector{

	// colour channels
  private static final int RED_CHANNEL   = 0;
  private static final int GREEN_CHANNEL = 1;
  private static final int BLUE_CHANNEL  = 2;

  /* VALUES FOR DECIDING IF AN OBJECT IS A NUCLEUS */
  private double minSize;
  private double maxSize;
  private double minCirc;
  private double maxCirc;

  private int  threshold;
  private int channel;

  private Roi[] roiArray;

  private Map<Roi, HashMap<String, Double>> roiMap = new HashMap<Roi, HashMap<String, Double>>(0);

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
			  Double.isNaN(this.maxCirc))
		  throw new IllegalArgumentException("Detection parameters not set");

	  if(image.getProcessor(this.channel)==null){
		  throw new IllegalArgumentException("Not a valid channel for this image");
	  }
	  findInImage(image);
  }

  public void run(ImagePlus image) throws Exception{

	  if(image==null){
		  throw new IllegalArgumentException("No image to analyse");
	  }

	  if( Double.isNaN(this.minSize) || 
			  Double.isNaN(this.maxSize) ||
			  Double.isNaN(this.minCirc) ||
			  Double.isNaN(this.maxCirc))
		  throw new Exception("Detection parameters not set");

	  if(	this.channel!=RED_CHANNEL 	&& 
			  this.channel!=GREEN_CHANNEL && 
			  this.channel!=BLUE_CHANNEL){
		  throw new Exception("RGB channel not set");
	  }



	  this.findInImage(image);
  }

  public List<Roi> getRois(){
  	List<Roi> result = new ArrayList<Roi>(0);
  	for(Roi r : this.roiArray){
  		result.add(r);
  	}
  	return result;
  }

  // ensure defensive
  public Map<Roi, HashMap<String, Double>> getRoiMap(){
  	Map<Roi, HashMap<String, Double>> resultMap = new HashMap<Roi, HashMap<String, Double>>(0);
  	Set<Roi> keys = this.roiMap.keySet();

  	for(Roi r : keys){
  		HashMap<String, Double> values = roiMap.get(r);
  		HashMap<String, Double> resultValues = new HashMap<String, Double>(0);
  		Set<String> valueKeys = values.keySet();
  		for( String s : valueKeys){
  			resultValues.put(s, values.get(s));
  		}
  		resultMap.put(r, resultValues);
  	}
  	return resultMap;
  }

  private ImagePlus getChannelImage(ImagePlus image, boolean invert){

    // split out colour channel
    ImagePlus[] channels = ChannelSplitter.split(image);
    ImagePlus searchImage = channels[this.channel];
    
    // threshold
    ImageProcessor ip = searchImage.getChannelProcessor();
    ip.smooth();
    ip.threshold(this.threshold);
    if(invert){
    	ip.invert(); // WHY IS THIS NEEDED? MAKES BLACK NUCLEUS ON WHITE. NEEDED BY PARTICLE DETECTOR ON MANETHEREN, BUT BREAKS ON WORK PC UNTIL IT SUDDENLY BROKE THE PARTICLE DETECTOR.
    }
    return searchImage;
  }

  
  private void findInImage(ImageStack image){

	  	ImageProcessor searchProcessor = image.getProcessor(this.channel).duplicate();
	  	searchProcessor.smooth();
	  	searchProcessor.threshold(this.threshold);
	  	
	    this.runAnalyser(searchProcessor);
	    
	    if(this.getRoiCount()==0){
	    	searchProcessor.invert();
	    	this.runAnalyser(searchProcessor);
	    }
	  }
  
  private void findInImage(ImagePlus image){

    
    ImagePlus searchImage = this.getChannelImage(image, false);
    this.runAnalyser(searchImage);
    if(this.getRoiCount()==0){
    	searchImage = this.getChannelImage(image, true);
    	this.runAnalyser(searchImage);
    }

   
  }
  
  private int getRoiCount() {
	return this.roiArray.length;
}

  private void runAnalyser(ImagePlus image){
	  RoiManager manager = new RoiManager(true);
	  // run the particle analyser
	    ResultsTable rt = new ResultsTable();
	    ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES, 
	                ParticleAnalyzer.CENTER_OF_MASS | ParticleAnalyzer.AREA | ParticleAnalyzer.PERIMETER | ParticleAnalyzer.FERET ,
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
	    for(int i=0;i<roiArray.length;i++){
	    	HashMap<String, Double> values = new HashMap<String, Double>(0);
	    	values.put("Area", rt.getValue("Area",i)); 
	    	values.put("Feret", rt.getValue("Feret",i)); 
	    	values.put("Perim", rt.getValue("Perim.",i)); 
	    	values.put("XM", rt.getValue("XM",i)); 
	    	values.put("YM", rt.getValue("YM",i)); 

	    	this.roiMap.put(roiArray[i], values);
	    }
  }

  private void runAnalyser(ImageProcessor processor){
	  ImagePlus image = new ImagePlus(null, processor);
	  RoiManager manager = new RoiManager(true);
	  // run the particle analyser
	    ResultsTable rt = new ResultsTable();
	    ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES, 
	                ParticleAnalyzer.CENTER_OF_MASS | ParticleAnalyzer.AREA | ParticleAnalyzer.PERIMETER | ParticleAnalyzer.FERET ,
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
	    for(int i=0;i<roiArray.length;i++){
	    	HashMap<String, Double> values = new HashMap<String, Double>(0);
	    	values.put("Area", rt.getValue("Area",i)); 
	    	values.put("Feret", rt.getValue("Feret",i)); 
	    	values.put("Perim", rt.getValue("Perim.",i)); 
	    	values.put("XM", rt.getValue("XM",i)); 
	    	values.put("YM", rt.getValue("YM",i)); 

	    	this.roiMap.put(roiArray[i], values);
	    }
  }

}