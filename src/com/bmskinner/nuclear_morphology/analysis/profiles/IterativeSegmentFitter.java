package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.ArrayList;
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
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;
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
     * not be modified
     * 
     * @param profile the median profile of the collection
     * @throws ProfileException if the profile to be segmented cannot be copied
     */
    @SuppressWarnings("null")
	public IterativeSegmentFitter(@NonNull final ISegmentedProfile template) throws ProfileException {
        if (template == null)
            throw new IllegalArgumentException("Template profile is null");
        if(template.getSegmentCount()<=1)
        	throw new IllegalArgumentException("Template profile does not have segments");
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
//        if(!target.hasSegments())
//        	return target;
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
//    	IProfile tempProfile = profile.copy();
        List<IBorderSegment> newSegments = new ArrayList<>();
        // fit each segment in turn
        for(int i=0; i<templateProfile.getSegmentCount(); i++) {
        	IBorderSegment templateSegment = templateProfile.getSegments().get(i);
        	newSegments = bestFitSegment(profile, newSegments, templateSegment.getID());
        }
//        tempProfile.setSegments(newSegments);
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
    		fine("Adding final segment "+lastSegment.getDetail());
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
    	IProfile template = templateProfile.getSubregion(templateProfile.getSegment(segId));
    	
    	// Find indexes that are minima or maxima. 
    	// If these are clear, they should be retained
    	BooleanProfile minimaMaxima = tempProfile.getLocalMaxima(5, 180).or(tempProfile.getLocalMinima(5, 180));
    	double bestScore = Double.MAX_VALUE;
        int bestIndex = 0;
        
        finer(String.format("Testing variation of end index from %s to %s", minIndex, maxIndex));
        
        for (int endIndex=minIndex; endIndex <maxIndex; endIndex+=stepSize) {
        	
        	IProfile segmentProfile = testProfile.getSubregion(startIndex, endIndex);
        	double score =  template.absoluteSquareDifference(segmentProfile);
        	
        	if(minimaMaxima.get(endIndex)) {
        		score *= 0.25; //TODO: formalise this rule and find the best value to use
        		fine(String.format("End index %s is a local min or max! Score altered to %s", endIndex, score));
        	}

            if (score < bestScore) {
            	bestIndex = endIndex;
            	bestScore = score;
            }
        }
        fine(String.format("Best offset is %s with score %s", bestIndex, bestScore));
        return bestIndex;
    }
}
