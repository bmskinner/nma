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


package com.bmskinner.nuclear_morphology.components.nuclear;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Border segments mark a region with start and end positions within a component
 * border, and provide iterative access through the {@link IBorderPoint}s they
 * contain. It is possible for the start position to be higher than the end
 * position if the segment spans the end of the profile and wraps back to the
 * beginning.
 * <p>
 * The precise definition of indexes and lengths for a segment are as follows:
 * <p>
 * A profile has a length n:
 * <p>
 * <pre>0        9<br>----------</pre>
 * <p>
 * This is the number of unique indexes in the profile. In the profile above, there are
 * 10 unique indexes, and the profile length is thus 10.
 * 
 * A segment is a contiguous set of indices in a profile. Each segment has a start index 
 * and an end index. For example:
 * <p>
 * <pre>0  3   7 9<br>---|---|--<br>    s0    </pre>
 * <p>
 * In the profile above, a segment boundary has been drawn at index 3 and index 7.
 * This results in two segments. Segment s0 has a start index of 3, and an end index of 7.
 * The length of the segment is calculated at the number of profile indexes contained within
 * the segment. For s0, this is 3-4-5-6-7, for a length of 5.
 * <p>
 * A second feature of segments is that they can <i>wrap</i>. The profile has a second 
 * segment, s1.
 * <p>
 * <pre>0  3   7 9<br>---|---|--<br>s1  s0  s1</pre>
 * <p>
 * Segment s1 begins at the breakpoint at index 7, and continues to the end of the
 * profile. It then resumes at index 0 - wrapping around the end of the profile - 
 * and continues to index 3. It therefore has a start index of 7 and an end index 
 * of 3. The length of the segment is 7-8-9-0-1-2-3, for a length of 7.
 * <p>
 * Note that the total length of the segments in the profile is longer than the profile
 * itself. The profile with length 10 has segments with combined length 12 (5+7). The 
 * combined segment length is profileLength + nSegments. This is because adjacent segments 
 * must share at least one breakpoint index. When implementing this interface, it is 
 * important to bear this in mind, and <b>not</b> allow patterns such as:
 * <p>
 * <pre>0  34  789<br>---||--||-<br>s1   s0  s1</pre>
 * <p>
 * In the example above, segments have adjacent endpoints, not overlapping endpoints,
 * and segmentation in the program will not be calculated properly. 
 * <p>
 * @author ben
 * @since 1.13.3
 *
 */
public interface IBorderSegment extends Serializable, Iterable<Integer>, Loggable {

    /**
     * The smallest number of indexes in a segment. 
     * Setting greater than 1 to allow space for interpolation
     */
    static final int    MINIMUM_SEGMENT_LENGTH       = 3;
    /**
     * The minimum length that a segment can be interpolated to.
     */
    static final int    INTERPOLATION_MINIMUM_LENGTH = 2;
    
    /**
     * The name segments are prefixed with for display
     */
    static final String SEGMENT_PREFIX               = "Seg_";

    /**
     * Create the preferred segment type for this interface
     * 
     * @param startIndex the starting index of the segment in a profile, inclusive
     * @param endIndex the end index of the segment in a profile, inclusive
     * @param total the total length of the profile
     * @param id  the id of the segment
     * @return a new segment of the default type
     */
    static IBorderSegment newSegment(int startIndex, int endIndex, int total, @NonNull UUID id) {
        return new DefaultBorderSegment(startIndex, endIndex, total, id);
    }

    /**
     * Create the preferred segment type based on the given template
     * 
     * @param seg the template segment
     * @return a new segment
     */
    static IBorderSegment newSegment(@NonNull IBorderSegment seg) {
        return new DefaultBorderSegment(seg);
    }

    /**
     * Create the preferred segment type for this interface
     * 
     * @param startIndex the starting index of the segment in a profile, inclusive
     * @param endIndex the end index of the segment in a profile, inclusive
     * @param total the total length of the profile
     * @return a new segment
     */
    static IBorderSegment newSegment(int startIndex, int endIndex, int total) {
        return IBorderSegment.newSegment(startIndex, endIndex, total, java.util.UUID.randomUUID());
    }

