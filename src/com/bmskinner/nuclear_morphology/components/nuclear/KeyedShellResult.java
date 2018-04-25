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

import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.ShrinkType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.stats.Stats;

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
	final ShrinkType type;
	private final Map<CountType, ShellCount> map = new HashMap<>();    
    
    /**
     * Create with the given number of shells
     * @param nShells
     */
    public KeyedShellResult(int nShells, ShrinkType type){
        if (nShells < 1) 
            throw new IllegalArgumentException("Shell count must be greater than 1");
        
        this.nShells = nShells;
        this.type = type;
        
        for(CountType t : CountType.values()) {
        	map.put(t, new ShellCount());
        }
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
        
        // Sanitise inputs
        for(int i=0; i<nShells; i++){
            if(shellData[i]<0){
                shellData[i]=0;
            }
        }
        
        Key k = signal==null 
        		? new Key(cell.getId(), nucleus.getID()) 
        		: new Key(cell.getId(), nucleus.getID(), signal.getID());

        map.get(type).addValues(k, shellData);
    }
    
    /**
     * Get the pixel count data for a signal in the given nucleus in the given cell.
     * @param type the type of pixel to fetch
     * @param cell the cell
     * @param nucleus the nucleus
     * @param signal the signal
     * @return the pixel counts in that object per shell
     */
    public long[] getPixelValues(@NonNull CountType type, @NonNull ICell cell, @NonNull Nucleus nucleus, @Nullable INuclearSignal signal) {
    	Key k = signal==null 
        		? new Key(cell.getId(), nucleus.getID()) 
        		: new Key(cell.getId(), nucleus.getID(), signal.getID());
    	return map.get(type).getPixelIntensities(k);
    }
       
    
    @Override
    public double[] getProportions(@NonNull Aggregation agg, @NonNull Normalisation norm) {
    	double[] result = new double[nShells];
    	for(int i=0; i<nShells; i++){
            result[i] = getAverageProportion(norm, agg, i);
        }
        return result;
    }
    
    @Override
    public double[] getStdErrs(@NonNull Aggregation agg, @NonNull Normalisation norm) {
    	double[] result = new double[nShells];
    	for(int i=0; i<nShells; i++){
            result[i] = getStdErr(norm, agg, i);
        }
        return result;
    }

    @Override
    public int getNumberOfShells() {
        return nShells;
    }
    
    @Override
    public ShrinkType getType() {
        return type;
    }
    
    @Override
    public double getChiSquareValue(@NonNull Aggregation agg, @NonNull Normalisation norm, @NonNull IShellResult expected) {
    	long[] observed   = getObserved(agg, norm);
    	
    	double[] other = expected.getProportions(agg, norm);
    	double[] exp = getExpected(agg, norm, other);
    	
    	for(double d : exp){
            if(d<=0) // we can't do a chi square test if one of the values is zero
                return 1;
        }

    	ChiSquareTest test = new ChiSquareTest();
    	return test.chiSquare(exp, observed);
    }
    
    @Override
    public double getPValue(@NonNull Aggregation agg, @NonNull Normalisation norm, @NonNull IShellResult expected) {
    	 long[] observed   = getObserved(agg, norm);
    	 System.out.println("Obs vals: "+Arrays.toString(observed));
    	 
    	 double[] other = expected.getProportions(agg, norm);
    	 System.out.println("Exp prop: "+Arrays.toString(other));
    	 
         double[] exp = getExpected(agg, norm, other);
         System.out.println("Exp vals: "+Arrays.toString(exp));
         for(double d : exp){
             if(d<=0) // we can't do a chi square test if one of the values is zero
                 return 1;
         }

    	 ChiSquareTest test = new ChiSquareTest();
    	 return test.chiSquareTest(exp, observed);
    }
    
    /**
     * Get the observed values as a long array.
     * @return the observed shell values
     */
    private long[] getObserved(@NonNull Aggregation agg, @NonNull Normalisation norm) {
        long[] observed = new long[nShells];
        int count = map.get(CountType.SIGNAL).size(agg);
        double[] means = getProportions(agg, norm);
        for (int i = 0; i < nShells; i++) {
            double mean = means[i];
            observed[i] = (long) (mean * count);
        }
        return observed;
    }
    
    /**
     * Get the expected values for chi-sqare test, assuming an equal proportion
     * of signal per shell
     * 
     * @return the expected values
     */
    private double[] getExpected(@NonNull Aggregation agg, @NonNull Normalisation norm, double[] other) {
        double[] expected = new double[nShells];
        int count = map.get(CountType.SIGNAL).size(agg);
        for (int i=0; i<nShells; i++) {
            expected[i] = other[i] * count;
        }
        return expected;
    }
    
	@Override
	public double getOverallShell(@NonNull Aggregation agg, @NonNull Normalisation norm) {
		double[] props = getProportions(agg, norm);
		
		double overall = 0;
		for(int i=0; i<nShells; i++) {
			overall += props[i]*i;
		}
		return overall;
	}
	    
    /**
     * Get the proportions of signal in the given shell by type
     * @param agg the type of signal to fetch
     * @param norm the normalisation to apply
     * @param shell the shell to fetch
     * @return the proportion of signal in the shell in the range 0-1 for each object
     */
    private double[] getProportions(Aggregation agg, Normalisation norm, int shell){
        if(shell<0||shell>=nShells)
            throw new IllegalArgumentException("Shell is out of bounds");
                        
        switch(norm) {
	    	case NONE: return map.get(CountType.SIGNAL).keys(agg).stream()
					.mapToDouble(key->{
						double sig = map.get(CountType.SIGNAL).getPixelIntensity(key, shell);
						double tot = map.get(CountType.SIGNAL).sum(key);
						return sig/tot;
					}).toArray();
	    	case DAPI: {
	    		return map.get(CountType.SIGNAL).keys(agg).stream()
	    		.mapToDouble(k-> {
	    			
	    			long[] sigs = map.get(CountType.SIGNAL).getPixelIntensities(k);
	    			long[] cnts = map.get(CountType.COUNTERSTAIN).getPixelIntensities(k.componentKey());
	    			
	    			double sig = sigs[shell];
	    			double cnt = cnts[shell];
	    			double nor = cnt==0d?0d:(double)sig/(double)cnt;
	    			        			
	    			double totalNor = 0;
	    			for(int i=0; i<nShells; i++) {	
	    				totalNor += cnts[i]==0d?0d:(double)sigs[i]/(double)cnts[i];
	    			}
	    			return nor / totalNor;	
	    		}).toArray();
	    	}
	    	default: return new double[0];
        }	
    }
        
    /**
     * Get the mean proportion of signal in the given shell by type
     * @param type the type of signal to fetch
     * @param norm the normalisation to apply
     * @param shell the shell to fetch
     * @return the mean signal proportion (the mean of the values returned by {@link getProportions()}
     */
    private double getAverageProportion(Normalisation norm, Aggregation agg, int shell){
//    	fine(norm+" - "+agg+" - shell "+shell+"\n"+Arrays.toString(getProportions(agg, norm, shell)));
        return DoubleStream.of(getProportions(agg, norm, shell)).average().orElse(-1);
    }
    
    private double getStdErr(Normalisation norm, Aggregation agg, int shell){
        return Stats.stderr(getProportions(agg, norm, shell));
    }
    
    @Override
    public String toString(){
    	StringBuilder sb = new StringBuilder();
    	for(CountType t : CountType.values()){
    		sb.append(t +"\n"+map.get(t).toString());
    	}
        return sb.toString();
    }
    
    /**
     * Store the individual counts per shell keyed to a source object
     * @author bms41
     *
     */
    private class ShellCount implements Serializable {

		private static final long serialVersionUID = 1L;
        final Map<Key, long[]> results;
        
        public ShellCount(){
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
        
        /**
         * Get the number of objects in the counter
         * @return
         */
        int size(){
            return results.size();
        }
        
        int size(Aggregation agg){
            return keys(agg).size();
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
         * @param k the key of the object
         * @return
         */
        long sum(Key k){
            if(results.containsKey(k))
                return LongStream.of(results.get(k)).sum();
            return 0;
        }
                
        /**
         * Fetch all the object keys
         * @return
         */
        Set<Key> keys(){
            return results.keySet();
        }
        
        /**
         * Fetch only the object keys that match the given aggregation level
         * i.e {@link Aggregation.BY_NUCLEUS} will not fetch individual signal pixel values and
         * {@link Aggregation.BY_SIGNAL} will not fetch whole nucleus pixel values
         * @param agg the aggregation level
         * @return the object keys matching the aggregation level
         */
        Set<Key> keys(Aggregation agg){
        	switch(agg){
        		case BY_NUCLEUS: return results.keySet().stream().filter(k->!k.hasSignal()).collect(Collectors.toSet());
        		case BY_SIGNAL:  return results.keySet().stream().filter(k->k.hasSignal()).collect(Collectors.toSet());
        		default: return results.keySet().stream().filter(k->k.hasSignal()).collect(Collectors.toSet());
        	}
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
            for(Key k :keys()){
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
