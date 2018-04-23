/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.components.nuclear;

import java.util.stream.LongStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.ShrinkType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

/**
 * The random shell result stores random simulated shell data without the need 
 * for a full data store.
 * @author bms41
 * @since 1.13.8
 *
 */
public class RandomShellResult implements IShellResult{
    
    private static final long serialVersionUID = 1L;
    final int nShells;
    final ShrinkType type;
    long[] counts;

    
    /**
     * Create with the given number of shells
     * @param nShells
     */
    public RandomShellResult(int nShells, ShrinkType type, long[] counts){
        if (nShells < 1) 
            throw new IllegalArgumentException("Shell count must be greater than 1");
        this.nShells = nShells;
        this.counts = counts;
        this.type = type;
    }
    
    @Override
    public ShrinkType getType() {
        return type;
    }

    @Override
    public double[] getProportions(@NonNull Aggregation agg, @NonNull Normalisation norm) {
        long total = LongStream.of(counts).sum();
        if(total==0){
            double[] result = new double[nShells];
            for(int i=0; i<nShells; i++){
                result[i] = 0;
            }
            return result;
        }
        return LongStream.of(counts).mapToDouble(l-> (double)l/(double)total).toArray();
    }

    @Override
    public double[] getStdErrs(@NonNull Aggregation agg, @NonNull Normalisation norm) {
        double[] result = new double[nShells];
        for(int i=0; i<nShells; i++){
            result[i] = 0;
        }
        return result;
    }

    @Override
    public double getChiSquareValue(@NonNull Aggregation agg, @NonNull Normalisation norm) {
        return 1;
    }

    @Override
    public double getPValue(@NonNull Aggregation agg, @NonNull Normalisation norm) {
        return 1;
    }

    @Override
    public double getOverallShell(@NonNull Aggregation agg, @NonNull Normalisation norm) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long[] getPixelValues(@NonNull CountType type, @NonNull ICell cell, @NonNull Nucleus nucleus,
            @Nullable INuclearSignal signal) {
        return counts;
    }

    @Override
    public int getNumberOfShells() {
        return nShells;
    }

}
