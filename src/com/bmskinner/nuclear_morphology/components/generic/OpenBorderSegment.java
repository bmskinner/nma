/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.components.generic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;

/**
 * A border segment without the length restrictions of the DefaultBorderSegment.
 * This allows segments smaller than the minimum to be created if needed. This class should
 * not be generally used.
 * @author bms41
 * @since 1.13.8
 *
 */
public class OpenBorderSegment implements IBorderSegment {

    private static final long serialVersionUID = 1L;

    private UUID uuid;

    private int startIndex, endIndex, totalLength;

    private short positionInProfile = 0; // for future refactor

    private IBorderSegment[] mergeSources = new IBorderSegment[0];

    /*
     * TRANSIENT FIELDS
     */

    private transient IBorderSegment prevSegment = null;
    private transient IBorderSegment nextSegment = null;
    private transient boolean        isLocked    = false;
                                                         

    /**
     * Construct with an existing UUID. This allows nucleus segments to directly
     * track median profile segments
     * 
     * @param startIndex the start index of the segment
     * @param endIndex the end index of the segment (inclusive)
     * @param total the length of the profile that generated the segment
     * @param id the id of the segment
     */
    public OpenBorderSegment(int startIndex, int endIndex, int total, UUID id) {

        if (id == null)
            throw new IllegalArgumentException("Segment ID cannot be null");

        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.totalLength = total;
        this.mergeSources = new IBorderSegment[0];
        this.uuid = id;
    }

    /**
     * Construct with a default random id
     * 
     * @param startIndex
     * @param endIndex
     * @param total
     */
    public OpenBorderSegment(int startIndex, int endIndex, int total) {
        this(startIndex, endIndex, total, java.util.UUID.randomUUID());
    }

    /**
     * Make a copy of the given segment, including the ID
     * 
     * @param n
     */
    public OpenBorderSegment(IBorderSegment n) {
        this.uuid = n.getID();
        this.startIndex = n.getStartIndex();
        this.endIndex = n.getEndIndex();
        this.totalLength = n.getProfileLength();
        this.nextSegment = n.nextSegment();
        this.prevSegment = n.prevSegment();

        if (n.hasMergeSources()) {
            List<IBorderSegment> otherSources = n.getMergeSources();
            this.mergeSources = new IBorderSegment[otherSources.size()];

            for (int i = 0; i < otherSources.size(); i++) {
                mergeSources[i] = otherSources.get(i);
            }

        } else {
            this.mergeSources = new IBorderSegment[0];
        }

        this.isLocked = n.isLocked();
    }
    
    @Override
	public IBorderSegment copy() {
		return new OpenBorderSegment(this);
	}

