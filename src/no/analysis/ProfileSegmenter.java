package no.analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.measure.Calibration;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.utility.Utils;
//import no.components.ProfileFeature;

// this is used to divide a median profile into segments of interest
public class ProfileSegmenter {
	
//	private ProfileFeature features = new ProfileFeature(); // hold the found features
	private Profile profile; // the profile to segment
	List<NucleusBorderSegment> segments = new ArrayList<NucleusBorderSegment>(0);
	
	public ProfileSegmenter(Profile p){
		if(p==null){
			throw new IllegalArgumentException("Profile is null");
		}
		this.profile = p;
	}
	
	// get the deltas and find minima and maxima. These switch between segments
	public List<NucleusBorderSegment> segment(){
		Profile maxima = this.profile.smooth(2).getLocalMaxima(5);
		Profile minima = this.profile.smooth(2).getLocalMinima(5);
		Profile deltas = this.profile.smooth(2).calculateDeltas(4);
		Profile dMax = deltas.getLocalMaxima(3);
		Profile dMin = deltas.getLocalMinima(3);
		
		int segmentStart = 0;
		int segmentEnd = 0;
		for(int i=0;i<profile.size();i++){
			segmentEnd = i;
			// we want a minima or maxima, and the rate of change must be enough to warrent a segemnt
			if( ( maxima.get(i)==1 || minima.get(i)==1 ) 
				 && Math.abs(deltas.get(i))> Math.max( deltas.getMax()*0.05, deltas.getMin()*0.05)){
//			if((dMax.get(i)==1 || dMin.get(i)==1) && Math.abs(deltas.get(i))>5){
				// we've hit a new segment
				NucleusBorderSegment seg = new NucleusBorderSegment(segmentStart, segmentEnd);
				segments.add(seg);
				segmentStart = i;
			}
		}
		// join up segments at start and end of profile 
		NucleusBorderSegment seg = segments.get(0);
		seg.update(segmentStart, seg.getEndIndex()); // runs through 0
		return segments;
	}
	
	public void print(){
		for(NucleusBorderSegment s : segments){
			s.print();
		}
	}
	
	public static Color getColor(int i){
		Color color = 	i%8==0 ? Color.RED : 
			i%8==1 ? Color.ORANGE :
			i%8==2 ? Color.GREEN :
			i%8==3 ? Color.MAGENTA :
			i%8==4 ? Color.CYAN :
			i%8==5 ? Color.YELLOW : 
			i%8==6 ? Color.PINK :
					 Color.LIGHT_GRAY;
		return color;
	}
	
	public void draw(String filename){
		
		Plot segPlot = new Plot("Segments", "Position", "Angle");
		segPlot.setSize(600,400);
		segPlot.setLineWidth(1);
		segPlot.setLimits(0,profile.size(),-50,360);
		segPlot.addPoints(profile.getPositions(profile.size()).asArray(), profile.smooth(2).asArray(), Plot.LINE);
		int i=0;
		for(NucleusBorderSegment b : segments){
			segPlot.setColor(Color.LIGHT_GRAY);
			segPlot.setLineWidth(1);
			segPlot.drawLine(b.getStartIndex(), -50, b.getStartIndex(),360);
			
			segPlot.setLineWidth(4);
			segPlot.setColor(getColor(i));
			if(b.getStartIndex()<b.getEndIndex()){
				segPlot.drawLine(b.getStartIndex(), -30, b.getEndIndex(), -30);
			} else { // hand wrap arounds
				segPlot.drawLine(0, -30, b.getEndIndex(), -30);
				segPlot.drawLine(b.getStartIndex(), -30, profile.size(), -30);
			}
			segPlot.setLineWidth(1);
			i++;
		}
		
		ImagePlus image = segPlot.getImagePlus();
	    Calibration cal = image.getCalibration();
	    cal.setUnit("pixels");
	    cal.pixelWidth = 1;
	    cal.pixelHeight = 1;
	    IJ.saveAsTiff(image, filename);
		
//		Profile deltas = this.profile.smooth(2).calculateDeltas(4);
//		Plot deltaPlot = new Plot("deltas", "position", "deltas");
//		deltaPlot.setLimits(0,deltas.size(),-100,100);
//		deltaPlot.setSize(400,300);
//		deltaPlot.addPoints(deltas.getPositions(deltas.size()).asArray(), deltas.asArray(), Plot.LINE);
//		for(NucleusBorderSegment b : segments){
//			deltaPlot.setColor(Color.BLUE);
//			deltaPlot.drawLine(b.getStartIndex(), -100, b.getStartIndex(),100);
//		}
//		deltaPlot.show();
		
	}

}
