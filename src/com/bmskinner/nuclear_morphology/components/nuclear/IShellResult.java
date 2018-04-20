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


package com.bmskinner.nuclear_morphology.components.nuclear;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * The interface for shell analysis results
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IShellResult extends Serializable, Loggable {
    
    public static final UUID RANDOM_SIGNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    
    /**
     * The types of pixel value that can be stored
     * @author bms41
     * @since 1.13.8
     *
     */
    public enum CountType {
        SIGNAL, COUNTERSTAIN;
    }
    
    /**
     * The types of normalisation that can be applied to signals
     * @author bms41
     * @since 1.13.8
     *
     */
    public enum Normalisation {
        NONE, DAPI;
    }
    
    /**
     * How pixels within a nucleus should be analysed: only the pixels within defined signals,
     * or any pixel in the nucleus 
     * @author bms41
     * @since 1.13.8
     *
     */
    public enum Aggregation {
        BY_SIGNAL, BY_NUCLEUS;
    }

    /**
     * Get the mean pixel proportions for the given type.
     * 
     * @param type the counting type
     * @return proportions of signal per shell. If there are 5 shells, the list will have 5 items, each item the mean of that shell
     */
    double[] getRawMeans(CountType type, Aggregation agg);

    /**
     * Get the normalised pixel proportions for the given type
     * 
     * @param type the counting type
     * @return proportions of normalised signal per shell. If there are 5 shells, the list will have 5 items, each item the mean of that shell
     */
    double[] getNormalisedMeans(CountType type, Aggregation agg);

//    /**
//     * Get the standard error of pixel proportions for the given type
//     * 
//     * @param type the counting type
//     * @return a list of standard errors per shell
//     */
//    List<Double> getRawStandardErrors(CountType type);
//
//    /**
//     * Get the standard error of pixel proportions for the given type
//     * 
//     * @param type the counting type
//     * @return a list of proportions of signal per shell
//     */
//    List<Double> getNormalisedStandardErrors(CountType type);

    /**
     * Get the raw chi square test value for the given type
     * 
     * @param type the counting type
     * @return the result of a chi square test against equal proportions per
     *         shell
     */
    double getRawChiSquare(CountType type);

    /**
     * Get the normalised chi square test value for the given type
     * 
     * @param type the counting type
     * @return the result of a chi square test against equal proportions per
     *         shell
     */
    double getNormalisedChiSquare(CountType type);

    /**
     * Get the raw chi square p-value for the given type
     * 
     * @param type the counting type
     * @return the result of a chi square test against equal proportions per
     *         shell
     */
    double getRawPValue(CountType type);

    /**
     * Get the normalised chi square p-value for the given type
     * 
     * @param type the counting type
     * @return the result of a chi square test against equal proportions per
     *         shell
     */
    double getNormalisedPValue(CountType type);
    
    /**
     * Get the mean shell position from all measured signals
     * @param type the counting type
     * @return the mean of the shell positions for all signals
     */
    double getRawMeanShell(CountType type);
    
    /**
     * Get the mean shell position from all measured signals
     * @param type the counting type
     * @return the mean of the shell positions for all signals
     */
    double getNormalisedMeanShell(CountType type);

    /**
     * Get the number of shells in the shell result
     * 
     * @return the shell count
     */
    int getNumberOfShells();
    

}
