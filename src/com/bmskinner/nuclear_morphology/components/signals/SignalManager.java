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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementDimension;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.ShrinkType;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.stats.Stats;

import ij.process.ImageProcessor;

/**
 * This class is designed to simplify operations on CellCollections
 * involving signals. It should be accessed via CellCollection.getSignalManager()
 * @author bms41
 *
 */
public class SignalManager {
	
	private static final Logger LOGGER = Logger.getLogger(SignalManager.class.getName());

    private ICellCollection collection;

    public SignalManager(@NonNull final ICellCollection collection) {
        this.collection = collection;
    }

    /**
     * Find cells with or without signals in the given group.
     * 
     * @param signalGroupId the group id
     * @param hasSignal true to retrive cells with signals. False to retrieve cells without signals
     * @return the cells
     */
    public Set<ICell> getCellsWithNuclearSignals(@NonNull final UUID signalGroupId, boolean hasSignal) {
        return collection.streamCells()
                .filter( c->c.hasNuclearSignals(signalGroupId)==hasSignal)
                .collect(Collectors.toSet());
    }
    
    /**
     * Find the number of cells with signals in the given group
     * @param signalGroupId the signal group
     * @return the number of cells in the collection with a signal in the group
     */
    public int getNumberOfCellsWithNuclearSignals(@NonNull final UUID signalGroupId) {
        return (int) collection.streamCells().filter(c->c.hasNuclearSignals(signalGroupId)).count();
    }

    /**
     * Get the number of signal groups in the cell collection
     * 
     * @return the number of signal groups
     */
    public int getSignalGroupCount() {
        int count = getSignalGroupIDs().size();
        if (this.hasSignals(IShellResult.RANDOM_SIGNAL_ID)) {
            count--;
        }
        return count;
    }

    /**
     * Fetch the signal group ids in this collection. This does not include any
     * random signals created in shell analysis
     * 
     * @return
     */
    public Set<UUID> getSignalGroupIDs() {

        Set<UUID> ids = new HashSet<>(collection.getSignalGroupIDs());
        ids.remove(IShellResult.RANDOM_SIGNAL_ID);
        return ids;
    }

    /**
     * Remove the given signal group
     * 
     * @param signalGroupId the signal group
     */
    public void removeSignalGroup(@NonNull final UUID signalGroupId) {
        collection.removeSignalGroup(signalGroupId);
    }

    
    /**
     * Remove all signal groups from the collection.
     */
    public void removeSignalGroups() {
        for (UUID id : this.getSignalGroupIDs()) {
            removeSignalGroup(id);
        }
    }

    /**
     * Fetch the signal groups in this collection
     * 
     * @param id
     * @return
     */
    public Collection<ISignalGroup> getSignalGroups() {
        return collection.getSignalGroups();
    }

    /**
     * Get the name of the signal group
     * @param signalGroupId the signal group
     * @return the signal group name, if set
     * @throws UnavailableSignalGroupException if the group is not present
     */
    public String getSignalGroupName(@NonNull final UUID signalGroupId) {
    	Optional<ISignalGroup> g = collection.getSignalGroup(signalGroupId);
        return g.isPresent() ? g.get().getGroupName() : "";
    }
    
    /**
     * Get the source image for the given signal group and cell. This will return the appropriate
     * image even if no signals were detected in the cell. 
     * @param signalGroupId the signal group id
     * @param cell the cell whose signal image to fetch
     * @return the image processor for the image
     * @throws UnloadableImageException if the image cannot be found or opened
     */
    public ImageProcessor getSignalSourceImage(@NonNull final UUID signalGroupId, @NonNull final ICell cell) throws UnloadableImageException {
    	Nucleus n = cell.getPrimaryNucleus();
    	if(n.getSignalCollection().hasSignal(signalGroupId))
    		return n.getSignalCollection().getImage(signalGroupId);
    	throw new UnloadableImageException( String.format("No signal image file %s for nucleus %s", n.getSignalCollection().getSourceFile(signalGroupId), n.getNameAndNumber()));   	
    }

