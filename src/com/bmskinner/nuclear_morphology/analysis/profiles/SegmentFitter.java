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
import java.util.logging.Level;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.BorderTag.BorderTagType;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
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

    private final ISegmentedProfile medianProfile; // the profile to align
                                                   // against

    /**
     * Construct with a median profile containing segments. The originals will
     * not be modified
     * 
     * @param profile the median profile of the collection
     * @throws ProfileException if the profile to be segmented cannot be copied
     * @throws Exception
     */
    public SegmentFitter(@NonNull final ISegmentedProfile profile) throws ProfileException {
        if (profile == null)
            throw new IllegalArgumentException("Median profile is null");

        medianProfile = profile.copy();
    }

    /**
     * Run the segment fitter on the given nucleus. It will take the segments from
     * the template profile loaded into the fitter upon construction, and apply them 
     * to the nucleus angle profile. Border tags are updated to the best matching location
     * in the template profile.
     * 
     * @param n the nucleus to fit to the current median profile
     * @param pc the IProfileCollection the nucleus is to be matched with
     * @throws UnavailableComponentException 
     * @throws ProfileException 
     * @throws IndexOutOfBoundsException 
     * @throws UnsegmentedProfileException 
     */
    public void fit(@NonNull final Nucleus n, @NonNull final IProfileCollection pc) throws IndexOutOfBoundsException, ProfileException, UnavailableComponentException, UnsegmentedProfileException {
        // Input checks
        if (n == null)
            throw new IllegalArgumentException("Test nucleus is null");
        
        if(!n.hasProfile(ProfileType.ANGLE))
        	throw new IllegalArgumentException("Nucleus has no angle profile");
        
        ISegmentedProfile angleProfile = n.getProfile(ProfileType.ANGLE);
        
        if(!angleProfile.hasSegments())
        	throw new UnsegmentedProfileException(String.format("Nucleus %s has no segments", n.getNameAndNumber()));

        ISegmentedProfile newProfile = remapSegmentEndpoints(angleProfile);

        n.setProfile(ProfileType.ANGLE, newProfile);

        remapBorderPoints(n, pc);
    }

    /**
     * Join the segments within the given nucleus into Frankenstein's Profile.
     * 
     * @param n the nucleus to recombine
     * @param tag the BorderTagObject to start from
     * @return a profile
     * @throws ProfileException 
     * @throws UnavailableProfileTypeException 
     * @throws UnavailableBorderTagException 
     * @throws Exception
     */
