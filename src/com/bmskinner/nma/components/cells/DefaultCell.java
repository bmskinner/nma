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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nma.components.ComponentMeasurer;
import com.bmskinner.nma.components.ComponentUpdateListener;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.components.measure.ArrayMeasurement;
import com.bmskinner.nma.components.measure.DefaultMeasurement;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.measure.MissingMeasurementException;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.io.XMLReader;
import com.bmskinner.nma.io.XmlSerializable;
import com.bmskinner.nma.utility.ArrayUtils;

/**
 * The cell is the highest level of analysis here. Cells we can analyse have a
 * nucleus, mitochondria, cytoplasm, and maybe a flagellum and acrosome.
 * 
 * @author Ben Skinner
 * @since 1.13.3
 */
public class DefaultCell implements ICell {

	private static final Logger LOGGER = Logger.getLogger(DefaultCell.class.getName());

	protected UUID uuid;
	protected ICytoplasm cytoplasm = null;
	protected List<Nucleus> nuclei = new ArrayList<>();

	transient protected boolean isRecalcHashcode = true;
	transient protected int hashcodeCache = 0;

	/** The statistical values stored for this object */
	private final Map<Measurement, Double> measurements = new HashMap<>();

	/**
	 * Measurements stored for this object that are associated with an array instead
	 * of single values e.g. histograms
	 */
	private final Map<Measurement, List<Double>> arrayMeasurements = new HashMap<>();

	transient protected List<ComponentUpdateListener> componentUpdateListeners = new ArrayList<>();

	/**
	 * Create a new cell with a random ID
	 */
	protected DefaultCell() {
		this(UUID.randomUUID());
	}


	/**
	 * Construct from an XML element. Use for unmarshalling. The element should
	 * conform to the specification in {@link XmlSerializable}.
	 * 
	 * @param e the XML element containing the data.
	 */
	public DefaultCell(@NonNull Element e) throws ComponentCreationException {
		uuid = UUID.fromString(e.getAttributeValue(XMLNames.XML_ID));

		for (final Element el : e.getChildren(XMLNames.XML_NUCLEUS)) {
			final Nucleus n = new DefaultNucleus(el);
			n.addComponentUpdateListener(this);
			nuclei.add(n);
		}

		// Add measurements
		for (final Element el : e.getChildren(XMLNames.XML_MEASUREMENT)) {
			final Measurement m = new DefaultMeasurement(el);
			measurements.put(m, Double.parseDouble(el.getAttributeValue(XMLNames.XML_VALUE)));
		}

		for (final Element el : e.getChildren(XMLNames.XML_ARRAY_MEASUREMENT)) {
			final Measurement m = new ArrayMeasurement(el);
			final double[] values = XMLReader.parseDoubleArray(el.getAttributeValue(XMLNames.XML_VALUE));
			arrayMeasurements.put(m, ArrayUtils.toMutableList(values));
		}

	}

	/**
	 * Create a new cell with the given ID.
	 * 
	 * @param id the id for the new cell
	 */
	public DefaultCell(@NonNull UUID id) {
		this.uuid = id;
	}

	/**
	 * Create a new cell based on the given nucleus. The nucleus is NOT copied.
	 * 
	 * @param n the template nucleus for the cell
	 */
	public DefaultCell(@NonNull Nucleus n) {
		this();
		addNucleus(n);
	}

	/**
	 * Duplicate a cell. The ID is kept consistent
	 * 
	 * @param c the cell to duplicate
	 * @throws ComponentCreationException
	 */
	public DefaultCell(@NonNull ICell c) {

		this.uuid = c.getId();

		nuclei = new ArrayList<>(0);
		for (final Nucleus m : c.getNuclei()) {
			final Nucleus n = m.duplicate();
			n.addComponentUpdateListener(this);
			nuclei.add(n);
		}

		if (c.hasCytoplasm()) {
			cytoplasm = c.getCytoplasm().duplicate();
			cytoplasm.addComponentUpdateListener(this);
		}

		for (final Measurement stat : c.getMeasurements()) {
			try {
				if (stat.isArrayMeasurement()) {
					// ensure we have a separate copy of the data
					final double[] tmp = ArrayUtils.toArray(c.getArrayMeasurement(stat, MeasurementScale.PIXELS));
					setMeasurement(stat, tmp);
				} else {
					setMeasurement(stat, c.getMeasurement(stat, MeasurementScale.PIXELS));
				}
			} catch (MissingDataException | ComponentCreationException | SegmentUpdateException e) {
				LOGGER.log(Level.SEVERE, "Error copying measurement to new cell", e);
			}
		}
	}

