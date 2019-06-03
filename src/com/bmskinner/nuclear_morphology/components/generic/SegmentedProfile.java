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
package com.bmskinner.nuclear_morphology.components.generic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.AbstractCellularComponent;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusBorderSegment;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.IJ;

/**
 * This class provides consistency and error checking for segmnentation applied
 * to profiles.
 *
 */
@Deprecated
public class SegmentedProfile extends Profile implements ISegmentedProfile {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final long serialVersionUID = 1L;

    // the segments
    protected List<IBorderSegment> segments = new ArrayList<IBorderSegment>(5);

    /**
     * Construct using a regular profile and a list of border segments
     * 
     * @param p
     *            the profile
     * @param segments
     *            the list of segments to use
     */
    private SegmentedProfile(IProfile p, List<IBorderSegment> segments) {
        super(p);

        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("Segment list is null or empty in segmented profile contructor");
        }

        if (p.size() != segments.get(0).getProfileLength()) {
            throw new IllegalArgumentException("Segments total length (" + segments.get(0).getProfileLength()
                    + ") does not fit profile (" + +p.size() + ")");
        }

        try {
            IBorderSegment.linkSegments(segments);
        } catch (ProfileException e) {
            LOGGER.log(Loggable.STACK, "Profile error linking segments", e);
        }