//    public ISegmentedProfile recombine(@NonNull final Nucleus n, Tag tag) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {
//        if (n == null)
//            throw new IllegalArgumentException("Test nucleus is null");
//
//        if (!n.getProfile(ProfileType.ANGLE).hasSegments())
//            throw new IllegalArgumentException("Test nucleus has no segments");
//
//        /*
//         * Generate a segmented profile from the angle profile of the point
//         * type. The zero index of the profile is the border tag. The segment
//         * list for the profile begins with seg 0 at the border tag.
//         */
//        ISegmentedProfile nucleusProfile = n.getProfile(ProfileType.ANGLE, tag).copy();
//
//
//        // stretch or squeeze the segments to match the length of the median
//        // profile of the collection
//        try {
//            return nucleusProfile.frankenNormaliseToProfile(medianProfile);
//        } catch (ProfileException e) {
//            error("Malformed profile in frankenprofiling", e);
//            finest("Median profile:");
//            finest(medianProfile.toString());
//            finest("Nucleus profile:");
//            finest(nucleusProfile.toString());
//
//        }
//
//        return null;
//    }

    /**
     * Move core border points within a nucleus to the end of their appropriate
     * segment based on the median profile segmentation pattern
     * 
     * @param n the nucleus to fit
     * @param pc the profile collection from the CellCollection
     * @throws ProfileException 
     * @throws UnsegmentedProfileException 
     * @throws UnavailableComponentException 
     * @throws UnavailableProfileTypeException 
     */
    private void remapBorderPoints(@NonNull final Nucleus n, IProfileCollection pc) throws ProfileException, UnsegmentedProfileException, UnavailableProfileTypeException, UnavailableComponentException {

        if (pc == null) {
            warn("No profile collection found, skipping remapping");
            return;
        }

        /*
         * RP not at segment start
         * Not all the tags will be associated with endpoints; e.g. the
         * intersection point. The orientation and reference points should be
         * updated though - members of the core border tag population
         */

        for (Tag tag : BorderTagObject.values(BorderTagType.CORE)) {

            // get the segments the point should lie between
            // from the median profile

            /*
             * The goal is to move the index of the border tag to the start
             * index of the relevant segment.
             * 
             * Select the segments from the median profile, offset to begin from
             * the tag. The relevant segment has a start index of 0 Find the
             * name of this segment, and adjust it's start position in the
             * individual nucleus profile.
             */
            IBorderSegment seg = pc.getSegmentStartingWith(tag);
            List<IBorderSegment> segments = pc.getSegments(tag);

            if (seg != null) {
                // Get the same segment in the nucleus, and move the tag to the
                // segment start point
                IBorderSegment nSeg = n.getProfile(ProfileType.ANGLE).getSegment(seg.getName());
                n.setBorderTag(tag, nSeg.getStartIndex());
            } else {

                // A segment was not found with a start index at zero; segName
                // is null
                fine("Border tag '" + tag + "' not found in median profile");
                fine("No segment with start index zero in median profile");
                fine("Median profile:");
                fine(pc.toString());
                fine("Median segment list:");
                fine(IBorderSegment.toString(segments));
                // n.log("Could not remapped border point '"+tag+"'");
                // Check to see if the segments are reversed
                seg = pc.getSegmentEndingWith(tag);
                if (seg != null) {
                    fine("Found segment " + seg.getName() + " ending with tag " + tag);

                } else {
                    fine("No segments end with tag " + tag);
                }

            }
        }
    }

    /**
     * 
     * @param profile
     *            the profile to fit against the median profile
     * @return a profile with best-fit segmentation to the median
     * @throws ProfileException 
     * @throws IndexOutOfBoundsException 
     * @throws UnavailableComponentException 
     * @throws Exception
     */
    private ISegmentedProfile remapSegmentEndpoints(@NonNull ISegmentedProfile profile) throws IndexOutOfBoundsException, ProfileException, UnavailableComponentException {
        // Input check
        if (profile == null)
            throw new IllegalArgumentException("Input profile is null");

        // By default, return the input profile
        ISegmentedProfile result = profile.copy();

        ISegmentedProfile tempProfile = profile.copy();

        // fit each segment independently
        List<UUID> idList = medianProfile.getSegmentIDs();

        for (UUID id : idList) {

            // get the current segment
            IBorderSegment segment = tempProfile.getSegment(id);

            if (!segment.isLocked()) { // only run the test if this segment is
                                       // unlocked

                // get the initial score for the segment and log it
                // double score = compareSegmentationPatterns(medianProfile,
                // tempProfile);
                //
                // find the best length and offset change
                // apply them to the profile
                tempProfile = testLength(tempProfile, id);

                // copy the best fit profile to the result
                result = tempProfile.copy();
            }
        }

        return result;
    }

    /**
     * Test the effect of changing length on the given segment
     * 
     * @param profile
     * @param id the segment to test
     * @return
     * @throws ProfileException 
     * @throws UnavailableComponentException 
     */
    private ISegmentedProfile testLength(ISegmentedProfile profile, UUID id) throws ProfileException, UnavailableComponentException {

        // by default, return the same profile that came in
    	ISegmentedProfile result = profile.copy();
//        ISegmentedProfile result = new SegmentedFloatProfile(profile);

        // the segment in the input profile to work on
        IBorderSegment segment = profile.getSegment(id);

        // Get the initial score to beat
        double bestScore = compareSegmentationPatterns(medianProfile, profile);

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
//            ISegmentedProfile testProfile = new SegmentedFloatProfile(profile);

            testProfile = testChange(profile, id, changeWindow);
            double score = compareSegmentationPatterns(medianProfile, testProfile);
            if (score < bestScore) {
                bestChangeWindow = changeWindow;
            }
        }

        int halfWindow = changeWindowSize / 2;
        // now we have the best window, drop down to a changeValue
        for (int changeValue = bestChangeWindow - halfWindow; changeValue < bestChangeWindow
                + halfWindow; changeValue++) {
        	ISegmentedProfile testProfile = profile.copy();
//            ISegmentedProfile testProfile = new SegmentedFloatProfile(profile);

            testProfile = testChange(profile, id, changeValue);
            double score = compareSegmentationPatterns(medianProfile, testProfile);
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
    private ISegmentedProfile testChange(ISegmentedProfile profile, UUID id, int changeValue) throws UnavailableComponentException, ProfileException {

        if (profile == null)
            throw new IllegalArgumentException(String.format("Input profile is null for segment %s change %d", id.toString(), changeValue));

        double bestScore = compareSegmentationPatterns(medianProfile, profile);

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
        
        double score = compareSegmentationPatterns(medianProfile, testProfile);

        if (score < bestScore)
        	return testProfile.copy();
//            return new SegmentedFloatProfile(testProfile);
        return profile;
    }
    
    /**
     * Get the score for an entire segment list of a profile. Tests the effect
     * of changing one segment on the entire set
     * 
     * @param reference
     * @param finder
     * @return the score
     * @throws UnavailableComponentException 
     * @throws Exception
     */
    private double compareSegmentationPatterns(@NonNull ISegmentedProfile referenceProfile, @NonNull ISegmentedProfile testProfile) throws UnavailableComponentException {

        if (referenceProfile == null || testProfile == null)
            throw new IllegalArgumentException("An input profile is null");

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
