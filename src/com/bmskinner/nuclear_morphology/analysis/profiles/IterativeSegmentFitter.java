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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Carry out iterative fitting of segments from a target profile to match
 * a template profile
 * @author bms41
 * @since 1.14.0
 *
 */
public class IterativeSegmentFitter implements Loggable {

    @NonNull private final ISegmentedProfile templateProfile;

    /**
     * Construct with a profile containing segments. The originals will
     * not be modified.
     * 
     * @param profile the template profile with segments to be fitted
     * @throws ProfileException if the template profile cannot be copied
     */
    @SuppressWarnings("null")
	public IterativeSegmentFitter(@NonNull final ISegmentedProfile template) throws ProfileException {
        if (template == null)
            throw new IllegalArgumentException("Template profile is null");
        templateProfile = template.copy();
    }

    /**
     * Find the best fit positions of the segment endpoints to the target profile
     * 
     * @param target the profile to fit to the current template profile 
     * @return the profile with fitted segments, or on error, the original profile
     */
    public ISegmentedProfile fit(@NonNull final IProfile target) {
    	finer("-------------------------");
    	finer("Beginning segment fitting");
    	finer("-------------------------");
    	
        if (target==null)
            throw new IllegalArgumentException("Target profile is null");
        
        if(templateProfile.getSegmentCount()==1)
        	return new SegmentedFloatProfile(target);

		try {
			return remapSegmentEndpoints(target);
		} catch (UnavailableComponentException | ProfileException e) {
			fine("Unable to remap segments in profile: "+e.getMessage(), e);
			if(target instanceof ISegmentedProfile)
				return (ISegmentedProfile)target;
			throw new IllegalArgumentException("Could not segment profile");
		}
    }

    /**
     * 
     * @param profile the profile to fit against the template profile
     * @return a profile with best-fit segmentation to the median
     * @throws ProfileException 
     * @throws UnavailableComponentException 
     */
    private ISegmentedProfile remapSegmentEndpoints(@NonNull IProfile profile) throws ProfileException, UnavailableComponentException {

        List<IBorderSegment> newSegments = new ArrayList<>();
        
        // fit each segment in turn
        for(IBorderSegment templateSegment : templateProfile.getOrderedSegments())
        	newSegments = bestFitSegment(profile, newSegments, templateSegment.getID());

        for(IBorderSegment s : newSegments) // unlock after fitting
        	s.setLocked(false);
        return new SegmentedFloatProfile(profile, newSegments);
    }

    /**
     * Find the best fit offset for the given segment id.
     * The segments of the profile are cleared, and the 
     * best fit for the segment is found within the constraints of
     * segment numbering.
     * 
     * @param profile
     * @param id the segment to test
     * @return
     * @throws ProfileException 
     * @throws UnavailableComponentException 
     */
    private List<IBorderSegment> bestFitSegment(@NonNull IProfile profile, List<IBorderSegment> segmentsSoFar, @NonNull UUID id) throws ProfileException, UnavailableComponentException {
    	
    	// Start by adding locked segments back to the profile
    	List<IBorderSegment> newSegments = new ArrayList<>();
    	for(IBorderSegment s : segmentsSoFar) {
    		if(s.isLocked())
    			newSegments.add(s);
    	}
    	
    	IBorderSegment templateSegment = templateProfile.getSegment(id);
    	// If it is the last segment, just link to the first and return
    	if(templateSegment.getPosition()==templateProfile.getSegmentCount()-1) {
    		IBorderSegment prevSegment = newSegments.get(newSegments.size()-1);
    		IBorderSegment lastSegment = new DefaultBorderSegment(prevSegment.getEndIndex(), 0, profile.size(), id);
    		lastSegment.setLocked(true);
    		finer("Adding final segment "+lastSegment.getDetail());
    		newSegments.add(lastSegment);
    		IBorderSegment.linkSegments(newSegments);
    		return newSegments;
    	}
    	
    	// Otherwise, find the best matching position for the given segment
    	// Add the segment to the result profile
    	// Lock the segment

        // The start index for the segment is fixed
        int startIndex = segmentsSoFar.size()>0 ? segmentsSoFar.get(segmentsSoFar.size()-1).getEndIndex():0;
        
        // the lowest index that can be applied to the end of this segment
        int minEnd = segmentsSoFar.size()>0 ? segmentsSoFar.get(segmentsSoFar.size()-1).getEndIndex() + IBorderSegment.MINIMUM_SEGMENT_LENGTH :IBorderSegment.MINIMUM_SEGMENT_LENGTH;

        // the maximum index that can be applied allowing all remaining segments to be added
        int segsRemaining = templateProfile.getSegmentCount()-templateSegment.getPosition();
        int maxEnd = profile.size() - (segsRemaining*IBorderSegment.MINIMUM_SEGMENT_LENGTH);

        int stepSize   = 10;
        int halfStep   = stepSize / 2;
        int bestEnd = findBestScoringSegmentEndpoint(profile, id, startIndex, minEnd, maxEnd, 1);

        // Create a new segment with the endpoint applied
        IBorderSegment newSeg = new DefaultBorderSegment(startIndex, bestEnd, profile.size(), id);
        newSeg.setLocked(true);
        newSegments.add(newSeg);
        
        if(templateSegment.getPosition()==0) {
        	finer("Adding first segment "+newSeg.getDetail());
        } else {
        	finer("Adding interior segment "+newSeg.getDetail());
        }
        return newSegments;
    }
    