    /**
     * Get the segment ID
     * @return the id
     */
    @NonNull UUID getID();

    /**
     * Get a copy of the merge source segments
     * 
     * @return
     */
    List<IBorderSegment> getMergeSources();

    /**
     * Add the given segment as a merge source
     * 
     * @param seg
     */
    void addMergeSource(@NonNull IBorderSegment seg);

    /**
     * Remove all merge sources from the segment
     */
    void clearMergeSources();

    /**
     * Test if this segment is a merge of other segments
     * 
     * @return
     */
    boolean hasMergeSources();
    
    /**
     * Test if this segment contains the given segment
     * 
     * @return true if a segment with the given id is present within this segmnet (recursive)
     */
    boolean hasMergeSource(@NonNull UUID id);
    
    /**
     * Get the merge source with the given id
     * 
     * @return the merge source segment with the given id if present within this segment (recursive)
     * @throws UnavailableComponentException if the segment does not have the requested source 
     */
    IBorderSegment getMergeSource(@NonNull UUID id) throws UnavailableComponentException;

    /**
     * Get the index at the start of the segment
     * 
     * @return
     */
    int getStartIndex();

    /**
     * Get the index at the end of the segment
     * 
     * @return
     */
    int getEndIndex();

    /**
     * Get the index closest to the fraction of the way through the segment.
     * For example in the segment below, requesting proportion 0.5 will return index 3:
     * <pre>0 0.5 1<br>|-----|<br>0  3  6</pre>
     * 
     * @param d a fraction between 0 (start) and 1 (end)
     * @return the nearest index, or -1 on error
     */
    int getProportionalIndex(double d);

    /**
     * Get the proportion of the given index along the segment from zero to one.
     * For example in the segment below, requesting index 3 will return 0.5:
     * <pre>0  3  6<br>|-----|<br>0 0.5 1</pre>
     * 
     * @param index the index
     * @return
     * @throws IllegalArgumentException if the index is not within the segment
     */
    double getIndexProportion(int index);

    /**
     * Get the name of the segment in the form "Seg_n" where n is the position
     * in the profile
     * 
     * @return
     */
    String getName();

    
    /**
     * Gets the index closest to the middle of the segment.
     * If the segment length is odd, this will be the exact midpoint.
     * If the segment length is even, this will be the lower of the two possible
     * midpoint indexes. For example:
     * <p>
     * <pre>Length 8   Length 7<br>0      7   0     6<br>|------|   |-----|<br>   4          3</pre>
     * @return
     */
    int getMidpointIndex();

    /**
     * Get the shortest distance of the given index to the start of the segment.
     * Since profiles wrap, this may return the distance within the segment, or it may 
     * return the distance passing outside the segment.
     * 
     * @param index
     * @return
     */
    int getShortestDistanceToStart(int index);
    
    /**
     * Get the distance within the segment of the given index to the start of the segment
     * 
     * @param index
     * @return
     */
    int getInternalDistanceToStart(int index);

    /**
     * Get the shortest distance of the given index to the end of the segment. 
     * Since profiles wrap, this may return the distance within the segment, or it may 
     * return the distance passing outside the segment.
     * 
     * @param index
     * @return
     */
    int getShortestDistanceToEnd(int index);
    
    /**
     * Get the distance within the segment of the given index to the end of the segment
     * 
     * @param index
     * @return
     */
    int getInternalDistanceToEnd(int index);

    /**
     * Test if the segment is locked from editing
     * 
     * @return
     */
    boolean isLocked();

    /**
     * Set the segment editing lock state
     * 
     * @param b
     */
    void setLocked(boolean b);

    /**
     * get the total length of the profile that this segment is a part of
     * 
     * @return
     */
    int getProfileLength();

    /**
     * Get the next segment in the profile. That is, the segment whose start index is the end
     * index of this segment
     * 
     * @return the segment
     */
    IBorderSegment nextSegment();

    /**
     * Get the previous segment in the profile. That is, the segment whose end index is the
     * start index of this segment
     * 
     * @return the segment
     */
    IBorderSegment prevSegment();

