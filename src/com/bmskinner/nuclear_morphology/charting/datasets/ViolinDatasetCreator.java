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


package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.ViolinPlots.ViolinCategoryDataset;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
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

/**
 * Creator for violin datasets
 * 
 * @author ben
 *
 */
public class ViolinDatasetCreator extends AbstractDatasetCreator<ChartOptions> {
	
	private static final int STEP_COUNT = 100;

    /**
     * Create with options
     * 
     * @param options
     */
    public ViolinDatasetCreator(@NonNull final ChartOptions options) {
        super(options);
    }

    /**
     * Get a violin dataset for the given statistic for each dataset in the
     * options
     * 
     * @param stat
     *            the statistic to chart
     * @return a violin dataset
     * @throws ChartDatasetCreationException
     *             if any error occurs or the statistic was not recognised
     */
    public ViolinCategoryDataset createPlottableStatisticViolinDataset(String component)
            throws ChartDatasetCreationException {

        finest("Creating violin dataset for " + component);

        if (CellularComponent.WHOLE_CELL.equals(component)) {
            return createCellStatisticViolinDataset();
        }

        if (CellularComponent.NUCLEUS.equals(component)) {
            return createNucleusStatisticViolinDataset();
        }

        if (CellularComponent.NUCLEAR_SIGNAL.equals(component)) {
            return createSignalStatisticViolinDataset();
        }

        if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component)) {
            return createSegmentStatisticDataset();
        }

        throw new ChartDatasetCreationException("Component not recognised: " + component);

    }

    /*
     * 
     * PRIVATE METHODS
     * 
     * 
     */

    /**
     * Get a boxplot dataset for the given statistic for each collection
     * 
     * @param options
     *            the charting options
     * @return
     * @throws Exception
     */
    private ViolinCategoryDataset createCellStatisticViolinDataset() {
        List<IAnalysisDataset> datasets = options.getDatasets();
        PlottableStatistic stat = options.getStat();
        MeasurementScale scale = options.getScale();
        ViolinCategoryDataset ds = new ViolinCategoryDataset();

        for (int i = 0; i < datasets.size(); i++) {
            ICellCollection c = datasets.get(i).getCollection();

            String rowKey = c.getName() + "_" + i;
            String colKey = stat.toString();

            // Add the boxplot values

            double[] stats = c.getRawValues(stat, CellularComponent.WHOLE_CELL, scale);
            List<Number> list = new ArrayList<Number>();
            for (double d : stats) {
                list.add(new Double(d));
            }

            ds.add(list, rowKey, colKey);

//            addProbabilities(ds, list, rowKey, colKey);
        }

        return ds;
    }

    /**
     * Get a boxplot dataset for the given statistic for each collection
     * 
     * @param options
     *            the charting options
     * @return
     * @throws Exception
     */
    private ViolinCategoryDataset createNucleusStatisticViolinDataset() {
        List<IAnalysisDataset> datasets = options.getDatasets();
        PlottableStatistic stat = options.getStat();
        MeasurementScale scale = options.getScale();
        ViolinCategoryDataset ds = new ViolinCategoryDataset();

        for (int i = 0; i < datasets.size(); i++) {
            ICellCollection c = datasets.get(i).getCollection();

            String rowKey = c.getName() + "_" + i;
            String colKey = stat.toString();

            // Add the boxplot values

            double[] stats = c.getRawValues(stat, CellularComponent.NUCLEUS, scale);
            List<Number> list = new ArrayList<Number>();
            for (double d : stats) {
                list.add(new Double(d));
            }

            ds.add(list, rowKey, colKey);

//            addProbabilities(ds, list, rowKey, colKey);
        }

        return ds;
    }

    /**
     * Create a boxplot dataset for signal statistics for a single analysis
     * dataset
     * 
     * @param dataset
     *            the AnalysisDataset to get signal info from
     * @return a boxplot dataset
     * @throws Exception
     */
    private ViolinCategoryDataset createSignalStatisticViolinDataset() {

        List<IAnalysisDataset> datasets = options.getDatasets();
        PlottableStatistic stat = options.getStat();
        MeasurementScale scale = options.getScale();
        ViolinCategoryDataset ds = new ViolinCategoryDataset();

        for (@NonNull IAnalysisDataset d : datasets) {

            ICellCollection collection = d.getCollection();

            for (@NonNull UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()) {

                if (collection.getSignalManager().hasSignals(signalGroup)) {

                    double[] values = collection.getSignalManager().getSignalStatistics(stat, scale, signalGroup);

                    String rowKey = CellularComponent.NUCLEAR_SIGNAL + "_" + signalGroup;
                    String colKey = collection.getName();
                    /*
                     * For charting, use offset angles, otherwise the boxplots
                     * will fail on wrapped signals
                     */
                    if (stat.equals(PlottableStatistic.ANGLE))
                        values = collection.getSignalManager().getOffsetSignalAngles(signalGroup);

                    List<Number> list = new ArrayList<>();
                    for (double value : values) {
                        list.add(new Double(value));
                    }
                    if(list.size()>0)
                        ds.add(list, rowKey, colKey);
                }
            }
        }
        return ds;
    }

    /**
     * Create a box and whisker dataset for the desired segment statistic
     * 
     * @param collections
     *            the datasets to include
     * @param segName
     *            the segment to calculate for
     * @param scale
     *            the scale
     * @param stat
     *            the segment statistic to use
     * @return
     * @throws ChartDatasetCreationException
     * @throws Exception
     */
    private ViolinCategoryDataset createSegmentStatisticDataset() throws ChartDatasetCreationException {

        finest("Making segment statistic dataset");

        PlottableStatistic stat = options.getStat();

        if (stat.equals(PlottableStatistic.LENGTH)) {
            return createSegmentLengthDataset(options.getDatasets(), options.getSegPosition(), options.getScale());
        }

        if (stat.equals(PlottableStatistic.DISPLACEMENT)) {
            return createSegmentDisplacementDataset(options.getDatasets(), options.getSegPosition());
        }

        return null;

    }

    /**
     * Get the lengths of the given segment in the collections
     * 
     * @param collections
     * @param segName
     * @return
     * @throws ChartDatasetCreationException
     * @throws Exception
     */
    private ViolinCategoryDataset createSegmentLengthDataset(List<IAnalysisDataset> collections, int segPosition,
            MeasurementScale scale) throws ChartDatasetCreationException {

        ViolinCategoryDataset dataset = new ViolinCategoryDataset();

        for (int i = 0; i < collections.size(); i++) {

            ICellCollection collection = collections.get(i).getCollection();
            try {
                IBorderSegment medianSeg = collection.getProfileCollection().getSegmentAt(Tag.REFERENCE_POINT,
                        segPosition);

                List<Number> list = new ArrayList<Number>(0);

                for (Nucleus n : collection.getNuclei()) {

                    IBorderSegment seg;

                    seg = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegment(medianSeg.getID());

                    double length = 0;
                    if (seg != null) {
                        int indexLength = seg.length();
                        double proportionPerimeter = (double) indexLength / (double) seg.getProfileLength();
                        length = n.getStatistic(PlottableStatistic.PERIMETER, scale) * proportionPerimeter;

                    }
                    list.add(length);
                    // finest("Added length "+length+" to segment dataset
                    // "+seg.getName() );
                }

                String rowKey = IBorderSegment.SEGMENT_PREFIX + segPosition + "_" + i;
                String colKey = IBorderSegment.SEGMENT_PREFIX + segPosition;
                dataset.add(list, rowKey, colKey);

//                addProbabilities(dataset, list, rowKey, colKey);

            } catch (ProfileException | UnavailableComponentException e) {
                fine("Error getting segmented profile", e);
                throw new ChartDatasetCreationException("Cannot get segmented profile", e);
            }
        }

        return dataset;
    }

    /**
     * Get the displacements of the given segment in the collections
     * 
     * @param collections
     * @param segName
     * @return
     * @throws ChartDatasetCreationException
     * @throws Exception
     */
    private ViolinCategoryDataset createSegmentDisplacementDataset(List<IAnalysisDataset> collections, int segPosition)
            throws ChartDatasetCreationException {

        ViolinCategoryDataset dataset = new ViolinCategoryDataset();

        for (int i = 0; i < collections.size(); i++) {

            ICellCollection collection = collections.get(i).getCollection();

            IBorderSegment medianSeg;
            try {
                medianSeg = collection.getProfileCollection()
                        .getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN)
                        .getSegmentAt(segPosition);
            } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException
                    | UnsegmentedProfileException e) {
                stack("Unable to get segmented median profile", e);
                throw new ChartDatasetCreationException("Cannot get median profile");
            }

            List<Number> list = new ArrayList<Number>(0);

            for (Nucleus n : collection.getNuclei()) {

                try {

                    ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);

                    IBorderSegment seg = profile.getSegment(medianSeg.getID());

                    double displacement = profile.getDisplacement(seg);
                    list.add(displacement);
                } catch (ProfileException | UnavailableComponentException e) {
                    stack("Error getting segmented profile", e);
                    throw new ChartDatasetCreationException("Cannot get segmented profile", e);
                }

            }

            String rowKey = IBorderSegment.SEGMENT_PREFIX + segPosition + "_" + i;
            String colKey = IBorderSegment.SEGMENT_PREFIX + segPosition;

            dataset.add(list, rowKey, colKey);

