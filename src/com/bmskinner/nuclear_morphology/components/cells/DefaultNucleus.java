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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.ComponentOrienter;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.rules.OrientationMark;
import com.bmskinner.nuclear_morphology.components.rules.PriorityAxis;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.components.signals.DefaultSignalCollection;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.signals.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.signals.NuclearSignalAddedListener;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;
import com.bmskinner.nuclear_morphology.utility.AngleTools;

import ij.gui.Roi;

/**
 * The standard round nucleus, implementing {@link Nucleus}. All non-round
 * nuclei extend this.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultNucleus extends ProfileableCellularComponent implements Nucleus, NuclearSignalAddedListener {

	private static final Logger LOGGER = Logger.getLogger(DefaultNucleus.class.getName());

	private static final String XML_NUCLEUS_NUMBER = "number";
	private static final String XML_ORIENTATION = "Orientation";
	private static final String XML_PRIORITY_AXIS = "axis";
	private static final String XML_SIGNAL_COLLECTION = "SignalCollection";

	/** The number of the nucleus in its image, for display */
	private int nucleusNumber;

	/** Store the landmarks to be used for orientation */
	private Map<@NonNull OrientationMark, Landmark> orientationMarks = new EnumMap<>(OrientationMark.class);

	private PriorityAxis priorityAxis = PriorityAxis.Y;

	/** FISH signals in the nucleus */
	private ISignalCollection signalCollection = new DefaultSignalCollection();

	private Nucleus orientedNucleus = null;

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
	public DefaultNucleus(@NonNull Roi roi, @NonNull IPoint centreOfMass, File source, int channel, int x, int y,
			int number, @Nullable UUID id, RuleSetCollection rsc) {
		super(roi, centreOfMass, source, channel, x, y, id);
		this.nucleusNumber = number;

		for (OrientationMark s : OrientationMark.values()) {
			if (rsc.getLandmark(s).isPresent()) {
				orientationMarks.put(s, rsc.getLandmark(s).get());
			}
		}
		signalCollection.addNuclearSignalAddedListener(this);
		priorityAxis = rsc.getPriorityAxis().orElse(PriorityAxis.Y);
	}

	/**
	 * Construct with an ROI, a source image and channel, and the original position
	 * in the source image
	 * 
	 * @param roi
	 * @param f
	 * @param channel
	 * @param position
	 * @param centreOfMass
	 */
	public DefaultNucleus(@NonNull Roi roi, @NonNull IPoint centreOfMass, @NonNull File f, int channel, int x, int y,
			int number, RuleSetCollection rsc) {
		this(roi, centreOfMass, f, channel, x, y, number, null, rsc);
	}

	/**
	 * Construct from a template Nucleus
	 * 
	 * @param n the template
	 * @throws UnprofilableObjectException
	 * @throws ComponentCreationException
	 */
	protected DefaultNucleus(@NonNull Nucleus n) throws UnprofilableObjectException, ComponentCreationException {
		super(n);
		nucleusNumber = n.getNucleusNumber();
		signalCollection = n.getSignalCollection().duplicate();
		signalCollection.addNuclearSignalAddedListener(this);

		for (OrientationMark s : OrientationMark.values()) {
			if (n.getLandmark(s) != null)
				orientationMarks.put(s, n.getLandmark(s));
		}

		priorityAxis = n.getPriorityAxis();
	}

	/**
	 * Construct from an XML element. Use for unmarshalling. The element should
	 * conform to the specification in {@link XmlSerializable}.
	 * 
	 * @param e the XML element containing the data.
	 */
	public DefaultNucleus(Element e) throws ComponentCreationException {
		super(e);
		nucleusNumber = Integer.valueOf(e.getAttributeValue(XML_NUCLEUS_NUMBER));

		for (Element el : e.getChildren(XML_ORIENTATION)) {
			OrientationMark name = OrientationMark.valueOf(el.getAttributeValue("name"));
			Landmark l = this.getLandmarks().keySet().stream()
					.filter(lm -> lm.getName().equals(el.getAttributeValue("value"))).findFirst().get();
			orientationMarks.put(name, l);
		}
		priorityAxis = PriorityAxis.valueOf(e.getAttributeValue(XML_PRIORITY_AXIS));
		signalCollection = new DefaultSignalCollection(e.getChild(XML_SIGNAL_COLLECTION));
		signalCollection.addNuclearSignalAddedListener(this);
	}

	@Override
	public Element toXmlElement() {
		Element e = super.toXmlElement().setName("Nucleus").setAttribute(XML_PRIORITY_AXIS, priorityAxis.toString())
				.setAttribute(XML_NUCLEUS_NUMBER, String.valueOf(nucleusNumber));

		for (Entry<OrientationMark, Landmark> entry : orientationMarks.entrySet()) {
			e.addContent(new Element(XML_ORIENTATION).setAttribute("name", entry.getKey().name()).setAttribute("value",
					entry.getValue().toString()));
		}

		e.addContent(signalCollection.toXmlElement());

		return e;
	}

	@Override
	public Nucleus duplicate() {
		try {
			return new DefaultNucleus(this);
		} catch (UnprofilableObjectException | ComponentCreationException e) {
			LOGGER.severe("Could not duplicate cell: " + e.getMessage());
		}
		return null;
	}

	@Override
	public @Nullable Landmark getLandmark(OrientationMark s) {
		return orientationMarks.get(s);
	}

	@Override
	public @Nullable PriorityAxis getPriorityAxis() {
		return priorityAxis;
	}

	@Override
	public void createProfiles(double proportion) throws ComponentCreationException {
		super.createProfiles(proportion);
	}

	@Override
	public int getNucleusNumber() {
		return nucleusNumber;
	}

	@Override
	public String getNameAndNumber() {
		return getSourceFileName() + "-" + getNucleusNumber();
	}

	@Override
	public String getPathAndNumber() {
		return getSourceFile() + File.separator + nucleusNumber;
	}

	@Override
	public void setScale(double scale) {
		super.setScale(scale);

		for (INuclearSignal s : this.getSignalCollection().getAllSignals()) {
			s.setScale(scale);
		}
	}

	protected void setSignals(ISignalCollection collection) {
		signalCollection = collection.duplicate();
		signalCollection.addNuclearSignalAddedListener(this);
	}

	@Override
	public ISignalCollection getSignalCollection() {
		return signalCollection;
	}

	public void updateSignalAngle(UUID channel, int signal, double angle) {
		signalCollection.getSignals(channel).get(signal).setMeasurement(Measurement.ANGLE, angle);
	}

	// do not move this into SignalCollection - it is overridden in
	// RodentSpermNucleus
