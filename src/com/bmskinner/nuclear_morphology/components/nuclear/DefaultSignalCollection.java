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
package com.bmskinner.nuclear_morphology.components.nuclear;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
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
	
	private static final Logger LOGGER = Logger.getLogger(DefaultSignalCollection.class.getName());
	
    private static final long serialVersionUID = 1L;

    /** Holds the signals */
    private Map<UUID, List<INuclearSignal>> collection = new LinkedHashMap<>();

    /**
     * Create an empty signal collection
     * 
     * @param s
     */
    public DefaultSignalCollection() {
    }

    /**
     * Duplicate a signal collection
     * 
     * @param s
     */
    public DefaultSignalCollection(@NonNull ISignalCollection s) {

        for (UUID group : s.getSignalGroupIds()) {

            List<INuclearSignal> list = new ArrayList<>();
            for (INuclearSignal signal : s.getSignals(group))
                list.add(signal.duplicate());

            collection.put(group, list);
        }

    }

	@Override
	public ISignalCollection duplicate() {
		return new DefaultSignalCollection(this);
	}
    
    @Override
    public void addSignalGroup(@NonNull List<INuclearSignal> list, @NonNull UUID groupID, @NonNull File sourceFile, int sourceChannel) {
        if (list == null || Integer.valueOf(sourceChannel) == null || sourceFile == null || groupID == null) {
            throw new IllegalArgumentException("Signal list or channel is null");
        }

        collection.put(groupID, list);
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
        if (signalGroup == null)
            throw new IllegalArgumentException("Group is null");

        if (collection.get(signalGroup) == null) {
            List<INuclearSignal> list = new ArrayList<>();
            collection.put(signalGroup, list);
        }
        collection.get(signalGroup).add(n);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#addSignals(java.util.List,
     * java.util.UUID)
     */
    @Override
    public void addSignals(@NonNull List<INuclearSignal> list, @NonNull UUID signalGroup) {
        if (list == null)
            throw new IllegalArgumentException("Signal is null");

        if (signalGroup == null)
            throw new IllegalArgumentException("Group is null");
        
        collection.get(signalGroup).addAll(list);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#getSignals()
     */
    @Override
    public List<List<INuclearSignal>> getSignals() {
        List<List<INuclearSignal>> result = new ArrayList<List<INuclearSignal>>(0);
        for (UUID signalGroup : this.getSignalGroupIds()) {
            result.add(getSignals(signalGroup));
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#getAllSignals()
     */
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
        if (signalGroup == null) {
            throw new IllegalArgumentException("Signal group is null");
        }
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

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#numberOfSignals(java.util.UUID)
     */
    @Override
    public int numberOfSignals(@NonNull UUID signalGroup) {
        if (signalGroup == null)
            return 0;
        if (this.hasSignal(signalGroup))
            return collection.get(signalGroup).size();
		return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#removeSignals()
     */
    @Override
    public void removeSignals() {
        collection = new LinkedHashMap<UUID, List<INuclearSignal>>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#removeSignals(java.util.UUID)
     */
    @Override
    public void removeSignals(@NonNull UUID signalGroup) {
        collection.remove(signalGroup);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.nuclear.ISignalCollection#getStatistics(stats.SignalStatistic,
     * components.generic.MeasurementScale, java.util.UUID)
     */
    @Override
    public List<Double> getStatistics(@NonNull PlottableStatistic stat, MeasurementScale scale, @NonNull UUID signalGroup) {
        List<INuclearSignal> list = getSignals(signalGroup);
        List<Double> result = new ArrayList<Double>(0);
        for (int i = 0; i < list.size(); i++) {
            result.add(list.get(i).getStatistic(stat, scale));
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#getImage(java.util.UUID)
     */
    @Override
    public ImageProcessor getImage(@NonNull final UUID signalGroup) throws UnloadableImageException {

        if (signalGroup == null)
            throw new UnloadableImageException("Signal group is null");

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
                                value = PlottableStatistic.DISTANCE_FROM_COM.convert(value, col.getScale(), scale);
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
                smallest = PlottableStatistic.DISTANCE_FROM_COM.convert(smallest, scaleFactor, scale);

                // Assign the pairwise distance
                PairwiseSignalDistanceValue p = new PairwiseSignalDistanceValue(id1, id2, smallest);
                result.add(p);

            }
            completeIDs.add(id1);
        }
        return result;
    }

    /**
     * Calculate the shortest distances between signals in the given signal
     * groups. Each signal is considered only once. Hence a group with 4 signals
     * compared to a group with 3 signals will produce a list of 3 values.
     * 
     * @param id1 the first signal group
     * @param id2  the second signal group
     * @return a list of the pixel distances between paired signals
     */
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
     * @param d1  the nuclear signals in group 1
     * @param d2 the nuclear signals in group 2
     * @param scale  the measurement scale
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

        StringBuilder b = new StringBuilder("Signal groups: ");
        b.append(size());
        b.append("\n ");

        for (UUID group : collection.keySet()) {
            b.append(group);
            b.append(": ");
            b.append(" : Channel: ");
            b.append(this.getSourceChannel(group));
            b.append(" : File: ");
            b.append(this.getSourceFile(group));
            b.append("\n");
        }
        return b.toString();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((collection == null) ? 0 : collection.hashCode());
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
		} else if (!collection.equals(other.collection))
			return false;
		return true;
	}
    
    


}
