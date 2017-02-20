/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
/*
  -----------------------
  DETECTOR
  -----------------------
  Contains the variables for detecting
  nuclei and signals
*/  
package com.bmskinner.nuclear_morphology.analysis.detection;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import com.bmskinner.nuclear_morphology.logging.Loggable;


/**
 * This abstract class is extended for detecting objects
 * in images. This provides methods for setting size, circularity
 * and thresholding criteria. It calls ImageJ's RoiManager to 
 * find the ROIs in the image.
 * @author bms41
 *
 */
public abstract class Detector implements Loggable {

	public static final int CLOSED_OBJECTS = 0; // Flags to allow detection of open or closed objects
	public static final int OPEN_OBJECTS   = 1;
	
	public static final String COM_X = "XM";
	public static final String COM_Y = "YM";
	
	private double minSize;
	private double maxSize;
	private double minCirc;
	private double maxCirc;

	private int  threshold;

	private List<Roi> roiList; // the detected ROIs
	
	public void setSize(double min, double max){
		minSize = min;
		maxSize = max;
	}
	
	public void setCirc(double min, double max){
		minCirc = min;
		maxCirc = max;
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
  
  protected List<Roi> detectRois(ImageProcessor image){
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

	  findInImage(image);
	  return roiList;
  }
  
  /**
   * Get the stats for the region covered by the given roi. Uses the channel
   * previously set.
   * @param roi the region to measure
   * @return
   */
  public StatsMap measure(Roi roi, ImageProcessor image ){
	  
	  if(image==null || roi==null){
		  throw new IllegalArgumentException("Image or roi is null");
	  }

	  ImageProcessor searchProcessor = image.duplicate();
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
	  values.add(COM_X, rt.getValue(COM_X,0)); 
	  values.add(COM_Y, rt.getValue(COM_Y,0)); 
	  return values;
  }
  
  /*
   *  PRIVATE METHODS
   * 
   * 
   */

  
  private void findInImage(ImageProcessor image){

	  if( ! (image instanceof ByteProcessor  || image instanceof ShortProcessor) ){
		  throw new IllegalArgumentException("Processor must be byte or short");
	  }
	  
	  ImageProcessor searchProcessor = image.duplicate();

	  searchProcessor.threshold(this.threshold);

	  roiList = this.runAnalyser(searchProcessor);
	  if(roiList.size()==0){
		  searchProcessor.invert(); // Work PC needs the inversion; MANETHEREN does not. Don't yet know why.
		  roiList = this.runAnalyser(searchProcessor);
	  }
  }

  private List<Roi> runAnalyser(ImageProcessor processor){
	  ImagePlus image    = new ImagePlus(null, processor);
	  RoiManager manager = new RoiManager(true);
	 
	  // run the particle analyser
	  ResultsTable rt     = new ResultsTable();
	  ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER 
			  | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES 
			  | ParticleAnalyzer.INCLUDE_HOLES, 
			  ParticleAnalyzer.FERET ,
			  rt, minSize, maxSize, minCirc, maxCirc);
	  
	  try {
		  ParticleAnalyzer.setRoiManager(manager);
		  boolean success = pa.analyze(image);
		  if(!success){
			  finest("Unable to perform particle analysis");
		  }
	  } catch(Exception e){
		  warn("Error in particle analyser");
		  stack("Error in particle analyser", e);
	  } finally {
		  image.close();
	  }
	  return Arrays.asList( manager.getSelectedRoisAsArray());
  }
  
 

}