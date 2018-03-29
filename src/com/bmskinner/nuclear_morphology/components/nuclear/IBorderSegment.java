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

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Border segments mark a region with start and end positions within a component
 * border, and provide iterative access through the {@link IBorderPoint}s they
 * contain. It is possible for the start position to be higher than the end
 * position if the segment spans the end of the profile and wraps back to the
 * beginning.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public interface IBorderSegment extends Serializable, Iterable<Integer>, Loggable {

    /**
     * The smallest number of indexes in a segment. 
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
    static IBorderSegment newSegment(int startIndex, int endIndex, int total, UUID id) {
        return new DefaultBorderSegment(startIndex, endIndex, total, id);
    }

    /**
     * Create the preferred segment type based on the given template
     * 
     * @param seg the template segment
     * @return a new segment
     */
    static IBorderSegment newSegment(IBorderSegment seg) {
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
    UUID getID();

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
    void addMergeSource(IBorderSegment seg);

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

    // String getLastFailReason();

    // void setLastFailReason(String reason);

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
     * Get the index closest to the fraction of the way through the segment
     * 
     * @param d
     *            a fraction between 0 (start) and 1 (end)
     * @return the nearest index, or -1 on error
     */
    int getProportionalIndex(double d);

    /**
     * Get the proportion of the given index along the segment from zero to one.
     * Returns -1 if the index was not found
     * 
     * @param index
     *            the index to test
     * @return
     */
    double getIndexProportion(int index);

    /**
     * Get the name of the segment in the form "Seg_n" where n is the position
     * in the profile
     * 
     * @return
     */
    String getName();

    // when using this, use wrapIndex()!
    int getMidpointIndex();

    /**
     * Get the shortest distance of the given index to the start of the segment
     * 
     * @param index
     * @return
     */
    int getDistanceToStart(int index);

    /**
     * Get the shortest distance of the given index to the end of the segment
     * 
     * @param index
     * @return
     */
    int getDistanceToEnd(int index);

    /**
     * Test if the segment is locked from editing
     * 
     * @return
     */
    boolean isLocked();

    /**
     * Set the segment lock state
     * 
     * @param b
     */
    void setLocked(boolean b);

    /**
     * get the total length of the profile that this segment is a part of
     * 
     * @return
     */
    int getTotalLength();

    /**
     * Get the next segment. That is, the segment whose start index is the end
     * index of this segment
     * 
     * @return the segment
     */
    IBorderSegment nextSegment();

    /**
     * Get the previous segment. That is, the segment whose end index is the
     * start index of this segment
     * 
     * @return the segment
     */
    IBorderSegment prevSegment();

    // /**
    // * Make this segment shorter by the given amount.
    // * The start index is moved forward. The previous segment
    // * is adjusted to keep the segments in sync
    // * @param value the amount to shorten
    // */
    // boolean shortenStart(int value);
    //
    // /**
    // * Make this segment shorter by the given amount.
    // * The end index is moved back. The next segment
    // * is adjusted to keep the segments in sync
    // * @param value the amount to shorten
    // */
    // boolean shortenEnd(int value);

    // /**
    // * Make this segment longer by the given amount.
    // * The start index is moved back. The previous segment
    // * is adjusted to keep the segments in sync
    // * @param value the amount to shorten
    // */
    // boolean lengthenStart(int value);
    //
    // /**
    // * Make this segment longer by the given amount.
    // * The end index is moved forward. The previous segment
    // * is adjusted to keep the segments in sync
    // * @param value the amount to shorten
    // */
    // boolean lengthenEnd(int value);

    /**
     * Get the length of this segment. Accounts for array wrapping
     * 
     * @return
     */
    int length();

    int hashCode();

    boolean equals(Object obj);

    /**
     * Test the effect of new start and end indexes on the length of the
     * segment. Use for validating updates. Also called by length() using real
     * values
     * 
     * @param start
     *            the new start index
     * @param end
     *            the new end index
     * @return the new segment length
     */
    int testLength(int start, int end);

    /**
     * Check if the segment would wrap with the given start and end points (i.e
     * contains 0)
     * 
     * @param start
     *            the start index
     * @param end
     *            the end index
     * @return
     */
    boolean wraps(int start, int end);

    /**
     * Test if the segment currently wraps
     * 
     * @return
     */
    boolean wraps();

    /**
     * Test if the segment contains the given index
     * 
     * @param index
     *            the index to test
     * @return
     */
    boolean contains(int index);

    /**
     * Test if the segment would contain the given index if it had the specified
     * start and end indexes. Acts as a wrapper for the real contains()
     * 
     * @param start
     *            the start to test
     * @param end
     *            the end to test
     * @param index
     *            the index to test
     * @return
     */
    boolean testContains(int start, int end, int index);

    /**
     * Update the segment to the given position. Also updates the previous and
     * next segments. Error if the values cause any segment to become negative
     * length
     * 
     * @param start
     *            the new start index
     * @param end
     *            the new end index
     */
    boolean update(int startIndex, int endIndex) throws SegmentUpdateException;

    /**
     * Set the next segment in the profile from this
     * 
     * @param s
     */
    void setNextSegment(IBorderSegment s);

    /**
     * Set the previous segment in the profile from this
     * 
     * @param s
     */
    void setPrevSegment(IBorderSegment s);

    /**
     * Check if a next segment has been added
     * 
     * @return
     */
    boolean hasNextSegment();

    /**
     * Check if a previous segment has been added
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
     * @param list
     *            the segments to link
     * @throws ProfileException
     *             if updating the first segment indexes fails
     */
    static void linkSegments(IBorderSegment[] list) throws ProfileException {
        if (list == null) {
            throw new IllegalArgumentException("List of segments is null");
        }

        if (list.length < 2) {
            throw new IllegalArgumentException("Must have at least two segments (have " + list.length + ")");
        }

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
     * @throws Exception
     */
    static void linkSegments(List<IBorderSegment> list) throws ProfileException {
        if (list == null) {
            throw new IllegalArgumentException("List of segments is null");
        }

        if (list.size() < 2) {
            throw new IllegalArgumentException("Must have at least two segments (have " + list.size() + ")");
        }

        for (int i = 0; i < list.size(); i++) {
            IBorderSegment s = list.get(i);
            // Wrap indices
            int p = i == 0 ? list.size() - 1 : i - 1;
            int n = i == list.size() - 1 ? 0 : i + 1;

            if (i == 0) {

                boolean lockState = list.get(0).isLocked();
                s.setLocked(false);
                try {

                    s.update(list.get(p).getEndIndex(), s.getEndIndex());

                } catch (IllegalArgumentException | SegmentUpdateException e) {
                    throw new ProfileException("Error linking final segment: " + e.getMessage());
                }
                s.setLocked(lockState);
            }

            s.setPrevSegment(list.get(p));
            s.setNextSegment(list.get(n));
            s.setPosition(i);

        }
    }

    /**
     * Nudge segments that are not linked together into a complete profile. Used
     * in merging and unmerging segments recursively.
     * 
     * @param list
     * @param value
     * @return
     * @throws Exception
     */
    static List<IBorderSegment> nudgeUnlinked(List<IBorderSegment> list, int value) {

        if (list == null) {
            throw new IllegalArgumentException("Input list cannot be null");
        }

        List<IBorderSegment> result = new ArrayList<IBorderSegment>(list.size());

        for (IBorderSegment segment : list) {

            IBorderSegment newSeg = IBorderSegment.newSegment(
                    CellularComponent.wrapIndex(segment.getStartIndex() + value, segment.getTotalLength()),
                    CellularComponent.wrapIndex(segment.getEndIndex() + value, segment.getTotalLength()),
                    segment.getTotalLength(), segment.getID());

            // adjust merge sources also and readd
            if (segment.hasMergeSources()) {

                List<IBorderSegment> adjustedMergeSources = nudgeUnlinked(segment.getMergeSources(), value);
                for (IBorderSegment newMergeSource : adjustedMergeSources) {
                    newSeg.addMergeSource(newMergeSource);
                }

            }

            result.add(newSeg);
        }
        return result;
    }

    static List<IBorderSegment> nudge(IBorderSegment[] list, int value) throws ProfileException {
        return nudge(Arrays.asList(list), value);
    }

    /**
     * Move the segments by the given amount along the profile, without
     * shrinking them.
     * 
     * @param list
     *            the list of segments
     * @param value
     *            the amount to nudge
     * @return a new list of segments
     * @throws ProfileException
     */
    static List<IBorderSegment> nudge(List<IBorderSegment> list, int value) throws ProfileException {

        List<IBorderSegment> result = new ArrayList<IBorderSegment>(list.size());

        for (IBorderSegment segment : list) {

            int toWrap = segment.getStartIndex() + value;

            int newStart = CellularComponent.wrapIndex(toWrap, segment.getTotalLength());

            int newEnd = CellularComponent.wrapIndex(segment.getEndIndex() + value, segment.getTotalLength());

            if (newStart < 0 || newStart >= segment.getTotalLength()) {
                throw new ProfileException("Index wrapping failed for segment: Index " + segment.getStartIndex()
                        + " wrapped to " + newStart + " given total length " + segment.getTotalLength()
                        + " an offset value of " + value + " and the input index to wrap was " + toWrap);
            }

            IBorderSegment newSeg = IBorderSegment.newSegment(newStart, newEnd, segment.getTotalLength(),
                    segment.getID());

            newSeg.setPosition(segment.getPosition());

            // adjust merge sources also and read
            if (segment.hasMergeSources()) {

                //
                List<IBorderSegment> adjustedMergeSources = nudgeUnlinked(segment.getMergeSources(), value);
                for (IBorderSegment newMergeSource : adjustedMergeSources) {
                    newSeg.addMergeSource(newMergeSource);
                }
            }

            result.add(newSeg);
        }

        linkSegments(result);

        return result;
    }

    /**
     * Scale the segments in the list to a new total length, preserving segment
     * fractional lengths as best as possible
     * 
     * @param list
     *            the segments
     * @param newLength
     *            the new length
     * @return
     */
    static List<IBorderSegment> scaleSegments(List<IBorderSegment> list, int newLength) throws ProfileException {
        List<IBorderSegment> result = new ArrayList<>();

        int segStart = list.get(0).getStartIndex();
        double segStartProportion = (double) segStart / (double) list.get(0).getTotalLength();
        
        segStart = (int) (((double) segStart) * segStartProportion);
        
        for (IBorderSegment segment : list) {
        	
            double proportion = (double) segment.length() / (double) segment.getTotalLength();

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
     * @param list
     *            the segments to copy
     * @return a new list
     * @throws Exception
     */
    static List<IBorderSegment> copy(List<IBorderSegment> list) throws ProfileException {

        List<IBorderSegment> result = copyWithoutLinking(list);

        linkSegments(result);
        return result;
    }

    /**
     * Make a copy of the given list of linked segments, but do not link the
     * segments
     * 
     * @param list
     *            the segments to copy
     * @return a new list
     * @throws Exception
     */
    static List<IBorderSegment> copyWithoutLinking(List<IBorderSegment> list) throws ProfileException {

        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("Cannot copy segments: segment list is null or empty");
        }

        List<IBorderSegment> result = new ArrayList<IBorderSegment>();

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
    static IBorderSegment[] copyWithoutLinking(IBorderSegment[] list) throws ProfileException {
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
    static IBorderSegment getSegment(List<IBorderSegment> list, String segName) {

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
    static boolean segmentCountsMatch(List<IAnalysisDataset> list) {

        int segCount = list.get(0).getCollection().getProfileManager().getSegmentCount();
        for (IAnalysisDataset d : list) {
            if (d.getCollection().getProfileManager().getSegmentCount() != segCount) {
                return false;
            }
        }

        return true;
    }

    static String toString(List<IBorderSegment> list) {
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
