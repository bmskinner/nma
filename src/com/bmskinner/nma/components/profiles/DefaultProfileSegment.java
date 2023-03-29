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
/*
  -----------------------
  NUCLEUS BORDER SEGMENT
  -----------------------
  A segment is made of multiple NucleusBorderPoints.
  A Nucleus can contain many segments, which may overlap.
  The NucleusBorderPoints are not stored with a segment;
  the segment merely provides a way to interact with the points
  as a group.
*/
package com.bmskinner.nma.components.profiles;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.cells.CellularComponent;

/**
 * The default implementation of the {@link IProfileSegment} interface.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultProfileSegment implements IProfileSegment {

	private static final Logger LOGGER = Logger.getLogger(DefaultProfileSegment.class.getName());

	private final UUID uuid;

	private int startIndex, endIndex, totalLength; // the start and end indexes inclusive

	@Deprecated
	private short positionInProfile = 0; // for future refactor

	/**
	 * Merge sources within this segment. Only two merge sources can be present per
	 * segment
	 */
	private IProfileSegment mergeSourceA = null;
	private IProfileSegment mergeSourceB = null;

	/** The previous segment in the profile if segments are linked */
	private IProfileSegment prevSegment = null;

	/** The next segment in the profile if segments are linked */
	private IProfileSegment nextSegment = null;

	/** Allow the start index to be fixed */
	private boolean isLocked = false;

	/**
	 * Construct with an existing UUID. This allows nucleus segments to directly
	 * track median profile segments
	 * 
	 * @param startIndex the start index of the segment
	 * @param endIndex   the end index of the segment (inclusive)
	 * @param total      the length of the profile that generated the segment
	 * @param id         the id of the segment
	 */
	public DefaultProfileSegment(int startIndex, int endIndex, int total, @NonNull UUID id) {
		if (IProfileCollection.DEFAULT_SEGMENT_ID.equals(id) && startIndex != endIndex)
			throw new IllegalArgumentException(String.format(
					"Cannot make default segment %s-%s; it would be shorter than the entire profile",
					startIndex, endIndex));

		if (startIndex < 0 || endIndex < 0)
			throw new IllegalArgumentException("Segment start and end indexes cannot be negative");

		if (startIndex > total || endIndex > total)
			throw new IllegalArgumentException("Segment start and end indexes (" + startIndex
					+ " and " + endIndex + ") cannot be above total length (" + total + ")");

		// ensure that the segment meets minimum length requirements
		if (!IProfileSegment.isLongEnough(startIndex, endIndex, total))
			throw new IllegalArgumentException(
					String.format(
							"Cannot create default segment of length %s: shorter than minimum (%s)",
							total, MINIMUM_SEGMENT_LENGTH));

		if (startIndex != endIndex && !IProfileSegment.isShortEnough(startIndex, endIndex, total))
			throw new IllegalArgumentException(
					String.format("Segment is too long for the profile: %s - %s of %s", startIndex,
							endIndex, total));

		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.totalLength = total;
		this.uuid = id;
	}

	@Override
	public Element toXmlElement() {
		Element e = new Element("Segment")
				.setAttribute("id", uuid.toString())
				.setAttribute("start", String.valueOf(startIndex))
				.setAttribute("end", String.valueOf(endIndex))
				.setAttribute("total", String.valueOf(totalLength));

		if (isLocked)
			e.setAttribute("lock", "true"); // don't waste space on unlocked segments

		if (mergeSourceA != null) {
			e.addContent(new Element("MergeSource").setContent(mergeSourceA.toXmlElement()));
			e.addContent(new Element("MergeSource").setContent(mergeSourceB.toXmlElement()));
		}
		return e;
	}

	public DefaultProfileSegment(Element e) {

		try {
			uuid = UUID.fromString(e.getAttributeValue("id"));
			startIndex = Integer.parseInt(e.getAttributeValue("start"));
			endIndex = Integer.parseInt(e.getAttributeValue("end"));
			totalLength = Integer.parseInt(e.getAttributeValue("total"));

			isLocked = e.getAttributeValue("lock") != null;

			List<Element> merges = e.getChildren("MergeSource");
			if (!merges.isEmpty()) {
				mergeSourceA = new DefaultProfileSegment(merges.get(0).getChild("Segment"));
				mergeSourceB = new DefaultProfileSegment(merges.get(1).getChild("Segment"));
			}
		} catch (NumberFormatException | NullPointerException e1) {
			LOGGER.warning("Unable to parse segment XML: " + e1.getMessage());
			LOGGER.fine(e.toString());
			throw e1;
		}

		if (totalLength <= 0)
			throw new IllegalArgumentException(
					"XML parsing issue: segment from profile of length zero. Check the XML is valid.");

	}

	/**
	 * Construct with a default random id
	 * 
	 * @param startIndex the start index of the segment
	 * @param endIndex   the end index of the segment (inclusive)
	 * @param total      the length of the profile that generated the segment
	 */
	public DefaultProfileSegment(int startIndex, int endIndex, int total) {
		this(startIndex, endIndex, total, UUID.randomUUID());
	}

	/**
	 * Make a copy of the given segment, including the ID
	 * 
	 * @param n
	 */
	public DefaultProfileSegment(@NonNull IProfileSegment n) {
		this.uuid = n.getID();
		this.startIndex = n.getStartIndex();
		this.endIndex = n.getEndIndex();
		this.totalLength = n.getProfileLength();
		this.nextSegment = n.nextSegment();
		this.prevSegment = n.prevSegment();
		this.isLocked = n.isLocked();

		if (n.hasMergeSources()) {
			List<IProfileSegment> otherSources = n.getMergeSources();
			mergeSourceA = otherSources.get(0).duplicate();
			mergeSourceB = otherSources.get(1).duplicate();
		}
	}

	@Override
	public IProfileSegment duplicate() {
		return new DefaultProfileSegment(this);
	}

	@Override
	public @NonNull UUID getID() {
		return this.uuid;
	}

	@Override
	public List<IProfileSegment> getMergeSources() {
		if (mergeSourceA == null && mergeSourceB == null)
			return new ArrayList<>();
		if (mergeSourceB == null)
			return List.of(mergeSourceA);
		return List.of(mergeSourceA, mergeSourceB);
	}

	@Override
	public void addMergeSource(@NonNull IProfileSegment seg) {
		if (seg.getID().equals(IProfileCollection.DEFAULT_SEGMENT_ID)) // never replace or chain the
																		// default segment
			return;
		if (seg.getID().equals(getID()))
			throw new IllegalArgumentException(String
					.format("Cannot add merge source with same id as parent: %s", seg.getID()));
		if (getMergeSources().stream().anyMatch(s -> s.getID().equals(seg.getID())))
			throw new IllegalArgumentException(
					String.format("Segment with id %s is already a merge source", seg.getID()));

		if (seg.getProfileLength() != totalLength)
			throw new IllegalArgumentException(String.format(
					"Merge source profile length (%d) does not match this segment (%d)",
					seg.getProfileLength(), this.getProfileLength()));

		if (!this.contains(seg.getStartIndex()))
			throw new IllegalArgumentException(
					String.format("Start index of merge source (%d) is not in this segment (%s)",
							seg.getStartIndex(), this.toString()));

		if (!this.contains(seg.getEndIndex()))
			throw new IllegalArgumentException(String.format(
					"End index of merge source (%s) is not in this segment (%s)", seg.toString(),
					this.toString()));

		if (this.length() < seg.length())
			throw new IllegalArgumentException(String.format(
					"Merge source (%s) is longer than the segment we are adding it to: %s",
					seg.toString(), this.toString()));
		if (mergeSourceA == null) {
			mergeSourceA = seg;
		} else {
			mergeSourceB = seg;
		}
	}

	@Override
	public boolean hasMergeSources() {
		return mergeSourceA != null || mergeSourceB != null;
	}

	@Override
	public void clearMergeSources() {
		mergeSourceA = null;
		mergeSourceB = null;
	}

	@Override
	public boolean hasMergeSource(@NonNull UUID id) {
		if (this.uuid.equals(id))
			return true;

		if (mergeSourceA != null && mergeSourceA.hasMergeSource(id))
			return true;
		if (mergeSourceB != null && mergeSourceB.hasMergeSource(id))
			return true;
		return false;
	}

	@Override
	public IProfileSegment getMergeSource(@NonNull UUID id) throws MissingComponentException {
		if (this.uuid.equals(id))
			return this;
		if (mergeSourceA.hasMergeSource(id))
			return mergeSourceA.getMergeSource(id);
		if (mergeSourceB.hasMergeSource(id))
			return mergeSourceB.getMergeSource(id);
		throw new MissingComponentException("Merge source not present");
	}

	@Override
	public int getStartIndex() {
		return this.startIndex;
	}

	@Override
	public int getEndIndex() {
		return this.endIndex;
	}

	@Override
	public int getProportionalIndex(double d) {
		if (d < 0 || d > 1)
			throw new IllegalArgumentException("Proportion must be between 0-1: " + d);
		double targetLength = length() * d;
		return (int) Math
				.round(CellularComponent.wrapIndex(startIndex + targetLength, getProfileLength()));
	}

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
				return (double) counter / (double) this.length();
			}
			counter++;
		}

		throw new IllegalArgumentException("Cannot get proportion for " + index);
	}

	@Override
	public String getName() {
		return "Seg_" + this.positionInProfile;
	}

	@Override
	public int getMidpointIndex() {
		if (this.wraps()) {

			int midLength = this.length() >> 1;
			if (midLength + startIndex < totalLength) {
				return midLength + startIndex;
			}
			return endIndex - midLength;

		}
		return ((endIndex - startIndex) / 2) + startIndex;
	}

	@Override
	public int getShortestDistanceToStart(int index) {
		if (index < 0 || index >= totalLength) {
			throw new IllegalArgumentException(
					"Index is not in profile: " + index + "; total " + totalLength);
		}

		// Two possibilieites: abs distance or total - abs distance

		int abs = Math.abs(index - startIndex);
		int alt = totalLength - abs;

		return Math.min(abs, alt);
	}

	@Override
	public int getShortestDistanceToEnd(int index) {
		if (index < 0 || index >= totalLength) {
			throw new IllegalArgumentException(
					"Index is not in profile: " + index + "; total " + totalLength);
		}

		int abs = Math.abs(index - endIndex);
		int alt = totalLength - abs;

		return Math.min(abs, alt);
	}

	@Override
	public int getInternalDistanceToStart(int index) {
		if (wraps() && startIndex > index)
			return index + (getProfileLength() - startIndex);
		return index - startIndex;
	}

	@Override
	public int getInternalDistanceToEnd(int index) {
		if (wraps() && getEndIndex() < index)
			return getEndIndex() + (getProfileLength() - index);
		return index - getEndIndex();
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
	public IProfileSegment nextSegment() {
		return this.nextSegment;
	}

	@Override
	public IProfileSegment prevSegment() {
		return this.prevSegment;
	}

	@Override
	public int length() {
		return testLength(this.getStartIndex(), this.getEndIndex());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + uuid.hashCode();
		result = prime * result + endIndex;
		result = prime * result + startIndex;
		result = prime * result + totalLength;
		result = mergeSourceA == null ? prime * result : prime * result + mergeSourceA.hashCode();
		result = mergeSourceB == null ? prime * result : prime * result + mergeSourceB.hashCode();
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
		DefaultProfileSegment other = (DefaultProfileSegment) obj;

		if (!uuid.equals(other.uuid))
			return false;
		if (endIndex != other.endIndex)
			return false;
		if (startIndex != other.startIndex)
			return false;
		if (totalLength != other.totalLength)
			return false;
		if (isLocked != other.isLocked)
			return false;
		if (mergeSourceA != null) {
			if (!mergeSourceA.equals(other.mergeSourceA))
				return false;
			if (!mergeSourceB.equals(other.mergeSourceB))
				return false;
		}
		return true;
	}

	@Override
	public IProfileSegment offset(int offset) {
		IProfileSegment seg = new DefaultProfileSegment(
				CellularComponent.wrapIndex(startIndex + offset, totalLength),
				CellularComponent.wrapIndex(endIndex + offset, totalLength),
				totalLength,
				uuid);

		if (mergeSourceA != null) {
			seg.addMergeSource(mergeSourceA.offset(offset));
			seg.addMergeSource(mergeSourceB.offset(offset));
		}
		seg.setLocked(isLocked);
		return seg;
	}

	@Override
	public int testLength(int start, int end) {
		if (wraps(start, end))
			return end + totalLength + 1 - start; // add total of 2; one for index 0 and one for
													// segment end
		return end - start + 1; // add one for segment end
	}

	@Override
	public boolean wraps(int start, int end) {
		if ((start < 0 || start > totalLength) || (end < 0 || end > totalLength)) {
			throw new IllegalArgumentException("Index is outside profile bounds");
		}
		return (end <= start);
	}

	@Override
	public boolean wraps() {
		return wraps(startIndex, endIndex);
	}

	@Override
	public boolean contains(int index) {
		return IProfileSegment.contains(startIndex, endIndex, index, totalLength);
	}

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
		if (this.isLocked)
			throw new SegmentUpdateException("Segment is locked");

		// only run an update and checks if the update will actually
		// cause changes to the segment. If not, return true so as not
		// to interfere with other linked segments
		if (!updateAffectsThisSegment(startIndex, endIndex))
			return true;

		// Check that the new positions will not make this segment too small
		int testLength = testLength(startIndex, endIndex);
		if (testLength < MINIMUM_SEGMENT_LENGTH)
			throw new SegmentUpdateException(
					String.format("Segment will become too short (%d) if updated to %d - %d (%s)",
							testLength, startIndex, endIndex, toString()));

		// Check that next and previous segments are not invalidated by length
		// change i.e the max length increase backwards is up to the MIN_SEG_LENGTH of
		// the previous segment, and the max length increase forwards is up to the
		// MIN_SEG_LENGTH of the next segment

		if (hasPrevSegment()) {
			if (prevSegment.testLength(prevSegment.getStartIndex(),
					startIndex) < MINIMUM_SEGMENT_LENGTH)
				throw new SegmentUpdateException(String.format(
						"Previous segment (%s) will become too short with start index %d",
						prevSegment.toString(), startIndex));
		}
		if (this.hasNextSegment()) {
			if (nextSegment.testLength(endIndex,
					nextSegment.getEndIndex()) < MINIMUM_SEGMENT_LENGTH)
				throw new SegmentUpdateException(
						String.format("Next segment (%s) will become too short with start index %d",
								nextSegment.toString(), endIndex));
		}

		// check that updating will not cause segments to overlap or invert
		// i.e. where a start becomes greater than an end without begin part of
		// an array wrap
		if (startIndex > endIndex) {
			if (!IProfileSegment.contains(startIndex, endIndex, 0, totalLength))
				throw new SegmentUpdateException(
						String.format("Segment would invert (%d - %d)", startIndex, endIndex));
		}

		// also test the effect on the next and previous segments
		if (this.hasPrevSegment()) {

			if (!contains(startIndex) && !prevSegment().contains(startIndex))

				throw new SegmentUpdateException(String.format(
						"Neither this nor previous segment %s contain the new start index %d",
						prevSegment().getDetail(), startIndex));

			if (this.prevSegment().getStartIndex() > startIndex) {

				if (!prevSegment.wraps() && prevSegment.wraps(startIndex, endIndex))
					throw new SegmentUpdateException(
							"Previous segment would convert to wrapping");

				// another wrapping test - if the new positions induce a wrap,
				// the segment should contain 0
				if (prevSegment.wraps(startIndex, endIndex)
						&& !IProfileSegment.contains(startIndex, endIndex, 0, totalLength))
					throw new SegmentUpdateException(
							"Segment would convert to wrapping but does not contain zero");
			}
		}

		if (this.hasNextSegment()) {
			if (!contains(endIndex) && !nextSegment().contains(endIndex))
				throw new SegmentUpdateException(String.format(
						"Neither this nor next segment %s contain the new end index %d",
						nextSegment().getDetail(), endIndex));

			if (endIndex > nextSegment.getEndIndex()) {

				// if the next segment goes from not wrapping to wrapping when
				// this segment is altered,
				// an inversion must have occurred. Prevent.
				if (!nextSegment.wraps() && nextSegment.wraps(startIndex, endIndex)) {
					throw new SegmentUpdateException(
							"Next segment would convert to wrapping");
				}

				// another wrapping test - if the new positions induce a wrap,
				// the segment should contain 0
				if (nextSegment.wraps(startIndex, endIndex)
						&& !IProfileSegment.contains(startIndex, endIndex, 0, totalLength)) {
					throw new SegmentUpdateException(
							"Segment would convert to wrapping but does not contain zero");
				}
			}
		}
		return true;

	}

	@Override
	public boolean update(int startIndex, int endIndex) throws SegmentUpdateException {
		// Check the incoming data
		if (startIndex < 0 || startIndex > totalLength)
			throw new SegmentUpdateException(
					"Start index is outside the profile range: " + startIndex);

		if (endIndex < 0 || endIndex > totalLength)
			throw new SegmentUpdateException("End index is outside the profile range: " + endIndex);

		// Ensure next and prev segments cannot be 'jumped over'

		if (!canUpdateSegment(startIndex, endIndex))
			throw new SegmentUpdateException("Unable to update segment");

		// All checks have been passed; the update can proceed

		/*
		 * We should remove any merge sources here because as segments repeatedly
		 * update, we can't be sure the boundaries will stay in sync for un-merging over
		 * repeated updates. If the merged segment shrinks, the merge sources may end up
		 * too small to be a valid segment - what happens if a merge source becomes 0
		 * length?
		 * 
		 * However, we can't just remove sources here, because some updates are made
		 * while linking segments, which removes valid merge sources.
		 * 
		 * It is simple for the user to create and move segments as needed to restore
		 * original patterns.
		 */
		if (hasMergeSources()) {
			mergeSourceA.update(startIndex, mergeSourceA.getEndIndex());
			mergeSourceB.update(mergeSourceB.getStartIndex(), endIndex);
		}

		// wrap in if to ensure we don't go in circles forever when testing a
		// circular profile
		if (this.getStartIndex() != startIndex) { // becomes false after the
													// first pass of the circle
			this.startIndex = startIndex;
			if (this.hasPrevSegment())
				prevSegment.update(prevSegment.getStartIndex(), startIndex);
		}

		if (this.getEndIndex() != endIndex) {
			this.endIndex = endIndex;

			if (this.hasNextSegment())
				nextSegment.update(endIndex, nextSegment.getEndIndex());
		}
		return true;

	}

	@Override
	public void setNextSegment(@NonNull IProfileSegment s) {
		if (s.getProfileLength() != this.getProfileLength())
			throw new IllegalArgumentException("Segment has a different profile length");
		if (s.getStartIndex() != this.getEndIndex())
			throw new IllegalArgumentException(
					String.format("Segment start (%d) does not overlap this end (%d)",
							s.getStartIndex(), getEndIndex()));

		nextSegment = s;
	}

	@Override
	public void setPrevSegment(@NonNull IProfileSegment s) {
		if (s.getProfileLength() != getProfileLength())
			throw new IllegalArgumentException("Segment has a different profile length");

		if (s.getEndIndex() != getStartIndex())
			throw new IllegalArgumentException(
					String.format("Segment end (%d) does not overlap this start (%d)",
							s.getEndIndex(), getStartIndex()));

		prevSegment = s;
	}

	@Override
	public boolean hasNextSegment() {
		return nextSegment() != null;
	}

	@Override
	public boolean hasPrevSegment() {
		return prevSegment() != null;
	}

	@Override
	public void setPosition(int i) {
		if (i < 0)
			throw new IllegalArgumentException("Position must be a positve integer");
		positionInProfile = (short) i;
	}

	@Override
	public int getPosition() {
		return positionInProfile;
	}

	@Override
	public String toString() {
		return String.format("%d - %d of %d: %s",
				startIndex, endIndex, totalLength, uuid.toString());
	}

	@Override
	public String getDetail() {
		return String.format("Segment %s | %s | %s | %s - %s | %s of %s | %s ",
				getName(), getID(), getPosition(), startIndex, endIndex,
				length(), totalLength, wraps());
	}

	@Override
	public Iterator<Integer> iterator() {

		List<Integer> indexes = new ArrayList<>();
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
	public boolean overlapsBeyondEndpoints(@NonNull IProfileSegment seg) {
		if (seg.getProfileLength() != getProfileLength())
			return false;

		Iterator<Integer> it = this.iterator();
		while (it.hasNext()) {
			int index = it.next();
			if (index == getStartIndex() || index == getEndIndex())
				continue;
			if (seg.contains(index))
				return true;
		}
		return false;
	}

	@Override
	public boolean overlaps(@NonNull IProfileSegment seg) {
		if (seg.getProfileLength() != getProfileLength())
			return false;
		return seg.contains(startIndex)
				|| seg.contains(getEndIndex())
				|| contains(seg.getStartIndex())
				|| contains(seg.getEndIndex());
	}

	@Override
	public IProfileSegment reverse() {
		// invert the segment by swapping start and end
		int newStart = totalLength - 1 - getEndIndex();
		int newEnd = totalLength - 1 - getStartIndex();

		return new DefaultProfileSegment(newStart, newEnd, totalLength, getID());
	}

}
