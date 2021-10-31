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
    double STAT_NOT_CALCULATED    = -3d;
    double INVALID_OBJECT_TYPE    = -4d;

    /**
     * Check if the given stat is present
     * 
     * @param stat
     * @return
     */
    boolean hasStatistic(@NonNull Measurement stat);

    /**
     * Get the value of the given statistic for this component. Note that
     * {@link Measurement.VARIABILILTY} returns zero, as this must be
     * calculated at the collection level
     * 
     * @param stat the statistic to fetch
     * @param scale the units to return values in
     * @return the value or zero if
     *         stat.equals(PlottableStatistic.VARIABILILTY)==true
     */
    double getStatistic(@NonNull Measurement stat, @NonNull MeasurementScale scale);

    /**
     * Get the value of the given {@link PlottableStatistic} for this nucleus.
     * Note that {@link PlottableStatistic.VARIABILILTY} returns zero, as this
     * must be calculated at the collection level, not the object level. This
     * method converts exceptions from {@link CellularComponent#getStatistic()}
     * into RuntimeExceptions, so the method can be used in streams
     * 
     * @param stat the statistic to fetch
     * @param scale the units to return values in
     * @return the value or zero if stat.equals(
     *         {@link NucleusStatistic.VARIABILILTY})==true
     */
    // public double getSafeStatistic(PlottableStatistic stat, MeasurementScale
    // scale);

    /**
     * Get the statistic at the default scale ({@link MeasurementScale.PIXELS})
     * 
     * @param stat
     * @return
     */
    double getStatistic(@NonNull Measurement stat);

    /**
     * Set the statistic at the default scale ({@link MeasurementScale.PIXELS})
     * 
     * @param stat
     * @param d
     */
    void setStatistic(@NonNull Measurement stat, double d);
    
    /*
     * Remove the given statistic
     * @param stat
     */
    void clearStatistic(@NonNull Measurement stat);

    /**
     * Get all the statistics in this object
     * 
     * @return
     */
    List<Measurement> getStatistics();

}
