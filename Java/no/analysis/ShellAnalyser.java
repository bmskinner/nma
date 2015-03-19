/*
  -----------------------
  SHELL ANALYSIS
  -----------------------
  Signal positions in round nuclei.
*/  
package no.analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.io.Opener;
import ij.plugin.filter.Analyzer;
import ij.plugin.ChannelSplitter;
import ij.plugin.RoiEnlarger;
import ij.plugin.RGBStackMerge;
import ij.process.ImageStatistics;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.io.File;
import java.util.*;
import no.nuclei.*;
import no.utility.*;
import no.collections.*;
import no.components.*;

public class ShellAnalyser {

	int shellCount = 5;

	ImagePlus image;
	ImagePlus[] channels;
	Roi originalRoi;

	List<Roi> shells = new ArrayList<Roi>(0);

	List<

	public ShellAnalyser(Roi roi, ImagePlus image){
		this.originalRoi = roi;
		this.image = image;
		ChannelSplitter cs = new ChannelSplitter();
	    this.channels = cs.split(this.image);
	}

	public run(){

		createShells();
		measureSignals(Detector.RED_CHANNEL);

	}

	private get

	private void createShells(){

	    ImagePlus searchImage = channels[Detector.BLUE_CHANNEL];
	    ImageProcessor ip = searchImage.getProcessor();

	    ImageStatistics stats = ImageStatistics.getStatistics(ip, Measurements.AREA, searchImage.getCalibration()); 
     	double initialArea = stats.area;

     	Roi shrinkingRoi = originalRoi.clone();
     	RoiEnlarger enlarger = new RoiEnlarger();

	    for(int i=shellCount; i>0; i--){
	    	
	    	while(area>initialArea* (i/shellCount)){
	    		shrinkingRoi = enlarger.enlarge(shrinkingRoi, -1);
	    		
	     		ip.setRoi(shrinkingRoi); 
	     		ImageStatistics stats = ImageStatistics.getStatistics(ip, Measurements.AREA, searchImage.getCalibration()); 
	     		double area = stats.area;
	     	}
	     	shells.add(shrinkingRoi.clone());
	    }
	}

	private void measureSignals(int channel){

		ImagePlus searchImage = channels[channel];
	    ImageProcessor ip = searchImage.getProcessor();

		for( Roi r : shells){

			ip.setRoi(r);

			ResultsTable rt = new ResultsTable();
			Analyzer analyser = new Analyzer( searchImage, Analyzer.INTEGRATED_DENSITY, rt);
			analyser.measure();

			double density = rt.getValue("IntDen",0);

		}

	}


}