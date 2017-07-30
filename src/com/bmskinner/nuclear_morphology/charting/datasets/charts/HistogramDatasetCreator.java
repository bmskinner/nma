package com.bmskinner.nuclear_morphology.charting.datasets.charts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.charting.datasets.AbstractDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.datasets.NucleusDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.stats.Max;
import com.bmskinner.nuclear_morphology.stats.Min;
import com.bmskinner.nuclear_morphology.utility.ArrayConverter;
import com.bmskinner.nuclear_morphology.utility.ArrayConverter.ArrayConversionException;

import weka.estimators.KernelEstimator;

/**
 * Abstract base for generating histogram datasets.
 * @author ben
 * @since 1.13.8
 *
 */
public abstract class HistogramDatasetCreator extends AbstractDatasetCreator<ChartOptions> {
	
	public static final int MIN_ROUNDED = 0;
    public static final int MAX_ROUNDED = 1;
    public static final int STEP_SIZE   = 2;
    
    public HistogramDatasetCreator(final ChartOptions o) {
        super(o);
    }

    protected double[] findMinAndMaxForHistogram(double[] values) {
        double min = Arrays.stream(values).min().orElse(0); // Stats.min(values);
        double max = Arrays.stream(values).max().orElse(0); // Stats.max(values);

        int log = (int) Math.floor(Math.log10(min)); // get the log scale

        int roundLog = log - 1 == 0 ? log - 2 : log - 1; // get the nearest log
                                                         // value that is not
                                                         // zero
        double roundAbs = Math.pow(10, roundLog); // find the absolute value of
                                                  // the log

        int binLog = log - 2; // get a value for the bin sizes that is 1/100 of
                              // the main log
        double stepSize = Math.pow(10, binLog); // turn the log into an absolute
                                                // step size

        finest("Range finding: binLog: " + binLog + "; step: " + stepSize);
        // If stepsize is < 1 for stats that increment in steps of 1, we will
        // get blanks in the histogram
        // Correct based on the stat.
        if (stepSize <= 1) {

            // Only worry if there are non integer values in the array
            boolean isInteger = true;
            for (double value : values) {
                // Check is an integer equivalent
                if (value != Math.floor(value)) {
                    isInteger = false;
                }
            }

            if (isInteger) {
                finest("Detected integer only values: setting histogram step size to 1");
                stepSize = 1;
            } else {
                finest("Non-integer values: setting histogram step size to " + stepSize);
            }

        }

        // use int truncation to round to nearest 100 above max
        int maxRounded = (int) ((((int) max + (roundAbs)) / roundAbs) * roundAbs);
        maxRounded = roundAbs > 1 ? maxRounded + (int) roundAbs : maxRounded + 1; // correct
                                                                                  // offsets
                                                                                  // for
                                                                                  // measures
                                                                                  // between
                                                                                  // 0-1
        int minRounded = (int) (((((int) min + (roundAbs)) / roundAbs) * roundAbs) - roundAbs);
        minRounded = roundAbs > 1 ? minRounded - (int) roundAbs : minRounded - 1; // correct
                                                                                  // offsets
                                                                                  // for
                                                                                  // measures
                                                                                  // between
                                                                                  // 0-1
        minRounded = minRounded < 0 ? 0 : minRounded; // ensure all measures
                                                      // start from at least
                                                      // zero

        double[] result = new double[3];
        result[0] = minRounded;
        result[1] = maxRounded;
        result[2] = stepSize;
        return result;
    }
    
    /**
     * Calculate the minimum and maximum ranges in a list of datasets for the
     * given stat type
     * 
     * @param list
     *            the datasets
     * @param stat
     *            the statistic to use
     * @return an array with the min and max of the range
     * @throws Exception
     */
    protected static int[] calculateMinAndMaxRange(List<IAnalysisDataset> list, PlottableStatistic stat, String component,
            MeasurementScale scale) throws ChartDatasetCreationException {

        int[] result = new int[2];
        result[0] = Integer.MAX_VALUE; // holds min
        result[1] = 0; // holds max

        for (IAnalysisDataset dataset : list) {

            double[] values = dataset.getCollection().getRawValues(stat, component, scale);

            updateMinMaxRange(result, values);
        }

        return result;
    }