//            addProbabilities(dataset, list, rowKey, colKey);
        }
        return dataset;
    }

//    protected synchronized void addProbabilities(ViolinCategoryDataset dataset, List<Number> list, Comparable<?> rowKey,
//            Comparable<?> colKey) {
//
//        double[] pdfValues = new double[STEP_COUNT+1];
//
//        if (list.isEmpty()) {
//            Range r = new Range(0, 0);
//            dataset.addProbabilityRange(r, rowKey, colKey);
//            dataset.addProbabilities(pdfValues, rowKey, colKey);
//            return;
//        }
//
//        double total = list.stream().mapToDouble(n->n.doubleValue()).sum();
//        double min = list.stream().mapToDouble(n->n.doubleValue()).min().orElse(0);
//        double max = list.stream().mapToDouble(n->n.doubleValue()).max().orElse(0);
//
//        // If all values are the same, min==max, and there will be a step error
//        // calculating values between them for pdf
//        if (list.size() > 2 && total > 0 && min < max) { // don't bother with  a dataset of a single cell, or if the  stat  is not present
//
//            double stepSize = (max - min) / STEP_COUNT;
//
//            KernelEstimator est = new NucleusDatasetCreator(options).createProbabililtyKernel(list, 0.001);
//
//            for(int i=0; i<STEP_COUNT; i++){
//            	double v = min+(stepSize*i);
//            	pdfValues[i] = est.getProbability(v);
//            }
//            // ensure last value in the array is at yMax; allows the renderer to have a flat top
//            pdfValues[STEP_COUNT] = est.getProbability(max);
//
//            Range r = new Range(min, max);
//            dataset.addProbabilityRange(r, rowKey, colKey);
//        }
//        dataset.addProbabilities(pdfValues, rowKey, colKey);
//
//    }

}
