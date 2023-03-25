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
package com.bmskinner.nma.analysis.profiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.profiles.BooleanProfile;
import com.bmskinner.nma.components.profiles.DefaultProfileSegment;
import com.bmskinner.nma.components.profiles.DefaultSegmentedProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.logging.Loggable;

/**
 * Carry out iterative fitting of segments from a target profile to match
 * a template profile
 * @author bms41
 * @since 1.14.0
 *
 */
public class IterativeSegmentFitter {
	
	private static final Logger LOGGER = Logger.getLogger(IterativeSegmentFitter.class.getName());

    @NonNull private final ISegmentedProfile templateProfile;

    /**
     * Construct with a profile containing segments. The originals will
     * not be modified.
     * 
     * @param profile the template profile with segments to be fitted
     * @throws ProfileException if the template profile cannot be copied
     */
	public IterativeSegmentFitter(@NonNull final ISegmentedProfile template) throws ProfileException {
        templateProfile = template.duplicate();
    }

    /**
     * Find the best fit positions of the segment endpoints in the 
     * template to the target profile. New segments are created in the target
     * profile.
     * 
     * @param target the profile to fit to the current template profile 
     * @return the profile with fitted segments, or on error, the original profile
     */
    public ISegmentedProfile fit(@NonNull final IProfile target) throws ProfileException {
    	LOGGER.finer( "Beginning segment fitting");
        
        if(templateProfile.getSegmentCount()==1)
        	return new DefaultSegmentedProfile(target);

		try {
			return remapSegmentEndpoints(target);
		} catch (MissingComponentException e) {
			LOGGER.log(Loggable.STACK, "Could not segment target profile: unable to remap segments in profile: "+e.getMessage(), e);
			throw new ProfileException("Could not segment target profile", e);
		}
    }

    /**
     * 
     * @param profile the profile to fit against the template profile
     * @return a profile with best-fit segmentation to the median
     * @throws ProfileException 
     * @throws MissingComponentException 
     */
    private ISegmentedProfile remapSegmentEndpoints(@NonNull IProfile profile) throws ProfileException, MissingComponentException {

        List<IProfileSegment> newSegments = new ArrayList<>();
        
        // fit each segment in turn
        for(IProfileSegment templateSegment : templateProfile.getOrderedSegments())
        	newSegments = bestFitSegment(profile, newSegments, templateSegment.getID());
        LOGGER.finer(String.format("Created %s segments in target profile", newSegments.size()));
        for(IProfileSegment s : newSegments) // unlock after fitting
        	s.setLocked(false);
        return new DefaultSegmentedProfile(profile, newSegments);
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
     * @throws MissingComponentException 
     */
    private List<IProfileSegment> bestFitSegment(@NonNull IProfile profile, List<IProfileSegment> segmentsSoFar, @NonNull UUID id) throws ProfileException, MissingComponentException {
    	
    	// Start by adding locked segments back to the profile
    	List<IProfileSegment> newSegments = new ArrayList<>();
    	for(IProfileSegment s : segmentsSoFar) {
    		if(s.isLocked())
    			newSegments.add(s);
    	}
    	
    	IProfileSegment templateSegment = templateProfile.getSegment(id);
    	// If it is the last segment, just link to the first and return
    	if(templateSegment.getPosition()==templateProfile.getSegmentCount()-1) {
    		IProfileSegment prevSegment = newSegments.get(newSegments.size()-1);
    		IProfileSegment lastSegment = new DefaultProfileSegment(prevSegment.getEndIndex(), 0, profile.size(), id);
    		lastSegment.setLocked(true);
    		LOGGER.finer( "Adding final segment "+lastSegment.getDetail());
    		newSegments.add(lastSegment);
    		IProfileSegment.linkSegments(newSegments);
    		return newSegments;
    	}
    	
    	// Otherwise, find the best matching position for the given segment
    	// Add the segment to the result profile
    	// Lock the segment

        // The start index for the segment is fixed
        int startIndex = !segmentsSoFar.isEmpty() ? segmentsSoFar.get(segmentsSoFar.size()-1).getEndIndex():0;
        
        // the lowest index that can be applied to the end of this segment
        int minEnd = !segmentsSoFar.isEmpty() ? segmentsSoFar.get(segmentsSoFar.size()-1).getEndIndex() + IProfileSegment.MINIMUM_SEGMENT_LENGTH :IProfileSegment.MINIMUM_SEGMENT_LENGTH;

        // the maximum index that can be applied allowing all remaining segments to be added
        int segsRemaining = templateProfile.getSegmentCount()-templateSegment.getPosition();
        int maxEnd = profile.size() - (segsRemaining*IProfileSegment.MINIMUM_SEGMENT_LENGTH);

        int bestEnd = findBestScoringSegmentEndpoint(profile, id, startIndex, minEnd, maxEnd, 1);

        // Create a new segment with the endpoint applied
        IProfileSegment newSeg = new DefaultProfileSegment(startIndex, bestEnd, profile.size(), id);
        newSeg.setLocked(true);
        newSegments.add(newSeg);
        
        if(templateSegment.getPosition()==0) {
        	LOGGER.finer( "Adding first segment "+newSeg.getDetail());
        } else {
        	LOGGER.finer( "Adding interior segment "+newSeg.getDetail());
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
     * @throws MissingComponentException
     * @throws ProfileException
     */
    private int findBestScoringSegmentEndpoint(@NonNull IProfile testProfile, @NonNull UUID segId, int startIndex, int minIndex, int maxIndex, int stepSize) throws MissingComponentException, ProfileException {

    	IProfile tempProfile = testProfile.duplicate();
    	IProfileSegment templateSegment = templateProfile.getSegment(segId);
    	IProfile template = templateProfile.getSubregion(templateSegment);
    	
    	double templateSegmentProportion = (double)templateSegment.length()/(double)templateProfile.size();
    	
    	LOGGER.finer( "Template end index: "+templateProfile.get(templateSegment.getEndIndex()));
    	LOGGER.finer( String.format("Template segment length %s, profile length %s, from %s",templateSegment.length(), template.size(), templateSegment.toString()));
    	LOGGER.finest( String.format("Target profile length %s",tempProfile.size()));
    	
    	// Find indexes that are minima or maxima. 
    	// If these are clear, they should be retained
    	BooleanProfile minimaMaxima = tempProfile.getLocalMaxima(5, 180).or(tempProfile.getLocalMinima(5, 180));
    	double bestScore = Double.MAX_VALUE;
        int bestIndex = 0;
        
        LOGGER.finest( String.format("Testing variation of end index from %s to %s", minIndex, maxIndex));
        
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
        	}
        	

            if (score < bestScore) {
            	bestIndex = endIndex;
            	bestScore = score;
            }
        }
        LOGGER.finest( Arrays.toString(testProfile.getSubregion(startIndex, bestIndex).toFloatArray()));
        LOGGER.finest( String.format("Best end index is %s with score %s", bestIndex, bestScore));
        return bestIndex;
    }
}
