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

import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * This shell result is designed to allow raw data to be
 * exported from shell analyses. Averages and normalisations are computed when data are requested,
 * rather than needing to be pre-computed and stored.
 * @author bms41
 * @since 1.13.8
 *
 */
public class DefaultShellResult implements IShellResult {
    
	private static final long serialVersionUID = 1L;
	private final int nShells;
	private final ShrinkType type;
	private final Map<CountType, ShellCount> map = new EnumMap<>(CountType.class);    
    
    /**
     * Create with the given number of shells
     * @param nShells
     */
    public DefaultShellResult(int nShells, ShrinkType type){
        if (nShells < 1) 
            throw new IllegalArgumentException("Shell count must be greater than 1");
        
        this.nShells = nShells;
        this.type = type;
        
        for(CountType t : CountType.values()) {
        	map.put(t, new ShellCount());
        }
    }
    
    public DefaultShellResult(Element e) {
    	nShells = Integer.valueOf(e.getChildText("nShells"));
    	type = ShrinkType.valueOf(e.getChildText("ShrinkType"));
    	
    	for(Element el : e.getChildren("CountEntry")) {
    		CountType c = CountType.valueOf(el.getChildText("CountType"));
    		ShellCount s = new ShellCount(el.getChild("ShellCount"));
    		map.put(c, s);
    	}
    }
    
    @Override
	public Element toXmlElement() {
		Element e = new Element("ShellResult");
		
		e.addContent(new Element("nShells").setText(String.valueOf(nShells)));
		e.addContent(new Element("ShrinkType").setText(type.toString()));
		
		for(Entry<CountType, ShellCount> entry : map.entrySet()) {
			Element c = new Element("CountEntry");
			
			c.addContent(new Element("CountType").setText(entry.getKey().toString()));
			c.addContent(entry.getValue().toXmlElement());
			e.addContent(c);
		}
		
		return e;
	}



	/**
     * Create with the given number of shells
     * @param nShells
     */
    private DefaultShellResult(DefaultShellResult r){
    	this(r.getNumberOfShells(), r.getType());
        
        for(CountType t : CountType.values()) {
        	map.put(t, r.map.get(t).duplicate());
        }
    }
    
    @Override
    public IShellResult duplicate() {
    	return new DefaultShellResult(this);
    }
    
    /**
     * Add shell data for the given nucleus in the given cell.
     * @param cell the cell
     * @param component the nucleus
     * @param shellData the pixel intensity counts per shell
     */
    public void addShellData(@NonNull CountType countType, @NonNull ICell c, @NonNull Nucleus n, long @NonNull [] shellData){
        addShellData(countType, c, n, null, shellData);
    }
    
    /**
     * Add shell data for a signal in the given nucleus in the given cell.
     * @param cell the cell
     * @param nucleus the nucleus
     * @param signal the signal
     * @param shellData the pixel intensity counts per shell
     */
    public void addShellData(@NonNull CountType countType, @NonNull ICell cell, @NonNull Nucleus nucleus, @Nullable INuclearSignal signal, long @NonNull [] shellData){
        if (shellData.length != nShells) 
            throw new IllegalArgumentException("Shell count must be "+nShells);
        
        // Sanitise inputs
        for(int i=0; i<nShells; i++){
            if(shellData[i]<0){
                shellData[i]=0;
            }
        }
        
        ShellKey k = signal==null 
        		? new ShellKey(cell.getId(), nucleus.getID()) 
        		: new ShellKey(cell.getId(), nucleus.getID(), signal.getID());

        map.get(countType).putValues(k, shellData);
    }
    
    @Override
	public long[] getPixelValues(@NonNull CountType countType, @NonNull ICell cell, @NonNull Nucleus nucleus, @Nullable INuclearSignal signal) {
    	ShellKey k = signal==null 
        		? new ShellKey(cell.getId(), nucleus.getID()) 
        		: new ShellKey(cell.getId(), nucleus.getID(), signal.getID());
    	return map.get(countType).getPixelIntensities(k);
    }  
    