    /**
     * Get the length of this segment. This is the number of profile indexes present
     * within the segment. For example the following segment:
     * <pre>   4      11   <br>---|######|---<br></pre>
     * wiil have a length of 8: 4-5-6-7-8-9-10-11
     * 
     * @return the number of profile indexes in the segment
     */
    int length();
    
    
    /**
     * Offset the segment and its merge sources by the given amount
     * @param offset
     */
    void offset(int offset);


    /**
     * Test the effect of new start and end indexes on the length of the
     * segment. Use for validating updates.
     * 
     * @param start the new start index
     * @param end the new end index
     * @return the new segment length
     */
    int testLength(int start, int end);

    /**
     * Check if the segment would wrap with the given start and end points (i.e
     * contains 0)
     * 
     * @param start the start index
     * @param endthe end index
     * @return
     */
    default boolean wraps(int start, int end) {
    	return end<=start;
    }

    /**
     * Test if the segment wraps (i.e contains index 0 as anything other than a start index).
     * For example:
     * <p>
     * <pre>    4      11   <br>####|------|###<br> s1         s1</pre>
     * The segment s1 above starts at 11 and ends at 4. It wraps because it contains 0.
     * <p>
     * <pre>0              <br>|###########<br>      s1     </pre>
     * The segment s1 above starts at 0 and ends at 0. It wraps because the start and end points are the same.
     * <p>
     * <pre>0   4          <br>|###|---------<br> s1     </pre>
     * The segment s1 above starts at 0 and ends at 4. It does not wrap because it only has a start point of 0.
     * 
     * 
     * @return
     */
    boolean wraps();

    /**
     * Test if the segment contains the given index
     * 
     * @param index the index to test
     * @return
     */
    boolean contains(int index);

    /**
     * Update the segment to the given position. Also updates the previous and
     * next segments.
     * 
     * @param startIndex the new start index
     * @param endIndex the new end index
     * @throws SegmentUpdateException if the update cannot proceed. The exception message gives detail on why the update failed
     */
    boolean update(int startIndex, int endIndex) throws SegmentUpdateException;

    /**
     * Set the next segment in the profile. The segment must have a
     * start index that is equal to the end index of this segment.
     * 
     * @param s
     * @throws IllegalArgumentException if the segment start does not overlap this segment end
     */
    void setNextSegment(@NonNull IBorderSegment s);

    /**
     * Set the previous segment in the profile. The segment must have an
     * end index that is equal to the start index of this segment.
     * 
     * @param s
     * @throws IllegalArgumentException if the segment end does not overlap this segment start
     */
    void setPrevSegment(@NonNull IBorderSegment s);

    /**
     * Check if a next segment has been defined
     * 
     * @return
     */
    boolean hasNextSegment();

    /**
     * Check if a previous segment has been defined
     * 
     * @return
     */
    boolean hasPrevSegment();

    /**
     * Set the position in the segmented profile. Should be clockwise from the
     * reference point.
     * 
     * @param i
     */
    void setPosition(int i);

    /**
     * Get the position in the segmented profile. Should be clockwise from the
     * reference point.
     */
    int getPosition();

    Iterator<Integer> iterator();
  
    /**
     * Get the full description of the segment
     * @return
     */
    String getDetail();
    
    /**
     * Test if the given segment overlaps this segment.
     * Formally, returns true if any indexes are shared in the
     * segments except for the start and end indexes
     * @param seg
     * @return true if an index other than the start and end index is shared
     */
    boolean overlaps(@NonNull IBorderSegment seg);
    
    /**
     * Given a list of segments, link them together into a circle.
     * 
     * @param list the segments to link
     * @throws ProfileException if updating the first segment indexes fails
     */
    static void linkSegments(@NonNull IBorderSegment[] list) throws ProfileException {
        if (list == null)
            throw new IllegalArgumentException("List of segments is null");

        if (list.length < 2)
            throw new IllegalArgumentException(String.format("Must have at least two segments (have %d)",list.length));

        for (int i = 0; i < list.length; i++) {
            IBorderSegment s = list[i];
            // Wrap indices
            int p = i == 0 ? list.length - 1 : i - 1;
            int n = i == list.length - 1 ? 0 : i + 1;

            if (i == 0) {

                boolean lockState = s.isLocked();
                s.setLocked(false);
                try {

                    s.update(list[p].getEndIndex(), s.getEndIndex());

                } catch (IllegalArgumentException | SegmentUpdateException e) {
                    throw new ProfileException("Error linking final segment: " + e.getMessage());
                }
                s.setLocked(lockState);
            }

            s.setPrevSegment(list[p]);
            s.setNextSegment(list[n]);
            s.setPosition(i);

        }
    }

