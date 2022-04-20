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
package com.bmskinner.nuclear_morphology.components.signals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

/**
 * The default implementation of the {@link ISignalCollection} interface
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultSignalCollection implements ISignalCollection {

	private static final String XML_COLLECTION = "SignalCollection";
	private static final String XML_SIGNALS = "Signals";
	private static final String XML_SIGNALGROUP_ID = "group";

	private static final Logger LOGGER = Logger.getLogger(DefaultSignalCollection.class.getName());

	/** Holds the signals */
	private Map<UUID, List<INuclearSignal>> collection = new LinkedHashMap<>();

	private List<NuclearSignalAddedListener> listeners = new ArrayList<>();

	/**
	 * Create an empty signal collection
	 * 
	 * @param s
	 */
	public DefaultSignalCollection() {
	}

	@Override
	public void addNuclearSignalAddedListener(NuclearSignalAddedListener l) {
		listeners.add(l);
	}

	@Override
	public void removeNuclearSignalAddedListener(NuclearSignalAddedListener l) {
		listeners.remove(l);
	}

	/**
	 * Construct from an XML element. Use for unmarshalling. The element should
	 * conform to the specification in {@link XmlSerializable}.
	 * 
	 * @param e the XML element containing the data.
	 */
	public DefaultSignalCollection(Element e) {
		for (Element id : e.getChildren(XML_SIGNALS)) {
			UUID uuid = UUID.fromString(id.getAttributeValue(XML_SIGNALGROUP_ID));
			collection.computeIfAbsent(uuid, k -> new ArrayList<>());

			for (Element s : id.getChildren()) {
				collection.get(uuid).add(new DefaultNuclearSignal(s));
			}
		}
	}

	@Override
	public Element toXmlElement() {
		Element e = new Element(XML_COLLECTION);

		for (Entry<UUID, List<INuclearSignal>> entry : collection.entrySet()) {

			for (INuclearSignal s : entry.getValue()) {
				e.addContent(new Element(XML_SIGNALS).setAttribute(XML_SIGNALGROUP_ID, entry.getKey().toString())
						.addContent(s.toXmlElement()));
			}
		}
		return e;
	}

	/**
	 * Duplicate a signal collection
	 * 
	 * @param s
	 */
	private DefaultSignalCollection(@NonNull ISignalCollection s) {
		for (UUID group : s.getSignalGroupIds()) {
			collection.computeIfAbsent(group, k -> new ArrayList<>());
			for (INuclearSignal signal : s.getSignals(group))
				collection.get(group).add(signal.duplicate());
		}
	}

	@Override
	public ISignalCollection duplicate() {
		return new DefaultSignalCollection(this);
	}

	@Override
	public void addSignalGroup(@NonNull List<INuclearSignal> list, @NonNull UUID groupID) {
		collection.put(groupID, list);
		fireNuclearSignalAdded();
	}

	@Override
	public Set<UUID> getSignalGroupIds() {
		return collection.keySet();
	}

	@Override
	public void updateSignalGroupId(@NonNull UUID oldID, @NonNull UUID newID) {

		if (!collection.containsKey(oldID))
			return;

		List<INuclearSignal> list = collection.get(oldID);

		collection.remove(oldID);
		collection.put(newID, list);
	}

	@Override
	public void addSignal(@NonNull INuclearSignal n, @NonNull UUID signalGroup) {
		if (collection.get(signalGroup) == null) {
			List<INuclearSignal> list = new ArrayList<>();
			collection.put(signalGroup, list);
		}
		collection.get(signalGroup).add(n);
		fireNuclearSignalAdded();
	}

	@Override
	public void addSignals(@NonNull List<INuclearSignal> list, @NonNull UUID signalGroup) {
		collection.get(signalGroup).addAll(list);
		fireNuclearSignalAdded();
	}

	private void fireNuclearSignalAdded() {
		for (NuclearSignalAddedListener l : listeners)
			l.nuclearSignalAdded();
	}

	@Override
	public List<List<INuclearSignal>> getSignals() {
		List<List<INuclearSignal>> result = new ArrayList<List<INuclearSignal>>(0);
		for (UUID signalGroup : this.getSignalGroupIds()) {
			result.add(getSignals(signalGroup));
		}
		return result;
	}

	@Override
	public List<INuclearSignal> getAllSignals() {
		List<INuclearSignal> result = new ArrayList<>(0);
		for (UUID signalGroup : this.getSignalGroupIds()) {
			result.addAll(getSignals(signalGroup));
		}
		return result;
	}

	@Override
	public List<INuclearSignal> getSignals(@NonNull UUID signalGroup) {
		if (this.hasSignal(signalGroup))
			return this.collection.get(signalGroup);
		return new ArrayList<>(0);
	}

	@Override
	public File getSourceFile(@NonNull UUID signalGroup) {
		if (collection.containsKey(signalGroup)) {
			List<INuclearSignal> list = collection.get(signalGroup);
			if (list != null && !list.isEmpty())
				return list.get(0).getSourceFile();
		}
		return null;
	}

	@Override
	public void updateSourceFile(@NonNull UUID signalGroup, @NonNull File f) {
		for (INuclearSignal s : collection.get(signalGroup)) {
			s.setSourceFile(f);
		}
	}

	@Override
	public int getSourceChannel(@NonNull UUID signalGroup) {
		if (collection.containsKey(signalGroup)) {
			List<INuclearSignal> list = collection.get(signalGroup);
			if (list != null && !list.isEmpty())
				return list.get(0).getChannel();
		}
		return -1;
	}

	@Override
	public int size() {
		return collection.size();
	}

	@Override
	public int numberOfSignals() {
		int count = 0;
		for (UUID group : collection.keySet()) {
			count += numberOfSignals(group);
		}
		return count;
	}

	@Override
	public boolean hasSignal(@NonNull UUID signalGroup) {
		if (!collection.containsKey(signalGroup)) {
			return false;
		}
		if (collection.get(signalGroup).isEmpty()) {
			return false;
		}
		return true;
	}

	@Override
	public boolean hasSignal() {
		return !collection.isEmpty();
	}

	@Override
	public int numberOfSignals(@NonNull UUID signalGroup) {
		if (this.hasSignal(signalGroup))
			return collection.get(signalGroup).size();
		return 0;
	}

	@Override
	public void removeSignals() {
		collection = new LinkedHashMap<UUID, List<INuclearSignal>>();
	}

	@Override
	public void removeSignals(@NonNull UUID signalGroup) {
		collection.remove(signalGroup);
	}

	@Override
	public List<Double> getStatistics(@NonNull Measurement stat, MeasurementScale scale, @NonNull UUID signalGroup) {
		List<INuclearSignal> list = getSignals(signalGroup);
		List<Double> result = new ArrayList<Double>(0);
		for (int i = 0; i < list.size(); i++) {
			result.add(list.get(i).getMeasurement(stat, scale));
		}
		return result;
	}

	@Override
	public ImageProcessor getImage(@NonNull final UUID signalGroup) throws UnloadableImageException {

		File f = this.getSourceFile(signalGroup);

		// Will be null if no signals were reported for the cell with this
		// collection
		if (f == null)
			throw new UnloadableImageException("File for signal group is null");

		int c = this.getSourceChannel(signalGroup);

		try {
			return new ImageImporter(f).importImageAndInvert(c);
		} catch (ImageImportException e) {
			LOGGER.log(Loggable.STACK, "Error importing image source file " + f.getAbsolutePath(), e);
			throw new UnloadableImageException("Unable to load signal image", e);
		}
	}

	/**
	 * Calculate the pairwise distances between all signals in the nucleus
	 */
	@Override
	public double[][] calculateDistanceMatrix(MeasurementScale scale) {

		// create a matrix to hold the data
		// needs to be between every signal and every other signal, irrespective
		// of colour
		int matrixSize = numberOfSignals();

		double[][] matrix = new double[matrixSize][matrixSize];

		int matrixRow = 0;
		int matrixCol = 0;

		for (List<INuclearSignal> signalsRow : getSignals()) {

			if (!signalsRow.isEmpty()) {

				for (INuclearSignal row : signalsRow) {

					matrixCol = 0;

					IPoint aCoM = row.getCentreOfMass();

					for (List<INuclearSignal> signalsCol : getSignals()) {

						if (!signalsCol.isEmpty()) {

							for (INuclearSignal col : signalsCol) {
								IPoint bCoM = col.getCentreOfMass();
								double value = aCoM.getLengthTo(bCoM);
								value = Measurement.DISTANCE_FROM_COM.convert(value, col.getScale(), scale);
								matrix[matrixRow][matrixCol] = value;
								matrixCol++;
							}

						}

					}
					matrixRow++;
				}
			}
		}
		return matrix;
	}

	/**
	 * For each signal group pair, find the smallest pairwise distance between
	 * signals in the collection.
	 * 
	 * @return a list of shortest distances for each pairwise group
	 */
	@Override
	public List<PairwiseSignalDistanceValue> calculateSignalColocalisation(MeasurementScale scale) {

		List<PairwiseSignalDistanceValue> result = new ArrayList<PairwiseSignalDistanceValue>();

		Set<UUID> completeIDs = new HashSet<UUID>();

		for (UUID id1 : this.getSignalGroupIds()) {

			if (!this.hasSignal(id1)) {
				continue;
			}

			List<INuclearSignal> signalList1 = this.getSignals(id1);

			for (UUID id2 : this.getSignalGroupIds()) {

				if (id1.equals(id2)) {
					continue;
				}

				if (!this.hasSignal(id2)) {
					continue;
				}

				List<INuclearSignal> signalList2 = this.getSignals(id2);

				// Compare all signal pairwise distances between groups 1 and 2
				double smallest = Double.MAX_VALUE;
				double scaleFactor = 1;
				for (INuclearSignal s1 : signalList1) {
					scaleFactor = s1.getScale();
					for (INuclearSignal s2 : signalList2) {

						double distance = s1.getCentreOfMass().getLengthTo(s2.getCentreOfMass());
						smallest = distance < smallest ? distance : smallest;

					}

				}

				// Use arbitrary distance measure to convert scale
				smallest = Measurement.DISTANCE_FROM_COM.convert(smallest, scaleFactor, scale);

				// Assign the pairwise distance
				PairwiseSignalDistanceValue p = new PairwiseSignalDistanceValue(id1, id2, smallest);
				result.add(p);

			}
			completeIDs.add(id1);
		}
		return result;
	}

	/**
	 * Calculate the shortest distances between signals in the given signal groups.
	 * Each signal is considered only once. Hence a group with 4 signals compared to
	 * a group with 3 signals will produce a list of 3 values.
	 * 
	 * @param id1 the first signal group
	 * @param id2 the second signal group
	 * @return a list of the pixel distances between paired signals
	 */
	@Override
	public List<Colocalisation<INuclearSignal>> calculateColocalisation(@NonNull UUID id1, @NonNull UUID id2) {

		if (id1.equals(id2)) {
			throw new IllegalArgumentException("Signal IDs are the same");
		}

		Set<INuclearSignal> d1 = new HashSet<INuclearSignal>(this.getSignals(id1));
		Set<INuclearSignal> d2 = new HashSet<INuclearSignal>(this.getSignals(id2));

		List<Colocalisation<INuclearSignal>> result = findColocalisingSignals(d1, d2);

		return result;

	}

	/**
	 * Recursively find signal pairs with the shortest distance between them.
	 * 
	 * @param d1    the nuclear signals in group 1
	 * @param d2    the nuclear signals in group 2
	 * @param scale the measurement scale
	 * @return a list of best colocalising signals
	 */
	private List<Colocalisation<INuclearSignal>> findColocalisingSignals(@NonNull Set<INuclearSignal> d1,
			@NonNull Set<INuclearSignal> d2) {

		List<Colocalisation<INuclearSignal>> result = new ArrayList<Colocalisation<INuclearSignal>>();

		if (d2.isEmpty() || d1.isEmpty()) {
			return result;
		}

		double smallest = Double.MAX_VALUE;

		INuclearSignal chosen1 = null, chosen2 = null;

		// Check all pairwise comparisons before returning a Colocalisation
		// in case the set lengths are unequal
		Iterator<INuclearSignal> it1 = d1.iterator();
		while (it1.hasNext()) {

			INuclearSignal s1 = it1.next();

			Iterator<INuclearSignal> it2 = d2.iterator();
			while (it2.hasNext()) {
				INuclearSignal s2 = it2.next();
				double distance = s1.getCentreOfMass().getLengthTo(s2.getCentreOfMass());

				boolean smaller = distance < smallest;

				// Replace selected signals if closer
				smallest = smaller ? distance : smallest;
				chosen2 = smaller ? s2 : chosen2;
				chosen1 = smaller ? s1 : chosen1;
			}
		}

		// Make a cColocalisation from the best pair

		if (chosen1 != null && chosen2 != null) {

			Colocalisation<INuclearSignal> col = new Colocalisation<INuclearSignal>(chosen1, chosen2);
			d1.remove(chosen1);
			d2.remove(chosen2);
			result.add(col);

			if (!d1.isEmpty() && !d2.isEmpty()) {
				result.addAll(findColocalisingSignals(d1, d2));
			}
		}

		return result;
	}

	@Override
	public String toString() {

		StringBuilder b = new StringBuilder("Signal groups: " + size() + "\n");

		for (UUID group : collection.keySet()) {
			b.append(group + ": Channel " + getSourceChannel(group) + "; File: " + getSourceFile(group) + "; size "
					+ numberOfSignals(group));
			b.append("\n");
			for (INuclearSignal s : getSignals(group)) {
				b.append(s.toString() + "\n");
			}
		}
		return b.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((collection == null) ? 0 : Objects.hashCode(collection));
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
		DefaultSignalCollection other = (DefaultSignalCollection) obj;
		if (collection == null) {
			if (other.collection != null)
				return false;
		}

		// Deep check on signals
		if (collection.size() != other.collection.size())
			return false;

		for (Entry<UUID, List<INuclearSignal>> e : collection.entrySet()) {
			if (!other.collection.containsKey(e.getKey()))
				return false;
			List<INuclearSignal> otherSignals = other.collection.get(e.getKey());
			if (e.getValue().size() != otherSignals.size())
				return false;
			if (!Objects.equals(e.getValue(), otherSignals))
				return false;
		}

//		return Objects.equals(collection, other.collection);
//		} else if (!collection.equals(other.collection))
//			return false;
		return true;
	}

}
