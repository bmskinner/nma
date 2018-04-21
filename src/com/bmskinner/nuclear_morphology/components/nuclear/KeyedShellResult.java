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

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

/**
 * This shell result is designed to allow raw data to be
 * exported from shell analyses. Averages and normalisations are computed when data are requested,
 * rather than needing to be pre-computed and stored as in the {@link DefaultShellResult}. It also prompted
 * the refactoring of the {@link IShellResult} interface.
 * @author bms41
 * @since 1.13.8
 *
 */
public class KeyedShellResult implements IShellResult {
    
	private static final long serialVersionUID = 1L;
	final int nShells;
    private final ShellCount signalCounts; // measurement in signal channel
    private final ShellCount counterCounts; // measurements in counterstain channel
    
    
    /**
     * Create with the given number of shells
     * @param nShells
     */
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
    public void addShellData(@NonNull CountType type, @NonNull ICell c, @NonNull Nucleus n, long @NonNull [] shellData){
        addShellData(type, c, n, null, shellData);
    }
    
    /**
     * Add shell data for a signal in the given nucleus in the given cell.
     * @param cell the cell
     * @param nucleus the nucleus
     * @param signal the signal
     * @param shellData the pixel intensity counts per shell
     */
    public void addShellData(@NonNull CountType type, @NonNull ICell cell, @NonNull Nucleus nucleus, @Nullable INuclearSignal signal, long @NonNull [] shellData){
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
    public double[] getProportions(Aggregation agg, Normalisation norm) {
    	double[] result = new double[nShells];
    	for(int i=0; i<nShells; i++){
            result[i] = getAverageProportion(norm, agg, i);
        }
        return result;
    }

    @Override
    public int getNumberOfShells() {
        return nShells;
    }
    
    @Override
    public double getChiSquareValue(Aggregation agg, Normalisation norm) {
    	return 0;
    }
    
    @Override
    public double getPValue(Aggregation agg, Normalisation norm) {
    	return 0;
    }
    
	@Override
	public double getOverallShell(Aggregation agg, Normalisation norm) {
		// TODO Auto-generated method stub
		return 0;
	}

    
    /**
     * Get the proportions of signal in the given shell by type
     * @param agg the type of signal to fetch
     * @param norm the normalisation to apply
     * @param shell the shell to fetch
     * @return the values in that shell
     */
    private double[] getProportions(Aggregation agg, Normalisation norm, int shell){
        if(shell<0||shell>=nShells)
            throw new IllegalArgumentException("Shell is out of bounds");
                
        switch(agg) {
	    	case BY_NUCLEUS: return getProportionsByNucleus(norm, shell);
	    	case BY_SIGNAL:  return getProportionsBySignal(norm, shell);
	    	default: return new double[0];
 
        }
    }
    
    private double[] getProportionsByNucleus(Normalisation norm, int shell){
        
        switch(norm) {
        	case NONE: return signalCounts.componentObjects().stream()
					.mapToDouble(k->signalCounts.getPixelIntensity(k, shell) / (double) signalCounts.sum(k))
					.toArray();
        	case DAPI: {
        		return signalCounts.componentObjects().stream()
        		.mapToDouble(k-> {
        			double sig = signalCounts.getPixelIntensity(k, shell);
        			double cnt = counterCounts.getPixelIntensity(k, shell);
        			double nor = sig/cnt;
        			
        			double totalNor = 0;
        			for(int i=0; i<nShells; i++) {
        				double s = signalCounts.getPixelIntensity(k, i);
        				double c = counterCounts.getPixelIntensity(k, i);
        				totalNor += c==0||s==0?0:s/c;
        			}
        			
        			return nor / totalNor;	
        		}).toArray();
        	}
        	default: return new double[0];
        }	
    }
    
    private double[] getProportionsBySignal(Normalisation norm, int shell){
        switch(norm) {
        	case NONE: return signalCounts.signalObjects().stream()
					.mapToDouble(k->signalCounts.getPixelIntensity(k, shell) / (double) signalCounts.sum(k))
					.toArray();
        	case DAPI: return new double[0];
        	default: return new double[0];
        }
        		
		
//		for(Key k : signalCounts.signalObjects()){
//			long totalSignal  = signalCounts.getPixelIntensity(k, shell);
//			if(norm.equals(Normalisation.NONE)){
//                long sum = signalCounts.sum(k);
//                double prop = (double) totalSignal / (double) sum; 
//                values.add(prop);  
//            } 
//		}
        
        
        
//        for(Key componentKey : signalCounts.componentObjects()){
//            
//            long totalSignal  = signalCounts.getPixelIntensity(componentKey, shell);
////            long totalCounter = counterCounts.getPixelIntensity(componentKey, shell);
//            
//            // No need to worry about normalising in either case
//            if(norm.equals(Normalisation.NONE)){
//                long sum = signalCounts.sum(componentKey);
//                double prop = (double) totalSignal / (double) sum; 
//                values.add(prop);  
//            } else { // Need to normalise values
//            	
//            	// For each signal, DAPI normalise counts
//            	
//                long sumSignal  = signalCounts.sum(componentKey);
//                long sumCounter = counterCounts.sum(componentKey);
//                double propCounter = (double) totalSignal / (double) sumCounter; 
//                
//                double propSignal = (double) totalSignal / (double) sumSignal; 
//                double normSignal = (double) propSignal / (double) sumCounter; 
//                values.add(normSignal);
//            }            
//        }
    }
    
    /**
     * Get the mean proportion of signal in the given shell by type
     * @param type the type of signal to fetch
     * @param norm the normalisation to apply
     * @param shell the shell to fetch
     * @return the mean signal proportion (the mean of the values returned by {@link getProportions()}
     */
    private double getAverageProportion(Normalisation norm, Aggregation agg, int shell){
    	fine(norm+" - "+agg+" - shell "+shell+"\n"+Arrays.toString(getProportions(agg, norm, shell)));
        return DoubleStream.of(getProportions(agg, norm, shell)).average().orElse(-1);
    }
    
    @Override
    public String toString(){
        return CountType.SIGNAL+"\n"+signalCounts.toString()
        	+"\n\n"+CountType.COUNTERSTAIN+"\n"+counterCounts.toString();
    }
    
    /**
     * Store the individual counts per shell keyed to a source object
     * @author bms41
     *
     */
    private class ShellCount implements Serializable {

		private static final long serialVersionUID = 1L;
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
            if(shell<0||shell>=nShells)
                throw new IllegalArgumentException("Shell is out of bounds");
            return results.values().stream().mapToLong(a->a[shell]).sum();
        }
        
        /**
         * Fetch the sum of all shells for the given object
         * @param id the id of the object
         * @return
         */
        long sum(Key k){
            if(results.containsKey(k))
                return LongStream.of(results.get(k)).sum();
            return 0;
        }
                
        Set<Key> objects(){
            return results.keySet();
        }
        
        Set<Key> signalObjects(){
            return results.keySet().stream().filter(k->k.hasSignal()).collect(Collectors.toSet());
        }
        
        Set<Key> componentObjects(){
            return results.keySet().stream().filter(k->!k.hasSignal()).collect(Collectors.toSet());
        }
        
        long getPixelIntensity(Key k, int shell){
            if(results.containsKey(k))
                return results.get(k)[shell];
            return 0;
        }
        
        long[] getPixelIntensities(Key k){
            return results.get(k);
        }
        
        List<long[]> getCellPixelIntensities(@NonNull UUID cellId){
            return results.keySet().stream()
                    .filter(k->k.hasCell(cellId))
                    .map(k->results.get(k))
                    .collect(Collectors.toList());
        }
        
        List<long[]> getComponentPixelIntensities(@NonNull UUID componentId){
            return results.keySet().stream()
                    .filter(k->k.hasComponent(componentId))
                    .map(k->results.get(k))
                    .collect(Collectors.toList());
        }
        
        List<long[]> getSignalPixelIntensities(@NonNull UUID signalId){
            return results.keySet().stream()
                    .filter(k->k.hasSignal(signalId))
                    .map(k->results.get(k))
                    .collect(Collectors.toList());
        }
        
        
        @Override
        public String toString(){
            StringBuilder b = new StringBuilder("Shells : "+nShells+"\n");
            b.append("Size : "+size()+"\n");
            b.append("Keys :\n");
            for(Key k :objects()){
                b.append(k+"\n");
            }
            for(int i=0; i<nShells; i++){
                 b.append("Shell "+i+": "+sum(i)+"\n");
            }
           return b.toString();
        }
    }
    
    /**
     * A key that allows distinction of cellular components
     * @author bms41
     *
     */
     private class Key implements Serializable {
		private static final long serialVersionUID = 1L;
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
        
        /**
         * Fetch the key covering the cell and component only
         * @return
         */
        public Key componentKey(){
            return new Key(cellId, componentId);
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
        
        public boolean hasCell(@NonNull UUID cellId){
            return this.cellId.equals(cellId);
        }
        
        public boolean hasComponent(@NonNull UUID id){
            return this.componentId.equals(id);
        }
        
        public boolean hasSignal(@NonNull UUID id){
            return signalId!=null&&signalId.equals(id);
        }
                
        public boolean hasSignal(){
            return signalId!=null;
        }
        
        @Override
        public String toString(){
            return signalId==null ? cellId.toString()+"_"+componentId.toString() : cellId.toString()+"_"+componentId.toString()+"_"+signalId.toString();
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
