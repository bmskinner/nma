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

import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.datasets.NucleusDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.stats.Stats;

import weka.estimators.KernelEstimator;

/**
 * Create histograms for nuclear statistics
 * @author ben
 * @since 1.13.8
 *
 */
public class NuclearHistogramDatasetCreator extends HistogramDatasetCreator {

    public NuclearHistogramDatasetCreator(final ChartOptions o) {
        super(o);
    }

    public HistogramDataset createNuclearStatsHistogramDataset() throws ChartDatasetCreationException {
        HistogramDataset ds = new HistogramDataset();

        finest("Creating histogram dataset: " + options.getStat());
        if (!options.hasDatasets()) {
            return ds;
        }

        for (IAnalysisDataset dataset : options.getDatasets()) {

            ICellCollection collection = dataset.getCollection();

            PlottableStatistic stat = options.getStat();
            double[] values = collection.getRawValues(stat, CellularComponent.NUCLEUS, options.getScale());

            double[] minMaxStep = findMinAndMaxForHistogram(values);
            int minRounded = (int) minMaxStep[0];
            int maxRounded = (int) minMaxStep[1];

            int bins = findNumberOfBins(values, minRounded, maxRounded, minMaxStep[2]);

            String groupLabel = stat.toString();

            if (minRounded >= maxRounded) {
                throw new ChartDatasetCreationException("Histogram lower bound equal to or grater than upper bound");
            }

            ds.addSeries(groupLabel + "_" + collection.getName(), values, bins, minRounded, maxRounded);
        }

        return ds;
    }

    

    /**
     * Make an XY dataset corresponding to the probability density of a given
     * nuclear statistic
     * 
     * @return a charting dataset
     * @throws ChartDatasetCreationException
     */
    public XYDataset createNuclearDensityHistogramDataset() throws ChartDatasetCreationException {
        DefaultXYDataset ds = new DefaultXYDataset();

        if (!options.hasDatasets()) {
            return ds;
        }

        List<IAnalysisDataset> list = options.getDatasets();
        PlottableStatistic stat = options.getStat();
        MeasurementScale scale = options.getScale();

        int[] minMaxRange = calculateMinAndMaxRange(list, stat, CellularComponent.NUCLEUS, scale);

        for (IAnalysisDataset dataset : list) {
            ICellCollection collection = dataset.getCollection();

            String groupLabel = stat.toString();
            double[] values = collection.getRawValues(stat, CellularComponent.NUCLEUS, scale);

            KernelEstimator est;
            try {
                est = new NucleusDatasetCreator(options).createProbabililtyKernel(values, 0.001);
            } catch (Exception e1) {
                throw new ChartDatasetCreationException("Cannot make probability kernel", e1);
            }

            double[] minMax = findMinAndMaxForHistogram(values);

            
            List<Double> xValues = new ArrayList<Double>();
            List<Double> yValues = new ArrayList<Double>();

            for (double i = minMaxRange[0]; i <= minMaxRange[1]; i += minMax[STEP_SIZE]) {

                xValues.add(i);
                yValues.add(est.getProbability(i));

            }

            // Make into an array of arrays

            double[] xData = xValues.stream().mapToDouble(d->d.doubleValue()).toArray();
            double[] yData = yValues.stream().mapToDouble(d->d.doubleValue()).toArray();

            double[][] data = { xData, yData };

            ds.addSeries(groupLabel + "_" + collection.getName(), data);

        }
        return ds;
    }


    /**
     * Create a histogram of segment lengths in the nuclei
     * @return
     * @throws ChartDatasetCreationException
     */
    public HistogramDataset createSegmentLengthHistogramDataset() throws ChartDatasetCreationException {
        HistogramDataset ds = new HistogramDataset();

        finest("Creating histogram dataset: " + options.getStat());

        if (!options.hasDatasets()) {
            return ds;
        }

        for (IAnalysisDataset dataset : options.getDatasets()) {

            ICellCollection collection = dataset.getCollection();

            /*
             * Find the seg id for the median segment at the requested position
             */

            try {

                IBorderSegment medianSeg = collection.getProfileCollection().getSegmentAt(Tag.REFERENCE_POINT,
                        options.getSegPosition());

                /*
                 * Use the segment id for this collection to fetch the
                 * individual nucleus segments
                 */

                double[] values;

                values = collection.getRawValues(PlottableStatistic.LENGTH,
                        CellularComponent.NUCLEAR_BORDER_SEGMENT, options.getScale(), medianSeg.getID());

                double[] minMaxStep = findMinAndMaxForHistogram(values);
                int minRounded = (int) minMaxStep[0];
                int maxRounded = (int) minMaxStep[1];

                int bins = findNumberOfBins(values, minRounded, maxRounded, minMaxStep[2]);
                // int bins = findBinSizeForHistogram(values, minMaxStep);

                ds.addSeries(IBorderSegment.SEGMENT_PREFIX + options.getSegPosition() + "_" + collection.getName(),
                        values, bins, minRounded, maxRounded);
            } catch (UnavailableBorderTagException | ProfileException e) {
                throw new ChartDatasetCreationException("Cannot get segments for " + dataset.getName(), e);
            }
        }

        finest("Completed histogram dataset");
        return ds;
    }

