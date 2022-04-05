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
package com.bmskinner.nuclear_morphology.components;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;

/**
 * This interface allows for the retrieval of statistics from cells and their
 * components
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public interface Statistical {

    double ERROR_CALCULATING_STAT = -1d;
    double MISSING_LANDMARK       = -2d;
    double INVALID_OBJECT_TYPE    = -4d;

    /**
     * Get the value of the given measurement for this component. Note that
     * {@link Measurement.VARIABILILTY} returns zero, as this must be
     * calculated at the collection level
     * 
     * @param stat the measurement to fetch
     * @param scale the units to return values in
     * @return the value or zero if
     *         stat.equals(Measurement.VARIABILILTY)
     */
    double getMeasurement(@NonNull Measurement stat, @NonNull MeasurementScale scale);

    /**
     * Get the measurement at the default scale ({@link MeasurementScale.PIXELS}),
     * calculating if not already present.
     * 
     * @param stat
     * @return
     */
    double getMeasurement(@NonNull Measurement stat);

    /**
     * Set the measurement at the default scale ({@link MeasurementScale.PIXELS})
     * 
     * @param stat
     * @param d
     */
    void setMeasurement(@NonNull Measurement stat, double d);
    
    /*
     * Remove the given measurement from the cache
     * @param measurement
     */
    void clearMeasurement(@NonNull Measurement stat);
    
    /**
     * Clear all measurements from the cache
     */
    void clearMeasurements();

    /**
     * Get all the measurements in this object
     * 
     * @return
     */
    List<Measurement> getMeasurements();

}
