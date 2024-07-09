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
package com.bmskinner.nma.components;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.measure.MissingMeasurementException;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ProfileException;

/**
 * Describes the methods for retrieving aggregate stats from collections of
 * objects implementing the Statistical interface
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public interface MeasureableCollection {

	/**
	 * Clear the measurement, forcing the measurement to be recalculated on next
	 * request
	 * 
	 * @param stat      the measurement to recalculate
	 * @param component the cellular component to fetch from
	 */
	void clear(@NonNull Measurement stat, @NonNull String component);

	/**
	 * Force the given measurement to be recalculated
	 * 
	 * @param stat      the measurement to recalculate
	 * @param component the cellular component to fetch from
	 * @param id        the sub-component id
	 */
	void clear(@NonNull Measurement stat, @NonNull String component, @NonNull UUID id);

	/**
	 * Force the measurements at the given scale to be recalculated. Use when the
	 * image scale has been changed.
	 * 
	 * @param scale the scale to recalculate
	 */
	void clear(MeasurementScale scale);

	/**
	 * Get the median value of the given measurement in the collection
	 * 
	 * @param stat      the measurement to fetch
	 * @param component the cellular component to fetch from
	 * 
	 * @return the median value
	 * @throws MissingMeasurementException
	 * @throws MissingDataException
	 * @throws ProfileException
	 */
	double getMedian(@NonNull Measurement stat, String component, MeasurementScale scale)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get the median value for a value with an ID - i.e. a nuclear signal or a
	 * segment
	 * 
	 * @param stat      the measurement to fetch
	 * @param component the cellular component to fetch from
	 * @param scale     the scale to convert values to
	 * @param id        the id of the component to fetch
	 * @return the median value
	 * @throws MissingMeasurementException
	 * @throws MissingDataException
	 * @throws ProfileException
	 */
	double getMedian(@NonNull Measurement stat, String component, MeasurementScale scale, UUID id)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get the minimum value of the given measurement in the collection
	 * 
	 * @param stat
	 * @param component
	 * @param scale
	 * @param id
	 * @return the minimum
	 * @throws MissingMeasurementException
	 * @throws MissingDataException
	 * @throws ProfileException
	 */
	double getMin(@NonNull Measurement stat, String component, MeasurementScale scale)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get the minimum value of the given measurement in the collection
	 * 
	 * @param stat
	 * @param component
	 * @param scale
	 * @param id
	 * @return the minimum
	 * @throws MissingMeasurementException
	 * @throws MissingDataException
	 * @throws ProfileException
	 */
	double getMin(@NonNull Measurement stat, String component, MeasurementScale scale, UUID id)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get the maximum value of the given measurement in the collection
	 * 
	 * @param stat
	 * @param component
	 * @param scale
	 * @param id
	 * @return the maxumum
	 * @throws MissingMeasurementException
	 * @throws MissingDataException
	 * @throws ProfileException
	 */
	double getMax(@NonNull Measurement stat, String component, MeasurementScale scale)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get the maximum value of the given measurement in the collection
	 * 
	 * @param stat
	 * @param component
	 * @param scale
	 * @param id
	 * @return the maxumum
	 * @throws MissingMeasurementException
	 * @throws MissingDataException
	 * @throws ProfileException
	 */
	double getMax(@NonNull Measurement stat, String component, MeasurementScale scale, UUID id)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get the raw values for the given measurement for each object in the
	 * collection
	 * 
	 * @param stat      the statistic to fetch
	 * @param component the cellular component to fetch from
	 * @param scale     the scale to convert values to
	 * @return the values in the collection
	 * @throws MissingMeasurementException
	 * @throws MissingDataException
	 * @throws ProfileException
	 */
	double[] getRawValues(@NonNull Measurement stat, String component, MeasurementScale scale)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get the raw values for the given measurement for each object in the
	 * collection with an ID - i.e. a nuclear signal or a segment
	 * 
	 * @param stat      the statistic to fetch
	 * @param component the cellular component to fetch from
	 * @param scale     the scale to convert values to
	 * @param id        the id of the compenent to fetch
	 * @return the values in the collection
	 * @throws MissingMeasurementException
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	double[] getRawValues(@NonNull Measurement stat, String component, MeasurementScale scale,
			UUID id)
			throws MissingDataException, SegmentUpdateException;

}
