/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.analysis.signals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

import org.apache.commons.math3.stat.inference.ChiSquareTest;

import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Mean;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Hold a count of the signal proportions and counts observed in each shell
 * during a shell analysis
 * 
 * @author bms41
 *
 */
public class ShellCounter implements Loggable {

    private int                        numberOfShells;
    private Map<Integer, List<Double>> signalProportionValues = new HashMap<Integer, List<Double>>(0); // raw
                                                                                                       // values
    private Map<Integer, List<Double>> signalNormalisedValues = new HashMap<Integer, List<Double>>(0); // values
                                                                                                       // after
                                                                                                       // DAPI
                                                                                                       // normalisation
    private Map<Integer, Integer>      signalPixelCounts      = new HashMap<Integer, Integer>(0);      // store
                                                                                                       // the
                                                                                                       // pixel
                                                                                                       // counts
                                                                                                       // for
                                                                                                       // signals

    private Map<Integer, List<Double>> nucleusProportionValues = new HashMap<Integer, List<Double>>(0); // raw
                                                                                                        // the
                                                                                                        // values
    private Map<Integer, List<Double>> nucleusNormalisedValues = new HashMap<Integer, List<Double>>(0); // values
                                                                                                        // after
                                                                                                        // DAPI
                                                                                                        // normalisation
    private Map<Integer, Integer>      nucleusPixelCounts      = new HashMap<Integer, Integer>(0);      // store
                                                                                                        // the
                                                                                                        // pixel
                                                                                                        // counts
                                                                                                        // for
                                                                                                        // nuclei

    /**
     * The type of pixels to be counted. These can be only pixels within the
     * borders of a signal, or all pixels within the borders of a nucleus.
     * 
     * @author bms41
     * @since 1.13.3
     *
     */
    public enum CountType {
        SIGNAL, NUCLEUS;
    }

    /**
     * Create a new shell counter with the given number of shells
     * 
     * @param numberOfShells
     *            the shell count
     */
    public ShellCounter(int numberOfShells) {

        this.numberOfShells = numberOfShells;
        for (int i = 0; i < numberOfShells; i++) {
            signalProportionValues.put(i, new ArrayList<Double>(0));
            signalNormalisedValues.put(i, new ArrayList<Double>(0));
            signalPixelCounts.put(i, 0);

            nucleusProportionValues.put(i, new ArrayList<Double>(0));
            nucleusNormalisedValues.put(i, new ArrayList<Double>(0));
            nucleusPixelCounts.put(i, 0);

        }

    }

    /**
     * Add an array of signal specific values
     * 
     * @param rawProportions
     *            the proportion of signal per shell (0-1)
     * @param normalisedProportions
     *            the DAPI normalised proportion of signal per shell
     * @param counts
     *            the total pixel intensity counts per shell
     */
    public void addSignalValues(double[] rawProportions, double[] normalisedProportions, int[] counts) {
        if (rawProportions.length != numberOfShells || counts.length != numberOfShells) {
            throw new IllegalArgumentException("Input array is wrong size");
        }

        // check the first entry in the list is a value
        Double firstShell = new Double(rawProportions[0]);
        if (firstShell.isNaN()) {
            throw new IllegalArgumentException("Argument is not a number");
        }

        for (int i = 0; i < numberOfShells; i++) {
            List<Double> raw = signalProportionValues.get(i);
            raw.add(rawProportions[i]);

            List<Double> norm = signalNormalisedValues.get(i);
            norm.add(normalisedProportions[i]);

            int shellTotal = this.signalPixelCounts.get(i);
            this.signalPixelCounts.put(i, shellTotal + counts[i]);
        }
    }

    /**
     * Add an array of signal specific values
     * 
     * @param rawProportions
     *            the proportion of signal per shell (0-1)
     * @param normalisedProportions
     *            the DAPI normalised proportion of signal per shell
     * @param counts
     *            the total pixel intensity counts per shell
     */
    public void addNucleusValues(double[] rawProportions, double[] normalisedProportions, int[] counts) {
        if (rawProportions.length != numberOfShells || counts.length != numberOfShells) {
            throw new IllegalArgumentException("Input array is wrong size");
        }

        // check the first entry in the list is a value
        Double firstShell = new Double(rawProportions[0]);
        if (firstShell.isNaN()) {
            throw new IllegalArgumentException("Argument is not a number");
        }

        for (int i = 0; i < numberOfShells; i++) {

            List<Double> raw = nucleusProportionValues.get(i);
            raw.add(rawProportions[i]);

            List<Double> norm = nucleusNormalisedValues.get(i);
            norm.add(normalisedProportions[i]);

            int shellTotal = this.signalPixelCounts.get(i);
            this.nucleusPixelCounts.put(i, shellTotal + counts[i]);
        }
    }

