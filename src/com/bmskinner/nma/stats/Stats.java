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
/* 
  -----------------------
  STATS FUNCTIONS
  -----------------------
  This class contains basic functions for 
  nuclear organisation analysis used across
  other classes
 */

package com.bmskinner.nma.stats;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.DoubleStream;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.commons.math3.stat.inference.OneWayAnova;
import org.eclipse.jdt.annotation.NonNull;

import ij.gui.Roi;
import ij.process.FloatPolygon;

/**
 * Provides quick implementations of basic stats methods
 * 
 * @author bms41
 *
 */
public class Stats {

	public static final double LOG2 = Math.log(2);
	public static final String NULL_OR_EMPTY_ARRAY_ERROR = "The data array either is null or does not contain any data.";

	public static final int LOWER_QUARTILE = 25;
	public static final int UPPER_QUARTILE = 75;

	private Stats() {
	}

	/**
	 * The median value (q50)
	 */
	public static final int MEDIAN = 50;
	public static final int ONE_HUNDRED_PERCENT = 100;

	public static double max(double[] array) {
		return DoubleStream.of(array).max().orElse(0);
	}

	public static double min(double[] array) {
		return DoubleStream.of(array).min().orElse(0);
	}

	public static float max(float[] array) {
		float max = -Float.MAX_VALUE;
		for (float f : array)
			max = Math.max(max, f);
		return max;
	}

	public static float min(float[] array) {
		float min = Float.MAX_VALUE;
		for (float f : array)
			min = Math.min(min, f);
		return min;
	}

	/**
	 * Calculate the standard error of an array of values
	 * 
	 * @param m
	 * @return
	 */
	public static double stderr(double[] m) {
		if (m == null || m.length == 0)
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
		if (m.length < 2)
			return 0;
		return stdev(m) / Math.sqrt(m.length);
	}

	/**
	 * Calculate the standard deviation of an array of values
	 * 
	 * @param m
	 * @return
	 */
	public static double stdev(float[] m) {
		if (m == null || m.length == 0)
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
		if (m.length < 2)
			return 0;
		return Math.sqrt(variance(m));
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
		if (m == null || m.length == 0)
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
		if (m.length < 2)
			return 0;
		double mean = DoubleStream.of(m).average().orElse(0);
		double temp = 0;
		for (double d : m) {
			temp += Math.pow(mean - d, 2);
		}
		return temp / m.length;
	}

	/**
	 * Calculate the variance of an array of values.
	 * 
	 * @param m the array
	 * @return the variance
	 */
	public static double variance(float[] m) {
		if (m == null || m.length == 0)
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
		if (m.length < 2)
			return 0;
		double total = 0;
		for (float f : m) {
			total += f;
		}
		double mean = total / m.length;
		double temp = 0;
		for (double d : m) {
			temp += Math.pow(mean - d, 2);
		}
		return temp / m.length;
	}

	public static double calculateLog2Ratio(double d) {
		return Math.log(d) / LOG2;
	}

	/**
	 * Calculate the Spearman's rank correlation coefficient between two arrays of
	 * paired values
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
		return r * Math.sqrt(((n - 2) / (1 - (r * r))));
	}

	/**
	 * Test if the given data is normally distributed. Creates a normal distribution
	 * with the data mean and stdev, and tests against the actual data with the
	 * KolmogorovSmirnov. If the p-value is greater than the given threshold, the
	 * data is considered normal.
	 * 
	 * @param data   the data to test
	 * @param pvalue the p-value threshold for rejecting the null hypothesis of
	 *               equality
	 * @return true if the data is normally distributed
	 */
	public static boolean isNormallyDistributed(double[] data, double pvalue) {
		// Check all arrays are normal
		SummaryStatistics ss = new SummaryStatistics();
		Arrays.stream(data).forEach(d -> ss.addValue(d));

		NormalDistribution nd = new NormalDistribution(ss.getMean(), ss.getStandardDeviation());
		KolmogorovSmirnovTest kt = new KolmogorovSmirnovTest();
		double p = kt.kolmogorovSmirnovTest(nd, data);
		return p > 0.05;

	}

	/**
	 * Perform a one-way ANOVA on the given data. The data should all be normally
	 * distributed. If not, the test will return 1.
	 * 
	 * @param data
	 * @return
	 */
	public static double getOneWayAnovaPValue(Collection<double[]> data) {

		for (double[] d : data) {
			if (!isNormallyDistributed(d, 0.05)) {
				return 1;
			}
		}
		OneWayAnova an = new OneWayAnova();

		return an.anovaPValue(data);
	}

	public record WilcoxonRankSumResult(double u, double p) {
	}

	/**
	 * Run a Wilcoxon test on the given arrays and return a Bonnferroni corrected
	 * p-value based on the total number of comparisons.
	 * 
	 * @param values0      the first array of values
	 * @param values1      the second array of values
	 * @param nComparisons the number of simultaneous comparisons being made for
	 *                     Bonnferroni correction
	 * @return
	 */
	public static WilcoxonRankSumResult runWilcoxonTest(double[] values0, double[] values1,
			int nComparisons) {

		MannWhitneyUTest test = new MannWhitneyUTest(); // default, NaN's are
														// left in place and
														// ties get the average
														// of applicable ranks

		double p = test.mannWhitneyUTest(values0, values1);
		p *= nComparisons; // Bonferroni correction
		p = p > SignificanceTest.ONE ? SignificanceTest.ONE : p; // limit p to 1

		double u = test.mannWhitneyU(values0, values1);

		return new WilcoxonRankSumResult(u, p);
	}

