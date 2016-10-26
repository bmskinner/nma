/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package analysis.profiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import components.generic.BooleanProfile;
import components.generic.BorderTagObject;
import components.generic.IProfile;
import components.generic.Tag;
import components.nuclear.NucleusBorderSegment;
import logging.Loggable;

/**
 * Divide a profile into segments of interest based on
 * minima and maxima.
 */
public class ProfileSegmenter implements Loggable {
	
	/**
	 * The smallest number of points a segment can contain. 
	 * Increasing this value will make the segment fitting more robust, 
	 * but reduces resolution
	 */
	public static final int MIN_SEGMENT_SIZE = 10;
	
	private static final int SMOOTH_WINDOW	 = 2; // the window size for smoothing profiles
	private static final int MAXIMA_WINDOW	 = 5; // the window size for calculating minima and maxima
	private static final int DELTA_WINDOW	 = 2; // the window size for calculating deltas
	private static final int ANGLE_THRESHOLD = 180; // a maximum must be above this, a minimum below it
	
	private static final double MIN_RATE_OF_CHANGE = 0.02; // a potential inflection cannot vary by more than this
	
	
	private final IProfile profile; // the profile to segment
	private final List<NucleusBorderSegment> segments = new ArrayList<NucleusBorderSegment>(0);
	
	private BooleanProfile inflectionPoints = null;
	private IProfile        deltaProfile     = null;
	private double         minRateOfChange  = 1;
	
	
	// These are points at which a segment boundary must be called.
	// Specifying indexes here will suppress automatic segmentation at points within
	// MIN_SEGMENT_SIZE of the index. If two BorderTagObject indexes in this list are within
	// MIN_SEGMENT_SIZE of each other, only the first will be assigned.
	private Map<Tag, Integer> tagsToSplitOn = new HashMap<Tag, Integer>(); 
		
	/**
	 * Constructed from a profile
	 * @param p
	 */
	public ProfileSegmenter(final IProfile p){
		if(p==null){
			throw new IllegalArgumentException("Profile is null");
		}
		this.profile = p;

		this.initialise();
		
		fine("Created profile segmenter");
	}
	
	/**
	 * Construct from a profile, and a map specifying BorderTags which must
	 * be segmented upon.
	 * @param p the profile
	 * @param map the border tags to segment at (RP is automatic)
	 */
	public ProfileSegmenter(final IProfile p, final Map<Tag, Integer> map){
		
		this(p);
		if(map==null){
			throw new IllegalArgumentException("Index map is null");
		}
		tagsToSplitOn = map;
		validateBorderTagMap();
		fine("Added map of BorderTagObject indexes to force segmentation");
		
	}
	
	/**
	 * Get the deltas and find minima and maxima. These switch between segments
	 * @param splitIndex an index point that must be segmented on
	 * @return a list of segments
	 */
	public List<NucleusBorderSegment> segment() throws UnsegmentableProfileException {

		/*
		 * Prepare segment start index
		 */
		int segmentStart = 0;




		/*
		 * Iterate through the profile, looking for breakpoints
		 * The reference point is at index 0, as defined by the 
		 * calling method.
		 * 
		 *  Therefore, a segment should always be started at index 0
		 */
		for(int index=0; index<profile.size(); index++){

			if(testSegmentEndFound(index, segmentStart)){

				// we've hit a new segment
				NucleusBorderSegment seg = new NucleusBorderSegment(segmentStart, index, profile.size());

				segments.add(seg);

				fine("New segment found in profile: "+seg.toString());

				segmentStart = index; // Prepare for the next segment
			}

		}

		/*
		 * Now, there is a list of segments which covers most but not all of 
		 * the profile. The final segment has not been defined: is is the current segment
		 * start, but has not had an endpoint added. Since segment ends cannot be
		 * called within MIN_SIZE of the profile end, there is enough space to make a segment
		 * running from the current segment start back to index 0
		 */
		NucleusBorderSegment seg = new NucleusBorderSegment(segmentStart, 0, profile.size());
		segments.add(seg);

		try {
			NucleusBorderSegment.linkSegments(segments);
		} catch (ProfileException e) {
			warn("Cannot link segments in profile");
			throw new UnsegmentableProfileException("Error making final profile", e);
		}

		fine("Segments linked");

		fine(this.toString());

		return segments;
	}
	