	@Override
	public ICell duplicate() {
		return new DefaultCell(this);
	}

	@Override
	public @NonNull UUID getId() {
		return uuid;
	}

	@Override
	public Nucleus getPrimaryNucleus() {
		return nuclei.get(0);
	}

	@Override
	public List<Nucleus> getNuclei() {
		return nuclei;
	}

	@Override
	public boolean hasMeasurement(@NonNull Measurement measurement) {
		return measurements.containsKey(measurement);
	}

	@Override
	public synchronized double getMeasurement(@NonNull Measurement stat)
			throws MissingMeasurementException {
		return this.getMeasurement(stat, MeasurementScale.PIXELS);
	}

	@Override
	public synchronized double getMeasurement(@NonNull Measurement stat,
			@NonNull MeasurementScale scale) throws MissingMeasurementException {

		// Get the scale of one of the components of the cell
		final double sc = chooseScale();

		if (!measurements.containsKey(stat))
			throw new MissingMeasurementException("Measurement '%s' not present".formatted(stat));

		final double result = measurements.get(stat);
		return stat.convert(result, sc, scale);
	}

	private double chooseScale() {

		if (hasNucleus())
			return getPrimaryNucleus().getScale();
		if (hasCytoplasm())
			return getCytoplasm().getScale();

		return 1d;
	}

	@Override
	public void setMeasurement(@NonNull Measurement stat, double d) {
		measurements.put(stat, d);
	}

	@Override
	public void clearMeasurement(@NonNull Measurement stat) {
		measurements.remove(stat);
	}

	@Override
	public void clearMeasurements() {
		measurements.clear();
	}

	@Override
	public List<Measurement> getMeasurements() {
		return new ArrayList<>(measurements.keySet());
	}

	@Override
	public void addNucleus(Nucleus nucleus) {
		nuclei.add(nucleus);
		nucleus.addComponentUpdateListener(this);
		measurements.clear();
		for (final Measurement m : Measurement.getCellStats()) {
			try {
				measurements.put(m, ComponentMeasurer.calculate(m, this));
			} catch (MissingDataException | ComponentCreationException | SegmentUpdateException e) {
				LOGGER.log(Level.SEVERE,
						"Unable to calculate cell measurements when adding new nucleus", e);
			}
		}
		fireComponentUpdated();
	}

	@Override
	public boolean hasNucleus() {
		return !nuclei.isEmpty();
	}

	@Override
	public ICytoplasm getCytoplasm() {
		return this.cytoplasm;
	}

	@Override
	public boolean hasCytoplasm() {
		return cytoplasm != null;
	}

	@Override
	public void setCytoplasm(ICytoplasm cytoplasm) {
		this.cytoplasm = cytoplasm;
		cytoplasm.addComponentUpdateListener(this);
		fireComponentUpdated();
	}

	@Override
	public List<Taggable> getTaggables() {
		final List<Taggable> result = new ArrayList<>(0);
		result.addAll(getTaggables(nuclei));

		if (hasCytoplasm()) {
			if (cytoplasm instanceof Taggable) {
				result.add((Taggable) cytoplasm);
			}
		}

		return result;

	}

	private List<Taggable> getTaggables(List<? extends CellularComponent> l) {
		return l.stream().filter(Taggable.class::isInstance).map(e -> (Taggable) e)
				.collect(Collectors.toList());
	}

	@Override
	public void setScale(double scale) {
		nuclei.stream().forEach(n -> n.setScale(scale));
		if (cytoplasm != null) {
			cytoplasm.setScale(scale);
		}
		fireComponentUpdated();
	}

