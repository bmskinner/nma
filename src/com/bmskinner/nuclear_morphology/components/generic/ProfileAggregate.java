/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components.generic;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.AbstractCellularComponent;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * This class holds the aggregates of individual profiles, so that a median
 * profile can be created. It also holds the methods for repairing median
 * profiles when too few nuclei are present to calculate a precise profile
 * 
 * @author bms41
 *
 */
@Deprecated
public class ProfileAggregate implements Loggable, Serializable, IProfileAggregate {

    private static final long               serialVersionUID = 1L;
    private Map<Double, Collection<Double>> aggregate        = new HashMap<Double, Collection<Double>>();
    private double                          profileIncrement;
    private int                             length;
    private double[]                        xPositions;

    public ProfileAggregate(int length) {

        if (length <= 0) {
            throw new IllegalArgumentException("Aggregate cannot be created with length <=0");
        }

        this.length = length;
        this.profileIncrement = (double) 100 / (double) length;

        xPositions = new double[length];
        double x = 0;
        for (int i = 0; i < length; i++) {
            xPositions[i] = x;
            x += profileIncrement;
        }
    }

    /*
     * We need to calculate the median angle profile. This requires binning the
     * normalised profiles into bins of size PROFILE_INCREMENT to generate a
     * table such as this: k 0.0 0.5 1.0 1.5 2.0 ... 99.5 <- normalised profile
     * bins NUCLEUS1 180 185 170 130 120 ... 50 <- angle within those bins
     * NUCLEUS2 180 185 170 130 120 ... 50
     * 
     * The median of each bin can then be calculated. Depending on the length of
     * the profile arrays and the chosen increment, there may be >1 or <1 angle
     * within each bin for any given nucleus. We rely on large numbers of nuclei
     * to average this problem away; further methods interpolate values from
     * surrounding bins to plug any holes left over
     * 
     * The data are stored as a Map<Double, Collection<Double>>
     * PROFILE_INCREMENT is 100 / the median array length. This ensures > 1
     * entry for each bin, while not pooling too many entries.
     */

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileAggregate#addValues(components.generic.
     * IProfile)
     */
    @Override
    public void addValues(IProfile yvalues) {

        // normalise the profile to the correct x positions
        double[] xvalues = new double[yvalues.size()];
        for (int i = 0; i < xvalues.length; i++) {
            xvalues[i] = ((double) i / (double) xvalues.length) * 100;
        }

        for (int i = 0; i < xvalues.length; i++) { // the positions in the input
                                                   // profile

            double testX = xvalues[i];

            for (int j = 0; j < this.length; j++) { // cover all the bin
                                                    // positions across the
                                                    // profile

                double x = xPositions[j]; // the bin to fill
                double maxX = x + profileIncrement;

                if (testX >= x && testX < maxX) { // profile xvalue is in bin

                    Collection<Double> values = aggregate.get(x);

                    if (values == null) { // this this profile increment has not
                                          // yet been encountered, create it
                        values = new ArrayList<Double>();
                        aggregate.put(x, values);
                    }
                    values.add(yvalues.get(i));
                    break;
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileAggregate#getBinSize()
     */
//    @Override
//    public double getBinSize() {
//        return this.profileIncrement;
//    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileAggregate#length()
     */
    @Override
    public int length() {
        return this.length;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileAggregate#getXPositions()
     */
    @Override
    public IProfile getXPositions() {
        double[] result = new double[length];

        // start counting half a bin below zero
        // this sets the value to the bin centre
        double x = -this.profileIncrement / 2;

        // add the bin size for each positions
        for (int i = 0; i < length; i++) {
            x += profileIncrement;
            result[i] = x;
        }
        return new Profile(result);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileAggregate#getMedian()
     */
    // public Profile getNumberOfPoints(){
    // double[] result = new double[length];
    //
    // for(int i=0;i<length;i++){
    // double x = xPositions[i];
    // result[i] = aggregate.containsKey(x) ? aggregate.get(x).size() : 0;
    // }
    // return new Profile(result);
    // }

    @Override
    public IProfile getMedian() throws ProfileException {
        IProfile result = null;
        try {
            result = calculateQuartile(Stats.MEDIAN);
        } catch (ProfileException e) {
            // if the profile >200, scale down to 200. Otherwise, reduce
            // stepwise until we get a profile
            int newLength = this.length <= 200 ? this.length - 5 : 200;

            if (newLength <= 0) {
                throw (e);
            }

            finest("Error in getting profile aggregate median: rescaling to " + newLength);
            this.rescaleProfile(newLength);
            result = this.getMedian(); // recurse through this function until we
                                       // get a profile
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileAggregate#getQuartile(double)
     */
    @Override
    public IProfile getQuartile(double quartile) throws ProfileException {
        IProfile result = null;
        try {

            result = calculateQuartile(quartile);

        } catch (ProfileException e) {
            result = this.getMedian(); // median is the only method that
                                       // rescales, so should have been called
                                       // before this
            finest("Cannot find quartile; falling back on median");
        }
        return result;
    }

    private IProfile calculateQuartile(double quartile) throws ProfileException {

        if (this.length == 0) {
            throw new ProfileException("Cannot calculate median profile, aggregate length is zero");
        }

        double[] medians = new double[this.length];

        int missing = 0;

        for (int i = 0; i < this.length; i++) {
            double x = xPositions[i];

            try {
                if (aggregate.containsKey(x)) {

                    Collection<Double> values = aggregate.get(x);
                    double[] d = new double[values.size()];
                    int j = 0;
                    for (Double val : values) {
                        d[j++] = val;
                    }

                    double median = Stats.quartile(d, (int) quartile);

                    medians[i] = median;
                } else {
                    medians[i] = Double.NaN;
                    missing++;
                }
            } catch (Exception e) {
                // IJ.log(" Index "+i+": Cannot calculate median for "+x);
                medians[i] = Double.NaN;
            }
        }
        if (missing > (double) length / 4) {
            throw new ProfileException("Too many missing values (" + missing + ") to calculate median profile");
        }

        if (missing == 0) {
            return new Profile(medians);
        } else {
            return repairProfile(medians);
        }
    }

    private IProfile repairProfile(double[] array) throws ProfileException {

        for (int i = 0; i < array.length; i++) {

            if (Double.isNaN(array[i])) {

                int replacementIndex = AbstractCellularComponent.wrapIndex(i + 1, array.length);
                if (!Double.isNaN(array[replacementIndex])) {
                    array[i] = array[replacementIndex];
                } else {
                    replacementIndex = AbstractCellularComponent.wrapIndex(i - 1, array.length);
                    if (!Double.isNaN(array[replacementIndex])) {
                        array[i] = array[replacementIndex];
                    } else {
                        throw new ProfileException("Unable to repair median profile");
                    }
                }
            }
        }
        return new Profile(array);
    }

    // this is for low count profiles, to get some kind of median even if poor
    private void rescaleProfile(int newLength) {

        double increment = (double) 100 / (double) newLength;
        double newX = 0;

        Map<Double, Collection<Double>> rescaled = new HashMap<Double, Collection<Double>>();

        // go through each new position
        for (int i = 0; i < newLength; i++) {

            // Go through each old position. Find the points that overlap
            for (int j = 0; j < length; j++) {
                double oldX = xPositions[j];
                if (oldX >= newX && oldX < newX + increment) { // the old x is
                                                               // in the new bin
                    // overlap; get all the values into the new bin
                    if (aggregate.containsKey(oldX)) {
                        Collection<Double> oldValues = aggregate.get(oldX);
                        Collection<Double> newValues = rescaled.get(newX);

                        if (newValues == null) { // this this profile increment
                                                 // has not yet been
                                                 // encountered, create it
                            newValues = new ArrayList<Double>();
                            rescaled.put(newX, newValues);
                        }
                        for (double d : oldValues) {
                            newValues.add(d);
                        }
                    }
                }
            }
            newX += increment;
        }

        this.aggregate = rescaled;
        this.length = newLength;
        this.profileIncrement = increment;
        this.xPositions = new double[newLength];
        double x = 0;
        for (int i = 0; i < length; i++) {
            this.xPositions[i] = x;
            x += profileIncrement;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileAggregate#getValuesAtPosition(double)
     */
    @Override
    public double[] getValuesAtPosition(double position) {
        if (position < 0 || position > 100) {
            throw new IllegalArgumentException("Desired x-position is out of range: " + position);
        }

        /*
         * if the given value is not an existing key, we need to find the
         * closest key to use instead
         */
        if (!aggregate.containsKey(position)) {

            Set<Double> keys = aggregate.keySet();

            Double[] array = keys.toArray(new Double[0]);
            Arrays.sort(array);

            int upper = 0;
            int lower = 0;

            for (int i = 0; i < array.length; i++) {
                double key = array[i];
                if (key < position) {
                    lower = i; // lower ends as the last key below position
                    upper = i + 1;
                }
                if (key > position) {
                    break;
                }

            }

            double diffL = Math.abs(array[lower] - position);
            double diffU = Math.abs(array[upper] - position);
            position = diffL < diffU ? array[lower] : array[upper]; // choose
                                                                    // the key
                                                                    // closest
                                                                    // to the
                                                                    // requested
                                                                    // position
        }

        // the desired position is chosen
        Collection<Double> values = aggregate.get(position);
        
        return values.stream().mapToDouble(d->d.doubleValue()).toArray();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfileAggregate#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("Profile aggregate with " + aggregate.size() + " bins of size " + profileIncrement + "\n");

        try {
            builder.append("First bin at " + xPositions[0] + ":\n");
            for (double d : getValuesAtPosition(xPositions[0])) {
                builder.append("\t" + d + "\n");
            }

            builder.append("Last bin at " + xPositions[aggregate.size() - 1] + ":\n");
            for (double d : getValuesAtPosition(xPositions[aggregate.size() - 1])) {
                builder.append("\t" + d + "\n");
            }

        } catch (Exception e) {
            builder.append("Could not display bins\n");
        }

        return builder.toString();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        // finest("\tReading profile aggregate");
        in.defaultReadObject();
        // finest("\tRead profile aggregate");
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        // finest("\tWriting profile aggregate");
        out.defaultWriteObject();
        // finest("\tWrote profile aggregate");
    }

}