    /**
     * Update the source image folder for the given signal group
     * 
     * @param signalGroupId the signal group
     * @param folder the folder containing the images
     */
    public void updateSignalSourceFolder(@NonNull final UUID signalGroupId, @NonNull final File folder) {

        if (!collection.hasSignalGroup(signalGroupId))
            return;
        
        if(!folder.isDirectory())
            return;

        collection.getNuclei().parallelStream().forEach(n -> {
            if (n.getSignalCollection().hasSignal(signalGroupId)) {
                String fileName = n.getSignalCollection().getSourceFile(signalGroupId).getName();
                File newFile = new File(folder, fileName);
                n.getSignalCollection().updateSourceFile(signalGroupId, newFile);
            }
        });
    }

    /**
     * Update a signal group id
     * 
     * @param oldID the id to replace
     * @param newID the new id
     */
    public void updateSignalGroupID(@NonNull final UUID oldID, @NonNull final UUID newID) {

        if (!collection.hasSignalGroup(oldID))
            return;

        for (Nucleus n : collection.getNuclei())
            n.getSignalCollection().updateSignalGroupId(oldID, newID);

        // the group the signals are currently in
		ISignalGroup oldGroup = collection.getSignalGroup(oldID).get();

		// Merge and rename signal groups

		if (collection.hasSignalGroup(newID)) { // check if the group already exists

		    ISignalGroup existingGroup = collection.getSignalGroup(newID).get();

		    if (!oldGroup.getGroupName().equals(existingGroup.getGroupName())) {
		        existingGroup.setGroupName("Merged_" + oldGroup.getGroupName() + "_" + existingGroup.getGroupName());
		    }

		} else { // the signal group does not exist, just copy the old group

		    // the new group for the signals
		    ISignalGroup newGroup = oldGroup.duplicate();
		    collection.addSignalGroup(newGroup);
		}

		collection.removeSignalGroup(oldID);
    }

    /**
     * Find the total number of signals within all nuclei of the collection.
     * 
     * @return the total
     */
    public int getSignalCount() {
        int count = 0;
        for (UUID signalGroup : getSignalGroupIDs()) {
            count += this.getSignalCount(signalGroup);
        }
        return count;
    }

    /**
     * Get the number of signals in the given group
     * 
     * @param signalGroupId the signal group
     * @return the count or -1 if the signal group is not present
     */
    public int getSignalCount(@NonNull final UUID signalGroupId) {
        int count = 0;

        if (collection.getSignalGroupIDs().contains(signalGroupId)) {
            count = 0;
            for (Nucleus n : collection.getNuclei()) {
                count += n.getSignalCollection().numberOfSignals(signalGroupId);

            } // end nucleus iterations
        }
        return count;
    }

    /**
     * Get the mean number of signals in each nucleus
     * 
     * @param signalGroupId the signal group
     * @return
     */
    public double getSignalCountPerNucleus(@NonNull final UUID signalGroupId) {
        if (getSignalCount(signalGroupId) == 0) {
            return 0;
        }

        return (double) getSignalCount(signalGroupId) / (double) getNumberOfCellsWithNuclearSignals(signalGroupId);
    }

