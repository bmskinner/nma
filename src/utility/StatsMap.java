package utility;

import java.util.HashMap;
import java.util.Set;

/**
 * Holds single stats. Used in the Detector to 
 * transfer CoM, area etc.
 *
 */
public class StatsMap {
	
	private HashMap<String, Double> values = new HashMap<String, Double>(0);
	
	public StatsMap(){
		
	}
	
	public StatsMap(StatsMap s){
		for(String key : s.keys()){
			this.add(key, new Double(s.get(key)));
		}
	}
	
	public Set<String> keys(){
		return this.values.keySet();
	}
	
	public void add(String s, Double d){
		if(s==null || d==null){
			throw new IllegalArgumentException("Argument is null");
		}
		values.put(s, d);
	}
	
	public Double get(String s){
		if(s==null){
			throw new IllegalArgumentException("Argument is null");
		}
		if(!values.containsKey(s)){
			throw new IllegalArgumentException("Key is not present");
		}
		return values.get(s);
	}

}
