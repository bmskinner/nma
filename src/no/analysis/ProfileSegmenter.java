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
			
}
