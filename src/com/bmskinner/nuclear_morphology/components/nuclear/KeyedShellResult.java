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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.LongStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

/**
 * This extension to the default shell result is designed to allow raw data to be
 * exported from shell analyses.
 * @author bms41
 * @since 1.13.8
 *
 */
public class KeyedShellResult implements IShellResult {
    
    final int nShells;
    private final ShellCount signalCounts;
    private final ShellCount counterCounts;
    
    
    public KeyedShellResult(int nShells){
        if (nShells < 1) 
            throw new IllegalArgumentException("Shell count must be greater than 1");
        
        this.nShells = nShells;
        signalCounts = new ShellCount(CountType.SIGNAL);
        counterCounts = new ShellCount(CountType.COUNTERSTAIN);
    }
    
    /**
     * Add shell data for the given nucleus in the given cell.
     * @param cell the cell
     * @param nucleus the nucleus
     * @param shellData the pixel intensity counts per shell
     */
    public void addShellData(@NonNull CountType type, @NonNull ICell c, @NonNull Nucleus n, long[] shellData){
        addShellData(type, c, n, null, shellData);
    }
    
    /**
     * Add shell data for a signal in the given nucleus in the given cell.
     * @param cell the cell
     * @param nucleus the nucleus
     * @param signal the signal
     * @param shellData the pixel intensity counts per shell
     */
    public void addShellData(@NonNull CountType type, @NonNull ICell cell, @NonNull Nucleus nucleus, @Nullable INuclearSignal signal, long[] shellData){
        if (shellData.length != nShells) 
            throw new IllegalArgumentException("Shell count must be "+nShells);
        
        Key k = signal==null ? new Key(cell.getId(), nucleus.getID()) : new Key(cell.getId(), nucleus.getID(), signal.getID());

        switch(type){
            case SIGNAL: signalCounts.addValues(k, shellData);
            break;
            case COUNTERSTAIN: counterCounts.addValues(k, shellData);
            break;
        }
    }
       
        
    @Override
    public List<Double> getRawMeans(CountType type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Double> getNormalisedMeans(CountType type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Double> getRawStandardErrors(CountType type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Double> getNormalisedStandardErrors(CountType type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getRawChiSquare(CountType type) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getNormalisedChiSquare(CountType type) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getRawPValue(CountType type) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getNormalisedPValue(CountType type) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getRawMeanShell(CountType type) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getNormalisedMeanShell(CountType type) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getNumberOfShells() {
        return nShells;
    }
    
    /**
     * Get the proportions of signal in the given shell by type
     * @param type the type of signal to fetch
     * @param norm the normalisation to apply
     * @param shell the shell to fetch
     * @return the values
     */
    public List<Double> getProportions(CountType type, Normalisation norm, int shell){
        if(shell<0||shell>=nShells)
            throw new IllegalArgumentException("Shell is out of bounds");
        
        List<Double> values = new ArrayList<>();
                
        for(Key id : signalCounts.objects()){
            long rawSignal  = signalCounts.getPixelIntensity(id, shell);
            long rawCounter = counterCounts.getPixelIntensity(id, shell);
            
            // No need to worry about normalising in either case
            if(type.equals(CountType.COUNTERSTAIN) || (type.equals(CountType.SIGNAL) && norm.equals(Normalisation.NONE))){
                long sum = signalCounts.sum(id);
                double prop = (double) rawCounter / (double) sum; 
                values.add(prop);  
            } else { // Need to normalise values
                long sumSignal  = signalCounts.sum(id);
                long sumCounter = counterCounts.sum(id);
                double propCounter = (double) rawCounter / (double) sumCounter; 
                
                double propSignal = (double) rawSignal / (double) sumSignal; 
                double normSignal = (double) propSignal / (double) sumCounter; 
                values.add(normSignal);
            }            
        }
        return values;
    }
    
    /**
     * Get the mean proportion of signal in the given shell by type
     * @param type the type of signal to fetch
     * @param norm the normalisation to apply
     * @param shell the shell to fetch
     * @return the mean signal proportion (the mean of the values returned by {@link getProportions()}
     */
    public double getAverageProportion(CountType type, Normalisation norm, int shell){
        return getProportions(type, norm, shell).stream().mapToDouble(d->d.doubleValue()).average().orElse(0);
    }
    
        
    
    /**
     * Store the individual counts per shell keyed to a source object
     * @author bms41
     *
     */
    private class ShellCount {
        
        final CountType type;
        final Map<Key, long[]> results;
        
        public ShellCount(@NonNull CountType type){
            this.type = type;
            results = new HashMap<>();
        }
        
        void addValues(@NonNull Key k, long[] values){
            results.put(k, values);
        }
        
        void addValue(@NonNull Key k, int shell, long value){
            if(!results.containsKey(k))
                results.put(k, new long[nShells]);
            results.get(k)[shell] = value;
        }
        
        int size(){
            return results.size();
        }
        
        /**
         * Fetch the sum of all values in the given shell
         * @param shell
         * @return
         */
        long sum(int shell){
            return results.values().stream().mapToLong(a->a[shell]).sum();
        }
        
        /**
         * Fetch the sum of all shells for the given object
         * @param id the id of the object
         * @return
         */
        long sum(Key k){
            return LongStream.of(results.get(k)).sum();
        }
                
        Set<Key> objects(){
            return results.keySet();
        }
        
        long getPixelIntensity(Key k, int shell){
            return results.get(k)[shell];
        }
        
        long[] getPixelIntensities(Key k){
            return results.get(k);
        }
    }
    
    /**
     * A key that allows distinction of cellular components
     * @author bms41
     *
     */
     private class Key {
        private final UUID cellId;
        private final UUID componentId;
        private final UUID signalId;
        
        public Key(@NonNull UUID cellId, @NonNull UUID componentId) {
            this(cellId, componentId, null );
        }
        
        public Key(@NonNull UUID cellId, @NonNull UUID componentId, @Nullable UUID signalId) {

            this.cellId = cellId;
            this.componentId = componentId;
            this.signalId = signalId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((cellId == null) ? 0 : cellId.hashCode());
            result = prime * result + ((componentId == null) ? 0 : componentId.hashCode());
            result = prime * result + ((signalId == null) ? 0 : signalId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (cellId == null) {
                if (other.cellId != null)
                    return false;
            } else if (!cellId.equals(other.cellId))
                return false;
            if (componentId == null) {
                if (other.componentId != null)
                    return false;
            } else if (!componentId.equals(other.componentId))
                return false;
            if (signalId == null) {
                if (other.signalId != null)
                    return false;
            } else if (!signalId.equals(other.signalId))
                return false;
            return true;
        }

        private KeyedShellResult getOuterType() {
            return KeyedShellResult.this;
        }
    }

    

    
}