    /**
     * Test whether the current population has signals in any channel
     * 
     * @return
     */
    public synchronized boolean hasSignals() {
        for (UUID i : getSignalGroupIDs()) {
            if (this.hasSignals(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test whether the current population has signals in the given group
     * @param signalGroupId the signal group
     * @return
     */
    public boolean hasSignals(@NonNull final UUID signalGroupId) {
        return this.getSignalCount(signalGroupId) > 0;
    }

    /**
     * Test if any of the signal groups in the collection have a shell result
     * 
     * @return
     */
    public synchronized boolean hasShellResult() {
        for (UUID id : collection.getSignalGroupIDs()) {
            if (collection.getSignalGroup(id).get().hasShellResult()) {
			    return true;
			}
        }
        return false;
    }

    /**
     * Get all the signals from all nuclei in the given channel
     * 
     * @param signalGroupId the signal group
     * @return the signals in the group
     */
    public List<INuclearSignal> getSignals(@NonNull final UUID signalGroupId) {

        List<INuclearSignal> result = new ArrayList<>();
        for (Nucleus n : collection.getNuclei()) {
            result.addAll(n.getSignalCollection().getSignals(signalGroupId));
        }
        return result;
    }

    /**
     * Get the median of the signal statistic in the given signal group
     * 
     * @param stat the statistic to fetch
     * @param scale the scale to fetch
     * @param signalGroupId the signal group
     * @return the median value
     */
    public double getMedianSignalStatistic(@NonNull final Measurement stat, @NonNull final MeasurementScale scale, @NonNull final UUID signalGroupId) {

        double[] values = null;
        double median;
        /*
         * Angles must be wrapped
         */
        if (stat.getDimension().equals(MeasurementDimension.ANGLE)) {
            values = getOffsetSignalAngles(signalGroupId);

            if (values.length == 0) {
                LOGGER.fine("No signals detected in group for " + stat);
                return 0;
            }

            median = Stats.quartile(values, Stats.MEDIAN);
            median += getMeanSignalAngle(signalGroupId);
            median = (median+360)%360; // ensure range
        } else {
            values = this.getSignalStatistics(stat, scale, signalGroupId);

            if (values.length == 0) {
                LOGGER.fine("No signals detected in group for " + stat);
                return 0;
            }

            median = Stats.quartile(values, Stats.MEDIAN);
        }

        return median;

    }

    /**
     * Get the signal statistics for the given group
     * 
     * @param stat the statistic to fetch
     * @param scale the scale to fetch
     * @param signalGroupId the signal group
     * @return the values
     */
    public double[] getSignalStatistics(@NonNull final Measurement stat, @NonNull final MeasurementScale scale, @NonNull final UUID signalGroupId) {

        if (!this.hasSignals(signalGroupId))
            return new double[0];
        
        if(Measurement.NUCLEUS_SIGNAL_COUNT.equals(stat)) {
        	return collection.getCells().stream().flatMap(c->c.getNuclei().stream())
                    .mapToDouble(n->n.getSignalCollection().numberOfSignals(signalGroupId))
                    .toArray();
        }

        Set<ICell> cells = getCellsWithNuclearSignals(signalGroupId, true);        
        return cells.stream().flatMap(  c->c.getNuclei().stream()  )
            .flatMap(  n->n.getSignalCollection()
                    .getStatistics(stat, scale, signalGroupId)
                    .stream()  )
            .mapToDouble(Double::doubleValue).toArray();
    }

    /**
     * Signal angles wrap, so a mean must be calculated as a zero point for
     * boxplots. Uses http://catless.ncl.ac.uk/Risks/7.44.html#subj4:
     * sum_i_from_1_to_N sin(a[i]) a = arctangent ---------------------------
     * sum_i_from_1_to_N cos(a[i])
     * 
     * @param signalGroupId the signal group
     * @return the mean angle
     */
    public double getMeanSignalAngle(@NonNull final UUID signalGroupId) {

        double[] values = getSignalStatistics(Measurement.ANGLE, MeasurementScale.PIXELS, signalGroupId);

        double sumSin = 0;
        double sumCos = 0;
        for (double value : values) {
            sumSin += Math.sin(value);
            sumCos += Math.cos(value);
        }

        double mean = Math.atan2(sumSin, sumCos);

        if (mean < 0) {
            mean += 360;
        }
        return mean;
    }

    /**
     * For the signals in a group, find the corrected mean angle using the
     * arctangent method, then rescale the angles to use the mean as a zero
     * point. The returned values should be in the range -180 - +180 from the
     * new zero
     * 
     * @param signalGroupId the signal group
     * @return the offset angles
     */
    public double[] getOffsetSignalAngles(@NonNull final UUID signalGroupId) {

        double[] values = getSignalStatistics(Measurement.ANGLE, MeasurementScale.PIXELS, signalGroupId);

        if (values.length == 0) {
            return new double[0];
        }

        /*
         * The mean is the actual mean of the series of signal angles, with
         * correction for wrapping.
         */

        double meanAngle = getMeanSignalAngle(signalGroupId);

        /*
         * This is the distance from the mean angle to the zero angle, so values
         * can be corrected back to 'real' angles
         */
        double offset = angleDistance(meanAngle, 0);

        double[] result = new double[values.length];

        for (int i = 0; i < values.length; i++) {

            /*
             * Calculate the distance of the signal from the mean value,
             * including a wrap.
             */

            double distance = angleDistance(values[i], meanAngle);

            /*
             * Correct the distance into the distance from the zero point of the
             * nucleus
             */
            result[i] = distance + offset;

        }
        return result;
    }

    /**
     * Copy the signal groups in this cell collection to the target collection,
     * preserving the signal group IDs
     * 
     * @param target the collection to add signals to
     */
    public void copySignalGroups(@NonNull final ICellCollection target) {

        for (UUID id : collection.getSignalGroupIDs()) {
            ISignalGroup newGroup;
            newGroup = collection.getSignalGroup(id).get().duplicate();
			target.addSignalGroup(newGroup);
        }
    }

    /**
     * Length (angular) of a shortest way between two angles. It will be in
     * range [-180, 180].
     */
    private double angleDistance(double a, double b) {
        double phi = Math.abs(b - a) % 360; // This is either the distance or
                                            // 360 - distance
        double distance = phi > 180 ? 360 - phi : phi;

        double sign = (a - b >= 0 && a - b <= 180) || (a - b <= -180 && a - b >= -360) ? 1 : -1;
        distance *= sign;
        return distance;

    }

    /**
     * Force signal angles from the OP to be recalculated. For
     * example, if the OP has moved, signal angles need to be
     *  recalculated.
     */
    public void recalculateSignalAngles() {
        LOGGER.finer( "Recalcalculating signal angles");
        for (Nucleus n : collection.getNuclei()) {
            try {
                n.calculateSignalAnglesFromPoint(n.getBorderPoint(Landmark.ORIENTATION_POINT));
            } catch (UnavailableBorderTagException e) {
                LOGGER.fine("Cannot get OP index");
            }
        }
    }

    /**
     * Shell counts are the same for all signal groups in the collection. If a
     * signal group in the collection has a shell result, the shell count is
     * returned.
     * 
     * @return the shell count of the collection, or zero if no shell results
     *         are present
     */
    public int getShellCount() {

        for(UUID id : getSignalGroupIDs()) {
            ISignalGroup group = collection.getSignalGroup(id).get();
            Optional<IShellResult> r = group.getShellResult();
        	if(r.isPresent())
                return r.get().getNumberOfShells();
        }
        return 0;
    }
    
    public Optional<ShrinkType> getShrinkType() {
    	for (ISignalGroup group : this.getSignalGroups()) {
        	Optional<IShellResult> r = group.getShellResult();
        	if(r.isPresent())
                return Optional.ofNullable(r.get().getType());
        }
        return Optional.empty();
    }

    /**
     * For each signal group pair, find the smallest pairwise distance between
     * signals for each nucleus in the collection. Return the data as a list of
     * pairwise signal distance collections
     */
    public PairwiseSignalDistanceCollection calculateSignalColocalisation(MeasurementScale scale) {

        PairwiseSignalDistanceCollection ps = new PairwiseSignalDistanceCollection();

        for (Nucleus n : collection.getNuclei()) {
            List<PairwiseSignalDistanceValue> list = n.getSignalCollection().calculateSignalColocalisation(scale);

            for (PairwiseSignalDistanceValue v : list) {
                ps.addValue(v);
            }
        }

        return ps;
    }

    /**
     * Calculate the best colocalising signal pairs in the collection
     * 
     * @param signalGroup1 the first signal group id
     * @param signalGroup2 the second signal group id
     * @return a list of colocalising signals
     * @throws IllegalArgumentException if the UUIDs are the same
     */
    public List<Colocalisation<INuclearSignal>> getColocalisingSignals(@NonNull final UUID signalGroup1, @NonNull final UUID signalGroup2) {

        if (signalGroup1.equals(signalGroup2)) {
            throw new IllegalArgumentException("Signal IDs are the same");
        }

        List<Colocalisation<INuclearSignal>> result = new ArrayList<>();

        for (Nucleus n : collection.getNuclei()) {
            result.addAll(n.getSignalCollection().calculateColocalisation(signalGroup1, signalGroup2));
        }
        return result;
    }
}
