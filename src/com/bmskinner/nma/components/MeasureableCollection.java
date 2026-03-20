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
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;

/**
 * Describes the methods for retrieving aggregate stats from collections of
 * objects implementing the Statistical interface
 * 
 * @author Ben Skinner
 * @since 1.13.4
 *
 */
public interface MeasureableCollection {

	/**
	 * Clear the measurement, forcing the measurement to be recalculated on next
	 * request
	 * 
	 * @param measurement the measurement to recalculate e.g. Measurement.AREA
	 * @param component   the cellular component to fetch from e.g.
	 *                    CellularComponent.NUCLEUS
	 */
	void clear(@NonNull Measurement measurement, @NonNull String component);

	/**
	 * Force the given measurement to be recalculated
	 * 
	 * @param measurement the measurement to recalculate e.g. Measurement.AREA
	 * @param component   the cellular component to fetch from e.g.
	 *                    CellularComponent.NUCLEUS
	 * @param id          the sub-component id
	 */
	void clear(@NonNull Measurement measurement, @NonNull String component, @NonNull UUID id);

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
	 * @param measurement the measurement value to be fetched e.g. Measurement.AREA
	 * @param component   the cellular component that the measurement belongs to
	 *                    e.g. CellularComponent.NUCLEUS
	 * @param scale       the scale at which the values should be presented
	 * @param id          the id of the component if relevant (used for signals and
	 *                    segments)
	 * @return the minimum value
	 * @throws MissingDataException   if no data is present for the given
	 *                                measurement
	 * @throws SegmentUpdateException if a segment calculation fails
	 */
	double getMedian(@NonNull Measurement measurement, String component, MeasurementScale scale)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get the median value for a value with an ID - i.e. a nuclear signal or a
	 * segment
	 * 
	 * @param measurement the measurement value to be fetched e.g. Measurement.AREA
	 * @param component   the cellular component that the measurement belongs to
	 *                    e.g. CellularComponent.NUCLEUS
	 * @param scale       the scale at which the values should be presented
	 * @param id          the id of the component if relevant (used for signals and
	 *                    segments)
	 * @return the minimum value
	 * @throws MissingDataException   if no data is present for the given
	 *                                measurement
	 * @throws SegmentUpdateException if a segment calculation fails
	 */
	double getMedian(@NonNull Measurement measurement, String component, MeasurementScale scale, UUID id)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get the minimum value of the given measurement in the collection
	 * 
	 * @param measurement the measurement value to be fetched e.g. Measurement.AREA
	 * @param component   the cellular component that the measurement belongs to
	 *                    e.g. CellularComponent.NUCLEUS
	 * @param scale       the scale at which the values should be presented
	 * @return the minimum value
	 * @throws MissingDataException   if no data is present for the given
	 *                                measurement
	 * @throws SegmentUpdateException if a segment calculation fails
	 */
	double getMin(@NonNull Measurement measurement, String component, MeasurementScale scale)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get the minimum value of the given measurement in the collection
	 * 
	 * @param measurement the measurement value to be fetched e.g. Measurement.AREA
	 * @param component   the cellular component that the measurement belongs to
	 *                    e.g. CellularComponent.NUCLEUS
	 * @param scale       the scale at which the values should be presented
	 * @param id          the id of the component if relevant (used for signals and
	 *                    segments)
	 * @return the minimum value
	 * @throws MissingDataException   if no data is present for the given
	 *                                measurement
	 * @throws SegmentUpdateException if a segment calculation fails
	 */
	double getMin(@NonNull Measurement measurement, String component, MeasurementScale scale, UUID id)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get the maximum value of the given measurement in the collection
	 * 
	 * @param measurement the measurement value to be fetched e.g. Measurement.AREA
	 * @param component   the cellular component that the measurement belongs to
	 *                    e.g. CellularComponent.NUCLEUS
	 * @param scale       the scale at which the values should be presented
	 * @return the maximum value
	 * @throws MissingDataException   if no data is present for the given
	 *                                measurement
	 * @throws SegmentUpdateException if a segment calculation fails
	 */
	double getMax(@NonNull Measurement measurement, String component, MeasurementScale scale)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get the maximum value of the given measurement in the collection
	 * 
	 * @param measurement the measurement value to be fetched e.g. Measurement.AREA
	 * @param component   the cellular component that the measurement belongs to
	 *                    e.g. CellularComponent.NUCLEUS
	 * @param scale       the scale at which the values should be presented
	 * @param id          the id of the component if relevant (used for signals and
	 *                    segments)
	 * @return the maximum value
	 * @throws MissingDataException   if no data is present for the given
	 *                                measurement
	 * @throws SegmentUpdateException if a segment calculation fails
	 */
	double getMax(@NonNull Measurement measurement, String component, MeasurementScale scale, UUID id)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get the raw values for the given measurement for each object in the
	 * collection
	 * 
	 * @param measurement the measurement value to be fetched e.g. Measurement.AREA
	 * @param component   the cellular component that the measurement belongs to
	 *                    e.g. CellularComponent.NUCLEUS
	 * @param scale       the scale at which the values should be presented
	 * @return the values in the collection
	 * @throws MissingDataException   if no data is present for the given
	 *                                measurement
	 * @throws SegmentUpdateException if a segment calculation fails
	 */
	double[] getRawValues(@NonNull Measurement measurement, String component, MeasurementScale scale)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get the raw values for the given measurement for each object in the
	 * collection
	 * 
	 * @param measurement the measurement value to be fetched e.g. Measurement.AREA
	 * @param component   the cellular component that the measurement belongs to
	 *                    e.g. CellularComponent.NUCLEUS
	 * @param scale       the scale at which the values should be presented
	 * @param id          the id of the component if relevant (used for signals and
	 *                    segments)
	 * @return the values in the collection
	 * @throws MissingDataException   if no data is present for the given
	 *                                measurement
	 * @throws SegmentUpdateException if a segment calculation fails
	 */
	double[] getRawValues(@NonNull Measurement measurement, String component, MeasurementScale scale,
			UUID id)
			throws MissingDataException, SegmentUpdateException;

}
