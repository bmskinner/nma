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

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.analysis.signals.ShellAnalysisException;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellCounter.CountType;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellDetector;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellDetector.Shell;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellRandomDistributionCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.utility.AngleTools;

public class NuclearSignalDatasetCreator extends AbstractDatasetCreator<ChartOptions> {

    public NuclearSignalDatasetCreator(final ChartOptions o) {
        super(o);
    }

    /**
     * Get the XY coordinates of a given signal centre of mass on a nuclear
     * outline
     * 
     * @param n
     *            the signal to plos
     * @param outline
     *            the outline to draw the signal on
     * @return the point of the signal centre of mass
     * @throws Exception
     */
    public IPoint getXYCoordinatesForSignal(INuclearSignal n, Nucleus outline) throws ChartDatasetCreationException {

        double angle = n.getStatistic(PlottableStatistic.ANGLE);

        double fractionalDistance = n.getStatistic(PlottableStatistic.FRACT_DISTANCE_FROM_COM);

        // determine the distance to the border at this angle
        double distanceToBorder = outline.getDistanceFromCoMToBorderAtAngle(angle);

        // convert to fractional distance to signal
        double signalDistance = distanceToBorder * fractionalDistance;

        // adjust X and Y because we are now counting angles from the vertical
        // axis
        double signalX = new AngleTools().getXComponentOfAngle(signalDistance, angle - 90);
        double signalY = new AngleTools().getYComponentOfAngle(signalDistance, angle - 90);
        return IPoint.makeNew(signalX, signalY);
    }

    /**
     * Create a chart dataset for the centres of mass of signals in the dataset
     * 
     * @param dataset
     *            the dataset
     * @return
     * @throws Exception
     */
    public XYDataset createSignalCoMDataset(IAnalysisDataset dataset) throws ChartDatasetCreationException {
        finer("Making signal CoM dataset");
        DefaultXYDataset ds = new DefaultXYDataset();
        ICellCollection collection = dataset.getCollection();

        if (collection.getSignalManager().hasSignals()) {
            finer("Collection " + collection.getName() + " has signals");

            for (UUID uuid : collection.getSignalManager().getSignalGroupIDs()) {

                try {

                    if (dataset.getCollection().getSignalGroup(uuid).isVisible()) {
                        finest("Group " + uuid.toString() + " is visible");
                        double[] xpoints = new double[collection.getSignalManager().getSignals(uuid).size()];
                        double[] ypoints = new double[collection.getSignalManager().getSignals(uuid).size()];

                        int signalCount = 0;
                        for (INuclearSignal n : collection.getSignalManager().getSignals(uuid)) {

                            IPoint p = getXYCoordinatesForSignal(n, collection.getConsensus());

                            xpoints[signalCount] = p.getX();
                            ypoints[signalCount] = p.getY();
                            signalCount++;

                        }
                        double[][] data = { xpoints, ypoints };
                        ds.addSeries(CellularComponent.NUCLEAR_SIGNAL + "_" + uuid, data);
                        finest("Group " + uuid.toString() + " added " + signalCount + " signals");
                    }

                } catch (UnavailableSignalGroupException e) {
                    stack("Signal group " + uuid + " is not present in collection", e);
                }
            }
        }
        finer("Finished signal CoM dataset");
        return ds;
    }

    public List<Shape> createSignalRadiusDataset(IAnalysisDataset dataset, UUID signalGroup)
            throws ChartDatasetCreationException {

        ICellCollection collection = dataset.getCollection();
        List<Shape> result = new ArrayList<Shape>(0);

        try {
            if (collection.getSignalGroup(signalGroup).isVisible()) {
                if (collection.getSignalManager().hasSignals(signalGroup)) {

                    for (INuclearSignal n : collection.getSignalManager().getSignals(signalGroup)) {
                        IPoint p = getXYCoordinatesForSignal(n, collection.getConsensus());

                        // ellipses are drawn starting from x y at upper left.
                        // Provide an offset from the centre
                        double offset = n.getStatistic(PlottableStatistic.RADIUS);

                        result.add(new Ellipse2D.Double(p.getX() - offset, p.getY() - offset, offset * 2, offset * 2));
                    }

                }
            }

        } catch (UnavailableSignalGroupException e) {
            stack("Signal group " + signalGroup + " is not present in collection", e);
        }
        return result;
    }

