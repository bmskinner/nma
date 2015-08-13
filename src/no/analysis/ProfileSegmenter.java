package no.analysis;


import ij.IJ;

import java.util.ArrayList;
import java.util.List;

import no.components.NucleusBorderSegment;
import no.components.Profile;

/**
 * Divide a profile into segments of interest based on
 * minima and maxima.
 */
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
	
	/**
	 * Constructed from a profile
	 * @param p
	 */
	public ProfileSegmenter(Profile p){
		if(p==null){
			throw new IllegalArgumentException("Profile is null");
		}
		this.profile = p;
	}
	
	/**
	 * Get the deltas and find minima and maxima. These switch between segments
	 * @return a list of segments
	 */
	public List<NucleusBorderSegment> segment(){
		Profile maxima = this.profile.smooth(SMOOTH_WINDOW).getLocalMaxima(MAXIMA_WINDOW);
		Profile minima = this.profile.smooth(SMOOTH_WINDOW).getLocalMinima(MAXIMA_WINDOW);
		Profile breakpoint = minima.add(maxima);
		Profile deltas = this.profile.smooth(SMOOTH_WINDOW).calculateDeltas(DELTA_WINDOW); // minima and maxima should be near 0 
		Profile dDeltas = deltas.smooth(SMOOTH_WINDOW).calculateDeltas(DELTA_WINDOW); // second differential
		double dMax = dDeltas.getMax();
		double dMin = dDeltas.getMin();
		double variationRange = Math.abs(dMax - dMin);

		int segmentStart = 0;
		int segmentEnd = 0;
		int segLength = 0;
		int segCount = 0;
		
		NucleusBorderSegment prevSegment = null;
		
		try{
			
			// iterate through the profile, looking for breakpoints
			for(int index=0; index<profile.size(); index++){
				segmentEnd = index;
				segLength++;

				// when we get to the end of the profile, seglength must  be discounted, so we can wrap
				// ditto for the beginning of the profile
				if(index>profile.size()-NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH || index<NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH){
					segLength = NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH;
				}

				// We want a minima or maxima, and the value must be distinct from its surroundings			
				if( breakpoint.get(index)==1 
						&& Math.abs(dDeltas.get(index)) > variationRange*0.02
						&& segLength >= NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH){
					
					// we've hit a new segment
					NucleusBorderSegment seg = new NucleusBorderSegment(segmentStart, segmentEnd, profile.size());
					seg.setSegmentType("Seg_"+segCount);

					// if a previous segment has been defined (i.e iteration 1+)
					// set the links
					if(prevSegment!=null){
						seg.setPrevSegment(prevSegment);
						prevSegment.setNextSegment(seg);
					} 
					segments.add(seg);

					prevSegment = seg;
					segmentStart = index; // start the next segment at this position
					segLength=0;
					segCount++;
				}
			}
			// join up segments at start and end of profile
			NucleusBorderSegment first = segments.get(0);
			
			// the start point of the first segment must be set to the end point of the 
			// last segment before they can be linked
			first.update(prevSegment.getEndIndex(), first.getEndIndex());
			
			// Now link them together
			prevSegment.setNextSegment(first);
			first.setPrevSegment(prevSegment);

		} catch (Exception e){
			IJ.log("Error in segmentation: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				IJ.log(e1.toString());
			}
		}
		
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
