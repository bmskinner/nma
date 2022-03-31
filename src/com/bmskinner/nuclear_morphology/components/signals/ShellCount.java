package com.bmskinner.nuclear_morphology.components.signals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;
import com.bmskinner.nuclear_morphology.io.xml.XMLReader;

/**
 * Store the individual counts per shell keyed to a source object
 * @author bms41
 *
 */
public class ShellCount implements XmlSerializable {

	private final Map<ShellKey, long[]> results = new HashMap<>();
    
    public ShellCount(){ /* Nothing to create */ }

    
    public ShellCount(Element e) throws ComponentCreationException {
    	
    	for(Element el : e.getChildren("ShellValues")) {
    		ShellKey k = new ShellKey(el);
    		long[] l = XMLReader.parseLongArray(el.getText());
    		results.put(k, l);
    	}
    }
    
    @Override
	public Element toXmlElement() {
    	Element e = new Element("ShellCount");
    	
    	for(Entry<ShellKey, long[]> entry : results.entrySet()) {
    		Element c =entry.getKey().toXmlElement();
    		c.setText(Arrays.toString(entry.getValue()));
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
    	int l = 0;
    	for(long[] a : results.values()) {
    		l += a[shell];
    	}
    	return l;
//        return results.values().stream().mapToLong(a->a[shell]).sum();
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
        StringBuilder b = new StringBuilder("ShellKeys : "+results.size()+"\n");
        b.append("Size : "+size()+"\n");
//        b.append("Keys :\n");
//        for(ShellKey k :keys()){
//            b.append(k+"\n");
//        }
        
        for(Entry<ShellKey, long[]> e : results.entrySet()) {
        	b.append("Key: "+e.getKey()+"\n");
            for(int i=0; i<e.getValue().length; i++){
                b.append("Shell "+i+": "+sum(i)+"\n");
           }
        }

       return b.toString();
    }


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hash(results);
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
		ShellCount other = (ShellCount) obj;
		if(results.size()!=other.results.size())
			return false;
		
		for(Entry<ShellKey, long[]> e : results.entrySet()) {
			if(!other.results.containsKey(e.getKey()))
				return false;
			if(! Arrays.equals(e.getValue(), other.results.get(e.getKey())))
				return false;
		}
		
		return true;
	}

    
    
}

