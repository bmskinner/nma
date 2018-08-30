package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
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
            throw new IllegalArgumentException("Median profile is null");
        templateProfile = template.copy();
    }

    /**
     * Find the best fit positions of the segment endpoints to the target profile
     * 
     * @param target the profile to fit to the current template profile 
     * @return the profile with fitted segments, or on error, the original profile
     */
    public ISegmentedProfile fit(@NonNull final ISegmentedProfile target) {
    	fine("-------------------------");
    	fine("Beginning segment fitting");
    	fine("-------------------------");
    	
        if (target==null)
            throw new IllegalArgumentException("Target profile is null");
        if(!target.hasSegments())
        	return target;
		try {
			return remapSegmentEndpoints(target);
		} catch (UnavailableComponentException | ProfileException e) {
			fine("Unable to remap segments in profile: "+e.getMessage(), e);
			return target;
		}
    }

    /**
     * 
     * @param profile the profile to fit against the template profile
     * @return a profile with best-fit segmentation to the median
     * @throws ProfileException 
     * @throws UnavailableComponentException 
     */
    private ISegmentedProfile remapSegmentEndpoints(@NonNull ISegmentedProfile profile) throws ProfileException, UnavailableComponentException {
        ISegmentedProfile tempProfile = profile.copy();
        List<IBorderSegment> newSegments = new ArrayList<>();
        // fit each segment in turn
        for(int i=0; i<templateProfile.getSegmentCount(); i++) {
        	IBorderSegment templateSegment = templateProfile.getSegments().get(i);
        	newSegments = bestFitSegment(tempProfile, newSegments, templateSegment.getID());
        }
        tempProfile.setSegments(newSegments);
        return tempProfile;
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
    private List<IBorderSegment> bestFitSegment(@NonNull ISegmentedProfile profile, List<IBorderSegment> segmentsSoFar, @NonNull UUID id) throws ProfileException, UnavailableComponentException {
    	
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

        IBorderSegment segment = profile.getSegment(id); // the segment in the input profile to work on

        // the most extreme negative offset that can be applied to the end of this segment
        // find the end point of the previously locked segment

        int minStartIndex = segmentsSoFar.size()>0 ? segmentsSoFar.get(segmentsSoFar.size()-1).getEndIndex() + IBorderSegment.MINIMUM_SEGMENT_LENGTH :IBorderSegment.MINIMUM_SEGMENT_LENGTH;
        int minimumOffset = 0-(segment.getEndIndex()-minStartIndex);//0 - (prevSegment.getEndIndex() + IBorderSegment.MINIMUM_SEGMENT_LENGTH);

        // the maximum offset that can be applied allowing all remaining segments to be added
        int segsRemaining = profile.getSegmentCount()-segment.getPosition();
        int distanceToEnd = profile.size()-segment.getEndIndex()-1;
        int maximumChange = distanceToEnd - (segsRemaining*IBorderSegment.MINIMUM_SEGMENT_LENGTH);

        int stepSize   = 10;
        int halfStep   = stepSize / 2;
        int bestChange = findBestScoringSegmentEndpoint(profile, id, minimumOffset, maximumChange, 1);
        // now we have the right general area, drop down to single values
//        bestChange = findBestScoringSegmentEndpoint(profile, id, bestChange-halfStep, bestChange+halfStep, 1);

        // Create a new segment with the change applied
        if(segment.getPosition()==0) {
        	IBorderSegment newSeg = new DefaultBorderSegment(0, segment.getEndIndex()+bestChange, profile.size(), id);
        	newSeg.setLocked(true);
        	fine("Adding first segment "+newSeg.getDetail());
        	newSegments.add(newSeg);
        } else {
        	IBorderSegment prevSegment = newSegments.get(newSegments.size()-1);
        	IBorderSegment newSeg = new DefaultBorderSegment(prevSegment.getEndIndex(), segment.getEndIndex()+bestChange, profile.size(), id);
        	newSeg.setLocked(true);
        	fine("Adding segment "+newSeg.getDetail());
        	newSegments.add(newSeg);
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
    private int findBestScoringSegmentEndpoint(@NonNull ISegmentedProfile testProfile, @NonNull UUID segId, int negOffset, int posOffset, int stepSize) throws UnavailableComponentException, ProfileException {

    	ISegmentedProfile tempProfile = testProfile.copy();
    	double bestScore = Double.MAX_VALUE;
        int bestChange = 0;
        
        int start = testProfile.getSegment(segId).getStartIndex();
        int oldEnd =  testProfile.getSegment(segId).getEndIndex();
        int minEnd = oldEnd+negOffset;
        int maxEnd = oldEnd+posOffset;
        
        fine(String.format("Testing variation of end index from %s to %s", minEnd, maxEnd));
        
        for (int offset = negOffset; offset < posOffset; offset += stepSize) {
        	
        	IProfile segmentProfile = testProfile.getSubregion(start, oldEnd+offset);
        	IProfile template = templateProfile.getSubregion(templateProfile.getSegment(segId));
        	
        	double score =  template.absoluteSquareDifference(segmentProfile);
//        	fine(String.format("Score with offset %s and end at %s: %s", offset, oldEnd+offset, score));
//        	score *= 1+((double)segmentProfile.size()/(double)template.size());

            if (score < bestScore) {
            	bestChange = offset;
            	bestScore = score;
            }
        }
        fine(String.format("Best offset is %s with score %s", bestChange, bestScore));
        return bestChange;
    }
}