    protected int findNumberOfBins(double[] values, int min, int max, double stepSize) {
        //
        // int minRounded = (int) minMaxStep[0];
        // int maxRounded = (int) minMaxStep[1];
        // double stepSize= minMaxStep[2];

        int bins = (int) (((double) max - (double) min) / stepSize);

        if (stepSize == 1d) {
            bins = max - min; // set integer steps directly
        }

        bins = bins > 100 ? 100 : bins; // but don't have too many bins

        bins = bins < 1 ? 11 : bins; // and also don't have too few bins, or the
                                     // chart looks silly
        return bins;
    }
    
    /**
     * Given an existing range for an axis scale, check if the range must be
     * expanded for the given set of values
     * 
     * @param range
     *            the existing min and max
     * @param values
     *            the new values
     * @return
     */
    public static int[] updateMinMaxRange(int[] range, double[] values) {

        double min = Arrays.stream(values).min().orElse(0); // Stats.min(values);
        double max = Arrays.stream(values).max().orElse(0); // Stats.max(values);

        int log = (int) Math.floor(Math.log10(min)); // get the log scale

        int roundLog = log - 1 == 0 ? log - 2 : log - 1;
        double roundAbs = Math.pow(10, roundLog);

        // use int truncation to round to nearest 100 above max
        int maxRounded = (int) ((((int) max + (roundAbs)) / roundAbs) * roundAbs);
        maxRounded = roundAbs > 1 ? maxRounded + (int) roundAbs : maxRounded + 1; // correct
                                                                                  // offsets
                                                                                  // for
                                                                                  // measures
                                                                                  // between
                                                                                  // 0-1
        int minRounded = (int) (((((int) min + (roundAbs)) / roundAbs) * roundAbs) - roundAbs);
        minRounded = roundAbs > 1 ? minRounded - (int) roundAbs : minRounded - 1; // correct
                                                                                  // offsets
                                                                                  // for
                                                                                  // measures
                                                                                  // between
                                                                                  // 0-1
        minRounded = minRounded < 0 ? 0 : minRounded; // ensure all measures
                                                      // start from at least
                                                      // zero

        range[0] = range[0] < minRounded ? range[0] : minRounded;
        range[1] = range[1] > maxRounded ? range[1] : maxRounded;

        return range;
    }
    
    /**
     * Create a histogram dataset from a list of double values
     * 
     * @param list
     * @return
     * @throws Exception
     */
    public static HistogramDataset createHistogramDatasetFromList(List<Double> list)
            throws ChartDatasetCreationException {
        HistogramDataset ds = new HistogramDataset();
        if (!list.isEmpty()) {

            double[] values;

            try {
                values = new ArrayConverter(list).toDoubleArray();

            } catch (ArrayConversionException e) {
                values = new double[0];
            }

            double min = new Min(list).doubleValue();
            double max = new Max(list).doubleValue();
            int bins = 100;

            ds.addSeries("Sample", values, bins, min, max);
        }
        return ds;
    }

    public XYDataset createDensityDatasetFromList(List<Double> list, double binWidth)
            throws ChartDatasetCreationException {
        DefaultXYDataset ds = new DefaultXYDataset();
        if (!list.isEmpty()) {

            double[] values;

            try {
                values = new ArrayConverter(list).toDoubleArray();

            } catch (ArrayConversionException e) {
                values = new double[0];
            }

            KernelEstimator est;
            try {
                est = new NucleusDatasetCreator(options).createProbabililtyKernel(values, binWidth);
            } catch (Exception e1) {
                throw new ChartDatasetCreationException("Cannot make probability kernel", e1);
            }

            List<Double> xValues = new ArrayList<Double>();
            List<Double> yValues = new ArrayList<Double>();

            double min = new Min(list).doubleValue();
            double max = new Max(list).doubleValue();

            for (double i = min; i <= max; i += 0.0001) {
                xValues.add(i);
                yValues.add(est.getProbability(i));
            }

            double[] xData;
            double[] yData;

            try {

                xData = new ArrayConverter(xValues).toDoubleArray();
                yData = new ArrayConverter(yValues).toDoubleArray();

            } catch (ArrayConversionException e) {
                xData = new double[0];
                yData = new double[0];
            }
            double[][] data = { xData, yData };

            ds.addSeries("Density", data);
        }
        return ds;

    }
        
}