    public List<Double> getRawMeans(CountType type) {

        List<Double> result = new ArrayList<Double>(0);
        for (int i = 0; i < numberOfShells; i++) {
            double[] values = null;
            switch (type) {
            case SIGNAL: {
                values = getRawSignalShell(i);
                break;
            }
            case NUCLEUS: {
                values = getRawNucleusShell(i);
                break;
            }
            }
            double mean = DoubleStream.of(values).average().orElse(0);
            result.add(mean);

        }
        return result;
    }

    public List<Double> getNormalisedMeans(CountType type) {

        List<Double> result = new ArrayList<Double>(0);
        for (int i = 0; i < numberOfShells; i++) {
            double[] values = null;
            switch (type) {
            case SIGNAL: {
                values = getNormSignalShell(i);
                break;
            }
            case NUCLEUS: {
                values = getNormNucleusShell(i);
                break;
            }
            default: {
                values = getNormNucleusShell(i);
                break;
            }
            }
            double mean = DoubleStream.of(values).average().orElse(0);
            result.add(mean);

        }
        return result;
    }

    public List<Double> getRawStandardErrors(CountType type) {

        List<Double> result = new ArrayList<Double>(0);
        for (int i = 0; i < numberOfShells; i++) {
            switch (type) {
            case SIGNAL: {
                result.add(Stats.stderr(getRawSignalShell(i)));
                break;
            }
            case NUCLEUS: {
                result.add(Stats.stderr(getRawNucleusShell(i)));
                break;
            }
            }
        }

        return result;
    }

    public List<Double> getNormalisedStandardErrors(CountType type) {

        List<Double> result = new ArrayList<Double>(numberOfShells);
        for (int i = 0; i < numberOfShells; i++) {
            switch (type) {
            case SIGNAL: {
                result.add(Stats.stderr(getNormSignalShell(i)));
                break;
            }
            case NUCLEUS: {
                result.add(Stats.stderr(getNormNucleusShell(i)));
                break;
            }
            }
        }
        return result;
    }

    public List<Integer> getPixelCounts(CountType type) {
        List<Integer> result = new ArrayList<Integer>(0);
        for (int i = 0; i < numberOfShells; i++) {

            switch (type) {
            case SIGNAL: {
                result.add(signalPixelCounts.get(i));
                break;
            }
            case NUCLEUS: {
                result.add(nucleusPixelCounts.get(i));
                break;
            }
            }

        }
        return result;
    }

    public double getRawPValue(CountType type) {
        double pvalue = 1;
        try {
            long[] observed = getRawObserved(type);
            double[] expected = getExpected(type);

            ChiSquareTest test = new ChiSquareTest();
            pvalue = test.chiSquareTest(expected, observed);

        } catch (Exception e) {
            stack("Error getting p-values in chi test", e);
            pvalue = 1;
        }
        return pvalue;
    }

    public double getNormalisedPValue(CountType type) {
        double pvalue = 1;
        try {
            long[] observed = getNormObserved(type);
            double[] expected = getExpected(type);

            ChiSquareTest test = new ChiSquareTest();
            pvalue = test.chiSquareTest(expected, observed);

        } catch (Exception e) {
            stack("Error getting p-values in chi test", e);
            pvalue = 1;
        }
        return pvalue;
    }

    public double getRawChiSquare(CountType type) {
        double chi = 0;
        try {
            long[] observed = getRawObserved(type);
            double[] expected = getExpected(type);
            ChiSquareTest test = new ChiSquareTest();
            chi = test.chiSquare(expected, observed);
        } catch (Exception e) {
            stack("Error getting chi square values", e);
            chi = 0;
        }
        return chi;
    }

