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
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.components.measure.DefaultMeasurement;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.measure.MissingMeasurementException;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.io.XmlSerializable;

/**
 * The cell is the highest level of analysis here. Cells we can analyse have a
 * nucleus, mitochondria, cytoplasm, and maybe a flagellum and acrosome.
 * 
 * @author bms41
 * @since 1.13.3
 */
public class DefaultCell implements ICell {

	private static final Logger LOGGER = Logger.getLogger(DefaultCell.class.getName());

	protected UUID uuid;
	protected ICytoplasm cytoplasm = null;
	protected List<Nucleus> nuclei = new ArrayList<>();

	/** The statistical values stored for this object */
	private Map<Measurement, Double> measurements = new HashMap<>();

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

		for (Element el : e.getChildren(XMLNames.XML_NUCLEUS)) {
			nuclei.add(new DefaultNucleus(el));
		}

		// Add measurements
		for (Element el : e.getChildren(XMLNames.XML_MEASUREMENT)) {
			Measurement m = new DefaultMeasurement(el);
			measurements.put(m, Double.parseDouble(el.getAttributeValue(XMLNames.XML_VALUE)));
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
		for (Nucleus m : c.getNuclei()) {
			nuclei.add(m.duplicate());
		}

		if (c.hasCytoplasm())
			this.cytoplasm = c.getCytoplasm().duplicate();

		measurements = new HashMap<>();
		for (Measurement stat : c.getMeasurements())
			try {
				measurements.put(stat, c.getMeasurement(stat));
			} catch (MissingDataException | ComponentCreationException | SegmentUpdateException e) {
				LOGGER.log(Level.SEVERE, "Error copying measurement to new cell", e);
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
		double sc = chooseScale();

		if (!measurements.containsKey(stat))
			throw new MissingMeasurementException("Measurement '%s' not present".formatted(stat));

		double result = measurements.get(stat);
		return stat.convert(result, sc, scale);
	}

	private double chooseScale() {

		if (hasNucleus()) {
			return getPrimaryNucleus().getScale();
		}
		if (hasCytoplasm()) {
			return getCytoplasm().getScale();
		}

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
		measurements.clear();
		for (Measurement m : Measurement.getCellStats())
			try {
				measurements.put(m, ComponentMeasurer.calculate(m, this));
			} catch (MissingDataException | ComponentCreationException | SegmentUpdateException e) {
				LOGGER.log(Level.SEVERE,
						"Unable to calculate cell measurements when adding new nucleus", e);
			}

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
	}

	@Override
	public List<Taggable> getTaggables() {
		List<Taggable> result = new ArrayList<>(0);
		result.addAll(getTaggables(nuclei));

		if (hasCytoplasm()) {
			if (cytoplasm instanceof Taggable) {
				result.add((Taggable) cytoplasm);
			}
		}

		return result;

	}

	private List<Taggable> getTaggables(List<? extends CellularComponent> l) {
		return l.stream().filter(e -> e instanceof Taggable).map(e -> (Taggable) e)
				.collect(Collectors.toList());
	}

	@Override
	public void setScale(double scale) {
		nuclei.stream().forEach(n -> n.setScale(scale));
		if (cytoplasm != null)
			cytoplasm.setScale(scale);
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
		List<Nucleus> other = o.getNuclei();

		for (int i = 0; i < other.size(); i++) {
			val += this.getNuclei().get(i).compareTo(other.get(i));
		}

		return val;
	}

	@Override
	public Element toXmlElement() {
		Element e = new Element(XMLNames.XML_CELL).setAttribute(XMLNames.XML_ID, uuid.toString());
		for (Entry<Measurement, Double> entry : measurements.entrySet()) {
			e.addContent(entry.getKey().toXmlElement().setAttribute(XMLNames.XML_VALUE,
					entry.getValue().toString()));
		}

		if (cytoplasm != null)
			e.addContent(cytoplasm.toXmlElement());

		for (Nucleus n : nuclei) {
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
		DefaultCell other = (DefaultCell) obj;
		return Objects.equals(cytoplasm, other.cytoplasm) && Objects.equals(nuclei, other.nuclei)
				&& Objects.equals(measurements, other.measurements)
				&& Objects.equals(uuid, other.uuid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(cytoplasm, nuclei, measurements, uuid);
	}

	@Override
	public String toString() {
		return "Cell " + this.uuid.toString() + ":\n" + measurements.toString() + "\n"
				+ nuclei.toString() + "\n";
	}

}
