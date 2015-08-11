package no.analysis;


//import ij.gui.Plot;

//import java.awt.Color;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.List;

import no.components.NucleusBorderSegment;
import no.components.Profile;
//import no.gui.ColourSelecter;

// this is used to divide a median profile into segments of interest
// it can also take a list of segments, and apply them
public class ProfileSegmenter {
		
	/**
	 * The smallest number of points a segment can contain. 
	 * Increasing this value will make the segment fitting more robust, 
	 * but reduces resolution
	 */
	public static final int MIN_SEGMENT_SIZE = 10;
	
	
	private static final int SMOOTH_WINDOW	= 2; // the window size for smoothing profiles
	private static final int MAXIMA_WINDOW	= 5; // the window size for calculating minima and maxima
	private static final int DELTA_WINDOW	= 2; // the window size for calculating deltas
	
	
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
	
	/**
	 * Get the deltas and find minima and maxima. These switch between segments
	 * @return a list of segments
	 */
	public List<NucleusBorderSegment> segment(){
		Profile maxima = this.profile.smooth(SMOOTH_WINDOW).getLocalMaxima(MAXIMA_WINDOW);
		Profile minima = this.profile.smooth(SMOOTH_WINDOW).getLocalMinima(MAXIMA_WINDOW);
		Profile either = minima.add(maxima);
		Profile deltas = this.profile.smooth(SMOOTH_WINDOW).calculateDeltas(DELTA_WINDOW); // minima and maxima should be near 0 
		Profile dDeltas = deltas.smooth(SMOOTH_WINDOW).calculateDeltas(DELTA_WINDOW); // second differential
		double dMax = dDeltas.getMax();
		double dMin = dDeltas.getMin();
		double variationRange = Math.abs(dMax - dMin);

		int segmentStart = 0;
		int segmentEnd = 0;
		int segLength = 0;
		int segCount = 0;
		for(int i=0;i<profile.size();i++){
			segmentEnd = i;
			segLength++;
			
			// when we get to the end of the profile, seglength must  be discounted, so we can wrap
			// ditto for the beginning of the profile
			if(i>profile.size()-ProfileSegmenter.MIN_SEGMENT_SIZE || i<ProfileSegmenter.MIN_SEGMENT_SIZE){
				segLength = ProfileSegmenter.MIN_SEGMENT_SIZE;
			}

			// We want a minima or maxima, and the value must be distinct from its surroundings			
			if( either.get(i)==1 
					&& Math.abs(dDeltas.get(i)) > variationRange*0.02
					&& segLength>= ProfileSegmenter.MIN_SEGMENT_SIZE){
				// we've hit a new segment
				NucleusBorderSegment seg = new NucleusBorderSegment(segmentStart, segmentEnd);
				seg.setSegmentType("Seg_"+segCount);
				segments.add(seg);
				segmentStart = i;
				segLength=0;
				segCount++;
			}
		}
		// join up segments at start and end of profile if needed
		NucleusBorderSegment seg = segments.get(0);
		seg.update(segmentStart, seg.getEndIndex()); // merge the segments around 0	

		return segments;
	}
	
	/**
	 * For debugging. Print the details of each segment found 
	 */
	public void print(){
		for(NucleusBorderSegment s : segments){
			s.print();
		}
	}
		