    @Override
	public double[] getProportions(@NonNull CountType countType, @NonNull ICell cell, @NonNull Nucleus nucleus, @Nullable INuclearSignal signal) {
    	ShellKey k = signal==null 
        		? new ShellKey(cell.getId(), nucleus.getID()) 
        		: new ShellKey(cell.getId(), nucleus.getID(), signal.getID());
        		
        long[] intensities = map.get(countType).getPixelIntensities(k);
        if(intensities==null)
        	return makeEmptyArray();
        long total = LongStream.of(intensities).sum();
        if(total==0)
        	return makeEmptyArray();

        return LongStream.of(intensities).mapToDouble(l-> (double)l/(double)total).toArray();
    } 
    
    private double[] makeEmptyArray() {
    	double[] result = new double[nShells];
    	Arrays.fill(result, 0);
    	return result;
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
   	public int getNumberOfSignals(@NonNull Aggregation agg) {
    	return map.get(CountType.SIGNAL).size(agg);
    }
    
    /**
     * Get the observed values as a long array.
     * @return the observed shell values
     */
    @Override
	public long[] getAggregateCounts(@NonNull Aggregation agg, @NonNull Normalisation norm) {
        long[] observed = new long[nShells];
        int nCells = map.get(CountType.SIGNAL).size(agg);
        double[] means = getProportions(agg, norm);
        for (int i = 0; i < nShells; i++) {
            double meanSignalProportion = means[i]; 
            observed[i] = (long) (meanSignalProportion * nCells); 
        }
        return observed;
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
	    			double nor = cnt==0d?0d:sig/cnt;
	    			        			
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
    private class ShellCount implements Serializable, XmlSerializable {

		private static final long serialVersionUID = 1L;
		private final Map<ShellKey, long[]> results = new HashMap<>();
        
        public ShellCount(){ /* Nothing to create */ }

        
        public ShellCount(Element e) {
        	
        	for(Element el : e.getChildren("Entry")) {
        		ShellKey k = new ShellKey(el.getChild("ShellKey"));
        		String[] s = el.getChildText("Counts").replace("[", "")
        		.replace("]", "").replace(" ", "").split(",");
        		
        		long[] l = new long[s.length];
        		for(int i=0; i<s.length; i++) {
        			l[i] = Long.parseLong(s[i]);
        		}
        	}
        }
        
        @Override
		public Element toXmlElement() {
        	Element e = new Element("ShellCount");
        	
        	for(Entry<ShellKey, long[]> entry : results.entrySet()) {
        		Element c = new Element("Entry");
        		c.addContent(entry.getKey().toXmlElement());
        		c.addContent(new Element("Counts").setText(Arrays.toString(entry.getValue())));
        		e.addContent(c);
        	}			
			return e;
		}



		public ShellCount duplicate() {
        	ShellCount result = new ShellCount();
        	for(ShellKey k: results.keySet()) {
        		result.results.put(k.duplicate(), results.get(k));
        	}
        	return result;
        }
        
        /**
         * Add the given values to the key. Existing values
         * are overwitten. 
         * @param k
         * @param values
         */
        public void putValues(@NonNull ShellKey k, long[] values){
            results.put(k, values);
        }
                
        /**
         * Get the number of objects in the counter
         * @return
         */
        public int size(){
            return results.size();
        }
        
        /**
         * Get the number of keys in the counter with the given
         * aggregation level
         * @return
         */
        public int size(Aggregation agg){
            return keys(agg).size();
        }
        
        /**
         * Fetch the sum of all values in the given shell
         * @param shell
         * @return
         */
        public long sum(int shell){
            if(shell<0||shell>=nShells)
                throw new IllegalArgumentException("Shell is out of bounds");
            return results.values().stream().mapToLong(a->a[shell]).sum();
        }
        
        /**
         * Fetch the sum of all shells for the given object
         * @param k the key of the object
         * @return
         */
        public long sum(ShellKey k){
            if(results.containsKey(k))
                return LongStream.of(results.get(k)).sum();
            return 0;
        }
                
        /**
         * Fetch all the object keys
         * @return
         */
        public Set<ShellKey> keys(){
            return results.keySet();
        }
        
        /**
         * Fetch only the object keys that match the given aggregation level
         * i.e {@link Aggregation.BY_NUCLEUS} will not fetch individual signal pixel values and
         * {@link Aggregation.BY_SIGNAL} will not fetch whole nucleus pixel values
         * @param agg the aggregation level
         * @return the object keys matching the aggregation level
         */
        public Set<ShellKey> keys(Aggregation agg){
        	switch(agg){
        		case BY_NUCLEUS: return results.keySet().stream().filter(k->!k.hasSignal()).collect(Collectors.toSet());
        		case BY_SIGNAL:  return results.keySet().stream().filter(k->k.hasSignal()).collect(Collectors.toSet());
        		default:         return results.keySet().stream().filter(k->k.hasSignal()).collect(Collectors.toSet());
        	}
        }
                
        public long getPixelIntensity(ShellKey k, int shell){
            if(results.containsKey(k))
                return results.get(k)[shell];
            return 0;
        }
        
        public long[] getPixelIntensities(ShellKey k){
            return results.get(k);
        }
        
        public List<long[]> getCellPixelIntensities(@NonNull UUID cellId){
            return results.keySet().stream()
                    .filter(k->k.hasCell(cellId))
                    .map(k->results.get(k))
                    .collect(Collectors.toList());
        }
        
        public List<long[]> getComponentPixelIntensities(@NonNull UUID componentId){
            return results.keySet().stream()
                    .filter(k->k.hasComponent(componentId))
                    .map(k->results.get(k))
                    .collect(Collectors.toList());
        }
        
        public List<long[]> getSignalPixelIntensities(@NonNull UUID signalId){
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
            for(ShellKey k :keys()){
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
     private class ShellKey implements Serializable, XmlSerializable {
		private static final long serialVersionUID = 1L;
		private final UUID cellId;
        private final UUID componentId;
        private final UUID signalId;
        
        public ShellKey(@NonNull UUID cellId, @NonNull UUID componentId) {
            this(cellId, componentId, null );
        }
        
        public ShellKey(@NonNull UUID cellId, @NonNull UUID componentId, @Nullable UUID signalId) {

            this.cellId = cellId;
            this.componentId = componentId;
            this.signalId = signalId;
        }
        
        public ShellKey(Element e) {
        	cellId = UUID.fromString(e.getChildText("CellId"));
        	componentId = UUID.fromString(e.getChildText("ComponentId"));
        	
        	if(e.getChild("SignalId")!=null)
        		signalId = UUID.fromString(e.getChildText("SignalId"));
        	else 
        		signalId = null;
        }
        
        @Override
		public Element toXmlElement() {
			Element e = new Element("ShellKey");
			
			if(cellId!=null)
				e.addContent(new Element("CellId").setText(cellId.toString()));
			if(componentId!=null)
				e.addContent(new Element("ComponentId").setText(componentId.toString()));
			if(signalId!=null)
				e.addContent(new Element("SignalId").setText(signalId.toString()));
			
			return e;
		}

		public ShellKey duplicate() {
        	if(signalId==null)
        		return new ShellKey(UUID.fromString(cellId.toString()), UUID.fromString(componentId.toString()), null);
        	return new ShellKey(UUID.fromString(cellId.toString()), UUID.fromString(componentId.toString()), UUID.fromString(signalId.toString()));
        }
        
        /**
         * Fetch the key covering the cell and component only
         * @return
         */
        public ShellKey componentKey(){
            return new ShellKey(cellId, componentId);
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
            ShellKey other = (ShellKey) obj;
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

        private DefaultShellResult getOuterType() {
            return DefaultShellResult.this;
        }
    }
    
}
