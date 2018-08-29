/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This class handles fitting of segments between profiles
 * @author bms41
 *
 */
public class SegmentFitter implements Loggable {

    /**
     * The multiplier to add to best-fit scores when shrinking a segment below
     * the minimum segment size specified in ProfileSegmenter
     */
    static final double PENALTY_SHRINK = 1.5;

    /**
     * The multiplier to add to best-fit scores when shrinking a segment above
     * the median segment size
     */
    static final double PENALTY_GROW = 20;

    @NonNull private final ISegmentedProfile templateProfile;

    /**
     * Construct with a median profile containing segments. The originals will
     * not be modified
     * 
     * @param profile the median profile of the collection
     * @throws ProfileException if the profile to be segmented cannot be copied
     */
    @SuppressWarnings("null")
	public SegmentFitter(@NonNull final ISegmentedProfile template) throws ProfileException {
        if (template == null)
            throw new IllegalArgumentException("Median profile is null");
        templateProfile = template.copy();
    }

    /**
     * Run the segment fitter on the given profile. It will take the segments from
     * the template profile loaded into the fitter upon construction, and apply them 
     * to the  profile.
     * 
     * @param target the profile to fit to the current template profile 
     * @return the profile with fitted segments, or on error, the original profile
     */
    public ISegmentedProfile fit(@NonNull final ISegmentedProfile target) {
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

        // By default, return the input profile
        ISegmentedProfile result = profile.copy();

        ISegmentedProfile tempProfile = profile.copy();

        // fit each segment in turn
        for(IBorderSegment templateSegment : templateProfile.getSegments()) {

            IBorderSegment segment = tempProfile.getSegment(templateSegment.getID());

            if (!segment.isLocked()) { 
                tempProfile = bestFitSegment(tempProfile, templateSegment.getID()).copy();
                result = tempProfile.copy();
            }
        }

        return result;
    }

    /**
     * Find the best fit offset for the given segment id
     * 
     * @param profile
     * @param id the segment to test
     * @return
     * @throws ProfileException 
     * @throws UnavailableComponentException 
     */
    private ISegmentedProfile bestFitSegment(@NonNull ISegmentedProfile profile, @NonNull UUID id) throws ProfileException, UnavailableComponentException {

        // by default, return the same profile that came in
    	ISegmentedProfile result = profile.copy();
    	
        // the segment in the input profile to work on
        IBorderSegment segment = profile.getSegment(id);

        // Get the initial score to beat
        double bestScore = compareSegmentationPatterns(templateProfile, profile);

        // the most extreme negative offset to apply to the end of this segment
        // without making the length invalid
        int minimumChange = 0 - (segment.length() - IBorderSegment.MINIMUM_SEGMENT_LENGTH);

        // the maximum length offset to apply
        // we can't go beyond the end of the next segment anyway, so use that as
        // the cutoff
        // how far from current end to next segment end?
        int maximumChange = segment.testLength(segment.getEndIndex(), segment.nextSegment().getEndIndex());

        /*
         * Trying all possible lengths takes a long time. Try adjusting lengths
         * in a window of <changeWindowSize>, and finding the window with the
         * best fit. Then drop down to individual index changes to refine the
         * match
         */
        int bestChangeWindow = 0;
        int changeWindowSize = 10;
        for (int changeWindow = minimumChange; changeWindow < maximumChange; changeWindow += changeWindowSize) {

            // find the changeWindow with the best fit,
            // apply all changes to a fresh copy of the profile
        	ISegmentedProfile testProfile = profile.copy();
            testProfile = testChange(profile, id, changeWindow);
            double score = compareSegmentationPatterns(templateProfile, testProfile);
            if (score < bestScore) {
                bestChangeWindow = changeWindow;
            }
        }

        int halfWindow = changeWindowSize / 2;
        // now we have the best window, drop down to a changeValue
        for (int changeValue = bestChangeWindow - halfWindow; changeValue < bestChangeWindow
                + halfWindow; changeValue++) {
        	ISegmentedProfile testProfile = profile.copy();

            testProfile = testChange(profile, id, changeValue);
            double score = compareSegmentationPatterns(templateProfile, testProfile);
            if (score < bestScore) {
                result = testProfile;
            }
        }

        return result;
    }

    /**
     * Test the effect of moving the segment start boundary of the profile by a
     * certain amount. If the change is a better fit to the median profile than
     * before, it is kept.
     * 
     * @param profile the profile to test
     * @param id the segment to alter
     * @param changeValue the amount to alter the segment by
     * @return the original profile, or a better fit to the median
     * @throws UnavailableComponentException 
     * @throws ProfileException 
     * @throws Exception
     */
    private ISegmentedProfile testChange(@NonNull ISegmentedProfile profile, @NonNull UUID id, int changeValue) throws UnavailableComponentException, ProfileException {
        double bestScore = compareSegmentationPatterns(templateProfile, profile);

        // apply all changes to a fresh copy of the profile
        ISegmentedProfile testProfile = profile.copy();

        // not permitted if it violates length constraints
        IBorderSegment seg = testProfile.getSegment(id);
        int newStart = testProfile.wrap(seg.getStartIndex()+changeValue);
        
        try {
			testProfile.getSegment(id).update(newStart, seg.getEndIndex());
		} catch (SegmentUpdateException e) {
			return profile;
		}
        
        double score = compareSegmentationPatterns(templateProfile, testProfile);

        if (score < bestScore)
        	return testProfile.copy();
        return profile;
    }
    
    /**
     * Get the score for an entire segment list of a profile. Tests the effect
     * of changing one segment on the entire set
     * 
     * @param referenceProfile
     * @param testProfile
     * @return the score
     * @throws UnavailableComponentException 
     * @throws Exception
     */
    private double compareSegmentationPatterns(@NonNull ISegmentedProfile referenceProfile, @NonNull ISegmentedProfile testProfile) throws UnavailableComponentException {
        if (referenceProfile.getSegmentCount() != testProfile.getSegmentCount())
            throw new IllegalArgumentException("Segment counts are different for profiles");

        double result = 0;
        for (UUID id : referenceProfile.getSegmentIDs()) {
            result += compareSegments(id, referenceProfile, testProfile);
        }
        return result;
    }

    /**
     * Get the sum-of-squares difference between two segments in the given
     * profile
     * 
     * @param name the name of the segment
     * @param referenceProfile the profile to measure against
     * @param testProfile the profile to measure
     * @return the sum of square differences between the segments
     * @throws UnavailableComponentException 
     * @throws Exception
     */
    private double compareSegments(@NonNull UUID id, @NonNull ISegmentedProfile referenceProfile, @NonNull ISegmentedProfile testProfile) throws UnavailableComponentException {
        if (id == null)
            throw new IllegalArgumentException("Segment id is null");

        if (referenceProfile == null || testProfile == null)
            throw new IllegalArgumentException("Test or reference profile is null");

        IBorderSegment reference = referenceProfile.getSegment(id);
        IBorderSegment test = testProfile.getSegment(id);

        double result = 0;

        try {

            IProfile refProfile = referenceProfile.getSubregion(reference);
            IProfile subjProfile = testProfile.getSubregion(test);

            result = refProfile.absoluteSquareDifference(subjProfile);

        } catch (ProfileException e) {
        	warn("Error calculating absolute square difference between segments");
            stack("Error calculating absolute square difference between segments", e);
        }
        return result;
    }
}
