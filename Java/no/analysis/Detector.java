/*
  -----------------------
  DETECTOR
  -----------------------
  Contains the variables for detecting
  nuclei and signals
*/  
package no.analysis;
import java.util.*;
import no.nuclei.*;
import no.utility.*;
import no.collections.*;
import no.components.*;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.Analyzer;
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

  private Map<Roi, Map<String, Double>> roiMap = new HashMap<Roi, HashMap<String, Double>>(0);

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

  public setThreshold(int i){
  	this.threshold = i;
  }

  public setChannel(int i){
  	this.channel = i;
  }

  public void run(ImagePlus image){
  	if(this.minSize==null || this.maxSize==null){
  		return;
  	}
  	if(this.minCirc==null || this.maxCirc==null){
  		return;
  	}
  	if(this.threshold==null){
  		return;
  	}

  	if(	this.channel!=RED_CHANNEL 	&& 
	  		this.channel!=GREEN_CHANNEL && 
	  		this.channel!=BLUE_CHANNEL){
  		return;
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
  public Map<Roi, Map<String, Double>> getRoiMap(){
  	Map<Roi, Map<String, Double>> resultMap = new HashMap<Roi, HashMap<String, Double>>(0);
  	Set<Roi> keys = this.roiMap.keySet();

  	for(Roi r : keys){
  		Map<String, Double> values = roiMap.get(r);
  		Map<String, Double> resultValues = new HashMap<String, Double>(0);
  		Set<String> valueKeys = values.keySet();
  		for( String s : valueKeys){
  			resultValues.put(s, values.get(s));
  		}
  		resultMap.put(r, resultValues);
  	}
  	return resultMap;
  }

  private void findInImage(ImagePlus image){

    RoiManager manager = new RoiManager(true);

    // split out blue channel
    ChannelSplitter cs = new ChannelSplitter();
    ImagePlus[] channels = cs.split(image);
    ImagePlus searchImage = channels[this.channel];
    
    // threshold
    ImageProcessor ip = searchImage.getChannelProcessor();
    ip.smooth();
    ip.threshold(this.threshold);
    ip.invert();

    // run the particle analyser
    ResultsTable rt = new ResultsTable();
    ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES, 
                ParticleAnalyzer.CENTER_OF_MASS | ParticleAnalyzer.AREA, ParticleAnalyzer.PERIMETER, ParticleAnalyzer.FERET ,
                 rt, this.minSize, this.maxSize, this.minCirc, this.maxCirc);
    try {
      pa.setRoiManager(manager);
      boolean success = pa.analyze(searchImage);
      if(!success){
        IJ.log("  Unable to perform particle analysis");
      }
    } catch(Exception e){
       IJ.log("  Error in particle analyser: "+e);
    } finally {
      searchImage.close();
    }

    this.roiArray = manager.getSelectedRoisAsArray();
    for(int i=0;i<roiArray.length;i++){
    	Map<String, Double> values = new HashMap<String, Double>(0);
    	values.put("Area", rt.getValue("Area",i)); 
    	values.put("Feret", rt.getValue("Feret",i)); 
    	values.put("Perim", rt.getValue("Perim.",i)); 
    	values.put("XM", rt.getValue("XM",i)); 
    	values.put("YM", rt.getValue("YM",i)); 

    	this.roiMap.put(roiArray[i], values);
    }
  }


}