    /*
     * ---------------- Getters ----------------
     */

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#getID()
     */
    @Override
    public @NonNull UUID getID() {
        return this.uuid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#getMergeSources()
     */
    @Override
    public List<IBorderSegment> getMergeSources() {
        List<IBorderSegment> result = new ArrayList<IBorderSegment>();
        for (IBorderSegment seg : this.mergeSources) {
            result.add(new DefaultBorderSegment(seg));
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#addMergeSource(components.nuclear.
     * NucleusBorderSegment)
     */
    @Override
    public void addMergeSource(IBorderSegment seg) {

        if (seg == null)
            throw new IllegalArgumentException("Merge source segment is null");
        if(seg.getID().equals(IProfileCollection.DEFAULT_SEGMENT_ID)) // never replace or chain the default segment
			return;
		if(seg.getID().equals(getID()))
			throw new IllegalArgumentException(String.format("Cannot add merge source with same id as parent: %s", seg.getID()));
		if(getMergeSources().stream().anyMatch(s->s.getID().equals(seg.getID())))
			throw new IllegalArgumentException(String.format("Segment with id %s is already a merge source", seg.getID()));
        if (seg.getProfileLength() != totalLength)
            throw new IllegalArgumentException("Merge source length does not match");

        if (!this.contains(seg.getStartIndex()))
            throw new IllegalArgumentException("Start index of source is not in this segment");

        if (!this.contains(seg.getEndIndex()))
            throw new IllegalArgumentException("End index of source is not in this segment");

        mergeSources = Arrays.copyOf(mergeSources, mergeSources.length + 1);
        mergeSources[mergeSources.length - 1] = seg;
    }

    @Override
    public boolean hasMergeSources() {
        return mergeSources.length > 0;
    }
    
	@Override
	public boolean hasMergeSource(@NonNull UUID id) {
		for(IBorderSegment s : mergeSources) {
			if(s.getID().equals(id) || s.hasMergeSource(id))
				return true;
		}
		return false;
	}

	@Override
	public IBorderSegment getMergeSource(@NonNull UUID id) throws UnavailableComponentException {
		for(IBorderSegment s : mergeSources) {
			if(s.getID().equals(id))
				return s;
			if(s.hasMergeSource(id)) {
				return s.getMergeSource(id);
			}
		}
		throw new UnavailableComponentException(String.format("Segment %s is not present", id));
	}

    @Override
    public void clearMergeSources() {
        mergeSources = new IBorderSegment[0];
    }

    @Override
    public int getStartIndex() {
        return this.startIndex;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#getEndIndex()
     */
    @Override
    public int getEndIndex() {
        return this.endIndex;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#getProportionalIndex(double)
     */
    @Override
    public int getProportionalIndex(double d) {
    	if (d < 0 || d > 1)
			throw new IllegalArgumentException("Proportion must be between 0-1: " + d);
		double targetLength = length() * d;
		return (int) Math.round(CellularComponent.wrapIndex(startIndex+targetLength, getProfileLength()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#getIndexProportion(int)
     */
    @Override
    public double getIndexProportion(int index) {
        if (!this.contains(index)) {
            throw new IllegalArgumentException("Segment does not contain index " + index);
        }

        int counter = 0;
        Iterator<Integer> it = this.iterator();
        while (it.hasNext()) {
            int test = it.next();

            if (index == test) {
                return (double) ((double) counter / (double) this.length());
            }
            counter++;
        }

        throw new IllegalArgumentException("Cannot get proportion for " + index);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#getName()
     */
    @Override
    public String getName() {
        return "Seg_" + this.positionInProfile;
    }

    // when using this, use wrapIndex()!
    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#getMidpointIndex()
     */
    @Override
    public int getMidpointIndex() {
        if (this.wraps()) {

            int midLength = this.length() >> 1;
            if (midLength + startIndex < totalLength) {
                return midLength + startIndex;
            } else {
                return endIndex - midLength;
            }

        } else {
            return ((endIndex - startIndex) / 2) + startIndex;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#getDistanceToStart(int)
     */
    @Override
    public int getShortestDistanceToStart(int index) {
        if (index < 0 || index >= totalLength) {
            throw new IllegalArgumentException("Index is not in profile: " + index + "; total " + totalLength);
        }

        // Two possibilieites: abs distance or total - abs distance

        int abs = Math.abs(index - startIndex);
        int alt = totalLength - abs;

        return Math.min(abs, alt);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#getDistanceToEnd(int)
     */
    @Override
    public int getShortestDistanceToEnd(int index) {
        if (index < 0 || index >= totalLength) {
            throw new IllegalArgumentException("Index is not in profile: " + index + "; total " + totalLength);
        }

        int abs = Math.abs(index - endIndex);
        int alt = totalLength - abs;

        return Math.min(abs, alt);
    }
    
    @Override
	public int getInternalDistanceToStart(int index) {
		if(wraps() && startIndex>index)
			return index + (getProfileLength()-startIndex);
		return index-startIndex;
	}

	@Override
	public int getInternalDistanceToEnd(int index) {
		if(wraps() && getEndIndex()<index)
			return getEndIndex() + (getProfileLength()-index);
		return index-getEndIndex();
	}

    @Override
    public boolean isLocked() {
        return isLocked;
    }

    @Override
    public void setLocked(boolean startPositionLocked) {
        this.isLocked = startPositionLocked;
    }

    @Override
    public int getProfileLength() {
        return this.totalLength;
    }

    @Override
    public IBorderSegment nextSegment() {
        return this.nextSegment;
    }

    @Override
    public IBorderSegment prevSegment() {
        return this.prevSegment;
    }

    @Override
    public int length() {
        return testLength(this.getStartIndex(), this.getEndIndex());
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + uuid.hashCode();
        result = prime * result + endIndex;
        result = prime * result + startIndex;
        result = prime * result + totalLength;
        result = prime * result + mergeSources.hashCode();
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OpenBorderSegment other = (OpenBorderSegment) obj;
        if(!uuid.equals(other.uuid))
        	return false;
        if (endIndex != other.endIndex)
            return false;
        if (startIndex != other.startIndex)
            return false;
        if (totalLength != other.totalLength)
            return false;
        return true;
    }
    
    @Override
    public void offset(int offset) {
    	startIndex = CellularComponent.wrapIndex(startIndex+offset, totalLength);
    	endIndex  = CellularComponent.wrapIndex(endIndex+offset, totalLength);
    	for(IBorderSegment s : mergeSources) {
    		s.offset(offset);
    	}
    }


    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#testLength(int, int)
     */
    @Override
    public int testLength(int start, int end) {
    	if(wraps(start, end))
			return end+totalLength+1-start+1; // add total of 2; one for index 0 and one for segment end
		return end-start+1; // add one for segment end
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#wraps(int, int)
     */
    @Override
    public boolean wraps(int start, int end) {
        if ((start < 0 || start > totalLength) || (end < 0 || end > totalLength)) {
            throw new IllegalArgumentException("Index is outside profile bounds");
        }
        return (end <= start);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#wraps()
     */
    @Override
    public boolean wraps() {
        return wraps(this.getStartIndex(), this.getEndIndex());
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#contains(int)
     */
    @Override
    public boolean contains(int index) {
        return IBorderSegment.contains(startIndex, endIndex, index, totalLength);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#testContains(int, int, int)
     */
//    @Override
//    public boolean testContains(int start, int end, int index) {
//        if (index < 0 || index > totalLength) {
//            return false;
//        }
//
//        if (wraps(start, end)) { // wrapped
//            return (index <= end || index >= start);
//        } else { // regular
//            return (index >= start && index <= end);
//        }
//
//    }

    /**
     * Test if a proposed update affects this segment
     * 
     * @param startIndex
     * @param endIndex
     * @return
     */
    private boolean updateAffectsThisSegment(int startIndex, int endIndex) {
        return (startIndex != this.startIndex || endIndex != this.endIndex);
    }

    private boolean canUpdateSegment(int startIndex, int endIndex) throws SegmentUpdateException {
        if (this.isLocked) { // don't allow locked segments to update
            return false;
        }

        // only run an update and checks if the update will actually
        // cause changes to the segment. If not, return true so as not
        // to interfere with other linked segments
        if (!updateAffectsThisSegment(startIndex, endIndex)) {
            return true;
        }

        // Check that the new positions will not make this segment too small
        int testLength = testLength(startIndex, endIndex);
        if (testLength < MINIMUM_SEGMENT_LENGTH) {
            return false;
        }

        // Check that next and previous segments are not invalidated by length
        // change
        // i.e the max length increase backwards is up to the MIN_SEG_LENGTH of
        // the
        // previous segment, and the max length increase forwards is up to the
        // MIN_SEG_LENGTH of the next segment

        if (this.hasPrevSegment()) {
            int prevTestLength = prevSegment.testLength(prevSegment.getStartIndex(), startIndex);
            if (prevTestLength < MINIMUM_SEGMENT_LENGTH) {
                return false;
            }
        }
        if (this.hasNextSegment()) {
            int nextTestLength = nextSegment.testLength(endIndex, nextSegment.getEndIndex());
            if (nextTestLength < MINIMUM_SEGMENT_LENGTH) {
                return false;
            }
        }

        // check that updating will not cause segments to overlap or invert
        // i.e. where a start becomes greater than an end without begin part of
        // an array wrap
        if (startIndex > endIndex) {

            if (!IBorderSegment.contains(startIndex, endIndex, 0, totalLength)) {
                return false;
            }

        }

        // also test the effect on the next and previous segments
        if (this.hasPrevSegment()) {
        	
        	if( !contains(startIndex) && !prevSegment().contains(startIndex))
				throw new SegmentUpdateException(String.format("Neither this nor previous segment %s contain the new start index %d", prevSegment().getDetail(), startIndex));
	        
            if (this.prevSegment().getStartIndex() > startIndex) {

                if (!prevSegment.wraps() && prevSegment.wraps(startIndex, endIndex)) {
                    return false;
                }

                // another wrapping test - if the new positions induce a wrap,
                // the segment should contain 0
                if (prevSegment.wraps(startIndex, endIndex) && !IBorderSegment.contains(startIndex, endIndex, 0, totalLength)) {
                    return false;
                }
            }
        }

        if (this.hasNextSegment()) {
        	
        	if( !contains(endIndex) && !nextSegment().contains(endIndex))
	        	throw new SegmentUpdateException(String.format("Neither this nor next segment %s contain the new end index %d", nextSegment().getDetail(), endIndex));

            if (endIndex > nextSegment.getEndIndex()) {
            	

                // if the next segment goes from not wrapping to wrapping when
                // this segment is altered,
                // an inversion must have occurred. Prevent.
                if (!nextSegment.wraps() && nextSegment.wraps(startIndex, endIndex)) {
                    return false;
                }

                // another wrapping test - if the new positions induce a wrap,
                // the segment should contain 0
                if (nextSegment.wraps(startIndex, endIndex) && !IBorderSegment.contains(startIndex, endIndex, 0, totalLength)) {
                    return false;
                }
            }
        }
        return true;

    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#update(int, int)
     */
    @Override
    public boolean update(int startIndex, int endIndex) throws SegmentUpdateException {
        // Check the incoming data
        if (startIndex < 0 || startIndex > totalLength)
            throw new SegmentUpdateException("Start index is outside the profile range: " + startIndex);
        if (endIndex < 0 || endIndex > totalLength)
            throw new SegmentUpdateException("End index is outside the profile range: " + endIndex);

        if (!canUpdateSegment(startIndex, endIndex))
        	throw new SegmentUpdateException("Unable to update segment");

        // All checks have been passed; the update can proceed

        // Remove any merge sources - we cannot guarantee that these can be
        // maintained
        // over repeated updates
        mergeSources = new IBorderSegment[0];

        // wrap in if to ensure we don't go in circles forever when testing a
        // circular profile
        if (this.getStartIndex() != startIndex) { // becomes false after the
                                                  // first pass of the circle
            this.startIndex = startIndex;
            if (this.hasPrevSegment()) {
                prevSegment.update(prevSegment.getStartIndex(), startIndex);
            }
        }

        if (this.getEndIndex() != endIndex) {
            this.endIndex = endIndex;

            if (this.hasNextSegment()) {
                nextSegment.update(endIndex, nextSegment.getEndIndex());
            }
        }
        return true;

    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#setNextSegment(components.nuclear.
     * NucleusBorderSegment)
     */
    @Override
    public void setNextSegment(IBorderSegment s) {
        if (s == null) {
            throw new IllegalArgumentException("Segment cannot be null");
        }

        if (s.getProfileLength() != this.getProfileLength()) {
            throw new IllegalArgumentException("Segment has a different total length");
        }
        if (s.getStartIndex() != this.getEndIndex()) {
            throw new IllegalArgumentException("Segment start (" + s.getStartIndex()
                    + ") does not overlap the end of this segment: " + this.getEndIndex());
        }

        this.nextSegment = s;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#setPrevSegment(components.nuclear.
     * IBorderSegment)
     */
    @Override
    public void setPrevSegment(IBorderSegment s) {

        if (s == null) {
            throw new IllegalArgumentException("Segment cannot be null");
        }

        if (s.getProfileLength() != this.getProfileLength()) {
            throw new IllegalArgumentException("Segment has a different total length");
        }
        if (s.getEndIndex() != this.getStartIndex()) {
            throw new IllegalArgumentException(
                    "Segment end (" + s.getEndIndex() + ") does not overlap start: " + this.getStartIndex());
        }

        this.prevSegment = s;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#hasNextSegment()
     */
    @Override
    public boolean hasNextSegment() {
        if (this.nextSegment() != null) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#hasPrevSegment()
     */
    @Override
    public boolean hasPrevSegment() {
        if (this.prevSegment() != null) {
            return true;
        } else {
            return false;
        }
    }

    // public void setName(String s){
    // this.name = s;
    // }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#setPosition(int)
     */
    @Override
    public void setPosition(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Position must be a positve integer");
        }
        this.positionInProfile = (short) i;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#getPosition()
     */
    @Override
    public int getPosition() {
        return this.positionInProfile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderSegment#toString()
     */
    @Override
    public String toString() {

        return this.getName();
    }

    public String getDetail() {

        StringBuilder builder = new StringBuilder();

        builder.append("Segment ");
        builder.append(this.getName());
        builder.append(" | ");
        builder.append(this.getID());
        builder.append(" | ");
        builder.append(this.getPosition());
        builder.append(" | ");
        builder.append(this.startIndex);
        builder.append(" - ");
        builder.append(this.endIndex);
        builder.append(" | ");
        builder.append(this.length());
        builder.append(" of ");
        builder.append(this.getProfileLength() - 1);
        builder.append(" | ");
        builder.append(this.wraps());

        return builder.toString();
    }

    @Override
    public Iterator<Integer> iterator() {

        List<Integer> indexes = new ArrayList<Integer>();

        if (this.wraps()) {

            for (int i = this.getStartIndex(); i < this.getProfileLength(); i++) {
                indexes.add(i);
            }
            for (int i = 0; i <= this.getEndIndex(); i++) {
                indexes.add(i);
            }

        } else {

            for (int i = this.getStartIndex(); i <= this.getEndIndex(); i++) {
                indexes.add(i);
            }

        }

        return indexes.iterator();
    }
    
    @Override
    public boolean overlapsBeyondEndpoints(@NonNull IBorderSegment seg){
    	if(seg==null)
    		return false;
    	if(seg.getProfileLength()!=getProfileLength())
			return false;
    	
    	Iterator<Integer> it = this.iterator();
    	while(it.hasNext()) {
    		int index = it.next();
    		if(index==getStartIndex() || index==getEndIndex())
    			continue;
    		if(seg.contains(index))
    			return true;
    	}
    	return false;
    }
    
    @Override
    public boolean overlaps(@NonNull IBorderSegment seg){
    	if(seg==null)
    		return false;
    	if(seg.getProfileLength()!=getProfileLength())
			return false;
		return seg.contains(startIndex) 
				|| seg.contains(getEndIndex()) 
				|| contains(seg.getStartIndex()) 
				|| contains(seg.getEndIndex());
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

}