    public double getNormalisedChiSquare(CountType type) {
        double chi = 0;
        try {
            long[] observed = getNormObserved(type);
            double[] expected = getExpected(type);
            ChiSquareTest test = new ChiSquareTest();
            chi = test.chiSquare(expected, observed);
        } catch (Exception e) {
            stack("Error getting chi square values", e);
            chi = 0;
        }
        return chi;
    }

    /*
     * 
     * PRIVATE METHODS
     * 
     * 
     */

    /**
     * Get the values within the current shell. If a value is NaN, return 0
     * 
     * @param shell
     *            the shell to return
     * @return
     */
    private double[] getRawSignalShell(int shell) {
        return getShellValues(signalProportionValues, shell);
    }

    /**
     * Get the values within the current shell. If a value is NaN, return 0
     * 
     * @param shell
     *            the shell to return
     * @return
     */
    private double[] getNormSignalShell(int shell) {

        return getShellValues(signalNormalisedValues, shell);
    }

    /**
     * Get the values within the current shell. If a value is NaN, return 0
     * 
     * @param shell
     *            the shell to return
     * @return
     */
    private double[] getRawNucleusShell(int shell) {
        return getShellValues(nucleusProportionValues, shell);
    }

    /**
     * Get the values within the current shell. If a value is NaN, return 0
     * 
     * @param shell
     *            the shell to return
     * @return
     */
    private double[] getNormNucleusShell(int shell) {

        return getShellValues(nucleusNormalisedValues, shell);
    }

    private double[] getShellValues(Map<Integer, List<Double>> allValues, int shell) {
        List<Double> values = allValues.get(shell);
        double[] array = new double[values.size()];

        try {
            for (int i = 0; i < array.length; i++) {

                // if the value is not a number, put zero
                array[i] = values.get(i).isNaN() ? 0 : values.get(i);
            }
        } catch (Exception e) {
            stack("Error getting shell values", e);
        }
        return array;
    }

    /**
     * Get the observed values as a long array. Long is needed for the
     * chi-square test
     * 
     * @return the observerd shell values
     * @throws Exception
     */
    private long[] getRawObserved(CountType type) throws Exception {
        long[] observed = new long[numberOfShells];
        int count = size(type);
        List<Double> means = getRawMeans(type);
        for (int i = 0; i < numberOfShells; i++) {
            double mean = means.get(i);
            observed[i] = (long) (mean * count);
        }
        return observed;
    }

    /**
     * Get the observed values as a long array. Long is needed for the
     * chi-square test
     * 
     * @return the observerd shell values
     * @throws Exception
     */
    private long[] getNormObserved(CountType type) throws Exception {
        long[] observed = new long[numberOfShells];
        int count = size(type);
        List<Double> means = getNormalisedMeans(type);
        for (int i = 0; i < numberOfShells; i++) {
            double mean = means.get(i);
            observed[i] = (long) (mean * count);
        }
        return observed;
    }

    /**
     * Get the expected values for chi-sqare test, assuming an equal proportion
     * of signal per shell
     * 
     * @return the expected values
     */
    private double[] getExpected(CountType type) {
        double[] expected = new double[numberOfShells];
        double count = size(type);
        for (int i = 0; i < numberOfShells; i++) {
            expected[i] = ((double) 1 / (double) numberOfShells) * count;
        }
        return expected;
    }

    /**
     * Get the number of signals measured
     * 
     * @param type
     *            the count type (signals or nucleus)
     * @return
     */
    public int size(CountType type) {
        switch (type) {
        case SIGNAL: {
            return signalProportionValues.get(0).size();
        }
        case NUCLEUS: {
            return nucleusProportionValues.get(0).size();
        }
        }

        return 0;
    }

    // /**
    // * For debugging - print the contents of the dataset to log
    // */
    // public void print(){
    // if(this.size()==0){ // don't make empty log files
    // return;
    // }
    // for(int i = 0; i< signalProportionValues.get(0).size();i++){ // go
    // through each signal
    // String line = "";
    // for(int j = 0; j<numberOfShells; j++){ // each shell for signal
    // List<Double> list = signalProportionValues.get(j);
    // line += list.get(i)+"\t";
    // }
    // log(line);
    // }
    // log("");
    // }

}