    /**
     * Given a list of segments, link them together into a circle. Links start
     * and end properly. Does not copy the segments.
     * 
     * @param list
     * @throws ProfileException
     */
    static void linkSegments(@NonNull List<IBorderSegment> list) throws ProfileException {
//    	Logger.getLogger(PROGRAM_LOGGER).fine("Linking list of "+list.size()+" segments");
        for (int i = 0; i < list.size(); i++) {
            IBorderSegment s = list.get(i);
            // Wrap indices
            int p = i == 0 ? list.size() - 1 : i - 1;
            int n = i == list.size() - 1 ? 0 : i + 1;

            if (i == 0) { // first segment

                boolean lockState = list.get(0).isLocked();
                s.setLocked(false);
                try {
                    s.update(list.get(p).getEndIndex(), s.getEndIndex());
                } catch (IllegalArgumentException | SegmentUpdateException e) {
                    throw new ProfileException("Error linking final segment: " + e.getMessage(), e);
                }
                s.setLocked(lockState);
            }

            s.setPrevSegment(list.get(p));
            s.setNextSegment(list.get(n));
            s.setPosition(i);

        }
    }

    /**
     * Scale the segments in the list to a new total length, preserving segment
     * fractional lengths as best as possible
     * 
     * @param list the segments
     * @param newLength the new length
     * @return
     */
    static List<IBorderSegment> scaleSegments(@NonNull List<IBorderSegment> list, int newLength) throws ProfileException {
    	List<IBorderSegment> result = new ArrayList<>();
    	
    	if(list.size()==1) {
    		// only a single segment; this must be from the new SegmentedCellularComponent added
    		// in 1.14.0. The single segment spans the entire profile, so just update the profile length
    		// and return.
    		IBorderSegment oldSeg = list.get(0);
    		IBorderSegment newSeg = IBorderSegment.newSegment(oldSeg.getStartIndex(), oldSeg.getStartIndex(), newLength, oldSeg.getID());
    		result.add(newSeg);
    		linkSegments(result);
    		return result;
    	}

        int segStart = list.get(0).getStartIndex();
        double segStartProportion = (double) segStart / (double) list.get(0).getProfileLength();
        
        segStart = (int) (((double) segStart) * segStartProportion);
        
        for (IBorderSegment segment : list) {
        	
            double proportion = (double) segment.length() / (double) segment.getProfileLength();

            int newSegLength = (int) ((double) newLength * proportion);

            int segEnd = CellularComponent.wrapIndex(segStart + newSegLength, newLength);

            IBorderSegment newSeg = IBorderSegment.newSegment(segStart, segEnd, newLength, segment.getID());

            segStart = segEnd;

            result.add(newSeg);
        }

        linkSegments(result);
        return result;
    }

    /**
     * Make a copy of the given list of linked segments, and link the new
     * segments
     * 
     * @param list the segments to copy
     * @return a new list
     * @throws Exception
     */
    static List<IBorderSegment> copy(@NonNull List<IBorderSegment> list) throws ProfileException {
    	if (list == null || list.isEmpty())
            throw new IllegalArgumentException("Cannot copy segments: segment list is null or empty");
        List<IBorderSegment> result = copyWithoutLinking(list);
        linkSegments(result);
        return result;
    }

    
    static IBorderSegment[] copy(@NonNull IBorderSegment[] segments) throws ProfileException {
    	if (segments == null || segments.length == 0)
            throw new IllegalArgumentException("Cannot copy segments: segment list is null or empty");
    	IBorderSegment[] result = copyWithoutLinking(segments);
        linkSegments(result);
        return result;
    }
    
