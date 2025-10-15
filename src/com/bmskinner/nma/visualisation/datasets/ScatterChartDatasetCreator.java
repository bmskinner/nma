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
package com.bmskinner.nma.visualisation.datasets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nma.analysis.classification.DimensionalityReductionMethod;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.measure.MissingMeasurementException;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.signals.INuclearSignal;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.components.signals.SignalManager;
import com.bmskinner.nma.gui.dialogs.DimensionalityReductionPlotDialog.ColourByType;
import com.bmskinner.nma.visualisation.options.ChartOptions;

/**
 * Create scatter chart datasets
 * 
 * @author Ben Skinner
 *
 */
public class ScatterChartDatasetCreator extends AbstractDatasetCreator<ChartOptions> {

	private static final Logger LOGGER = Logger
			.getLogger(ScatterChartDatasetCreator.class.getName());

	/**
	 * Construct with an options
	 * 
	 * @param options the chart options
	 */
	public ScatterChartDatasetCreator(@NonNull final ChartOptions options) {
		super(options);
	}

	/**
	 * Create a scatter dataset for the given statistics for each analysis dataset
	 * 
	 * @return a charting dataset
	 * @throws ChartDatasetCreationException
	 * @throws MissingMeasurementException
	 * @throws ComponentCreationException
	 * @throws MissingLandmarkException
	 */
	public XYDataset createScatterDataset(String component) throws ChartDatasetCreationException {
		try {
			if (CellularComponent.NUCLEUS.equals(component))
				return createNucleusScatterDataset();

			if (CellularComponent.NUCLEAR_SIGNAL.equals(component))
				return createSignalScatterDataset();
		} catch (SegmentUpdateException | MissingDataException | ComponentCreationException e) {
			LOGGER.log(Level.FINE, "Error making scatter dataset: %s".formatted(e.getMessage()), e);
			throw new ChartDatasetCreationException(
					"Error creating chart dataset for %s".formatted(component), e);
		}
		throw new ChartDatasetCreationException(
				"Component for scatter chart not recognised: " + component);

	}

	/**
	 * Get a boxplot dataset for the given statistic for each collection
	 * 
	 * @param options the charting options
	 * @return
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 */
	private XYDataset createNucleusScatterDataset()
			throws SegmentUpdateException, MissingDataException, ComponentCreationException {

		final DefaultXYDataset ds = new DefaultXYDataset();

		if (!options.hasDatasets())
			return ds;

		final List<IAnalysisDataset> datasets = options.getDatasets();

		final MeasurementScale scale = options.getScale();

		final Measurement statA = options.getStat(0);
		final Measurement statB = options.getStat(1);

		for (int i = 0; i < datasets.size(); i++) {

			final ICellCollection c = datasets.get(i).getCollection();

			// to make charts more responsive, only take n nuclei
			final int count = Math.min(c.getNucleusCount(), MAX_SCATTER_CHART_ITEMS);
			final double[] xpoints = new double[count];
			final double[] ypoints = new double[count];

			final List<Nucleus> nuclei = new ArrayList<>();
			nuclei.addAll(c.getNuclei());
			Collections.shuffle(nuclei);

			for (int j = 0; j < count; j++) {
				final Nucleus n = nuclei.get(j);
				double statAValue;
				double statBValue;

				if (statA.equals(Measurement.VARIABILITY)) {
					statAValue = c.getNormalisedDifferenceToMedian(OrientationMark.REFERENCE,
							n);
				} else {
					statAValue = n.getMeasurement(statA, scale);
				}

				if (statB.equals(Measurement.VARIABILITY)) {
					statBValue = c.getNormalisedDifferenceToMedian(OrientationMark.REFERENCE,
							n);
				} else {
					statBValue = n.getMeasurement(statB, scale);
				}

				xpoints[j] = statAValue;
				ypoints[j] = statBValue;
			}

			final double[][] data = { xpoints, ypoints };
			final Comparable<?> seriesKey = new DatasetNameKey(datasets.get(i));
			ds.addSeries(seriesKey, data);

		}

		return ds;
	}

	/**
	 * Get a boxplot dataset for the given statistic for each collection
	 * 
	 * @param options the charting options
	 * @return
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 * @throws ChartDatasetCreationException
	 * @throws Exception
	 */
	private SignalXYDataset createSignalScatterDataset()
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		final List<IAnalysisDataset> datasets = options.getDatasets();

		final List<Measurement> stats = options.getStats();

		final MeasurementScale scale = options.getScale();

		final Measurement statA = stats.get(0);
		final Measurement statB = stats.get(1);

		final SignalXYDataset ds = new SignalXYDataset();

