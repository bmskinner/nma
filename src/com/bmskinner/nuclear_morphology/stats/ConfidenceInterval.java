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
package com.bmskinner.nuclear_morphology.stats;

import java.util.stream.DoubleStream;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class ConfidenceInterval {

    Number ci;
    Number mean;

    /**
     * Calculate the confidence interval about the data mean for the given
     * confidence level
     * 
     * @param data
     *            the array of data points
     * @param level
     *            the confidence level (0-1)
     */
    public ConfidenceInterval(double[] data, double level) {

        // Calculate 95% confidence interval
        ci = calculateConfidenceIntervalSize(data, level);
        mean = DoubleStream.of(data).average().orElse(0);

    }

    public Number getMean() {
        return mean;
    }

    public Number getUpper() {
        return mean.doubleValue() + ci.doubleValue();
    }

    public Number getLower() {
        return mean.doubleValue() - ci.doubleValue();
    }

    public Number getSize() {
        return ci;
    }

    /**
     * Calculate the confidence interval about the mean using a T-distribution
     * 
     * @param stats
     * @param level
     * @return
     */
    private Number calculateConfidenceIntervalSize(double[] data, double level) {

        if (data.length < 2) {
            return 0;
        }
        try {

            SummaryStatistics stats = new SummaryStatistics();
            for (double val : data) {
                stats.addValue(val);
            }

            // Create T Distribution with N-1 degrees of freedom
            TDistribution tDist = new TDistribution(stats.getN() - 1);
            // Calculate critical value
            double critVal = tDist.inverseCumulativeProbability(1.0 - (1 - level) / 2);
            // Calculate confidence interval
            return critVal * stats.getStandardDeviation() / Math.sqrt(stats.getN());
        } catch (MathIllegalArgumentException e) {
            return Double.NaN;
        }
    }

}