        this.segments = segments;
    }

    /**
     * Construct using an existing profile. Copies the data and segments
     * 
     * @param profile
     *            the segmented profile to copy
     */
    private SegmentedProfile(final SegmentedProfile profile) {
        this(profile, profile.getSegments());
    }

    /**
     * Construct using a basic profile. Two segments are created that span the
     * entire profile, half each
     * 
     * @param profile
     */
    private SegmentedProfile(Profile profile) {
        super(profile);
        int midpoint = profile.size() / 2;
        IBorderSegment segment1 = new NucleusBorderSegment(0, midpoint, profile.size());

        segment1.setPosition(0);
        IBorderSegment segment2 = new NucleusBorderSegment(midpoint, 0, profile.size());

        segment2.setPosition(1);
        List<IBorderSegment> segments = new ArrayList<IBorderSegment>();
        segments.add(segment1);
        segments.add(segment2);

        try {
            IBorderSegment.linkSegments(segments);
        } catch (ProfileException e) {
            LOGGER.warning("Error linking segments");
        }

        this.segments = segments;
    }

    /**
     * Construct from an array of values
     * 
     * @param values
     * @throws Exception
     */
    private SegmentedProfile(double[] values) {
        this(new Profile(values));
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#hasSegments()
     */
    @Override
    public boolean hasSegments() {
        if (this.segments == null || this.segments.isEmpty())
            return false;
		return true;
    }


    @Override
    public @NonNull List<IBorderSegment> getSegments() {
        try {
            return IBorderSegment.copy(this.segments);
        } catch (ProfileException e) {
            LOGGER.log(Loggable.STACK, "Error copying segments", e);
        }
        return new ArrayList<>();
    }

    @Override
    public @NonNull IBorderSegment getSegment(@NonNull UUID id) throws UnavailableComponentException {
        for (IBorderSegment seg : this.segments) {
            if (seg.getID().equals(id)) {
                return seg;
            }
        }
        throw new UnavailableComponentException("Segment with id "+id.toString()+" is not present");
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#hasSegment(java.util.UUID)
     */
    @Override
    public boolean hasSegment(@NonNull UUID id) {
        for (IBorderSegment seg : this.segments) {
            if (seg.getID().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#getSegmentsFrom(java.util.UUID)
     */
    @Override
    public List<IBorderSegment> getSegmentsFrom(@NonNull UUID id) throws ProfileException, UnavailableComponentException {
        return getSegmentsFrom(getSegment(id));
    }

    /**
     * Get the segments in order from the given segment
     * 
     * @param firstSeg
     * @return
     * @throws Exception
     */
    private List<IBorderSegment> getSegmentsFrom(IBorderSegment firstSeg) throws ProfileException {

        if (firstSeg == null) {
            throw new IllegalArgumentException("Requested first segment is null");
        }

        List<IBorderSegment> result = new ArrayList<IBorderSegment>();
        int i = segments.size() - 1; // the number of segments
        result.add(firstSeg);
        while (i > 0) {

            if (firstSeg.hasNextSegment()) {
                firstSeg = firstSeg.nextSegment();
                result.add(firstSeg);
                i--;
            } else {
                throw new ProfileException(i + ": No next segment in " + firstSeg.toString());
            }
        }
        return IBorderSegment.copy(result);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#getOrderedSegments()
     */
    @Override
    public List<IBorderSegment> getOrderedSegments() {

        IBorderSegment firstSeg = null; // default to the first segment in the
                                        // profile

        /*
         * Choose the first segment of the profile to be the segment starting at
         * the zero index
         */
        for (IBorderSegment seg : segments) {

            if (seg.getStartIndex() == ZERO_INDEX) {
                firstSeg = seg;
            }
        }

        if (firstSeg == null) {

            /*
             * A subset of nuclei do not produce segment boundaries
             */
//            fine("Cannot get ordered segments");
//            fine("Profile is " + this.toString());
//            // log(Level.WARNING, this.toString());
//            fine("Using the first segment in the profile");
            firstSeg = this.getSegments().get(0); // default to the first
                                                  // segment in the profile
        }

        List<IBorderSegment> result;
        try {
            result = getSegmentsFrom(firstSeg);
        } catch (ProfileException e) {
            LOGGER.warning("Profile error getting segments");
            LOGGER.log(Loggable.STACK, "Profile error getting segments", e);
            result = new ArrayList<IBorderSegment>();
        }

        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#getSegment(java.lang.String)
     */
    @Override
    public IBorderSegment getSegment(@NonNull String name) {
        if (name == null) {
            throw new IllegalArgumentException("Requested segment name is null");
        }

        for (IBorderSegment seg : this.segments) {
            if (seg.getName().equals(name)) {
                return seg;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#getSegment(components.nuclear.
     * NucleusBorderSegment)
     */
    @Override
    public IBorderSegment getSegment(@NonNull IBorderSegment segment) {
        if (!this.contains(segment)) {
            throw new IllegalArgumentException("Requested segment name is not present");
        }

        IBorderSegment result = null;
        for (IBorderSegment seg : this.segments) {
            if (seg.equals(segment)) {
                result = seg;
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#getSegmentAt(int)
     */
    @Override
    public IBorderSegment getSegmentAt(int position) {

        IBorderSegment result = null;
        for (IBorderSegment seg : this.segments) {
            if (seg.getPosition() == position) {
                result = seg;
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#getSegmentContaining(int)
     */
    @Override
    public IBorderSegment getSegmentContaining(int index) {

        IBorderSegment result = null;
        for (IBorderSegment seg : this.segments) {
            if (seg.contains(index)) {
                result = seg;
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#setSegments(java.util.List)
     */
    @Override
    public void setSegments(@NonNull List<IBorderSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("Segment list is null or empty");
        }

        if (segments.get(0).getProfileLength() != this.size()) {
            throw new IllegalArgumentException("Segment list is from a different total length");
        }

        try {
            this.segments = IBorderSegment.copy(segments);
        } catch (ProfileException e) {
            LOGGER.warning("Cannot copy segments");
        }
    }

    // /**
    // * Replace the segments in the profile with the given list. If the
    // * segments come from a different length of profile to the current
    // * profile, each segment is adjusted to an equivalent proportion
    // * of the profile.
    // * @param segments
    // */
    // public void setNormalisedSegments(List<NucleusBorderSegment> segments){
    // if(segments.get(0).getTotalLength()!=this.size()){
    // finer("Profile lengths are the same, falling back to default method");
    // setSegments(segments);
    // }
    //
    // ..
    //
    // }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#clearSegments()
     */
    @Override
    public void clearSegments() {
        this.segments = new ArrayList<IBorderSegment>(0);
        // this.firstSegment = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#getSegmentNames()
     */
    @Override
    public List<String> getSegmentNames() {
        List<String> result = new ArrayList<String>();
        for (IBorderSegment seg : this.segments) {
            result.add(seg.getName());
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#getSegmentIDs()
     */
    @Override
    public List<UUID> getSegmentIDs() {
        List<UUID> result = new ArrayList<UUID>();
        for (IBorderSegment seg : this.segments) {
            result.add(seg.getID());
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#getSegmentCount()
     */
    @Override
    public int getSegmentCount() {
        return this.segments.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.ISegmentedProfile#getDisplacement(components.nuclear.
     * NucleusBorderSegment)
     */
    @Override
    public double getDisplacement(@NonNull IBorderSegment segment) {
        if (this.contains(segment)) {

            double start = this.get(segment.getStartIndex());
            double end = this.get(segment.getEndIndex());

            double min = Math.min(start, end);
            double max = Math.max(start, end);

            return max - min;

        }
		return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#contains(components.nuclear.
     * NucleusBorderSegment)
     */
    @Override
    public boolean contains(@NonNull IBorderSegment segment) {
        if (segment == null) {
            return false;
        }
        boolean result = false;
        for (IBorderSegment seg : this.segments) {
            if (seg.getStartIndex() == segment.getStartIndex() && seg.getEndIndex() == segment.getEndIndex()
                    && seg.getProfileLength() == this.size()

            ) {
                return true;
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#update(components.nuclear.
     * NucleusBorderSegment, int, int)
     */
    @Override
    public boolean update(@NonNull IBorderSegment segment, int startIndex, int endIndex) throws SegmentUpdateException {

        if (!this.contains(segment)) {
            throw new IllegalArgumentException("Segment is not part of this profile");
        }

        // test effect on all segments in list: the update should
        // not allow the endpoints to move within a segment other than
        // next or prev
        IBorderSegment nextSegment = segment.nextSegment();
        IBorderSegment prevSegment = segment.prevSegment();

        for (IBorderSegment testSeg : this.segments) {

            // if the proposed start or end index is found in another segment
            // that is not next or prev, do not proceed
            if (testSeg.contains(startIndex) || testSeg.contains(endIndex)) {

                if (!testSeg.getName().equals(segment.getName()) && !testSeg.getName().equals(nextSegment.getName())
                        && !testSeg.getName().equals(prevSegment.getName())) {
                    // segment.setLastFailReason("Index out of bounds of next
                    // and prev");
                    return false;
                }
            }
        }

        // the basic checks have been passed; the update will not damage linkage
        // Allow the segment to determine if the update is valid and apply it

        if (segment.update(startIndex, endIndex)) {
            return true;
        } else {
            // // If something is wrong, linkage may have been disrupted. Check
            // for debugging
            if (testLinked()) {
                return false;
            } else {

                // IJ.log("Error updating SegmentedProfile: segments unlinked:
                // "+segment.getLastFailReason());
                IJ.log(segment.toString());
                return false;
            }
        }
    }

    /**
     * Test if the all the segments are currently linked
     * 
     * @return
     */
    private boolean testLinked() {
        boolean result = true;
        for (IBorderSegment seg : this.segments) {
            if (!seg.hasNextSegment() || !seg.hasPrevSegment()) {
                result = false;
            }
        }
        return result;
    }

//    /*
//     * (non-Javadoc)
//     * 
//     * @see
//     * components.generic.ISegmentedProfile#adjustSegmentStart(java.util.UUID,
//     * int)
//     */
//    @Override
//    public boolean adjustSegmentStart(@NonNull UUID id, int amount) throws SegmentUpdateException {
//        if (!this.getSegmentIDs().contains(id)) {
//            throw new IllegalArgumentException("Segment is not part of this profile");
//        }
//
//        // get the segment within this profile, not a copy
//		try {
//			IBorderSegment segmentToUpdate = this.getSegment(id);
//		
//
//        int newValue = AbstractCellularComponent.wrapIndex(segmentToUpdate.getStartIndex() + amount,
//                segmentToUpdate.getProfileLength());
//        return this.update(segmentToUpdate, newValue, segmentToUpdate.getEndIndex());
//        
//		} catch (UnavailableComponentException e) {
//			LOGGER.log(Loggable.STACK, e.getMessage(), "Error adjusting segment start", e);
//			throw new SegmentUpdateException(e);
//		}
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see
//     * components.generic.ISegmentedProfile#adjustSegmentEnd(java.util.UUID,
//     * int)
//     */
//    @Override
//    public boolean adjustSegmentEnd(@NonNull UUID id, int amount) throws SegmentUpdateException {
//
//    	if (!this.getSegmentIDs().contains(id)) {
//    		throw new IllegalArgumentException("Segment is not part of this profile");
//    	}
//
//    	// get the segment within this profile, not a copy
//    	try {
//    		IBorderSegment segmentToUpdate = this.getSegment(id);
//
//    		int newValue = AbstractCellularComponent.wrapIndex(segmentToUpdate.getEndIndex() + amount,
//    				segmentToUpdate.getProfileLength());
//    		return this.update(segmentToUpdate, segmentToUpdate.getStartIndex(), newValue);
//    	} catch (UnavailableComponentException e) {
//    		LOGGER.log(Loggable.STACK, e.getMessage(), "Error adjusting segment start", e);
//    		throw new SegmentUpdateException(e);
//    	}
//    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#nudgeSegments(int)
     */
    @Override
    public void nudgeSegments(int amount) throws ProfileException {
//        this.segments = IBorderSegment.nudge(getSegments(), amount);
        
        List<IBorderSegment> result = getSegments();
        for(IBorderSegment s: result) {
        	s.offset(amount);
        }
        segments = result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see no.components.Profile#offset(int) Offset the segment by the given
     * amount. Returns a copy of the profile.
     */
    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#offset(int)
     */
    @Override
    public SegmentedProfile offset(int newStartIndex) throws ProfileException {

        // get the basic profile with the offset applied
        Profile offsetProfile = super.offset(newStartIndex);

        /*
         * The segmented profile starts like this:
         * 
         * 0 5 15 35 |-----|----------|--------------------|
         * 
         * After applying newStartIndex=5, the profile looks like this:
         * 
         * 0 10 30 35 |----------|--------------------|-----|
         * 
         * The new profile starts at index 'newStartIndex' in the original
         * profile This means that we must subtract 'newStartIndex' from the
         * segment positions to make them line up.
         * 
         * The nudge function in NucleusBorderSegment moves endpoints by a
         * specified amount
         * 
         */
        // IJ.log("Offsetting segments to begin at "+newStartIndex );
        // IJ.log(NucleusBorderSegment.toString(getSegments()));

//        List<IBorderSegment> segments = IBorderSegment.nudge(getSegments(), -newStartIndex);
        List<IBorderSegment> segments = getSegments();
        for(IBorderSegment s: segments) {
        	s.offset(-newStartIndex);
        }
        /*
         * Ensure that the first segment in the list is at index zero
         */

        return new SegmentedProfile(offsetProfile, segments);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.ISegmentedProfile#frankenNormaliseToProfile(components
     * .generic.ISegmentedProfile)
     */
    @Override
    public ISegmentedProfile frankenNormaliseToProfile(@NonNull ISegmentedProfile template) throws ProfileException {

        if (this.getSegmentCount() != template.getSegmentCount()) {
            throw new IllegalArgumentException("Segment counts are different in profile and template");
        }

        /*
         * The final frankenprofile is made of stitched together profiles from
         * each segment
         */
        List<IProfile> finalSegmentProfiles = new ArrayList<IProfile>(this.getSegmentCount());

        try {

            for (UUID segID : template.getSegmentIDs()) {
                // Get the corresponding segment in this profile, by segment
                // position
                IBorderSegment testSeg = this.getSegment(segID);
                IBorderSegment templateSeg = template.getSegment(segID);

                if (testSeg == null) {
                    throw new ProfileException("Cannot find segment " + segID + " in test profile");
                }

                if (templateSeg == null) {
                    throw new ProfileException("Cannot find segment " + segID + " in template profile");
                }

                // Interpolate the segment region to the new length
                IProfile revisedProfile = interpolateSegment(testSeg, templateSeg.length());
                finalSegmentProfiles.add(revisedProfile);
            }
        } catch (UnavailableComponentException e) {
            LOGGER.log(Loggable.STACK, e.getMessage(), e);
            throw new ProfileException("Error getting segment for normalising");
        }

        // Recombine the segment profiles
        Profile mergedProfile = new Profile(Profile.merge(finalSegmentProfiles));

        SegmentedProfile result = new SegmentedProfile(mergedProfile, template.getSegments());
        return result;
    }

    /**
     * The interpolation step of frankenprofile creation. The segment in this
     * profile, with the same name as the template segment is interpolated to
     * the length of the template, and returned as a new Profile.
     * 
     * @param templateSegment
     *            the segment to interpolate
     * @param newLength
     *            the new length of the segment profile
     * @return the interpolated profile
     * @throws Exception
     */
    private IProfile interpolateSegment(IBorderSegment testSeg, int newLength) throws ProfileException {

        // get the region within the segment as a new profile
        // Exclude the last index of each segment to avoid duplication
        // the first index is kept, because the first index is used for border
        // tags
        int lastIndex = AbstractCellularComponent.wrapIndex(testSeg.getEndIndex() - 1, testSeg.getProfileLength());

        IProfile testSegProfile = this.getSubregion(testSeg.getStartIndex(), lastIndex);

        // interpolate the test segments to the length of the median segments
        IProfile revisedProfile = testSegProfile.interpolate(newLength);
        return revisedProfile;
    }


    @Override
    public ISegmentedProfile interpolate(int length) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#reverse()
     */
    @Override
    public void reverse() {
        super.reverse();

        // reverse the segments
        // in a profile of 100
        // if a segment began at 10 and ended at 20, it should begin at 80 and
        // end at 90

        // if is begins at 90 and ends at 10, it should begin at 10 and end at
        // 90
        List<IBorderSegment> segments = new ArrayList<IBorderSegment>();
        for (IBorderSegment seg : this.getSegments()) {

            // invert the segment by swapping start and end
            int newStart = (this.size() - 1) - seg.getEndIndex();
            int newEnd = AbstractCellularComponent.wrapIndex(newStart + seg.length(), this.size());
            IBorderSegment newSeg = new NucleusBorderSegment(newStart, newEnd, this.size(), seg.getID());
            // newSeg.setName(seg.getName());
            // since the order is reversed, add them to the top of the new list
            segments.add(0, newSeg);
        }
        try {
            IBorderSegment.linkSegments(segments);
        } catch (ProfileException e) {
            LOGGER.log(Loggable.STACK, "Cannot link segments in reversed profile", e);
        }
        this.setSegments(segments);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.ISegmentedProfile#mergeSegments(components.nuclear.
     * NucleusBorderSegment, components.nuclear.NucleusBorderSegment,
     * java.util.UUID)
     */
    @Override
    public void mergeSegments(@NonNull IBorderSegment segment1, @NonNull IBorderSegment segment2, @NonNull UUID id) throws ProfileException {

        // Check the segments belong to the profile
        if (!this.contains(segment1) || !this.contains(segment2)) {
            throw new IllegalArgumentException("An input segment is not part of this profile");
        }

        // Check the segments are linked
        if (!segment1.nextSegment().equals(segment2) && !segment1.prevSegment().equals(segment2)) {
            throw new IllegalArgumentException("Input segments are not linked");
        }

        // Ensure we have the segments in the correct order
        IBorderSegment firstSegment = segment1.nextSegment().equals(segment2) ? segment1 : segment2;
        IBorderSegment secondSegment = segment2.nextSegment().equals(segment1) ? segment1 : segment2;

        // Create the new segment
        int startIndex = firstSegment.getStartIndex();
        int endIndex = secondSegment.getEndIndex();
        NucleusBorderSegment mergedSegment = new NucleusBorderSegment(startIndex, endIndex, this.size(), id);
        // mergedSegment.setName(firstSegment.getName());

        mergedSegment.addMergeSource(firstSegment);
        mergedSegment.addMergeSource(secondSegment);

        // Replace the two segments in this profile
        List<IBorderSegment> oldSegs = this.getSegments();
        List<IBorderSegment> newSegs = new ArrayList<IBorderSegment>();

        int position = 0;
        for (IBorderSegment oldSegment : oldSegs) {

            if (oldSegment.equals(firstSegment)) {
                // add the merge instead
                mergedSegment.setPosition(position);
                newSegs.add(mergedSegment);
            } else if (oldSegment.equals(secondSegment)) {
                // do nothing
            } else {
                // add the original segments
                oldSegment.setPosition(position);
                newSegs.add(oldSegment);
            }
            position++;
        }

        IBorderSegment.linkSegments(newSegs);

        this.setSegments(newSegs);
    }

    @Override
	public void unmergeSegment(@NonNull UUID segId) throws ProfileException {
		try {
			unmergeSegment(getSegment(segId));
		} catch(UnavailableComponentException e) {
			throw new ProfileException(e);
		}
	}
    
    @Override
    public void unmergeSegment(@NonNull IBorderSegment segment) throws ProfileException {
        // Check the segments belong to the profile
        if (!this.contains(segment)) {
            throw new IllegalArgumentException("Input segment is not part of this profile");
        }

        if (!segment.hasMergeSources()) {
            throw new IllegalArgumentException("Segment does not have merge sources");
        }

        // Replace the two segments in this profile
        List<IBorderSegment> oldSegs = this.getSegments();
        List<IBorderSegment> newSegs = new ArrayList<IBorderSegment>();

        int position = 0;
        for (IBorderSegment oldSegment : oldSegs) {

            if (oldSegment.equals(segment)) {

                // add each of the old segments
                for (IBorderSegment mergedSegment : segment.getMergeSources()) {
                    mergedSegment.setPosition(position);
                    newSegs.add(mergedSegment);
                    position++;
                }

            } else {

                // add the original segments
                oldSegment.setPosition(position);
                newSegs.add(oldSegment);
            }
            position++;
        }
        IBorderSegment.linkSegments(newSegs);
        this.setSegments(newSegs);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.ISegmentedProfile#splitSegment(components.nuclear.
     * NucleusBorderSegment, int, java.util.UUID, java.util.UUID)
     */
    @Override
    public void splitSegment(@NonNull IBorderSegment segment, int splitIndex, @NonNull UUID id1, @NonNull UUID id2) throws ProfileException {
        // Check the segments belong to the profile
        if (!this.contains(segment)) {
            throw new IllegalArgumentException("Input segment is not part of this profile");
        }

        if (!segment.contains(splitIndex)) {
            throw new IllegalArgumentException("Splitting index is not within the segment");
        }

        // Replace the two segments in this profile
        List<IBorderSegment> oldSegs = this.getSegments();
        List<IBorderSegment> newSegs = new ArrayList<IBorderSegment>();

        // Add the new segments to a list
        List<IBorderSegment> splitSegments = new ArrayList<IBorderSegment>();
        splitSegments.add(new NucleusBorderSegment(segment.getStartIndex(), splitIndex, segment.getProfileLength(), id1));
        splitSegments.add(new NucleusBorderSegment(splitIndex, segment.getEndIndex(), segment.getProfileLength(), id2));

        segment.addMergeSource(splitSegments.get(0));
        segment.addMergeSource(splitSegments.get(1));

        int position = 0;
        for (IBorderSegment oldSegment : oldSegs) {

            if (oldSegment.equals(segment)) {

                // add each of the old segments
                // for(NucleusBorderSegment mergedSegment : splitSegments){
                // mergedSegment.setPosition(position);
                newSegs.add(segment);
                // position++;
                // }

            } else {

                // add the original segments
                oldSegment.setPosition(position);
                newSegs.add(oldSegment);
            }
            position++;
        }
        IBorderSegment.linkSegments(newSegs);
        this.setSegments(newSegs);
        this.unmergeSegment(segment);

    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (IBorderSegment seg : this.segments) {
            builder.append(seg.toString() + System.getProperty("line.separator"));
        }
        return builder.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#valueString()
     */
    @Override
    public String valueString() {
        return super.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((segments == null) ? 0 : segments.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        SegmentedProfile other = (SegmentedProfile) obj;
        if (segments == null) {
            if (other.segments != null)
                return false;
        } else if (!segments.equals(other.segments))
            return false;
        return true;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        // finest("\tWriting segmented profile");
        out.defaultWriteObject();
        // finest("\tWrote segmented profile");
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        // finest("\tReading segmented profile");
        in.defaultReadObject();
        // finest("\tRead segmented profile");
    }

    @Override
    public boolean isSplittable(@NonNull UUID id, int splitIndex) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public ISegmentedProfile copy() {
        try {
            return new SegmentedProfile(this);
        } catch (IndexOutOfBoundsException e) {
            LOGGER.log(Loggable.STACK, e.getMessage(), e);
        }
        return null;
    }

	@Override
	public void mergeSegments(@NonNull UUID segment1, @NonNull UUID segment2, @NonNull UUID mergedId)
			throws ProfileException {
		// TODO Auto-generated method stub
		
	}
}