		for (int i = 0; i < datasets.size(); i++) {

			final ICellCollection c = datasets.get(i).getCollection();
			final SignalManager m = c.getSignalManager();

			for (@NonNull final
			UUID id : m.getSignalGroupIDs()) {

				final ISignalGroup gp = c.getSignalGroup(id).get();

				final int signalCount = m.getSignalCount(id);

				final double[] xpoints = new double[signalCount];
				final double[] ypoints = new double[signalCount];

				final List<INuclearSignal> list = m.getSignals(id);

				for (int j = 0; j < signalCount; j++) {
					xpoints[j] = list.get(j).getMeasurement(statA, scale);
					ypoints[j] = list.get(j).getMeasurement(statB, scale);
				}

				final double[][] data = { xpoints, ypoints };

				final Comparable<?> seriesKey = new SignalNameKey(gp, id);
				ds.addSeries(seriesKey, data);

//				final String seriesKey = c.getName() + "_" + gp.getGroupName();
//				ds.addSeries(seriesKey, data);
				ds.addDataset(datasets.get(i), seriesKey);
				ds.addSignalGroup(gp, seriesKey);
				ds.addSignalId(id, seriesKey);

			}

		}

		return ds;
	}

	/**
	 * Create a scatter dataset for dimensionality reduction plots. The plot may be
	 * coloured by merge source, or by another cluster group.
	 * 
	 * @param d           the dataset to plot
	 * @param type        the colour scheme to apply
	 * @param plotGroup   the cluster group for cell point locations
	 * @param colourGroup the cluster group for cell point colours
	 * @return
	 * @throws MissingDataException
	 * @throws ComponentCreationException
	 * @throws SegmentUpdateException
	 */
	public static XYDataset createDimensionalityReductionScatterDataset(@NonNull IAnalysisDataset d,
			@NonNull ColourByType type,
			@NonNull IClusterGroup plotGroup,
			@Nullable IClusterGroup colourGroup)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		final ComponentXYDataset<Nucleus> ds = new ComponentXYDataset<>();
		
		
		final DimensionalityReductionMethod method = DimensionalityReductionMethod
				.fromClusterGroupOptions(plotGroup.getOptions().get());

		// Choose the array measurement to use
		final Measurement measurement = switch(method) {
		case PCA -> Measurement.makePrincipalComponent(plotGroup.getId());
		case TSNE -> Measurement.makeTSNE(plotGroup.getId());
		case UMAP -> Measurement.makeUMAP(plotGroup.getId());
		case NONE -> Measurement.makeUMAP(plotGroup.getId());
		};

		if (type.equals(ColourByType.CLUSTER) && colourGroup == null) {
			type = ColourByType.NONE;
		}

		if (type.equals(ColourByType.MERGE_SOURCE) && !d.hasMergeSources()) {
			type = ColourByType.NONE;
		}

		if (type.equals(ColourByType.MERGE_SOURCE)) {
			for (final IAnalysisDataset mergeSource : d.getMergeSources()) {
				final List<Nucleus> nuclei = new ArrayList<>(mergeSource.getCollection().getNuclei());
				final double[][] data = createDimensionalityReductionValues(nuclei,
						measurement, 0, 1);
				final Comparable<?> seriesKey = new DatasetNameKey(mergeSource);
				ds.addSeries(seriesKey, data, nuclei);
			}
			return ds;
		}

		if (type.equals(ColourByType.CLUSTER) | type.equals(ColourByType.NONE)) {
			// colourGroup cannot be null here, we changed type earlier if it was
			for (final UUID childId : colourGroup.getUUIDs()) {
				final IAnalysisDataset childDataset = d.getChildDataset(childId);
				final List<Nucleus> nuclei = new ArrayList<>(childDataset.getCollection().getNuclei());
				final double[][] data = createDimensionalityReductionValues(nuclei,
						measurement, 0, 1);
				final Comparable<?> seriesKey = new DatasetNameKey(childDataset);
				ds.addSeries(seriesKey, data, nuclei);
			}
			return ds;
		}
		return ds;
	}

	/**
	 * Create a matrix of plottable values for the given array measurement. Choose
	 * which indexes of the array measurement are to be plotted on the x and y axes.
	 * 
	 * @param nuclei      the nuclei to plot
	 * @param measurement the array measurement to plot
	 * @param index0      the index of the array for the x axis (0-indexed)
	 * @param index1      the index of the array for the y axis (0-indexed)
	 * @return
	 * @throws MissingDataException
	 * @throws ComponentCreationException
	 * @throws SegmentUpdateException
	 */
	private static double[][] createDimensionalityReductionValues(List<Nucleus> nuclei,
			Measurement measurement, int index0, int index1)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		final double[] xpoints = new double[nuclei.size()];
		final double[] ypoints = new double[nuclei.size()];

		// need to transpose the matrix
		for (int i = 0; i < nuclei.size(); i++) {
			final Nucleus n = nuclei.get(i);

			final List<Double> values = n.getArrayMeasurement(measurement);

			xpoints[i] = values.get(index0);
			ypoints[i] = values.get(index1);
		}
		return new double[][] { xpoints, ypoints };
	}
}