	/**
	 * Draw the current segmented profile to screen
	 * @param filename the absolute path of the file to save as
	 */
//	public void draw(String filename){
//		
//		int narrowLine = 3;
//		int wideLine = 10;
//		int verticalLine = 3;
//		
//		Plot segPlot = new Plot("Segments", "Position", "Angle");
//		segPlot.setSize(600,400);
//		segPlot.setLineWidth(narrowLine);
//		segPlot.setLimits(0,profile.size(),-50,360);
//		
//		// draw 180 degree line
//		segPlot.setLineWidth(narrowLine);
//		segPlot.setColor(Color.DARK_GRAY);
//		segPlot.drawLine(0, 180, profile.size(),180);			
//		
//		// draw the background black median line for contrast
//		double[] xpoints = profile.getPositions(profile.size()).asArray();
//		double[] ypoints = profile.smooth(2).asArray();
//		segPlot.setLineWidth(wideLine);
//		segPlot.addPoints(xpoints, ypoints, Plot.LINE);
//		segPlot.setLineWidth(narrowLine);
//		
//		// draw the coloured segments
////		IJ.log("");
//		int i=0;
//		for(NucleusBorderSegment b : segments){
//			
////			IJ.log("Segment "+i);
////			b.print();
//						
//			segPlot.setLineWidth(4);
//			if(i==0 && segments.size()==ColourSelecter.getSegmentListSize()+1){ // avoid colour wrapping when segment number is 1 more than the colour list
//				segPlot.setColor(Color.MAGENTA);
//			} else{
//				segPlot.setColor(ColourSelecter.getSegmentColor(i));
//			}
//			
////			IJ.log("    Colour: "+getColor(i));
//			if(b.getStartIndex()<b.getEndIndex()){
//				
//				// draw the coloured line at the base of the plot
//				segPlot.drawLine(b.getStartIndex(), -30, b.getEndIndex(), -30);
////				IJ.log("    Line from "+b.getStartIndex()+" to "+b.getEndIndex());
//				
//				// draw the section of the profile
//				double[] xPart = Arrays.copyOfRange(xpoints, b.getStartIndex(), b.getEndIndex());
//				double[] yPart = Arrays.copyOfRange(ypoints, b.getStartIndex(), b.getEndIndex());
//				segPlot.setLineWidth(narrowLine);
//				segPlot.addPoints(xPart, yPart, Plot.LINE);
//				segPlot.setLineWidth(4);
//				
//			} else { // handle wrap arounds
//				segPlot.drawLine(0, -30, b.getEndIndex(), -30);
//				segPlot.drawLine(b.getStartIndex(), -30, profile.size(), -30);
////				IJ.log("    Line from 0 to "+b.getEndIndex()+" and "+b.getStartIndex()+" to "+profile.size());
//				
//				double[] xPart = Arrays.copyOfRange(xpoints, b.getStartIndex(), profile.size()-1);
//				double[] yPart = Arrays.copyOfRange(ypoints, b.getStartIndex(), profile.size()-1);
//				segPlot.setLineWidth(narrowLine);
//				segPlot.addPoints(xPart, yPart, Plot.LINE);
//				xPart = Arrays.copyOfRange(xpoints, 0, b.getEndIndex());
//				yPart = Arrays.copyOfRange(ypoints, 0, b.getEndIndex());
//				segPlot.addPoints(xPart, yPart, Plot.LINE);
//				segPlot.setLineWidth(4);
//				
//			}
//			// draw the vertical lines
//			segPlot.setColor(Color.LIGHT_GRAY);
//			segPlot.setLineWidth(verticalLine);
//			segPlot.drawLine(b.getStartIndex(), -50, b.getStartIndex(),360);			
//			segPlot.setLineWidth(1);
//			i++;
//		}
//		segPlot.show();
//				
//		
//		Profile deltas = this.profile.smooth(2).calculateDeltas(2);
//		Plot deltaPlot = new Plot("deltas", "position", "deltas");
//		deltaPlot.setLimits(0,deltas.size(),-100,100);
//		deltaPlot.setSize(400,300);
//		deltaPlot.addPoints(deltas.getPositions(deltas.size()).asArray(), deltas.asArray(), Plot.LINE);
//		for(NucleusBorderSegment b : segments){
//			deltaPlot.setColor(Color.BLUE);
//			deltaPlot.drawLine(b.getStartIndex(), -100, b.getStartIndex(),100);
//		}
//		deltaPlot.show();
//		
//		Profile dDeltas = deltas.smooth(2).calculateDeltas(2);
//		Plot dDeltaPlot = new Plot("Second differential", "position", "deltas");
//		dDeltaPlot.setLimits(0,dDeltas.size(),-100,100);
//		dDeltaPlot.setSize(400,300);
//		dDeltaPlot.addPoints(dDeltas.getPositions(dDeltas.size()).asArray(), dDeltas.asArray(), Plot.LINE);
//		for(NucleusBorderSegment b : segments){
//			dDeltaPlot.setColor(Color.BLUE);
//			dDeltaPlot.drawLine(b.getStartIndex(), -100, b.getStartIndex(),100);
//		}
//		dDeltaPlot.show();
//		
//	}
	
}
