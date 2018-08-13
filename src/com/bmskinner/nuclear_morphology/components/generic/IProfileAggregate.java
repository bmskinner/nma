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


package com.bmskinner.nuclear_morphology.components.generic;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;

public interface IProfileAggregate {

    /**
     * Add the values from the given profile to the aggregate via interpolation
     * @param yvalues
     * @throws ProfileException
     */
    void addValues(@NonNull IProfile yvalues) throws ProfileException;

    /**
     * Get the size of the bins covering the range 0-100
     * 
     * @return
     */
    double getBinSize();

    /**
     * Get the aggregate length
     * @return
     */
    int length();

    /**
     * Get the x-axis positions of the centre of each bin.
     * 
     * @return the Profile of positions
     */
    IProfile getXPositions();


    /**
     * Get the median profile of the aggregate
     * @return
     * @throws ProfileException
     */
    IProfile getMedian() throws ProfileException;

    /**
     * Get the profile corresponding to the given quartile of the values in the aggregate
     * @param quartile
     * @return
     * @throws ProfileException
     */
    IProfile getQuartile(double quartile) throws ProfileException;

    /**
     * Get the angle values at the given position in the aggragate from all
     * nuclei
     * 
     * @param position
     *            the position to search. Must be between 0 and the length of
     *            the aggregate.
     * @return an unsorted array of the values at the given position
     * @throws Exception
     */
    double[] getValuesAtPosition(double position);

    List<Double> getXKeyset();
}
