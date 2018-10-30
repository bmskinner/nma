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
package com.bmskinner.nuclear_morphology.components.nuclear;

import java.util.Arrays;
import java.util.stream.LongStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.CountType;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Normalisation;
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
    public IShellResult duplicate() {
    	RandomShellResult r = new RandomShellResult(nShells, type, counts);
    	return r;
    }

    @Override
    public double[] getProportions(@NonNull Aggregation agg, @NonNull Normalisation norm) {
        
        switch(norm){
        
            case NONE:{long total = LongStream.of(counts).sum();
                if(total==0){
                    double[] result = new double[nShells];
                    for(int i=0; i<nShells; i++){
                        result[i] = 0;
                    }
                    return result;
                }
                return LongStream.of(counts).mapToDouble(l-> (double)l/(double)total).toArray();
            }
            case DAPI:{
                double[] result = new double[nShells];
                Arrays.fill(result, 1d/nShells);
                return result;
            }     
            default:{
                double[] result = new double[nShells];
                Arrays.fill(result, 1d/nShells);
                return result;
            } 
        }

    }
    
	@Override
	public double[] getProportions(@NonNull CountType type, @NonNull ICell cell, @NonNull Nucleus nucleus,
			@Nullable INuclearSignal signal) {
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
    public double getOverallShell(@NonNull Aggregation agg, @NonNull Normalisation norm) {
        return (nShells-1)/2;
    }

    @Override
    public long[] getPixelValues(@NonNull CountType type, @NonNull ICell cell, @NonNull Nucleus nucleus,
            @Nullable INuclearSignal signal) {
        return counts;
    }
    
    /**
     * Get the observed values as a long array.
     * @return the observed shell values
     */
    @Override
	public long[] getAggregateCounts(@NonNull Aggregation agg, @NonNull Normalisation norm) {
    	return counts;
    }
    
    @Override
   	public int getNumberOfSignals(@NonNull Aggregation agg) {
    	return 1;
    }

    @Override
    public int getNumberOfShells() {
        return nShells;
    }
    
    @Override
    public String toString(){
            StringBuilder b = new StringBuilder("Shells : "+nShells+"\n");
            for(int i=0; i<nShells; i++){
                 b.append("Shell "+i+": "+counts[i]+"\n");
            }
           return b.toString();
        
    }
}
