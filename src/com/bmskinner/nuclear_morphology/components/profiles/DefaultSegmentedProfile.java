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
package com.bmskinner.nuclear_morphology.components.profiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.MissingComponentException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * The default implementation of a segmented profile.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultSegmentedProfile extends DefaultProfile implements ISegmentedProfile {
	
	private static final Logger LOGGER = Logger.getLogger(DefaultSegmentedProfile.class.getName());

    // the segments
    private IProfileSegment[] segments = new IProfileSegment[0];

    /**
     * Construct using a regular profile and a list of border segments
     * 
     * @param p the profile
     * @param segments the list of segments to use
     * @throws ProfileException
     */
    public DefaultSegmentedProfile(@NonNull final IProfile p, @NonNull final List<IProfileSegment> segments) throws ProfileException {
        super(p);
        if (segments.isEmpty())
            throw new IllegalArgumentException("Segment list is empty in segmented profile contructor");

        if (p.size() != segments.get(0).getProfileLength())
            throw new IllegalArgumentException(String.format("Cannot construct new profile; segment profile length (%d) does not fit this profile (%d)", segments.get(0).getProfileLength(), p.size()));

        // Link and add the segments into this profile        
        this.segments = new IProfileSegment[segments.size()];
        for (int i = 0; i < segments.size(); i++) {
            this.segments[i] = segments.get(i).copy();
        }
        
        IProfileSegment.linkSegments(this.segments);
    }

    /**
     * Construct using an existing profile. Copies the data and segments
     * 
     * @param profile the segmented profile to copy
     * @throws ProfileException
     * @throws IndexOutOfBoundsException
     */
    public DefaultSegmentedProfile(@NonNull final ISegmentedProfile profile) throws IndexOutOfBoundsException, ProfileException {
        this(profile, profile.getSegments());
    }

    /**
     * Construct using a basic profile. One segments is created that spans the
     * entire profile
     * 
     * @param profile
     * @throws ProfileException 
     */
    public DefaultSegmentedProfile(@NonNull final IProfile profile) throws ProfileException {
        super(profile);
        segments = new IProfileSegment[1];
        segments[0] = IProfileSegment.newSegment(0, 0, profile.size(), IProfileCollection.DEFAULT_SEGMENT_ID);
        IProfileSegment.linkSegments(this.segments);
    }

    /**
     * Construct from an array of values
     * 
     * @param values
     * @throws ProfileException 
     * @throws Exception
     */
    public DefaultSegmentedProfile(float[] values) throws ProfileException {
        this(new DefaultProfile(values));
    }

    @Override
    public boolean hasSegments() {
        return segments!=null && segments.length>0;
    }

    @Override
    public @NonNull List<IProfileSegment> getSegments() {
        try {
           return IProfileSegment.copyAndLink(segments);
        } catch (ProfileException | IllegalArgumentException e) {
            LOGGER.log(Loggable.STACK, "Error copying segments", e);
            return new ArrayList<>();
        }
    }

    @Override
    public @NonNull IProfileSegment getSegment(@NonNull UUID id) throws MissingComponentException {
        for (IProfileSegment seg : this.segments) {
            if (seg.getID().equals(id)) {
                return seg;
            }
        }
        throw new MissingComponentException("Segment with id " + id.toString() + " not found");

    }

    @Override
    public boolean hasSegment(@NonNull UUID id) {
        for (IProfileSegment seg : this.segments) {
            if (seg.getID().equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<IProfileSegment> getSegmentsFrom(@NonNull UUID id) throws MissingComponentException, ProfileException {
        return getSegmentsFrom(getSegment(id));
    }

    /**
     * Get the segments in order from the given segment
     * 
     * @param firstSeg
     * @return
     * @throws ProfileException 
     */
    private List<IProfileSegment> getSegmentsFrom(@NonNull IProfileSegment firstSeg) throws ProfileException {

        List<IProfileSegment> result = new ArrayList<>();
        
        result.add(firstSeg);
        
        IProfileSegment nextSeg = firstSeg.nextSegment();
        while(nextSeg!=firstSeg){
        	result.add(nextSeg);
        	nextSeg = nextSeg.nextSegment();
        }
        return IProfileSegment.copyAndLink(result);
    }

    @Override
    public List<IProfileSegment> getOrderedSegments() {
    	try {
			for (IProfileSegment seg : getSegments()) {
				if (seg.contains(ZERO_INDEX) && (getSegmentCount()==1 || seg.getEndIndex()!=ZERO_INDEX))
					return getSegmentsFrom(seg);
			}
		} catch (ProfileException e) {
			LOGGER.warning("Profile error getting segments");
			LOGGER.log(Loggable.STACK, "Profile error getting segments", e);
			return new ArrayList<>();
		}
		return new ArrayList<>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.ISegmentedProfile#getSegment(java.lang.String)
     */
    @Override
    public IProfileSegment getSegment(@NonNull String name) throws MissingComponentException {

        for (IProfileSegment seg : this.segments) {
            if (seg.getName().equals(name)) {
                return seg;
            }
        }
        throw new MissingComponentException("Requested segment name is not present");
    }

    @Override
    public IProfileSegment getSegment(@NonNull IProfileSegment segment) throws ProfileException {
        if (!this.contains(segment)) {
            throw new IllegalArgumentException("Requested segment name is not present");
        }

        for (IProfileSegment seg : this.segments) {
            if (seg.equals(segment)) {
                return seg;
            }
        }
        throw new ProfileException("Cannot find segment "+segment.toString());
    }

    @Override
    public IProfileSegment getSegmentContaining(int index) {

        if (index < 0 || index >= this.size()) {
            throw new IllegalArgumentException("Index is out of profile bounds");
        }

        for (IProfileSegment seg : segments) {
            if (seg.contains(index)) {
                return seg;
            }
        }
        throw new IllegalArgumentException("Index not in profile");
    }

    @Override
    public void setSegments(@NonNull List<IProfileSegment> segList) {
        if (segList.isEmpty())
            throw new IllegalArgumentException("Segment list is null or empty");

        if (segList.get(0).getProfileLength() != this.size())
            throw new IllegalArgumentException("Segment list is from a different length profile");

        try {
            segments = new IProfileSegment[segList.size()];
            for (int i = 0; i < segList.size(); i++) {
                this.segments[i] = segList.get(i).copy();
            }

            IProfileSegment.linkSegments(segments);

        } catch (Exception e) {
            LOGGER.warning("Cannot copy segments");
        }
    }

    @Override
    public void clearSegments() throws ProfileException {
        segments = new IProfileSegment[1];
        segments[0] = IProfileSegment.newSegment(0, 0, size(), IProfileCollection.DEFAULT_SEGMENT_ID);
        IProfileSegment.linkSegments(segments);
    }

    @Override
    public List<String> getSegmentNames() {
        List<String> result = new ArrayList<>();
        for (IProfileSegment seg : segments) {
            result.add(seg.getName());
        }
        return result;
    }

    @Override
    public List<UUID> getSegmentIDs() {
        List<UUID> result = new ArrayList<>();
        for (IProfileSegment seg : this.segments) {
            result.add(seg.getID());
        }
        return result;
    }

    @Override
    public int getSegmentCount() {
        return this.segments.length;
    }

    @Override
    public double getDisplacement(@NonNull IProfileSegment segment) {
        
        if (!contains(segment)) {
            throw new IllegalArgumentException("Segment is not in profile: "+segment);
        }
        double start = this.get(segment.getStartIndex());
        double end = this.get(segment.getEndIndex());

        double min = Math.min(start, end);
        double max = Math.max(start, end);

        return max - min;
    }

    @Override
    public boolean contains(@NonNull IProfileSegment segment) {
        for (IProfileSegment seg : this.segments) {
            if (seg.equals(segment)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean update(@NonNull IProfileSegment segment, int startIndex, int endIndex) throws SegmentUpdateException, ProfileException {

        if (!this.contains(segment))
            throw new IllegalArgumentException("Segment is not part of this profile");

        // test effect on all segments in list: the update should
        // not allow the endpoints to move within a segment other than
        // next or prev
        IProfileSegment nextSegment = segment.nextSegment();
        IProfileSegment prevSegment = segment.prevSegment();

        for (IProfileSegment testSeg : this.segments) {

            // if the proposed start or end index is found in another segment
            // that is not next or prev, do not proceed
            if (testSeg.contains(startIndex) || testSeg.contains(endIndex)) {

                if(!testSeg.getName().equals(segment.getName()) && !testSeg.getName().equals(nextSegment.getName())
                        && !testSeg.getName().equals(prevSegment.getName())) {
                    return false;
                }
            }
        }

        // the basic checks have been passed; the update will not damage linkage
        // Allow the segment to determine if the update is valid and apply it
        
        return getSegment(segment).update(startIndex, endIndex);
    }

    @Override
    public void moveSegments(int offset) throws ProfileException {

        List<IProfileSegment> result = getSegments();

        this.segments = new IProfileSegment[segments.length];
        for (int i = 0; i < segments.length; i++) {
            this.segments[i] = result.get(i).offset(offset);
        }
        IProfileSegment.linkSegments(this.segments);
    }

    @Override
    public ISegmentedProfile startFrom(int newStartIndex) throws ProfileException {

//    	LOGGER.fine("Getting profile pre offset of "+newStartIndex+": "+this);
        // get the basic profile with the offset applied
        IProfile offsetProfile = super.startFrom(newStartIndex);

        /*
         * An example segmented profile starts like this:
         * 
         * 0 A	 5    B     15        C          35 
         * |-----|----------|--------------------|
         * 
         * After applying offset=5, the profile looks like this:
         * 
         * 0    B     10         C         30 A  35 
         * |----------|--------------------|-----|
         * 
         * The new profile starts at index 'offset' in the original profile. This
         * means that we must subtract 'offset' from the segment positions to
         * make them line up.
         * 
         */        
        ISegmentedProfile s = new DefaultSegmentedProfile(offsetProfile, getSegments());
        s.moveSegments(-newStartIndex);
        return s;        
    }
    
    

    @Override
    public ISegmentedProfile interpolate(int length) throws ProfileException {
    	if(length<1)
    		throw new IllegalArgumentException("Cannot interpolate to a zero or negative length");
    	
    	
    	LOGGER.finer("Interpolating profile with "+segments.length+" segments from "+this.size()+" to "+length);
    	
        // interpolate the IProfile
        IProfile newProfile = super.interpolate(length);
        List<IProfileSegment> newSegs = new ArrayList<>();
        
        // No segments in profile or single default segment
        if(segments.length<=1) {
        	newSegs.add(new DefaultProfileSegment(0, 0, length, IProfileCollection.DEFAULT_SEGMENT_ID));
        	return new DefaultSegmentedProfile(newProfile, newSegs);
        }
            	

        // get the proportions of the existing segments
        double[] props = new double[segments.length];

        for (int i = 0; i < segments.length; i++) {
            props[i] = this.getFractionOfIndex(segments[i].getStartIndex());
        }

        // get the target start indexes of the new segments

        int[] newStarts = new int[segments.length];

        for (int i = 0; i < segments.length; i++) {
            newStarts[i] = (int) (props[i] * (double) length);
        }

        // Make the new segments
        
        for (int i = 0; i < segments.length - 1; i++) {

            int testStart = newStarts[i];
            int testEnd = newStarts[i + 1];

            if (testEnd - testStart < IProfileSegment.MINIMUM_SEGMENT_LENGTH) {
                newStarts[i + 1] = newStarts[i + 1] + 1;
            }
            
            LOGGER.finer("Creating segment "+newStarts[i]+" - "+newStarts[i + 1]);

            IProfileSegment seg = new DefaultProfileSegment(newStarts[i], newStarts[i + 1], length, segments[i].getID());
            newSegs.add(seg);
        }

        // Add final segment
        // We need to correct start and end positions appropriately.
        // Since the start position may already have been set, we need to adjust
        // the end,
        // i.e the start position of the first segment.

        try {
            int firstStart = newStarts[0];
            int lastStart = newStarts[segments.length - 1];
            if (newSegs.get(0).wraps(newStarts[segments.length - 1], newStarts[0])) {
                // wrapping final segment
                if (firstStart + (length - lastStart) < IProfileSegment.MINIMUM_SEGMENT_LENGTH) {
                    newStarts[0] = firstStart + 1; // update the start in the
                                                   // array
                    newSegs.get(0).update(firstStart, newSegs.get(1).getStartIndex()); // update
                                                                                       // the
                                                                                       // new
                                                                                       // segment
                }

            } else {
                // non-wrapping final segment
                if (firstStart - lastStart < IProfileSegment.MINIMUM_SEGMENT_LENGTH) {
                    newStarts[0] = firstStart + 1; // update the start in the
                                                   // array
                    newSegs.get(0).update(firstStart, newSegs.get(1).getStartIndex()); // update
                                                                                       // the
                                                                                       // new
                                                                                       // segment

                }
            }
        } catch (SegmentUpdateException e) {
            throw new ProfileException("Could not update segment indexes");
        }

        IProfileSegment lastSeg = new DefaultProfileSegment(newStarts[segments.length - 1], newStarts[0], length,
                segments[segments.length - 1].getID());
        newSegs.add(lastSeg);

        if (newSegs.size() != segments.length) {
            throw new ProfileException("Error interpolating segments");
        }

        // assign new segments
        IProfileSegment.linkSegments(newSegs);
        return new DefaultSegmentedProfile(newProfile, newSegs);
    }

    /**
     * The interpolation step of frankenprofile creation. The segment in this
     * profile, with the same name as the template segment is interpolated to
     * the length of the template, and returned as a new Profile.
     * 
     * @param templateSegment the segment to interpolate
     * @param newLength the new length of the segment profile
     * @return the interpolated profile
     * @throws Exception
     */
    private IProfile interpolateSegment(IProfileSegment testSeg, int newLength) throws ProfileException {

        // get the region within the segment as a new profile
        // Exclude the last index of each segment to avoid duplication
        // the first index is kept, because the first index is used for border
        // tags
        int lastIndex = CellularComponent.wrapIndex(testSeg.getEndIndex() - 1, testSeg.getProfileLength());

        IProfile testSegProfile = this.getSubregion(testSeg.getStartIndex(), lastIndex);

        // interpolate the test segments to the length of the median segments
        IProfile revisedProfile = testSegProfile.interpolate(newLength);
        return revisedProfile;
    }


    @Override
    public void reverse() {
        super.reverse();
        
        LOGGER.finer("Reversing profile");

        // reverse the segments
        List<IProfileSegment> newSegments = new ArrayList<>();
        for (IProfileSegment seg : this.getSegments()) {
        	newSegments.add(0, seg.reverse());
        }
        try {        	
            IProfileSegment.linkSegments(newSegments);
        } catch (ProfileException e) {
            LOGGER.warning("Error linking segments");
            LOGGER.log(Loggable.STACK, "Cannot link segments in reversed profile", e);
        }
        this.setSegments(newSegments);
    }
    
    @Override
    public void mergeSegments(@NonNull UUID seg1Id, @NonNull UUID seg2Id, @NonNull UUID newId) throws ProfileException {
        
        IProfileSegment segment1;
        IProfileSegment segment2;
		try {
			segment1 = getSegment(seg1Id);
			segment2 = getSegment(seg2Id);
		} catch (MissingComponentException e) {
			throw new IllegalArgumentException("An input segment is not part of this profile: "+e.getMessage());
		}
		
        // Check the segments belong to the profile
        if (!this.contains(segment1) || !this.contains(segment2))
            throw new IllegalArgumentException("An input segment is not part of this profile");

        if(!segment1.hasNextSegment() || !segment2.hasPrevSegment())
            throw new IllegalArgumentException("Input segments are not linked: "+segment1.toString()+" and "+segment2.toString());
        
        // Check the segments are linked
        if (!segment1.nextSegment().getID().equals(segment2.getID()) && !segment1.prevSegment().getID().equals(segment2.getID()))
            throw new IllegalArgumentException("Input segments are not linked");

        // Ensure we have the segments in the correct order
        IProfileSegment firstSegment =  segment1.nextSegment().getID().equals(segment2.getID()) ? segment1 : segment2;
        IProfileSegment secondSegment = segment2.nextSegment().getID().equals(segment1.getID()) ? segment1 : segment2;

        // Create the new segment
        int startIndex = firstSegment.getStartIndex();
        int endIndex   = secondSegment.getEndIndex();
        IProfileSegment mergedSegment = IProfileSegment.newSegment(startIndex, endIndex, this.size(), newId);

        LOGGER.fine(()->"Merged segment has source 1: "+mergedSegment.hasMergeSource(seg1Id));
        LOGGER.fine(()->"Merged segment has source 2: "+mergedSegment.hasMergeSource(seg2Id));
        // Replace the two segments in this profile
        List<IProfileSegment> oldSegs = this.getSegments();
        List<IProfileSegment> newSegs = new ArrayList<>();


        for (IProfileSegment oldSegment : oldSegs) {
            if (oldSegment.getID().equals(firstSegment.getID())) {
                // add the merge instead
                newSegs.add(mergedSegment);
            } else if (oldSegment.getID().equals(secondSegment.getID())) {
                // do nothing
            } else {
                // add the original segments
                newSegs.add(oldSegment);
            }
        }
  
        IProfileSegment.linkSegments(newSegs);

        mergedSegment.addMergeSource(firstSegment);
        mergedSegment.addMergeSource(secondSegment);
        
        this.setSegments(newSegs);
    }
    
    @Override
	public void mergeSegments(@NonNull IProfileSegment segment1, @NonNull IProfileSegment segment2, @NonNull UUID id) throws ProfileException {
		mergeSegments(segment1.getID(), segment2.getID(), id);
	}

    @Override
	public void unmergeSegment(@NonNull UUID segId) throws ProfileException {
		try {
			unmergeSegment(getSegment(segId));
		} catch(MissingComponentException e) {
			throw new ProfileException(e);
		}
	}

    @Override
    public void unmergeSegment(@NonNull IProfileSegment segment) throws ProfileException {
        // Check the segments belong to the profile
        if (!this.contains(segment)) {
            throw new IllegalArgumentException("Input segment is not part of this profile");
        }

        if (!segment.hasMergeSources())
            return;

        // Replace the two segments in this profile
        List<IProfileSegment> oldSegs = this.getSegments();
        List<IProfileSegment> newSegs = new ArrayList<>();

        int position = 0;
        for (IProfileSegment oldSegment : oldSegs) {

            if (oldSegment.equals(segment)) {

                // add each of the old segments
                for (IProfileSegment mergedSegment : segment.getMergeSources()) {
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
        IProfileSegment.linkSegments(newSegs);
        this.setSegments(newSegs);

    }

    @Override
    public boolean isSplittable(@NonNull UUID id, int splitIndex) {
        if (!this.hasSegment(id))
            return false;

        try {
        	IProfileSegment segment = getSegment(id);
        	
        	if (!segment.contains(splitIndex))
                return false;

            return IProfileSegment.isLongEnough(segment.getStartIndex(), splitIndex, segment.getProfileLength())
                    && IProfileSegment.isLongEnough(splitIndex, segment.getEndIndex(), segment.getProfileLength());
        } catch (MissingComponentException e) {
            LOGGER.log(Loggable.STACK, e.getMessage(), e);
            return false;
        }

    }

    @Override
    public void splitSegment(@NonNull IProfileSegment segment, int splitIndex, @NonNull UUID id1, @NonNull UUID id2) throws ProfileException {
        // Check the segments belong to the profile
        if (!this.contains(segment)) {
            throw new IllegalArgumentException("Input segment is not part of this profile");
        }

        if (!segment.contains(splitIndex)) {
            throw new IllegalArgumentException("Splitting index is not within the segment");
        }

        // Remove old merge sources from this segment
        segment.clearMergeSources();

        /*
         * Create two new segments, make them into merge sources for the segment
         * to be split then use the existing unmerge method to put them into the
         * full profile
         */

        // Replace the two segments in this profile
        List<IProfileSegment> oldSegs = this.getSegments();
        List<IProfileSegment> newSegs = new ArrayList<>();

        // Add the new segments to a list
        List<IProfileSegment> splitSegments = new ArrayList<>();
        splitSegments
                .add(IProfileSegment.newSegment(segment.getStartIndex(), splitIndex, segment.getProfileLength(), id1));
        splitSegments.add(IProfileSegment.newSegment(splitIndex, segment.getEndIndex(), segment.getProfileLength(), id2));

        segment.addMergeSource(splitSegments.get(0));
        segment.addMergeSource(splitSegments.get(1));

        int position = 0;
        for (IProfileSegment oldSegment : oldSegs) {

            if (oldSegment.equals(segment)) {
                newSegs.add(segment);
            } else {

                // add the original segments
                oldSegment.setPosition(position);
                newSegs.add(oldSegment);
            }
            position++;
        }
        IProfileSegment.linkSegments(newSegs);
        this.setSegments(newSegs);
        this.unmergeSegment(segment);

    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Profile");
        for (IProfileSegment seg : this.getOrderedSegments()) {
            builder.append(" | "+seg.toString());
        }
        return builder.toString();
    }


    @Override
    public String valueString() {
        return super.toString();
    }
    
    @Override
    public ISegmentedProfile copy() throws ProfileException {
    	return new DefaultSegmentedProfile(this);
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((segments == null) ? 0 : Arrays.hashCode(segments));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultSegmentedProfile other = (DefaultSegmentedProfile) obj;
        if (segments == null) {
            if (other.segments != null)
                return false;
        } else if (!Arrays.equals(segments, other.segments))
            return false;
        return true;
    }
}