	@Override
	public boolean hasNuclearSignals() {
		return getNuclei().stream().anyMatch(n -> n.getSignalCollection().hasSignal());
	}

	@Override
	public boolean hasNuclearSignals(UUID signalGroupId) {
		return getNuclei().stream().anyMatch(n -> n.getSignalCollection().hasSignal(signalGroupId));
	}

	@Override
	public int compareTo(ICell o) {

		if (!this.hasNucleus())
			return -1;

		// If different number of nuclei
		if (this.getNuclei().size() != o.getNuclei().size())
			return this.getNuclei().size() - o.getNuclei().size();

		int val = 0;
		final List<Nucleus> other = o.getNuclei();

		for (int i = 0; i < other.size(); i++) {
			val += this.getNuclei().get(i).compareTo(other.get(i));
		}

		return val;
	}

	@Override
	@NonNull public Element toXmlElement() {
		final Element e = new Element(XMLNames.XML_CELL).setAttribute(XMLNames.XML_ID, uuid.toString());

		for (final Entry<Measurement, Double> entry : measurements.entrySet()) {
			e.addContent(entry.getKey().toXmlElement().setAttribute(XMLNames.XML_VALUE,
					entry.getValue().toString()));
		}

		for (final Entry<Measurement, List<Double>> entry : arrayMeasurements.entrySet()) {
			final double[] values = ArrayUtils.toArray(entry.getValue());
			e.addContent(entry.getKey().toXmlElement().setAttribute(XMLNames.XML_VALUE,
					Arrays.toString(values)));
		}

		if (cytoplasm != null) {
			e.addContent(cytoplasm.toXmlElement());
		}

		for (final Nucleus n : nuclei) {
			e.addContent(n.toXmlElement());
		}

		return e;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final DefaultCell other = (DefaultCell) obj;
		return Objects.equals(cytoplasm, other.cytoplasm) && Objects.equals(nuclei, other.nuclei)
				&& Objects.equals(measurements, other.measurements)
				&& Objects.equals(uuid, other.uuid);
	}

	protected int recalculateHashcodeCache() {
		return Objects.hash(cytoplasm, nuclei, measurements, uuid);
	}

	@Override
	public int hashCode() {
		if(isRecalcHashcode) { // default undeclared value
			hashcodeCache = recalculateHashcodeCache();
			isRecalcHashcode = false;
		}
		return hashcodeCache;
	}

	@Override
	public String toString() {
		return "Cell " + this.uuid.toString() + ":\n" + measurements.toString() + "\n"
				+ nuclei.toString() + "\n";
	}

	@Override
	public void componentUpdated(ComponentUpdateEvent e) {
		// Recalc hash and pass update onwards
		isRecalcHashcode = true;
		fireComponentUpdated();
	}

	@Override
	public void fireComponentUpdated() {
		isRecalcHashcode = true;
		for(final ComponentUpdateListener l : componentUpdateListeners) {
			l.componentUpdated(new ComponentUpdateEvent(this));
		}
	}



	@Override
	public void addComponentUpdateListener(ComponentUpdateListener l) {
		componentUpdateListeners.add(l);
	}



	@Override
	public void removeComponentUpdateListener(ComponentUpdateListener l) {
		componentUpdateListeners.remove(l);
	}

	@Override
	public List<Double> getArrayMeasurement(@NonNull Measurement measurement, @NonNull MeasurementScale scale)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		final double sc = chooseScale();
		if (!this.arrayMeasurements.containsKey(measurement)) {
			setMeasurement(measurement, ComponentMeasurer.calculate(measurement, this));
		}
		return measurement.convert(arrayMeasurements.get(measurement), sc, scale);
	}

	@Override
	public List<Double> getArrayMeasurement(@NonNull Measurement measurement)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		return getArrayMeasurement(measurement, MeasurementScale.PIXELS);
	}

	@Override
	public void setMeasurement(@NonNull Measurement measurement, double[] d) {
		arrayMeasurements.put(measurement, ArrayUtils.toMutableList(d));
		fireComponentUpdated();
	}


}
