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
package com.bmskinner.nuclear_morphology.charting.datasets.charts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.jfree.data.Range;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;

import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.datasets.NucleusDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusType;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import weka.estimators.KernelEstimator;

/**
 * Create histograms for signal statistics
 * @author ben
 * @since 1.13.8
 *
 */
public class SignalHistogramDatasetCreator extends HistogramDatasetCreator {
	
	private static final Logger LOGGER = Logger.getLogger(SignalHistogramDatasetCreator.class.getName());

	public SignalHistogramDatasetCreator(final ChartOptions o) {
		super(o);
	}

	/**
     * Create a histogram dataset covering the signal statistic for the given
     * analysis datasets
     * 
     * @param list
     *            the list of datasets
     * @return a histogram of angles
     * @throws Exception
     */
    public List<HistogramDataset> createSignalStatisticHistogramDataset(List<IAnalysisDataset> list,
            Measurement stat, MeasurementScale scale) throws ChartDatasetCreationException {

        List<HistogramDataset> result = new ArrayList<HistogramDataset>();

        for (IAnalysisDataset dataset : list) {
            HistogramDataset ds = new HistogramDataset();
            ICellCollection collection = dataset.getCollection();

            for (UUID signalGroup : dataset.getCollection().getSignalManager().getSignalGroupIDs()) {

                if (collection.getSignalGroup(signalGroup).get().isVisible()) {

				    if (collection.getSignalManager().hasSignals(signalGroup)) {

				        double[] values = collection.getSignalManager().getSignalStatistics(stat, scale,
				                signalGroup);

				        ds.addSeries(
				                CellularComponent.NUCLEAR_SIGNAL + "_" + signalGroup + "_" + collection.getName(),
				                values, 12);
				    }
				}
            }
            result.add(ds);

        }
        return result;
    }
    
    /**
     * Make an XY dataset corresponding to the probability density of a given
     * nuclear statistic
     * 
     * @param list
     *            the datasets to draw
     * @param stat
     *            the statistic to measure
     * @return a charting dataset
     * @throws Exception
     */
    public List<DefaultXYDataset> createSignalDensityHistogramDataset() throws ChartDatasetCreationException {

        List<IAnalysisDataset> list = options.getDatasets();
        Measurement stat = options.getStat();
        MeasurementScale scale = options.getScale();

        List<DefaultXYDataset> result = new ArrayList<DefaultXYDataset>();

        Range range = calculateMinAndMaxRange(list, stat, scale);
//        int[] minMaxRange = calculateMinAndMaxRange(list, stat, scale);

        for (IAnalysisDataset dataset : list) {

            DefaultXYDataset ds = new DefaultXYDataset();

            ICellCollection collection = dataset.getCollection();

            for (UUID uuid : collection.getSignalManager().getSignalGroupIDs()) {

                String groupLabel = CellularComponent.NUCLEAR_SIGNAL + "_" + uuid + "_" + stat.toString();
                boolean ignoreSignalGroup = false;

                // If the angle is always zero, the estimator will fail
                if (collection.getNucleusType().equals(NucleusType.ROUND) && stat.equals(Measurement.ANGLE)) {
                    ignoreSignalGroup = true;
                }

                // If the group is present but empty, the estimator will fail
                if (!collection.getSignalManager().hasSignals(uuid)) {
                    ignoreSignalGroup = true;
                }

                // Skip if needed
                if (ignoreSignalGroup) {
                    // Add an empty series
                    double[] xData = { 0 };
                    double[] yData = { 0 };
                    double[][] data = { xData, yData };
                    ds.addSeries(groupLabel, data);
                    continue;
                }

                double[] values = dataset.getCollection()
	                	.getSignalManager()
	                	.getSignalStatistics(stat, scale, uuid);

                // Cannot estimate pdf with too few values
                if (values.length < 3) {
                    // Add an empty series
                    double[] xData = { 0 };
                    double[] yData = { 0 };
                    double[][] data = { xData, yData };
                    ds.addSeries(groupLabel, data);
                    continue;
                }

                KernelEstimator est;
                try {
                    est = new NucleusDatasetCreator(options).createProbabililtyKernel(values, 0.001);
                } catch (Exception e1) {
                    LOGGER.log(Loggable.STACK, "Error creating probability kernel", e1);
                    throw new ChartDatasetCreationException("Cannot make probability dataset", e1);
                }

                double min = Arrays.stream(values).min().orElse(0); // Stats.min(values);
                double max = Arrays.stream(values).max().orElse(0); // Stats.max(values);

                int log = (int) Math.floor(Math.log10(min)); // get the log
                                                             // scale

                int roundLog = log - 1 == 0 ? log - 2 : log - 1;
                double roundAbs = Math.pow(10, roundLog);

                int binLog = log - 2;
                double stepSize = Math.pow(10, binLog);

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
                minRounded = minRounded < 0 ? 0 : minRounded; // ensure all
                                                              // measures start
                                                              // from at least
                                                              // zero

                List<Double> xValues = new ArrayList<Double>();
                List<Double> yValues = new ArrayList<Double>();

                for (double i = range.getLowerBound(); i <= range.getUpperBound(); i += stepSize) {
                    xValues.add(i);
                    yValues.add(est.getProbability(i));
                }
                
                double[] xData = xValues.stream().mapToDouble(d->d.doubleValue()).toArray();
                double[] yData = yValues.stream().mapToDouble(d->d.doubleValue()).toArray();


                double[][] data = { xData, yData };

                ds.addSeries(groupLabel, data);

            }
            result.add(ds);
        }

        return result;
    }
    
    /**
     * Given a dataset and a stats parameter, get the values for that stat
     * 
     * @param dataset
     *            the Analysis Dataset
     * @param stat
     *            the statistic to fetch
     * @param scale
     *            the scale to display at
     * @return the array of values
     * @throws Exception
     */
//    private double[] findSignalDatasetValues(IAnalysisDataset dataset, PlottableStatistic stat, MeasurementScale scale,
//            UUID signalGroup) throws ChartDatasetCreationException {
//
//        ICellCollection collection = dataset.getCollection();
//        return collection.getSignalManager().getSignalStatistics(stat, scale, signalGroup);
//    }
    
    /**
     * Calculate the minimum and maximum ranges in a list of datasets for the
     * given stat type
     * 
     * @param list
     *            the datasets
     * @param stat
     *            the statistic to use
     * @return a Range covering all the data
     * @throws Exception
     */
    private Range calculateMinAndMaxRange(List<IAnalysisDataset> list, Measurement stat, MeasurementScale scale)
            throws ChartDatasetCreationException {

    	
    	Range r = null; 

        for (IAnalysisDataset dataset : list) {

            for (UUID signalGroup : dataset.getCollection().getSignalManager().getSignalGroupIDs()) {

                if (dataset.getCollection().getSignalManager().hasSignals(signalGroup)) {

                	double[] values = dataset.getCollection()
	                	.getSignalManager()
	                	.getSignalStatistics(stat, scale, signalGroup);
                    
                    double min = Arrays.stream(values).min().orElse(0);
                    double max = Arrays.stream(values).max().orElse(0);
                    if(r==null){
                    	r=new Range(min,max );
                    } else {
                    	r = Range.combine(r, new Range(min,max ));
                    }
                }

            }
        }

        return r;
    }

}
