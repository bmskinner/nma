/*
  -----------------------
  IMAGE ALIGNER
  -----------------------
  Given two images, find the offset that best
  aligns them
*/  
package no.analysis;
import java.util.*;
import no.nuclei.*;
import no.utility.*;
import no.collections.*;
import no.components.*;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.plugin.ChannelSplitter;
import ij.plugin.filter.Analyzer;
import ij.process.ImageProcessor;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;


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
  * Constructor. Takes two ImagePlus images. These should be geryscale, i.e.
  * have only one channel. 
  *
  * @param staticImage the image that will be used as a reference
  * @param testImage the image that will be moved to best fit
  */
  public ImageAligner(ImagePlus staticImage, ImagePlus testImage){
    this.staticImage = staticImage;
    this.testImage = testImage;
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

  private void maskImage(ImagePlus image, int threshold){
    ImageProcessor ip = image.getProcessor();
    ip.threshold(threshold);
    ip.invert();
  }

  private void offsetImage(ImagePlus image, int x, int y){
    ImageProcessor ip = image.getProcessor();
    ip.setBackgroundValue(255);
    ip.translate(x, y);
  }

  private int compareImages(){
    int height = staticImage.getHeight();
    int width = staticImage.getWidth();
    int score = 0;

    for(int i=0; i<height; i++){
      for(int j=0; j<width; j++){
        int a = staticImage.getPixel(i, j)[0]; // greyscale values are in the first index
        int b = testImage.getPixel(i, j)[0];
        if(a&b ==1){
          score++;
        }
      }
    }
    return score;
  }

  private void findBestOffset(){

    int bestScore = compareImages();
    int bestX = 0;
    int bestY = 0;

    for(int x= xOffset-this.range; x<this.range;x++){
      for(int y= yOffset-this.range; y<this.range;y++){
        offsetImage(this.testImage, x, y); // need to make a copy of the image
        int score = compareImages();
        if(score>bestScore){
          bestScore = score;
          bestX = x;
          bestY = y;
        }
      }
    }
  }

}