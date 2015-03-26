package no.analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.measure.Calibration;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import no.components.NucleusBorderSegment;
import no.components.Profile;

// this is used to divide a median profile into segments of interest
// it can also take a list of segments, and apply them
public class ProfileSegmenter {
	
	private Profile profile; // the profile to segment
	List<NucleusBorderSegment> segments = new ArrayList<NucleusBorderSegment>(0);
	
	public ProfileSegmenter(Profile p){
		if(p==null){
			throw new IllegalArgumentException("Profile is null");
		}
		this.profile = p;
	}
	
	/**
	 * Create using existing segments. Can then be used to draw plots
	 * without calling the segmenting method
	 * @param p the profile
	 * @param n a list of segments
	 */
	public ProfileSegmenter(Profile p, List<NucleusBorderSegment> n){
		this.profile = p;
		this.segments = n;
	}
	
	// get the deltas and find minima and maxima. These switch between segments
	public List<NucleusBorderSegment> segment(){
		Profile maxima = this.profile.smooth(2).getLocalMaxima(5);
		Profile minima = this.profile.smooth(2).getLocalMinima(5);
		Profile deltas = this.profile.smooth(2).calculateDeltas(4);
//		Profile dMax = deltas.getLocalMaxima(3);
//		Profile dMin = deltas.getLocalMinima(3);
		
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
		
		int narrowLine = 3;
		int wideLine = 10;
		int verticalLine = 3;
		
		Plot segPlot = new Plot("Segments", "Position", "Angle");
		segPlot.setSize(600,400);
		segPlot.setLineWidth(narrowLine);
		segPlot.setLimits(0,profile.size(),-50,360);
		
		// draw 180 degree line
		segPlot.setLineWidth(narrowLine);
		segPlot.setColor(Color.DARK_GRAY);
		segPlot.drawLine(0, 180, profile.size(),180);			
		
		// draw the background black median line for contrast
		double[] xpoints = profile.getPositions(profile.size()).asArray();
		double[] ypoints = profile.smooth(2).asArray();
		segPlot.setLineWidth(wideLine);
		segPlot.addPoints(xpoints, ypoints, Plot.LINE);
		segPlot.setLineWidth(narrowLine);
		
		// draw the coloured segments
		int i=0;
		for(NucleusBorderSegment b : segments){
						
			segPlot.setLineWidth(4);
			if(i==0 && segments.size()==9){ // avoid colour wrapping when segment number is 1 more than the colour list
				segPlot.setColor(Color.MAGENTA);
			} else{
				segPlot.setColor(getColor(i));
			}
			if(b.getStartIndex()<b.getEndIndex()){
				segPlot.drawLine(b.getStartIndex(), -30, b.getEndIndex(), -30);
				double[] xPart = Arrays.copyOfRange(xpoints, b.getStartIndex(), b.getEndIndex());
				double[] yPart = Arrays.copyOfRange(ypoints, b.getStartIndex(), b.getEndIndex());
				segPlot.setLineWidth(narrowLine);
				segPlot.addPoints(xPart, yPart, Plot.LINE);
				segPlot.setLineWidth(4);
				
			} else { // handle wrap arounds
				segPlot.drawLine(0, -30, b.getEndIndex(), -30);
				segPlot.drawLine(b.getStartIndex(), -30, profile.size(), -30);
				
				double[] xPart = Arrays.copyOfRange(xpoints, b.getStartIndex(), profile.size()-1);
				double[] yPart = Arrays.copyOfRange(ypoints, b.getStartIndex(), profile.size()-1);
				segPlot.setLineWidth(narrowLine);
				segPlot.addPoints(xPart, yPart, Plot.LINE);
				xPart = Arrays.copyOfRange(xpoints, 0, b.getEndIndex());
				yPart = Arrays.copyOfRange(ypoints, 0, b.getEndIndex());
				segPlot.addPoints(xPart, yPart, Plot.LINE);
				segPlot.setLineWidth(4);
				
			}
			// draw the vertical lines
			segPlot.setColor(Color.LIGHT_GRAY);
			segPlot.setLineWidth(verticalLine);
			segPlot.drawLine(b.getStartIndex(), -50, b.getStartIndex(),360);			
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