    /**
     * Create a boxplot dataset for signal statistics
     * 
     * @param options
     *            the chart options
     * @return a boxplot dataset
     * @throws Exception
     */
    public BoxAndWhiskerCategoryDataset createSignalStatisticBoxplotDataset() throws ChartDatasetCreationException {

        return createMultiDatasetSignalStatisticBoxplotDataset();
    }

    /**
     * Create a boxplot dataset for signal statistics for a single analysis
     * dataset
     * 
     * @param dataset
     *            the AnalysisDataset to get signal info from
     * @return a boxplot dataset
     * @throws ChartDatasetCreationException
     */
    private BoxAndWhiskerCategoryDataset createMultiDatasetSignalStatisticBoxplotDataset()
            throws ChartDatasetCreationException {

        ExportableBoxAndWhiskerCategoryDataset result = new ExportableBoxAndWhiskerCategoryDataset();
        PlottableStatistic stat = options.getStat();

        for (IAnalysisDataset d : options.getDatasets()) {

            ICellCollection collection = d.getCollection();

            for (UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()) {

                double[] values = collection.getSignalManager().getSignalStatistics(stat, options.getScale(),
                        signalGroup);
                /*
                 * For charting, use offset angles, otherwise the boxplots will
                 * fail on wrapped signals
                 */
                if (stat.equals(PlottableStatistic.ANGLE)) {
                    values = collection.getSignalManager().getOffsetSignalAngles(signalGroup);
                }

                List<Double> list = new ArrayList<Double>();
                for (double value : values) {
                    list.add(value);
                }

                result.add(list, CellularComponent.NUCLEAR_SIGNAL + "_" + signalGroup, collection.getName());
            }
        }
        return result;
    }

    /**
     * Create a list of shell result datasets for each analysis dataset in the
     * given options
     * 
     * @param options
     * @return
     * @throws ChartDatasetCreationException
     */
    public List<CategoryDataset> createShellBarChartDataset() throws ChartDatasetCreationException {
        // ChartOptions op = (ChartOptions) options;
        List<CategoryDataset> result = new ArrayList<CategoryDataset>();

        for (IAnalysisDataset dataset : options.getDatasets()) {

            ShellResultDataset ds = new ShellResultDataset();

            ICellCollection collection = dataset.getCollection();

            if (collection.hasSignalGroup(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)) {
                if (options.isShowAnnotations()) {
                    addRandomShellData(ds, collection, options);
                }
            }

            for (UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()) {
            	try {
            		if(collection.getSignalGroup(signalGroup).isVisible()){
            			addRealShellData(ds, collection, options, signalGroup);
            		}
            	} catch (UnavailableSignalGroupException e) {
            		stack("Signal group not found", e);
            	}
            }
            result.add(ds);
        }
        return result;
    }

    /**
     * Create a consensus nucleus dataset overlaid with shells. Requires a
     * single dataset in the options.
     * 
     * @param options
     *            the options
     * @return a chart dataset
     * @throws ChartDatasetCreationException
     *             if the IAnalysisDataset has no shell results or the dataset
     *             count is not 1
     */
    public XYDataset createShellConsensusDataset() throws ChartDatasetCreationException {

        if (!options.isSingleDataset()) {
            throw new ChartDatasetCreationException("Single dataset required");
        }

        DefaultXYDataset ds = new DefaultXYDataset();

        // Make the shells from the consensus nucleus
        ShellDetector c;
        try {

            int shellCount = options.firstDataset().getCollection().getSignalManager().getShellCount();

            if (shellCount == 0) {
                throw new ChartDatasetCreationException("Cannot make dataset for zero shells");
            }

            c = new ShellDetector(options.firstDataset().getCollection().getConsensus(), shellCount);
        } catch (ShellAnalysisException e) {
            stack("Error making shells in consensus", e);
            throw new ChartDatasetCreationException("Error making shells", e);
        }

        // Draw the shells
        int shellNumber = 0;
        for (Shell shell : c.getShells()) {

            Polygon p = shell.toPolygon();

            double[] xpoints = new double[p.npoints + 1];
            double[] ypoints = new double[p.npoints + 1];

            for (int i = 0; i < p.npoints; i++) {

                xpoints[i] = p.xpoints[i];
                ypoints[i] = p.ypoints[i];
            }
            // complete the line
            xpoints[p.npoints] = xpoints[0];
            ypoints[p.npoints] = ypoints[0];

            double[][] data = { xpoints, ypoints };
            ds.addSeries("Shell_" + shellNumber, data);
            shellNumber++;

        }

        return ds;

    }

