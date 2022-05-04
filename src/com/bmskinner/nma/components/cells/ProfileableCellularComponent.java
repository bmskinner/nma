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
package com.bmskinner.nma.components.cells;

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
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jdom2.Element;

import com.bmskinner.nma.analysis.profiles.ProfileCreator;
import com.bmskinner.nma.analysis.profiles.SegmentFitter;
import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.profiles.DefaultLandmark;
import com.bmskinner.nma.components.profiles.DefaultProfileSegment;
import com.bmskinner.nma.components.profiles.DefaultSegmentedProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileCollection;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.profiles.UnprofilableObjectException;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.PriorityAxis;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.logging.Loggable;

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
public abstract class ProfileableCellularComponent extends DefaultCellularComponent
		implements Taggable {

	private static final String XML_WINDOW_PROPORTION = "window";
	private static final String XML_ORIENTATION = "Orientation";
	private static final String XML_PRIORITY_AXIS = "axis";

	private static final Logger LOGGER = Logger
			.getLogger(ProfileableCellularComponent.class.getName());

	/** The proportion of the perimeter to use for profiling */
	private double windowProportion = IAnalysisOptions.DEFAULT_WINDOW_PROPORTION;

	/** The segmentation pattern for the object */
	private final List<IProfileSegment> segments = new ArrayList<>();

	/** The indexes of landmarks in the profiles and border list */
	protected Map<Landmark, Integer> profileLandmarks = new HashMap<>();

	/** Store the landmarks to be used for orientation */
	protected Map<@NonNull OrientationMark, Landmark> orientationMarks = new EnumMap<>(
			OrientationMark.class);

	private PriorityAxis priorityAxis = PriorityAxis.Y;

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
	 * @throws ComponentCreationException
	 */
	protected ProfileableCellularComponent(@NonNull Roi roi, @NonNull IPoint centreOfMass,
			File source, int channel, @NonNull RuleSetCollection rsc)
			throws ComponentCreationException {
		this(roi, centreOfMass, source, channel, null, rsc);
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
	 * @throws ComponentCreationException
	 */
	protected ProfileableCellularComponent(@NonNull Roi roi, @NonNull IPoint centreOfMass,
			File source, int channel, @Nullable UUID id, @NonNull RuleSetCollection rsc)
			throws ComponentCreationException {
		super(roi, centreOfMass, source, channel, id);

		for (@NonNull
		OrientationMark s : rsc.getOrientionMarks()) {
			Landmark l = rsc.getLandmark(s).orElseThrow(ComponentCreationException::new);
			orientationMarks.put(s, l);
		}

		priorityAxis = rsc.getPriorityAxis().orElse(PriorityAxis.Y);
		profileLandmarks.put(orientationMarks.get(OrientationMark.REFERENCE), 0);
	}

	/**
	 * Create a new component based on the given template object. If the object has
	 * segments, these will be copied to the new component.
	 * 
	 * @param c
	 * @throws UnprofilableObjectException
	 */
	protected ProfileableCellularComponent(@NonNull final Taggable c)
			throws ComponentCreationException {
		super(c);

		this.windowProportion = c.getWindowProportion();

		for (OrientationMark s : OrientationMark.values()) {
			if (c.getLandmark(s) != null)
				orientationMarks.put(s, c.getLandmark(s));
		}

		profileLandmarks.clear();
		for (Entry<OrientationMark, Integer> entry : c.getOrientationMarkMap().entrySet())
			profileLandmarks.put(orientationMarks.get(entry.getKey()), entry.getValue());

		priorityAxis = c.getPriorityAxis();

		this.isLocked = c.isLocked();

		try {

			// When duplicating components we can copy the existing profiles
			for (ProfileType type : ProfileType.values()) {
				profileMap.put(type, c.getUnsegmentedProfile(type, OrientationMark.REFERENCE)
						.startFrom(-c.getBorderIndex(OrientationMark.REFERENCE)));
			}

			segments.clear();
			segments.addAll(c.getProfile(ProfileType.ANGLE)
					.startFrom(-c.getBorderIndex(OrientationMark.REFERENCE))
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
			profileLandmarks.put(new DefaultLandmark(el.getAttributeValue("name")),
					Integer.parseInt(el.getAttributeValue("index")));
		}

		for (Element el : e.getChildren(XML_ORIENTATION)) {
			OrientationMark name = OrientationMark.valueOf(el.getAttributeValue("name"));
			Landmark l = profileLandmarks.keySet().stream()
					.filter(lm -> lm.getName().equals(el.getAttributeValue("value"))).findFirst()
					.get();
			orientationMarks.put(name, l);
		}
		priorityAxis = PriorityAxis.valueOf(e.getAttributeValue(XML_PRIORITY_AXIS));

		try {
			for (Element el : e.getChildren("Segment")) {
				segments.add(new DefaultProfileSegment(el));
			}
			IProfileSegment.linkSegments(segments);

			for (ProfileType type : ProfileType.values()) {
				profileMap.put(type, ProfileCreator.createProfile(this, type));
			}

		} catch (ProfileException e1) {
			throw new ComponentCreationException("Unable to link segments in object constructor",
					e1);
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
					new DefaultProfileSegment(0, 0, this.getBorderLength(),
							IProfileCollection.DEFAULT_SEGMENT_ID));
		} catch (ProfileException e) {
			throw new ComponentCreationException(
					"Could not calculate profiles due to " + e.getMessage(), e);
		}
	}

	@Override
	public @Nullable PriorityAxis getPriorityAxis() {
		return priorityAxis;
	}

	@Override
	public void setLandmark(@NonNull OrientationMark om, int newLmIndex)
			throws MissingProfileException, MissingLandmarkException, ProfileException {
		Landmark land = orientationMarks.get(om);

		if (land == null)
			throw new MissingLandmarkException("Cannot find landmark for '" + om + "' (we have "
					+ profileLandmarks.entrySet().stream()
							.map(e -> e.getKey().toString() + " - " + String.valueOf(e.getValue()))
							.collect(Collectors.joining(", "))
					+ ")");
		setLandmark(land, newLmIndex);
	}

	@Override
	public void setLandmark(@NonNull Landmark land, int newLmIndex)
			throws MissingProfileException, ProfileException, MissingLandmarkException {
		if (isLocked)
			return;

		if (newLmIndex < 0 || newLmIndex >= this.getBorderLength())
			throw new IllegalArgumentException(
					String.format("Index %s is out of bounds for border length %s", newLmIndex,
							getBorderLength()));

		// If not the RP, set and return
		Landmark rp = orientationMarks.get(OrientationMark.REFERENCE);

		if (!rp.equals(land)) {
			profileLandmarks.put(land, newLmIndex);
			return;
		}

		// On assignment of the RP, ensure that a segment
		// boundary is at the RP. We do this by moving the existing
		// segments along until the old RP boundary segment is at the new RP.
		int oldRP = profileLandmarks.get(land);

		// This profile has segments starting from the old RP
		ISegmentedProfile p = new DefaultSegmentedProfile(profileMap.get(ProfileType.ANGLE),
				segments);

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
		profileLandmarks.put(land, newLmIndex);

		// At this point the RP should be on a segment boundary
		boolean isOk = false;
		for (IProfileSegment s : segments)
			if (s.getStartIndex() == newLmIndex)
				isOk = true;
		assert (isOk);
	}

	@Override
	public int getIndexRelativeTo(@NonNull OrientationMark om, int index)
			throws MissingLandmarkException {
		return wrapIndex(index + getBorderIndex(om));
	}

	@Override
	public int getBorderIndex(@NonNull OrientationMark om) throws MissingLandmarkException {
		Landmark lm = getLandmark(om);
		if (lm == null)
			throw new MissingLandmarkException("Cannot find landmark for " + om);
		return getBorderIndex(lm);
	}

	@Override
	public int getBorderIndex(@NonNull Landmark lm) throws MissingLandmarkException {

		if (profileLandmarks.containsKey(lm) && profileLandmarks.get(lm) != null)
			return profileLandmarks.get(lm);

		throw new MissingLandmarkException("Landmark '" + lm + "' has no index set (we have "
				+ profileLandmarks.entrySet().stream()
						.map(e -> e.getKey().toString() + " - " + String.valueOf(e.getValue()))
						.collect(Collectors.joining(", "))
				+ ")");
	}

	@Override
	public IPoint getBorderPoint(@NonNull Landmark lm) throws MissingLandmarkException {
		return getBorderPoint(getBorderIndex(lm));
	}

	@Override
	public IPoint getBorderPoint(@NonNull OrientationMark om) throws MissingLandmarkException {
		int borderIndex = this.getBorderIndex(om);

		if (borderIndex < 0 || borderIndex >= this.getBorderLength())
			throw new MissingLandmarkException(
					String.format(
							"Landmark '%s' saved at index %s is not within profile (length %s)", om,
							borderIndex,
							this.getBorderLength()));

		return getBorderPoint(borderIndex);
	}

	@Override
	public void setOrientationMark(@NonNull OrientationMark om, int i)
			throws IndexOutOfBoundsException, MissingProfileException, ProfileException,
			MissingLandmarkException {
		setLandmark(om, i);
	}

	@Override
	public Map<OrientationMark, Integer> getOrientationMarkMap() {
		Map<OrientationMark, Integer> result = new HashMap<>();
		for (Entry<@NonNull OrientationMark, Landmark> entry : orientationMarks.entrySet()) {
			result.put(entry.getKey(), profileLandmarks.get(entry.getValue()));
		}
		return result;
	}

	@Override
	public @Nullable Landmark getLandmark(OrientationMark s) {
		return orientationMarks.get(s);
	}

	@Override
	public boolean hasLandmark(@NonNull OrientationMark landmark) {
		return orientationMarks.containsKey(landmark);
	}

	@Override
	public @NonNull ISegmentedProfile getProfile(@NonNull ProfileType type,
			@NonNull OrientationMark om)
			throws MissingLandmarkException, MissingProfileException, ProfileException {
		Landmark lm = getLandmark(om);
		if (lm == null)
			throw new MissingLandmarkException("Cannot find landmark for " + om);
		return getProfile(type, lm);
	}

	@Override
	public @NonNull ISegmentedProfile getProfile(@NonNull ProfileType type, @NonNull Landmark lm)
			throws ProfileException, MissingLandmarkException, MissingProfileException {

		// fetch the index of the pointType (the zero index of the profile to return)
		if (profileLandmarks.get(lm) == null)
			throw new MissingLandmarkException("Cannot find index for " + lm);

		int lmIndex = profileLandmarks.get(lm);

		// Get the raw profile
		ISegmentedProfile profile = new DefaultSegmentedProfile(profileMap.get(type), segments);

		// offset the profile to start at the desired landmark
		return profile.startFrom(lmIndex);
	}

	@Override
	public IProfile getUnsegmentedProfile(@NonNull ProfileType type, @NonNull OrientationMark om)
			throws ProfileException, MissingLandmarkException, MissingProfileException {
		Landmark lm = getLandmark(om);
		if (lm == null)
			throw new MissingLandmarkException("Cannot find landmark for " + om);
		int lmIndex = profileLandmarks.get(lm);
		return profileMap.get(type).startFrom(lmIndex);
	}

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
		return Math.max(1,
				(int) Math.ceil(getMeasurement(Measurement.PERIMETER) * windowProportion));
	}

	@Override
	public double getWindowProportion() {
		return windowProportion;
	}

	@Override
	public void setWindowProportion(double d) {
		if (d <= 0 || d >= 1)
			throw new IllegalArgumentException(
					"Angle window proportion must be higher than 0 and less than 1");

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
	public void setSegments(@NonNull List<IProfileSegment> segs)
			throws MissingLandmarkException, ProfileException {

		if (isLocked) {
			LOGGER.finer("Cannot set profile segments: object is locked");
			return;
		}

		if (segs.get(0).getStartIndex() != 0)
			throw new ProfileException(
					"Cannot set segments: no boundary at index 0: " + segs.get(0).toString());

		// fetch the index of the RP (the zero of the input profile)
		int rpIndex = profileLandmarks.get(getLandmark(OrientationMark.REFERENCE));

		// add the RP offset so the segments match the absolute RP
		segments.clear();
		for (IProfileSegment s : segs) {
			segments.add(s.offset(rpIndex));
		}
		IProfileSegment.linkSegments(segments);

		// At this point the RP should be on a segment boundary
		boolean isOk = false;
		for (IProfileSegment s : segments)
			if (s.getStartIndex() == rpIndex)
				isOk = true;
		assert (isOk);
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
		ISegmentedProfile oldAngleProfile = this.getProfile(ProfileType.ANGLE,
				OrientationMark.REFERENCE);

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
		Landmark rp = orientationMarks.get(OrientationMark.REFERENCE);
		this.profileLandmarks.put(rp, segments.get(0).getStartIndex());

		// Other landmarks are usually not set at the point we reverse borders
		// BUT if they are present find the best guess given that the border
		// length may have changed
		for (Entry<Landmark, Integer> entry : profileLandmarks.entrySet()) {
			Landmark lm = entry.getKey();
			if (rp.equals(lm))
				continue;
			int index = entry.getValue();

			// if was 0, will be <length-1>; if
			// was length-1, will be 0
			int newIndex = this.getBorderLength() - index - 1;

			// update the landmark map directly to avoid segmentation changes
			// due to RP shift
			profileLandmarks.put(lm, newIndex);
		}

		// At this point the RP should be on a segment boundary
		boolean isOk = false;
		for (IProfileSegment s : segments)
			if (s.getStartIndex() == profileLandmarks.get(rp))
				isOk = true;
		assert (isOk);
	}

	@Override
	public Element toXmlElement() {
		Element e = super.toXmlElement()
				.setAttribute(XML_WINDOW_PROPORTION, String.valueOf(windowProportion))
				.setAttribute(XML_PRIORITY_AXIS, priorityAxis.toString());

		if (isLocked)
			e.setAttribute("locked", "true");

		for (IProfileSegment s : segments) {
			e.addContent(s.toXmlElement());
		}

		for (Entry<Landmark, Integer> entry : profileLandmarks.entrySet()) {
			e.addContent(new Element("Landmark").setAttribute("name", entry.getKey().toString())
					.setAttribute("index", String.valueOf(entry.getValue())));
		}

		for (@NonNull
		Entry<OrientationMark, Landmark> entry : orientationMarks.entrySet()) {
			e.addContent(new Element(XML_ORIENTATION).setAttribute("name", entry.getKey().name())
					.setAttribute("value",
							entry.getValue().toString()));
		}

		return e;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ Objects.hash(isLocked, profileLandmarks, profileMap, segments, orientationMarks,
						priorityAxis, windowProportion);
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

		return isLocked == other.isLocked
				&& Objects.equals(profileLandmarks, other.profileLandmarks)
				&& Objects.equals(segments, other.segments)
				&& Objects.equals(priorityAxis, other.priorityAxis)
				&& Objects.equals(orientationMarks, other.orientationMarks)
				&& Double.doubleToLongBits(windowProportion) == Double
						.doubleToLongBits(other.windowProportion);
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