	/**
	 * Check that the given map is suitable for segmentation.
	 * If two BorderTagObject indexes in this list are within
	 *  MIN_SEGMENT_SIZE of each other, the second will be silently removed.
	 */
	private void validateBorderTagMap(){
		
		List<Tag> toRemove = new ArrayList<Tag>();
		
		for(Tag tag : tagsToSplitOn.keySet()){
			
			Integer index = tagsToSplitOn.get(tag);
			
			for(Tag test : tagsToSplitOn.keySet()){
				if(test.equals(tag)){
					continue;
				}
				
				// Check if the test is within MIN_SEGMENT_SIZE of tag
				Integer testIndex = tagsToSplitOn.get(test);
				
				if(testIndex >= index){
					
					if(testIndex - index < MIN_SEGMENT_SIZE ){
						toRemove.add(test); // Remove whichever has larger index
						fine("Removing "+test+": too close to "+tag);
					}
					
				} else {
					if( index - testIndex < MIN_SEGMENT_SIZE ){
						toRemove.add(tag);
					}
				}
				
				
			}
			
		}
		
		// Remove the unsuitable tags from the map
		for(Tag tag : toRemove){
			tagsToSplitOn.remove(tag);
		}
		
	}
	
	private void initialise(){

		/*
		 * Find minima and maxima, to set the inflection points in the profile
		 */
		BooleanProfile maxima = this.profile.smooth(SMOOTH_WINDOW).getLocalMaxima(MAXIMA_WINDOW, ANGLE_THRESHOLD);
		BooleanProfile minima = this.profile.smooth(SMOOTH_WINDOW).getLocalMinima(MAXIMA_WINDOW, ANGLE_THRESHOLD);
		inflectionPoints = minima.or(maxima);
		
		/*
		 * Find second derivative rates of change
		 */
		IProfile deltas  = this.profile.smooth(SMOOTH_WINDOW).calculateDeltas(DELTA_WINDOW); // minima and maxima should be near 0 
		deltaProfile =       deltas.smooth(SMOOTH_WINDOW).calculateDeltas(DELTA_WINDOW); // second differential
		
		/*
		 * Find levels of variation within the complete profile
		 */
		double dMax = deltaProfile.getMax();
		double dMin = deltaProfile.getMin();
		double variationRange = Math.abs(dMax - dMin);
		
		minRateOfChange = variationRange * MIN_RATE_OF_CHANGE;
		
	}
	
	

	/**
	 * @param index the current index being tested
	 * @param segmentStart the start of the current segment being built
	 * @return
	 * @throws Exception 
	 */
	private boolean testSegmentEndFound(int index, int segmentStart) {

		/*
		 * The first segment must meet the length limit
		 */
		if(index < MIN_SEGMENT_SIZE ){
			return false;
		}
		
		/*
		 * If the index is a forced BorderTagObject boundary, 
		 * must segment
		 */
		for(Tag tag : tagsToSplitOn.keySet()){
			
			Integer test = tagsToSplitOn.get(tag);
			if(test.intValue()==index){
				finest("Forcing segment for "+test);
				return true;
			}
			
		}
		
		/*
		 * If the index is within MIN_SEGMENT_SIZE of a
		 * forced BorderTagObject boundary, must not segment
		 */
		for(Tag tag : tagsToSplitOn.keySet()){
			
			Integer test = tagsToSplitOn.get(tag);
			if(Math.abs(test.intValue()-index) < MIN_SEGMENT_SIZE){
				return false;
			}
			
		}
		
		

		/*
		 * Segment must be long enough
		 */
		int potentialSegLength = index-segmentStart;
		
		if(potentialSegLength < MIN_SEGMENT_SIZE ){
			return false;
		}
		
		/*
		 * Once the index has got close to the end of the profile, a new segmnet cannot be called,
		 * even at a really nice infection point, because it would be too close to the 
		 * reference point
		 */
		if( index >  profile.size() - MIN_SEGMENT_SIZE ){
			return false;
		}
		
		/*
		 * All length and position checks are passed.
		 * Now test for a good inflection point.
		 * 
		 * We want a minima or maxima, and the value
		 * must be distinct from its surroundings.	
		 */

		if( (   inflectionPoints.get(index)==true 
				&& Math.abs(deltaProfile.get(index)) > minRateOfChange
				&& potentialSegLength >= MIN_SEGMENT_SIZE)){
			
			return true;

		}
		return false;
	}

	
	@Override
	public String toString(){
		
		StringBuilder b = new StringBuilder();
		for(NucleusBorderSegment s : segments){
			b.append(s.toString()+"\n");
		}
		return b.toString();
	}
	
	public class UnsegmentableProfileException extends Exception {
		private static final long serialVersionUID = 1L;
		public UnsegmentableProfileException() { super(); }
		public UnsegmentableProfileException(String message) { super(message); }
		public UnsegmentableProfileException(String message, Throwable cause) { super(message, cause); }
		public UnsegmentableProfileException(Throwable cause) { super(cause); }
	}
}