    /**
     * Add the simulated random data from the given collection to the result
     * dataset
     * 
     * @param ds
     *            the dataset to add values to
     * @param collection
     *            the cell collection to take random shell data from
     * @param options
     *            the chart options
     */
    private void addRandomShellData(ShellResultDataset ds, ICellCollection collection, ChartOptions options) {
        // Create the random distribution

        try {

            UUID signalGroup = ShellRandomDistributionCreator.RANDOM_SIGNAL_ID;

            // Choose between signal or nucleus level analysis
            CountType type = options.getCountType();

            boolean isNormalised = options.isNormalised();

            if (collection.getSignalGroup(signalGroup).hasShellResult()) {
                IShellResult r = collection.getSignalGroup(signalGroup).getShellResult();

                for (int shell = 0; shell < r.getNumberOfShells(); shell++) {
                    Double d = isNormalised ? r.getNormalisedMeans(type).get(shell) * 100
                            : r.getRawMeans(type).get(shell) * 100;

                    Double std = isNormalised ? r.getNormalisedStandardErrors(type).get(shell) * 100
                            : r.getRawStandardErrors(type).get(shell) * 100;

                    ds.add(signalGroup, -d.doubleValue(), std.doubleValue(),
                            "Group_" + signalGroup + "_" + collection.getName(), String.valueOf(shell));
                    // we need the string value for shell otherwise we get error
                    // "the method addValue(Number, Comparable, Comparable) is
                    // ambiguous for the type DefaultCategoryDataset"
                    // ditto the doublevalue for std

                }
            }
        } catch (UnavailableSignalGroupException e) {
            stack("Error getting random signal group", e);
        }
    }

    /**
     * Add the real shell data from the given collection to the result dataset
     * 
     * @param ds
     *            the dataset to add values to
     * @param collection
     *            the cell collection to take shell data from
     * @param options
     *            the chart options
     */
    private void addRealShellData(ShellResultDataset ds, ICellCollection collection, ChartOptions options,
            UUID signalGroup) {

        try {

            // Choose between signal or nucleus level analysis
            CountType type = options.getCountType();

            // Choose whether to display signals or pixel counts
            // boolean showSignals = options.isShowSignals();

            boolean isNormalised = options.isNormalised();

            if (collection.getSignalManager().hasSignals(signalGroup)) {
            	ISignalGroup group = collection.getSignalGroup(signalGroup);
                if (group.hasShellResult()) {
                    IShellResult r = group.getShellResult();

                    for (int shell = 0; shell < r.getNumberOfShells(); shell++) {

                        Double d = isNormalised ? r.getNormalisedMeans(type).get(shell)
                                : r.getRawMeans(type).get(shell);

                        Double std = isNormalised ? r.getNormalisedStandardErrors(type).get(shell)
                                : r.getRawStandardErrors(type).get(shell);
                        ds.add(signalGroup, d * 100, std.doubleValue() * 100,
                                "Group_" + group.getGroupName() + "_" + collection.getName(), String.valueOf(shell));
                        // we need the string value for shell otherwise we get
                        // error
                        // "the method addValue(Number, Comparable, Comparable)
                        // is ambiguous for the type DefaultCategoryDataset"
                        // ditto the doublevalue for std

                    }
                }
            }
        } catch (UnavailableSignalGroupException e) {
            stack("Error getting random signal group", e);
        }
    }

}