    /**
     * Get the lengths of the given segment in the collections
     * 
     * @return
     * @throws ChartDatasetCreationException
     */
    public XYDataset createSegmentLengthDensityDataset() throws ChartDatasetCreationException {

        int[] minMaxRange = { Integer.MAX_VALUE, 0 }; // start with extremes,
                                                      // trim to fit data
        for (IAnalysisDataset dataset : options.getDatasets()) {
            ICellCollection collection = dataset.getCollection();

            /*
             * Find the seg id for the median segment at the requested position
             */
            IBorderSegment medianSeg;

            try {
                medianSeg = collection.getProfileCollection()
                        .getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN)
                        .getSegmentAt(options.getSegPosition());
            } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException
                    | UnsegmentedProfileException e) {
                fine("Error getting profile from tag", e);
                throw new ChartDatasetCreationException("Unable to get median profile", e);
            }

            /*
             * Use the segment id for this collection to fetch the individual
             * nucleus segments
             */
            int count = 0;
            double[] lengths = new double[collection.size()];
            for (Nucleus n : collection.getNuclei()) {

                IBorderSegment seg;
                try {
                    seg = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegment(medianSeg.getID());
                    int indexLength = seg.length();
                    double proportionPerimeter = (double) indexLength / (double) seg.getProfileLength();
                    double length = n.getStatistic(PlottableStatistic.PERIMETER, options.getScale())
                            * proportionPerimeter;
                    lengths[count] = length;
                } catch (ProfileException | UnavailableComponentException e) {
                    fine("Error getting segment length");
                    lengths[count] = 0;
                } finally {
                    count++;
                }

            }

            minMaxRange = updateMinMaxRange(minMaxRange, lengths);
        }

        // Ranges are found, now make the kernel
        DefaultXYDataset ds = new DefaultXYDataset();

        for (IAnalysisDataset dataset : options.getDatasets()) {
            ICellCollection collection = dataset.getCollection();
            /*
             * Find the seg id for the median segment at the requested position
             */
            IBorderSegment medianSeg;
            try {
                medianSeg = collection.getProfileCollection()
                        .getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN)
                        .getSegmentAt(options.getSegPosition());
            } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException
                    | UnsegmentedProfileException e2) {
                fine("Error getting profile from tag", e2);
                throw new ChartDatasetCreationException("Unable to get median profile", e2);
            }

            /*
             * Use the segment id for this collection to fetch the individual
             * nucleus segments
             */
            int count = 0;
            double[] lengths = new double[collection.size()];
            for (Nucleus n : collection.getNuclei()) {

                IBorderSegment seg;
                try {
                    seg = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegment(medianSeg.getID());

                    int indexLength = seg.length();
                    double proportionPerimeter = (double) indexLength / (double) seg.getProfileLength();
                    double length = n.getStatistic(PlottableStatistic.PERIMETER, options.getScale())
                            * proportionPerimeter;
                    lengths[count] = length;
                } catch (ProfileException | UnavailableComponentException e) {
                    fine("Error getting segment length");
                    lengths[count] = 0;
                } finally {
                    count++;
                }

            }

            KernelEstimator est;
            try {
                est = new NucleusDatasetCreator(options).createProbabililtyKernel(lengths, 0.001);
            } catch (Exception e1) {
                throw new ChartDatasetCreationException("Cannot make probability kernel", e1);
            }

            double min = Arrays.stream(lengths).min().orElse(0); // Stats.min(values);
            double max = Arrays.stream(lengths).max().orElse(0); // Stats.max(values);

            int log = (int) Math.floor(Math.log10(min)); // get the log scale

            int roundLog = log - 1 == 0 ? log - 2 : log - 1;
            double roundAbs = Math.pow(10, roundLog);

            int binLog = log - 2;
            double stepSize = Math.pow(10, binLog);

            // IJ.log(" roundLog: "+roundLog);
            // IJ.log(" round to nearest: "+roundAbs);

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

            List<Double> xValues = new ArrayList<Double>();
            List<Double> yValues = new ArrayList<Double>();

            for (double i = minMaxRange[0]; i <= minMaxRange[1]; i += stepSize) {
                xValues.add(i);
                yValues.add(est.getProbability(i));
            }

            double[] xData = xValues.stream().mapToDouble(d->d.doubleValue()).toArray();
            double[] yData = yValues.stream().mapToDouble(d->d.doubleValue()).toArray();

            double[][] data = { xData, yData };

            ds.addSeries(IBorderSegment.SEGMENT_PREFIX + options.getSegPosition() + "_" + collection.getName(), data);
        }

        return ds;
    }

}