	/**
	 * Get the quartile for a float array
	 * 
	 * @param values   the values
	 * @param quartile the quartile to find
	 * @return the quartile value
	 */
	public static float quartile(float[] values, int quartile) {

		if (values == null || values.length == 0)
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);

		if (values.length == 1)
			return values[0];

		DescriptiveStatistics ds = new DescriptiveStatistics();
		for (float d : values)
			ds.addValue(d);
		return (float) ds.getPercentile(quartile);

//        // Rank order the values
//        float[] v = new float[values.length];
//        System.arraycopy(values, 0, v, 0, values.length);
//        Arrays.sort(v);
//
//        if (values.length == 2)
//        	return quartile < MEDIAN ? v[0] : v[1];
//        int n = Math.round(((float) v.length * quartile) / ONE_HUNDRED_PERCENT);
//        return v[n];
	}

	/**
	 * Get the quartile for a float array
	 * 
	 * @param values   the values
	 * @param quartile the quartile to find
	 * @return the quartile value
	 */
	public static int quartile(int[] values, int quartile) {

		if (values == null || values.length == 0)
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);

		if (values.length == 1)
			return values[0];

		DescriptiveStatistics ds = new DescriptiveStatistics();
		for (int d : values)
			ds.addValue(d);
		return (int) ds.getPercentile(quartile);
	}

	/**
	 * Get the quartile for a double array
	 * 
	 * @param values   the values
	 * @param quartile the quartile to find
	 * @return the quartile value
	 */
	public static double quartile(double[] values, int quartile) {

		if (values == null || values.length == 0)
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);

		if (values.length == 1)
			return values[0];

		DescriptiveStatistics ds = new DescriptiveStatistics();
		for (double d : values)
			ds.addValue(d);
		return ds.getPercentile(quartile);
	}

	/**
	 * Get the mean for a int array
	 * 
	 * @param values the values
	 * @return the mean value
	 */
	public static int mean(int[] values) {

		if (values == null || values.length == 0)
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);

		if (values.length == 1)
			return values[0];

		DescriptiveStatistics ds = new DescriptiveStatistics();
		for (int d : values)
			ds.addValue(d);
		return (int) ds.getMean();
	}

	/**
	 * Get the mean for a float array
	 * 
	 * @param values the values
	 * @return the mean value
	 */
	public static float mean(float[] values) {

		if (values == null || values.length == 0)
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);

		if (values.length == 1)
			return values[0];

		DescriptiveStatistics ds = new DescriptiveStatistics();
		for (float d : values)
			ds.addValue(d);
		return (float) ds.getMean();
	}

	/**
	 * Get the mean for a double array
	 * 
	 * @param values the values
	 * @return the mean value
	 */
	public static double mean(double[] values) {

		if (values == null || values.length == 0)
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);

		if (values.length == 1)
			return values[0];

		DescriptiveStatistics ds = new DescriptiveStatistics();
		for (double d : values)
			ds.addValue(d);
		return ds.getMean();
	}

	/**
	 * Calculate the area of the given roi
	 * 
	 * @param r
	 * @return
	 * @throws IllegalArgumentException if the roi does not enclose an area (i.e if
	 *                                  it is a line)
	 */
	public static double area(@NonNull Roi r) {
		if (!r.isArea())
			throw new IllegalArgumentException("Roi is not an area");

		return calculatePolygonArea(r);
	}

	/**
	 * Calculate the area of the given shape in pixels
	 * 
	 * @param s
	 * @return
	 */
	public static double area(@NonNull Shape s) {
		return calculateShapeIntArea(s);
	}

	/**
	 * Calculate the integer area of the shape. Checks each pixel for belonging to
	 * the shape.
	 * 
	 * @param s
	 * @return
	 */
	private static int calculateShapeIntArea(@NonNull Shape s) {
		int count = 0;
		Rectangle roiBounds = s.getBounds();
		// get the bounding box of the intersection
		// test each pixel for overlaps
		int minX = (int) roiBounds.getX();
		int maxX = minX + (int) roiBounds.getWidth();

		int minY = (int) roiBounds.getY();
		int maxY = minY + (int) roiBounds.getHeight();

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				if (s.contains(x, y)) {
					count++;
				}
			}
		}
		return count;

	}

	private static double calculatePolygonArea(@NonNull Roi r) {

		FloatPolygon f = r.getInterpolatedPolygon();
		double sum = 0;
		for (int i = 0; i < f.npoints - 1; i++) {
			sum = sum + f.xpoints[i] * f.ypoints[i + 1] - f.ypoints[i] * f.xpoints[i + 1];
		}
		return Math.abs(sum / 2);
	}

}