    /**
     * Make a copy of the given list of linked segments, but do not link the
     * segments
     * 
     * @param list the segments to copy
     * @return a new list
     * @throws Exception
     */
    static List<IBorderSegment> copyWithoutLinking(@NonNull List<IBorderSegment> list) throws ProfileException {
    	
        if (list == null || list.isEmpty())
            throw new IllegalArgumentException("Cannot copy segments: segment list is null or empty");
        
        List<IBorderSegment> result = new ArrayList<>();
        for (IBorderSegment segment : list) {

            result.add(IBorderSegment.newSegment(segment));
        }
        return result;
    }

    /**
     * Make a copy of the given list of segments, but do not link the segments
     * 
     * @param list
     * @return
     * @throws ProfileException
     */
    static IBorderSegment[] copyWithoutLinking(@NonNull IBorderSegment[] list) throws ProfileException {
        if (list == null || list.length == 0) {
            throw new IllegalArgumentException("Cannot copy segments: segment list is null or empty");
        }
        return Arrays.copyOf(list, list.length);

    }

    /**
     * Given a list of ordered segments, fetch the segment with the given name
     * 
     * @param list
     *            the linked order list
     * @param segName
     *            the segment name to find ('Seg_n')
     * @return the segment or null
     * @throws Exception
     */
    static IBorderSegment getSegment(@NonNull List<IBorderSegment> list, String segName) {

        for (IBorderSegment segment : list) {

            if (segment.getName().equals(segName)) {
                return segment;
            }
        }
        return null;
    }

    /**
     * Test if the regular median profiles of the given datasets have the same
     * segment counts
     * 
     * @param list
     * @return
     */
    static boolean segmentCountsMatch(@NonNull List<IAnalysisDataset> list) {

        int segCount = list.get(0).getCollection().getProfileManager().getSegmentCount();
        for (IAnalysisDataset d : list) {
            if (d.getCollection().getProfileManager().getSegmentCount() != segCount) {
                return false;
            }
        }

        return true;
    }

    static String toString(@NonNull List<IBorderSegment> list) {
        StringBuilder builder = new StringBuilder();
        builder.append("List of segments:\n");
        for (IBorderSegment segment : list) {

            builder.append("\t" + segment.toString() + "\n");
        }
        return builder.toString();
    }
    
    static int calculateSegmentLength(int start, int end, int total){
        return start < end ? end - start : end + (total - start);
    }

    /**
     * Test the length for new segments
     * 
     * @param start the segment start index
     * @param end the segment end index
     * @param total the profile length
     * @return true if the distance between start and end indexes is longer than {@link IBorderSegment.MINIMUM_SEGMENT_LENGTH}
     */
    static boolean isLongEnough(int start, int end, int total) {
        int testLength = calculateSegmentLength(start, end, total);
        return testLength >= MINIMUM_SEGMENT_LENGTH;
    }
    
    /**
     * Test the length for new segments. Can a second segment be added to the profile?
     * 
     * @param start the segment start index
     * @param end the segment end index
     * @param total the profile length
     * @return true if the total distance minus the distance between start and end indexes is longer than {@link IBorderSegment.MINIMUM_SEGMENT_LENGTH}
     */
    static boolean isShortEnough(int start, int end, int total) {
        int testLength = calculateSegmentLength(start, end, total);
        return total-testLength >= MINIMUM_SEGMENT_LENGTH;
    }
    
    /**
     * Test if a segment would contain the given index if it had the specified
     * start and end indexes.
     * 
     * @param start the start to test
     * @param end the end to test
     * @param index the index to test
     * @return
     */
    static boolean contains(int start, int end, int index, int total){
        if (index < 0 || index > total)
            return false;

        if (end<start)// wrapped
            return (index <= end || index >= start);
//      regular
        return (index >= start && index <= end);
    }

    /**
     * Thrown when a segment update attempt fails
     * 
     * @author bms41
     * @since 1.13.4
     *
     */
    public class SegmentUpdateException extends Exception {
        private static final long serialVersionUID = 1L;

        public SegmentUpdateException() {
            super();
        }

        public SegmentUpdateException(String message) {
            super(message);
        }

        public SegmentUpdateException(String message, Throwable cause) {
            super(message, cause);
        }

        public SegmentUpdateException(Throwable cause) {
            super(cause);
        }

    }


}
