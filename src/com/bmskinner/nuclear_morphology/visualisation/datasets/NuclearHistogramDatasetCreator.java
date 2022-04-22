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
package com.bmskinner.nuclear_morphology.visualisation.datasets;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;

import weka.estimators.KernelEstimator;

/**
 * Create histograms for nuclear statistics
 * 
 * @author ben
 * @since 1.13.8
 *
 */
public class NuclearHistogramDatasetCreator extends HistogramDatasetCreator {

	private static final Logger LOGGER = Logger.getLogger(NuclearHistogramDatasetCreator.class.getName());

	public NuclearHistogramDatasetCreator(final ChartOptions o) {
		super(o);
	}

	public HistogramDataset createNuclearStatsHistogramDataset() throws ChartDatasetCreationException {
		HistogramDataset ds = new HistogramDataset();

		LOGGER.finest("Creating histogram dataset: " + options.getMeasurement());
		if (!options.hasDatasets()) {
			return ds;
		}

		for (IAnalysisDataset dataset : options.getDatasets()) {

			ICellCollection collection = dataset.getCollection();

			Measurement stat = options.getMeasurement();
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
		Measurement stat = options.getMeasurement();
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

			List<Double> xValues = new ArrayList<>();
			List<Double> yValues = new ArrayList<>();

			for (double i = minMaxRange[0]; i <= minMaxRange[1]; i += minMax[STEP_SIZE]) {

				xValues.add(i);
				yValues.add(est.getProbability(i));

			}

			// Make into an array of arrays

			double[] xData = xValues.stream().mapToDouble(d -> d.doubleValue()).toArray();
			double[] yData = yValues.stream().mapToDouble(d -> d.doubleValue()).toArray();

			double[][] data = { xData, yData };

			ds.addSeries(groupLabel + "_" + collection.getName(), data);

		}
		return ds;
	}
}