    /**
     * Find the best scoring position for the end index of the given segment in the template
     * profile  
     * @param testProfile the profile being matched
     * @param segId the segment id to match
     * @param negOffset the greatest negative offset to the segment end index 
     * @param posOffset the greatest positive offset to the segmnet end index
     * @param stepSize the amount to change the offset in each iteration
     * @return
     * @throws UnavailableComponentException
     * @throws ProfileException
     */
    private int findBestScoringSegmentEndpoint(@NonNull IProfile testProfile, @NonNull UUID segId, int startIndex, int minIndex, int maxIndex, int stepSize) throws UnavailableComponentException, ProfileException {

    	IProfile tempProfile = testProfile.copy();
    	IBorderSegment templateSegment = templateProfile.getSegment(segId);
    	IProfile template = templateProfile.getSubregion(templateSegment);
    	
    	double templateSegmentProportion = (double)templateSegment.length()/(double)templateProfile.size();
    	
    	finer("Template end index: "+templateProfile.get(templateSegment.getEndIndex()));
    	finer(String.format("Template segment length %s, profile length %s, from %s",templateSegment.length(), template.size(), templateSegment.toString()));
    	finer(String.format("Target profile length %s",tempProfile.size()));
    	
    	// Find indexes that are minima or maxima. 
    	// If these are clear, they should be retained
    	BooleanProfile minimaMaxima = tempProfile.getLocalMaxima(5, 180).or(tempProfile.getLocalMinima(5, 180));
    	double bestScore = Double.MAX_VALUE;
        int bestIndex = 0;
        
        finer(String.format("Testing variation of end index from %s to %s", minIndex, maxIndex));
        
        for (int endIndex=minIndex; endIndex <maxIndex; endIndex+=stepSize) {
        	
        	IProfile segmentProfile = testProfile.getSubregion(startIndex, endIndex);
        	
        	double score =  template.absoluteSquareDifference(segmentProfile, 100);
        	
        	double testSegmentProportion = (double)segmentProfile.size()/(double)testProfile.size();
        	
        	// apply a penalty as we get further from the proportional length of the template
        	double difference = Math.abs(testSegmentProportion-templateSegmentProportion);
        	// as difference increases from 0, increase the score
        	score = Math.pow(score, 1+difference);
        	
        	if(minimaMaxima.get(endIndex)) {
        		score *= 0.25; //TODO: formalise this rule and find the best value to use
//        		fine(String.format("End index %s is a local min or max! Score altered to %s", endIndex, score));
        	}
        	
        	if(startIndex==11)
        		finer(endIndex+"\t"+score+"\t"+tempProfile.get(endIndex));

            if (score < bestScore) {
            	bestIndex = endIndex;
            	bestScore = score;
            }
        }
        finest(Arrays.toString(testProfile.getSubregion(startIndex, bestIndex).toFloatArray()));
        finest(String.format("Best end index is %s with score %s", bestIndex, bestScore));
        return bestIndex;
    }
}
