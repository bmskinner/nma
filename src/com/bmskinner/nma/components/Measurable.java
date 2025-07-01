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

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;

/**
 * This interface allows for the retrieval of measurements from cells and their
 * components
 * 
 * @author Ben Skinner
 * @since 1.13.4
 *
 */
public interface Measurable {

	/**
	 * Get the value of the given measurement for this component. Note that
	 * {@link Measurement.VARIABILILTY} returns zero, as this must be calculated at
	 * the collection level
	 * 
	 * @param measurement the measurement to fetch
	 * @param scale       the units to return values in
	 * @return the value or zero if stat.equals(Measurement.VARIABILILTY)
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	double getMeasurement(@NonNull Measurement measurement, @NonNull MeasurementScale scale)
			throws MissingDataException, ComponentCreationException,
			SegmentUpdateException;

	/**
	 * Get the measurement at the default scale ({@link MeasurementScale.PIXELS}),
	 * calculating if not already present.
	 * 
	 * @param measurement
	 * @return
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	double getMeasurement(@NonNull Measurement measurement)
			throws MissingDataException, ComponentCreationException,
			SegmentUpdateException;

	/**
	 * Get the value of the given measurement for this component. Note that
	 * {@link Measurement.VARIABILILTY} returns zero, as this must be calculated at
	 * the collection level
	 * 
	 * @param measurement the measurement to fetch
	 * @param scale       the units to return values in
	 * @return the value or zero if stat.equals(Measurement.VARIABILILTY)
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	List<Double> getArrayMeasurement(@NonNull Measurement measurement, @NonNull MeasurementScale scale)
			throws MissingDataException, ComponentCreationException,
			SegmentUpdateException;

	/**
	 * Get the measurement at the default scale ({@link MeasurementScale.PIXELS}),
	 * calculating if not already present.
	 * 
	 * @param measurement
	 * @return
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	List<Double> getArrayMeasurement(@NonNull Measurement measurement)
			throws MissingDataException, ComponentCreationException,
			SegmentUpdateException;

	/**
	 * Set the measurement at the default scale ({@link MeasurementScale.PIXELS})
	 * 
	 * @param measurement
	 * @param d
	 */
	void setMeasurement(@NonNull Measurement measurement, double d);

	/**
	 * Set an array measurement at the default scale
	 * ({@link MeasurementScale.PIXELS})
	 * 
	 * @param measurement
	 * @param d
	 */
	void setMeasurement(@NonNull Measurement measurement, double[] d);

	/*
	 * Remove the given measurement from the cache
	 * 
	 * @param measurement
	 */
	void clearMeasurement(@NonNull Measurement measurement);

	/**
	 * Clear all measurements from the cache
	 */
	void clearMeasurements();

	boolean hasMeasurement(@NonNull Measurement measurement);

	/**
	 * Get all the measurements in this object
	 * 
	 * @return
	 */
	List<Measurement> getMeasurements();

}
