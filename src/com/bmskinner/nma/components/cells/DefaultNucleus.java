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
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jdom2.Element;

import com.bmskinner.nma.components.ComponentOrienter;
import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.UnprofilableObjectException;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.components.signals.DefaultSignalCollection;
import com.bmskinner.nma.components.signals.INuclearSignal;
import com.bmskinner.nma.components.signals.ISignalCollection;
import com.bmskinner.nma.components.signals.NuclearSignalAddedListener;
import com.bmskinner.nma.io.XmlSerializable;

import ij.gui.Roi;

/**
 * The standard round nucleus, implementing {@link Nucleus}. All non-round
 * nuclei extend this.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultNucleus extends ProfileableCellularComponent
		implements Nucleus, NuclearSignalAddedListener {

	private static final Logger LOGGER = Logger.getLogger(DefaultNucleus.class.getName());

	private static final String XML_NUCLEUS_NUMBER = "number";

	private static final String XML_SIGNAL_COLLECTION = "SignalCollection";

	/** The number of the nucleus in its image, for display */
	private int nucleusNumber;

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
	 * @throws ComponentCreationException
	 */
	public DefaultNucleus(@NonNull Roi roi, @NonNull IPoint centreOfMass, File source, int channel,
			int number, @Nullable UUID id, RuleSetCollection rsc)
			throws ComponentCreationException {
		super(roi, centreOfMass, source, channel, id, rsc);
		this.nucleusNumber = number;
		signalCollection.addNuclearSignalAddedListener(this);
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
	 * @throws ComponentCreationException
	 */
	public DefaultNucleus(@NonNull Roi roi, @NonNull IPoint centreOfMass, @NonNull File f,
			int channel, int number, RuleSetCollection rsc) throws ComponentCreationException {
		this(roi, centreOfMass, f, channel, number, null, rsc);
	}

	/**
	 * Construct from a template Nucleus
	 * 
	 * @param n the template
	 * @throws UnprofilableObjectException
	 * @throws ComponentCreationException
	 */
	protected DefaultNucleus(@NonNull Nucleus n)
			throws UnprofilableObjectException, ComponentCreationException {
		super(n);
		nucleusNumber = n.getNucleusNumber();
		signalCollection = n.getSignalCollection().duplicate();
		signalCollection.addNuclearSignalAddedListener(this);
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

		signalCollection = new DefaultSignalCollection(e.getChild(XML_SIGNAL_COLLECTION));
		signalCollection.addNuclearSignalAddedListener(this);
	}

	@Override
	public Element toXmlElement() {
		Element e = super.toXmlElement().setName("Nucleus").setAttribute(XML_NUCLEUS_NUMBER,
				String.valueOf(nucleusNumber));

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

	@Override
	public void setLandmark(@NonNull OrientationMark lm, int newLmIndex)
			throws MissingProfileException, MissingLandmarkException, ProfileException {
		super.setLandmark(lm, newLmIndex);

		// If any of the updated landmarks affect
		// the orientation, clear the cached data
		if (orientationMarks.containsKey(lm))
			orientedNucleus = null;
	}

	@Override
	public void setLandmark(@NonNull Landmark lm, int newLmIndex)
			throws MissingProfileException, MissingLandmarkException, ProfileException {
		super.setLandmark(lm, newLmIndex);

		// In case any of the updated landmarks affect
		// the orientation, clear the cached data
		if (profileLandmarks.containsKey(lm))
			orientedNucleus = null;
	}

	@Override
	public void setSegments(@NonNull List<IProfileSegment> segs)
			throws MissingLandmarkException, ProfileException {
		super.setSegments(segs);

		// New segments must be drawn when we get the nucleus
		orientedNucleus = null;
	}

	@Override
	public void clearMeasurements() {
		super.clearMeasurements();

		// Ensure recalculation is on a fresh nucleus
		orientedNucleus = null;
	}

	@Override
	public Nucleus getOrientedNucleus()
			throws MissingLandmarkException, ComponentCreationException {
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

		for (INuclearSignal s : signalCollection.getAllSignals())
			s.flipHorizontal(p);
	}

	@Override
	public void flipVertical(@NonNull IPoint p) {
		super.flipVertical(p);

		for (INuclearSignal s : signalCollection.getAllSignals())
			s.flipVertical(p);

	}

	@Override
	public void rotate(double angle, IPoint anchor) {
		super.rotate(angle, anchor);
		if (angle != 0) {
			// Rotate signals
			for (INuclearSignal s : signalCollection.getAllSignals())
				s.rotate(angle, anchor);

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
		result = prime * result + Objects.hash(nucleusNumber, signalCollection);
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

		return nucleusNumber == other.nucleusNumber
				&& Objects.equals(signalCollection, other.signalCollection);
	}

	@Override
	public void nuclearSignalAdded() {
		orientedNucleus = null; // we need to clear it so signals will be duplicated with it
	}
}
