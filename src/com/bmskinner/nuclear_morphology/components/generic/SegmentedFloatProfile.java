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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * The default implementation of a segmented profile.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class SegmentedFloatProfile extends FloatProfile implements ISegmentedProfile {
	
	private static final Logger LOGGER = Logger.getLogger(SegmentedFloatProfile.class.getName());
    private static final long serialVersionUID = 1L;

    // the segments
    protected IBorderSegment[] segments = new IBorderSegment[0];

    /**
     * Construct using a regular profile and a list of border segments
     * 
     * @param p the profile
     * @param segments the list of segments to use
     * @throws ProfileException
     */
    public SegmentedFloatProfile(@NonNull final IProfile p, @NonNull final List<IBorderSegment> segments) throws ProfileException {
        super(p);
        if (segments == null || segments.isEmpty())
            throw new IllegalArgumentException("Segment list is null or empty in segmented profile contructor");

        if (p.size() != segments.get(0).getProfileLength())
            throw new IllegalArgumentException(String.format("Cannot construct new profile; segment profile length (%d) does not fit this profile (%d)", segments.get(0).getProfileLength(), p.size()));

        // Link and add the segments into this profile
        
        
        
        IBorderSegment.linkSegments(segments);

        this.segments = new IBorderSegment[segments.size()];
        for (int i = 0; i < segments.size(); i++) {
            this.segments[i] = segments.get(i);
        }
    }

    /**
     * Construct using an existing profile. Copies the data and segments
     * 
     * @param profile the segmented profile to copy
     * @throws ProfileException
     * @throws IndexOutOfBoundsException
     */
    public SegmentedFloatProfile(@NonNull final ISegmentedProfile profile) throws IndexOutOfBoundsException, ProfileException {
        this(profile, profile.getSegments());
    }

    /**
     * Construct using a basic profile. Two segments are created that span the
     * entire profile, half each
     * 
     * @param profile
     */
    public SegmentedFloatProfile(@NonNull final IProfile profile) {
        super(profile);
        segments = new IBorderSegment[1];
        segments[0] = IBorderSegment.newSegment(0, 0, profile.size(), IProfileCollection.DEFAULT_SEGMENT_ID);
    }

    /**
     * Construct from an array of values
     * 
     * @param values
     * @throws Exception
     */
    public SegmentedFloatProfile(float[] values) {
        this(new FloatProfile(values));
    }

    @Override
    public boolean hasSegments() {
        return segments!=null && segments.length>0;
    }

    @Override
    public @NonNull List<IBorderSegment> getSegments() {
    	
        List<IBorderSegment> temp = new ArrayList<>();
        if(segments.length==0)
        	return temp;
        
        for (IBorderSegment seg : segments) {
            temp.add(seg);
        }

        try {
           return IBorderSegment.copy(temp);
        } catch (ProfileException | IllegalArgumentException e) {
            LOGGER.log(Loggable.STACK, "Error copying segments", e);
            return new ArrayList<>();
        }
    }

    @Override
    public @NonNull IBorderSegment getSegment(@NonNull UUID id) throws UnavailableComponentException {
        for (IBorderSegment seg : this.segments) {
            if (seg.getID().equals(id)) {
                return seg;
            }
        }
        throw new UnavailableComponentException("Segment with id " + id.toString() + " not found");

    }

    @Override
    public boolean hasSegment(@NonNull UUID id) {
        for (IBorderSegment seg : this.segments) {
            if (seg.getID().equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<IBorderSegment> getSegmentsFrom(@NonNull UUID id) throws UnavailableComponentException, ProfileException {
        return getSegmentsFrom(getSegment(id));
    }

    /**
     * Get the segments in order from the given segment
     * 
     * @param firstSeg
     * @return
     * @throws ProfileException 
     * @throws Exception
     */
    private List<IBorderSegment> getSegmentsFrom(@NonNull IBorderSegment firstSeg) throws UnavailableComponentException, ProfileException {

        if (firstSeg == null)
            throw new IllegalArgumentException("Requested first segment is null");

        List<IBorderSegment> result = new ArrayList<>();
        
        result.add(firstSeg);
        
        IBorderSegment nextSeg = firstSeg.nextSegment();
        while(nextSeg!=firstSeg){
        	result.add(nextSeg);
        	nextSeg = nextSeg.nextSegment();
        }
        return IBorderSegment.copy(result);
    }

    @Override
    public List<IBorderSegment> getOrderedSegments() {
    	try {
			for (IBorderSegment seg : getSegments()) {
				if (seg.contains(ZERO_INDEX) && (getSegmentCount()==1 || seg.getEndIndex()!=ZERO_INDEX))
					return getSegmentsFrom(seg);
			}
		} catch (UnavailableComponentException | ProfileException e) {
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
    public IBorderSegment getSegment(@NonNull String name) throws UnavailableComponentException {
        if (name == null) {
            throw new IllegalArgumentException("Requested segment name is null");
        }

        for (IBorderSegment seg : this.segments) {
            if (seg.getName().equals(name)) {
                return seg;
            }
        }
        throw new UnavailableComponentException("Requested segment name is not present");
    }

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

    @Override
    public IBorderSegment getSegmentAt(int position) {

        if(position < 0 || position > segments.length-1){
            throw new IllegalArgumentException("Segment position is out of bounds");
        }
        IBorderSegment result = null;
        for (IBorderSegment seg : this.segments) {
            if (seg.getPosition() == position) {
                result = seg;
            }
        }
        return result;
    }

    @Override
    public IBorderSegment getSegmentContaining(int index) {

        if (index < 0 || index >= this.size()) {
            throw new IllegalArgumentException("Index is out of profile bounds");
        }

        for (IBorderSegment seg : segments) {
            if (seg.contains(index)) {
                return seg;
            }
        }
        throw new IllegalArgumentException("Index not in profile");
    }

    @Override
    public void setSegments(@NonNull List<IBorderSegment> segments) {
        if (segments == null || segments.isEmpty())
            throw new IllegalArgumentException("Segment list is null or empty");

        if (segments.get(0).getProfileLength() != this.size())
            throw new IllegalArgumentException("Segment list is from a different length profile");

        try {
            segments = IBorderSegment.copy(segments);

            this.segments = new IBorderSegment[segments.size()];
            for (int i = 0; i < segments.size(); i++) {
                this.segments[i] = segments.get(i);
            }

        } catch (ProfileException e) {
            LOGGER.warning("Cannot copy segments");
        }
    }

    @Override
    public void clearSegments() {
        segments = new IBorderSegment[1];
        segments[0] = IBorderSegment.newSegment(0, 0, size(), IProfileCollection.DEFAULT_SEGMENT_ID);
    }

    @Override
    public List<String> getSegmentNames() {
        List<String> result = new ArrayList<>();
        for (IBorderSegment seg : segments) {
            result.add(seg.getName());
        }
        return result;
    }

    @Override
    public List<UUID> getSegmentIDs() {
        List<UUID> result = new ArrayList<>();
        for (IBorderSegment seg : this.segments) {
            result.add(seg.getID());
        }
        return result;
    }

    @Override
    public int getSegmentCount() {
        return this.segments.length;
    }

    @Override
    public double getDisplacement(@NonNull IBorderSegment segment) {
        
        if (!contains(segment)) {
            throw new IllegalArgumentException("Segment is not in profile");
        }
        double start = this.get(segment.getStartIndex());
        double end = this.get(segment.getEndIndex());

        double min = Math.min(start, end);
        double max = Math.max(start, end);

        return max - min;
    }

    @Override
    public boolean contains(@NonNull IBorderSegment segment) {
        if (segment == null) {
            return false;
        }

        for (IBorderSegment seg : this.segments) {
            if (seg.equals(segment)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean update(@NonNull IBorderSegment segment, int startIndex, int endIndex) throws SegmentUpdateException {

        if (!this.contains(segment))
            throw new IllegalArgumentException("Segment is not part of this profile");

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
                    return false;
                }
            }
        }

        // the basic checks have been passed; the update will not damage linkage
        // Allow the segment to determine if the update is valid and apply it

        return segment.update(startIndex, endIndex);
    }

    @Override
    public void nudgeSegments(int amount) {

        List<IBorderSegment> result = getSegments();
        for(IBorderSegment s: result) {
        	s.offset(amount);
        }
//        try {
//            result = IBorderSegment.nudge(getSegments(), amount);
//        } catch (ProfileException e) {
//            LOGGER.fine("Error offsetting segments", e);
//            return;
//        }
        this.segments = new IBorderSegment[segments.length];
        for (int i = 0; i < segments.length; i++) {
            this.segments[i] = result.get(i);
        }

    }

    @Override
    public ISegmentedProfile offset(int offset) throws ProfileException {

        // get the basic profile with the offset applied
        IProfile offsetProfile = super.offset(offset);

        /*
         * The segmented profile starts like this:
         * 
         * 0 5 15 35 |-----|----------|--------------------|
         * 
         * After applying offset=5, the profile looks like this:
         * 
         * 0 10 30 35 |----------|--------------------|-----|
         * 
         * The new profile starts at index 'offset' in the original profile This
         * means that we must subtract 'offset' from the segment positions to
         * make them line up.
         * 
         * The nudge function in IBorderSegment moves endpoints by a specified
         * amount
         * 
         */
        
        List<IBorderSegment> segments = getSegments();
        for(IBorderSegment s: segments) {
        	s.offset(-offset);
        }
                
//        List<IBorderSegment> segments = IBorderSegment.nudge(getSegments(), -offset);
        return new SegmentedFloatProfile(offsetProfile, segments);
    }
    
    

    @Override
    public ISegmentedProfile interpolate(int length) throws ProfileException {
    	if(length<1)
    		throw new IllegalArgumentException("Cannot interpolate to a zero or negative length");
        // interpolate the IProfile
        IProfile newProfile = super.interpolate(length);
        List<IBorderSegment> newSegs = new ArrayList<>();
        
        // No segments in profile or single default segment
        if(segments.length<=1) {
        	newSegs.add(new DefaultBorderSegment(0, 0, length, IProfileCollection.DEFAULT_SEGMENT_ID));
        	return new SegmentedFloatProfile(newProfile, newSegs);
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

            if (testEnd - testStart < IBorderSegment.MINIMUM_SEGMENT_LENGTH) {
                newStarts[i + 1] = newStarts[i + 1] + 1;
            }

            IBorderSegment seg = new DefaultBorderSegment(newStarts[i], newStarts[i + 1], length, segments[i].getID());
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
                if (firstStart + (length - lastStart) < IBorderSegment.MINIMUM_SEGMENT_LENGTH) {
                    newStarts[0] = firstStart + 1; // update the start in the
                                                   // array
                    newSegs.get(0).update(firstStart, newSegs.get(1).getStartIndex()); // update
                                                                                       // the
                                                                                       // new
                                                                                       // segment
                }

            } else {
                // non-wrapping final segment
                if (firstStart - lastStart < IBorderSegment.MINIMUM_SEGMENT_LENGTH) {
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

        IBorderSegment lastSeg = new DefaultBorderSegment(newStarts[segments.length - 1], newStarts[0], length,
                segments[segments.length - 1].getID());
        newSegs.add(lastSeg);

        if (newSegs.size() != segments.length) {
            throw new ProfileException("Error interpolating segments");
        }

        // assign new segments
        IBorderSegment.linkSegments(newSegs);
        return new SegmentedFloatProfile(newProfile, newSegs);
    }

    @Override
    public ISegmentedProfile frankenNormaliseToProfile(@NonNull ISegmentedProfile template) throws ProfileException {
        
        if (template==null)
            throw new IllegalArgumentException("Template segment is null");

        if (this.getSegmentCount() != template.getSegmentCount())
            throw new IllegalArgumentException("Segment counts are different in profile and template");
        
        for(UUID id : template.getSegmentIDs()){
            if(!hasSegment(id))
                throw new IllegalArgumentException("Segment ids do not match between profile and template");
        }

        /*
         * The final frankenprofile is made of stitched together profiles from
         * each segment. The resulting profile should have the same length as this profile.
         * The segment boundaries should have the same proportional indexes as the template profile
         */
                
        List<IProfile> finalSegmentProfiles = new ArrayList<>(segments.length);

        try {

        	int counter = 0;
            for (UUID segID : template.getSegmentIDs()) {
                IBorderSegment thisSeg = this.getSegment(segID);
                IBorderSegment templateSeg = template.getSegment(segID);
                
                // For each segment, 1 must be subtracted from the length because the
                // segment lengths include the overlapping end and start indexes.
                int newLength = templateSeg.length()-1;

                // Interpolate the segment region to the new length
                IProfile revisedProfile = interpolateSegment(thisSeg, newLength);
                finalSegmentProfiles.add(revisedProfile);
                counter++;
            }

        } catch (UnavailableComponentException e) {
            throw new ProfileException("Unable to get segment for interpolation: "+e.getMessage(), e);
        }
        
        // Recombine the segment profiles
        IProfile mergedProfile = IProfile.merge(finalSegmentProfiles);

        if(mergedProfile.size()!=template.size())
        	throw new ProfileException(String.format("Frankenprofile has a different length (%d) to source profile (%d)", mergedProfile.size(), template.size()));
        
        return new SegmentedFloatProfile(mergedProfile, template.getSegments());
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
    private IProfile interpolateSegment(IBorderSegment testSeg, int newLength) throws ProfileException {

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

        // reverse the segments
        List<IBorderSegment> segments = new ArrayList<IBorderSegment>();
        for (IBorderSegment seg : this.getSegments()) {

            // invert the segment by swapping start and end
            int newStart = size() - 1 - seg.getEndIndex();
            int newEnd   = size() - 1 - seg.getStartIndex();
            
            IBorderSegment newSeg = new DefaultBorderSegment(newStart, newEnd, size(), seg.getID());
            segments.add(0, newSeg);
        }
        try {
            IBorderSegment.linkSegments(segments);
        } catch (ProfileException e) {
            LOGGER.warning("Error linking segments");
            LOGGER.log(Loggable.STACK, "Cannot link segments in reversed profile", e);
        }
        this.setSegments(segments);
    }
    
    @Override
    public void mergeSegments(@NonNull UUID seg1, @NonNull UUID seg2, @NonNull UUID id) throws ProfileException {

        if (seg1 == null)
            throw new IllegalArgumentException("Segment 1 cannot be null");
        if (seg2 == null)
            throw new IllegalArgumentException("Segment 2 cannot be null");
        if (id == null)
            throw new IllegalArgumentException("New segment UUID cannot be null");
        
        IBorderSegment segment1;
        IBorderSegment segment2;
		try {
			segment1 = getSegment(seg1);
			segment2 = getSegment(seg2);
		} catch (UnavailableComponentException e) {
			throw new IllegalArgumentException("An input segment is not part of this profile");
		}
		
        // Check the segments belong to the profile
        if (!this.contains(segment1) || !this.contains(segment2))
            throw new IllegalArgumentException("An input segment is not part of this profile");

        if(!segment1.hasNextSegment() || !segment2.hasPrevSegment())
            throw new IllegalArgumentException("Input segments are not linked");
        
        // Check the segments are linked
        if (!segment1.nextSegment().getID().equals(segment2.getID()) && !segment1.prevSegment().getID().equals(segment2.getID()))
            throw new IllegalArgumentException("Input segments are not linked");

        // Ensure we have the segments in the correct order
        IBorderSegment firstSegment =  segment1.nextSegment().getID().equals(segment2.getID()) ? segment1 : segment2;
        IBorderSegment secondSegment = segment2.nextSegment().getID().equals(segment1.getID()) ? segment1 : segment2;

        // Create the new segment
        int startIndex = firstSegment.getStartIndex();
        int endIndex   = secondSegment.getEndIndex();
        IBorderSegment mergedSegment = IBorderSegment.newSegment(startIndex, endIndex, this.size(), id);

        mergedSegment.addMergeSource(firstSegment);
        mergedSegment.addMergeSource(secondSegment);

        LOGGER.fine("Merged segment has source 1: "+mergedSegment.hasMergeSource(seg1));
        LOGGER.fine("Merged segment has source 2: "+mergedSegment.hasMergeSource(seg2));
        // Replace the two segments in this profile
        List<IBorderSegment> oldSegs = this.getSegments();
        List<IBorderSegment> newSegs = new ArrayList<>();


        for (IBorderSegment oldSegment : oldSegs) {
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

        // This will remove merge sources of segment 0 if the start index is not already in the correct place 
        IBorderSegment.linkSegments(newSegs);
        
        this.setSegments(newSegs);
    }
    
    @Override
	public void mergeSegments(@NonNull IBorderSegment segment1, @NonNull IBorderSegment segment2, @NonNull UUID id) throws ProfileException {
		mergeSegments(segment1.getID(), segment2.getID(), id);
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

        if (!segment.hasMergeSources())
            return;

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

    @Override
    public boolean isSplittable(@NonNull UUID id, int splitIndex) {
        if (!this.hasSegment(id))
            return false;

        try {
        	IBorderSegment segment = getSegment(id);
        	
        	if (!segment.contains(splitIndex))
                return false;

            return IBorderSegment.isLongEnough(segment.getStartIndex(), splitIndex, segment.getProfileLength())
                    && IBorderSegment.isLongEnough(splitIndex, segment.getEndIndex(), segment.getProfileLength());
        } catch (UnavailableComponentException e) {
            LOGGER.log(Loggable.STACK, e.getMessage(), e);
            return false;
        }

    }

    @Override
    public void splitSegment(@NonNull IBorderSegment segment, int splitIndex, @NonNull UUID id1, @NonNull UUID id2) throws ProfileException {
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
        List<IBorderSegment> oldSegs = this.getSegments();
        List<IBorderSegment> newSegs = new ArrayList<IBorderSegment>();

        // Add the new segments to a list
        List<IBorderSegment> splitSegments = new ArrayList<IBorderSegment>();
        splitSegments
                .add(IBorderSegment.newSegment(segment.getStartIndex(), splitIndex, segment.getProfileLength(), id1));
        splitSegments.add(IBorderSegment.newSegment(splitIndex, segment.getEndIndex(), segment.getProfileLength(), id2));

        segment.addMergeSource(splitSegments.get(0));
        segment.addMergeSource(splitSegments.get(1));

        int position = 0;
        for (IBorderSegment oldSegment : oldSegs) {

            if (oldSegment.equals(segment)) {

                // add each of the old segments
                // for(IBorderSegment mergedSegment : splitSegments){
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Profile");
        for (IBorderSegment seg : this.segments) {
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
    	return new SegmentedFloatProfile(this);
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
        SegmentedFloatProfile other = (SegmentedFloatProfile) obj;
        if (segments == null) {
            if (other.segments != null)
                return false;
        } else if (!Arrays.equals(segments, other.segments))
            return false;
        return true;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        // finest("\tReading segmented profile");
        in.defaultReadObject();
        // finest("\tRead segmented profile");
        if (size() != segments[0].getProfileLength()) {
            LOGGER.warning("Error reading segments: " + " segment length " + segments[0].getProfileLength()
                    + " different to profile " + size());
        }
    }

}
