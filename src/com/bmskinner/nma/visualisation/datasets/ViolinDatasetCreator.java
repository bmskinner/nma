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
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.stats.Stats;
import com.bmskinner.nma.visualisation.options.ChartOptions;

/**
 * Creator for violin datasets
 * 
 * @author Ben Skinner
 *
 */
public class ViolinDatasetCreator extends AbstractDatasetCreator<ChartOptions> {

	private static final Logger LOGGER = Logger.getLogger(ViolinDatasetCreator.class.getName());

//	private static final int STEP_COUNT = 100;

	/**
	 * Create with options
	 * 
	 * @param options
	 */
	public ViolinDatasetCreator(@NonNull final ChartOptions options) {
		super(options);
	}

	/**
	 * Get a violin dataset for the given statistic for each dataset in the options
	 * 
	 * @param stat the statistic to chart
	 * @return a violin dataset
	 * @throws ChartDatasetCreationException if any error occurs or the statistic
	 *                                       was not recognised
	 */
	public synchronized ViolinCategoryDataset createPlottableStatisticViolinDataset(
			@NonNull String component)
			throws ChartDatasetCreationException {

		try {
			if (CellularComponent.WHOLE_CELL.equals(component))
				return createCellStatisticViolinDataset();

			if (CellularComponent.NUCLEUS.equals(component))
				return createNucleusStatisticViolinDataset();

			if (CellularComponent.NUCLEAR_SIGNAL.equals(component))
				return createSignalStatisticViolinDataset();

			if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component))
				return createSegmentStatisticDataset();

		} catch (MissingDataException | SegmentUpdateException e) {
			throw new ChartDatasetCreationException(
					"Error making violin dataset for %s".formatted(component));
		}
		throw new ChartDatasetCreationException(
				"Component not recognised: %s".formatted(component));

	}

	/**
	 * Get a boxplot dataset for the given statistic for each collection
	 * 
	 * @param options the charting options
	 * @return
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 * @throws Exception
	 */
	private ViolinCategoryDataset createCellStatisticViolinDataset()
			throws MissingDataException, SegmentUpdateException {
		final List<IAnalysisDataset> datasets = options.getDatasets();
		final Measurement stat = options.getMeasurement();
		final MeasurementScale scale = options.getScale();
		final ViolinCategoryDataset ds = new ViolinCategoryDataset();

		for (int i = 0; i < datasets.size(); i++) {
			final ICellCollection c = datasets.get(i).getCollection();

			final String rowKey = c.getName() + "_" + i;
			final String colKey = stat.toString();

			// Add the boxplot values

			final double[] stats = c.getRawValues(stat, CellularComponent.WHOLE_CELL, scale);
			final List<Number> list = new ArrayList<>();
			for (final double d : stats) {
				list.add(d);
			}

			ds.add(list, rowKey, colKey);
		}

		return ds;
	}

	/**
	 * Get a boxplot dataset for the given statistic for each collection
	 * 
	 * @return
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 * @throws Exception
	 */
	private ViolinCategoryDataset createNucleusStatisticViolinDataset()
			throws MissingDataException, SegmentUpdateException {
		final List<IAnalysisDataset> datasets = options.getDatasets();
		final Measurement stat = options.getMeasurement();
		final MeasurementScale scale = options.getScale();
		final ViolinCategoryDataset ds = new ViolinCategoryDataset();

		for (int i = 0; i < datasets.size(); i++) {
			final ICellCollection c = datasets.get(i).getCollection();

			final String rowKey = c.getName() + "_" + i;
			final String colKey = stat.toString();

			// Add the boxplot values
			final double[] stats = c.getRawValues(stat, CellularComponent.NUCLEUS, scale);
			final List<Number> list = new ArrayList<>();
			for (final double d : stats) {
				list.add(d);
			}

			ds.add(list, rowKey, colKey);
		}

		return ds;
	}

	/**
	 * Create a violin dataset for signal statistics for a single analysis dataset
	 * 
	 * @param dataset the analysis dataset to get signal info from
	 * @return a violin dataset
	 */
	private ViolinCategoryDataset createSignalStatisticViolinDataset() {

		final List<IAnalysisDataset> datasets = options.getDatasets();
		final Measurement stat = options.getMeasurement();
		final MeasurementScale scale = options.getScale();
		final ViolinCategoryDataset ds = new ViolinCategoryDataset();
		for (@NonNull final
		IAnalysisDataset d : datasets) {

			final ICellCollection collection = d.getCollection();

			for (@NonNull final
			UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()) {

				if (collection.getSignalManager().hasSignals(signalGroup)) {

					final ISignalGroup group = collection.getSignalGroup(signalGroup).get();

					double[] values = collection.getSignalManager().getSignalStatistics(stat, scale,
							signalGroup);

					final String rowKey = CellularComponent.NUCLEAR_SIGNAL + "_" + signalGroup + "_"
							+ group.getGroupName();
					final String colKey = collection.getName();
					/*
					 * For charting, use offset angles, otherwise the boxplots will fail on wrapped
					 * signals
					 */
					if (stat.equals(Measurement.ANGLE)) {
						values = collection.getSignalManager().getOffsetSignalAngles(signalGroup);
					}

					final List<Number> list = new ArrayList<>();
					for (final double value : values) {
						list.add(value);
					}
					if (!list.isEmpty()) {
						ds.add(list, rowKey, colKey);
					}
				}
			}
		}
		return ds;
	}

	/**
	 * Create a violin dataset for the desired segment statistic
	 * 
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	private synchronized ViolinCategoryDataset createSegmentStatisticDataset()
			throws ChartDatasetCreationException {

		LOGGER.finest("Making segment statistic dataset");

		final Measurement stat = options.getMeasurement();

		if (stat.equals(Measurement.LENGTH))
			return createSegmentLengthDataset(options.getDatasets(), options.getSegPosition());

		if (stat.equals(Measurement.DISPLACEMENT))
			return createSegmentDisplacementDataset(options.getDatasets(),
					options.getSegPosition());

		return null;

	}

	/**
	 * Get the lengths of the given segment in the collections
	 * 
	 * @param datasets
	 * @param segPosition
	 * @return
	 * @throws ChartDatasetCreationException
	 * @throws Exception
	 */
	private synchronized ViolinCategoryDataset createSegmentLengthDataset(
			@NonNull final List<IAnalysisDataset> datasets, final int segPosition)
			throws ChartDatasetCreationException {

		final ViolinCategoryDataset dataset = new ViolinCategoryDataset();

		for (int i = 0; i < datasets.size(); i++) {

			final ICellCollection collection = datasets.get(i).getCollection();
			try {
				final IProfileSegment medianSeg = collection.getProfileCollection().getSegmentAt(
						OrientationMark.REFERENCE,
						segPosition);

				final List<Number> list = new ArrayList<>();

				for (final Nucleus n : collection.getNuclei()) {

					try {

						final IProfileSegment seg = n
								.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE)
								.getSegment(medianSeg.getID());

						double length = 0;
						final int indexLength = seg.length();
						final double proportionPerimeter = (double) indexLength
								/ (double) seg.getProfileLength();
						length = n.getMeasurement(Measurement.PERIMETER, options.getScale())
								* proportionPerimeter;
						list.add(length);

					} catch (MissingDataException | ComponentCreationException e) {
						throw new ChartDatasetCreationException(
								"Error fetching segment for nucleus " + n.getNameAndNumber(), e);
					}
				}

				final String rowKey = IProfileSegment.SEGMENT_PREFIX + segPosition + "_" + i;
				final String colKey = IProfileSegment.SEGMENT_PREFIX + segPosition;
				dataset.add(list, rowKey, colKey);

			} catch (final SegmentUpdateException e) {
				throw new ChartDatasetCreationException("Error fetching median profile", e);
			} catch (final MissingDataException e) {
				throw new ChartDatasetCreationException("Error fetching segment", e);
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
	private ViolinCategoryDataset createSegmentDisplacementDataset(
			List<IAnalysisDataset> collections, int segPosition)
			throws ChartDatasetCreationException {

		final ViolinCategoryDataset dataset = new ViolinCategoryDataset();

		for (int i = 0; i < collections.size(); i++) {

			final ICellCollection collection = collections.get(i).getCollection();

			IProfileSegment medianSeg;
			try {
				medianSeg = collection.getProfileCollection()
						.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE,
								Stats.MEDIAN)
						.getSegments().get(options.getSegPosition());
			} catch (MissingDataException | SegmentUpdateException e) {
				LOGGER.log(Level.SEVERE, "Unable to get segmented median profile", e);
				throw new ChartDatasetCreationException("Cannot get median profile");
			}

			final List<Number> list = new ArrayList<>(0);

			for (final Nucleus n : collection.getNuclei()) {

				try {

					final ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE,
							OrientationMark.REFERENCE);

					final IProfileSegment seg = profile.getSegment(medianSeg.getID());

					final double displacement = profile.getDisplacement(seg);
					list.add(displacement);
				} catch (MissingDataException | SegmentUpdateException e) {
					LOGGER.log(Level.SEVERE, "Error getting segmented profile", e);
					throw new ChartDatasetCreationException("Cannot get segmented profile", e);
				}

			}

			final String rowKey = IProfileSegment.SEGMENT_PREFIX + segPosition + "_" + i;
			final String colKey = IProfileSegment.SEGMENT_PREFIX + segPosition;

			dataset.add(list, rowKey, colKey);
		}
		return dataset;
	}
}
