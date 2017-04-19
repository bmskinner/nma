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
  IMAGE ALIGNER
  -----------------------
  Given two images, find the offset that best
  aligns them
*/  
package com.bmskinner.nuclear_morphology.analysis.image;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.process.ImageProcessor;


public class ImageAligner{

  private ImagePlus staticImage; // the image that is used as the reference
  private ImagePlus testImage; // the image that will be offset to match the reference

  private int xOffset = 0;
  private int yOffset = 0;

  /** 
  * The max number of pixels to move in any direction. A value of 50
  * would mean a range of -50 to 50 x and -50 to 50 y
  */
  private int range = 50; 

  /**
  * Constructor. Takes two ImagePlus images. These should be greyscale, i.e.
  * have only one channel. 
  *
  * @param staticImage the image that will be used as a reference
  * @param testImage the image that will be moved to best fit
  */
  public ImageAligner(ImagePlus staticImage, ImagePlus testImage, int threshold){
    this.staticImage = staticImage;
    this.testImage = testImage;
    maskImage(this.staticImage, threshold);
    maskImage(this.testImage, threshold);
  }

  /**
  * Set an initial value for the x offset
  *
  * @param i the x-value
  */
  public void setXOffset(int i){
    this.xOffset = i;
  }

  /**
  * Set an initial value for the y offset
  *
  * @param i the y-value
  */
  public void setYOffset(int i){
    this.yOffset = i;
  }

  public int getXOffset(){
    return this.xOffset;
  }

  public int getYOffset(){
    return this.yOffset;
  }

  public void run(){

    int bestScore = compareImages(this.staticImage, this.testImage);
    int bestX = 0;
    int bestY = 0;

    int interval = 5; // must be smaller than nuclear size to ensure some hits

    // perform the offsets at a rough resolution, then go finer
     for(int x= xOffset-this.range; x<xOffset+this.range+1;x+=interval){
      for(int y= yOffset-this.range; y<yOffset+this.range+1; y+=interval){
        
        ImagePlus offsetImage = new ImagePlus("offset", testImage.getProcessor().duplicate());
        offsetImage(offsetImage, x, y); // need to use a copy of the image
        int score = compareImages(this.staticImage, offsetImage);
        offsetImage.close();
        if(score<bestScore){ // minimise blacks; best overlap
          bestScore = score;
          bestX = x;
          bestY = y;
        }
      }
    }


    for(int x=bestX-(interval-1); x<bestX+interval;x++){
      for(int y=bestY-(interval-1); y<bestY+interval; y++){
        
        ImagePlus offsetImage = new ImagePlus("offset", testImage.getProcessor().duplicate());
        offsetImage(offsetImage, x, y); // need to use a copy of the image
        int score = compareImages(this.staticImage, offsetImage);
        offsetImage.close();
        if(score<bestScore){
          bestScore = score;
          bestX = x;
          bestY = y;
        }
      }
    }

    IJ.log("  Images aligned at: x: "+bestX+" y:"+bestY);

    this.xOffset = bestX;
    this.yOffset = bestY;
  }

  private void maskImage(ImagePlus image, int threshold){
    ImageProcessor ip = image.getProcessor();
    ip.threshold(threshold);
    ip.invert();
  }

  private void offsetImage(ImagePlus image, int x, int y){
    ImageProcessor ip = image.getProcessor();
    ip.setValue(0);
    ip.setBackgroundValue(0);
    ip.setColor(0);
    ip.translate(x, y);

    // cannot get background to white, so need to change manually
    int roiXmin = x > 0 ? 0 : ip.getWidth()+x;
    int roiXmax = x > 0 ? x : ip.getWidth();

    int roiYmin = y > 0 ? 0 : ip.getHeight()+y;
    int roiYmax = y > 0 ? y : ip.getHeight();
    
    // ip.snapshot();
    ip.setRoi(roiXmin, 0, roiXmax, ip.getHeight());
    ip.setColor(255);
    ip.fill();
    ip.resetRoi();
    ip.setRoi(0, roiYmin, ip.getWidth(), roiYmax);
    ip.setColor(255);
    ip.fill();
    ip.resetRoi();
  }

  private int compareImages(ImagePlus image1, ImagePlus image2){
    int height = image1.getHeight();
    int width = image1.getWidth();
    int score = 0;

    ImageCalculator ic = new ImageCalculator();
    ImagePlus imp3 = ic.run("and create", image1, image2);

    for(int i=0; i<height; i++){
      for(int j=0; j<width; j++){
        int pixel = imp3.getPixel(i,j)[0];
        if(pixel==0){ // if black
          score++;
        }
      }
    }
    imp3.close();
    return score;
  }

}