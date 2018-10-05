/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Divide a profile into segments of interest based on minima and maxima.
 */
public class ProfileSegmenter implements Loggable {

    /**
     * The smallest number of points a segment can contain. Increasing this
     * value will make the segment fitting more robust, but reduces resolution
     */
    public static final int MIN_SEGMENT_SIZE = 10;

    /** Window size for smoothing profiles prior to minima and maxima testing */
    private static final int SMOOTH_WINDOW   = 2;
    
    /**  Window size for calculating minima and maxima */
    private static final int MAXIMA_WINDOW   = 5;
        
    /**
     * Threshold for calling minima and maxima; a maximum must be above
     * this value, a minimum must be below it
     */
    private static final int ANGLE_THRESHOLD = 180;
    
    /** The exclusion zone above and below the threshold, as a fraction
     * of the threshold value. If this were 0.10, for an angle profile, 
     * with threshold 180, this is 18 degrees, or a range of 162-198 within
     *  which min/maxima  will not be called */
    private static final double THRESHOLD_FRACTION = 0.10;

    private final IProfile profile; // the profile to segment
    private final List<IBorderSegment> segments = new ArrayList<>();

    private BooleanProfile minOrMax = null;

    /**
     * These are points at which a segment boundary must be called.
     * Specifying indexes here will suppress automatic segmentation at points
     * within MIN_SEGMENT_SIZE of the index. If two Tag indexes in this 
     * list are within MIN_SEGMENT_SIZE of each other, only the first will 
     * be assigned.
     */
    private int[] mustSplit = null;

    /**
     * Constructed from a profile
     * 
     * @param p
     */
    public ProfileSegmenter(@NonNull final IProfile p) {
        if (p == null)
            throw new IllegalArgumentException("Profile is null");
        profile = p;
        initialise();
    }

    /**
     * Construct from a profile, and indexes which must be
     * segmented upon.
     * 
     * @param p the profile
     * @param map the indexes to segment at (RP is automatic)
     */
    public ProfileSegmenter(@NonNull final IProfile p, @NonNull final List<Integer> map) {
        this(p);
        if (map == null)
            throw new IllegalArgumentException("Index map is null");
        mustSplit = validateBorderTagMap(map);
    }
    
    /**
     * Get the deltas and find minima and maxima. These switch between segments
     * 
     * @param splitIndex an index point that must be segmented on
     * @return a list of segments
     */
    public List<IBorderSegment> segment() throws UnsegmentableProfileException {
    	fine("-------------------------");
    	fine("Beginning segmentation   ");
    	fine("-------------------------");
        /*
         * Prepare segment start index
         */
        int segmentStart = 0;
        finer("Profile length "+profile.size());
        
        /*
         * Iterate through the profile, looking for breakpoints The reference
         * point is at index 0, as defined by the calling method.
         * 
         * Therefore, a segment should always be started at index 0
         */
        for (int index = 0; index < profile.size(); index++) {

            if (isValidSegmentEnd(index, segmentStart)) {

                // we've hit a new segment
                IBorderSegment seg = IBorderSegment.newSegment(segmentStart, index, profile.size());

                segments.add(seg);

                fine("New segment found in profile: " + seg.toString());

                segmentStart = index; // Prepare for the next segment
            }

        }

        /*
         * Now, there is a list of segments which covers most but not all of the
         * profile. The final segment has not been defined: is is the current
         * segment start, but has not had an endpoint added. Since segment ends
         * cannot be called within MIN_SIZE of the profile end, there is enough
         * space to make a segment running from the current segment start back
         * to index 0
         */
        IBorderSegment seg = IBorderSegment.newSegment(segmentStart, 0, profile.size());
        segments.add(seg);

        try {
            IBorderSegment.linkSegments(segments);
        } catch (ProfileException e) {
            warn("Cannot link segments in profile");
            throw new UnsegmentableProfileException("Error making final profile", e);
        }

        finer("Segments linked");
        fine(String.format("Created %s segments in profile", segments.size()));
        return segments;
    }

    
    
    /**
     * Check that the given map is suitable for segmentation. If two
     * BorderTagObject indexes in this list are within MIN_SEGMENT_SIZE of each
     * other, the second will be silently removed.
     */
    private int[] validateBorderTagMap(List<Integer> initialMap) {

    	List<Integer> toRemove = new ArrayList<>(initialMap.size());
        for (int index1 : initialMap) {

            for (int index2 : initialMap) {
            	if(index1>=index2)
            		continue;
                // Check if the test is within MIN_SEGMENT_SIZE of tag
            	if (index2 - index1 < MIN_SEGMENT_SIZE) {
            		toRemove.add(index2);
            	}
            }
        }
        
        // Remove the unsuitable tags from the map
        return initialMap.stream()
        		.filter(i->!toRemove.contains(i))
        		.mapToInt(i->i.intValue())
        		.toArray();
    }
    
    private void initialise() {

        /*
         * Find minima and maxima, to set the inflection points in the profile
         */
    	
    	IProfile smoothed = profile.smooth(SMOOTH_WINDOW);
    	
    	minOrMax = smoothed.getLocalMaxima(MAXIMA_WINDOW, ANGLE_THRESHOLD+(ANGLE_THRESHOLD*THRESHOLD_FRACTION))
        		.or(smoothed.getLocalMinima(MAXIMA_WINDOW, ANGLE_THRESHOLD-(ANGLE_THRESHOLD*THRESHOLD_FRACTION)));
    }

    /**
     * @param index the current index being tested
     * @param segmentStart the start of the current segment being built
     * @return true if a segment end can be called at this index
     */
    private boolean isValidSegmentEnd(int index, int segmentStart) {

        /*
         * The first segment must meet the length limit
         */
        if (index < MIN_SEGMENT_SIZE)
            return false;
        
        //What happens if a forced split conflicts with a minimum segment size rule?
        // Priority is to not create segments that are too short. A border tag will
        // not be segmented.
        
        // If the index is within MIN_SEGMENT_SIZE of a forced boundary, must not segment        
        if(mustSplit!=null) {
        	for(int mustSplitIndex : mustSplit) {
        		if (Math.abs(mustSplitIndex-index) < MIN_SEGMENT_SIZE)
        			return false; 
        	}
        }

        // If the index is a forced index boundary, must segment
        if(mustSplit!=null) {
        	for(int mustSplitIndex : mustSplit) {
        		if (mustSplitIndex==index)
        			return true; 
        	}
        }

        // Segment must be long enough
        if (index - segmentStart < MIN_SEGMENT_SIZE)
            return false;

        /*
         * Once the index has got close to the end of the profile, a new segmnet
         * cannot be called, even at a really nice infection point, because it
         * would be too close to the reference point
         */
        if (index > (profile.size()-1)-MIN_SEGMENT_SIZE)
            return false;

        // Must be a minima or maxima        
        if(!minOrMax.get(index))
        	return false;
        return true;
    }

    @Override
    public String toString() {

        StringBuilder b = new StringBuilder();
        for (IBorderSegment s : segments) {
            b.append(s.toString());
            b.append(" | ");
        }
        return b.toString();
    }

    public class UnsegmentableProfileException extends Exception {
        private static final long serialVersionUID = 1L;

        public UnsegmentableProfileException() {
            super();
        }

        public UnsegmentableProfileException(String message) {
            super(message);
        }

        public UnsegmentableProfileException(String message, Throwable cause) {
            super(message, cause);
        }

        public UnsegmentableProfileException(Throwable cause) {
            super(cause);
        }
    }
}
