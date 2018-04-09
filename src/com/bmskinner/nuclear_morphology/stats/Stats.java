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


/* 
  -----------------------
  STATS FUNCTIONS
  -----------------------
  This class contains basic functions for 
  nuclear organisation analysis used across
  other classes
 */

package com.bmskinner.nuclear_morphology.stats;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.DoubleStream;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.commons.math3.stat.inference.OneWayAnova;

import com.bmskinner.nuclear_morphology.logging.Loggable;

public class Stats implements Loggable {

    public static final double LOG2                      = Math.log(2);
    public static final String NULL_OR_EMPTY_ARRAY_ERROR = "The data array either is null or does not contain any data.";
    
    public static final int LOWER_QUARTILE = 25;
    public static final int UPPER_QUARTILE = 75;
    public static final int MEDIAN         = 50;

    static double max(double[] array) {
        return DoubleStream.of(array).max().orElse(0);
    }

    static double min(double[] array) {
        return DoubleStream.of(array).min().orElse(0);
    }

    /**
     * Calculate the standard error of an array of values
     * 
     * @param m
     * @return
     */
    public static double stderr(double[] m) {
        if (m == null || m.length == 0) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
        }

        if (m.length < 2) {
            return 0;
        }

        return stdev(m) / Math.sqrt(m.length);
    }

    /**
     * Calculate the standard deviation of an array of values
     * 
     * @param m
     * @return
     */
    public static double stdev(double[] m) {
        if (m == null || m.length == 0) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
        }

        if (m.length < 2) {
            return 0;
        }
        return Math.sqrt(variance(m));
    }

    /**
     * Calculate the variance of an array of values.
     * 
     * @param m the array
     * @return the variance
     */
    public static double variance(double[] m) {
        if (m == null || m.length == 0) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
        }

        if (m.length < 2) {
            return 0;
        }
        double mean = DoubleStream.of(m).average().orElse(0);
        double temp = 0;
        for (double d : m){
            temp += Math.pow(mean - d, 2);
        }
        return temp / m.length;
    }

    public static double calculateLog2Ratio(double d) {
        return Math.log(d) / LOG2;
    }

    /**
     * Calculate the Spearman's rank correlation coefficient between two arrays
     * of paired values
     * 
     * @param x
     * @param y
     * @return
     */
    public static double getSpearmansCorrelation(double[] x, double[] y) {

        if (x == null || x.length == 0) {
            throw new IllegalArgumentException("x:" + NULL_OR_EMPTY_ARRAY_ERROR);
        }

        if (y == null || y.length == 0) {
            throw new IllegalArgumentException("y:" + NULL_OR_EMPTY_ARRAY_ERROR);
        }

        if (x.length != y.length) {
            throw new IllegalArgumentException("Input arrays have different lengths");
        }

        if (x.length == 1) {
            return 0;
        }
        SpearmansCorrelation sp = new SpearmansCorrelation();
        return sp.correlation(x, y);

    }

    public static double getSpearmanPValue(double r, double n) {
        double t = getTStatistic(r, n);
        TDistribution tDist = new TDistribution(n - 1);
        return tDist.probability(t);
    }

    /**
     * Get the t-statistic for a Spearman's rho at a given sample size
     * 
     * @param r
     * @param n
     * @return
     */
    public static double getTStatistic(double r, double n) {
        // t = r * sqrt( n-2 / 1-r^2 )

        double t = r * Math.sqrt(((n - 2) / (1 - (r * r))));

        return t;

    }
    
    /**
     * Test if the given data is normally distributed.
     * Creates a normal distribution with the data mean
     * and stdev, and tests against the actual data with 
     * the KolmogorovSmirnov. If the p-value is greater than
     * the given threshold, the data is considered normal.
     * 
     * @param data the data to test
     * @param pvalue the p-value threshold for rejecting the null hypothesis of equality
     * @return true if the data is normally distributed
     */
    public static boolean isNormallyDistributed(double[] data, double pvalue) {
    	// Check all arrays are normal
    	SummaryStatistics ss = new SummaryStatistics();
    	Arrays.stream(data).forEach(d->ss.addValue(d));
    	
    	NormalDistribution nd = new NormalDistribution(ss.getMean(), ss.getStandardDeviation());
    	KolmogorovSmirnovTest kt = new KolmogorovSmirnovTest();
    	double p = kt.kolmogorovSmirnovTest(nd, data);
    	return p > 0.05;
    	
    }
    
    /**
     * Perform a one-way ANOVA on the given data. The data should all be normally
     * distributed. If not, the test will return 1.
     * @param data
     * @return
     */
    public static double getOneWayAnovaPValue(Collection<double[]> data){
    	
    	for(double[] d : data){
    		if(!isNormallyDistributed(d, 0.05)){
    			return 1;
    		}
    	}
    	OneWayAnova an = new OneWayAnova();
    	
    	return an.anovaPValue(data);
    }


    /**
     * Run a Wilcoxon test on the given arrays.
     * 
     * @param values0
     * @param values1
     * @param isGetPValue
     * @return
     */
    public static double runWilcoxonTest(double[] values0, double[] values1, boolean isGetPValue) {

        double result = 0;
        MannWhitneyUTest test = new MannWhitneyUTest(); // default, NaN's are
                                                        // left in place and
                                                        // ties get the average
                                                        // of applicable ranks

        if (isGetPValue) { // above diagonal, p-value
            result = test.mannWhitneyUTest(values0, values1); // correct for the
                                                              // number of
                                                              // datasets tested

        } else { // below diagonal, U statistic
            result = test.mannWhitneyU(values0, values1);
        }
        return result;
    }
    
    /**
     * Get the quartile for a float array
     * 
     * @param values the values
     * @param quartile the quartile to find
     * @return the quartile value
     */
    public static float quartile(float[] values, int quartile) {

        if (values == null || values.length == 0)
            throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);

        if (values.length == 1)
            return values[0];

        if (values.length == 2)
            return quartile < MEDIAN ? values[0] : values[1];

        // Rank order the values
        float[] v = new float[values.length];
        System.arraycopy(values, 0, v, 0, values.length);
        Arrays.sort(v);

        int n = Math.round(((float) v.length * quartile) / 100);
        return v[n];
    }
    
    /**
     * Get the quartile for a float array
     * 
     * @param values the values
     * @param quartile the quartile to find
     * @return the quartile value
     */
    public static int quartile(int[] values, int quartile) {

        if (values == null || values.length == 0)
            throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);

        if (values.length == 1)
            return values[0];

        if (values.length == 2)
            return quartile < MEDIAN ? values[0] : values[1];

        // Rank order the values
        int[] v = new int[values.length];
        System.arraycopy(values, 0, v, 0, values.length);
        Arrays.sort(v);

        int n = Math.round(((float) v.length * quartile) / 100);

        return v[n];
    }

    /**
     * Get the quartile for a double array
     * 
     * @param values
     *            the values
     * @param quartile
     *            the quartile to find
     * @return the quartile value
     */
    public static double quartile(double[] values, int quartile) {

        if (values == null || values.length == 0)
            throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);

        if (values.length == 1)
            return values[0];

        if (values.length == 2)
            return quartile < MEDIAN ? values[0] : values[1];

        // Rank order the values
        double[] v = new double[values.length];
        System.arraycopy(values, 0, v, 0, values.length);
        Arrays.sort(v);

        int n = Math.round(((float) v.length * quartile) / 100);

        return v[n];
    }

}
