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

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.visualisation.charts.ProfileChartFactory;
import com.bmskinner.nma.visualisation.options.DisplayOptions;
import com.google.api.client.repackaged.com.google.common.base.Objects;

/**
 * Base class for chart dataset creators
 * 
 * @author Ben Skinner
 *
 * @param <E> the options type describing the chart to build
 */
public abstract class AbstractDatasetCreator<E extends DisplayOptions> {

	protected final @NonNull E options;

	public static final String TAG_PREFIX = "Tag_";
	public static final String SEGMENT_SERIES_PREFIX = "Seg_";
	public static final String NUCLEUS_SERIES_PREFIX = "Nucleus_";
	public static final String QUARTILE_SERIES_PREFIX = "Q";
	public static final String PROFILE_SERIES_PREFIX = "Profile_";
	public static final String MEDIAN_SERIES_PREFIX = "Median_";

	protected static final String EMPTY_STRING = "";

	/**
	 * The maximum number of points from a single dataset that will be displayed in
	 * a scatter chart
	 */
	protected static final int MAX_SCATTER_CHART_ITEMS = 2000;

	/**
	 * The maximum number of nucleus profiles from a single dataset that will be
	 * displayed in a chart. This is separate from the
	 * {@link ProfileChartFactory.MAX_CELLS_FOR_INDIVIDUAL_PROFILE_CHART}, which
	 * controls the switchover between individual nuclei and ribbons. A dataset with
	 * 1500 cells would display individual nuclei, but only
	 * {@code MAX_PROFILE_CHART_ITEMS} will be drawn
	 */
	protected static final int MAX_PROFILE_CHART_ITEMS = 150;

	/** Default format for numbers */
	public static final String DEFAULT_DECIMAL_FORMAT = "#0.00";

	/** Default format for p-values */
	public static final String DEFAULT_PROBABILITY_FORMAT = "#0.0000";

	/**
	 * Construct with an options object describing the chart options
	 * 
	 * @param options
	 */
	protected AbstractDatasetCreator(@NonNull final E options) {
		this.options = options;
	}

	/**
	 * Keys for storing dataset related information in chart datasets. Used as an
	 * alternative to String keys for better parsing of data for legends
	 * 
	 */
	public record DatasetNameKey(IAnalysisDataset dataset) implements Comparable {
		@Override
		public int compareTo(Object o) {
			final String s = dataset.getName() + dataset.getId();
			if (o instanceof final DatasetNameKey k) {
				final String sk = k.dataset.getName() + k.dataset.getId();
				return s.compareTo(sk);
			}
			return s.compareTo(o.toString());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof final DatasetNameKey k)
				return dataset.equals(k.dataset);

			return false;
		}

		@Override
		public int hashCode() {
			return dataset.hashCode();
		}

		@Override
		public String toString() {
			return dataset.getName();
		}

	}

	/**
	 * Keys for storing dataset related information in chart datasets. Used as an
	 * alternative to String keys for better parsing of data for legends
	 * 
	 */
	public record MeasurementNameKey(Measurement m) implements Comparable {
		@Override
		public int compareTo(Object o) {
			final String s = m.toString();
			if (o instanceof final MeasurementNameKey k) {
				final String sk = k.toString();
				return s.compareTo(sk);
			}
			return s.compareTo(o.toString());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof final MeasurementNameKey k)
				return m.equals(k.m);

			return false;
		}

		@Override
		public int hashCode() {
			return m.hashCode();
		}

		@Override
		public String toString() {
			return m.toString();
		}
	}

	/**
	 * Keys for storing dataset related information in chart datasets. Used as an
	 * alternative to String keys for better parsing of data for legends
	 * 
	 */
	public record SignalNameKey(ISignalGroup sg, UUID signalGroupId) implements Comparable {
		@Override
		public int compareTo(Object o) {
			final String s = sg.toString() + "_" + signalGroupId.toString();
			if (o instanceof final SignalNameKey k) {
				final String sk = k.sg().toString() + "_" + k.signalGroupId().toString();
				return s.compareTo(sk);
			}
			return s.compareTo(o.toString());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof final SignalNameKey k)
				return sg.equals(k.sg) && signalGroupId.equals(k.signalGroupId);

			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(sg, signalGroupId);
		}

		@Override
		public String toString() {
			return sg.getGroupName();
		}

	}

	/**
	 * Keys for storing dataset related information in chart datasets. Used as an
	 * alternative to String keys for better parsing of data for legends
	 * 
	 */
	public record SegmentNameKey(int segmentPosition) implements Comparable {
		@Override
		public int compareTo(Object o) {
			if (o instanceof final SegmentNameKey k)
				return Integer.compare(segmentPosition, k.segmentPosition());
			return this.toString().compareTo(o.toString());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof final SegmentNameKey k)
				return segmentPosition == k.segmentPosition;

			return false;
		}

		@Override
		public int hashCode() {
			return Integer.hashCode(segmentPosition);
		}

		@Override
		public String toString() {
			return "Segment " + segmentPosition;
		}

	}
}
