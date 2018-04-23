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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.CountType;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Normalisation;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
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
     * The types of normalisation that can be applied to pixels
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
     * Get the pixel proportions in each shell
     * 
     * @param agg the aggregation of signal
     * @param norm the normalisation method
     * @return the pixel proportions of signal
     */
    double[] getProportions(@NonNull Aggregation agg, @NonNull Normalisation norm);
    
    /**
     * Get the standard errors of the proportions in each shell
     * 
     * @param agg the aggregation of signal
     * @param norm the normalisation method
     * @return the pixel proportions of signal
     */
	double[] getStdErrs(@NonNull Aggregation agg, @NonNull Normalisation norm);
    
    /**
     * Get the chi square test value for the pixels against a random distribution
     * 
     * @param agg the aggregation of signal
     * @param norm the normalisation method
     * @return the result of a chi square test against equal proportions per
     *         shell
     */
    double getChiSquareValue(@NonNull Aggregation agg, @NonNull Normalisation norm);
    
    /**
     * Get the chi square p-value for the the pixels against a random distribution
     * 
     * @param agg the aggregation of signal
     * @param norm the normalisation method
     * @return the result of a chi square test against equal proportions per
     *         shell
     */
    double getPValue(@NonNull Aggregation agg, @NonNull Normalisation norm);
    
    /**
     * Get the overall shell position for the pixels 
     * 
     * @param agg the aggregation of signal
     * @param norm the normalisation method
     * @return the overall shell position
     */
    double getOverallShell(@NonNull Aggregation agg, @NonNull Normalisation norm);

    /**
     * Get the pixel count data for a signal in the given nucleus in the given cell.
     * @param type the type of pixel to fetch
     * @param cell the cell
     * @param nucleus the nucleus
     * @param signal the signal
     * @return the pixel counts in that object per shell
     */
    public long[] getPixelValues(@NonNull CountType type, @NonNull ICell cell, @NonNull Nucleus nucleus, @Nullable INuclearSignal signal);
    
    /**
     * Get the number of shells in the shell result
     * 
     * @return the shell count
     */
    int getNumberOfShells();    

}
