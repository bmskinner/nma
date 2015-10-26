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
package analysis.nucleus;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import components.generic.BooleanProfile;
import components.generic.Profile;
import components.nuclear.NucleusBorderSegment;

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
	private static final int ANGLE_THRESHOLD= 180; // a maximum must be above this, a minimum below it
	
	
	private Profile profile; // the profile to segment
	List<NucleusBorderSegment> segments = new ArrayList<NucleusBorderSegment>(0);
	
	private Logger logger;
		
	/**
	 * Constructed from a profile
	 * @param p
	 */
	public ProfileSegmenter(Profile p, Logger logger){
		if(p==null){
			throw new IllegalArgumentException("Profile is null");
		}
		this.profile = p;
//		logger = new Logger(debugFile, "ProfileSegmenter");
		this.logger = logger;
	}
	
	/**
	 * Get the deltas and find minima and maxima. These switch between segments
	 * @return a list of segments
	 */
	public List<NucleusBorderSegment> segment(){
		BooleanProfile maxima = this.profile.smooth(SMOOTH_WINDOW).getLocalMaxima(MAXIMA_WINDOW, ANGLE_THRESHOLD);
		BooleanProfile minima = this.profile.smooth(SMOOTH_WINDOW).getLocalMinima(MAXIMA_WINDOW, ANGLE_THRESHOLD);
		
		BooleanProfile breakpoint = minima.or(maxima);
		
		Profile deltas = this.profile.smooth(SMOOTH_WINDOW).calculateDeltas(DELTA_WINDOW); // minima and maxima should be near 0 
		Profile dDeltas = deltas.smooth(SMOOTH_WINDOW).calculateDeltas(DELTA_WINDOW); // second differential
		double dMax = dDeltas.getMax();
		double dMin = dDeltas.getMin();
		double variationRange = Math.abs(dMax - dMin);
		double minRateOfChange = variationRange*0.02;

		int segmentStart = 0;
		int segmentEnd = 0;
		int segLength = 0;
		int segCount = 0;
				
		try{
			
			/*
			 * Iterate through the profile, looking for breakpoints
			 * A new segment should always be called at the reference point, 
			 * regardless of minima, since the segments in this region are 
			 * used in frankenprofiling
			 */
			for(int index=0; index<profile.size(); index++){
				segmentEnd = index;
				segLength++;

				// when we get to the end of the profile, seglength must  be discounted, so we can wrap
				// ditto for the beginning of the profile
				if(index>profile.size()-MIN_SEGMENT_SIZE || index<MIN_SEGMENT_SIZE){
					segLength = MIN_SEGMENT_SIZE;
				}

				// We want a minima or maxima, and the value must be distinct from its surroundings			
				if( (   breakpoint.get(index)==true 
						&& Math.abs(dDeltas.get(index)) > minRateOfChange
						&& segLength >= MIN_SEGMENT_SIZE)){
					
					// we've hit a new segment
					NucleusBorderSegment seg = new NucleusBorderSegment(segmentStart, segmentEnd, profile.size());
					seg.setName("Seg_"+segCount);

					segments.add(seg);
					
					logger.log(Level.FINE, "New segment found: "+seg.toString());

					segmentStart = index; // start the next segment at this position
					segLength=0;
					segCount++;
				}
			}
			
			/*
			 * End of the profile; call a new segment boundary to avoid
			 * linking across the reference point
			 * 
			 * If a boundary is already called at index 0 in the first segment,
			 * do not create a new segment, as it would have 1 length
			 * 
			 * If a boundary is already called at the last index in the profile,
			 *  do not add a terminal segment, and just allow merging
			 */
			if(  (segments.get(0).getEndIndex()!=0) && segments.get(segments.size()-1).getEndIndex() != profile.size()-1 ) {
				NucleusBorderSegment seg = new NucleusBorderSegment(segmentStart, segmentEnd, profile.size());
				seg.setName("Seg_"+segCount);
				segments.add(seg);
				logger.log(Level.FINE, "Terminal segment found: "+seg.toString());
				
			} else {
				// the first segment is not larger than the minimum size
				// We need to merge the first and last segments
				
				logger.log(Level.FINE, "Terminal segment not needed: first segment has index 0 or last has full index");
			}
			
			NucleusBorderSegment.linkSegments(segments);
			
			logger.log(Level.FINE, "Segments linked");
			for(NucleusBorderSegment s : segments){
				logger.log(Level.FINE, s.toString());
			}
			

		} catch (Exception e){
			logger.log(Level.SEVERE, "Error in segmentation", e);
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
