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

package com.bmskinner.nuclear_morphology.analysis.signals.shells;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.LongStream;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.CellularComponent;

/**
 * A replacement for the ShellCounter that retains object identities for exporting
 * @author bms41
 * @since 1.13.8
 *
 */
public class ObjectShellCounter {
    
    private final int nShells;    
    private final ShellCount signalCounts;
    private final ShellCount counterCounts;
        
    /**
     * The types of pixel value that can be stored
     * @author bms41
     *
     */
    public enum CountType {
        SIGNAL, COUNTERSTAIN;
    }
    
    /**
     * The types of normalisation that can be applied to signals
     * @author bms41
     *
     */
    public enum Normalisation {
        NONE, DAPI;
    }
    
    
    private class ShellCount {
        
        final CountType type;
        
        final Map<UUID, long[]> map;
        
        public ShellCount(CountType type){
            this.type = type;
            map = new HashMap<>();
        }
        
        void addValue(UUID id, int shell, long value){
            if(!map.containsKey(id))
                map.put(id, new long[nShells]);
            map.get(id)[shell] = value;
        }
        
        int size(){
            return map.size();
        }
        
        /**
         * Fetch the sum of all values in the given shell
         * @param shell
         * @return
         */
        long sum(int shell){
            return map.values().stream().mapToLong(a->a[shell]).sum();
        }
        
        /**
         * Fetch the sum of all shells for the given object
         * @param id the id of the object
         * @return
         */
        long sum(UUID id){
            return LongStream.of(map.get(id)).sum();
        }
                
        Set<UUID> objects(){
            return map.keySet();
        }
        
        long getPixelIntensity(UUID id, int shell){
            return map.get(id)[shell];
        }
    }
    
    /**
     * Store the pixel counts for a shell by object id
     * @author bms41
     * @since 1.13.8
     *
     */
    public ObjectShellCounter(int numberOfShells) {
        nShells = numberOfShells;
        signalCounts = new ShellCount(CountType.SIGNAL);
        counterCounts = new ShellCount(CountType.COUNTERSTAIN);
    }
    
    /**
     * Add the component intensity for a shell
     * @param type the type of measurement
     * @param shell the shell number (must be in the range 0<=shell<numberOfShells)
     * @param componentId the id of the object being measured
     * @param intensity the signal intensity in the shell (sum of 8bit pixel values within the object)
     */
    public void addShellPixelCounts(@NonNull CountType type, int shell, @NonNull UUID componentId, int intensity){
        if(shell<0||shell>=nShells)
            throw new IllegalArgumentException("Shell is out of bounds");
        

        switch(type){
            case SIGNAL: {
                signalCounts.addValue(componentId, shell, intensity);
                break;
            }    
            case COUNTERSTAIN: {
                counterCounts.addValue(componentId, shell, intensity);
                break;
            }
        }
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
                
        for(UUID id : signalCounts.objects()){
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
}
