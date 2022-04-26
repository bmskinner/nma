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
package com.bmskinner.nuclear_morphology.components.cells;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileCreator;
import com.bmskinner.nuclear_morphology.analysis.profiles.SegmentFitter;
import com.bmskinner.nuclear_morphology.components.MissingComponentException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultSegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.LandmarkType;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.rules.OrientationMark;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.gui.Roi;

/**
 * This is the class of objects that can have profiles applied to them.
 * Positions around the border of the component can be tagged; the profiles will
 * track the tags. Default was false
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public abstract class ProfileableCellularComponent extends DefaultCellularComponent implements Taggable {

	private static final String XML_WINDOW_PROPORTION = "window";

	private static final Logger LOGGER = Logger.getLogger(ProfileableCellularComponent.class.getName());

	/** The proportion of the perimeter to use for profiling */
	private double windowProportion = IAnalysisOptions.DEFAULT_WINDOW_PROPORTION;

	/** The segmentation pattern for the object */
	private final List<IProfileSegment> segments = new ArrayList<>();

	/** The indexes of landmarks in the profiles and border list */
	private Map<Landmark, Integer> profileLandmarks = new HashMap<>();

	/** Store the landmarks to be used for orientation */
	private Map<@NonNull OrientationMark, Landmark> orientationMarks = new EnumMap<>(OrientationMark.class);

	/** allow locking of segments and landmarks */
	private boolean isLocked = false;

	/** The profiles for this object */
	private Map<ProfileType, IProfile> profileMap = new ConcurrentHashMap<>();

	/**
	 * Construct with an ROI, a source image and channel, and the original position
	 * in the source image. It sets the immutable original centre of mass, and the
	 * mutable current centre of mass. It also assigns a random ID to the component.
	 * 
	 * @param roi          the roi of the object
	 * @param centerOfMass the original centre of mass of the component
	 * @param source       the image file the component was found in
	 * @param channel      the RGB channel the component was found in
	 * @param position     the bounding position of the component in the original
	 *                     image
	 */
	protected ProfileableCellularComponent(@NonNull Roi roi, @NonNull IPoint centreOfMass, File source, int channel,
			int x, int y) {
		this(roi, centreOfMass, source, channel, x, y, null);
	}

	/**
	 * Construct with an ROI, a source image and channel, and the original position
	 * in the source image. It sets the immutable original centre of mass, and the
	 * mutable current centre of mass. It also assigns a random ID to the component.
	 * 
	 * @param roi          the roi of the object
	 * @param centerOfMass the original centre of mass of the component
	 * @param source       the image file the component was found in
	 * @param channel      the RGB channel the component was found in
	 * @param position     the bounding position of the component in the original
	 *                     image
	 * @param id           the id of the component. Only use when deserialising!
	 */
	protected ProfileableCellularComponent(@NonNull Roi roi, @NonNull IPoint centreOfMass, File source, int channel,
			int x, int y, @Nullable UUID id) {
		super(roi, centreOfMass, source, channel, x, y, id);
		profileLandmarks.put(OrientationMark.REFERENCE, 0);
	}

	/**
	 * Create a new component based on the given template object. If the object has
	 * segments, these will be copied to the new component.
	 * 
	 * @param c
	 * @throws UnprofilableObjectException
	 */
	protected ProfileableCellularComponent(@NonNull final Taggable c) throws ComponentCreationException {
		super(c);

		this.windowProportion = c.getWindowProportion();

		profileLandmarks.clear();
		for (Entry<Landmark, Integer> entry : c.getLandmarks().entrySet())
			profileLandmarks.put(entry.getKey(), entry.getValue());

		this.isLocked = c.isLocked();

		try {

			// When duplicating components we can copy the existing profiles
			for (ProfileType type : ProfileType.values()) {
				profileMap.put(type, c.getUnsegmentedProfile(type, OrientationMark.REFERENCE)
						.startFrom(-c.getBorderIndex(OrientationMark.REFERENCE)));
			}

			segments.clear();
			segments.addAll(c.getProfile(ProfileType.ANGLE).startFrom(-c.getBorderIndex(OrientationMark.REFERENCE))
					.getSegments()); // getSegments creates a copy, no need to duplicate again
			IProfileSegment.linkSegments(segments);

		} catch (ProfileException | MissingProfileException | MissingLandmarkException e) {
			throw new ComponentCreationException("Cannot make new profile from template", e);
		}

	}

	protected ProfileableCellularComponent(Element e) throws ComponentCreationException {
		super(e);
		windowProportion = Double.parseDouble(e.getAttributeValue(XML_WINDOW_PROPORTION));

		isLocked = e.getAttributeValue("locked") != null;

		for (Element el : e.getChildren("Landmark")) {
			profileLandmarks.put(
					Landmark.of(el.getAttributeValue("name"), LandmarkType.valueOf(el.getAttributeValue("type"))),
					Integer.parseInt(el.getAttributeValue("index")));
		}

		try {
			for (Element el : e.getChildren("Segment")) {
				segments.add(new DefaultProfileSegment(el));
			}
			IProfileSegment.linkSegments(segments);

			for (ProfileType type : ProfileType.values()) {
				profileMap.put(type, ProfileCreator.createProfile(this, type));
			}

		} catch (ProfileException e1) {
			throw new ComponentCreationException("Unable to link segments in object constructor", e1);
		}

		// Note - do not call initialise here since subclasses
		// will not have set all fields yet
	}

	@Override
	public void createProfiles(double proportion) throws ComponentCreationException {
		if (proportion <= 0 || proportion >= 1)
			throw new ComponentCreationException("Must have a value between 0-1");
		LOGGER.finer("Beginning create profiles");
		windowProportion = proportion;

		try {
			for (ProfileType type : ProfileType.values())
				profileMap.put(type, ProfileCreator.createProfile(this, type));

			// Any existing segments should be completely cleared on new initialisation
			segments.clear();
			segments.add(
					new DefaultProfileSegment(0, 0, this.getBorderLength(), IProfileCollection.DEFAULT_SEGMENT_ID));
		} catch (ProfileException e) {
			throw new ComponentCreationException("Could not calculate profiles due to " + e.getMessage(), e);
		}
	}

	public IPoint getPoint(@NonNull Landmark tag) throws MissingLandmarkException {
		int index = this.getBorderIndex(tag);
		return this.getBorderPoint(index);
	}

	@Override
	public IPoint getBorderPoint(@NonNull Landmark tag) throws MissingLandmarkException {
		int borderIndex = this.getBorderIndex(tag);

		if (borderIndex < 0 || borderIndex >= this.getBorderLength())
			throw new MissingLandmarkException(
					String.format("Landmark '%s' saved at index %s is not within profile (length %s)", tag, borderIndex,
							this.getBorderLength()));

		return getBorderPoint(borderIndex);
	}

	@Override
	public Map<Landmark, Integer> getLandmarks() {
		Map<Landmark, Integer> result = new HashMap<>();
		for (Landmark b : profileLandmarks.keySet()) {
			result.put(b, profileLandmarks.get(b));
		}
		return result;
	}

	@Override
	public Landmark getBorderTag(int index) {
		for (Entry<Landmark, Integer> entry : profileLandmarks.entrySet()) {
			if (entry.getValue() == index)
				return entry.getKey();
		}
		return null;
	}

	@Override
	public int getBorderIndex(@NonNull Landmark tag) throws MissingLandmarkException {
		if (profileLandmarks.containsKey(tag))
			return profileLandmarks.get(tag);
		throw new MissingLandmarkException("Tag " + tag + " is not present");
	}

	@Override
	public void setLandmark(@NonNull Landmark lm, int newLmIndex)
			throws MissingProfileException, ProfileException, MissingLandmarkException {
		if (isLocked)
			return;

		if (newLmIndex < 0 || newLmIndex >= this.getBorderLength())
			throw new IllegalArgumentException(
					String.format("Index %s is out of bounds for border length %s", newLmIndex, getBorderLength()));

		// If not the RP, set and return
		if (!OrientationMark.REFERENCE.equals(lm)) {
			profileLandmarks.put(lm, newLmIndex);
			return;
		}

		// On assignment of the RP, ensure that a segment
		// boundary is at the RP. We do this by moving the existing
		// segments along until the old RP boundary segment is at the new RP.
		int oldRP = profileLandmarks.get(lm);

		// This profile has segments starting from the old RP
		ISegmentedProfile p = new DefaultSegmentedProfile(profileMap.get(ProfileType.ANGLE), segments);

		/*
		 * There is at least one segment starting at the original RP index. When the
		 * profile is set to start from the old RP, all the segment boundary coordinates
		 * in this profile will have been shifted by -oldRP.
		 */
		int diff = newLmIndex - oldRP;
		p.moveSegments(diff);

		segments.clear();
		for (IProfileSegment s : p.getSegments()) {
			segments.add(s.duplicate());
		}
		profileLandmarks.put(lm, newLmIndex);
	}

	@Override
	public boolean hasLandmark(@NonNull Landmark tag) {
		return this.profileLandmarks.containsKey(tag);
	}

	@Override
	public int getIndexRelativeTo(@NonNull Landmark reference, int index) throws MissingLandmarkException {
		return wrapIndex(index + this.getBorderIndex(reference));
	}

	/*
	 * ############################################# Methods implementing the
	 * Profileable interface #############################################
	 */

	@Override
	public boolean isLocked() {
		return isLocked;
	}

	@Override
	public void setLocked(boolean b) {
		isLocked = b;
	}

	@Override
	public int getWindowSize() {
		return Math.max(1, (int) Math.ceil(getMeasurement(Measurement.PERIMETER) * windowProportion));
	}

	@Override
	public double getWindowProportion() {
		return windowProportion;
	}

	@Override
	public void setWindowProportion(double d) {
		if (d <= 0 || d >= 1)
			throw new IllegalArgumentException("Angle window proportion must be higher than 0 and less than 1");

		if (isLocked)
			return;

		windowProportion = d;

		try {
			for (ProfileType type : ProfileType.values())
				profileMap.put(type, ProfileCreator.createProfile(this, type));
		} catch (ProfileException e) {
			LOGGER.warning("Unable to set window proportion");
			LOGGER.log(Loggable.STACK, e.getMessage(), e);
		}

	}

	@Override
	public boolean hasProfile(@NonNull ProfileType type) {
		return this.profileMap.containsKey(type);
	}

	@Override
	public ISegmentedProfile getProfile(@NonNull ProfileType type)
			throws MissingProfileException, ProfileException, MissingLandmarkException {
		if (!this.hasProfile(type))
			throw new MissingProfileException("Cannot get profile type " + type);
		return getProfile(type, OrientationMark.REFERENCE);
	}

	@Override
	public ISegmentedProfile getProfile(@NonNull ProfileType type, @NonNull Landmark lm)
			throws ProfileException, MissingLandmarkException, MissingProfileException {

		if (!this.hasLandmark(lm))
			throw new MissingLandmarkException("Landmark " + lm + " not present");

		// fetch the index of the pointType (the zero index of the profile to return)
		int lmIndex = profileLandmarks.get(lm);

		// Get the raw profile
		ISegmentedProfile profile = new DefaultSegmentedProfile(profileMap.get(type), segments);

		// offset the profile to start at the desired landmark
		return profile.startFrom(lmIndex);
	}

	@Override
	public IProfile getUnsegmentedProfile(@NonNull ProfileType type, @NonNull Landmark lm)
			throws ProfileException, MissingLandmarkException {
		if (!this.hasLandmark(lm))
			throw new MissingLandmarkException("Landmark " + lm + " not present");
		int lmIndex = profileLandmarks.get(lm);
		return profileMap.get(type).startFrom(lmIndex);
	}

	@Override
	public void setSegments(@NonNull List<IProfileSegment> segs) throws MissingLandmarkException, ProfileException {

		if (isLocked) {
			LOGGER.finer("Cannot set profile segments: object is locked");
			return;
		}

		if (segs.get(0).getStartIndex() != 0)
			throw new ProfileException("Cannot set segments: no boundary at index 0: " + segs.get(0).toString());

		// fetch the index of the RP (the zero of the input profile)
		int rpIndex = profileLandmarks.get(OrientationMark.REFERENCE);

		// add the RP offset so the segments match the absolute RP
		segments.clear();
		for (IProfileSegment s : segs) {
			segments.add(s.offset(rpIndex));
		}
		IProfileSegment.linkSegments(segments);
	}

	@Override
	public void setSegmentStartLock(boolean isLocked, @NonNull UUID segId) {

		for (IProfileSegment seg : segments) {
			if (seg.getID().equals(segId))
				seg.setLocked(isLocked);
		}
	}

	@Override
	public void reverse() throws MissingComponentException, ProfileException {
		if (isLocked)
			return;

		// Note that this action can alter the interpolated
		// perimeter length, invalidating any existing segments
		super.reverse();

		// Recreate profiles for new outline
		ISegmentedProfile oldAngleProfile = this.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

		for (ProfileType t : ProfileType.values()) {
			IProfile p = ProfileCreator.createProfile(this, t);
			profileMap.put(t, p);
		}

		// Reapply and rescale segments

		// Profile length may have changed, we need to adjust the segmentation pattern
		// Reverse the copied angle profile, and fit it to the new angle profile
		oldAngleProfile.reverse();
		ISegmentedProfile newAngleProfile = SegmentFitter.fit(oldAngleProfile,
				new DefaultSegmentedProfile(profileMap.get(ProfileType.ANGLE)));

		segments.clear();
		for (IProfileSegment s : newAngleProfile.getSegments()) {
			segments.add(s);
		}
		IProfileSegment.linkSegments(segments);

		// Update positions of landmarks

		// The RP needs to be moved to the first segment start index
		this.profileLandmarks.put(OrientationMark.REFERENCE, segments.get(0).getStartIndex());

		// Other landmarks are usually not set at the point we reverse borders
		// BUT if they are present find the best guess given that the border
		// length may have changed
		for (Entry<Landmark, Integer> entry : profileLandmarks.entrySet()) {
			if (OrientationMark.REFERENCE.equals(entry.getKey()))
				continue;
			int index = entry.getValue();

			// if was 0, will be <length-1>; if
			// was length-1, will be 0
			int newIndex = this.getBorderLength() - index - 1;

			// update the landmark map directly to avoid segmentation changes
			// due to RP shift
			profileLandmarks.put(entry.getKey(), newIndex);
		}
	}

	@Override
	public Element toXmlElement() {
		Element e = super.toXmlElement().setAttribute(XML_WINDOW_PROPORTION, String.valueOf(windowProportion));

		if (isLocked)
			e.setAttribute("locked", "true");

		for (IProfileSegment s : segments) {
			e.addContent(s.toXmlElement());
		}

		for (Entry<Landmark, Integer> entry : profileLandmarks.entrySet()) {
			e.addContent(new Element("Landmark").setAttribute("name", entry.getKey().toString())
					.setAttribute("type", entry.getKey().type().toString())
					.setAttribute("index", String.valueOf(entry.getValue())));
		}

		return e;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(isLocked, profileLandmarks, profileMap, segments, windowProportion);
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
		ProfileableCellularComponent other = (ProfileableCellularComponent) obj;

		for (Entry<ProfileType, IProfile> e : profileMap.entrySet()) {
			if (!other.profileMap.containsKey(e.getKey()))
				return false;
			if (!e.getValue().equals(other.profileMap.get(e.getKey())))
				return false;
		}

		return isLocked == other.isLocked && Objects.equals(profileLandmarks, other.profileLandmarks)
				&& Objects.equals(segments, other.segments)
				&& Double.doubleToLongBits(windowProportion) == Double.doubleToLongBits(other.windowProportion);
	}

	@Override
	public String toString() {
		String newLine = System.getProperty("line.separator");
		StringBuilder builder = new StringBuilder(super.toString() + newLine);

		builder.append("Window prop: " + this.windowProportion);
		builder.append(newLine);
		builder.append(newLine);
		builder.append("Segments: " + this.segments);
		builder.append(newLine);
		builder.append("Landmarks: " + this.profileLandmarks);
		builder.append(newLine);
		builder.append("Lockstate: " + this.isLocked);
		builder.append(newLine);
		builder.append("Profile map: " + this.profileMap);
		return builder.toString();
	}

}