//	@Override
//	public void calculateSignalAnglesFromPoint(@NonNull IPoint p) {
//
//		try {
//			getOrientedNucleus();
//		} catch (MissingLandmarkException | ComponentCreationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		for (UUID signalGroup : signalCollection.getSignalGroupIds()) {
//
//			if (signalCollection.hasSignal(signalGroup)) {
//				for (INuclearSignal s : signalCollection.getSignals(signalGroup)) {
//
//					double angle = this.getCentreOfMass().findAbsoluteAngle(p, s.getCentreOfMass());
//					if (orientedNucleus.getFlipState().h()) // account for horizontal flip
//						s.setMeasurement(Measurement.ANGLE, 180 - angle);
//					else
//						s.setMeasurement(Measurement.ANGLE, angle);
//				}
//			}
//		}
//	}

	@Override
	public void setLandmark(@NonNull Landmark lm, int newLmIndex)
			throws MissingProfileException, MissingLandmarkException, ProfileException {
		super.setLandmark(lm, newLmIndex);

		// If any of the updated landmarks affect
		// the orientation, clear the cached data
		if (orientationMarks.values().contains(lm))
			orientedNucleus = null;
	}

	@Override
	public void setSegments(@NonNull List<IProfileSegment> segs) throws MissingLandmarkException, ProfileException {
		super.setSegments(segs);

		// New segments must be drawn when we get the nucleus
		orientedNucleus = null;
	}

	@Override
	public Nucleus getOrientedNucleus() throws MissingLandmarkException, ComponentCreationException {
		// Make an exact copy of the nucleus
		// and cache
		if (orientedNucleus == null) {
			orientedNucleus = this.duplicate();
			orientedNucleus.orient();
		}

		return orientedNucleus;
	}

	@Override
	public void moveCentreOfMass(@NonNull IPoint point) {
		double diffX = point.getX() - getCentreOfMass().getX();
		double diffY = point.getY() - getCentreOfMass().getY();
		offset(diffX, diffY);
	}

	@Override
	public void offset(double xOffset, double yOffset) {

		super.offset(xOffset, yOffset);

		// Move signals within the nucleus
		if (signalCollection != null) {
			for (INuclearSignal s : this.signalCollection.getAllSignals()) {
				s.offset(xOffset, yOffset);
			}
		}
	}

	/*
	 * ############################################# Methods implementing the
	 * Rotatable interface #############################################
	 */

	@Override
	public void orient() throws MissingLandmarkException {
		ComponentOrienter.orient(this);
	}

	@Override
	public List<OrientationMark> getOrientationMarks() {
		List<OrientationMark> result = new ArrayList<>();
		result.addAll(orientationMarks.keySet());
		return result;
	}

	@Override
	public void flipHorizontal(@NonNull IPoint p) {
		super.flipHorizontal(p);

		for (UUID id : signalCollection.getSignalGroupIds()) {
			signalCollection.getSignals(id).stream().forEach(s -> s.flipHorizontal(p));
		}

	}

	@Override
	public void flipVertical(@NonNull IPoint p) {
		super.flipVertical(p);

		for (UUID id : signalCollection.getSignalGroupIds()) {
			signalCollection.getSignals(id).stream().forEach(s -> s.flipVertical(p));
		}

	}

	@Override
	public void rotate(double angle) {

		super.rotate(angle);

		if (angle != 0) {

			for (UUID id : signalCollection.getSignalGroupIds()) {

				signalCollection.getSignals(id).parallelStream().forEach(s -> {

					s.rotate(angle);

					// get the new signal centre of mass based on the nucleus rotation

					IPoint p = AngleTools.rotateAboutPoint(s.getCentreOfMass(), getCentreOfMass(), angle);
					s.moveCentreOfMass(p);
				});

			}
		}
	}

	/**
	 * Describes the nucleus state
	 * 
	 * @return
	 */
	@Override
	public String toString() {
		String newLine = System.getProperty("line.separator");
		StringBuilder builder = new StringBuilder(super.toString() + newLine);

		builder.append("Name: " + this.getNameAndNumber());
		builder.append(newLine);
		builder.append("Signals: " + this.getSignalCollection().toString());
		builder.append(newLine);
		return builder.toString();
	}

	@Override
	public int compareTo(Nucleus n) {

		int number = this.getNucleusNumber();
		String name = this.getSourceFileNameWithoutExtension();

		// Compare on image name.
		// If that is equal, compare on nucleus number

		int byName = name.compareTo(n.getSourceFileNameWithoutExtension());

		if (byName == 0) {

			if (number < n.getNucleusNumber()) {
				return -1;
			} else if (number > n.getNucleusNumber()) {
				return 1;
			} else {
				return 0;
			}

		}
		return byName;

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(nucleusNumber, orientationMarks, priorityAxis, signalCollection);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass())
			return false;
		DefaultNucleus other = (DefaultNucleus) obj;

		return nucleusNumber == other.nucleusNumber && Objects.equals(orientationMarks, other.orientationMarks)
				&& priorityAxis == other.priorityAxis && Objects.equals(signalCollection, other.signalCollection);
	}

	@Override
	public void nuclearSignalAdded() {
		orientedNucleus = null; // we need to clear it so signals will be duplicated with it
	}
}
