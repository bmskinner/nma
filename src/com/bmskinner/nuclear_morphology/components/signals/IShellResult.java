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
package com.bmskinner.nuclear_morphology.components.signals;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;

/**
 * The interface for shell analysis results
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IShellResult extends XmlSerializable {
    
    @NonNull UUID RANDOM_SIGNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    
    
    /**
     * The types of shrinking that can be used to generate shells.
     * An area shrinking will attempt to keep all shells at equal area.
     * A radius shrinking will attempt to make shells evenly spaced from the centre
     * @author bms41
     * @since 1.13.8
     *
     */
    public enum ShrinkType {
    	
    	/** Shells have an equal radius, and different areas */
        RADIUS, 
        
        /** Shells have an equal area, and different radii */
        AREA;
    }
    
    
    /**
     * The types of pixel value that can be stored
     * @author bms41
     * @since 1.13.8
     *
     */
    public enum CountType {
    	
        /** Pixels in signal channels */
        SIGNAL,
        
        /** Pixels in counterstain channel */
        COUNTERSTAIN;
    }
    
    /**
     * The types of normalisation that can be applied to pixels
     * @author bms41
     * @since 1.13.8
     *
     */
    public enum Normalisation {
    	/** No normalisation is applied */
        NONE, 
        
        /** Signal intensities are normalised by counterstain intensity */
        DAPI;
    }
    
    /**
     * How pixels within a nucleus should be analysed: only the pixels within defined signals,
     * or any pixel in the nucleus 
     * @author bms41
     * @since 1.13.8
     *
     */
    public enum Aggregation {
    	/** Only pixels within defined signals are considered */
        BY_SIGNAL,
        
        /** All pixels within the nucleus are considered */
        BY_NUCLEUS,
    	
    	/** Only the position of a defined signal centre of mass is considered */
        SIGNAL_COM;
    }
    
    /**
     * Get the type of shrinking used to generate the shells
     * @return
     */
    ShrinkType getType();
    
    IShellResult duplicate();
    
    /**
     * Get the mean pixel proportion in each shell averaged
     * across all cells
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
     * Get the overall shell position for the pixels 
     * 
     * @param agg the aggregation of signal
     * @param norm the normalisation method
     * @return the overall shell position
     */
    double getOverallShell(@NonNull Aggregation agg, @NonNull Normalisation norm);
    
    
    /**
     * Get the observed aggregate values as a long array. This is the mean signal proportion per shell
     * multipled by the number of cells
     * @return the shell values
     */
    long[] getAggregateCounts(@NonNull Aggregation agg, @NonNull Normalisation norm);

    /**
     * Get the pixel count data for a signal in the given nucleus in the given cell.
     * The signal parameter can be left null to fetch pixels for the entire nucleus
     * @param type the type of pixel to fetch
     * @param cell the cell
     * @param nucleus the nucleus
     * @param signal the signal, or null to fetch all pixels in the nucleus
     * @return the pixel counts in that object per shell
     */
    long[] getPixelValues(@NonNull CountType type, @NonNull ICell cell, @NonNull Nucleus nucleus, @Nullable INuclearSignal signal);
    
    /**
     * Get the proportion of signal in each shell for a signal in the given nucleus in the given cell.
     * The signal parameter can be left null to fetch pixels for the entire nucleus
     * @param type the type of pixel to fetch
     * @param cell the cell
     * @param nucleus the nucleus
     * @param signal the signal, or null to fetch all pixels in the nucleus
     * @return the pixel counts in that object per shell
     */
    double[] getProportions(@NonNull CountType type, @NonNull ICell cell, @NonNull Nucleus nucleus, @Nullable INuclearSignal signal);
    
    /**
     * Get the number of shells in the shell result
     * 
     * @return the shell count
     */
    int getNumberOfShells();    
    
    /**
     * Get the number of signals stored in this result under the given aggregation type
     * @return
     */
    int getNumberOfSignals(@NonNull Aggregation agg);

}
